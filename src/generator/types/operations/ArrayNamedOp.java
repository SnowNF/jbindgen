package generator.types.operations;

import generator.PackageManager;
import generator.types.ArrayTypeNamed;
import generator.types.CommonTypes;
import generator.types.TypeAttr;

import static generator.types.CommonTypes.SpecificTypes.MemoryUtils;

public class ArrayNamedOp implements OperationAttr.MemoryBasedOperation {
    private final ArrayTypeNamed arrayType;
    private final TypeAttr.OperationType element;

    public ArrayNamedOp(ArrayTypeNamed arrayType) {
        this.arrayType = arrayType;
        this.element = arrayType.element();
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
                return new Result("new %s(%s)".formatted(typeName(packages), varName));
            }

            @Override
            public CommonTypes.Primitives getPrimitiveType() {
                return CommonTypes.Primitives.ADDRESS;
            }
        };
    }

    private String typeName(PackageManager packages) {
        return packages.useClass(arrayType);
    }

    @Override
    public MemoryOperation getMemoryOperation(PackageManager packages) {
        return new MemoryOperation() {
            private final String memoryLayout = getCommonOperation().makeMemoryLayout(packages).getMemoryLayout(packages);

            @Override
            public Getter getter(String ms, long offset) {
                return new Getter("", typeName(packages),
                        "new %s(%s)".formatted(typeName(packages),
                                "%s.asSlice(%s, %s)".formatted(ms, offset, memoryLayout)));
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
                return CommonOperation.makeStaticOperation(packages, arrayType);
            }

            @Override
            public UpperType getUpperType(PackageManager packages) {
                End<?> end = new End<>(arrayType, packages);
                return new Warp<>(CommonTypes.BasicOperations.ArrayNamed, end);
            }

            @Override
            public AllocatorType getAllocatorType() {
                return AllocatorType.STANDARD;
            }
        };
    }
}
