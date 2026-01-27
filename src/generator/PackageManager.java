package generator;

import generator.types.TypeAttr;
import generator.types.TypeImports;
import utils.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class PackageManager {
    private final Generators.GenerationProvider location;
    private final PackagePath currPackage;
    private final HashMap<String, TypeAttr.GenerationType> imports = new HashMap<>();
    private final HashSet<TypeAttr.GenerationType> used = new HashSet<>();

    public PackageManager(Generators.GenerationProvider location, TypeAttr.GenerationType type) {
        this(location, location.queryPath(type));
    }

    public PackageManager(Generators.GenerationProvider location, PackagePath currPackage) {
        this.location = location;
        this.currPackage = currPackage;
        currPackage.reqClosed();
    }

    public Set<TypeAttr.GenerationType> usedTypes() {
        HashSet<TypeAttr.GenerationType> ret = new HashSet<>(used);
        ret.addAll(imports.values());
        return ret;
    }

    public void addImport(TypeImports imports) {
        used.addAll(imports.getImports());
        for (TypeAttr.GenerationType anImport : imports.getImports()) {
            this.imports.put(((TypeAttr.NamedType) anImport).typeName(TypeAttr.NameType.RAW), anImport);
        }
    }

    public String useClass(TypeAttr.GenerationType type) {
        used.add(type);
        PackagePath packagePath = location.queryPath(type);
        String rootClassName = packagePath.getRootClassName();
        TypeAttr.GenerationType generationType = imports.get(rootClassName);
        if (generationType != null && !generationType.equals(type)) {
            return packagePath.getFullClassName();
        }
        imports.put(rootClassName, type);
        return packagePath.getClassName();
    }

    public String useTypePrefix(TypeAttr.GenerationType type) {
        used.add(type);
        PackagePath packagePath = location.queryPath(type);
        String rootClassName = packagePath.getRootClassName();
        String lastClassName = packagePath.getLastClassName();
        String typeName = ((TypeAttr.NamedType) type).typeName(TypeAttr.NameType.RAW);
        if (!lastClassName.equals(typeName)) {
            CommonUtils.shouldNotReachHere();
        }
        ArrayList<String> path = new ArrayList<>();
        if (imports.containsKey(rootClassName)) {
            // already imports same class name
            path.addAll(packagePath.getPackagePath());
        } else {
            imports.put(rootClassName, type);
        }
        path.addAll(packagePath.getPrefixClassPath());
        if (path.isEmpty()) {
            return "";
        }
        return String.join(".", path) + ".";
    }

    public String useType(TypeAttr.GenerationType type, TypeAttr.NameType name) {
        return useTypePrefix(type) + ((TypeAttr.NamedType) type).typeName(name);
    }

    public String makeImports() {
        Set<String> imports = new HashSet<>();
        for (TypeAttr.GenerationType type : this.imports.values()) {
            PackagePath packagePath = location.queryPath(type);
            if (packagePath.samePackage(currPackage)) {
                continue;
            }
            imports.add(packagePath.makeImport());
        }
        ArrayList<String> sort = new ArrayList<>(imports);
        sort.sort(String::compareTo);
        return String.join("", sort);
    }

    public PackagePath getCurrPackage() {
        return currPackage;
    }

    public String getClassName() {
        return currPackage.getClassName();
    }
}
