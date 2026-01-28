package generator.types;

import generator.PackageManager;
import generator.Utils;
import generator.generation.generator.FuncProtocolGenerator;
import generator.types.operations.CommonOperation;
import generator.types.operations.FunctionPtrBased;
import generator.types.operations.OperationAttr;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static utils.CommonUtils.Assert;

// function ptr type, not function protocol type
public final class FunctionPtrType implements SingleGenerationType {
    public record Arg(String argName, TypeAttr.TypeRefer type) {
        public Arg {
            Assert(Utils.isValidVarName(argName), "Arg name must be a valid variable name: " + argName);
        }
    }

    private final String typeName;
    private final List<Arg> args;
    private final TypeAttr.TypeRefer returnType;
    private final CommonOperation.AllocatorType allocator;

    public FunctionPtrType(String typeName, List<Arg> args, TypeAttr.TypeRefer retType) {
        this.typeName = typeName;
        this.args = List.copyOf(args);
        returnType = switch (retType) {
            case TypeAttr.SizedType normalType -> ((TypeAttr.TypeRefer) normalType);
            case VoidType _ -> null;
            default -> throw new IllegalStateException("Unexpected value: " + retType);
        };
        allocator = returnType instanceof TypeAttr.OperationType o
                ? o.getOperation().getCommonOperation().getAllocatorType()
                : CommonOperation.AllocatorType.NONE;
    }


    public CommonOperation.AllocatorType allocatorType() {
        return allocator;
    }

    public Optional<TypeAttr.OperationType> getReturnType() {
        return Optional.ofNullable(returnType).map(referenceType -> ((TypeAttr.OperationType) referenceType));
    }

    public List<Arg> getArgs() {
        return args;
    }

    public List<MemoryLayouts> getMemoryLayouts(PackageManager packages) {
        ArrayList<MemoryLayouts> memoryLayout = new ArrayList<>();
        if (this.getReturnType().isPresent())
            memoryLayout.add(this.getReturnType().get().getOperation().getCommonOperation().makeMemoryLayout(packages));
        for (Arg arg : this.getArgs()) {
            memoryLayout.add(((TypeAttr.OperationType) arg.type()).getOperation().getCommonOperation().makeMemoryLayout(packages));
        }
        return memoryLayout;
    }

    @Override
    public OperationAttr.Operation getOperation() {
        return new FunctionPtrBased(this, typeName);
    }

    @Override
    public String typeName() {
        return typeName;
    }

    public String innerFunctionTypeName(PackageManager packages) {
        return packages.useClass(this) + "." + FuncProtocolGenerator.FUNCTION_TYPE_NAME;
    }

    @Override
    public long byteSize() {
        return CommonTypes.Primitives.ADDRESS.byteSize();
    }

    @Override
    public TypeImports getUseImportTypes() {
        return new TypeImports(this);
    }

    @Override
    public String toString() {
        return "FunctionPtrType{" +
               "typeName='" + typeName + '\'' +
               ", args=" + args +
               ", returnType=" + returnType +
               ", allocator=" + allocator +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FunctionPtrType that)) return false;
        return Objects.equals(typeName, that.typeName) && Objects.equals(args, that.args) && Objects.equals(returnType, that.returnType) && allocator == that.allocator;
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName, args, returnType, allocator);
    }
}
