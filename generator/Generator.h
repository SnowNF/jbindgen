//
// Created by nettal on 23-11-9.
//

#ifndef JAVABINDGEN_GENERATOR_H
#define JAVABINDGEN_GENERATOR_H

#include <string>
#include <utility>
#include "GenUtils.h"
#include "../analyser/EnumDeclaration.h"
#include "EnumGenerator.h"
#include "StructGenerator.h"
#include "StructGeneratorUtils.h"
#include "FunctionSymbolGenerator.h"
#include "FunctionSymbolGeneratorUtils.h"

namespace jbindgen {
    struct GeneratorConfig {
        //root
        const std::string rootDir;
        const std::string nativeName;
        const std::string nativePackageName;

        //enum
        struct {
            std::string enumDir;
            std::string enumClassName;
            std::string enumPackageName;
            jbindgen::PFN_rename enumRename;
        } enums;

        struct {
            std::string structsDir;
            std::string packageName;
            PFN_structName structName;
            PFN_structMemberName memberName;
            PFN_decodeGetter decodeGetter;
            PFN_decodeSetter decodeSetter;
        } structs;

        struct {
            PFN_makeFunction makeFunction;
            std::string libName;
            std::string functionLoader;
            std::string head;
            std::string tail;
            std::string className;
        } functions;
    };

    inline GeneratorConfig defaultConfig(std::string rootDir, std::string nativeName, std::string nativePackageName) {
        GeneratorConfig config{.rootDir = std::move(rootDir), .nativeName=std::move(
                nativeName), .nativePackageName=std::move(nativePackageName)};
        config.enums.enumDir = config.rootDir;
        config.enums.enumClassName = config.nativeName + "Enums";
        config.enums.enumPackageName = config.nativePackageName;
        config.enums.enumRename = [](const std::string &s, void *) { return s; };
        config.structs.structsDir = config.rootDir + "/structs";
        config.structs.packageName = config.nativePackageName + ".structs";
        config.structs.structName = [](auto &s, void *) { return s.structType.name; };
        config.structs.memberName = StructGeneratorUtils::defaultStructMemberName;
        config.structs.decodeGetter = StructGeneratorUtils::defaultStructDecodeGetter;
        config.structs.decodeSetter = StructGeneratorUtils::defaultStructDecodeSetter;
        config.functions.className = config.nativeName + "Functions";
        config.functions.head = FunctionSymbolGeneratorUtils::defaultHead(config.functions.className,
                                                                          config.nativePackageName,
                                                                          config.functions.libName);
        config.functions.tail = FunctionSymbolGeneratorUtils::defaultTail();
        config.functions.makeFunction = FunctionSymbolGeneratorUtils::defaultMakeFunction;
        return config;
    }

    class Generator {
        const GeneratorConfig config;

    public:
        explicit Generator(GeneratorConfig config);

        void generateEnum(const std::vector<EnumDeclaration> &enums, void *enumRenameUserdata,
                          PFN_EnumGenerationFilter enumGenerationFilter, void *enumGenerationFilterUserdata = nullptr) {
            EnumGenerator generator(enums, config.enums.enumPackageName, config.enums.enumClassName,
                                    config.enums.enumDir,
                                    config.enums.enumRename, enumGenerationFilter);
            generator.build(enumRenameUserdata, enumGenerationFilterUserdata);
        }

        void generateStructs(StructDeclaration declaration, void *structRenameUserData, void *memberRenameUserData,
                             void *decodeGetterUserData, void *decodeSetterUserData,
                             PFN_StructGenerationFilter structGenerationFilter,
                             void *structGenerationFilterUserdata = nullptr) {
            StructGenerator generator(std::move(declaration), config.structs.structsDir, config.structs.packageName,
                                      config.structs.structName, config.structs.memberName,
                                      config.structs.decodeGetter, config.structs.decodeSetter, structGenerationFilter);
            generator.build(structRenameUserData, memberRenameUserData,
                            decodeGetterUserData, decodeSetterUserData, structGenerationFilterUserdata);
        }

        void generateFunctions(std::vector<FunctionDeclaration> declarations) {
            FunctionSymbolGenerator generator(config.functions.libName, config.functions.makeFunction,
                                              config.functions.functionLoader,
                                              config.functions.head, config.functions.tail, config.rootDir,
                                              std::move(declarations), config.functions.className);
            generator.build(nullptr);
        }
    };

} // jbindgen

#endif //JAVABINDGEN_GENERATOR_H
