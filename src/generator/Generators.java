package generator;

import generator.generation.Generation;
import generator.types.TypeAttr;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static utils.CommonUtils.Assert;

public class Generators {
    public static final boolean DEBUG = false;
    public static final String DEBUG_NAME_APPEND = "Debug";

    public interface GenerationProvider {
        Generation<? extends TypeAttr.GenerationType> queryGeneration(TypeAttr.GenerationType type);
    }

    private final Set<Generation<?>> mustGenerate;

    private final Dependency dependency;
    private final GenerationProvider provider;
    private final Writer writer = new Writer();

    /**
     * generate java files
     *
     * @param provider     provide other generations
     * @param mustGenerate must generate this, when missing symbols, will throw
     */
    public Generators(Set<Generation<?>> mustGenerate, GenerationProvider provider) {
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
                generation.generate(dependency, writer);
            }
            generations.clear();
            generations.addAll(newGen);
        } while (!generations.isEmpty());
    }

    public static class Writer {
        private final HashSet<PackagePath> WRITING_PATHS = new HashSet<>();

        public void write(PackageManager packages, String content) {
            String str = packages.getCurrPackage().makePackage();
            str += "\n";
            str += packages.makeImports();
            str += "\n";
            str += content;
            write(packages.getCurrPackage(), str);
        }

        public void write(PackagePath path, String content) {
            if (WRITING_PATHS.contains(path)) {
                throw new RuntimeException("Path " + path.getFilePath() + " already written");
            }
            WRITING_PATHS.add(path);
            Utils.write(path.getFilePath(), content);
        }
    }
}
