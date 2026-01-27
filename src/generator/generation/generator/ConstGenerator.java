package generator.generation.generator;

import generator.Dependency;
import generator.Generators;
import generator.PackageManager;
import generator.PackagePath;
import generator.generation.ConstValues;
import generator.types.TypeAttr;

import java.util.List;

public class ConstGenerator implements Generator {
    private final PackageManager packages;
    private final List<ConstValues.Value> values;
    private final Generators.Writer writer;

    public ConstGenerator(ConstValues constValues, PackagePath path,
                          List<ConstValues.Value> values, Dependency dependency, Generators.Writer writer) {
        this.packages = new PackageManager(dependency, path);
        this.values = values;
        this.writer = writer;
    }

    @Override
    public void generate() {
        if (values.isEmpty()) return;
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
                """.formatted(packages.getCurrentClass(), core.toString()));
    }
}
