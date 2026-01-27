package generator.types;

public sealed interface SingleGenerationType
        extends TypeAttr.SizedType, TypeAttr.OperationType, TypeAttr.NamedType, TypeAttr.TypeRefer, TypeAttr.GenerationType
        permits AbstractGenerationType, ArrayTypeNamed, CommonTypes.BindTypes, StructType {

    default TaggedNamedType makeTaggedNamedType() {
        return new TaggedNamedType(typeName(TypeAttr.NameType.RAW), this);
    }
}
