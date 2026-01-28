package generator.types;

import generator.PackageManager;

import java.util.Objects;
import java.util.function.Function;

public sealed abstract class AbstractGenerationType
        implements SingleGenerationType permits EnumType, FunctionPtrType, ValueBasedType {
    protected final Function<PackageManager, MemoryLayouts> memoryLayout;
    protected final String typeName;
    protected final long byteSize;

    public AbstractGenerationType(Function<PackageManager, MemoryLayouts> memoryLayout, String typeName, long byteSize) {
        this.memoryLayout = memoryLayout;
        this.typeName = typeName;
        this.byteSize = byteSize;
    }

    @Override
    public MemoryLayouts getMemoryLayout(PackageManager packages) {
        return memoryLayout.apply(packages);
    }

    @Override
    public String typeName(TypeAttr.NameType nameType) {
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
