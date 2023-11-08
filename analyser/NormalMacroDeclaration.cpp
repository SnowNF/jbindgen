//
// Created by snownf on 23-11-7.
//

#include <clang-c/Index.h>
#include <string>
#include <cstring>
#include <iostream>
#include <utility>
#include "NormalMacroDeclaration.h"
#include "Analyser.h"

namespace jbindgen {
    NormalMacroDeclaration NormalMacroDeclaration::visit(CXCursor param) {
        CXTranslationUnit tu = clang_Cursor_getTranslationUnit(param);
        const std::string &ori = toString(clang_getCursorSpelling(param));
        std::string mapped;
        CXToken *tokens;
        unsigned numTokens;
        clang_tokenize(tu, clang_getCursorExtent(param), &tokens, &numTokens); // 将宏定义转换为令牌序列
        if (strcmp(toString(clang_getTokenSpelling(clang_Cursor_getTranslationUnit(param), tokens[0])).c_str(),
                   ori.c_str()) == 0) {
            for (unsigned i = 1; i < numTokens; ++i) {
                if (DEBUG_LOG)
                    std::cout << "token kind: " << clang_getTokenKind(tokens[i]) << std::endl << std::flush;
                mapped += toString(clang_getTokenSpelling(clang_Cursor_getTranslationUnit(param),
                                                          tokens[i]));
            }
        }
        NormalMacroDeclaration def = NormalMacroDeclaration(std::pair<std::string, std::string>(ori, mapped));
        clang_disposeTokens(clang_Cursor_getTranslationUnit(param), tokens, numTokens); // 释放令牌序列
        return def;
    }

    NormalMacroDeclaration::NormalMacroDeclaration(std::pair<std::string, std::string> pair1) : normalDefines(
            std::move(pair1)) {
    }
} // jbindgen