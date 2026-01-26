package generator.generation.generator;

import generator.Dependency;
import generator.generation.TaggedNames;

public class TaggedNamedGenerator implements Generator {
    private final TaggedNames namedType;
    private final Dependency dependency;

    public TaggedNamedGenerator(TaggedNames namedType, Dependency dependency) {
        this.namedType = namedType;
        this.dependency = dependency;
    }

    @Override
    public void generate() {
    }
}
