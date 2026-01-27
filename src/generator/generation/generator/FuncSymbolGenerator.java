package generator.generation.generator;

import generator.Generators;
import generator.PackageManager;
import generator.PackagePath;
import generator.types.CommonTypes;
import generator.types.FunctionPtrType;
import generator.types.SymbolProviderType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FuncSymbolGenerator implements Generator {
    private final List<FunctionPtrType> functions;
    private final PackagePath dest;
    private final SymbolProviderType symbolProvider;

    public FuncSymbolGenerator(List<FunctionPtrType> functions, PackagePath dest, SymbolProviderType symbolProvider) {
        this.functions = new ArrayList<>(functions);
        this.dest = dest;
        this.symbolProvider = symbolProvider;
    }

    @Override
    public GenerateResult generate(Generators.GenerationProvider locations, Generators.Writer writer) {
        PackageManager packages = new PackageManager(locations, dest);
        String functions = this.functions.stream().map(type ->
                        makeDirectCall(new FunctionRawUtils(packages, type), packages.useClass(symbolProvider), packages)
                        + "\n" + makeWrappedCall(new FunctionWrapUtils(packages, type)))
                .collect(Collectors.joining(System.lineSeparator()));
        writer.write(packages, "public final class %s {\n%s}".formatted(packages.getClassName(), functions));
        return new GenerateResult(List.of(packages), List.of());
    }


    private static String makeDirectCall(FunctionRawUtils raw, String symbolClassName, PackageManager packages) {
        packages.useClass(CommonTypes.FFMTypes.METHOD_HANDLE);
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
                packages.useClass(CommonTypes.SpecificTypes.FunctionUtils),
                packages.getClassName() // 9
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
        wrap.hasOnHeapReturnVariant().ifPresent(variant ->
                sb.append("""
                        
                            public static %2$s %1$s(%3$s) {
                                %4$s;
                            }
                        """.formatted(variant.getFunctionName(),
                        variant.downcallRetType(),
                        variant.downcallUpperPara(), // 3
                        variant.downcallTypeReturn("%s$Raw(%s)".formatted(variant.getFunctionName(), variant.downcallUpperParaDestruct())))));
        return sb.toString();
    }
}
