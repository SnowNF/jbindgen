package generator.generation.generator;

import generator.Dependency;
import generator.Generators;
import generator.generation.VoidBased;
import generator.types.CommonTypes;
import generator.types.TypeAttr;

public class VoidBasedGenerator implements Generator {
    private final VoidBased voidType;
    private final Dependency dependency;
    private final Generators.Writer writer;

    public VoidBasedGenerator(VoidBased voidType, Dependency dependency, Generators.Writer writer) {
        this.voidType = voidType;
        this.dependency = dependency;
        this.writer = writer;
    }

    @Override
    public void generate() {
        String out = voidType.getTypePkg().packagePath().makePackage();
        out += Generator.extractImports(voidType, dependency);
        out += makeContent(voidType.getTypePkg().type().typeName(TypeAttr.NameType.GENERIC));
        writer.write(voidType.getTypePkg().packagePath(), out);
    }


    private static String makeContent(String className) {
        return """
                public class %1$s {
                    private %1$s() {
                        throw new UnsupportedOperationException();
                    }
                
                    public static final %2$s.Operations<%1$s> OPERATIONS = %2$s.makeOperations();
                }
                """.formatted(className,
                CommonTypes.BasicOperations.Info.typeName(TypeAttr.NameType.RAW) // 2
        );
    }
}
