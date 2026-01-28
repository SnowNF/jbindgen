package generator.generators;

import generator.Generators;
import generator.PackageManager;
import generator.PackagePath;
import generator.types.TypeAttr;

import java.util.List;

public class ConstGenerator implements Generator {
    private final List<ConstValue> values;
    private final PackagePath path;

    public ConstGenerator(List<ConstValue> values, PackagePath path) {
        this.values = values;
        this.path = path;
    }

    @Override
    public GenerateResult generate(Generators.GenerationProvider locations, Generators.Writer writer) {
        var packages = new PackageManager(locations, path);
        if (values.isEmpty()) return null;
        StringBuilder core = new StringBuilder();
        for (var val : values) {
            String typeName = packages.useClass((TypeAttr.GenerationType) val.type());
            core.append("""
                        public static final %s %s = new %s(%s);
                    """.formatted(typeName, val.name(), typeName, val.value()));
        }
        writer.write(packages, """
                public class %s {
                %s
                }
                """.formatted(packages.getClassName(), core.toString()));
        return new GenerateResult(List.of(packages), List.of());
    }

    public static record ConstValue(TypeAttr.TypeRefer type, String value, String name) {

    }
}
