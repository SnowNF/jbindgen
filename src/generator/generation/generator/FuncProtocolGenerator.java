package generator.generation.generator;

import generator.Dependency;
import generator.Utils;
import generator.generation.FuncPointer;
import generator.types.CommonTypes;
import generator.types.FunctionPtrType;
import generator.types.TypeAttr;
import generator.types.operations.CommonOperation.AllocatorType;

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
        String className = funcPointer.getTypePkg().packagePath().getClassName();

        String out = funcPointer.getTypePkg().packagePath().makePackage();
        out += Generator.extractImports(funcPointer, dependency);
        String interfaces = """
                    public interface %6$sRaw {
                        %3$s invoke(%2$s);
                    }
                
                    public interface %6$s {
                        %4$s invoke(%5$s);
                    }
                """.formatted(className,
                FuncPtrUtils.makeRawPara(type, AllocatorType.NONE),
                FuncPtrUtils.makeRawRetType(type),
                FuncPtrUtils.makeWrappedRetType(type, AllocatorType.NONE),
                FuncPtrUtils.makeWrappedPara(type, AllocatorType.NONE), FUNCTION_TYPE_NAME);// 6

        FunctionPtrType lambdaType = getNonConflictLambdaType(type);
        String constructors = """
                    public %1$s(Arena funcLifeTime, %4$sRaw function) {
                        try {
                            methodHandle = MethodHandles.lookup().findVirtual(%4$sRaw.class,
                                    "invoke", FUNCTIONDESCRIPTOR.toMethodType()).bindTo(function);
                            funPtr = %5$s.upcallStub(funcLifeTime, methodHandle, FUNCTIONDESCRIPTOR);
                        } catch (NoSuchMethodException | IllegalAccessException e) {
                            throw new %5$s.SymbolNotFound(e);
                        }
                    }
                
                    public static %1$s of(Arena funcLifeTime, %4$s function) {
                        return new %1$s(funcLifeTime, (%4$sRaw) (%2$s)
                                -> %3$s);
                    }
                """.formatted(className,
                FuncPtrUtils.makeParaNameStr(lambdaType, AllocatorType.NONE),
                FuncPtrUtils.makeWrappedRetDestruct("function.invoke(%s)".
                        formatted(FuncPtrUtils.makeWrappedParaConstruct(lambdaType, AllocatorType.NONE)), lambdaType),
                FUNCTION_TYPE_NAME, utilsClassName);

        String invokes = """
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
                """.formatted(FuncPtrUtils.makeRawRetType(type),
                FuncPtrUtils.makeRawPara(type, type.allocatorType()),
                FuncPtrUtils.makeRawStrBeforeInvoke(type),
                FuncPtrUtils.makeRawInvokeStr(type), // 4
                FuncPtrUtils.makeWrappedRetType(type, type.allocatorType()),
                FuncPtrUtils.makeUpperWrappedPara(type, type.allocatorType()), // 6
                FuncPtrUtils.makeWrappedStrForInvoke("invokeRaw(%s)".
                        formatted(FuncPtrUtils.makeUpperWrappedParaDestruct(type, type.allocatorType())), type, type.allocatorType()),
                utilsClassName); // 8
        if (type.allocatorType() == AllocatorType.STANDARD) {
            // return on heap type (Arena.ofAuto())
            invokes += """
                    
                        public %1$s invoke(%2$s) {
                            %3$s;
                        }
                    """.formatted(FuncPtrUtils.makeWrappedRetType(type, AllocatorType.ON_HEAP),
                    FuncPtrUtils.makeUpperWrappedPara(type, AllocatorType.ON_HEAP),
                    FuncPtrUtils.makeWrappedStrForInvoke("invokeRaw(%s)".
                                    formatted(FuncPtrUtils.makeUpperWrappedParaDestruct(type, AllocatorType.ON_HEAP)),
                            type, AllocatorType.ON_HEAP)); // 3
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
        out += make(className, type, interfaces, constructors, invokes, toString);
        Utils.write(funcPointer.getTypePkg().packagePath(), out);
    }

    private String make(String className, FunctionPtrType type, String interfaces, String constructors, String invokes, String ext) {
        return """
                public class %1$s implements %9$s<%1$s, %1$s.Function>, %8$s<%1$s> {
                    public static final %8$s.Operations<%1$s> OPERATIONS = %9$s.makeOperations(%1$s::new);
                    public static final FunctionDescriptor FUNCTIONDESCRIPTOR = %2$s;
                
                %3$s
                    private final MemorySegment funPtr;
                    private final MethodHandle methodHandle;
                
                %4$s
                
                    public %1$s(MemorySegment funPtr) {
                        this(funPtr, false);
                    }
                
                    public %1$s(MemorySegment funPtr, boolean critical) {
                        this.funPtr = funPtr;
                        methodHandle = funPtr.address() == 0 ? null : %7$s.downcallHandle(funPtr, FUNCTIONDESCRIPTOR, critical);
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
                }""".formatted(className, FuncPtrUtils.makeFuncDescriptor(type),
                interfaces, constructors, invokes, ext, // 6
                utilsClassName, // 7
                CommonTypes.BasicOperations.Info.typeName(TypeAttr.NameType.RAW), // 8
                CommonTypes.BindTypeOperations.PtrOp.typeName(TypeAttr.NameType.RAW), // 9
                CommonTypes.BindTypeOperations.PtrOp.operatorTypeName() // 10
        );
    }
}
