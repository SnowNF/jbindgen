package generator;

import generator.generation.Generation;
import generator.types.TypeAttr;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static utils.CommonUtils.Assert;

public class Generator {
    public static final boolean DEBUG = true;
    public static final String DEBUG_NAME_APPEND = "Debug";

    public interface GenerationProvider {
        Generation<? extends TypeAttr.GenerationType> queryGeneration(TypeAttr.GenerationType type);
    }

    private final Set<Generation<?>> mustGenerate;

    private final Dependency dependency;
    private final GenerationProvider provider;

    /**
     * generate java files
     *
     * @param provider     provide other generations
     * @param mustGenerate must generate this, when missing symbols, will throw
     */
    public Generator(Set<Generation<?>> mustGenerate, GenerationProvider provider) {
        this.mustGenerate = mustGenerate;
        dependency = new Dependency()
                .addType(mustGenerate.stream().map(Generation::getImplTypes).flatMap(Set::stream).toList());
        this.provider = provider;
    }

    public void generate() {
        Set<Generation<?>> generations = new HashSet<>(mustGenerate);
        HashSet<TypeAttr.GenerationType> generated = new HashSet<>();
        do {
            HashSet<TypeAttr.GenerationType> reference = new HashSet<>();
            for (Generation<?> gen : generations) {
                generated.addAll(gen.getImplTypes().stream().map(TypePkg::type).toList());
                reference.addAll(gen.getDefineImportTypes().getImports());
            }
            reference.removeAll(generated);
            Set<Generation<?>> newGen = new HashSet<>();
            while (!reference.isEmpty()) {
                var type = reference.iterator().next();
                Generation<? extends TypeAttr.GenerationType> generation = provider.queryGeneration(type);
                Assert(generation != null, "missing generation: hash: " + type.hashCode() + " " + type);
                List<? extends TypeAttr.GenerationType> impl = generation.getImplTypes().stream().map(TypePkg::type).toList();
                Assert(impl.contains(type), "missing type generation:" + type);
                impl.forEach(reference::remove);
                newGen.add(generation);
                dependency.addType(generation.getImplTypes());
            }
            for (Generation<?> generation : generations) {
                generation.generate(dependency);
            }
            generations.clear();
            generations.addAll(newGen);
        } while (!generations.isEmpty());
    }
}
