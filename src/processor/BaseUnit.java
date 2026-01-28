package processor;

import generator.PackagePath;
import generator.generators.CommonGenerator;
import generator.generators.EmptyGenerator;
import generator.generators.Generator;
import generator.types.ArrayType;
import generator.types.CommonTypes;
import generator.types.PointerType;
import generator.types.TypeAttr;

import java.util.*;

class BaseUnit implements GenerateUnit {
    private final Utils.DestinationProvider dest;
    private final HashMap<TypeAttr.GenerationType, PackagePath> allTypes = new HashMap<>();

    BaseUnit(Utils.DestinationProvider dest) {
        this.dest = dest;
        generators = new ArrayList<>();
    }

    final ArrayList<Generator> generators;

    public void addAllType(List<? extends CommonTypes.BaseType> generation) {
        for (CommonTypes.BaseType baseType : generation) {
            allTypes.put(baseType, dest.common().path().close(baseType.typeName()));
        }
        generators.add(new CommonGenerator(generation));
    }

    public void addAllType(Map<? extends CommonTypes.BaseType, PackagePath> generations) {
        allTypes.putAll(generations);
        generators.add(new CommonGenerator(generations.keySet().stream().toList()));
    }

    @Override
    public ArrayList<Generator> queryGenerators(Set<TypeAttr.GenerationType> unhandledTypes) {
        ArrayList<Generator> generators = new ArrayList<>();
        for (TypeAttr.GenerationType unhandledType : unhandledTypes) {
            switch (unhandledType) {
                case CommonTypes.BaseType baseType -> generators.add(new CommonGenerator(baseType));
                case PointerType _, ArrayType _ -> generators.add(new EmptyGenerator(unhandledType));
                default -> {
                }
            }
        }
        return generators;
    }

    @Override
    public Optional<PackagePath> queryPath(TypeAttr.GenerationType unlocatedType) {
        var packagePath = allTypes.get(unlocatedType);
        if (packagePath != null)
            return Optional.of(packagePath);
        var path = switch (unlocatedType) {
            case ArrayType a -> dest.common().path().close(a.typeName());
            case PointerType p -> dest.common().path().close(p.typeName());
            default -> null;
        };
        return Optional.ofNullable(path);
    }

    @Override
    public List<Generator> makeInitialGenerators() {
        return generators;
    }
}
