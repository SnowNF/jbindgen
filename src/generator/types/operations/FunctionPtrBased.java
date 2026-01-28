package generator.types.operations;

import generator.PackageManager;
import generator.generation.generator.FuncProtocolGenerator;
import generator.types.*;

import static generator.types.CommonTypes.SpecificTypes.MemoryUtils;

public class FunctionPtrBased implements OperationAttr.ValueBasedOperation {

    private final FunctionPtrType functionPtrType;
    private final String typeName;

    public FunctionPtrBased(FunctionPtrType functionPtrType, String typeName) {
        this.functionPtrType = functionPtrType;
        this.typeName = typeName;
    }

    @Override
    public FuncOperation getFuncOperation(PackageManager packages) {
        return new FuncOperation() {
            @Override
            public Result destructToPara(String varName) {
                return new Result(varName + ".operator().value()", new TypeImports().addUseImports(functionPtrType));
            }

            @Override
            public Result constructFromRet(String varName) {
                return new Result("new " + typeName + "(" + varName + ")", new TypeImports().addUseImports(functionPtrType));
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
                return new Getter("", typeName, "new %s(%s)".formatted(typeName,
                        "%s.getAddr(%s, %s)".formatted(
                                MemoryUtils.typeName(TypeAttr.NameType.RAW),
                                ms, offset)), new TypeImports().addUseImports(functionPtrType).addUseImports(MemoryUtils));
            }

            @Override
            public Setter setter(String ms, long offset, String varName) {
                CommonOperation.UpperType upperType = getCommonOperation().getUpperType(packages);
                return new Setter(upperType.typeName(TypeAttr.NameType.WILDCARD) + " " + varName,
                        "%s.setAddr(%s, %s, %s.operator().value())".formatted(
                                MemoryUtils.typeName(TypeAttr.NameType.RAW), ms, offset, varName),
                        upperType.typeImports().addUseImports(MemoryUtils));
            }
        };
    }

    @Override
    public CommonOperation getCommonOperation() {
        return new CommonOperation() {
            @Override
            public Operation makeOperation(PackageManager packages) {
                return CommonOperation.makeStaticOperation(functionPtrType, typeName);
            }


            @Override
            public MemoryLayouts makeDirectMemoryLayout(PackageManager packages) {
                return CommonOperation.makeStaticMemoryLayout(CommonTypes.Primitives.ADDRESS.getMemoryLayout(packages));
            }

            @Override
            public UpperType getUpperType(PackageManager packages) {
                End<?> end = new End<>(functionPtrType, functionPtrType.typeName(TypeAttr.NameType.RAW) + "." + FuncProtocolGenerator.FUNCTION_TYPE_NAME);
                return new Warp<>(CommonTypes.ValueInterface.PtrI, end);
            }
        };
    }
}
