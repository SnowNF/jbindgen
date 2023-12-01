//
// Created by nettal on 23-11-18.
//

#include "TypedefGenerator.h"

#include <utility>

namespace jbindgen {
    TypedefGenerator::TypedefGenerator(NormalTypedefDeclaration declaration,
                                       std::string defStructPackageName,
                                       std::string defValuePackageName,
                                       std::string defEnumPackageName,
                                       std::string defEnumDir,
                                       std::string defStructDir,
                                       std::string defValueDir,
                                       std::string defCallbackPackageName,
                                       std::string defCallbackDir,
                                       std::string nativeFunctionPackageName,
                                       FN_def_name name) :
            declaration(std::move(declaration)),
            defsStructPackageName(std::move(defStructPackageName)),
            defsValuePackageName(std::move(defValuePackageName)),
            defEnumDir(std::move(defEnumDir)),
            defStructDir(std::move(defStructDir)),
            defValueDir(std::move(defValueDir)),
            defsEnumPackageName(std::move(defEnumPackageName)),
            defCallbackDir(std::move(defCallbackDir)),
            defsCallbackPackageName(std::move(defCallbackPackageName)),
            nativeFunctionPackageName(std::move(nativeFunctionPackageName)),
            name(name) {
    }
} // jbindgen