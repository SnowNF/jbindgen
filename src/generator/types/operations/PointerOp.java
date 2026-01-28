package generator.types.operations;

import generator.PackageManager;
import generator.types.*;

import static generator.generation.generator.CommonGenerator.PTR_MAKE_OPERATION_METHOD;

public class PointerOp implements OperationAttr.ValueBasedOperation {
    private final PointerType pointerType;
    private final TypeAttr.OperationType pointeeType;

    public PointerOp(PointerType pointerType) {
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
                return new Result("new %s(%s, %s)".formatted(pointerType.typeName(packages, TypeAttr.NameType.GENERIC), varName, operation.str()),
                        new TypeImports().addUseImports(CommonTypes.BindTypes.Ptr));
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
                return new Getter("", pointerType.typeName(packages, TypeAttr.NameType.GENERIC),
                        "new %s(%s, %s)".formatted(pointerType.typeName(packages, TypeAttr.NameType.GENERIC),
                                "%s.getAddr(%s, %s)".formatted(
                                        CommonTypes.SpecificTypes.MemoryUtils.typeName(), ms, offset),
                                pointeeType.getOperation().getCommonOperation().makeOperation(packages).str()),
                        new TypeImports().addUseImports(pointerType).addUseImports(CommonTypes.SpecificTypes.MemoryUtils));
            }

            @Override
            public Setter setter(String ms, long offset, String varName) {
                CommonOperation.UpperType upperType = getCommonOperation().getUpperType(packages);
                return new Setter(upperType.typeName(packages, TypeAttr.NameType.WILDCARD) + " " + varName,
                        "%s.setAddr(%s, %s, %s.operator().value())".formatted(
                                packages.useClass(CommonTypes.SpecificTypes.MemoryUtils),
                                ms, offset, varName), new TypeImports());
            }
        };
    }

    @Override
    public CommonOperation getCommonOperation() {
        return new CommonOperation() {
            @Override
            public Operation makeOperation(PackageManager packages) {
                Operation pointeeOp = pointeeType.getOperation().getCommonOperation().makeOperation(packages);
                return new Operation(packages.useClass(pointerType) + "." + PTR_MAKE_OPERATION_METHOD + "(%s)".formatted(pointeeOp.str()));
            }

            @Override
            public MemoryLayouts makeMemoryLayout(PackageManager packages) {
                return CommonOperation.makeStaticMemoryLayout(MemoryLayouts.ADDRESS);
            }

            @Override
            public UpperType getUpperType(PackageManager packages) {
                return new Warp<>(CommonTypes.ValueInterface.PtrI, pointeeType.getOperation().getCommonOperation(), packages);
            }
        };
    }
}
