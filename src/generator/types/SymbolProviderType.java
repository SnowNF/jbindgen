package generator.types;

import generator.PackagePath;

import java.util.Objects;

public record SymbolProviderType(String className, PackagePath path)
        implements TypeAttr.TypeRefer, TypeAttr.GenerationType, TypeAttr.NamedType {

    public SymbolProviderType {
        path.reqClosed();
        Objects.requireNonNull(className);
    }

    public SymbolProviderType(PackagePath path) {
        this(path.getClassName(), path);
    }

    @Override
    public String typeName() {
        return className;
    }
}
