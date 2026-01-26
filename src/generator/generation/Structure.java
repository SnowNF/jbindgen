package generator.generation;

import generator.Dependency;
import generator.Generators;
import generator.PackagePath;
import generator.generation.generator.StructGenerator;
import generator.types.CommonTypes;
import generator.types.StructType;
import generator.types.TypeAttr;
import generator.types.TypeImports;
import generator.types.operations.MemoryOperation;
import generator.types.operations.OperationAttr;

public final class Structure extends AbstractGeneration<StructType> {
    public Structure(PackagePath packagePath, StructType type) {
        super(packagePath, type);
    }

    @Override
    public TypeImports getDefineImportTypes() {
        TypeImports imports = super.getDefineImportTypes().addUseImports(CommonTypes.FFMTypes.MEMORY_SEGMENT)
                .addUseImports(CommonTypes.FFMTypes.SEGMENT_ALLOCATOR)
                .addUseImports(CommonTypes.ValueInterface.I64I);
        for (StructType.Member member : getTypePkg().type().getMembers()) {
            OperationAttr.Operation operation = ((TypeAttr.OperationType) member.type()).getOperation();
            MemoryOperation memoryOperation = operation.getMemoryOperation();
            if (member.bitField()) {
                memoryOperation.setterBitfield("", 0, 0, "").ifPresent(getter -> {
                    imports.addImport(getter.imports());
                });
                memoryOperation.getterBitfield("", 0, 0).ifPresent(getter -> {
                    imports.addImport(getter.imports());
                });
            }
            imports.addImport(memoryOperation.setter("", 0, "").imports());
            imports.addImport(memoryOperation.getter("", 0).imports());
        }
        imports.addImport(typePkg.type().getMemoryLayout().getTypeImports());
        return imports;
    }

    @Override
    public void generate(Dependency dependency, Generators.Writer writer) {
        new StructGenerator(this, dependency, writer).generate();
    }
}
