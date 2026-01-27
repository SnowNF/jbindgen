package generator.types;

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
        return new PointerOp(CommonTypes.BindTypes.makePtrGenericName(((TypeAttr.NamedType) pointee).typeName(TypeAttr.NameType.GENERIC)), this);
    }

    @Override
    public TypeImports getDefineImportTypes() {
        return pointee.getUseImportTypes().addUseImports(CommonTypes.BindTypes.Ptr);
    }

    @Override
    public TypeImports getUseImportTypes() {
        return pointee.getUseImportTypes().addUseImports(CommonTypes.BindTypes.Ptr);
    }

    @Override
    public String toString() {
        return "PointerType{" +
               "pointee=" + ((TypeAttr.NamedType) pointee).typeName(TypeAttr.NameType.GENERIC) +
               '}';
    }

    @Override
    public MemoryLayouts getMemoryLayout() {
        return CommonTypes.Primitives.ADDRESS.getMemoryLayout();
    }

    @Override
    public long byteSize() {
        return CommonTypes.Primitives.ADDRESS.byteSize();
    }

    @Override
    public String typeName(TypeAttr.NameType nameType) {
        return switch (nameType) {
            case WILDCARD ->
                    CommonTypes.BindTypes.makePtrWildcardName(((TypeAttr.NamedType) pointee).typeName(TypeAttr.NameType.WILDCARD));
            case GENERIC ->
                    CommonTypes.BindTypes.makePtrGenericName(((TypeAttr.NamedType) pointee).typeName(TypeAttr.NameType.GENERIC));
            case RAW -> CommonTypes.BindTypes.Ptr.typeName(TypeAttr.NameType.RAW);
        };
    }
}
