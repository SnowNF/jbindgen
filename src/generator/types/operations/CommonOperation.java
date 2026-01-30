package generator.types.operations;

import generator.PackageManager;
import generator.types.CommonTypes;
import generator.types.MemoryLayouts;
import generator.types.TypeAttr;

public interface CommonOperation {
    interface UpperType {
        String typeName(PackageManager packages);

        String innerName(PackageManager packages);

        TypeAttr.OperationType type();
    }

    UpperType getUpperType(PackageManager packages);

    record End<T extends TypeAttr.OperationType>
            (T type, String typeName, boolean wildcard) implements UpperType {
        public End(T type, PackageManager packages, boolean wildcard) {
            this(type, packages.useClass(type), wildcard);
        }

        @Override
        public String typeName(PackageManager packages) {
            return typeName;
        }

        @Override
        public String innerName(PackageManager packages) {
            return wildcard ? ("? extends " + typeName) : typeName;
        }
    }

    record Reject<T extends TypeAttr.OperationType>
            (T t) implements UpperType {
        @Override
        public String typeName(PackageManager packages) {
            return packages.useClass(t);
        }

        @Override
        public String innerName(PackageManager packages) {
            return "?";
        }

        @Override
        public TypeAttr.OperationType type() {
            return t;
        }
    }

    record Warp<T extends TypeAttr.OperationType>
            (T outer, UpperType inner) implements UpperType {
        @Override
        public String typeName(PackageManager packages) {
            final String outerRaw = packages.useClass(outer);
            return outerRaw + "<%s>".formatted(inner.innerName(packages));
        }

        @Override
        public String innerName(PackageManager packages) {
            final String outerRaw = packages.useClass(outer);
            var name = (outerRaw + "<%s>".formatted(inner.innerName(packages)));
            return "? extends " + name;
        }

        @Override
        public TypeAttr.OperationType type() {
            return outer;
        }
    }

    record Operation(String str) {
    }

    Operation makeOperation(PackageManager packages);

    static Operation makeStaticOperation(PackageManager packages, TypeAttr.GenerationType type) {
        return makeStaticOperation(packages.useClass(type));
    }

    static Operation makeStaticOperation(String typeName) {
        return new Operation(typeName + ".OPERATIONS");
    }

    static Operation makeVoidOperation(PackageManager packages) {
        return new Operation(packages.useClass(CommonTypes.BasicOperations.Info) + ".makeOperations()");
    }

    default MemoryLayouts makeMemoryLayout(PackageManager packages) {
        return makeStaticMemoryLayout(makeOperation(packages));
    }

    static MemoryLayouts makeStaticMemoryLayout(Operation operation) {
        return MemoryLayouts.operationLayout(operation);
    }

    static MemoryLayouts makeStaticMemoryLayout(MemoryLayouts memoryLayouts) {
        return memoryLayouts;
    }

    enum AllocatorType {
        NONE,
        STANDARD, // return Single<T>, need explicit SegmentAllocator
        ON_HEAP // return int128, on heap struct etc.
    }

    default AllocatorType getAllocatorType() {
        return AllocatorType.NONE;
    }
}
