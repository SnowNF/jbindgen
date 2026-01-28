package generator.generation.generator;

import generator.PackageManager;
import generator.types.CommonTypes;
import generator.types.FunctionPtrType;
import generator.types.TypeAttr;
import generator.types.operations.CommonOperation;
import generator.types.operations.CommonOperation.AllocatorType;
import generator.types.operations.FuncOperation;
import generator.types.operations.OperationAttr;
import utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static generator.generation.generator.FuncPtrUtils.SEGMENT_ALLOCATOR_PARAMETER_NAME;
import static generator.generation.generator.FuncPtrUtils.arenaAutoAllocator;
import static generator.types.CommonTypes.FFMTypes.SEGMENT_ALLOCATOR;

public class FunctionWrapUtils {
    private final PackageManager packages;
    private final FunctionPtrType function;
    private final AllocatorType allocatorType;

    public FunctionWrapUtils(PackageManager packages, FunctionPtrType function) {
        this.packages = packages;
        this.function = function;
        allocatorType = function.allocatorType();
    }

    public String getFunctionName() {
        return function.typeName();
    }

    public String downcallRetType() {
        if (function.getReturnType().isEmpty()) {
            return "void";
        }
        TypeAttr.OperationType retType = function.getReturnType().get();
        var r = packages.useType((TypeAttr.GenerationType) retType, TypeAttr.NameType.GENERIC);
        if (allocatorType == AllocatorType.STANDARD) {
            // warp with Ptr<T>
            return packages.useTypePrefix(CommonTypes.BindTypes.Ptr) + CommonTypes.BindTypes.makePtrGenericName(r);
        }
        return r;
    }

    public String upcallUpperRetType() {
        if (function.getReturnType().isEmpty()) {
            return "void";
        }
        TypeAttr.OperationType retType = function.getReturnType().get();
        var operation = retType.getOperation();
        CommonOperation.UpperType upperType = operation.getCommonOperation().getUpperType(packages);
        return upperType.typeName(packages, TypeAttr.NameType.WILDCARD);
    }

    public String downcallUpperPara() {
        List<String> out = new ArrayList<>();
        if (allocatorType == AllocatorType.STANDARD) {
            out.add(packages.useClass(SEGMENT_ALLOCATOR) + " " + SEGMENT_ALLOCATOR_PARAMETER_NAME);
        }
        for (FunctionPtrType.Arg arg : function.getArgs()) {
            var operation = ((TypeAttr.OperationType) arg.type()).getOperation();
            CommonOperation.UpperType upperType = operation.getCommonOperation().getUpperType(packages);
            String typeName = upperType.typeName(packages, TypeAttr.NameType.WILDCARD);
            out.add(typeName + " " + arg.argName());
        }
        return String.join(", ", out);
    }

    public String upcallPara() {
        List<String> out = new ArrayList<>();
        for (FunctionPtrType.Arg arg : function.getArgs()) {
            out.add(packages.useClass((TypeAttr.GenerationType) arg.type()) + " " + arg.argName());
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
            FuncOperation.Result construct = type.getOperation().getFuncOperation(packages).constructFromRet(a.argName());
            String destruct = construct.codeSegment();
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
            TypeAttr.OperationType upperType = op.getOperation().getCommonOperation().getUpperType(packages).type();
            var destruct = upperType.getOperation().getFuncOperation(packages).destructToPara(a.argName());
            out.add(destruct.codeSegment());
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
            String r = packages.useClass((TypeAttr.GenerationType) retType);
            // warp Ptr<T>
            return "return new " + packages.useTypePrefix(CommonTypes.BindTypes.Ptr) + CommonTypes.BindTypes.makePtrGenericName(r)
                   + "(%s, %s)".formatted(value, operation.getCommonOperation().makeOperation(packages).str());
        }
        FuncOperation.Result construct = operation.getFuncOperation(packages).constructFromRet(value);
        return "return " + construct.codeSegment();
    }

    public String upcallUpperRetTypeDestruct(String value) {
        if (function.getReturnType().isEmpty()) {
            return value;
        }
        TypeAttr.OperationType retType = function.getReturnType().get();
        TypeAttr.OperationType upperType = retType.getOperation().getCommonOperation().getUpperType(packages).type();
        FuncOperation.Result destruct = upperType.getOperation().getFuncOperation(packages).destructToPara(value);
        return destruct.codeSegment();
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
            return wrap.packages.useClass((TypeAttr.GenerationType) retType);
        }

        public String downcallUpperPara() {
            List<String> out = new ArrayList<>();
            for (FunctionPtrType.Arg arg : function.getArgs()) {
                var operation = ((TypeAttr.OperationType) arg.type()).getOperation();
                CommonOperation.UpperType upperType = operation.getCommonOperation().getUpperType(wrap.packages);
                String typeName = upperType.typeName(wrap.packages, TypeAttr.NameType.WILDCARD);
                out.add(typeName + " " + arg.argName());
            }
            return String.join(", ", out);
        }

        public String downcallUpperParaDestruct() {
            List<String> out = new ArrayList<>();
            out.add(arenaAutoAllocator(wrap.packages));
            for (FunctionPtrType.Arg a : function.getArgs()) {
                TypeAttr.OperationType upperType = (TypeAttr.OperationType) a.type();
                TypeAttr.OperationType type = upperType.getOperation().getCommonOperation().getUpperType(wrap.packages).type();
                FuncOperation.Result destruct = type.getOperation().getFuncOperation(wrap.packages).destructToPara(a.argName());
                out.add(destruct.codeSegment());
            }
            return String.join(", ", out);
        }

        public String downcallTypeReturn(String value) {
            OperationAttr.Operation operation = retType.getOperation();
            FuncOperation.Result construct = operation.getFuncOperation(wrap.packages).constructFromRet(value);
            return "return " + construct.codeSegment();
        }
    }
}
