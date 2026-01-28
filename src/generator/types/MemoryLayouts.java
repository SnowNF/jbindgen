package generator.types;

import generator.PackageManager;
import generator.types.operations.CommonOperation;

import java.util.List;
import java.util.stream.Collectors;

public class MemoryLayouts {
    private final EvalString eval;

    public interface EvalString {
        String makeStr(PackageManager packages);
    }

    private MemoryLayouts(EvalString eval) {
        this.eval = eval;
    }

    public static MemoryLayouts withName(MemoryLayouts l, String name) {
        return new MemoryLayouts(p -> l.eval.makeStr(p) + ".withName(\"%s\")".formatted(name));
    }

    public static MemoryLayouts structLayout(List<MemoryLayouts> inner) {
        return new MemoryLayouts(p -> {
            String string = inner.stream().map(layouts -> layouts.eval.makeStr(p)).collect(Collectors.joining(", "));
            return p.useClass(CommonTypes.FFMTypes.MEMORY_LAYOUT) + ".structLayout(%s)".formatted(string);
        });
    }

    public static MemoryLayouts unionLayout(List<MemoryLayouts> inner) {
        return new MemoryLayouts(p -> {
            String string = inner.stream().map(layouts -> layouts.eval.makeStr(p)).collect(Collectors.joining(", "));
            return p.useClass(CommonTypes.FFMTypes.MEMORY_LAYOUT) + ".unionLayout(%s)".formatted(string);
        });
    }

    public static MemoryLayouts sequenceLayout(MemoryLayouts inner, long len) {
        return new MemoryLayouts(p ->
                p.useClass(CommonTypes.FFMTypes.MEMORY_LAYOUT)
                + ".sequenceLayout(%s, %s)".formatted(len, inner.eval.makeStr(p)));
    }

    public static MemoryLayouts paddingLayout(long byteSize) {
        return new MemoryLayouts(p -> p.useClass(CommonTypes.FFMTypes.MEMORY_LAYOUT)
                                      + ".paddingLayout(%s)".formatted(byteSize));
    }

    public static MemoryLayouts operationLayout(CommonOperation.Operation operation) {
        return new MemoryLayouts(p -> operation.str() + ".memoryLayout()");
    }

    public static final MemoryLayouts ADDRESS = new MemoryLayouts(p -> p.useClass(CommonTypes.FFMTypes.VALUE_LAYOUT) + ".ADDRESS");
    public static final MemoryLayouts JAVA_BYTE = new MemoryLayouts(p -> p.useClass(CommonTypes.FFMTypes.VALUE_LAYOUT) + ".JAVA_BYTE");
    public static final MemoryLayouts JAVA_BOOLEAN = new MemoryLayouts(p -> p.useClass(CommonTypes.FFMTypes.VALUE_LAYOUT) + ".JAVA_BOOLEAN");
    public static final MemoryLayouts JAVA_CHAR = new MemoryLayouts(p -> p.useClass(CommonTypes.FFMTypes.VALUE_LAYOUT) + ".JAVA_CHAR");
    public static final MemoryLayouts JAVA_SHORT = new MemoryLayouts(p -> p.useClass(CommonTypes.FFMTypes.VALUE_LAYOUT) + ".JAVA_SHORT");
    public static final MemoryLayouts JAVA_INT = new MemoryLayouts(p -> p.useClass(CommonTypes.FFMTypes.VALUE_LAYOUT) + ".JAVA_INT");
    public static final MemoryLayouts JAVA_LONG = new MemoryLayouts(p -> p.useClass(CommonTypes.FFMTypes.VALUE_LAYOUT) + ".JAVA_LONG");
    public static final MemoryLayouts JAVA_FLOAT = new MemoryLayouts(p -> p.useClass(CommonTypes.FFMTypes.VALUE_LAYOUT) + ".JAVA_FLOAT");
    public static final MemoryLayouts JAVA_DOUBLE = new MemoryLayouts(p -> p.useClass(CommonTypes.FFMTypes.VALUE_LAYOUT) + ".JAVA_DOUBLE");

    @Override
    public String toString() {
        return "MemoryLayouts{" +
               "eval=" + eval.makeStr(PackageManager.testPackageManager()) +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getMemoryLayout(PackageManager packages) {
        return eval.makeStr(packages);
    }
}
