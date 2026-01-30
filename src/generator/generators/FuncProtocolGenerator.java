package generator.generators;

import generator.Generators;
import generator.PackageManager;
import generator.types.CommonTypes;
import generator.types.FunctionPtrType;

import java.util.List;

import static generator.generators.FuncPtrUtils.getNonConflictType;

public class FuncProtocolGenerator implements Generator {
    private final FunctionPtrType functionPtrType;

    public FuncProtocolGenerator(FunctionPtrType functionPtrType) {
        this.functionPtrType = functionPtrType;
    }

    private final static List<String> FORBID_LAMBDA_NAMES = List.of("function", "funcLifeTime");

    private static FunctionPtrType getNonConflictLambdaType(FunctionPtrType function) {
        return getNonConflictType(function, FORBID_LAMBDA_NAMES);
    }

    @Override
    public GenerateResult generate(Generators.GenerationProvider locations, Generators.Writer writer) {
        var packages = new PackageManager(locations, functionPtrType);
        var utilsClassName = packages.useClass(CommonTypes.SpecificTypes.FunctionUtils);
        FunctionRawUtils raw = new FunctionRawUtils(packages, functionPtrType);
        FunctionWrapUtils wrap = new FunctionWrapUtils(packages, functionPtrType);
        String className = packages.getClassName();
        String interfaces = """
                    public interface %7$s {
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
                functionPtrType.innerFunctionTypeName(), // 6
                functionPtrType.innerFunctionTypeRawName() // 7
        );

        FunctionPtrType lambdaType = getNonConflictLambdaType(functionPtrType);
        FunctionWrapUtils lambda = new FunctionWrapUtils(packages, lambdaType);
        String constructors = """
                    public %1$s(%7$s funcLifeTime, %6$s function) {
                        try {
                            methodHandle = %8$s.lookup().findVirtual(%6$s.class,
                                    "invoke", FUNCTION_DESCRIPTOR.toMethodType()).bindTo(function);
                            funPtr = %5$s.upcallStub(funcLifeTime, methodHandle, FUNCTION_DESCRIPTOR);
                        } catch (java.lang.NoSuchMethodException | java.lang.IllegalAccessException e) {
                            throw new %5$s.SymbolNotFound(e);
                        }
                    }
                
                    public static %1$s of(%7$s funcLifeTime, %4$s function) {
                        return new %1$s(funcLifeTime, (%6$s) (%2$s)
                                -> %3$s);
                    }
                """.formatted(className,
                lambda.upcallParaNames(),
                lambda.upcallUpperRetTypeDestruct("function.invoke(%s)".formatted(lambda.upcallParaConstruct())),
                functionPtrType.innerFunctionTypeName(),
                utilsClassName, // 5
                functionPtrType.innerFunctionTypeRawName(),
                packages.useClass(CommonTypes.FFMTypes.ARENA), // 7
                packages.useClass(CommonTypes.FFMTypes.METHOD_HANDLES) // 8
        );

        StringBuilder invokes = new StringBuilder("""
                    private %1$s invokeRaw(%2$s) {
                        try {
                            %3$sthis.methodHandle.invokeExact(%4$s);
                        } catch (java.lang.Throwable e) {
                            throw new %5$s.InvokeException(e);
                        }
                    }
                """.formatted(raw.rawRetType(),
                raw.rawDowncallPara(),
                raw.rawReturnCast(),
                raw.rawDowncallStr(), // 4
                utilsClassName)); // 5
        if (wrap.onHeapReturnVariant().isEmpty()) {
            invokes.append("""
                        public %1$s invoke(%2$s) {
                            %3$s;
                        }
                    """.formatted(
                    wrap.downcallRetType(),
                    wrap.downcallUpperPara(),
                    wrap.downcallTypeReturn("invokeRaw(%s)".formatted(wrap.downcallUpperParaDestruct()))));
        } else {
            var variant = wrap.onHeapReturnVariant().get();
            invokes.append("""
                    
                        public %1$s invoke(%2$s) {
                            %3$s;
                        }
                    """.formatted(
                    variant.downcallRetType(),
                    variant.downcallUpperPara(),
                    variant.downcallTypeReturn("invokeRaw(%s)".formatted(variant.downcallUpperParaDestruct()) // 3
                    )));
        }
        String toString = """
                    @Override
                    public String toString() {
                        return "%1$s{" +
                                "funPtr=" + funPtr +
                                ", methodHandle=" + methodHandle +
                                '}';
                    }
                """.formatted(className);
        writer.write(packages, make(packages, raw, interfaces, constructors, invokes.toString(), toString, utilsClassName));
        return new GenerateResult(packages, functionPtrType);
    }

    private String make(PackageManager packages, FunctionRawUtils raw, String interfaces, String constructors, String invokes, String ext, Object utilsClassName) {
        return """
                public class %1$s implements %9$s<%1$s, %1$s.%14$s>, %8$s<%1$s> {
                    public static final %8$s.Operations<%1$s> OPERATIONS = %9$s.makeOperations(%1$s::new);
                    public static final %13$s FUNCTION_DESCRIPTOR = %2$s;
                
                %3$s
                    private final %12$s funPtr;
                    private final %11$s methodHandle;
                
                %4$s
                
                    public %1$s(%12$s funPtr) {
                        this(funPtr, false);
                    }
                
                    public %1$s(%12$s funPtr, boolean critical) {
                        this.funPtr = funPtr;
                        methodHandle = funPtr.address() == 0 ? null : %7$s.downcallHandle(funPtr, FUNCTION_DESCRIPTOR, critical);
                    }
                
                %5$s
                
                    @Override
                    public %10$s<%1$s, %14$s> operator() {
                        return new %10$s<>() {
                            @Override
                            public %8$s.Operations<%14$s> elementOperation() {
                                throw new java.lang.UnsupportedOperationException();
                            }
                
                            @Override
                            public void setPointee(%14$s pointee) {
                                throw new java.lang.UnsupportedOperationException();
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
                            public %14$s pointee() {
                                throw new java.lang.UnsupportedOperationException();
                            }
                
                            @Override
                            public %12$s value() {
                                return funPtr;
                            }
                        };
                    }
                
                %6$s
                }""".formatted(packages.getClassName(), raw.funcDescriptor(),
                interfaces, constructors, invokes, ext, // 6
                utilsClassName, // 7
                packages.useClass(CommonTypes.BasicOperations.Info), // 8
                packages.useClass(CommonTypes.BindTypeOperations.PtrOp), // 9
                CommonTypes.BindTypeOperations.PtrOp.operatorTypeName(), // 10
                packages.useClass(CommonTypes.FFMTypes.METHOD_HANDLE), // 11
                packages.useClass(CommonTypes.FFMTypes.MEMORY_SEGMENT), // 12
                packages.useClass(CommonTypes.FFMTypes.FUNCTION_DESCRIPTOR), // 13
                functionPtrType.innerFunctionTypeRawName() // 14
        );
    }
}
