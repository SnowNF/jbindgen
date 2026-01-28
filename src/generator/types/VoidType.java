package generator.types;

import generator.PackageManager;
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
    public String typeName() {
        return typeName;
    }

    @Override
    public String typeName(PackageManager packages, TypeAttr.NameType nameType) {
        if (realVoid()) {
            return typeName;
        }
        return TypeAttr.NamedType.super.typeName(packages, nameType);
    }

    @Override
    public OperationAttr.Operation getOperation() {
        return new CommonOpOnly<>(this, realVoid()// no class will generate, inline it
        );
    }

    @Override
    public String useTypeReplace() {
        if (realVoid()) {
            return typeName;
        }
        return null;
    }
}
