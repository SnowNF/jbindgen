//
// Created by nettal on 23-11-7.
//

#include "StructDeclaration.h"
#include "Analyser.h"

#include <utility>
#include <iostream>
#include <cassert>

#include "../generator/GenUtils.h"

using std::cout;

namespace jbindgen {
    StructDeclaration::StructDeclaration(VarDeclare structType) : structType(std::move(structType)) {
    }

    std::string const StructDeclaration::getName() const {
        if (std::equal(structType.name.begin(), structType.name.end(), NO_NAME)) {
            assert(usages.size() == 1); //note: maybe greater than 1
            assert(parent!=nullptr);
            return parent->getName() + "$" + usages[0];
        }
        return structType.name;
    }

    void StructDeclaration::addUsage(const std::string&c) {
        usages.emplace_back(c);
    }

    std::shared_ptr<StructDeclaration> StructDeclaration::visit(CXCursor c, Analyser&analyser) {
        assert(c.kind == CXCursor_StructDecl);
        CXType type = clang_getCursorType(c);
        assert(type.kind == CXType_Record);
        assert(!clang_Cursor_isAnonymous(c));
        auto name = toStringWithoutConst(type);
        if (name.starts_with("struct ")) {
            name = name.substr(std::string_view("struct ").length());
        }
        StructDeclaration declaration(VarDeclare(name, type, clang_Type_getSizeOf(type),
                                                 getCommit(c), clang_getTypeDeclaration(type)));
        std::shared_ptr<StructDeclaration> shared_ptr = std::make_shared<StructDeclaration>(declaration);
        visitShared(c, shared_ptr, analyser);
        return shared_ptr;
    }

    std::shared_ptr<StructDeclaration> StructDeclaration::visitInternalStruct(CXCursor c, std::shared_ptr<StructDeclaration> parent,
                                                             Analyser&analyser) {
        assert(c.kind == CXCursor_StructDecl);
        CXType type = clang_getCursorType(c);
        c = clang_getTypeDeclaration(type);
        assert(c.kind == CXCursor_StructDecl);
        assert(type.kind == CXType_Record);
        assert(clang_Cursor_isAnonymous(c));
        assert(parent!=nullptr);
        StructDeclaration declaration(VarDeclare(NO_NAME,
                                                 type, clang_Type_getSizeOf(type), getCommit(c),
                                                 clang_getTypeDeclaration(type)));
        std::shared_ptr<StructDeclaration> shared_ptr = std::make_shared<StructDeclaration>(declaration);
        shared_ptr->parent = std::move(parent);
        visitShared(c, shared_ptr, analyser);
        assert(shared_ptr->parent!=nullptr);
        return shared_ptr;
    }

    CXChildVisitResult StructDeclaration::visitChildren4Members(CXCursor cursor, CXCursor parent,
                                                                CXClientData client_data) {
        auto* this_ptr = reinterpret_cast<std::shared_ptr<StructDeclaration> *>(static_cast<intptr_t *>(client_data)[0]);
        const auto theAnalyser = reinterpret_cast<Analyser *>(static_cast<intptr_t *>(client_data)[1]);
        if (clang_getCursorKind(cursor) == CXCursor_FieldDecl) {
            const int64_t offset = clang_Cursor_getOffsetOfField(cursor);
            if (offset < 0) {
                throw std::runtime_error(std::to_string(offset));
            }
            const CXType&cursorType = clang_getCursorType(cursor);
            const auto memberName = toString(clang_getCursorSpelling(cursor));
            auto member = StructMember(VarDeclare(
                                           memberName, cursorType, clang_Type_getSizeOf(cursorType), getCommit(cursor),
                                           clang_getTypeDeclaration(cursorType)), offset);
            (*this_ptr)->members.emplace_back(member);
        }
        return CXChildVisit_Continue;
    }

