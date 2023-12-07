//
// Created by snownf on 23-11-19.
//

#ifndef JBINDGEN_TYPEDEFGENERATORUTILS_H
#define JBINDGEN_TYPEDEFGENERATORUTILS_H

#include "TypedefGenerator.h"
#include <string>


#define GEN_FUNCTION "GEN_FUNCTION"

#define VOID_OR_FUNCTION_PTR "purVoid"

namespace jbindgen {
    class TypedefGeneratorUtils {

    public:
        static std::tuple<value::jbasic::ValueType, std::string, bool>
        defaultNameFunction(const jbindgen::NormalTypedefDeclaration *declaration);

        static std::string
        GenFuncSym(std::vector<std::string> jParameters, std::vector<std::string> functionDescriptors,
                   std::string className,
                   bool hasResult, std::string resultStr);

        static std::string
        GenFuncWrapper(std::vector<std::string> jParameters, const std::vector<std::string> &toLowerLevel,
                       const std::vector<std::string> &toUpperLevel,
                       const std::vector<std::string> &parentParameters, std::string className,
                       std::string parentClassName, bool hasResult, std::string resultType,
                       std::string parentResultType);
    };
}

#endif //JBINDGEN_TYPEDEFGENERATORUTILS_H
