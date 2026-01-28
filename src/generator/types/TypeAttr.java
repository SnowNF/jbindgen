package generator.types;

import generator.PackageManager;
import generator.types.operations.OperationAttr;

public class TypeAttr {
    private TypeAttr() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * types that have size, layout
     */
    public sealed interface SizedType permits SingleGenerationType, ArrayType, CommonTypes.Primitives, PointerType {
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
        RAW,
    }

    public sealed interface NamedType permits ArrayType, CommonTypes.BaseType, TaggedNamedType, PointerType, RefOnlyType, SingleGenerationType, SymbolProviderType, VoidType {

        /**
         * get the type name in java
         *
         * @return the type name
         */
        String typeName();

        default String typeName(PackageManager packages, TypeAttr.NameType nameType) {
            return packages.useClass((GenerationType) this);
        }
    }

    /**
     * types have generation
     */
    public sealed interface GenerationType permits ArrayType, CommonTypes.BaseType, PointerType, RefOnlyType, SingleGenerationType, SymbolProviderType, TaggedNamedType, VoidType {
        default String useTypeReplace() {
            return null;
        }
    }

    public sealed interface TypeRefer permits ArrayType, CommonTypes.BaseType, TaggedNamedType, PointerType, RefOnlyType, SingleGenerationType, SymbolProviderType, VoidType {
    }
}
