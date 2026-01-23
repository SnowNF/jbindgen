package generator.generation.generator;

import generator.Dependency;
import generator.Utils;
import generator.generation.Structure;
import generator.types.CommonTypes;
import generator.types.MemoryLayouts;
import generator.types.StructType;
import generator.types.TypeAttr;
import generator.types.operations.MemoryOperation;
import generator.types.operations.OperationAttr;

import java.util.Optional;

public class StructGenerator implements Generator {
    private final Structure structure;
    private final Dependency dependency;

    public StructGenerator(Structure struct, Dependency dependency) {
        this.structure = struct;
        this.dependency = dependency;
    }

    @Override
    public void generate() {
        StringBuilder stringBuilder = new StringBuilder();
        StructType structType = structure.getTypePkg().type();
        for (StructType.Member member : structType.getMembers()) {
            makeGetterAndSetter(Generator.getTypeName(structType), member).ifPresent(getterAndSetter ->
                    stringBuilder.append(getterAndSetter.getter)
                            .append(System.lineSeparator())
                            .append(getterAndSetter.setter));
        }
        String out = structure.getTypePkg().packagePath().makePackage();
        out += Generator.extractImports(structure, dependency);
        out += getMain(Generator.getTypeName(structType), structType.getMemoryLayout(),
                stringBuilder + toString(structType));
        Utils.write(structure.getTypePkg().packagePath(), out);
    }

    record GetterAndSetter(String getter, String setter) {
    }

    private static String toString(StructType s) {
        var ss = s.getMembers().stream().filter(member -> !member.bitField()).map(member -> """
                %s=" + %s() +
                """.formatted(member.name(), member.name())).toList();
        return """
                    @Override
                    public String toString() {
                        return ms.address() == 0 ? ms.toString()
                                : "%s{" +
                                %s                '}';
                    }
                """.formatted(Generator.getTypeName(s), ss.isEmpty() ? "" : "\"" + String.join("                \", ", ss));
    }

    private static Optional<GetterAndSetter> makeGetterAndSetter(String thisName, StructType.Member member) {
        if (member.bitField()) {
            // skip this
            return Optional.empty();
        }
        OperationAttr.Operation operation = ((TypeAttr.OperationType) member.type()).getOperation();
        String memberName = member.name();
        MemoryOperation.Getter getter = operation.getMemoryOperation().getter("ms", member.offset() / 8);
        MemoryOperation.Setter setter = operation.getMemoryOperation().setter("ms", member.offset() / 8, memberName);
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
                        """.formatted(thisName, memberName, setter.para(), setter.codeSegment())));
    }

    private static String getMain(String className, MemoryLayouts layout, String ext) {
        return """
                import java.util.Objects;
                
                public final class %1$s implements %5$s<%1$s>, %7$s<%1$s> {
                    private final MemorySegment ms;
                    public static final %7$s.Operations<%1$s> OPERATIONS = %5$s.makeOperations(%1$s::new, %2$s);
                
                    public %1$s(MemorySegment ms) {
                        this.ms = ms;
                    }
                
                    public %1$s(SegmentAllocator allocator) {
                        this.ms = allocator.allocate(OPERATIONS.memoryLayout().byteSize());
                    }
                
                    public static %8$s<%1$s> list(SegmentAllocator allocator, %6$s<?> len) {
                        return list(allocator, len.operator().value());
                    }
                
                    public static %8$s<%1$s> list(SegmentAllocator allocator, long len) {
                        return new %8$s<>(allocator, %1$s.OPERATIONS, len);
                    }
                
                    public static %9$s<%1$s> single(SegmentAllocator allocator) {
                        return new %9$s<>(allocator, %1$s.OPERATIONS);
                    }
                
                    @Override
                    public StructOpI<%1$s> operator() {
                        return new StructOpI<>() {
                            @Override
                            public %1$s reinterpret() {
                                return new %1$s(ms.reinterpret(OPERATIONS.memoryLayout().byteSize()));
                            }
                
                            @Override
                            public %4$s<%1$s> getPointer() {
                                return new %4$s<>(ms, OPERATIONS);
                            }
                
                            @Override
                            public %7$s.Operations<%1$s> getOperations() {
                                return OPERATIONS;
                            }
                
                            @Override
                            public MemorySegment value() {
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
                }""".formatted(className, layout.getMemoryLayout(), ext,
                CommonTypes.BindTypes.Ptr.typeName(TypeAttr.NameType.RAW),
                CommonTypes.SpecificTypes.StructOp.typeName(TypeAttr.NameType.RAW),//5
                CommonTypes.ValueInterface.I64I.typeName(TypeAttr.NameType.RAW),
                CommonTypes.BasicOperations.Info.typeName(TypeAttr.NameType.RAW), // 7
                CommonTypes.SpecificTypes.Array.typeName(TypeAttr.NameType.RAW), // 8
                CommonTypes.SpecificTypes.Single.typeName(TypeAttr.NameType.RAW) // 9
        );
    }
}
