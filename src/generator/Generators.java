package generator;

import generator.generators.Generator;
import generator.types.TypeAttr;
import utils.CommonUtils;

import java.util.*;

public class Generators {
    public static final boolean DEBUG = false;
    public static final String DEBUG_NAME_APPEND = "Debug";

    public interface GeneratorProvider {
        ArrayList<Generator> queryGenerators(Set<TypeAttr.GenerationType> unhandledTypes);
    }

    public interface GenerationProvider {
        PackagePath queryPath(TypeAttr.GenerationType unlocatedType);
    }

    private final ArrayList<Generator> initialGenerators;
    private final GeneratorProvider generators;
    private final GenerationProvider locations;
    private final Writer writer = new Writer();

    /**
     * generate java files
     *
     * @param generators        provide other generations
     * @param initialGenerators initial generators invoke first
     */
    public Generators(List<Generator> initialGenerators, GeneratorProvider generators, GenerationProvider locations) {
        this.initialGenerators = new ArrayList<>(initialGenerators);
        this.generators = generators;
        this.locations = locations;
    }

    private final HashMap<TypeAttr.GenerationType, PackagePath> located = new HashMap<>();

    public void generate() {
        ArrayList<Generator> generators = new ArrayList<>(initialGenerators);
        HashSet<TypeAttr.GenerationType> generated = new HashSet<>();
        HashSet<TypeAttr.GenerationType> ungenerated = new HashSet<>();
        do {
            for (Generator generator : generators) {
                HashSet<TypeAttr.GenerationType> touched = new HashSet<>();
                Generator.GenerateResult generateResult = generator.generate(type -> {
                    touched.add(type);
                    return located.computeIfAbsent(type, locations::queryPath);
                }, writer);
                checkTouched(generator, touched, generateResult);
                generated.addAll(generateResult.generated());
                for (PackageManager value : generateResult.packages()) {
                    Set<TypeAttr.GenerationType> usedTypes = value.usedTypes();
                    ungenerated.addAll(usedTypes);
                }
                ungenerated.removeAll(generated);
            }
            generators.clear();
            ArrayList<Generator> newGenerators = this.generators.queryGenerators(ungenerated);
            generators.addAll(newGenerators);
            if (ungenerated.isEmpty()) break;
            if (generators.isEmpty()) {
                CommonUtils.Fail("Has ungenerated: %s, but generators is empty".formatted(ungenerated));
            }
        } while (true);
    }

    private static void checkTouched(Generator generator,
                                     HashSet<TypeAttr.GenerationType> touched,
                                     Generator.GenerateResult result) {
        // generated
        Set<TypeAttr.GenerationType> expected = new HashSet<>(result.generated());
        // used types
        for (PackageManager p : result.packages()) {
            expected.addAll(p.usedTypes());
        }
        // expected - touched
        Set<TypeAttr.GenerationType> missingInTouched = new HashSet<>(expected);
        missingInTouched.removeAll(touched);

        // touched - expected
        Set<TypeAttr.GenerationType> extraInTouched = new HashSet<>(touched);
        extraInTouched.removeAll(expected);

        if (!missingInTouched.isEmpty() || !extraInTouched.isEmpty()) {
            throw new IllegalStateException(
                    "For generator:" + generator + ", Touched mismatch:\n" +
                    "missingInTouched = " + missingInTouched + "\n" +
                    "extraInTouched   = " + extraInTouched
            );
        }
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

        private void write(PackagePath path, String content) {
            if (WRITING_PATHS.contains(path)) {
                throw new RuntimeException("Path " + path.getFilePath() + " already written");
            }
            WRITING_PATHS.add(path);
            Utils.write(path.getFilePath(), content);
        }
    }
}
