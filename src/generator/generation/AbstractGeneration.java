package generator.generation;

import generator.PackagePath;
import generator.TypePkg;
import generator.types.TypeAttr;
import generator.types.TypeImports;

import java.util.Objects;
import java.util.Set;

public sealed abstract class AbstractGeneration
        <T extends TypeAttr.GenerationType & TypeAttr.NamedType & TypeAttr.TypeRefer>
        implements Generation<T> permits
        ArrayNamed, Common, Enumerate, FuncPointer, RefOnly, Structure, SymbolProvider, ValueBased, VoidBased {
    protected final TypePkg<? extends T> typePkg;

    public AbstractGeneration(PackagePath packagePath, T type) {
        typePkg = new TypePkg<>(type, packagePath.close(type.typeName(TypeAttr.NameType.RAW)));
    }


    public TypePkg<? extends T> getTypePkg() {
        return typePkg;
    }

    @Override
    public Set<TypePkg<? extends T>> getImplTypes() {
        return Set.of(typePkg);
    }

    @Override
    public TypeImports getDefineImportTypes() {
        return typePkg.type().getDefineImportTypes();
    }

    @Override
    public String toString() {
        return "AbstractGeneration{" +
               "typePkg=" + typePkg +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AbstractGeneration<?> that)) return false;
        return Objects.equals(typePkg, that.typePkg);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(typePkg);
    }
}
