package generator.generation.generator;

import generator.Dependency;
import generator.generation.TaggedNamed;

public class TaggedNamedGenerator implements Generator {
    private final TaggedNamed namedType;
    private final Dependency dependency;

    public TaggedNamedGenerator(TaggedNamed namedType, Dependency dependency) {
        this.namedType = namedType;
        this.dependency = dependency;
    }

    @Override
    public void generate() {
        String out = namedType.getTypePkg().packagePath().makePackage();
        out += Generator.extractImports(namedType, dependency);
    }
}
