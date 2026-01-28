package generator.types;

import generator.types.operations.OperationAttr;
import generator.types.operations.ValueBased;

import java.util.List;
import java.util.Objects;

import static utils.CommonUtils.Assert;

public final class EnumType implements SingleGenerationType {
    /**
     * the enum member
     */
    public record Member(long val, String name) {

    }

    private final String typeName;
    private final List<Member> members;
    private final CommonTypes.BindTypes type;

    public EnumType(CommonTypes.BindTypes type, String typeName, List<Member> members) {
        this.typeName = typeName;
        this.members = List.copyOf(members);
        this.type = type;
        Assert(type.equals(CommonTypes.BindTypes.I32));
    }

    @Override
    public OperationAttr.Operation getOperation() {
        return new ValueBased<>(this, type);
    }

    public CommonTypes.BindTypes getType() {
        return type;
    }

    @Override
    public String typeName() {
        return typeName;
    }

    @Override
    public long byteSize() {
        return type.byteSize();
    }

    @Override
    public TypeImports getUseImportTypes() {
        return new TypeImports(this);
    }

    @Override
    public String toString() {
        return "EnumType{" +
               "typeName='" + typeName + '\'' +
               ", members=" + members +
               ", type=" + type +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EnumType enumType)) return false;
        return Objects.equals(typeName, enumType.typeName) && Objects.equals(members, enumType.members) && type == enumType.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName, members, type);
    }

    public List<Member> getMembers() {
        return members;
    }
}
