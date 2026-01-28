package generator.types;

import generator.types.operations.CommonOpOnly;
import generator.types.operations.OperationAttr;

import java.util.Objects;

public record VoidType(String typeName) implements
        TypeAttr.TypeRefer, TypeAttr.NamedType, TypeAttr.GenerationType, TypeAttr.OperationType {
    public VoidType {
        Objects.requireNonNull(typeName, "use VoidType.VOID instead");
    }

    public boolean realVoid() {
        return this.equals(VoidType.VOID);
    }

    public static final VoidType VOID = new VoidType("Void");

    @Override
    public TypeImports getUseImportTypes() {
        if (realVoid())
            return new TypeImports();
        return new TypeImports(this);
    }

    @Override
    public String typeName(TypeAttr.NameType nameType) {
        return typeName;
    }

    @Override
    public OperationAttr.Operation getOperation() {
        return new CommonOpOnly<>(this, realVoid()// no class will generate, inline it
        );
    }
}
