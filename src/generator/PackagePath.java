package generator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;

import static utils.CommonUtils.Assert;

public class PackagePath {
    private final ArrayList<String> packages = new ArrayList<>();
    private final Path root;
    private final ArrayList<String> classNames = new ArrayList<>();
    private final boolean closed;

    public PackagePath(Path root) {
        this.root = root;
        closed = false;
    }

    public PackagePath() {
        root = null;
        closed = false;
    }

    private PackagePath(Path root, ArrayList<String> packages, ArrayList<String> className, boolean closed) {
        this.root = root;
        this.packages.addAll(packages);
        this.classNames.addAll(className);
        this.closed = closed;
    }

    private PackagePath(Path root, ArrayList<String> packages, ArrayList<String> className) {
        this.root = root;
        this.packages.addAll(packages);
        this.classNames.addAll(className);
        this.closed = false;
    }

    // ----- modify -----//
    public PackagePath add(String packageName) {
        requireNonClose();
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
        return new PackagePath(root, pkg, classNames);
    }

    public PackagePath close(String className) {
        requireNonClose();
        Assert(Utils.isValidClassName(className), "invalid class name: " + className);
        ArrayList<String> strings = new ArrayList<>(packages);
        if (Generators.DEBUG && !packages.getFirst().equals("java")) {
            strings.add(className.toLowerCase());
        }
        ArrayList<String> classes = new ArrayList<>(classNames);
        classes.add(className);
        return new PackagePath(root, strings, classes, true);
    }

    public PackagePath open() {
        reqClosed(); // req closed
        return new PackagePath(root, packages, classNames, false);
    }


    public PackagePath className(String className) {
        requireNonClose();
        Assert(Utils.isValidClassName(className), "invalid class name: " + className);
        ArrayList<String> classes = new ArrayList<>(classNames);
        classes.add(className);
        return new PackagePath(root, packages, classes);
    }

    public PackagePath removeClasses() {
        Assert(!this.classNames.isEmpty());
        return new PackagePath(root, packages, new ArrayList<>());
    }

    // ----- infos -----//
    public String makePackage() {
        return "package " + String.join(".", packages) + ";\n";
    }

    public String makeImport() {
        reqClosed();
        return "import " + String.join(".", packages) + "." + String.join(".", classNames) + ";\n";
    }

    public boolean samePackage(PackagePath that) {
        return that.packages.equals(packages);
    }

    // root class and inner class
    public String getClassName() {
        reqClosed();
        return String.join(".", classNames);
    }

    public String getFullClassName() {
        reqClosed();
        return String.join(".", packages) + "." + String.join(".", classNames);
    }

    public ArrayList<String> getPrefixClassPath() {
        reqClosed();
        ArrayList<String> names = new ArrayList<>(classNames);
        names.removeLast();
        return names;
    }

    public ArrayList<String> getPackagePath() {
        reqClosed();
        return new ArrayList<>(packages);
    }

    public String getRootClassName() {
        reqClosed();
        return classNames.getFirst();
    }

    public String getLastClassName() {
        reqClosed();
        return classNames.getLast();
    }

    public Path getFilePath() {
        reqClosed();
        Path path = root;
        for (String p : packages) {
            path = path.resolve(p);
        }
        return path.resolve(classNames.getFirst() + ".java");
    }

    // ----- utilities -----//
    public PackagePath reqClosed() {
        if (!closed) {
            throw new IllegalArgumentException("need class name");
        }
        return this;
    }

    public void requireNonClose() {
        if (closed) {
            throw new IllegalStateException("Package path has been closed, package: " + packages + ", class: " + classNames);
        }
    }

    public PackagePath reqNonClassName() {
        if (!classNames.isEmpty()) {
            throw new IllegalArgumentException("Class " + classNames + " is already defined");
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PackagePath that)) return false;
        return closed == that.closed && Objects.equals(packages, that.packages) && Objects.equals(root, that.root) && Objects.equals(classNames, that.classNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packages, root, classNames, closed);
    }
}
