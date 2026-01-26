package generator.generation;

import generator.Dependency;
import generator.Generators;
import generator.PackagePath;
import generator.generation.generator.CommonGenerator;
import generator.types.CommonTypes;

import java.util.Arrays;
import java.util.List;

public final class Common extends AbstractGeneration<CommonTypes.BaseType> {
    public static List<Common> makeBindTypes(PackagePath packagePath) {
        return Arrays.stream(CommonTypes.BindTypes.values())
                .map(types -> new Common(packagePath, types)).toList();
    }

    public static List<Common> makeValueInterfaces(PackagePath packagePath) {
        return Arrays.stream(CommonTypes.ValueInterface.values())
                .map(types -> new Common(packagePath, types)).toList();
    }

    public static List<Common> makeBasicOperations(PackagePath packagePath) {
        return Arrays.stream(CommonTypes.BasicOperations.values())
                .map(types -> new Common(packagePath, types)).toList();
    }

    public static List<Common> makeBindTypeInterface(PackagePath packagePath) {
        return Arrays.stream(CommonTypes.BindTypeOperations.values())
                .map(types -> new Common(packagePath, types)).toList();
    }

    public static List<Common> makeSpecific(PackagePath packagePath, CommonTypes.SpecificTypes specificTypes) {
        return List.of(new Common(packagePath, specificTypes));
    }

    public static List<Common> makeSpecific(PackagePath packagePath) {
        return Arrays.stream(CommonTypes.SpecificTypes.values())
                .map(types -> new Common(packagePath, types)).toList();
    }

    public static List<Common> makeFFMs() {
        return Arrays.stream(CommonTypes.FFMTypes.values())
                .map(types -> new Common(makePackagePathByClass(types.getType()), types)).toList();
    }

    private static PackagePath makePackagePathByClass(Class<?> clazz) {
        String packageName = clazz.getPackageName();
//        String simpleName = clazz.getSimpleName();
        PackagePath packagePath = new PackagePath();
        for (String s : packageName.split("\\.")) {
            packagePath = packagePath.add(s);
        }
        return packagePath;
    }

    public Common(PackagePath path, CommonTypes.BaseType type) {
        super(path, type);
    }

    @Override
    public void generate(Dependency dependency, Generators.Writer writer) {
        new CommonGenerator(this, dependency, writer).generate();
    }
}
