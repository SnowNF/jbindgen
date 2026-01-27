package generator.generation.generator;

import generator.PackageManager;
import generator.types.CommonTypes;
import generator.types.FunctionPtrType;
import generator.types.MemoryLayouts;
import generator.types.TypeAttr;
import generator.types.operations.CommonOperation;
import generator.types.operations.OperationAttr;

import java.util.ArrayList;
import java.util.List;

import static generator.generation.generator.FuncPtrUtils.SEGMENT_ALLOCATOR_PARAMETER_NAME;
import static generator.generation.generator.FuncPtrUtils.arenaAutoAllocator;
import static generator.types.CommonTypes.FFMTypes.FUNCTION_DESCRIPTOR;
import static generator.types.CommonTypes.FFMTypes.SEGMENT_ALLOCATOR;

public class FunctionRawUtils {
    private final PackageManager packages;
    private final FunctionPtrType function;

    public FunctionRawUtils(PackageManager packages, FunctionPtrType func) {
        this.packages = packages;
        this.function = func;
    }

    public String getFunctionName() {
        return function.typeName(TypeAttr.NameType.RAW);
    }

    public String rawRetType() {
        return function.getReturnType().map(operationType ->
                        operationType.getOperation().getFuncOperation().getPrimitiveType().useType(packages))
                .orElse("void");
    }


    public String funcDescriptor() {
        List<String> memoryLayout = new ArrayList<>();
        for (MemoryLayouts l : function.getMemoryLayouts()) {
            packages.addImport(l.getTypeImports());
            memoryLayout.add(l.getMemoryLayout());
        }
        var str = String.join(", ", memoryLayout);
        return packages.useClass(FUNCTION_DESCRIPTOR)
               + (function.getReturnType().isPresent()
                ? ".of(%s)"
                : ".ofVoid(%s)").formatted(str);
    }

    public String rawReturnCast() {
        if (function.getReturnType().isEmpty())
            return "";
        TypeAttr.OperationType operationType = function.getReturnType().get();
        CommonTypes.Primitives primitiveType = operationType.getOperation().getFuncOperation().getPrimitiveType();
        return "return (%s) ".formatted(primitiveType.useType(packages));
    }

    public String rawDowncallStr() {
        List<String> para = new ArrayList<>();
        if (function.allocatorType() == CommonOperation.AllocatorType.STANDARD) {
            para.add(SEGMENT_ALLOCATOR_PARAMETER_NAME);
        }
        if (function.allocatorType() == CommonOperation.AllocatorType.ON_HEAP) {
            para.add(arenaAutoAllocator(packages));
        }
        para.addAll(function.getArgs().stream().map(FunctionPtrType.Arg::argName).toList());
        return String.join(", ", para);
    }

    private void commonInvokeParaStr(List<String> out) {
        for (FunctionPtrType.Arg arg : function.getArgs()) {
            OperationAttr.Operation operation = ((TypeAttr.OperationType) arg.type()).getOperation();
            CommonTypes.Primitives primitiveType = operation.getFuncOperation().getPrimitiveType();
            String p = primitiveType.useType(packages) + " " + arg.argName();
            out.add(p);
        }
    }

    public String rawDowncallPara() {
        List<String> out = new ArrayList<>();
        if (function.allocatorType() == CommonOperation.AllocatorType.STANDARD) {
            out.add(packages.useClass(SEGMENT_ALLOCATOR) + " " + SEGMENT_ALLOCATOR_PARAMETER_NAME);
        }
        commonInvokeParaStr(out);
        return String.join(", ", out);
    }

    public String rawUpcallPara() {
        List<String> out = new ArrayList<>();
        commonInvokeParaStr(out);
        return String.join(", ", out);
    }
}
