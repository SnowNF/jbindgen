package generator.types;

import generator.types.operations.OperationAttr;

public class TypeAttr {
    private TypeAttr() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * types that have size, layout
     */
    public sealed interface SizedType permits SingleGenerationType, ArrayType, CommonTypes.Primitives, PointerType {
        /**
         * get the string of {@link java.lang.foreign.MemoryLayout}
         *
         * @return the presentation of the MemoryLayout and type used
         */
        MemoryLayouts getMemoryLayout();

        long byteSize();
    }

    public sealed interface OperationType permits SingleGenerationType, ArrayType, CommonTypes.BasicOperations, CommonTypes.ValueInterface, PointerType, RefOnlyType, VoidType {
        /**
         * ways to construct, destruct the type
         */
        OperationAttr.Operation getOperation();
    }

    public enum NameType {
        WILDCARD,
        GENERIC,
        RAW
    }

    public sealed interface NamedType permits ArrayType, CommonTypes.BaseType, TaggedNamedType, PointerType, RefOnlyType, SingleGenerationType, SymbolProviderType, VoidType {

        /**
         * get the type name in java
         *
         * @return the type name
         */
        String typeName(TypeAttr.NameType nameType);
    }

    /**
     * types have generation
     */
    public sealed interface GenerationType permits ArrayType, CommonTypes.BaseType, PointerType, RefOnlyType, SingleGenerationType, SymbolProviderType, TaggedNamedType, VoidType {
    }

    public sealed interface TypeRefer permits ArrayType, CommonTypes.BaseType, TaggedNamedType, PointerType, RefOnlyType, SingleGenerationType, SymbolProviderType, VoidType {
        /**
         * @return the types when use this type
         */
        TypeImports getUseImportTypes();

        /**
         * @return the types used when define this type
         * @implNote do not include self
         */
        TypeImports getDefineImportTypes();
    }
}
