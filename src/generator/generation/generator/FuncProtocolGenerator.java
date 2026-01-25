package generator.generation.generator;

import generator.Dependency;
import generator.Utils;
import generator.generation.FuncPointer;
import generator.types.CommonTypes;
import generator.types.FunctionPtrType;
import generator.types.TypeAttr;

import java.util.List;

import static generator.generation.generator.FuncPtrUtils.getNonConflictType;

public class FuncProtocolGenerator implements Generator {
    public static final String FUNCTION_TYPE_NAME = "Function";
    private final FuncPointer funcPointer;
    private final Dependency dependency;
    private final String utilsClassName;

    public FuncProtocolGenerator(FuncPointer funcPointer, Dependency dependency) {
        this.funcPointer = funcPointer;
        this.dependency = dependency;
        utilsClassName = dependency.getTypePackagePath(CommonTypes.SpecificTypes.FunctionUtils).getClassName();
    }

    private final static List<String> FORBID_LAMBDA_NAMES = List.of("function", "funcLifeTime");

    private static FunctionPtrType getNonConflictLambdaType(FunctionPtrType function) {
        return getNonConflictType(function, FORBID_LAMBDA_NAMES);
    }

    @Override
    public void generate() {
        FunctionPtrType type = funcPointer.getTypePkg().type();
        FunctionRawUtils raw = new FunctionRawUtils(type);
        FunctionWrapUtils wrap = new FunctionWrapUtils(type);
        String className = funcPointer.getTypePkg().packagePath().getClassName();
        String out = funcPointer.getTypePkg().packagePath().makePackage();
        out += Generator.extractImports(funcPointer, dependency);
        String interfaces = """
                    public interface %6$sRaw {
                        %3$s invoke(%2$s);
                    }
                
                    public interface %6$s {
                        %5$s invoke(%4$s);
                    }
                """.formatted(className,
                raw.rawUpcallPara(),
                raw.rawRetType(),
                wrap.upcallPara(),
                wrap.upcallUpperRetType(),
                FUNCTION_TYPE_NAME // 6
        );

        FunctionPtrType lambdaType = getNonConflictLambdaType(type);
        FunctionWrapUtils lambda = new FunctionWrapUtils(lambdaType);
        String constructors = """
                    public %1$s(Arena funcLifeTime, %4$sRaw function) {
                        try {
                            methodHandle = MethodHandles.lookup().findVirtual(%4$sRaw.class,
                                    "invoke", FUNCTION_DESCRIPTOR.toMethodType()).bindTo(function);
                            funPtr = %5$s.upcallStub(funcLifeTime, methodHandle, FUNCTION_DESCRIPTOR);
                        } catch (NoSuchMethodException | IllegalAccessException e) {
                            throw new %5$s.SymbolNotFound(e);
                        }
                    }
                
                    public static %1$s of(Arena funcLifeTime, %4$s function) {
                        return new %1$s(funcLifeTime, (%4$sRaw) (%2$s)
                                -> %3$s);
                    }
                """.formatted(className,
                lambda.upcallParaNames(),
                lambda.upcallUpperRetTypeDestruct("function.invoke(%s)".formatted(lambda.upcallParaConstruct())),
                FUNCTION_TYPE_NAME, utilsClassName);

        StringBuilder invokes = new StringBuilder("""
                    private %1$s invokeRaw(%2$s) {
                        try {
                            %3$sthis.methodHandle.invokeExact(%4$s);
                        } catch (Throwable e) {
                            throw new %8$s.InvokeException(e);
                        }
                    }
                
                    public %5$s invoke(%6$s) {
                        %7$s;
                    }
                """.formatted(raw.rawRetType(),
                raw.rawDowncallPara(),
                raw.rawReturnCast(),
                raw.rawDowncallStr(), // 4
                wrap.downcallRetType(),
                wrap.downcallUpperPara(), // 6
                wrap.downcallTypeReturn("invokeRaw(%s)".formatted(wrap.downcallUpperParaDestruct())),
                utilsClassName)); // 8
        wrap.hasOnHeapReturnVariant().ifPresent(variant ->
                invokes.append("""
                        
                            public %1$s invoke(%2$s) {
                                %3$s;
                            }
                        """.formatted(
                        variant.downcallRetType(),
                        variant.downcallUpperPara(),
                        variant.downcallTypeReturn("invokeRaw(%s)".formatted(variant.downcallUpperParaDestruct()) // 3
                        ))));
        String toString = """
                    @Override
                    public String toString() {
                        return "%1$s{" +
                                "funPtr=" + funPtr +
                                ", methodHandle=" + methodHandle +
                                '}';
                    }
                """.formatted(className);
        out += make(className, raw, interfaces, constructors, invokes.toString(), toString);
        Utils.write(funcPointer.getTypePkg().packagePath(), out);
    }

    private String make(String className, FunctionRawUtils raw, String interfaces, String constructors, String invokes, String ext) {
        return """
                public class %1$s implements %9$s<%1$s, %1$s.Function>, %8$s<%1$s> {
                    public static final %8$s.Operations<%1$s> OPERATIONS = %9$s.makeOperations(%1$s::new);
                    public static final FunctionDescriptor FUNCTION_DESCRIPTOR = %2$s;
                
                %3$s
                    private final MemorySegment funPtr;
                    private final MethodHandle methodHandle;
                
                %4$s
                
                    public %1$s(MemorySegment funPtr) {
                        this(funPtr, false);
                    }
                
                    public %1$s(MemorySegment funPtr, boolean critical) {
                        this.funPtr = funPtr;
                        methodHandle = funPtr.address() == 0 ? null : %7$s.downcallHandle(funPtr, FUNCTION_DESCRIPTOR, critical);
                    }
                
                %5$s
                
                    @Override
                    public %10$s<%1$s, Function> operator() {
                        return new %10$s<>() {
                            @Override
                            public %8$s.Operations<Function> elementOperation() {
                                throw new UnsupportedOperationException();
                            }
                
                            @Override
                            public void setPointee(Function pointee) {
                                throw new UnsupportedOperationException();
                            }
                
                            @Override
                            public %8$s.Operations<%1$s> getOperations() {
                                return OPERATIONS;
                            }
                
                            @Override
                            public %1$s self() {
                                return %1$s.this;
                            }
                
                            @Override
                            public Function pointee() {
                                throw new UnsupportedOperationException();
                            }
                
                            @Override
                            public MemorySegment value() {
                                return funPtr;
                            }
                        };
                    }
                
                %6$s
                }""".formatted(className, raw.funcDescriptor(),
                interfaces, constructors, invokes, ext, // 6
                utilsClassName, // 7
                CommonTypes.BasicOperations.Info.typeName(TypeAttr.NameType.RAW), // 8
                CommonTypes.BindTypeOperations.PtrOp.typeName(TypeAttr.NameType.RAW), // 9
                CommonTypes.BindTypeOperations.PtrOp.operatorTypeName() // 10
        );
    }
}
