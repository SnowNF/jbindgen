package generator.types.operations;

import generator.PackageManager;
import generator.types.CommonTypes;
import generator.types.TypeAttr;

import static utils.CommonUtils.Assert;

public class NoJavaPrimitiveType<T extends TypeAttr.GenerationType & TypeAttr.OperationType> implements OperationAttr.MemoryBasedOperation {
    private final CommonTypes.BindTypes bindTypes;
    private final T type;

    public NoJavaPrimitiveType(T type, CommonTypes.BindTypes bindTypes) {
        Assert(bindTypes.getOperations().getValue().getPrimitive().byteSize() == 16);
        this.bindTypes = bindTypes;
        this.type = type;
    }

    @Override
    public FuncOperation getFuncOperation(PackageManager packages) {
        return new FuncOperation() {
            @Override
            public Result destructToPara(String varName) {
                return new Result(varName + ".operator().value()");
            }

            @Override
            public Result constructFromRet(String varName) {
                return new Result("new " + packages.useClass(type) + "(" + varName + ")");
            }

            @Override
            public CommonTypes.Primitives getPrimitiveType() {
                return CommonTypes.Primitives.ADDRESS;
            }
        };
    }

    @Override
    public MemoryOperation getMemoryOperation(PackageManager packages) {
        return new MemoryOperation() {
            private final String memoryLayout = getCommonOperation().makeMemoryLayout(packages).getMemoryLayout(packages);

            @Override
            public Getter getter(String ms, long offset) {
                return new Getter("", packages.useClass(type),
                        "new %s(%s)".formatted(packages.useClass(type),
                                "%s.asSlice(%s, %s)".formatted(ms, offset, memoryLayout)));
            }

            @Override
            public Setter setter(String ms, long offset, String varName) {
                CommonOperation.UpperType upperType = getCommonOperation().getUpperType(packages);
                return new Setter(upperType.typeName(packages) + " " + varName,
                        "%s.memcpy(%s.operator().value(), %s, %s, %s, %s.byteSize())".formatted(
                                packages.useClass(CommonTypes.SpecificTypes.MemoryUtils),
                                varName, 0, ms, offset, memoryLayout));
            }
        };
    }

    @Override
    public CommonOperation getCommonOperation() {
        return new CommonOperation() {
            @Override
            public Operation makeOperation(PackageManager packages) {
                return CommonOperation.makeStaticOperation(packages, type);
            }

            @Override
            public AllocatorType getAllocatorType() {
                return AllocatorType.ON_HEAP;
            }

            @Override
            public UpperType getUpperType(PackageManager packages) {
                return new Warp<>(bindTypes.getOperations().getValue(), new Reject<>(type));
            }
        };
    }
}
