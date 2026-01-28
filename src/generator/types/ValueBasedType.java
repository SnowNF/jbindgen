package generator.types;

import generator.types.operations.NoJavaPrimitiveType;
import generator.types.operations.OperationAttr;
import generator.types.operations.ValueBased;

import java.util.Objects;
import java.util.Optional;

import static utils.CommonUtils.Assert;

public final class ValueBasedType implements TypeAttr.SizedType, TypeAttr.OperationType, TypeAttr.NamedType, TypeAttr.TypeRefer, TypeAttr.GenerationType {
    private final CommonTypes.BindTypes bindTypes;
    private final PointerType pointerType;
    private final String typeName;

    public ValueBasedType(String typeName, CommonTypes.BindTypes bindTypes) {
        this.typeName = typeName;
        Assert(bindTypes != CommonTypes.BindTypes.Ptr);
        this.bindTypes = bindTypes;
        this.pointerType = null;
    }

    public ValueBasedType(String typeName, PointerType pointerType) {
        this.typeName = typeName;
        this.bindTypes = CommonTypes.BindTypes.Ptr;
        this.pointerType = pointerType;
    }

    @Override
    public OperationAttr.Operation getOperation() {
        if (bindTypes.getOperations().getValue().getPrimitive().noJavaPrimitive()) {
            return new NoJavaPrimitiveType<>(this, bindTypes);
        }
        return new ValueBased<>(this, bindTypes);
    }

    public CommonTypes.BindTypes getBindTypes() {
        return bindTypes;
    }

    public Optional<PointerType> getPointerType() {
        return Optional.ofNullable(pointerType);
    }

    @Override
    public String toString() {
        return "ValueBasedType{" +
               "bindTypes=" + bindTypes +
               ", pointerType=" + pointerType +
               ", typeName='" + typeName + '\'' +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ValueBasedType that)) return false;
        return bindTypes == that.bindTypes && Objects.equals(pointerType, that.pointerType) && Objects.equals(typeName, that.typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bindTypes, pointerType, typeName);
    }

    @Override
    public String typeName() {
        return typeName;
    }

    @Override
    public long byteSize() {
        return bindTypes.byteSize();
    }

}
