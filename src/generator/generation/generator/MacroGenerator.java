package generator.generation.generator;

import generator.Generators;
import generator.PackageManager;
import generator.PackagePath;
import generator.generation.GenerationMacro;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MacroGenerator implements Generator {
    private final PackagePath dest;
    private final Set<GenerationMacro> macros;

    public MacroGenerator(PackagePath dest, Set<GenerationMacro> macros) {
        this.dest = dest;
        this.macros = new HashSet<>(macros);
    }

    @Override
    public GenerateResult generate(Generators.GenerationProvider locations, Generators.Writer writer) {
        var packages = new PackageManager(locations, dest);
        StringBuilder core = new StringBuilder();
        for (var macro : macros) {
            switch (macro) {
                case GenerationMacro.Primitive p -> core.append("""
                            public static final %s %s = %s; // %s
                        """.formatted(p.primitives().useType(packages), p.declName(), p.initializer(), p.comment()));
                case GenerationMacro.StrMacro s -> core.append("""
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
}
