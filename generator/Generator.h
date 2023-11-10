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
            PFN_rename structRename;
            PFN_rename memberRename;
            PFN_decodeGetter decodeGetter;
            PFN_decodeSetter decodeSetter;
        } structs;
    };

    inline GeneratorConfig defaultConfig(std::string rootDir, std::string nativeName, std::string nativePackageName) {
        GeneratorConfig config{.rootDir = std::move(rootDir), .nativeName=std::move(
                nativeName), .nativePackageName=std::move(nativePackageName)};
        config.enums.enumDir = config.rootDir;
        config.enums.enumClassName = config.nativeName + "Enums";
        config.enums.enumPackageName = config.nativePackageName;
        config.enums.enumRename = [](std::string s) { return s; };
        config.structs.structsDir = config.rootDir + "/structs";
        config.structs.packageName = config.nativePackageName + ".structs";
        config.structs.structRename = [](std::string s) { return s; };
        config.structs.memberRename = [](std::string s) { return s; };//todo rename toString ,clone and others
        config.structs.decodeGetter = nullptr;//todo
        config.structs.decodeSetter = nullptr;//todo
        return config;
    }

    class Generator {
        const GeneratorConfig config;

    public:
        explicit Generator(GeneratorConfig config);

        void generateEnum(const std::vector<EnumDeclaration> &enums) {
            EnumGenerator generator(enums, config.enums.enumPackageName, config.enums.enumClassName,
                                    config.enums.enumDir,
                                    config.enums.enumRename);
            generator.build();
        }
    };

} // jbindgen

#endif //JAVABINDGEN_GENERATOR_H
