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
    private record ProcessResult(
            List<Declare> varDeclares,
            Map<String, Type> types,
            Set<Function> funSymbols) {
    }

    private static ProcessResult processType(ComponentUnit generateUnit,
                                             List<Function> functions,
                                             HashSet<Macro> macros,
                                             ArrayList<Declare> varDeclares,
                                             HashMap<String, Type> types,
                                             Set<Declare> processedVarDeclares,
                                             Map<String, Type> processedTypes,
                                             Set<Function> processedFunSymbols) {
        LinkedHashMap<ConstGenerator.ConstValue, Optional<String>> constValues = new LinkedHashMap<>();
        for (Declare declare : varDeclares) {
            if (processedVarDeclares.contains(declare)) {
                continue;
            }
            ConstGenerator.ConstValue constValue = new ConstGenerator.ConstValue(Utils.conv(declare.type(), null), declare.value(), declare.name());
            constValues.put(constValue, Optional.ofNullable(declare.location()));
        }
        // types
        for (var t : types.entrySet()) {
            if (processedTypes.containsKey(t.getKey())) {
                continue;
            }
            Type s = t.getValue();
            var location = s.getLocation();
            TypeAttr.GenerationType conv = Utils.conv(s, null);
            switch (conv) {
                case ArrayTypeNamed arrayTypeNamed -> generateUnit.addType(arrayTypeNamed, location);
                case EnumType e -> {
                    Type type = Utils.typedefLookUp(s);
                    Enum en = (Enum) type;
                    if (en.isUnnamed()) {
                        for (Declare declare : en.getDeclares()) {
                            constValues.put(
                                    new ConstGenerator.ConstValue(Utils.conv(declare.type(), null), declare.value(), declare.name()),
                                    Optional.ofNullable(declare.location()));
                        }
                    } else {
                        generateUnit.addType(e, location);
                    }
                }
                case FunctionPtrType functionPtrType -> generateUnit.addType(functionPtrType, location);
                case ValueBasedType valueBasedType -> generateUnit.addType(valueBasedType, location);
                case VoidType voidType -> generateUnit.addType(voidType, location);
                case RefOnlyType refOnlyType -> generateUnit.addType(refOnlyType, location);
                case StructType structType -> generateUnit.addType(structType, location);
                case CommonTypes.BindTypes _, PointerType _, ArrayType _ -> {
                }
                default -> throw new IllegalStateException("Unexpected value: " + conv);
            }
        }
        // constants
        generateUnit.addConstValues(constValues);
        // macros
        LinkedHashMap<MacroGenerator.Macro, Optional<String>> macro = new LinkedHashMap<>();
        macros.forEach(e -> {
            switch (e.type()) {
                case PrimitiveTypes.CType c ->
                        macro.put(new MacroGenerator.Macro.Primitive(Utils.conv2BindTypes(c).getPrimitiveType(), e.declName(), e.initializer(), e.comment()), Optional.ofNullable(e.location()));
                case PrimitiveTypes.JType _ ->
                        macro.put(new MacroGenerator.Macro.String(e.declName(), e.initializer(), e.comment()), Optional.ofNullable(e.location()));
            }
        });
        generateUnit.addMacros(macro);
        // function symbols
        LinkedHashMap<FunctionPtrType, Optional<String>> functionPtrTypes = new LinkedHashMap<>();
        for (Function function : functions) {
            if (processedFunSymbols.contains(function))
                continue;
            List<FunctionPtrType.Arg> args = function.paras().stream()
                    .map(para -> new FunctionPtrType.Arg(para.paraName(), Utils.conv(para.paraType(), null))).toList();
            functionPtrTypes.put(new FunctionPtrType(function.name(), args, Utils.conv(function.ret(), null)), Optional.ofNullable(function.location()));
        }
        generateUnit.addFunctionSymbols(functionPtrTypes);
        return new ProcessResult(varDeclares, types, new HashSet<>(functions));
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
    private final HashSet<Declare> processedVarDeclares = new HashSet<>();

    public Processor withExtra(List<Function> functions, HashSet<Macro> macros, ArrayList<Declare> varDeclares,
                               HashMap<String, Type> types, Utils.DestinationProvider dest, Utils.Filter filter, boolean greedy) {
        // symbol provider
        SymbolProviderType provider = new SymbolProviderType(dest.symbolProvider().path());
        ComponentUnit generateUnit = new ComponentUnit(provider, dest, filter, greedy);
        ProcessResult processResult = processType(generateUnit, functions, macros, varDeclares, types,
                Collections.unmodifiableSet(processedVarDeclares),
                Collections.unmodifiableMap(processedTypes),
                Collections.unmodifiableSet(processedFunSymbols));
        processedTypes.putAll(processResult.types);
        processedFunSymbols.addAll(processResult.funSymbols);
        processedVarDeclares.addAll(processResult.varDeclares);
        units.add(generateUnit);
        return this;
    }

    public Processor withExtra(Analyser analyser, Utils.DestinationProvider dest, boolean greedy, Utils.Filter filter) {
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