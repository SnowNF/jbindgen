package generator;

import generator.types.TypeAttr;

import java.util.*;

import static utils.CommonUtils.Assert;

public class Dependency {
    private final HashMap<TypeAttr.GenerationType, PackagePath> allGenerations = new HashMap<>();

    public Dependency() {
    }

    public Dependency addType(Collection<? extends TypePkg<?>> typePkgs) {
        for (TypePkg<?> selfType : typePkgs) {
            allGenerations.put(selfType.type(), selfType.packagePath());
        }
        return this;
    }

    public String getTypeImports(Set<TypeAttr.GenerationType> types) {
        Set<String> imports = new HashSet<>();
        for (TypeAttr.GenerationType type : types) {
            imports.add(getTypePackagePath(type).makeImport());
        }
        ArrayList<String> sort = new ArrayList<>(imports);
        sort.sort(String::compareTo);
        return String.join("", sort);
    }

    public PackagePath getTypePackagePath(TypeAttr.GenerationType type) {
        Assert(allGenerations.containsKey(type), "missing type generation: " + type);
        return allGenerations.get(type);
    }
}
