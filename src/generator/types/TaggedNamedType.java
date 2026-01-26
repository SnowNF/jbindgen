package generator.types;

// used to generate tagged interface
public record TaggedNamedType(String typeName) implements TypeAttr.TypeRefer, TypeAttr.GenerationType, TypeAttr.NamedType {
    @Override
    public TypeImports getUseImportTypes() {
        return new TypeImports(this);
    }

    @Override
    public TypeImports getDefineImportTypes() {
        return new TypeImports();
    }

    @Override
    public String typeName(TypeAttr.NameType nameType) {
        return typeName;
    }
}
