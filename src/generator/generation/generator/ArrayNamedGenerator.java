package generator.generation.generator;

import generator.Dependency;
import generator.Generators;
import generator.PackagePath;
import generator.generation.ArrayNamed;
import generator.types.ArrayTypeNamed;
import generator.types.CommonTypes;
import generator.types.TypeAttr;

public class ArrayNamedGenerator implements Generator {
    private final Dependency dependency;
    private final ArrayNamed arrayNamed;
    private final Generators.Writer writer;

    public ArrayNamedGenerator(ArrayNamed v, Dependency dependency, Generators.Writer writer) {
        this.dependency = dependency;
        this.arrayNamed = v;
        this.writer = writer;
    }

    @Override
    public void generate() {
        PackagePath packagePath = arrayNamed.getTypePkg().packagePath();
        String out = packagePath.makePackage();
        out += Generator.extractImports(arrayNamed, dependency);
        out += makeValue(packagePath, arrayNamed.getTypePkg().type());
        writer.write(packagePath, out);
    }

    private String makeValue(PackagePath packagePath, ArrayTypeNamed type) {
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
                }""".formatted(packagePath.getClassName(), ((TypeAttr.NamedType) type.element()).typeName(TypeAttr.NameType.RAW),
                ((TypeAttr.OperationType) type.element()).getOperation().getCommonOperation().makeOperation().str(), // 3
                type.getOperation().getCommonOperation().makeDirectMemoryLayout(), type.length(), //5
                CommonTypes.ValueInterface.I64I.typeName(TypeAttr.NameType.RAW), //6
                CommonTypes.BindTypes.I64.typeName(TypeAttr.NameType.RAW), // 7
                CommonTypes.BasicOperations.Info.typeName(TypeAttr.NameType.RAW), // 8
                CommonTypes.SpecificTypes.MemoryUtils.typeName(TypeAttr.NameType.RAW), // 9
                CommonTypes.BindTypes.Ptr.typeName(TypeAttr.NameType.RAW), // 10
                CommonTypes.SpecificTypes.ArrayOp.typeName(TypeAttr.NameType.RAW) // 11
                );
    }
}
