package generator.types;

import generator.types.operations.CommonOpOnly;
import generator.types.operations.OperationAttr;

public record RefOnlyType(String typeName) implements TypeAttr.OperationType {

    @Override
    public String typeName() {
        return typeName;
    }

    @Override
    public OperationAttr.Operation getOperation() {
        return new CommonOpOnly<>(this, false);
    }
}
