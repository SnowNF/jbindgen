package generator.types;

import generator.types.operations.ArrayNamedOp;
import generator.types.operations.OperationAttr;

import static utils.CommonUtils.Assert;

public record ArrayTypeNamed(String typeName, long length, TypeAttr.SizedType element, long byteSize)
        implements TypeAttr.SizedType {
    public ArrayTypeNamed {
        Assert(length > 0, "length must be greater than zero");
    }

    @Override
    public OperationAttr.Operation getOperation() {
        return new ArrayNamedOp(this);
    }

    @Override
    public String typeName() {
        return typeName;
    }
}
