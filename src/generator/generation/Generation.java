package generator.generation;

import generator.Dependency;
import generator.Generators;
import generator.TypePath;
import generator.types.TypeAttr;
import generator.types.TypeImports;

import java.util.Set;

public  interface Generation<T extends TypeAttr.GenerationType> {
    Set<TypePath<? extends T>> getImplTypes();

    TypeImports getDefineImportTypes();

    void generate(Dependency dependency, Generators.Writer writer);
}
