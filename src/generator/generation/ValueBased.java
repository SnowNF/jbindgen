package generator.generation;

import generator.Dependency;
import generator.Generators;
import generator.PackagePath;
import generator.generation.generator.ValueBasedGenerator;
import generator.types.ValueBasedType;

public final class ValueBased extends AbstractGeneration<ValueBasedType> {
    public ValueBased(PackagePath packagePath, ValueBasedType type) {
        super(packagePath, type);
    }

    @Override
    public void generate(Dependency dependency, Generators.Writer writer) {
        new ValueBasedGenerator(this, dependency, writer).generate();
    }
}
