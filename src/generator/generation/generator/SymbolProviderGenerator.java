package generator.generation.generator;

import generator.Generators;
import generator.PackageManager;
import generator.types.CommonTypes;
import generator.types.SymbolProviderType;

public class SymbolProviderGenerator implements Generator {
    private final SymbolProviderType symbolProvider;

    public SymbolProviderGenerator(SymbolProviderType symbolProvider) {
        this.symbolProvider = symbolProvider;
    }

    @Override
    public GenerateResult generate(Generators.GenerationProvider locations, Generators.Writer writer) {
        var packages = new PackageManager(locations, symbolProvider);
        writer.write(packages, """
                import java.lang.foreign.FunctionDescriptor;
                import java.lang.foreign.MemorySegment;
                import java.lang.invoke.MethodHandle;
                import java.util.Objects;
                import java.util.Optional;
                
                public class %2$s {
                    private %2$s() {
                        throw new UnsupportedOperationException();
                    }
                
                    public static %4$s.SymbolProvider symbolProvider = null;
                
                    private static final class SymbolProviderHolder {
                        private static final %4$s.SymbolProvider SYMBOL_PROVIDER = Objects.requireNonNull(symbolProvider);
                    }
                
                    public static Optional<MethodHandle> downcallHandle(String functionName, FunctionDescriptor fd) {
                        Optional<%4$s.Symbol> symbol = SymbolProviderHolder.SYMBOL_PROVIDER.provide(functionName);
                        if (symbol.isPresent() && symbol.get() instanceof %4$s.FunctionSymbol(MemorySegment ms, boolean critical)) {
                            return Optional.of(%4$s.downcallHandle(ms, fd, critical));
                        }
                        return Optional.empty();
                    }
                }
                """.formatted(null,
                packages.getClassName(),
                null,
                packages.useClass(CommonTypes.SpecificTypes.FunctionUtils)));
        return new GenerateResult(packages, symbolProvider);
    }
}
