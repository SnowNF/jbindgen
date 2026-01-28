package generator.types.operations;

import generator.PackageManager;
import generator.types.*;

public class MemoryBased implements OperationAttr.MemoryBasedOperation {
    private final StructType structType;

    public MemoryBased(StructType structType) {
        this.structType = structType;
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
                return new Result("new " + packages.useClass(structType) + "(" + varName + ")");
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
                return new Getter("", packages.useClass(structType),
                        "new %s(%s)".formatted(packages.useClass(structType),
                                "%s.asSlice(%s, %s)".formatted(ms, offset, memoryLayout)));
            }

            @Override
            public Setter setter(String ms, long offset, String varName) {
                CommonOperation.UpperType upperType = getCommonOperation().getUpperType(packages);
                return new Setter(upperType.typeName(packages, TypeAttr.NameType.WILDCARD) + " " + varName,
                        "%s.memcpy(%s.operator().value(), %s, %s, %s, %s.byteSize())".formatted(
                                packages.useClass(CommonTypes.SpecificTypes.MemoryUtils),
                                varName, 0, ms, offset, memoryLayout), new TypeImports());
            }
        };
    }

    @Override
    public CommonOperation getCommonOperation() {
        return new CommonOperation() {
            @Override
            public Operation makeOperation(PackageManager packages) {
                return CommonOperation.makeStaticOperation(packages, structType);
            }

            @Override
            public MemoryLayouts makeMemoryLayout(PackageManager packages) {
                return CommonOperation.makeStaticMemoryLayout(makeOperation(packages));
            }

            @Override
            public UpperType getUpperType(PackageManager packages) {
                End<?> end = new End<>(structType, packages);
                return new Warp<>(CommonTypes.BasicOperations.StructI, end);
            }

            @Override
            public AllocatorType getAllocatorType() {
                return AllocatorType.STANDARD;
            }
        };
    }

}
