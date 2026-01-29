package generator.types.operations;

import generator.PackageManager;
import generator.types.ArrayType;
import generator.types.CommonTypes;
import generator.types.MemoryLayouts;
import generator.types.TypeAttr;

import static generator.generators.CommonGenerator.ARRAY_MAKE_OPERATION_METHOD;
import static generator.types.CommonTypes.SpecificTypes.MemoryUtils;

public class ArrayOp implements OperationAttr.MemoryBasedOperation {
    private final ArrayType arrayType;
    private final TypeAttr.OperationType element;

    public ArrayOp(ArrayType arrayType) {
        this.arrayType = arrayType;
        this.element = (TypeAttr.OperationType) arrayType.element();
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
                CommonOperation.Operation operation = element.getOperation().getCommonOperation().makeOperation(packages);
                return new Result("new %s(%s, %s)".formatted(packages.useType(arrayType, TypeAttr.NameType.GENERIC), varName, operation.str()));
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
                return new Getter("", packages.useType(arrayType, TypeAttr.NameType.GENERIC),
                        "new %s(%s, %s)".formatted(packages.useType(arrayType, TypeAttr.NameType.GENERIC),
                                "%s.asSlice(%s, %s)".formatted(ms, offset, memoryLayout),
                                element.getOperation().getCommonOperation().makeOperation(packages).str()));
            }

            @Override
            public Setter setter(String ms, long offset, String varName) {
                CommonOperation.UpperType upperType = getCommonOperation().getUpperType(packages);
                return new Setter(upperType.typeName(packages) + " " + varName,
                        "%s.memcpy(%s.operator().value(), %s, %s, %s, %s.byteSize())".formatted(
                                packages.useClass(MemoryUtils),
                                varName, 0, ms, offset, memoryLayout));

            }
        };
    }

    @Override
    public CommonOperation getCommonOperation() {
        return new CommonOperation() {
            @Override
            public Operation makeOperation(PackageManager packages) {
                Operation eleOp = element.getOperation().getCommonOperation().makeOperation(packages);
                return new Operation(packages.useClass(arrayType) + "." + ARRAY_MAKE_OPERATION_METHOD + "(%s, %s)"
                        .formatted(eleOp.str(), arrayType.length()));
            }

            @Override
            public UpperType getUpperType(PackageManager packages) {
                return new Warp<>(CommonTypes.BasicOperations.ArrayI, element.getOperation().getCommonOperation().getUpperType(packages));
            }

            @Override
            public MemoryLayouts makeMemoryLayout(PackageManager packages) {
                return MemoryLayouts.sequenceLayout(element.getOperation().getCommonOperation().makeMemoryLayout(packages), arrayType.length());
            }

            @Override
            public AllocatorType getAllocatorType() {
                return AllocatorType.STANDARD;
            }
        };
    }

}
