package generator.types.operations;

import generator.PackageManager;
import generator.types.TypeAttr;

public class CommonOpOnly<T extends TypeAttr.NamedType & TypeAttr.TypeRefer & TypeAttr.OperationType> implements OperationAttr.CommonOnlyOperation {
    private final String typeName;
    private final T type;
    private final boolean realVoid;

    public CommonOpOnly(T type, boolean realVoid) {
        this.typeName = type.typeName();
        this.type = type;
        this.realVoid = realVoid;
    }

    @Override
    public FuncOperation getFuncOperation(PackageManager packages) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public MemoryOperation getMemoryOperation(PackageManager packages) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CommonOperation getCommonOperation() {
        return new CommonOperation() {
            @Override
            public Operation makeOperation(PackageManager packages) {
                return realVoid ? CommonOperation.makeVoidOperation() : CommonOperation.makeStaticOperation(type, typeName);
            }

            @Override
            public UpperType getUpperType(PackageManager packages) {
                // use Ptr<?> instead of Ptr<? extends Void>
                if (realVoid) {
                    return new Reject<>(type);
                }
                return new End<>(type);
            }
        };
    }
}
