package generator.types;

import java.util.Objects;

@Deprecated
public sealed abstract class AbstractGenerationType
        implements SingleGenerationType permits EnumType, FunctionPtrType, ValueBasedType {
    protected final String typeName;
    protected final long byteSize;

    public AbstractGenerationType(String typeName, long byteSize) {
        this.typeName = typeName;
        this.byteSize = byteSize;
    }

    @Override
    public String typeName() {
        return typeName;
    }

    @Override
    public TypeImports getUseImportTypes() {
        return new TypeImports(this);
    }

    @Override
    public String toString() {
        return "AbstractType{" + "typeName='" + typeName + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AbstractGenerationType that)) return false;
        return byteSize == that.byteSize && Objects.equals(typeName, that.typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName, byteSize);
    }

    @Override
    public long byteSize() {
        return byteSize;
    }
}
