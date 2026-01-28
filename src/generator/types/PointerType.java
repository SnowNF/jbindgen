package generator.types;

import generator.PackageManager;
import generator.types.operations.OperationAttr;
import generator.types.operations.PointerOp;

public record PointerType(TypeAttr.GenerationType pointee) implements
        TypeAttr.SizedType {

    @Override
    public OperationAttr.Operation getOperation() {
        return new PointerOp(this);
    }

    @Override
    public String toString() {
        return "PointerType{" +
               "pointee=" + pointee.typeName() +
               '}';
    }

    public MemoryLayouts getMemoryLayout(PackageManager packages) {
        return CommonTypes.Primitives.ADDRESS.getMemoryLayout();
    }

    @Override
    public long byteSize() {
        return CommonTypes.Primitives.ADDRESS.byteSize();
    }

    @Override
    public String typeName() {
        return CommonTypes.BindTypes.Ptr.typeName();
    }

    @Override
    public String typeName(PackageManager packages, TypeAttr.NameType nameType) {
        return switch (nameType) {
            case WILDCARD ->
                    CommonTypes.BindTypes.makePtrWildcardName(packages, pointee.typeName(packages, TypeAttr.NameType.WILDCARD));
            case GENERIC ->
                    CommonTypes.BindTypes.makePtrGenericName(packages, pointee.typeName(packages, TypeAttr.NameType.GENERIC));
            case RAW -> packages.useClass(CommonTypes.BindTypes.Ptr);
        };
    }
}
