package generator.generation.generator;

import generator.types.CommonTypes;
import generator.types.FunctionPtrType;
import generator.types.TypeAttr;
import generator.types.operations.CommonOperation;
import generator.types.operations.CommonOperation.AllocatorType;
import generator.types.operations.OperationAttr;
import utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static generator.generation.generator.FuncPtrUtils.SEGMENT_ALLOCATOR_PARAMETER_NAME;
import static generator.generation.generator.FuncPtrUtils.arenaAutoAllocator;
import static generator.types.CommonTypes.FFMTypes.SEGMENT_ALLOCATOR;

public class FunctionWrapUtils {
    private final FunctionPtrType function;
    private final AllocatorType allocatorType;

    public FunctionWrapUtils(FunctionPtrType function) {
        this.function = function;
        allocatorType = function.allocatorType();
    }

    public String getFunctionName() {
        return Generator.getTypeName(function);
    }

    public String downcallRetType() {
        if (function.getReturnType().isEmpty()) {
            return "void";
        }
        TypeAttr.OperationType retType = function.getReturnType().get();
        var r = Generator.getTypeName((TypeAttr.TypeRefer) retType);
        if (allocatorType == AllocatorType.STANDARD) {
            // warp with Ptr<T>
            return CommonTypes.BindTypes.makePtrGenericName(r);
        }
        return r;
    }

    public String upcallUpperRetType() {
        if (function.getReturnType().isEmpty()) {
            return "void";
        }
        TypeAttr.OperationType retType = function.getReturnType().get();
        var operation = retType.getOperation();
        CommonOperation.UpperType upperType = operation.getCommonOperation().getUpperType();
        return upperType.typeName(TypeAttr.NameType.WILDCARD);
    }

    public String downcallUpperPara() {
        List<String> out = new ArrayList<>();
        if (allocatorType == AllocatorType.STANDARD) {
            out.add(SEGMENT_ALLOCATOR.typeName(TypeAttr.NameType.RAW) + " " + SEGMENT_ALLOCATOR_PARAMETER_NAME);
        }
        for (FunctionPtrType.Arg arg : function.getArgs()) {
            var operation = ((TypeAttr.OperationType) arg.type()).getOperation();
            CommonOperation.UpperType upperType = operation.getCommonOperation().getUpperType();
            String typeName = upperType.typeName(TypeAttr.NameType.WILDCARD);
            out.add(typeName + " " + arg.argName());
        }
        return String.join(", ", out);
    }

    public String upcallPara() {
        List<String> out = new ArrayList<>();
        for (FunctionPtrType.Arg arg : function.getArgs()) {
            out.add(Generator.getTypeName(arg.type()) + " " + arg.argName());
        }
        return String.join(", ", out);
    }

    public String upcallParaNames() {
        List<String> out = new ArrayList<>();
        for (FunctionPtrType.Arg arg : function.getArgs()) {
            out.add(arg.argName());
        }
        return String.join(", ", out);
    }

    public String upcallParaConstruct() {
        List<String> para = new ArrayList<>();
        for (FunctionPtrType.Arg a : function.getArgs()) {
            TypeAttr.OperationType type = (TypeAttr.OperationType) a.type();
            String destruct = type.getOperation().getFuncOperation().constructFromRet(a.argName()).codeSegment();
            para.add(destruct);
        }
        return String.join(", ", para);
    }

    public String downcallUpperParaDestruct() {
        List<String> out = new ArrayList<>();
        if (allocatorType == AllocatorType.STANDARD) {
            out.add(SEGMENT_ALLOCATOR_PARAMETER_NAME);
        }
        for (FunctionPtrType.Arg a : function.getArgs()) {
            TypeAttr.OperationType op = (TypeAttr.OperationType) a.type();
            TypeAttr.OperationType upperType = op.getOperation().getCommonOperation().getUpperType().typeOp();
            String destruct = upperType.getOperation().getFuncOperation().destructToPara(a.argName()).codeSegment();
            out.add(destruct);
        }
        return String.join(", ", out);
    }

    public String downcallTypeReturn(String value) {
        if (function.getReturnType().isEmpty()) {
            // no return, just keep content
            return value;
        }
        TypeAttr.OperationType retType = function.getReturnType().get();
        OperationAttr.Operation operation = retType.getOperation();
        if (allocatorType == AllocatorType.STANDARD) {
            var r = Generator.getTypeName((TypeAttr.TypeRefer) retType);
            // warp Ptr<T>
            return "return new " + CommonTypes.BindTypes.makePtrGenericName(r) + "(%s, %s)"
                    .formatted(value, operation.getCommonOperation().makeOperation().str());
        }
        return "return " + operation.getFuncOperation().constructFromRet(value).codeSegment();
    }

    public String upcallUpperRetTypeDestruct(String value) {
        if (function.getReturnType().isEmpty()) {
            return value;
        }
        TypeAttr.OperationType retType = function.getReturnType().get();
        TypeAttr.OperationType upperType = retType.getOperation().getCommonOperation().getUpperType().typeOp();
        return upperType.getOperation().getFuncOperation().destructToPara(value).codeSegment();
    }

    public Optional<OnHeapReturnVariant> hasOnHeapReturnVariant() {
        if (allocatorType == AllocatorType.STANDARD) {
            return Optional.of(new OnHeapReturnVariant(this));
        }
        return Optional.empty();
    }

    public static class OnHeapReturnVariant {
        private final FunctionWrapUtils wrap;
        private final FunctionPtrType function;
        private final TypeAttr.OperationType retType;

        private OnHeapReturnVariant(FunctionWrapUtils function) {
            CommonUtils.Assert(function.allocatorType == AllocatorType.STANDARD);
            this.wrap = function;
            this.function = function.function;
            this.retType = function.function.getReturnType().orElseThrow();
        }

        public String getFunctionName() {
            return wrap.getFunctionName();
        }

        public String downcallRetType() {
            return Generator.getTypeName((TypeAttr.TypeRefer) retType);
        }

        public String downcallUpperPara() {
            List<String> out = new ArrayList<>();
            for (FunctionPtrType.Arg arg : function.getArgs()) {
                var operation = ((TypeAttr.OperationType) arg.type()).getOperation();
                CommonOperation.UpperType upperType = operation.getCommonOperation().getUpperType();
                String typeName = upperType.typeName(TypeAttr.NameType.WILDCARD);
                out.add(typeName + " " + arg.argName());
            }
            return String.join(", ", out);
        }

        public String downcallUpperParaDestruct() {
            List<String> out = new ArrayList<>();
            out.add(arenaAutoAllocator());
            for (FunctionPtrType.Arg a : function.getArgs()) {
                TypeAttr.OperationType upperType = (TypeAttr.OperationType) a.type();
                TypeAttr.OperationType type = upperType.getOperation().getCommonOperation().getUpperType().typeOp();
                String destruct = type.getOperation().getFuncOperation().destructToPara(a.argName()).codeSegment();
                out.add(destruct);
            }
            return String.join(", ", out);
        }

        public String downcallTypeReturn(String value) {
            OperationAttr.Operation operation = retType.getOperation();
            return "return " + operation.getFuncOperation().constructFromRet(value).codeSegment();
        }
    }
}
