package generator.types.operations;

import generator.types.*;

import static generator.generation.generator.CommonGenerator.ARRAY_MAKE_OPERATION_METHOD;
import static generator.types.CommonTypes.SpecificTypes.MemoryUtils;

public class ArrayOp implements OperationAttr.MemoryBasedOperation {
    private final String typeName;
    private final ArrayType arrayType;
    private final TypeAttr.OperationType element;

    public ArrayOp(String typeName, ArrayType arrayType) {
        this.arrayType = arrayType;
        this.element = (TypeAttr.OperationType) arrayType.element();
        this.typeName = typeName;
    }

    @Override
    public FuncOperation getFuncOperation() {
        return new FuncOperation() {
            @Override
            public Result destructToPara(String varName) {
                return new Result(varName + ".operator().value()", new TypeImports().addUseImports(arrayType));
            }

            @Override
            public Result constructFromRet(String varName) {
                CommonOperation.Operation operation = element.getOperation().getCommonOperation().makeOperation();
                return new Result("new %s(%s, %s)".formatted(typeName, varName, operation.str()), operation.imports());
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
                return new Getter("", typeName, "new %s(%s, %s)".formatted(typeName,
                        "%s.asSlice(%s, %s)".formatted(ms, offset, memoryLayout),
                        element.getOperation().getCommonOperation().makeOperation().str()),
                        new TypeImports().addUseImports(arrayType));
            }

            @Override
            public Setter setter(String ms, long offset, String varName) {
                CommonOperation.UpperType upperType = getCommonOperation().getUpperType();
                return new Setter(upperType.typeName(TypeAttr.NameType.WILDCARD) + " " + varName,
                        "%s.memcpy(%s.operator().value(), %s, %s, %s, %s.byteSize())".formatted(
                                MemoryUtils.typeName(TypeAttr.NameType.RAW),
                                varName, 0, ms, offset, memoryLayout),
                        upperType.typeImports().addUseImports(MemoryUtils));

            }
        };
    }

    @Override
    public CommonOperation getCommonOperation() {
        return new CommonOperation() {
            @Override
            public Operation makeOperation() {
                Operation eleOp = element.getOperation().getCommonOperation().makeOperation();
                return new Operation(arrayType.typeName(TypeAttr.NameType.RAW) + "." + ARRAY_MAKE_OPERATION_METHOD + "(%s, %s)"
                        .formatted(eleOp.str(), arrayType.length()), eleOp.imports().addUseImports(arrayType));
            }

            @Override
            public UpperType getUpperType() {
                return new Warp<>(CommonTypes.BasicOperations.ArrayI, element.getOperation().getCommonOperation());
            }

            @Override
            public MemoryLayouts makeDirectMemoryLayout() {
                return arrayType.getMemoryLayout();
            }

            @Override
            public AllocatorType getAllocatorType() {
                return AllocatorType.STANDARD;
            }
        };
    }

}
