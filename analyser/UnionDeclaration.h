//
// Created by snownf on 23-11-7.
//

#ifndef JAVABINDGEN_UNIONDECLARATION_H
#define JAVABINDGEN_UNIONDECLARATION_H

#include "StructDeclaration.h"

namespace jbindgen {

    class UnionDeclaration : public StructDeclaration {


        explicit UnionDeclaration(Typed typed);

    public:
        static UnionDeclaration visit(CXCursor c);

        friend std::ostream &operator<<(std::ostream &stream, const UnionDeclaration &str) {
            stream << "#### Union " << str.structType << std::endl;
            for (auto &item: str.members) {
                stream << "  " << item << std::endl;
            }
            return stream;
        };
    };
}


#endif //JAVABINDGEN_UNIONDECLARATION_H
