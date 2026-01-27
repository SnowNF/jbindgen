package generator.generation.generator;

import generator.Generators;
import generator.PackageManager;
import generator.types.TypeAttr;

public class EmptyGenerator implements Generator {

    private final TypeAttr.GenerationType type;

    public EmptyGenerator(TypeAttr.GenerationType type) {
        this.type = type;
    }

    @Override
    public GenerateResult generate(Generators.GenerationProvider locations, Generators.Writer writer) {
        return new GenerateResult(new PackageManager(locations, type), type);
    }
}
