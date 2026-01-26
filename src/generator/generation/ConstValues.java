package generator.generation;

import generator.Dependency;
import generator.Generators;
import generator.PackagePath;
import generator.TypePkg;
import generator.generation.generator.ConstGenerator;
import generator.types.TypeAttr;
import generator.types.TypeImports;
import generator.types.operations.OperationAttr;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static utils.CommonUtils.Assert;

/**
 * const value like const int XXX
 */
public final class ConstValues implements Generation<TypeAttr.GenerationType> {
    private final PackagePath path;
    private final List<Value> values;

    public record Value(TypeAttr.TypeRefer type, String value, String name) {

    }

    public ConstValues(PackagePath path, List<Value> values) {
        this.path = path;
        this.values = values;
        for (var value : values) {
            Assert(value.type instanceof TypeAttr.OperationType operationType
                   && operationType.getOperation() instanceof OperationAttr.ValueBasedOperation,
                    "type must be ValueBased");
        }
    }

    @Override
    public TypeImports getDefineImportTypes() {
        TypeImports imports = new TypeImports();
        for (Value value : values) {
            imports.addUseImports(value.type);
        }
        return imports;
    }

    @Override
    public void generate(Dependency dependency, Generators.Writer writer) {
        new ConstGenerator(this, path, values, dependency, writer).generate();
    }

    @Override
    public Set<TypePkg<? extends TypeAttr.GenerationType>> getImplTypes() {
        return Set.of();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ConstValues that)) return false;
        return Objects.equals(path, that.path) && Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, values);
    }
}
