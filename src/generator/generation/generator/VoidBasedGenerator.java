package generator.generation.generator;

import generator.Dependency;
import generator.Utils;
import generator.generation.VoidBased;
import generator.types.CommonTypes;
import generator.types.TypeAttr;

public class VoidBasedGenerator implements Generator {
    private final VoidBased voidType;
    private final Dependency dependency;

    public VoidBasedGenerator(VoidBased voidType, Dependency dependency) {
        this.voidType = voidType;
        this.dependency = dependency;
    }

    @Override
    public void generate() {
        String out = voidType.getTypePkg().packagePath().makePackage();
        out += Generator.extractImports(voidType, dependency);
        out += makeContent(voidType.getTypePkg().type().typeName(TypeAttr.NameType.GENERIC));
        Utils.write(voidType.getTypePkg().packagePath(), out);
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
                CommonTypes.BasicOperations.Info.typeName(TypeAttr.NameType.RAW));
    }
}
