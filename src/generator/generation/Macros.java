package generator.generation;


import generator.Dependency;
import generator.Generators;
import generator.PackagePath;
import generator.TypePkg;
import generator.generation.generator.MacroGenerator;
import generator.types.CommonTypes;
import generator.types.TypeAttr;
import generator.types.TypeImports;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class Macros implements Generation<TypeAttr.GenerationType> {
    public sealed interface Macro {
    }

    public record Primitive(CommonTypes.Primitives primitives, String declName,
                            String initializer, String comment) implements Macro {
    }

    public record StrMacro(String declName, String initializer, String comment) implements Macro {
    }

    private final PackagePath packagePath;
    private final Set<Macro> macros;

    public Macros(PackagePath packagePath, HashSet<Macro> macros) {
        this.packagePath = packagePath;
        this.macros = Set.copyOf(macros);
    }

    @Override
    public Set<TypePkg<? extends TypeAttr.GenerationType>> getImplTypes() {
        return Set.of();
    }

    @Override
    public TypeImports getDefineImportTypes() {
        return new TypeImports();
    }

    @Override
    public void generate(Dependency dependency, Generators.Writer writer) {
        new MacroGenerator(packagePath, macros, dependency, writer).generate();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Macros macros1)) return false;
        return Objects.equals(packagePath, macros1.packagePath) && Objects.equals(macros, macros1.macros);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packagePath, macros);
    }
}
