package generator.types;

import generator.PackageManager;
import generator.types.operations.ArrayOp;
import generator.types.operations.OperationAttr;

import static utils.CommonUtils.Assert;

public record ArrayType(long length, TypeAttr.SizedType element, long byteSize) implements TypeAttr.SizedType {
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
                    ARRAY_TYPE.getWildcardName(packages, element.typeName(packages, TypeAttr.NameType.WILDCARD));
            case GENERIC -> ARRAY_TYPE.getGenericName(packages, element.typeName(packages, TypeAttr.NameType.GENERIC));
            case RAW -> packages.useClass(ARRAY_TYPE);
        };
    }
}
