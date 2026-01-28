package processor;

import analyser.*;
import analyser.types.Enum;
import analyser.types.Type;
import generator.Generators;
import generator.PackagePath;
import generator.generators.*;
import generator.types.*;

import java.util.*;

public class Processor {
    private static class GenerateUnit {
        private final HashMap<TypeAttr.GenerationType, Optional<PackagePath>> allTypes = new HashMap<>();
        private final ArrayList<ConstGenerator.ConstValue> constValues = new ArrayList<>();
        private final HashSet<MacroGenerator.Macro> macros = new HashSet<>();
        private final ArrayList<FunctionPtrType> funcSymbols = new ArrayList<>();
        private final SymbolProviderType provider;
        private final Utils.DestinationProvider dest;
        private final Utils.Filter filter;

        GenerateUnit(SymbolProviderType provider, Utils.DestinationProvider dest, Utils.Filter filter) {
            this.filter = filter;
            this.dest = dest;
            this.provider = provider;
            allTypes.put(provider, Optional.of(provider.path()));
        }

        GenerateUnit(Utils.DestinationProvider dest, Utils.Filter filter) {
            this.filter = filter;
            this.dest = dest;
            this.provider = null;
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

        List<Generator> makeInitialGenerators() {
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
            if (provider != null) {
                generators.add(new SymbolProviderGenerator(provider));
            }
            return generators;
        }

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
                    case FunctionPtrType functionPtrType ->
                            dest.funcProtocol().path().close(functionPtrType.typeName());
                    case ValueBasedType valueBasedType -> dest.valueBased().path().close(valueBasedType.typeName());
                    case ArrayTypeNamed arrayTypeNamed -> dest.arrayNamed().path().close(arrayTypeNamed.typeName());
                    case CommonTypes.BindTypes bindTypes ->
                            throw new UnsupportedOperationException("Not supported yet.");
                    case StructType structType -> dest.struct().path().close(structType.typeName());
                };
                case SymbolProviderType symbolProviderType -> symbolProviderType.path();
                case TaggedNamedType _, ArrayType _, PointerType _ -> throw new UnsupportedOperationException();
                case VoidType voidType -> dest.voidBased().path().close(voidType.typeName());
            };
            allTypes.put(unlocatedType, Optional.of(path));
            return Optional.of(path);
        }

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
                    case TaggedNamedType _, ArrayType _, PointerType _ ->
                            throw new UnsupportedOperationException("todo");
                    case VoidType voidType -> new VoidBasedGenerator(voidType);
                };
                generators.add(generator);
            }
            return generators;
        }
    }

    private static void processType(GenerateUnit generateUnit, List<Function> functions, HashSet<Macro> macros,
                                    ArrayList<Declare> varDeclares, HashMap<String, Type> types,
                                    Map<String, Type> processedTypes,
                                    Set<Function> processedFunSymbols) {
        ArrayList<ConstGenerator.ConstValue> constValues = new ArrayList<>(varDeclares.stream()
                .map(d -> new ConstGenerator.ConstValue(Utils.conv(d.type(), null), d.value(), d.name())).toList());
        // types
        for (var t : types.entrySet()) {
            if (processedTypes.containsKey(t.getKey())) {
                continue;
            }
            Type s = t.getValue();
            TypeAttr.TypeRefer conv = Utils.conv(s, null);
            switch (conv) {
                case ArrayTypeNamed arrayTypeNamed -> generateUnit.addType(arrayTypeNamed);
                case EnumType e -> {
                    Type type = Utils.typedefLookUp(s);
                    Enum en = (Enum) type;
                    if (en.isUnnamed()) {
                        for (Declare declare : en.getDeclares()) {
                            constValues.add(new ConstGenerator.ConstValue(Utils.conv(declare.type(), null), declare.value(), declare.name()));
                        }
                    } else {
                        generateUnit.addType(e);
                    }
                }
                case FunctionPtrType functionPtrType -> generateUnit.addType(functionPtrType);
                case ValueBasedType valueBasedType -> generateUnit.addType(valueBasedType);
                case VoidType voidType -> generateUnit.addType(voidType);
                case RefOnlyType refOnlyType -> generateUnit.addType(refOnlyType);
                case StructType structType -> generateUnit.addType(structType);
                case CommonTypes.BindTypes _, PointerType _, ArrayType _ -> {
                }
                default -> throw new IllegalStateException("Unexpected value: " + conv);
            }
        }
        // constants
        generateUnit.addConstValues(constValues);
        // macros
        HashSet<MacroGenerator.Macro> macro = new HashSet<>();
        macros.forEach(e -> {
            switch (e.type()) {
                case PrimitiveTypes.CType c ->
                        macro.add(new MacroGenerator.Macro.Primitive(Utils.conv2BindTypes(c).getPrimitiveType(), e.declName(), e.initializer(), e.comment()));
                case PrimitiveTypes.JType _ ->
                        macro.add(new MacroGenerator.Macro.String(e.declName(), e.initializer(), e.comment()));
            }
        });
        generateUnit.addMacros(macro);
        // function symbols
        ArrayList<FunctionPtrType> functionPtrTypes = new ArrayList<>();
        for (Function function : functions) {
            if (processedFunSymbols.contains(function))
                continue;
            List<FunctionPtrType.Arg> args = function.paras().stream()
                    .map(para -> new FunctionPtrType.Arg(para.paraName(), Utils.conv(para.paraType(), null))).toList();
            functionPtrTypes.add(new FunctionPtrType(function.name(), args, Utils.conv(function.ret(), null)));
        }
        generateUnit.addFunctionSymbols(functionPtrTypes);
    }

    private final ArrayList<GenerateUnit> units = new ArrayList<>();

    public Processor(Utils.DestinationProvider dest, Utils.Filter filter) {
        class BaseUnit extends GenerateUnit {
            BaseUnit(Utils.DestinationProvider dest, Utils.Filter filter) {
                super(dest, filter);
            }

            final ArrayList<Generator> generators = new ArrayList<>();

            public void addAllType(List<? extends CommonTypes.BaseType> generation) {
                generation.forEach(this::addType);
                generators.add(new CommonGenerator(generation));
            }

            public void addAllType(Map<? extends CommonTypes.BaseType, PackagePath> generations) {
                generations.forEach(this::addType);
                generators.add(new CommonGenerator(generations.keySet().stream().toList()));
            }

            @Override
            public ArrayList<Generator> queryGenerators(Set<TypeAttr.GenerationType> unhandledTypes) {
                ArrayList<Generator> ret = new ArrayList<>();
                for (TypeAttr.GenerationType unhandledType : unhandledTypes) {
                    if (unhandledType instanceof PointerType || unhandledType instanceof ArrayType) {
                        ret.add(new EmptyGenerator(unhandledType));
                    }
                }
                ret.addAll(super.queryGenerators(unhandledTypes));
                return ret;
            }

            @Override
            public Optional<PackagePath> queryPath(TypeAttr.GenerationType unlocatedType) {
                if (unlocatedType instanceof PointerType ptr) {
                    return Optional.of(dest.common().path().close(ptr.typeName()));
                }
                if (unlocatedType instanceof ArrayType arr) {
                    return Optional.of(dest.common().path().close(arr.typeName()));
                }
                return super.queryPath(unlocatedType);
            }

            @Override
            List<Generator> makeInitialGenerators() {
                return generators;
            }
        }
        BaseUnit generateUnit = new BaseUnit(dest, filter);
        // common
        generateUnit.addAllType(List.of(CommonTypes.BindTypes.values()));
        generateUnit.addAllType(List.of(CommonTypes.ValueInterface.values()));
        generateUnit.addAllType(CommonTypes.FFMTypes.packagePaths());
        generateUnit.addAllType(List.of(CommonTypes.BindTypeOperations.values()));
        generateUnit.addAllType(List.of(CommonTypes.BasicOperations.values()));
        generateUnit.addAllType(List.of(CommonTypes.SpecificTypes.values()));
        units.add(generateUnit);
    }

    private final HashMap<String, Type> processedTypes = new HashMap<>();
    private final HashSet<Function> processedFunSymbols = new HashSet<>();

    public Processor withExtra(List<Function> functions, HashSet<Macro> macros, ArrayList<Declare> varDeclares,
                               HashMap<String, Type> types, Utils.DestinationProvider dest, Utils.Filter filter) {
        // symbol provider
        SymbolProviderType provider = new SymbolProviderType(dest.symbolProvider().path());
        GenerateUnit generateUnit = new GenerateUnit(provider, dest, filter);
        processType(generateUnit, functions, macros, varDeclares, types, processedTypes, processedFunSymbols);
        processedTypes.putAll(types);
        processedFunSymbols.addAll(functions);
        units.add(generateUnit);
        return this;
    }

    public Processor withExtra(Analyser analyser, Utils.DestinationProvider dest, Utils.Filter filter) {
        return withExtra(analyser.getFunctions(), analyser.getMacros(), analyser.getVarDeclares(), analyser.getTypes(), dest, filter);
    }

    public void generate() {
        ArrayList<Generator> generators = new ArrayList<>();
        for (GenerateUnit unit : units) {
            generators.addAll(unit.makeInitialGenerators());
        }
        Generators gen = new Generators(generators, unhandledTypes -> {
            ArrayList<Generator> ret = new ArrayList<>();
            for (GenerateUnit unit : units) {
                ret.addAll(unit.queryGenerators(unhandledTypes));
            }
            return ret;
        }, unlocatedType -> {
            for (GenerateUnit unit : units) {
                Optional<PackagePath> packagePath = unit.queryPath(unlocatedType);
                if (packagePath.isPresent()) return packagePath.get();
            }
            System.out.println(unlocatedType);
            for (GenerateUnit unit : units) {
                Optional<PackagePath> packagePath = unit.queryPath(unlocatedType);
                if (packagePath.isPresent()) return packagePath.get();
            }

            throw new IllegalStateException("No package path found for " + unlocatedType);
        });
        gen.generate();
    }
}