package generator.generation.generator;

import generator.Generators;
import generator.PackageManager;
import generator.types.ArrayTypeNamed;
import generator.types.CommonTypes;
import generator.types.CommonTypes.BasicOperations;
import generator.types.CommonTypes.BindTypes;
import generator.types.CommonTypes.SpecificTypes;
import generator.types.CommonTypes.ValueInterface;
import generator.types.TypeAttr;

public class ArrayNamedGenerator implements Generator {
    private final ArrayTypeNamed arrayNamed;

    public ArrayNamedGenerator(ArrayTypeNamed arrayNamed) {
        this.arrayNamed = arrayNamed;
    }

    @Override
    public GenerateResult generate(Generators.GenerationProvider locations, Generators.Writer writer) {
        var packages = new PackageManager(locations, arrayNamed);
        String content = makeValue(packages, arrayNamed);
        writer.write(packages, content);
        return new GenerateResult(packages, arrayNamed);
    }

    private static String makeValue(PackageManager packages, ArrayTypeNamed type) {
        packages.addImport(((TypeAttr.OperationType) type.element()).getOperation().getCommonOperation().makeOperation(packages).imports());
        packages.addImport(type.getOperation().getCommonOperation().makeDirectMemoryLayout(packages).getTypeImports());
        packages.useClass(CommonTypes.FFMTypes.MEMORY_SEGMENT);
        packages.useClass(CommonTypes.FFMTypes.VALUE_LAYOUT);
        packages.useClass(CommonTypes.FFMTypes.SEGMENT_ALLOCATOR);
        return """
                import java.util.List;
                import java.util.Objects;
                
                public class %1$s extends %11$s.AbstractRandomAccessList<%2$s> implements %11$s<%1$s, %2$s>, %8$s<%1$s> {
                    public static final %8$s.Operations<%2$s> ELE_OPERATIONS = %3$s;
                    public static final long LENGTH = %5$s;
                    public static final %8$s.Operations<%1$s> OPERATIONS = new %8$s.Operations<>(
                            (param, offset) -> new %1$s(%9$s.getAddr(param, offset).reinterpret(LENGTH * ELE_OPERATIONS.memoryLayout().byteSize())),
                            (source, dest, offset) -> %9$s.setAddr(dest, offset, source.ms),
                            ValueLayout.ADDRESS);
                
                    private final MemorySegment ms;
                
                    public %1$s(MemorySegment ms) {
                        this.ms = ms;
                    }
                
                    public %1$s(SegmentAllocator allocator) {
                        this.ms = allocator.allocate(OPERATIONS.memoryLayout(), LENGTH);
                    }
                
                    @Override
                    public FixedArrayOpI<%1$s, %2$s> operator() {
                        return new FixedArrayOpI<>() {
                            @Override
                            public %1$s reinterpret(long length) {
                                return new %1$s(ms.reinterpret(length * ELE_OPERATIONS.memoryLayout().byteSize()));
                            }
                
                            @Override
                            public %1$s reinterpret(%6$s<?> length) {
                                return reinterpret(length.operator().value());
                            }
                
                            @Override
                            public %1$s reinterpret() {
                                return new %1$s(ms.reinterpret(OPERATIONS.memoryLayout().byteSize()));
                            }
                
                            @Override
                            public %10$s<%2$s> pointerAt(%6$s<?> index) {
                                return pointerAt(index.operator().value());
                            }
                
                            @Override
                            public %10$s<%2$s> pointerAt(long index) {
                                Objects.checkIndex(index, size());
                                return new %10$s<>(ms.asSlice(index * ELE_OPERATIONS.memoryLayout().byteSize(), ELE_OPERATIONS.memoryLayout().byteSize()), ELE_OPERATIONS);
                            }
                
                            @Override
                            public List<%10$s<%2$s>> pointerList() {
                                return new AbstractRandomAccessList<>() {
                                    @Override
                                    public %10$s<%2$s> get(int index) {
                                        return pointerAt(index);
                                    }
                
                                    @Override
                                    public int size() {
                                        return %1$s.this.size();
                                    }
                                };
                            }
                
                            @Override
                            public %8$s.Operations<%2$s> elementOperation() {
                                return ELE_OPERATIONS;
                            }
                
                            @Override
                            public void setPointee(%2$s pointee) {
                                set(0, pointee);
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
                            public %2$s pointee() {
                                return get(0);
                            }
                
                            @Override
                            public MemorySegment value() {
                                return ms;
                            }
                
                            @Override
                            public %7$s longSize() {
                                return new %7$s(ms.byteSize() / ELE_OPERATIONS.memoryLayout().byteSize());
                            }
                        };
                    }
                
                    @Override
                    public %2$s set(int index, %2$s element) {
                        Objects.checkIndex(index, size());
                        ELE_OPERATIONS.copy().copyTo(element, ms, index * ELE_OPERATIONS.memoryLayout().byteSize());
                        return element;
                    }
                
                    @Override
                    public %2$s get(int index) {
                        Objects.checkIndex(index, size());
                        return ELE_OPERATIONS.constructor().create(ms, index * ELE_OPERATIONS.memoryLayout().byteSize());
                    }
                
                    @Override
                    public int size() {
                        return (int) (ms.byteSize() / ELE_OPERATIONS.memoryLayout().byteSize());
                    }
                }""".formatted(packages.getClassName(),
                packages.useClass((TypeAttr.GenerationType) type.element()),
                ((TypeAttr.OperationType) type.element()).getOperation().getCommonOperation().makeOperation(packages).str(), // 3
                type.getOperation().getCommonOperation().makeDirectMemoryLayout(packages),
                type.length(), //5
                packages.useClass(ValueInterface.I64I), //6
                packages.useClass(BindTypes.I64), // 7
                packages.useClass(BasicOperations.Info), // 8
                packages.useClass(SpecificTypes.MemoryUtils), // 9
                packages.useClass(BindTypes.Ptr), // 10
                packages.useClass(SpecificTypes.ArrayOp) // 11
        );
    }
}
