package generator.generation;

import generator.Dependency;
import generator.PackagePath;
import generator.generation.generator.FuncProtocolGenerator;
import generator.types.*;
import generator.types.operations.CommonOperation;
import generator.types.operations.OperationAttr;

/**
 * used to generate function pointer, normally used in callback ptr
 */
public final class FuncPointer extends AbstractGeneration<FunctionPtrType> {
    public FuncPointer(PackagePath packagePath, FunctionPtrType type) {
        super(packagePath, type);
    }

    @Override
    public TypeImports getDefineImportTypes() {
        TypeImports imports = super.getDefineImportTypes();
        FunctionPtrType function = typePkg.type();
        // we need implement invokeRaw() and invoke()
        if (function.allocatorType() == CommonOperation.AllocatorType.ON_HEAP) {
            imports.addUseImports(CommonTypes.SpecificTypes.MemoryUtils);
        }
        if (function.allocatorType() == CommonOperation.AllocatorType.STANDARD) {
            imports.addUseImports(CommonTypes.SpecificTypes.MemoryUtils);
            imports.addUseImports(CommonTypes.FFMTypes.ARENA);
            imports.addUseImports(CommonTypes.FFMTypes.SEGMENT_ALLOCATOR);
            imports.addUseImports(CommonTypes.BindTypes.Ptr);
        }
        for (MemoryLayouts memoryLayout : function.getMemoryLayouts()) {
            imports.addImport(memoryLayout.getTypeImports());
        }
        for (TypeAttr.TypeRefer type : function.getFunctionSignatureTypes()) {
            // destruct for downcall & construct for upcall
            OperationAttr.Operation operation = ((TypeAttr.OperationType) type).getOperation();
            imports.addImport(operation.getFuncOperation().constructFromRet("").imports());
            imports.addImport(operation.getFuncOperation().destructToPara("").imports());

            // raw java signature
            operation.getFuncOperation().getPrimitiveType().getExtraPrimitiveImportType().ifPresent(imports::addUseImports);

            // destruct upper parameters and upper return type
            CommonOperation.UpperType upperType = operation.getCommonOperation().getUpperType();
            imports.addImport(upperType.typeImports());
            imports.addImport(upperType.typeOp().getOperation().getFuncOperation().destructToPara("").imports());
        }
        imports.addUseImports(CommonTypes.SpecificTypes.FunctionUtils);
        imports.addUseImports(CommonTypes.BasicOperations.Info);
        imports.addUseImports(CommonTypes.BindTypeOperations.PtrOp);
        imports.addUseImports(CommonTypes.FFMTypes.FUNCTION_DESCRIPTOR);
        imports.addUseImports(CommonTypes.FFMTypes.ARENA);
        imports.addUseImports(CommonTypes.FFMTypes.METHOD_HANDLE);
        imports.addUseImports(CommonTypes.FFMTypes.METHOD_HANDLES);
        imports.addUseImports(CommonTypes.FFMTypes.MEMORY_SEGMENT);
        return imports;
    }

    @Override
    public void generate(Dependency dependency) {
        new FuncProtocolGenerator(this, dependency).generate();
    }
}
