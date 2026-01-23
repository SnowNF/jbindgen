package generator.generation.generator;

import generator.types.*;
import generator.types.operations.CommonOperation.AllocatorType;
import utils.ConflictNameUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static utils.CommonUtils.Assert;

public class FuncPtrUtils {

    public static final String SEGMENT_ALLOCATOR_PARAMETER_NAME = CommonTypes.FFMTypes.SEGMENT_ALLOCATOR.typeName(TypeAttr.NameType.RAW).toLowerCase();

    private FuncPtrUtils() {
        throw new IllegalStateException("Utility class");
    }

    static String makeFuncDescriptor(FunctionPtrType function) {
        List<String> memoryLayout = function.getMemoryLayouts().stream().map(MemoryLayouts::getMemoryLayout).toList();
        var str = String.join(", ", memoryLayout);
        return (function.getReturnType().isPresent()
                ? "FunctionDescriptor.of(%s)"
                : "FunctionDescriptor.ofVoid(%s)").formatted(str);
    }

    static String makeRawRetType(FunctionPtrType function) {
        return function.getReturnType().map(operationType ->
                        operationType.getOperation().getFuncOperation().getPrimitiveType().getPrimitiveTypeName())
                .orElse("void");
    }

    static String makeWrappedRetType(FunctionPtrType function, AllocatorType allocatorType) {
        if (function.getReturnType().isEmpty()) {
            return "void";
        }
        TypeAttr.OperationType retType = function.getReturnType().get();
        var r = Generator.getTypeName((TypeAttr.TypeRefer) retType);
        if (allocatorType == AllocatorType.STANDARD) {
            // warp Single<T>
            return CommonTypes.SpecificTypes.Single.getGenericName(r);
        }
        return r;
    }

    static String makeWrappedStrForInvoke(String invokeStr, FunctionPtrType function, AllocatorType allocatorType) {
        if (function.getReturnType().isEmpty()) {
            return invokeStr;
        }
        if (allocatorType == AllocatorType.STANDARD) {
            TypeAttr.OperationType retType = function.getReturnType().get();
            var r = Generator.getTypeName((TypeAttr.TypeRefer) retType);
            // warp Single<T>
            return "return new " + CommonTypes.SpecificTypes.Single.getGenericName(r) + "(%s, %s)"
                    .formatted(invokeStr, retType.getOperation().getCommonOperation().makeOperation().str());
        }
        return "return " + makeWrappedRetConstruct(invokeStr, function);
    }

    static String makeWrappedRetDestruct(String paraStr, FunctionPtrType type) {
        return type.getReturnType().map(operationType -> operationType.getOperation()
                .getFuncOperation().destructToPara(paraStr).codeSegment()).orElse(paraStr);
    }

    private static String makeWrappedRetConstruct(String paraStr, FunctionPtrType type) {
        return type.getReturnType().map(operationType -> operationType.getOperation()
                .getFuncOperation().constructFromRet(paraStr).codeSegment()).orElse(paraStr);
    }

    private static String arenaAutoAllocator() {
        return "Arena.ofAuto()";
    }

    private static String makeAllocatorParaStr(FunctionPtrType function) {
        return switch (function.allocatorType()) {
            case NONE -> throw new AssertionError("Illegal allocator type");
            case STANDARD -> SEGMENT_ALLOCATOR_PARAMETER_NAME;
            case ON_HEAP -> """
                    %s.allocate(%s.memoryLayout())
                    """.formatted(arenaAutoAllocator(),
                    function.getReturnType().orElseThrow().getOperation().getCommonOperation().makeOperation().str());
        };
    }

    private static List<String> makeUpperWrappedParaDestructName(FunctionPtrType function, AllocatorType allocatorType) {
        List<String> para = new ArrayList<>();
        if (allocatorType == AllocatorType.STANDARD) {
            para.add(SEGMENT_ALLOCATOR_PARAMETER_NAME);
        }
        if (allocatorType == AllocatorType.ON_HEAP) {
            para.add(arenaAutoAllocator());
        }
        for (FunctionPtrType.Arg a : function.getArgs()) {
            TypeAttr.OperationType upperType = (TypeAttr.OperationType) a.type();
            TypeAttr.OperationType type = upperType.getOperation().getCommonOperation().getUpperType().typeOp();
            String destruct = type.getOperation().getFuncOperation().destructToPara(a.argName()).codeSegment();
            para.add(destruct);
        }
        return para;
    }

    private static List<String> makeWrappedParaConstructName(FunctionPtrType function, AllocatorType allocatorType) {
        List<String> para = new ArrayList<>();
        if (allocatorType == AllocatorType.STANDARD) {
            para.add(SEGMENT_ALLOCATOR_PARAMETER_NAME);
        }
        for (FunctionPtrType.Arg a : function.getArgs()) {
            TypeAttr.OperationType type = (TypeAttr.OperationType) a.type();
            String destruct = type.getOperation().getFuncOperation().constructFromRet(a.argName()).codeSegment();
            para.add(destruct);
        }
        return para;
    }

    static String makeRawStrBeforeInvoke(FunctionPtrType function) {
        return function.getReturnType().map(normalType -> "return (%s) ".formatted(normalType
                        .getOperation().getFuncOperation().getPrimitiveType().getPrimitiveTypeName()))
                .orElse("");
    }

