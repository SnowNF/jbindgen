package generator.types.operations;

import generator.types.CommonTypes;
import generator.types.TypeAttr;
import generator.types.TypeImports;

import static utils.CommonUtils.Assert;

public class NoJavaPrimitiveType<T extends TypeAttr.NamedType & TypeAttr.TypeRefer & TypeAttr.OperationType> implements OperationAttr.MemoryBasedOperation {
    private final String typeName;
    private final CommonTypes.BindTypes bindTypes;
    private final T type;

    public NoJavaPrimitiveType(T type, CommonTypes.BindTypes bindTypes) {
        Assert(bindTypes.getOperations().getValue().getPrimitive().byteSize() == 16);
        this.typeName = type.typeName(TypeAttr.NameType.RAW);
        this.bindTypes = bindTypes;
        this.type = type;
    }

    @Override
    public FuncOperation getFuncOperation() {
        return new FuncOperation() {
            @Override
            public Result destructToPara(String varName) {
                return new Result(varName + ".operator().value()", new TypeImports().addUseImports(type));
            }

            @Override
            public Result constructFromRet(String varName) {
                return new Result("new " + typeName + "(" + varName + ")", new TypeImports().addUseImports(type));
            }

            @Override
            public CommonTypes.Primitives getPrimitiveType() {
                return CommonTypes.Primitives.ADDRESS;
            }
        };
    }

    @Override
    public MemoryOperation getMemoryOperation() {
        return new MemoryOperation() {
            private final String memoryLayout = getCommonOperation().makeDirectMemoryLayout().getMemoryLayout();

            @Override
            public Getter getter(String ms, long offset) {
                return new Getter("", typeName, "new %s(%s)".formatted(typeName,
                        "%s.asSlice(%s, %s)".formatted(ms, offset, memoryLayout)),
                        new TypeImports().addUseImports(type));
            }

            @Override
            public Setter setter(String ms, long offset, String varName) {
                CommonOperation.UpperType upperType = getCommonOperation().getUpperType();
                return new Setter(upperType.typeName(TypeAttr.NameType.WILDCARD) + " " + varName,
                        "%s.memcpy(%s.operator().value(), %s, %s, %s, %s.byteSize())".formatted(
                                CommonTypes.SpecificTypes.MemoryUtils.typeName(TypeAttr.NameType.RAW),
                                varName, 0, ms, offset, memoryLayout),
                        upperType.typeImports().addUseImports(CommonTypes.SpecificTypes.MemoryUtils));
            }
        };
    }

    @Override
    public CommonOperation getCommonOperation() {
        return new CommonOperation() {
            @Override
            public Operation makeOperation() {
                return CommonOperation.makeStaticOperation(type, typeName);
            }

            @Override
            public AllocatorType getAllocatorType() {
                return AllocatorType.ON_HEAP;
            }

            @Override
            public UpperType getUpperType() {
                return new Warp<>(bindTypes.getOperations().getValue(), new Reject<>(type));
            }
        };
    }
}
