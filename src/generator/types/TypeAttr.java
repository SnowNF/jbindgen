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
    public sealed interface SizedType permits ArrayType, ArrayTypeNamed, CommonTypes.BindTypes, CommonTypes.Primitives, EnumType, FunctionPtrType, PointerType, StructType, ValueBasedType {
        long byteSize();
    }

    public sealed interface OperationType permits ArrayType, ArrayTypeNamed, CommonTypes.BasicOperations, CommonTypes.BindTypes, CommonTypes.ValueInterface, EnumType, FunctionPtrType, PointerType, RefOnlyType, StructType, ValueBasedType, VoidType {
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

    public sealed interface NamedType permits ArrayType, ArrayTypeNamed, CommonTypes.BaseType, CommonTypes.BindTypes, EnumType, FunctionPtrType, PointerType, RefOnlyType, StructType, SymbolProviderType, TaggedNamedType, ValueBasedType, VoidType {

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
    public sealed interface GenerationType permits ArrayType, ArrayTypeNamed, CommonTypes.BaseType, CommonTypes.BindTypes, EnumType, FunctionPtrType, PointerType, RefOnlyType, StructType, SymbolProviderType, TaggedNamedType, ValueBasedType, VoidType {
        default String useTypeReplace() {
            return null;
        }
    }

    public sealed interface TypeRefer permits ArrayType, ArrayTypeNamed, CommonTypes.BaseType, CommonTypes.BindTypes, EnumType, FunctionPtrType, PointerType, RefOnlyType, StructType, SymbolProviderType, TaggedNamedType, ValueBasedType, VoidType {
    }
}