    static String makeRawInvokeStr(FunctionPtrType function) {
        List<String> para = new ArrayList<>();
        if (function.allocatorType() != AllocatorType.NONE) {
            para.add(makeAllocatorParaStr(function));
        }
        para.addAll(function.getArgs().stream().map(FunctionPtrType.Arg::argName).toList());
        return String.join(", ", para);
    }

    static String makeParaNameStr(FunctionPtrType function, AllocatorType allocatorType) {
        return String.join(", ", makeParaName(function, allocatorType));
    }


    static String makeWrappedPara(FunctionPtrType function, AllocatorType allocatorType) {
        List<String> out = new ArrayList<>();
        List<String> type = makeWrappedParaType(function, allocatorType);
        List<String> para = makeParaName(function, allocatorType);
        Assert(type.size() == para.size(), "type.size() != para.size");
        for (int i = 0; i < type.size(); i++) {
            out.add(type.get(i) + " " + para.get(i));
        }
        return String.join(", ", out);
    }

    static String makeUpperWrappedPara(FunctionPtrType function, AllocatorType allocatorType) {
        List<String> out = new ArrayList<>();
        List<String> type = makeUpperWrappedParaType(function, allocatorType);
        List<String> para = makeParaName(function, allocatorType);
        Assert(type.size() == para.size(), "type.size() != para.size");
        for (int i = 0; i < type.size(); i++) {
            out.add(type.get(i) + " " + para.get(i));
        }
        return String.join(", ", out);
    }

    static String makeUpperWrappedParaDestruct(FunctionPtrType function, AllocatorType allocatorType) {
        List<String> para = makeUpperWrappedParaDestructName(function, allocatorType);
        return String.join(", ", para);
    }

    static String makeWrappedParaConstruct(FunctionPtrType function, AllocatorType allocatorType) {
        List<String> para = makeWrappedParaConstructName(function, allocatorType);
        return String.join(", ", para);
    }

    static String makeRawPara(FunctionPtrType function, AllocatorType allocatorType) {
        List<String> out = new ArrayList<>();
        List<String> type = makeDirectParaType(function, allocatorType);
        List<String> para = makeParaName(function, allocatorType);
        Assert(type.size() == para.size(), "type.size() != para.size");
        for (int i = 0; i < type.size(); i++) {
            out.add(type.get(i) + " " + para.get(i));
        }
        return String.join(", ", out);
    }

    private static List<String> makeWrappedParaType(FunctionPtrType function, AllocatorType allocatorType) {
        List<String> para = new ArrayList<>();
        if (allocatorType == AllocatorType.STANDARD) {
            para.add(CommonTypes.FFMTypes.SEGMENT_ALLOCATOR.typeName(TypeAttr.NameType.RAW));
        }
        para.addAll(function.getArgs().stream().map(arg -> Generator.getTypeName(arg.type())).toList());
        return para;
    }

    private static List<String> makeUpperWrappedParaType(FunctionPtrType function, AllocatorType allocatorType) {
        List<String> para = new ArrayList<>();
        if (allocatorType == AllocatorType.STANDARD) {
            para.add(CommonTypes.FFMTypes.SEGMENT_ALLOCATOR.typeName(TypeAttr.NameType.RAW));
        }
        para.addAll(function.getArgs().stream().map(arg -> {
            TypeAttr.OperationType upperType = (TypeAttr.OperationType) arg.type();
            return upperType.getOperation().getCommonOperation().getUpperType().typeName(TypeAttr.NameType.WILDCARD);
        }).toList());
        return para;
    }

    private static List<String> makeDirectParaType(FunctionPtrType function, AllocatorType allocatorType) {
        List<String> para = new ArrayList<>();
        if (allocatorType == AllocatorType.STANDARD) {
            para.add(CommonTypes.FFMTypes.SEGMENT_ALLOCATOR.typeName(TypeAttr.NameType.RAW));
        }
        para.addAll(getFuncArgPrimitives(function.getArgs().stream()).map(CommonTypes.Primitives::getPrimitiveTypeName).toList());
        return para;
    }

    private static Stream<CommonTypes.Primitives> getFuncArgPrimitives(Stream<FunctionPtrType.Arg> arg) {
        return arg.map(a -> ((TypeAttr.OperationType) a.type()).getOperation().getFuncOperation().getPrimitiveType());
    }

    private static List<String> makeParaName(FunctionPtrType function, AllocatorType allocatorType) {
        List<String> para = new ArrayList<>();
        if (allocatorType == AllocatorType.STANDARD) {
            para.add(SEGMENT_ALLOCATOR_PARAMETER_NAME);
        }
        para.addAll(function.getArgs().stream().map(FunctionPtrType.Arg::argName).toList());
        return para;
    }

    static FunctionPtrType getNonConflictType(FunctionPtrType function, List<String> forbidNames) {
        ArrayList<String> existingNames = new ArrayList<>(function.getArgs().stream().map(FunctionPtrType.Arg::argName).toList());
        ArrayList<FunctionPtrType.Arg> args = new ArrayList<>();
        for (FunctionPtrType.Arg arg : function.getArgs()) {
            args.add(new FunctionPtrType.Arg(ConflictNameUtils.getNonConflictsNameExt(arg.argName(), forbidNames, existingNames), arg.type()));
        }
        return new FunctionPtrType(function.typeName(null), args, (TypeAttr.TypeRefer) function.getReturnType().orElse(VoidType.VOID));
    }

}
