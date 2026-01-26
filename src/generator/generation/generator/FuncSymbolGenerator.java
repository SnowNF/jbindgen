package generator.generation.generator;

import generator.Dependency;
import generator.Generators;
import generator.PackagePath;
import generator.TypePkg;
import generator.generation.FuncSymbols;
import generator.types.CommonTypes;
import generator.types.SymbolProviderType;
import generator.types.TypeAttr;

import java.util.stream.Collectors;

public class FuncSymbolGenerator implements Generator {
    private final FuncSymbols funcSymbols;
    private final Dependency dependency;
    private final Generators.Writer writer;
    private final String symbolClassName;

    public FuncSymbolGenerator(FuncSymbols funcSymbols, Dependency dependency, SymbolProviderType symbolProvider, Generators.Writer writer) {
        this.funcSymbols = funcSymbols;
        this.dependency = dependency;
        this.writer = writer;
        this.symbolClassName = dependency.getTypePackagePath(symbolProvider).getClassName();
    }

    @Override
    public void generate() {
        PackagePath pp = funcSymbols.getPackagePath();
        String out = pp.makePackage();
        out += Generator.extractImports(funcSymbols, dependency);
        String functions = funcSymbols.getFunctions().stream().map(TypePkg::type)
                .map(type -> makeDirectCall(new FunctionRawUtils(type), symbolClassName, pp.getClassName())
                             + "\n" + makeWrappedCall(new FunctionWrapUtils(type)))
                .collect(Collectors.joining(System.lineSeparator()));
        out += "public final class %s {\n%s}".formatted(pp.getClassName(), functions);
        writer.write(pp, out);
    }


    private static String makeDirectCall(FunctionRawUtils raw,
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
                """.formatted(
                raw.getFunctionName(),
                raw.rawRetType(),
                raw.funcDescriptor(),
                symbolClassName, // 4
                raw.rawReturnCast(),
                raw.rawDowncallStr(), // 6
                raw.rawDowncallPara(), // 7
                CommonTypes.SpecificTypes.FunctionUtils.typeName(TypeAttr.NameType.RAW),
                className // 9
        );
    }

    private static String makeWrappedCall(FunctionWrapUtils wrap) {
        // only consider AllocatorType.STANDARD and AllocatorType.NONE here
        StringBuilder sb = new java.lang.StringBuilder();
        sb.append("""     
                    public static %2$s %1$s(%3$s) {
                        %4$s;
                    }
                """.formatted(
                wrap.getFunctionName(),
                wrap.downcallRetType(),
                wrap.downcallUpperPara(),
                wrap.downcallTypeReturn("%s$Raw(%s)".formatted(wrap.getFunctionName(), wrap.downcallUpperParaDestruct()))));
        wrap.hasOnHeapReturnVariant().ifPresent(variant -> {
            sb.append("""
                    
                        public static %2$s %1$s(%3$s) {
                            %4$s;
                        }
                    """.formatted(variant.getFunctionName(),
                    variant.downcallRetType(),
                    variant.downcallUpperPara(), // 3
                    variant.downcallTypeReturn("%s$Raw(%s)".formatted(variant.getFunctionName(), variant.downcallUpperParaDestruct()))));
        });
        return sb.toString();
    }
}
