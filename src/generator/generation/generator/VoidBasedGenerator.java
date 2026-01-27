package generator.generation.generator;

import generator.Dependency;
import generator.Generators;
import generator.PackageManager;
import generator.generation.VoidBased;
import generator.types.CommonTypes;

public class VoidBasedGenerator implements Generator {
    private final PackageManager packages;
    private final Generators.Writer writer;


    public VoidBasedGenerator(VoidBased voidType, Dependency dependency, Generators.Writer writer) {
        packages = new PackageManager(dependency, voidType.getTypePkg().packagePath());
        this.writer = writer;
    }

    @Override
    public void generate() {
        writer.write(packages, makeContent(packages));
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
