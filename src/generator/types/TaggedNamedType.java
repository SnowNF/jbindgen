package generator.types;

// used to generate tagged named interface
public record TaggedNamedType(String typeName, TypeAttr.NamedType referType)
        implements TypeAttr.TypeRefer, TypeAttr.GenerationType, TypeAttr.NamedType {

    @Override
    public String typeName() {
        return typeName;
    }
}
