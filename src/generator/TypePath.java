package generator;

import generator.types.TypeAttr;

public record TypePath<T extends TypeAttr.GenerationType & TypeAttr.NamedType & TypeAttr.TypeRefer>
        (T type, PackagePath packagePath) {
}
