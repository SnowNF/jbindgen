package generator.types.operations;

import generator.PackageManager;
import generator.types.CommonTypes;
import generator.types.TypeAttr;
import generator.types.TypeImports;

public class DestructOnlyOp<T extends TypeAttr.NamedType & TypeAttr.TypeRefer> implements OperationAttr.DesctructOnlyOperation {
    private final T type;
    private final CommonTypes.Primitives primitives;

    public DestructOnlyOp(T type, CommonTypes.Primitives primitives) {
        this.type = type;
        this.primitives = primitives;
    }

    @Override
    public FuncOperation getFuncOperation(PackageManager packages) {
        return new FuncOperation() {
            @Override
            public Result destructToPara(String varName) {
                return new Result(varName + ".operator().value()", new TypeImports().addUseImports(type));
            }

            @Override
            public Result constructFromRet(String varName) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CommonTypes.Primitives getPrimitiveType() {
                return primitives;
            }
        };
    }

    @Override
    public MemoryOperation getMemoryOperation(PackageManager packages) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CommonOperation getCommonOperation() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
