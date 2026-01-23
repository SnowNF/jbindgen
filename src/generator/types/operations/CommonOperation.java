package generator.types.operations;

import generator.types.CommonTypes;
import generator.types.MemoryLayouts;
import generator.types.TypeAttr;
import generator.types.TypeImports;

public interface CommonOperation {
    interface UpperType {
        String typeName(TypeAttr.NameType nameType);

        TypeImports typeImports();

        TypeAttr.OperationType typeOp();

        default boolean rejectWildcard() {
            return false;
        }
    }

    UpperType getUpperType();

    record End<T extends TypeAttr.NamedType & TypeAttr.TypeRefer & TypeAttr.OperationType>
            (T type, String typeName) implements UpperType {
        public End(T type) {
            this(type, type.typeName(TypeAttr.NameType.RAW));
        }

        @Override
        public String typeName(TypeAttr.NameType nameType) {
            return typeName;
        }

        @Override
        public TypeImports typeImports() {
            return new TypeImports().addUseImports(type);
        }

        @Override
        public TypeAttr.OperationType typeOp() {
            return type;
        }

    }

    record Reject<T extends TypeAttr.NamedType & TypeAttr.TypeRefer & TypeAttr.OperationType>(
            T t) implements UpperType {
        @Override
        public String typeName(TypeAttr.NameType nameType) {
            return t.typeName(TypeAttr.NameType.RAW);
        }

        @Override
        public TypeImports typeImports() {
            return new TypeImports();
        }

        @Override
        public TypeAttr.OperationType typeOp() {
            return t;
        }

        @Override
        public boolean rejectWildcard() {
            return true;
        }
    }

    record Warp<T extends TypeAttr.NamedType & TypeAttr.TypeRefer>(T outer, UpperType inner) implements UpperType {
        public Warp(T outer, CommonOperation inner) {
            this(outer, inner.getUpperType());
        }

        @Override
        public String typeName(TypeAttr.NameType nameType) {
            return switch (nameType) {
                case WILDCARD -> inner.rejectWildcard()
                        ? outer.typeName(TypeAttr.NameType.RAW) + "<?>"
                        : outer.typeName(TypeAttr.NameType.RAW) + "<? extends %s>".formatted(inner.typeName(nameType));
                case GENERIC -> outer.typeName(TypeAttr.NameType.RAW) + "<%s>".formatted(inner.typeName(nameType));
                case RAW -> outer.typeName(TypeAttr.NameType.RAW);
            };
        }

        @Override
        public TypeImports typeImports() {
            return inner.typeImports().addUseImports(outer);
        }

        @Override
        public TypeAttr.OperationType typeOp() {
            return ((TypeAttr.OperationType) outer);
        }
    }

    record Operation(String str, TypeImports imports) {
    }

    Operation makeOperation();

    static Operation makeStaticOperation(TypeAttr.TypeRefer type, String typeName) {
        return new Operation(typeName + ".OPERATIONS", new TypeImports().addUseImports(type));
    }

    static Operation makeVoidOperation() {
        return new Operation(CommonTypes.BasicOperations.Info.typeName(TypeAttr.NameType.RAW) + ".makeOperations()",
                new TypeImports().addUseImports(CommonTypes.BasicOperations.Info));
    }

    default MemoryLayouts makeDirectMemoryLayout() {
        return makeStaticMemoryLayout(makeOperation());
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
