package generator.types;

import generator.PackageManager;
import generator.types.operations.OperationAttr;
import generator.types.operations.PointerOp;

public record PointerType(TypeAttr.TypeRefer pointee) implements
        TypeAttr.SizedType,
        TypeAttr.OperationType,
        TypeAttr.NamedType,
        TypeAttr.TypeRefer,
        TypeAttr.GenerationType {

    @Override
    public OperationAttr.Operation getOperation() {
        return new PointerOp( this);
    }

    @Override
    public TypeImports getUseImportTypes() {
        return pointee.getUseImportTypes().addUseImports(CommonTypes.BindTypes.Ptr);
    }

    @Override
    public String toString() {
        return "PointerType{" +
               "pointee=" + ((TypeAttr.NamedType) pointee).typeName() +
               '}';
    }

    @Override
    public MemoryLayouts getMemoryLayout(PackageManager packages) {
        return CommonTypes.Primitives.ADDRESS.getMemoryLayout(packages);
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
                    CommonTypes.BindTypes.makePtrWildcardName(((TypeAttr.NamedType) pointee).typeName(packages, TypeAttr.NameType.WILDCARD));
            case GENERIC ->
                    CommonTypes.BindTypes.makePtrGenericName(((TypeAttr.NamedType) pointee).typeName(packages, TypeAttr.NameType.GENERIC));
        };
    }
}
