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
    public sealed interface SizedType extends OperationType permits ArrayType, ArrayTypeNamed, CommonTypes.BindTypes, EnumType, FunctionPtrType, PointerType, StructType, ValueBasedType {
        long byteSize();
    }

    public sealed interface OperationType extends GenerationType permits CommonTypes.BasicOperations, CommonTypes.ValueInterface, RefOnlyType, SizedType, VoidType {
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

    /**
     * types have generation
     */
    public sealed interface GenerationType permits CommonTypes.BaseType, SymbolProviderType, TaggedNamedType, OperationType {
        /**
         * get the type name in java
         *
         * @return the type name
         */
        String typeName();

        default String typeName(PackageManager packages, TypeAttr.NameType nameType) {
            return packages.useClass(this);
        }

        default String useTypeReplace() {
            return null;
        }
    }
}