    CXChildVisitResult StructDeclaration::visitChildren4Declarations(CXCursor cursor, CXCursor parent,
                                                                     CXClientData client_data) {
        auto* this_ptr = reinterpret_cast<std::shared_ptr<StructDeclaration> *>(static_cast<intptr_t *>(client_data)[0]);
        const auto theAnalyser = reinterpret_cast<Analyser *>(static_cast<intptr_t *>(client_data)[1]);
        if (clang_getCursorKind(cursor) == CXCursor_FieldDecl) {
            return CXChildVisit_Continue;
        }
        if (clang_getCursorKind(cursor) == CXCursor_TypedefDecl) {
            theAnalyser->visitStructInternalFunctionPointer(cursor, *this_ptr);
            return CXChildVisit_Continue;
        }
        if (clang_getCursorKind(cursor) == CXCursor_FunctionDecl) {
            assert(0);
        }
        if (clang_getCursorKind(cursor) == CXCursor_StructDecl) {
            theAnalyser->visitStructInternalStruct(cursor, *this_ptr);
            return CXChildVisit_Continue;
        }
        if (clang_getCursorKind(cursor) == CXCursor_UnionDecl) {
            theAnalyser->visitStructInternalUnion(cursor, *this_ptr);
            return CXChildVisit_Continue;
        }
        assert(0);
    }

    CXChildVisitResult StructDeclaration::visitChildren4Usages(CXCursor cursor, CXCursor parent,
                                                               CXClientData client_data) {
        auto* this_ptr = reinterpret_cast<std::shared_ptr<StructDeclaration>*>(static_cast<intptr_t *>(client_data)[0]);
        const auto theAnalyser = reinterpret_cast<Analyser *>(static_cast<intptr_t *>(client_data)[1]);
        if (clang_getCursorKind(cursor) == CXCursor_FieldDecl) {
            const CXType&cursorType = clang_getCursorType(cursor);
            const auto memberName = toString(clang_getCursorSpelling(cursor));
            //visit member type here to prevent internal named struct become indepent,
            //and then have equal class name in generated java class
            if (cursorType.kind == CXType_Pointer || cursorType.kind == CXType_BlockPointer) {
                theAnalyser->visitCXCursor(clang_getTypeDeclaration(toDeepPointeeOrArrayType(cursorType, cursor)));
            }
            if (isArrayType(cursorType.kind)) {
                theAnalyser->visitCXCursor(clang_getTypeDeclaration(toDeepPointeeOrArrayType(cursorType, cursor)));
            }
            if (cursorType.kind == CXType_Elaborated) {
                theAnalyser->visitCXCursor(clang_getTypeDeclaration(toDeepPointeeOrArrayType(cursorType, cursor)));
            }
            if (hasDeclaration(cursorType)) {
                auto decl = clang_getTypeDeclaration(cursorType);
                assert(theAnalyser->cxCursorMap.contains(decl));
                auto value = theAnalyser->cxCursorMap[decl];
                value->addUsage(memberName);
            }
        }
        return CXChildVisit_Continue;
    }

    std::ostream& operator<<(std::ostream&stream, const StructDeclaration&str) {
        stream << "#### Structure " << str.structType << std::endl;
        for (auto&item: str.members) {
            stream << "  " << item << std::endl;
        }
        return stream;
    }


    void StructDeclaration::visitShared(CXCursor c, std::shared_ptr<StructDeclaration> declaration,
                                                     Analyser&analyser) {
        if (declaration->structType.byteSize < 0) {
            return;
        }
        intptr_t pUser[] = {
            reinterpret_cast<intptr_t>(&declaration), reinterpret_cast<intptr_t>(&analyser)
        };
        clang_visitChildren(c, StructDeclaration::visitChildren4Members, pUser);
        clang_visitChildren(c, StructDeclaration::visitChildren4Declarations, pUser);
        clang_visitChildren(c, StructDeclaration::visitChildren4Usages, pUser);
    }

    StructMember::StructMember(jbindgen::VarDeclare type, int64_t offsetOfBit) : var(std::move(type)),
        offsetOfBit(offsetOfBit) {
    }

    std::ostream& operator<<(std::ostream&stream, const StructMember&member) {
        stream << "Struct Member Info:  " << member.var << " offsetOfBit: " << member.offsetOfBit;
        return stream;
    }
}
