package generator.generation;

import generator.Dependency;
import generator.Generators;
import generator.PackagePath;
import generator.TypePkg;
import generator.types.TaggedNamedType;
import generator.types.TypeImports;

import java.util.Set;

public final class TaggedNames implements Generation<TaggedNamedType> {
    public TaggedNames(PackagePath packagePath, TaggedNamedType type) {
    }

    @Override
    public Set<TypePkg<? extends TaggedNamedType>> getImplTypes() {
        return Set.of();
    }

    @Override
    public TypeImports getDefineImportTypes() {
        return new TypeImports();
    }

    @Override
    public void generate(Dependency dependency, Generators.Writer writer) {

    }
}
