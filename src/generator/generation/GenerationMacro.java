package generator.generation;

import generator.types.CommonTypes;

public sealed interface GenerationMacro {
    record Primitive(CommonTypes.Primitives primitives, String declName,
                     String initializer, String comment) implements GenerationMacro {
    }

    record StrMacro(String declName, String initializer, String comment) implements GenerationMacro {
    }
}
