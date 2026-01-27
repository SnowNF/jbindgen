package generator.generation;

import generator.Dependency;
import generator.Generators;
import generator.PackagePath;
import generator.TypePkg;
import generator.types.TaggedNamedType;
import generator.types.TypeImports;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public final class TaggedNames implements Generation<TaggedNamedType> {
    private final PackagePath packagePath;
    private final HashSet<TaggedNamedType> type;

    public TaggedNames(PackagePath packagePath, ArrayList<TaggedNamedType> type) {
        this.packagePath = packagePath;
        this.type = new HashSet<>(type);
    }

    @Override
    public Set<TypePkg<? extends TaggedNamedType>> getImplTypes() {
        HashSet<TypePkg<? extends TaggedNamedType>> typePkgs = new HashSet<>();
        for (TaggedNamedType taggedNamedType : type) {
            typePkgs.add(new TypePkg<>(taggedNamedType, packagePath.open().close(taggedNamedType.typeName())));
        }
        return typePkgs;
    }

    @Override
    public TypeImports getDefineImportTypes() {
        return new TypeImports();
    }

    @Override
    public void generate(Dependency dependency, Generators.Writer writer) {

    }
}
