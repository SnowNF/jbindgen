package generator.generation.generator;

import generator.Generators;
import generator.PackageManager;
import generator.types.TypeAttr;

import java.util.List;

public interface Generator {
    /**
     * @param packages  used types
     * @param generated generated types
     */
    record GenerateResult(List<PackageManager> packages, List<? extends TypeAttr.GenerationType> generated) {
        public GenerateResult(PackageManager packages, TypeAttr.GenerationType type) {
            this(List.of(packages), List.of(type));
        }
    }

    GenerateResult generate(Generators.GenerationProvider generators, Generators.Writer writer);
}
