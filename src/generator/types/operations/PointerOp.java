package generator.types.operations;

import generator.PackageManager;
import generator.types.*;

import static generator.generation.generator.CommonGenerator.PTR_MAKE_OPERATION_METHOD;

public class PointerOp implements OperationAttr.ValueBasedOperation {
    private final String typeName;
    private final PointerType pointerType;
    private final TypeAttr.OperationType pointeeType;

    public PointerOp(String typeName, PointerType pointerType) {
        this.typeName = typeName;
        this.pointerType = pointerType;
        pointeeType = (TypeAttr.OperationType) pointerType.pointee();
    }

    @Override
    public FuncOperation getFuncOperation(PackageManager packages) {
        return new FuncOperation() {
            @Override
            public Result destructToPara(String varName) {
                return new Result(varName + ".operator().value()", new TypeImports().addUseImports(pointerType));
            }

            @Override
            public Result constructFromRet(String varName) {
                CommonOperation.Operation operation = pointeeType.getOperation().getCommonOperation().makeOperation(packages);
                return new Result("new %s(%s, %s)".formatted(typeName, varName, operation.str()),
                        operation.imports().addUseImports(CommonTypes.BindTypes.Ptr));
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
                return new Getter("", typeName, "new %s(%s, %s)".formatted(typeName,
                        "%s.getAddr(%s, %s)".formatted(
                                CommonTypes.SpecificTypes.MemoryUtils.typeName(TypeAttr.NameType.RAW), ms, offset),
                        pointeeType.getOperation().getCommonOperation().makeOperation(packages).str()),
                        new TypeImports().addUseImports(pointerType).addUseImports(CommonTypes.SpecificTypes.MemoryUtils));
            }

            @Override
            public Setter setter(String ms, long offset, String varName) {
                CommonOperation.UpperType upperType = getCommonOperation().getUpperType(packages);
                return new Setter(upperType.typeName(TypeAttr.NameType.WILDCARD) + " " + varName,
                        "%s.setAddr(%s, %s, %s.operator().value())".formatted(
                                CommonTypes.SpecificTypes.MemoryUtils.typeName(TypeAttr.NameType.RAW),
                                ms, offset, varName), upperType.typeImports().addUseImports(CommonTypes.SpecificTypes.MemoryUtils));
            }
        };
    }

    @Override
    public CommonOperation getCommonOperation() {
        return new CommonOperation() {
            @Override
            public Operation makeOperation(PackageManager packages) {
                Operation pointeeOp = pointeeType.getOperation().getCommonOperation().makeOperation(packages);
                return new Operation(pointerType.typeName(TypeAttr.NameType.RAW) + "." + PTR_MAKE_OPERATION_METHOD + "(%s)"
                        .formatted(pointeeOp.str()), pointeeOp.imports().addUseImports(pointerType));
            }

            @Override
            public MemoryLayouts makeDirectMemoryLayout(PackageManager packages) {
                return CommonOperation.makeStaticMemoryLayout(MemoryLayouts.ADDRESS);
            }

            @Override
            public UpperType getUpperType(PackageManager packages) {
                return new Warp<>(CommonTypes.ValueInterface.PtrI, pointeeType.getOperation().getCommonOperation(), packages);
            }
        };
    }
}
