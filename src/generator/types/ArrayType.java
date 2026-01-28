package generator.types;

import generator.PackageManager;
import generator.types.operations.ArrayOp;
import generator.types.operations.OperationAttr;

import static utils.CommonUtils.Assert;

public record ArrayType(long length, TypeAttr.TypeRefer element, long byteSize) implements
        TypeAttr.SizedType, TypeAttr.OperationType, TypeAttr.NamedType, TypeAttr.TypeRefer, TypeAttr.GenerationType {
    public static final CommonTypes.SpecificTypes ARRAY_TYPE = CommonTypes.SpecificTypes.Array;

    public ArrayType {
        Assert(length > 0, "length must be greater than zero");
    }

    @Override
    public OperationAttr.Operation getOperation() {
        return new ArrayOp(this);
    }

    @Override
    public String typeName() {
        return ARRAY_TYPE.typeName();
    }

    @Override
    public String typeName(PackageManager packages, TypeAttr.NameType nameType) {
        return switch (nameType) {
            case WILDCARD ->
                    ARRAY_TYPE.getWildcardName(((TypeAttr.NamedType) element).typeName(packages, TypeAttr.NameType.WILDCARD));
            case GENERIC ->
                    ARRAY_TYPE.getGenericName(((TypeAttr.NamedType) element).typeName(packages, TypeAttr.NameType.GENERIC));
            case RAW -> packages.useClass(ARRAY_TYPE);
        };
    }
}
