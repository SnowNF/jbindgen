package generator.generators;

import generator.Generators;
import generator.PackageManager;
import generator.types.CommonTypes;
import generator.types.VoidType;

public class VoidBasedGenerator implements Generator {

    private final VoidType voidType;

    public VoidBasedGenerator(VoidType voidType) {
        this.voidType = voidType;
    }

    @Override
    public GenerateResult generate(Generators.GenerationProvider locations, Generators.Writer writer) {
        var packages = new PackageManager(locations, voidType);
        if (!voidType.realVoid())
            writer.write(packages, makeContent(packages));
        return new GenerateResult(packages, voidType);
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
