package generator.generators;

import generator.Generators;
import generator.PackageManager;
import generator.PackagePath;
import generator.types.CommonTypes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MacroGenerator implements Generator {
    private final PackagePath dest;
    private final Set<Macro> macros;

    public MacroGenerator(PackagePath dest, Set<Macro> macros) {
        this.dest = dest;
        this.macros = new HashSet<>(macros);
    }

    @Override
    public GenerateResult generate(Generators.GenerationProvider locations, Generators.Writer writer) {
        var packages = new PackageManager(locations, dest);
        StringBuilder core = new StringBuilder();
        for (var macro : macros) {
            switch (macro) {
                case Macro.Primitive p -> core.append("""
                            public static final %s %s = %s; // %s
                        """.formatted(p.primitives().useType(packages), p.declName(), p.initializer(), p.comment()));
                case Macro.String s -> core.append("""
                            public static final String %s = %s; // %s
                        """.formatted(s.declName(), s.initializer(), s.comment()));
            }

        }
        writer.write(packages, """
                public class %s {
                %s
                }
                """.formatted(packages.getClassName(), core.toString()));
        return new GenerateResult(List.of(packages), List.of());
    }

    public sealed interface Macro {
        record Primitive(CommonTypes.Primitives primitives, java.lang.String declName,
                         java.lang.String initializer, java.lang.String comment) implements Macro {
        }
        record String(java.lang.String declName, java.lang.String initializer, java.lang.String comment) implements Macro {
        }
    }
}
