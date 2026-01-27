package processor;

import analyser.*;
import analyser.types.Enum;
import analyser.types.Type;
import generator.Generators;
import generator.TypePkg;
import generator.generation.*;
import generator.types.*;

import java.util.*;
import java.util.stream.Collectors;

public class Processor {
    record GenerateUnit(HashMap<Generation<?>, Optional<String>> genMap) {
        GenerateUnit() {
            this(new HashMap<>());
        }

        public void add(Generation<?> generation) {
            genMap.put(generation, Optional.empty());
        }

        public void add(Generation<?> generation, String location) {
            genMap.put(generation, Optional.ofNullable(location));
        }

        public void addAll(Collection<? extends Generation<?>> generation) {
            genMap.putAll(generation.stream().collect(Collectors.toMap(k -> k, _ -> Optional.empty())));
        }

        Set<Generation<?>> toMustGenerate(Utils.Filter filter) {
            return genMap.entrySet().stream().filter(filter).map(Map.Entry::getKey).collect(Collectors.toSet());
        }

        HashMap<TypeAttr.GenerationType, Generation<?>> getTypeGenerations() {
            HashMap<TypeAttr.GenerationType, Generation<?>> depGen = new HashMap<>();
            genMap.forEach((g, _) -> g.getImplTypes().stream()
                    .map(TypePkg::type).forEach(o -> depGen.put(o, g)));
            return depGen;
        }
    }

    private final HashMap<String, Type> processedTypes = new HashMap<>();
    private final HashSet<Function> processedFuncs = new HashSet<>();
    private final HashMap<TypeAttr.GenerationType, Generation<?>> allTypes = new HashMap<>();
    private final HashSet<Generation<?>> mustGenerate = new HashSet<>();

    public Processor(Utils.DestinationProvider dest, Utils.Filter filter) {
        GenerateUnit generateUnit = new GenerateUnit();
        // common
        generateUnit.addAll(Common.makeBindTypes(dest.common().path()));
        generateUnit.addAll(Common.makeValueInterfaces(dest.common().path()));
        generateUnit.addAll(Common.makeFFMs());
        generateUnit.addAll(Common.makeBindTypeInterface(dest.common().path()));
        generateUnit.addAll(Common.makeBasicOperations(dest.common().path()));
        generateUnit.addAll(Common.makeSpecific(dest.common().path()));
        allTypes.putAll(generateUnit.getTypeGenerations());
        mustGenerate.addAll(generateUnit.toMustGenerate(filter));
    }

    private static void processType(GenerateUnit generateUnit, List<Function> functions, HashSet<Macro> macros,
                                    ArrayList<Declare> varDeclares, HashMap<String, Type> types,
                                    Utils.DestinationProvider dest, Map<String, Type> processedTypes,
                                    Set<Function> processedFuncs) {
        ArrayList<ConstValues.Value> constValues = new ArrayList<>(varDeclares.stream()
                .map(d -> new ConstValues.Value(Utils.conv(d.type(), null), d.value(), d.name())).toList());
        // types
        for (var t : types.entrySet()) {
            if (processedTypes.containsKey(t.getKey())) {
                continue;
            }
            Type s = t.getValue();
            TypeAttr.TypeRefer conv = Utils.conv(s, null);
            switch (conv) {
                case ArrayTypeNamed arrayTypeNamed -> generateUnit.add(new ArrayNamed(dest.arrayNamed().path(), arrayTypeNamed), s.getLocation());
                case EnumType e -> {
                    Type type = Utils.typedefLookUp(s);
                    Enum en = (Enum) type;
                    if (en.isUnnamed()) {
                        for (Declare declare : en.getDeclares()) {
                            constValues.add(new ConstValues.Value(Utils.conv(declare.type(), null), declare.value(), declare.name()));
                        }
                    } else {
                        generateUnit.add(new Enumerate(dest.enumerate().path(), e));
                    }
                }
                case FunctionPtrType functionPtrType ->
                        generateUnit.add(new FuncPointer(dest.funcProtocol().path(), functionPtrType));
                case ValueBasedType valueBasedType -> generateUnit.add(new ValueBased(dest.valueBased().path(), valueBasedType));
                case VoidType voidType -> generateUnit.add(new VoidBased(dest.voidBased().path(), voidType));
                case RefOnlyType refOnlyType -> generateUnit.add(new RefOnly(dest.refOnly().path(), refOnlyType));
                case StructType structType -> generateUnit.add(new Structure(dest.struct().path(), structType));
                case CommonTypes.BindTypes _, PointerType _, ArrayType _ -> {
                }
                default -> throw new IllegalStateException("Unexpected value: " + conv);
            }
        }
        // constants
        generateUnit.add(new ConstValues(dest.constants().path(), constValues));
        // macros
        HashSet<Macros.Macro> macro = new HashSet<>();
        macros.forEach(e -> {
            switch (e.type()) {
                case PrimitiveTypes.CType c ->
                        macro.add(new Macros.Primitive(Utils.conv2BindTypes(c).getPrimitiveType(), e.declName(), e.initializer(), e.comment()));
                case PrimitiveTypes.JType _ ->
                        macro.add(new Macros.StrMacro(e.declName(), e.initializer(), e.comment()));
            }
        });
        generateUnit.add(new Macros(dest.macros().path(), macro));

        // symbol provider
        SymbolProviderType provider = new SymbolProviderType(dest.symbolProvider().path().getClassName());
        generateUnit.add(new SymbolProvider(dest.symbolProvider().path().removeClasses(), provider));

        // function symbols
        ArrayList<FunctionPtrType> functionPtrTypes = new ArrayList<>();
        for (Function function : functions) {
            if (processedFuncs.contains(function))
                continue;
            List<FunctionPtrType.Arg> args = function.paras().stream()
                    .map(para -> new FunctionPtrType.Arg(para.paraName(), Utils.conv(para.paraType(), null))).toList();
            functionPtrTypes.add(new FunctionPtrType(function.name(), args, Utils.conv(function.ret(), null)));
        }
        generateUnit.add(new FuncSymbols(dest.funcSymbols().path(), functionPtrTypes, provider));
    }

    public Processor withExtra(List<Function> functions, HashSet<Macro> macros, ArrayList<Declare> varDeclares,
                               HashMap<String, Type> types, Utils.DestinationProvider dest, Utils.Filter filter) {
        GenerateUnit generateUnit = new GenerateUnit();
        processType(generateUnit, functions, macros, varDeclares, types, dest,
                Collections.unmodifiableMap(processedTypes), Collections.unmodifiableSet(processedFuncs));
        processedTypes.putAll(types);
        processedFuncs.addAll(functions);
        allTypes.putAll(generateUnit.getTypeGenerations());
        mustGenerate.addAll(generateUnit.toMustGenerate(filter));
        return this;
    }

    public Processor withExtra(Analyser analyser, Utils.DestinationProvider dest, Utils.Filter filter) {
        return withExtra(analyser.getFunctions(), analyser.getMacros(), analyser.getVarDeclares(), analyser.getTypes(), dest, filter);
    }

    public void generate() {
        Generators generators = new Generators(mustGenerate, allTypes::get);
        generators.generate();
    }
}