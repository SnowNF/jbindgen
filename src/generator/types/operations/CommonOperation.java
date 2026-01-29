package generator.types.operations;

import generator.PackageManager;
import generator.types.CommonTypes;
import generator.types.MemoryLayouts;
import generator.types.TypeAttr;

public interface CommonOperation {
    interface UpperType {
        String typeName(PackageManager packages);

        TypeAttr.OperationType type();

        default boolean rejectWildcard() {
            return false;
        }
    }

    UpperType getUpperType(PackageManager packages);

    record End<T extends TypeAttr.OperationType & TypeAttr.GenerationType>
            (T type, String typeName) implements UpperType {
        public End(T type, PackageManager packages) {
            this(type, type.typeName(packages, TypeAttr.NameType.RAW));
        }

        @Override
        public String typeName(PackageManager packages) {
            return typeName;
        }
    }

    record Reject<T extends TypeAttr.OperationType & TypeAttr.GenerationType>
            (T t) implements UpperType {
        @Override
        public String typeName(PackageManager packages) {
            return t.typeName(packages, TypeAttr.NameType.WILDCARD);
        }

        @Override
        public TypeAttr.OperationType type() {
            return t;
        }

        @Override
        public boolean rejectWildcard() {
            return true;
        }
    }

    record Warp<T extends TypeAttr.OperationType & TypeAttr.GenerationType>
            (T outer, UpperType inner) implements UpperType {
        @Override
        public String typeName(PackageManager packages) {
            final String outerRaw = outer.typeName(packages, TypeAttr.NameType.RAW);
            return inner.rejectWildcard()
                    ? outerRaw + "<?>"
                    : outerRaw + "<? extends %s>".formatted(inner.typeName(packages));
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
