package processor;

import analyser.*;
import analyser.types.Enum;
import analyser.types.Type;
import generator.Generators;
import generator.PackagePath;
import generator.generators.ConstGenerator;
import generator.generators.Generator;
import generator.generators.MacroGenerator;
import generator.types.*;

import java.util.*;

public class Processor {
    private static void processType(ComponentUnit generateUnit, List<Function> functions, HashSet<Macro> macros,
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
            TypeAttr.GenerationType conv = Utils.conv(s, null);
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

    public Processor(Utils.DestinationProvider dest) {
        BaseUnit generateUnit = new BaseUnit(dest);
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
                               HashMap<String, Type> types, Utils.DestinationProvider dest, Utils.Filter filter, boolean greedy) {
        // symbol provider
        SymbolProviderType provider = new SymbolProviderType(dest.symbolProvider().path());
        ComponentUnit generateUnit = new ComponentUnit(provider, dest, filter, greedy);
        processType(generateUnit, functions, macros, varDeclares, types,
                Collections.unmodifiableMap(processedTypes), Collections.unmodifiableSet(processedFunSymbols));
        processedTypes.putAll(types);
        processedFunSymbols.addAll(functions);
        units.add(generateUnit);
        return this;
    }

    public Processor withExtra(Analyser analyser, Utils.DestinationProvider dest, Utils.Filter filter, boolean greedy) {
        return withExtra(analyser.getFunctions(), analyser.getMacros(), analyser.getVarDeclares(), analyser.getTypes(), dest, filter, greedy);
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
            throw new IllegalStateException("No package path found for " + unlocatedType);
        });
        gen.generate();
    }
}