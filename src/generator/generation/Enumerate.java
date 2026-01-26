package generator.generation;

import generator.Dependency;
import generator.Generators;
import generator.PackagePath;
import generator.generation.generator.EnumGenerator;
import generator.types.CommonTypes;
import generator.types.EnumType;
import generator.types.TypeImports;


public final class Enumerate extends AbstractGeneration<EnumType> {
    public Enumerate(PackagePath packagePath, EnumType type) {
        super(packagePath, type);
    }

    @Override
    public TypeImports getDefineImportTypes() {
        return super.getDefineImportTypes()
                .addUseImports(CommonTypes.SpecificTypes.FunctionUtils)
                .addUseImports(CommonTypes.ValueInterface.I64I)
                .addUseImports(typePkg.type().getType().getOperations().getValue());
    }

    @Override
    public void generate(Dependency dependency, Generators.Writer writer) {
        new EnumGenerator(this, dependency, writer).generate();
    }
}
