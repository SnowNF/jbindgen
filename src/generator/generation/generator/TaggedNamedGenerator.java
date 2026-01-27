package generator.generation.generator;

import generator.Dependency;
import generator.Generators;

public class TaggedNamedGenerator implements Generator {
    private final Dependency dependency;

    public TaggedNamedGenerator( Dependency dependency) {
        this.dependency = dependency;
    }

    @Override
    public GenerateResult generate(Generators.GenerationProvider generators, Generators.Writer writer) {
        return null;
    }
}
