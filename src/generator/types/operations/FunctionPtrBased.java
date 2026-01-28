package generator.types.operations;

import generator.PackageManager;
import generator.types.CommonTypes;
import generator.types.FunctionPtrType;
import generator.types.MemoryLayouts;
import generator.types.TypeAttr;

import static generator.types.CommonTypes.SpecificTypes.MemoryUtils;

public class FunctionPtrBased implements OperationAttr.ValueBasedOperation {

    private final FunctionPtrType functionPtrType;

    public FunctionPtrBased(FunctionPtrType functionPtrType) {
        this.functionPtrType = functionPtrType;
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
                return new Result("new " + packages.useClass(functionPtrType) + "(" + varName + ")");
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
            @Override
            public Getter getter(String ms, long offset) {
                return new Getter("", packages.useClass(functionPtrType),
                        "new %s(%s)".formatted(packages.useClass(functionPtrType),
                                "%s.getAddr(%s, %s)".formatted(
                                        packages.useClass(MemoryUtils), ms, offset)));
            }

            @Override
            public Setter setter(String ms, long offset, String varName) {
                CommonOperation.UpperType upperType = getCommonOperation().getUpperType(packages);
                return new Setter(upperType.typeName(packages, TypeAttr.NameType.WILDCARD) + " " + varName,
                        "%s.setAddr(%s, %s, %s.operator().value())".formatted(
                                packages.useClass(MemoryUtils), ms, offset, varName));
            }
        };
    }

    @Override
    public CommonOperation getCommonOperation() {
        return new CommonOperation() {
            @Override
            public Operation makeOperation(PackageManager packages) {
                return CommonOperation.makeStaticOperation(packages, functionPtrType);
            }


            @Override
            public MemoryLayouts makeMemoryLayout(PackageManager packages) {
                return CommonOperation.makeStaticMemoryLayout(CommonTypes.Primitives.ADDRESS.getMemoryLayout());
            }

            @Override
            public UpperType getUpperType(PackageManager packages) {
                End<?> end = new End<>(functionPtrType, functionPtrType.innerFunctionTypePath(packages));
                return new Warp<>(CommonTypes.ValueInterface.PtrI, end);
            }
        };
    }
}
