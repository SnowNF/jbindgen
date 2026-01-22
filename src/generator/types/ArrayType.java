package generator.types;

import generator.types.operations.ArrayOp;
import generator.types.operations.OperationAttr;

import static utils.CommonUtils.Assert;

public record ArrayType(long length, TypeAttr.TypeRefer element, long byteSize) implements
        TypeAttr.SizedType, TypeAttr.OperationType, TypeAttr.NamedType, TypeAttr.TypeRefer {
    public static final CommonTypes.SpecificTypes LIST_TYPE = CommonTypes.SpecificTypes.Array;

    public ArrayType {
        Assert(length > 0, "length must be greater than zero");
    }

    @Override
    public OperationAttr.Operation getOperation() {
        return new ArrayOp(typeName(TypeAttr.NameType.GENERIC), this);
    }

    @Override
    public TypeImports getDefineImportTypes() {
        return element.getUseImportTypes().addUseImports(LIST_TYPE);
    }

    @Override
    public TypeImports getUseImportTypes() {
        return element.getUseImportTypes().addUseImports(LIST_TYPE);
    }

    @Override
    public String typeName(TypeAttr.NameType nameType) {
        return switch (nameType) {
            case WILDCARD ->
                    LIST_TYPE.getWildcardName(((TypeAttr.NamedType) element).typeName(TypeAttr.NameType.WILDCARD));
            case GENERIC ->
                    LIST_TYPE.getGenericName(((TypeAttr.NamedType) element).typeName(TypeAttr.NameType.GENERIC));
            case RAW -> LIST_TYPE.typeName(TypeAttr.NameType.RAW);
        };
    }

    @Override
    public MemoryLayouts getMemoryLayout() {
        return MemoryLayouts.sequenceLayout(((TypeAttr.OperationType) element).getOperation().getCommonOperation().makeDirectMemoryLayout(), length);
    }
}
