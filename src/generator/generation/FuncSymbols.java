package generator.generation;

import generator.Dependency;
import generator.PackagePath;
import generator.TypePkg;
import generator.generation.generator.FuncSymbolGenerator;
import generator.types.*;
import generator.types.operations.CommonOperation;
import generator.types.operations.OperationAttr;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.Objects;
import java.util.Set;


/**
 * exported function symbol, use {@link Linker#downcallHandle(MemorySegment, FunctionDescriptor, Linker.Option...)} to import symbol
 */
public final class FuncSymbols implements Generation<FunctionPtrType> {
    private final List<TypePkg<FunctionPtrType>> functions;
    private final PackagePath packagePath;
    private final SymbolProviderType symbolProvider;

    public FuncSymbols(PackagePath packagePath, List<FunctionPtrType> functions, SymbolProviderType symbolProvider) {
        this.packagePath = packagePath.reqClassName();
        this.symbolProvider = symbolProvider;
        this.functions = functions.stream().map(functionType ->
                new TypePkg<>(functionType, packagePath.removeEnd().add(packagePath.getClassName()).end(functionType.typeName(TypeAttr.NameType.GENERIC))
                )).toList();
    }

    public List<TypePkg<FunctionPtrType>> getFunctions() {
        return functions;
    }

    public PackagePath getPackagePath() {
        return packagePath;
    }

    @Override
    public TypeImports getDefineImportTypes() {
        TypeImports imports = symbolProvider.getUseImportTypes();
        for (var fun : functions) {
            FunctionPtrType function = fun.type();
            if (function.allocatorType() == CommonOperation.AllocatorType.ON_HEAP) {
                imports.addUseImports(CommonTypes.FFMTypes.ARENA);
            }
            if (function.allocatorType() == CommonOperation.AllocatorType.STANDARD) {
                imports.addUseImports(CommonTypes.FFMTypes.ARENA);
                imports.addUseImports(CommonTypes.FFMTypes.SEGMENT_ALLOCATOR);
                imports.addUseImports(CommonTypes.BindTypes.Ptr);
            }
            // imports for memory layout
            for (MemoryLayouts memoryLayout : function.getMemoryLayouts()) {
                imports.addImport(memoryLayout.getTypeImports());
            }
            // imports for raw java signature
            for (TypeAttr.TypeRefer type : function.getFunctionSignatureTypes()) {
                OperationAttr.Operation operation = ((TypeAttr.OperationType) type).getOperation();
                operation.getFuncOperation().getPrimitiveType().getExtraPrimitiveImportType().ifPresent(imports::addUseImports);
            }
            // imports for construct return type
            function.getReturnType().ifPresent(type -> {
                OperationAttr.Operation operation = type.getOperation();
                imports.addImport(operation.getFuncOperation().constructFromRet("").imports());
            });
            // imports for destruct upper parameters
            for (FunctionPtrType.Arg arg : function.getArgs()) {
                CommonOperation.UpperType upperType = ((TypeAttr.OperationType) arg.type()).getOperation().getCommonOperation().getUpperType();
                imports.addImport(upperType.typeImports());
                imports.addImport(upperType.typeOp().getOperation().getFuncOperation().destructToPara("").imports());
            }
        }
        return imports.addUseImports(CommonTypes.SpecificTypes.FunctionUtils)
                .addUseImports(CommonTypes.FFMTypes.METHOD_HANDLE)
                .addUseImports(CommonTypes.FFMTypes.FUNCTION_DESCRIPTOR);
    }

    @Override
    public void generate(Dependency dependency) {
        new FuncSymbolGenerator(this, dependency, symbolProvider).generate();
    }

    @Override
    public Set<TypePkg<? extends FunctionPtrType>> getImplTypes() {
        return Set.of();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FuncSymbols that)) return false;
        return Objects.equals(functions, that.functions) && Objects.equals(packagePath, that.packagePath) && Objects.equals(symbolProvider, that.symbolProvider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(functions, packagePath, symbolProvider);
    }
}
