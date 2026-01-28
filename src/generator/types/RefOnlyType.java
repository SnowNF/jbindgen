package generator.types;

import generator.types.operations.CommonOpOnly;
import generator.types.operations.OperationAttr;

public record RefOnlyType(String typeName) implements
        TypeAttr.TypeRefer, TypeAttr.GenerationType, TypeAttr.NamedType, TypeAttr.OperationType {

    @Override
    public TypeImports getUseImportTypes() {
        return new TypeImports(this);
    }

    @Override
    public String typeName(TypeAttr.NameType nameType) {
        return typeName;
    }

    @Override
    public OperationAttr.Operation getOperation() {
        return new CommonOpOnly<>(this, false);
    }
}
