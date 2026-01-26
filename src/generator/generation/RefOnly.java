package generator.generation;

import generator.Dependency;
import generator.Generators;
import generator.PackagePath;
import generator.generation.generator.RefOnlyGenerator;
import generator.types.CommonTypes;
import generator.types.RefOnlyType;
import generator.types.TypeImports;

public final class RefOnly extends AbstractGeneration<RefOnlyType> {
    public RefOnly(PackagePath packagePath, RefOnlyType type) {
        super(packagePath, type);
    }

    @Override
    public TypeImports getDefineImportTypes() {
        return super.getDefineImportTypes().addUseImports(CommonTypes.BasicOperations.Info);
    }

    @Override
    public void generate(Dependency dependency, Generators.Writer writer) {
        new RefOnlyGenerator(this, dependency, writer).generate();
    }
}
