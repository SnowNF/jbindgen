package processor;

import generator.PackagePath;
import generator.generators.*;
import generator.types.*;

import java.util.*;

public class ComponentUnit implements GenerateUnit {
    private final HashMap<TypeAttr.GenerationType, Optional<PackagePath>> allTypes = new HashMap<>();
    private final ArrayList<ConstGenerator.ConstValue> constValues = new ArrayList<>();
    private final HashSet<MacroGenerator.Macro> macros = new HashSet<>();
    private final ArrayList<FunctionPtrType> funcSymbols = new ArrayList<>();
    private final SymbolProviderType provider;
    private final boolean greedyGenerate;
    private final Utils.DestinationProvider dest;
    private final Utils.Filter filter;

    ComponentUnit(SymbolProviderType provider, Utils.DestinationProvider dest, Utils.Filter filter, boolean greedyGenerate) {
        this.filter = filter;
        this.dest = dest;
        this.provider = provider;
        this.greedyGenerate = greedyGenerate;
        allTypes.put(provider, Optional.of(provider.path()));
    }

    public void addType(TypeAttr.GenerationType generation) {
        allTypes.put(generation, Optional.empty());
    }

    public void addType(TypeAttr.GenerationType generation, PackagePath path) {
        allTypes.put(generation, Optional.of(path));
    }

    public void addConstValues(ArrayList<ConstGenerator.ConstValue> generation) {
        constValues.addAll(generation);
    }

    public void addMacros(HashSet<MacroGenerator.Macro> generation) {
        macros.addAll(generation);
    }

    public void addFunctionSymbols(ArrayList<FunctionPtrType> symbols) {
        funcSymbols.addAll(symbols);
    }

    @Override
    public List<Generator> makeInitialGenerators() {
        ArrayList<Generator> generators = new ArrayList<>();
        if (!funcSymbols.isEmpty()) {
            generators.add(new FuncSymbolGenerator(funcSymbols, dest.funcSymbols().path(), provider));
        }
        if (!macros.isEmpty()) {
            generators.add(new MacroGenerator(dest.macros().path(), macros));
        }
        if (!constValues.isEmpty()) {
            generators.add(new ConstGenerator(constValues, dest.constants().path()));
        }
        if (provider != null && !greedyGenerate) {
            generators.add(new SymbolProviderGenerator(provider));
        }
        if (greedyGenerate) {
            ArrayList<Generator> greedy = queryGenerators(allTypes.keySet());
            generators.addAll(greedy);
        }
        return generators;
    }

    @Override
    public Optional<PackagePath> queryPath(TypeAttr.GenerationType unlocatedType) {
        Optional<PackagePath> packagePath = allTypes.get(unlocatedType);
        if (packagePath == null)
            return Optional.empty();
        if (packagePath.isPresent()) {
            return packagePath;
        }
        var path = switch (unlocatedType) {
            case CommonTypes.BaseType baseType -> dest.common().path().close(baseType.typeName());
            case RefOnlyType refOnlyType -> dest.refOnly().path().close(refOnlyType.typeName());
            case SingleGenerationType singleGenerationType -> switch (singleGenerationType) {
                case EnumType enumType -> dest.enumerate().path().close(enumType.typeName());
                case FunctionPtrType functionPtrType -> dest.funcProtocol().path().close(functionPtrType.typeName());
                case ValueBasedType valueBasedType -> dest.valueBased().path().close(valueBasedType.typeName());
                case ArrayTypeNamed arrayTypeNamed -> dest.arrayNamed().path().close(arrayTypeNamed.typeName());
                case CommonTypes.BindTypes bindTypes -> throw new UnsupportedOperationException("Not supported yet.");
                case StructType structType -> dest.struct().path().close(structType.typeName());
            };
            case SymbolProviderType symbolProviderType -> symbolProviderType.path();
            case TaggedNamedType _, ArrayType _, PointerType _ -> throw new UnsupportedOperationException();
            case VoidType voidType -> dest.voidBased().path().close(voidType.typeName());
        };
        allTypes.put(unlocatedType, Optional.of(path));
        return Optional.of(path);
    }

    @Override
    public ArrayList<Generator> queryGenerators(Set<TypeAttr.GenerationType> unhandledTypes) {
        ArrayList<Generator> generators = new ArrayList<>();
        for (TypeAttr.GenerationType unhandledType : unhandledTypes) {
            if (!allTypes.containsKey(unhandledType))
                continue;
            var generator = switch (unhandledType) {
                case CommonTypes.BaseType baseType -> new CommonGenerator(baseType);
                case RefOnlyType refOnlyType -> new RefOnlyGenerator(refOnlyType);
                case SingleGenerationType singleGenerationType -> switch (singleGenerationType) {
                    case EnumType enumType -> new EnumGenerator(enumType);
                    case FunctionPtrType functionPtrType -> new FuncProtocolGenerator(functionPtrType);
                    case ValueBasedType valueBasedType -> new ValueBasedGenerator(valueBasedType);
                    case ArrayTypeNamed arrayTypeNamed -> new ArrayNamedGenerator(arrayTypeNamed);
                    case CommonTypes.BindTypes bindTypes ->
                            throw new UnsupportedOperationException("Not supported yet.");
                    case StructType structType -> new StructGenerator(structType);
                };
                case SymbolProviderType symbolProviderType -> new SymbolProviderGenerator(symbolProviderType);
                case TaggedNamedType _, ArrayType _, PointerType _ -> throw new UnsupportedOperationException("todo");
                case VoidType voidType -> new VoidBasedGenerator(voidType);
            };
            generators.add(generator);
        }
        return generators;
    }
}
