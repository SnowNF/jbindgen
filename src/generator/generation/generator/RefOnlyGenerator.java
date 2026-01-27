package generator.generation.generator;

import generator.Generators;
import generator.PackageManager;
import generator.types.CommonTypes;
import generator.types.RefOnlyType;

public class RefOnlyGenerator implements Generator {
    private final RefOnlyType refOnly;

    public RefOnlyGenerator(RefOnlyType refOnly) {
        this.refOnly = refOnly;
    }

    @Override
    public GenerateResult generate(Generators.GenerationProvider locations, Generators.Writer writer) {
        PackageManager packages = new PackageManager(locations, refOnly);
        writer.write(packages, makeContent(packages));
        return new GenerateResult(packages, refOnly);
    }

    private static String makeContent(PackageManager packages) {
        return """
                public class %1$s {
                    private %1$s() {
                        throw new UnsupportedOperationException();
                    }
                
                    public static final %2$s.Operations<%1$s> OPERATIONS = %2$s.makeOperations();
                }
                """.formatted(packages.getClassName(),
                packages.useClass(CommonTypes.BasicOperations.Info) // 2
        );
    }
}
