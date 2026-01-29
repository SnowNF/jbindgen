package processor;

import generator.PackagePath;
import generator.generators.*;
import generator.types.*;
import utils.CommonUtils;

import java.util.*;

public class ComponentUnit implements GenerateUnit {
    private final HashMap<TypeAttr.GenerationType, Optional<String>> allTypes = new HashMap<>();
    private final LinkedHashMap<ConstGenerator.ConstValue, Optional<String>> constValues = new LinkedHashMap<>();
    private final LinkedHashMap<MacroGenerator.Macro, Optional<String>> macros = new LinkedHashMap<>();
    private final LinkedHashMap<FunctionPtrType, Optional<String>> funcSymbols = new LinkedHashMap<>();
    private final SymbolProviderType provider;
    private final boolean greedyGenerate;
    private final Utils.DestinationProvider dest;
    private final Utils.Filter filter;

    ComponentUnit(SymbolProviderType provider, Utils.DestinationProvider dest, Utils.Filter filter, boolean greedyGenerate) {
        this.filter = filter;
        this.dest = dest;
        this.provider = provider;
        this.greedyGenerate = greedyGenerate;
        allTypes.put(provider, Optional.empty());
    }

    public void addType(TypeAttr.GenerationType generation, String header) {
        allTypes.put(generation, Optional.ofNullable(header));
    }

    public void addConstValues(Map<ConstGenerator.ConstValue, Optional<String>> generation) {
        generation.forEach((key, value) -> {
            if (filter.testConstValues(key, value)) constValues.put(key, value);
        });
    }

    public void addMacros(Map<MacroGenerator.Macro, Optional<String>> generation) {
        generation.forEach((key, value) -> {
            if (filter.testMacros(key, value)) macros.put(key, value);
        });
    }

    public void addFunctionSymbols(Map<FunctionPtrType, Optional<String>> generation) {
        generation.forEach((key, value) -> {
            if (filter.testFuncSymbols(key, value)) funcSymbols.put(key, value);
        });
    }

    @Override
    public List<Generator> makeInitialGenerators() {
        ArrayList<Generator> generators = new ArrayList<>();
        if (!funcSymbols.isEmpty()) {
            generators.add(new FuncSymbolGenerator(funcSymbols.keySet(), dest.funcSymbols().path(), provider));
        }
        if (!macros.isEmpty()) {
            generators.add(new MacroGenerator(dest.macros().path(), macros.keySet()));
        }
        if (!constValues.isEmpty()) {
            generators.add(new ConstGenerator(constValues.keySet(), dest.constants().path()));
        }
        if (provider != null) {
            generators.add(new SymbolProviderGenerator(provider));
        }
        if (greedyGenerate) {
            ArrayList<Generator> greedy = new ArrayList<>();
            allTypes.forEach((key, value) -> {
                boolean r = switch (key) {
                    case CommonTypes.BaseType _, SymbolProviderType _, TaggedNamedType _ -> false;
                    case TypeAttr.OperationType operationType -> switch (operationType) {
                        case RefOnlyType refOnlyType -> filter.testRefOnly(refOnlyType, value);
                        case TypeAttr.SizedType sizedType -> switch (sizedType) {
                            case ArrayType arrayType -> throw CommonUtils.shouldNotReachHere();
                            case ArrayTypeNamed arrayTypeNamed -> filter.testArrayNamed(arrayTypeNamed, value);
                            case CommonTypes.BindTypes bindTypes -> throw CommonUtils.shouldNotReachHere();
                            case EnumType enumType -> filter.testEnumerate(enumType, value);
                            case FunctionPtrType functionPtrType -> filter.testFuncPointer(functionPtrType, value);
                            case PointerType pointerType -> throw CommonUtils.shouldNotReachHere();
                            case StructType structType -> filter.testStructure(structType, value);
                            case ValueBasedType valueBasedType -> filter.testValueBased(valueBasedType, value);
                        };
                        case VoidType voidType -> filter.testVoidBased(voidType, value);
                        default -> throw CommonUtils.shouldNotReachHere();
                    };
                };
                if (r) {
                    greedy.addAll(queryGenerators(Set.of(key)));
                }
            });
            generators.addAll(greedy);
        }
        return generators;
    }

    @Override
    public Optional<PackagePath> queryPath(TypeAttr.GenerationType unlocatedType) {
        var packagePath = allTypes.get(unlocatedType);
        if (packagePath == null)
            return Optional.empty();
        var path = switch (unlocatedType) {
            case CommonTypes.BaseType baseType -> dest.common().path().close(baseType.typeName());
            case RefOnlyType refOnlyType -> dest.refOnly().path().close(refOnlyType.typeName());
            case EnumType enumType -> dest.enumerate().path().close(enumType.typeName());
            case FunctionPtrType functionPtrType -> dest.funcProtocol().path().close(functionPtrType.typeName());
            case ValueBasedType valueBasedType -> dest.valueBased().path().close(valueBasedType.typeName());
            case ArrayTypeNamed arrayTypeNamed -> dest.arrayNamed().path().close(arrayTypeNamed.typeName());
            case StructType structType -> dest.struct().path().close(structType.typeName());
            case SymbolProviderType symbolProviderType -> symbolProviderType.path();
            case TaggedNamedType _, ArrayType _, PointerType _ -> throw new UnsupportedOperationException();
            case VoidType voidType -> dest.voidBased().path().close(voidType.typeName());
        };
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
                case EnumType enumType -> new EnumGenerator(enumType);
                case FunctionPtrType functionPtrType -> new FuncProtocolGenerator(functionPtrType);
                case ValueBasedType valueBasedType -> new ValueBasedGenerator(valueBasedType);
                case ArrayTypeNamed arrayTypeNamed -> new ArrayNamedGenerator(arrayTypeNamed);
                case StructType structType -> new StructGenerator(structType);
                case SymbolProviderType symbolProviderType -> new SymbolProviderGenerator(symbolProviderType);
                case TaggedNamedType _, ArrayType _, PointerType _ -> throw new UnsupportedOperationException("todo");
                case VoidType voidType -> new VoidBasedGenerator(voidType);
            };
            generators.add(generator);
        }
        return generators;
    }
}
