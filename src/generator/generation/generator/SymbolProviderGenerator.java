package generator.generation.generator;

import generator.Dependency;
import generator.Generators;
import generator.PackageManager;
import generator.generation.SymbolProvider;
import generator.types.CommonTypes;

public class SymbolProviderGenerator implements Generator {
    private final PackageManager packages;
    private final Generators.Writer writer;

    public SymbolProviderGenerator(SymbolProvider symbolProvider, Dependency dependency, Generators.Writer writer) {
        this.packages = new PackageManager(dependency, symbolProvider.getTypePkg().packagePath());
        this.writer = writer;
    }

    @Override
    public void generate() {
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
    }
}
