package generator.types;

public sealed interface SingleGenerationType
        extends TypeAttr.SizedType, TypeAttr.OperationType, TypeAttr.NamedType, TypeAttr.TypeRefer, TypeAttr.GenerationType
        permits ArrayTypeNamed, CommonTypes.BindTypes, EnumType, FunctionPtrType, StructType, ValueBasedType {
}
