package generator.generation.generator;

import generator.types.TypeAttr;

public interface Generator {
    static String getTypeName(TypeAttr.TypeRefer type) {
        return ((TypeAttr.NamedType) type).typeName(TypeAttr.NameType.GENERIC);
    }

    void generate();
}
