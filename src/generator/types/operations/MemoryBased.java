package generator.types.operations;

import generator.types.*;

public class MemoryBased implements OperationAttr.MemoryBasedOperation {
    private final String typeName;
    private final StructType structType;

    public MemoryBased(StructType structType) {
        this.typeName = structType.typeName(TypeAttr.NameType.RAW);
        this.structType = structType;
    }

    @Override
    public FuncOperation getFuncOperation() {
        return new FuncOperation() {
            @Override
            public Result destructToPara(String varName) {
                return new Result(varName + ".operator().value()", new TypeImports().addUseImports(structType));
            }

            @Override
            public Result constructFromRet(String varName) {
                return new Result("new " + typeName + "(" + varName + ")", new TypeImports().addUseImports(structType));
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
                        new TypeImports().addUseImports(structType));
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
                return CommonOperation.makeStaticOperation(structType, typeName);
            }

            @Override
            public MemoryLayouts makeDirectMemoryLayout() {
                return CommonOperation.makeStaticMemoryLayout(makeOperation());
            }

            @Override
            public UpperType getUpperType() {
                End<?> end = new End<>(structType);
                return new Warp<>(CommonTypes.BasicOperations.StructI, end);
            }

            @Override
            public AllocatorType getAllocatorType() {
                return AllocatorType.STANDARD;
            }
        };
    }

}
