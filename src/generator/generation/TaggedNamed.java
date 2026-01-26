package generator.generation;

import generator.Dependency;
import generator.Generators;
import generator.PackagePath;
import generator.types.CommonTypes;
import generator.types.TaggedNamedType;
import generator.types.TypeImports;

public final class TaggedNamed extends AbstractGeneration<TaggedNamedType> {
    public TaggedNamed(PackagePath packagePath, TaggedNamedType type) {
        super(packagePath, type);
    }

    @Override
    public TypeImports getDefineImportTypes() {
        return super.getDefineImportTypes().addUseImports(CommonTypes.BasicOperations.Info);
    }

    @Override
    public void generate(Dependency dependency, Generators.Writer writer) {
//        new RefOnlyGenerator(this, dependency, writer).generate();
    }
}
