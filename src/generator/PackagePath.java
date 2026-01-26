package generator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;

import static utils.CommonUtils.Assert;

public class PackagePath {
    private final ArrayList<String> packages = new ArrayList<>();
    private final Path root;
    private final String className;

    public PackagePath(Path root) {
        this.root = root;
        className = null;
    }

    public PackagePath() {
        root = null;
        className = null;
    }

    private PackagePath(Path root, ArrayList<String> packages, String className) {
        this.root = root;
        this.packages.addAll(packages);
        this.className = className;
    }

    public PackagePath add(String packageName) {
        Assert(this.className == null);
        reqNonClassName();
        var pkg = new ArrayList<>(packages);
        if (packageName.contains(".")) {
            String[] split = packageName.split("\\.");
            for (String s : split) {
                Assert(Utils.isValidPackagePath(s), "invalid package path: " + packageName);
                pkg.add(s);
            }
        } else {
            Assert(Utils.isValidPackagePath(packageName), "invalid package path: " + packageName);
            pkg.add(packageName);
        }
        return new PackagePath(root, pkg, className);
    }

    public PackagePath end(String className) {
        Assert(this.className == null);
        Assert(Utils.isValidClassName(className), "invalid class name: " + className);
        if (Generators.DEBUG && !packages.getFirst().equals("java")) {
            ArrayList<String> strings = new ArrayList<>(packages);
            strings.add(className.toLowerCase());
            return new PackagePath(root, strings, className);
        }
        return new PackagePath(root, packages, className);
    }

    public PackagePath removeEnd() {
        Assert(this.className != null);
        return new PackagePath(root, packages, null);
    }

    public String makePackage() {
        return "package " + String.join(".", packages) + ";\n";
    }

    public String makeImport() {
        reqClassName();
        return "import " + String.join(".", packages) + "." + className + ";\n";
    }


    public String getClassName() {
        reqClassName();
        return className;
    }

    public Path getFilePath() {
        reqClassName();
        Path path = root;
        for (String p : packages) {
            path = path.resolve(p);
        }
        return path.resolve(className + ".java");
    }

    public PackagePath reqClassName() {
        if (className == null) {
            throw new IllegalArgumentException("need class name");
        }
        return this;
    }

    public boolean hasClassName() {
        return className != null;
    }

    public PackagePath reqNonClassName() {
        if (className != null) {
            throw new IllegalArgumentException("Class " + className + " is already defined, path is end");
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PackagePath that)) return false;
        return Objects.equals(packages, that.packages) && Objects.equals(root, that.root) && Objects.equals(className, that.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packages, root, className);
    }
}
