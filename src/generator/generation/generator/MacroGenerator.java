package generator.generation.generator;

import generator.Dependency;
import generator.Generators;
import generator.PackagePath;
import generator.generation.Macros;

import java.util.Set;

public class MacroGenerator implements Generator {
    private final PackagePath packagePath;

    private final Set<Macros.Macro> macros;
    private final Generators.Writer writer;

    public MacroGenerator(PackagePath packagePath, Set<Macros.Macro> macros, Dependency dependency, Generators.Writer writer) {
        this.packagePath = packagePath;
        this.macros = macros;
        this.writer = writer;
    }

    @Override
    public void generate() {
        StringBuilder core = new StringBuilder();
        for (var macro : macros) {
            switch (macro) {
                case Macros.Primitive p -> core.append("""
                            public static final %s %s = %s; // %s
                        """.formatted(p.primitives().getPrimitiveTypeName(), p.declName(), p.initializer(), p.comment()));
                case Macros.StrMacro s -> core.append("""
                            public static final String %s = %s; // %s
                        """.formatted(s.declName(), s.initializer(), s.comment()));
            }

        }
        String out = packagePath.makePackage();
        out += """
                public class %s {
                %s
                }
                """.formatted(packagePath.getClassName(), core.toString());
        writer.write(packagePath, out);
    }
}
