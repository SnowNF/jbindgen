package generator.generation;

import generator.Dependency;
import generator.Generators;
import generator.PackagePath;
import generator.TypePkg;
import generator.types.TypeAttr;
import generator.types.TypeImports;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.Set;

/**
 * exported variable symbol, use {@link Linker#downcallHandle(MemorySegment, FunctionDescriptor, Linker.Option...)} to import symbolL
 */
public final class VarSymbols implements Generation<TypeAttr.GenerationType> {
    private final List<TypeAttr.TypeRefer> normalTypes;

    public VarSymbols(PackagePath packagePath, List<TypeAttr.TypeRefer> normalTypes) {
        this.normalTypes = normalTypes;
    }

    @Override
    public Set<TypePkg<? extends TypeAttr.GenerationType>> getImplTypes() {
        return Set.of();
    }

    @Override
    public TypeImports getDefineImportTypes() {
        return new TypeImports().addUseImports(normalTypes);
    }

    @Override
    public void generate(Dependency dependency, Generators.Writer writer) {
        System.err.println("todo: generate this");
    }
}
