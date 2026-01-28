package generator.generation.generator;

import generator.Generators;
import generator.PackageManager;
import generator.types.CommonTypes;
import generator.types.MemoryLayouts;
import generator.types.StructType;
import generator.types.TypeAttr;
import generator.types.operations.MemoryOperation;
import generator.types.operations.OperationAttr;

import java.util.ArrayList;
import java.util.Optional;

public class StructGenerator implements Generator {
    private final StructType struct;

    public StructGenerator(StructType struct) {
        this.struct = struct;
    }

    @Override
    public GenerateResult generate(Generators.GenerationProvider locations, Generators.Writer writer) {
        var packages = new PackageManager(locations, struct);
        StringBuilder stringBuilder = new StringBuilder();
        ArrayList<StructType.Member> availableMembers = new ArrayList<>();
        for (StructType.Member member : struct.getMembers()) {
            makeGetterAndSetter(packages, member)
                    .ifPresent(getterAndSetter -> {
                        availableMembers.add(member);
                        stringBuilder.append(getterAndSetter.getter)
                                .append(System.lineSeparator())
                                .append(getterAndSetter.setter);
                    });
            stringBuilder.append("\n");
        }
        stringBuilder.append(toString(packages.getClassName(), availableMembers));
        writer.write(packages, getMain(packages, struct.getMemoryLayout(packages), stringBuilder.toString()));
        return new GenerateResult(packages, struct);
    }

    record GetterAndSetter(String getter, String setter) {
    }

    private static String toString(String className, ArrayList<StructType.Member> availableMembers) {
        var ss = availableMembers.stream().map(member -> """
                %s=" + %s() +
                """.formatted(member.name(), member.name())).toList();
        return """
                    @Override
                    public String toString() {
                        return (ms.address() == 0 && ms.isNative()) ? ms.toString()
                                : "%s{" +
                                  %s                  '}';
                    }
                """.formatted(className, ss.isEmpty() ? "" : "\"" + String.join("                  \", ", ss));
    }

    private static Optional<GetterAndSetter> makeGetterAndSetter(PackageManager packages, StructType.Member member) {
        OperationAttr.Operation operation = ((TypeAttr.OperationType) member.type()).getOperation();
        String memberName = member.name();
        if (member.bitField()) {
            var get = operation.getMemoryOperation(packages).getterBitfield("ms", member.offset(), member.bitSize());
            var set = operation.getMemoryOperation(packages).setterBitfield("ms", member.offset(), member.bitSize(), memberName);
            if (get.isEmpty() || set.isEmpty())
                return Optional.empty();
            return Optional.of(new GetterAndSetter("""
                        public %s %s(%s) {
                    %s
                        }
                    """.formatted(get.get().ret(), memberName, get.get().para(), get.get().codeSegment()),
                    """
                                public %s %s(%s) {
                            %s
                                    return this;
                                }
                            """.formatted(packages.getClassName(), memberName, set.get().para(), set.get().codeSegment())));
        }
        MemoryOperation.Getter getter = operation.getMemoryOperation(packages).getter("ms", member.offset() / 8);
        MemoryOperation.Setter setter = operation.getMemoryOperation(packages).setter("ms", member.offset() / 8, memberName);
        return Optional.of(new GetterAndSetter("""
                    public %s %s(%s) {
                        return %s;
                    }
                """.formatted(getter.ret(), memberName, getter.para(), getter.codeSegment()),
                """
                            public %s %s(%s) {
                                %s;
                                return this;
                            }
                        """.formatted(packages.getClassName(), memberName, setter.para(), setter.codeSegment())));
    }

    private static String getMain(PackageManager packages, MemoryLayouts layout, String ext) {
        return """
                import java.util.Objects;
                
                public final class %1$s implements %4$s<%1$s>, %6$s<%1$s> {
                    private final %10$s ms;
                    public static final %6$s.Operations<%1$s> OPERATIONS = %4$s.makeOperations(%1$s::new, %2$s);
                
                    public %1$s(%10$s ms) {
                        this.ms = ms;
                    }
                
                    public %1$s() {
                        this.ms = %9$s.onHeapAllocator().allocate(OPERATIONS.memoryLayout());
                    }
                
                    public static %7$s<%1$s> array(%11$s allocator, %5$s<?> len) {
                        return array(allocator, len.operator().value());
                    }
                
                    public static %7$s<%1$s> array(%11$s allocator, long len) {
                        return new %7$s<>(allocator, %1$s.OPERATIONS, len);
                    }
                
                    public static %8$s<%1$s> ptr(%11$s allocator) {
                        return new %8$s<>(allocator, %1$s.OPERATIONS);
                    }
                
                    @Override
                    public StructOpI<%1$s> operator() {
                        return new StructOpI<>() {
                            @Override
                            public %1$s reinterpret() {
                                return new %1$s(ms.reinterpret(OPERATIONS.memoryLayout().byteSize()));
                            }
                
                            @Override
                            public %6$s.Operations<%1$s> getOperations() {
                                return OPERATIONS;
                            }
                
                            @Override
                            public %1$s self() {
                                return %1$s.this;
                            }
                
                            @Override
                            public %10$s value() {
                                return ms;
                            }
                        };
                    }
                
                    @Override
                    public boolean equals(Object o) {
                        if (!(o instanceof %1$s s)) return false;
                        return Objects.equals(ms, s.ms);
                    }
                
                    @Override
                    public int hashCode() {
                        return Objects.hashCode(ms);
                    }
                
                %3$s
                }""".formatted(packages.getClassName(), layout.getMemoryLayout(packages), ext,
                packages.useClass(CommonTypes.SpecificTypes.StructOp), // 4
                packages.useClass(CommonTypes.ValueInterface.I64I), // 5
                packages.useClass(CommonTypes.BasicOperations.Info), // 6
                packages.useClass(CommonTypes.SpecificTypes.Array), // 7
                packages.useClass(CommonTypes.BindTypes.Ptr), // 8
                packages.useClass(CommonTypes.SpecificTypes.MemoryUtils), // 9
                packages.useClass(CommonTypes.FFMTypes.MEMORY_SEGMENT), // 10
                packages.useClass(CommonTypes.FFMTypes.SEGMENT_ALLOCATOR) // 11
        );
    }
}
