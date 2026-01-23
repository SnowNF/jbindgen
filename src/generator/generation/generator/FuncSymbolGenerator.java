package generator.generation.generator;

import generator.Dependency;
import generator.PackagePath;
import generator.TypePkg;
import generator.Utils;
import generator.generation.FuncSymbols;
import generator.types.CommonTypes;
import generator.types.FunctionPtrType;
import generator.types.SymbolProviderType;
import generator.types.TypeAttr;
import generator.types.operations.CommonOperation.AllocatorType;

import java.util.stream.Collectors;

public class FuncSymbolGenerator implements Generator {
    private final FuncSymbols funcSymbols;
    private final Dependency dependency;
    private final String symbolClassName;

    public FuncSymbolGenerator(FuncSymbols funcSymbols, Dependency dependency, SymbolProviderType symbolProvider) {
        this.funcSymbols = funcSymbols;
        this.dependency = dependency;
        this.symbolClassName = dependency.getTypePackagePath(symbolProvider).getClassName();
    }

    @Override
    public void generate() {
        PackagePath pp = funcSymbols.getPackagePath();
        String out = pp.makePackage();
        out += Generator.extractImports(funcSymbols, dependency);
        out += "public final class %s {\n%s}".formatted(pp.getClassName(),
                funcSymbols.getFunctions().stream().map(TypePkg::type)
                        .map(type ->
                                makeDirectCall(type, symbolClassName, pp.getClassName())
                                + System.lineSeparator()
                                + makeWrappedCall(type))
                        .collect(Collectors.joining(System.lineSeparator())));
        Utils.write(pp, out);
    }


    private static String makeDirectCall(FunctionPtrType type,
                                         String symbolClassName, String className) {
        return """
                    private static MethodHandle %1$s;
                
                    private static %2$s %1$s$Raw(%7$s) {
                        if (%9$s.%1$s == null) {
                            %9$s.%1$s = %4$s.downcallHandle("%1$s", %3$s).orElseThrow(() -> new %8$s.SymbolNotFound("%1$s"));
                        }
                        try {
                            %5$s%9$s.%1$s.invoke(%6$s);
                        } catch (Throwable e) {
                            throw new %8$s.InvokeException(e);
                        }
                    }
                """.formatted(Generator.getTypeName(type), FuncPtrUtils.makeRawRetType(type),
                FuncPtrUtils.makeFuncDescriptor(type), symbolClassName, // 4
                FuncPtrUtils.makeRawStrBeforeInvoke(type), FuncPtrUtils.makeRawInvokeStr(type), // 6
                FuncPtrUtils.makeRawPara(type, type.allocatorType()), // 7
                CommonTypes.SpecificTypes.FunctionUtils.typeName(TypeAttr.NameType.RAW),
                className // 9
        );
    }

    private static String makeWrappedCall(FunctionPtrType type) {
        // only consider AllocatorType.STANDARD and AllocatorType.NONE here
        AllocatorType allocatorType = type.allocatorType();
        var out = """     
                    public static %2$s %1$s(%3$s) {
                        %4$s;
                    }
                """.formatted(Generator.getTypeName(type),
                FuncPtrUtils.makeWrappedRetType(type, allocatorType), FuncPtrUtils.makeUpperWrappedPara(type, allocatorType),
                FuncPtrUtils.makeWrappedStrForInvoke("%s$Raw(%s)".formatted(Generator.getTypeName(type),
                        FuncPtrUtils.makeUpperWrappedParaDestruct(type, allocatorType)), type, type.allocatorType()));
        if (type.allocatorType() == AllocatorType.STANDARD) {
            // return on heap type (Arena.ofAuto())
            out += """
                    
                        public static %2$s %1$s(%3$s) {
                            %4$s;
                        }
                    """.formatted(Generator.getTypeName(type),
                    FuncPtrUtils.makeWrappedRetType(type, AllocatorType.ON_HEAP),
                    FuncPtrUtils.makeUpperWrappedPara(type, AllocatorType.ON_HEAP),
                    FuncPtrUtils.makeWrappedStrForInvoke("%s$Raw(%s)".formatted(Generator.getTypeName(type),
                            FuncPtrUtils.makeUpperWrappedParaDestruct(type, AllocatorType.ON_HEAP)), type, AllocatorType.ON_HEAP)); // 3
        }
        return out;
    }
}
