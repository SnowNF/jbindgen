package generator.types;

import java.util.*;

public class TypeImports {
    private final Set<TypeAttr.GenerationType> imports = new HashSet<>();

    TypeImports(Set<TypeAttr.GenerationType> imports) {
        this.imports.addAll(imports);
    }

    TypeImports(TypeAttr.GenerationType imports) {
        this.imports.add(imports);
    }

    public TypeImports() {
    }

    public TypeImports addImport(TypeImports imports) {
        this.imports.addAll(imports.imports);
        return this;
    }

    TypeImports removeImport(TypeAttr.GenerationType type) {
        imports.remove(type);
        return this;
    }

    public TypeImports addUseImports(TypeAttr.TypeRefer type) {
        addImport(type.getUseImportTypes());
        return this;
    }

    public TypeImports addUseImports(Collection<TypeAttr.TypeRefer> type) {
        type.forEach(this::addUseImports);
        return this;
    }

    public Set<TypeAttr.GenerationType> getImports() {
        return Collections.unmodifiableSet(imports);
    }

    public TypeImports duplicate() {
        return new TypeImports(imports);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TypeImports imports1)) return false;
        return Objects.equals(imports, imports1.imports);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(imports);
    }
}
