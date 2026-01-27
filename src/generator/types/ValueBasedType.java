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
        super(bindTypes.getPrimitiveType().getMemoryLayout(), typeName, bindTypes.byteSize());
        Assert(bindTypes != CommonTypes.BindTypes.Ptr);
        this.bindTypes = bindTypes;
        this.pointerType = null;
    }

    public ValueBasedType(String typeName, PointerType pointerType) {
        super(pointerType.getMemoryLayout(), typeName, pointerType.byteSize());
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
    public TypeImports getDefineImportTypes() {
        TypeImports imports = new TypeImports()
                .addUseImports(bindTypes.getOperations())
                .addUseImports(bindTypes.getOperations().getValue())
                .addUseImports(CommonTypes.BasicOperations.Info)
                .addUseImports(CommonTypes.ValueInterface.I64I)
                .addUseImports(CommonTypes.SpecificTypes.Array)
                .addUseImports(CommonTypes.BindTypes.Ptr)
                .addUseImports(CommonTypes.FFMTypes.SEGMENT_ALLOCATOR);
        bindTypes.getPrimitiveType().importNeedType().ifPresent(imports::addUseImports);
        if (pointerType != null) {
            imports.addUseImports(pointerType.pointee())
                    .addUseImports(CommonTypes.BasicOperations.Value)
                    .addUseImports(CommonTypes.SpecificTypes.ArrayOp)
                    .addUseImports(CommonTypes.ValueInterface.PtrI);
        }
        if (bindTypes.getPrimitiveType().noJavaPrimitive()) {
            imports.addUseImports(CommonTypes.SpecificTypes.MemoryUtils)
                    .addUseImports(bindTypes);
        }
        return imports;
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
