package processor;

import generator.PackagePath;
import generator.generators.Generator;
import generator.types.TypeAttr;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface GenerateUnit {
    List<Generator> makeInitialGenerators();

    Optional<PackagePath> queryPath(TypeAttr.GenerationType unlocatedType);

    ArrayList<Generator> queryGenerators(Set<TypeAttr.GenerationType> unhandledTypes);
}
