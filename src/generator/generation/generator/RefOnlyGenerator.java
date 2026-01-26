package generator.generation.generator;

import generator.Dependency;
import generator.Generators;
import generator.generation.RefOnly;
import generator.types.CommonTypes;
import generator.types.TypeAttr;

public class RefOnlyGenerator implements Generator {
    private final RefOnly refOnly;
    private final Dependency dependency;
    private final Generators.Writer writer;

    public RefOnlyGenerator(RefOnly refOnly, Dependency dependency, Generators.Writer writer) {
        this.refOnly = refOnly;
        this.dependency = dependency;
        this.writer = writer;
    }

    @Override
    public void generate() {
        String out = refOnly.getTypePkg().packagePath().makePackage();
        out += Generator.extractImports(refOnly, dependency);
        out += makeContent(refOnly.getTypePkg().type().typeName(TypeAttr.NameType.GENERIC));
        writer.write(refOnly.getTypePkg().packagePath(), out);
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
