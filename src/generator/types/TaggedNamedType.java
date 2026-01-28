package generator.types;

// used to generate tagged named interface
public record TaggedNamedType(String typeName, TypeAttr.GenerationType referType)
        implements TypeAttr.GenerationType {

    @Override
    public String typeName() {
        return typeName;
    }
}
