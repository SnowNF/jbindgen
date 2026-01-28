package generator.types;

import generator.types.operations.OperationAttr;
import generator.types.operations.ValueBased;

import java.util.List;
import java.util.Objects;

import static utils.CommonUtils.Assert;

public final class EnumType extends AbstractGenerationType {
    /**
     * the enum member
     */
    public record Member(long val, String name) {

    }

    private final List<Member> members;
    private final CommonTypes.BindTypes type;

    public EnumType(CommonTypes.BindTypes type, String typeName, List<Member> members) {
        super(type.getPrimitiveType().getMemoryLayout(), typeName, type.byteSize());
        this.members = List.copyOf(members);
        this.type = type;
        Assert(type.equals(CommonTypes.BindTypes.I32));
    }

    @Override
    public OperationAttr.Operation getOperation() {
        return new ValueBased<>(this, typeName, type);
    }

    public CommonTypes.BindTypes getType() {
        return type;
    }

    @Override
    public String toString() {
        return "EnumType{" +
               "members=" + members +
               ", typeName='" + typeName + '\'' +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EnumType enumType)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(members, enumType.members) && type == enumType.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), members, type);
    }

    public List<Member> getMembers() {
        return members;
    }
}
