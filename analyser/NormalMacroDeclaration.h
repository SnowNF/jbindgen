//
// Created by snownf on 23-11-7.
//

#ifndef JAVABINDGEN_NORMALDEFINITIONDECLARATION_H
#define JAVABINDGEN_NORMALDEFINITIONDECLARATION_H

#include <clang-c/Index.h>

namespace jbindgen {

    class NormalMacroDeclaration {
        explicit NormalMacroDeclaration(std::pair<std::string, std::string> pair1,
                                        CXCursor cursor);

    public:
        const std::pair<std::string, std::string> normalDefines;
        const CXCursor cursor;
        static NormalMacroDeclaration visit(CXCursor param);

        friend std::ostream &operator<<(std::ostream &stream, const NormalMacroDeclaration &normal);
    };

} // jbindgen

#endif //JAVABINDGEN_NORMALDEFINITIONDECLARATION_H
