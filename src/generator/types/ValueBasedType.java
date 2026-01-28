package generator.types;

import generator.types.operations.NoJavaPrimitiveType;
import generator.types.operations.OperationAttr;
import generator.types.operations.ValueBased;

import java.util.Objects;
import java.util.Optional;

import static utils.CommonUtils.Assert;

public final class ValueBasedType extends AbstractGenerationType {
    private final CommonTypes.BindTypes bindTypes;
    private final PointerType pointerType;

    public ValueBasedType(String typeName, CommonTypes.BindTypes bindTypes) {
        super(p -> bindTypes.getPrimitiveType().getMemoryLayout(p), typeName, bindTypes.byteSize());
        Assert(bindTypes != CommonTypes.BindTypes.Ptr);
        this.bindTypes = bindTypes;
        this.pointerType = null;
    }

    public ValueBasedType(String typeName, PointerType pointerType) {
        super(pointerType::getMemoryLayout, typeName, pointerType.byteSize());
        this.bindTypes = CommonTypes.BindTypes.Ptr;
        this.pointerType = pointerType;
    }

    @Override
    public OperationAttr.Operation getOperation() {
        if (bindTypes.getOperations().getValue().getPrimitive().noJavaPrimitive()) {
            return new NoJavaPrimitiveType<>(this, bindTypes);
        }
        return new ValueBased<>(this, typeName, bindTypes);
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
               ", typeName='" + typeName + '\'' +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ValueBasedType that)) return false;
        if (!super.equals(o)) return false;
        return bindTypes == that.bindTypes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), bindTypes);
    }
}
