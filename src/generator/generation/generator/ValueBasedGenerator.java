package generator.generation.generator;

import generator.Dependency;
import generator.Generators;
import generator.PackagePath;
import generator.generation.ValueBased;
import generator.types.CommonTypes;
import generator.types.PointerType;
import generator.types.TypeAttr;
import generator.types.ValueBasedType;

public class ValueBasedGenerator implements Generator {
    private final Dependency dependency;
    private final ValueBased valueBased;
    private final Generators.Writer writer;

    public ValueBasedGenerator(ValueBased v, Dependency dependency, Generators.Writer writer) {
        this.dependency = dependency;
        this.valueBased = v;
        this.writer = writer;
    }

    @Override
    public void generate() {
        makeValue(valueBased.getTypePkg().packagePath(), valueBased.getTypePkg().type(), Generator.extractImports(valueBased, dependency));
    }

    private void makeValue(PackagePath path, ValueBasedType type, String imports) {
        String typeName = Generator.getTypeName(type);
        CommonTypes.BindTypes bindTypes = type.getBindTypes();
        if (bindTypes != CommonTypes.BindTypes.Ptr) {
            CommonGenerator.genValueBasedTypes(path, type.getBindTypes(), imports, type.typeName(TypeAttr.NameType.RAW), writer);
            return;
        }
        PointerType pointerType = type.getPointerType().orElseThrow();
        var pointee = ((TypeAttr.OperationType) pointerType.pointee());
        String pointeeName = Generator.getTypeName(pointerType.pointee());
        writer.write(path, """
                %1$s
                
                %2$s
                import java.util.Objects;
                
                public class %3$s implements %5$s<%3$s, %4$s>, %11$s<%3$s> {
                    public static final %11$s.Operations<%4$s> ELEMENT_OPERATIONS = %6$s;
                    public static final %11$s.Operations<%3$s> OPERATIONS = %5$s.makeOperations(%3$s::new);
                
                    private final MemorySegment segment;
                
                    private MemorySegment fitByteSize(MemorySegment segment) {
                        return segment.byteSize() == ELEMENT_OPERATIONS.memoryLayout().byteSize() ? segment : segment.reinterpret(ELEMENT_OPERATIONS.memoryLayout().byteSize());
                    }
                
                    public %3$s(MemorySegment segment) {
                        this.segment = fitByteSize(segment);
                    }
                
                    public %3$s(%9$s<?, %4$s> arrayOperation) {
                        this.segment = fitByteSize(arrayOperation.operator().value());
                    }
                
                    public %3$s(%14$s<MemorySegment> pointee) {
                        this.segment = fitByteSize(pointee.operator().value());
                    }
                
                    public %3$s(%7$s<%4$s> pointee) {
                        this.segment = fitByteSize(pointee.operator().value());
                    }
                
                    public static %12$s<%3$s> array(SegmentAllocator allocator, %10$s<?> len) {
                        return array(allocator, len.operator().value());
                    }
                
                    public static %12$s<%3$s> array(SegmentAllocator allocator, long len) {
                        return new %12$s<>(allocator, %3$s.OPERATIONS, len);
                    }
                
                    public static %13$s<%3$s> ptr(SegmentAllocator allocator) {
                        return new %13$s<>(allocator, %3$s.OPERATIONS);
                    }
                
                    @Override
                    public String toString() {
                        return "%3$s{" +
                                "segment=" + segment +
                                '}';
                    }
                
                    public MemorySegment value() {
                        return segment;
                    }
                
                    public %4$s pointee() {
                        return operator().pointee();
                    }
                
                    @Override
                    public %8$s<%3$s, %4$s> operator() {
                        return new %8$s<>() {
                            @Override
                            public MemorySegment value() {
                                return segment;
                            }
                
                            @Override
                            public %4$s pointee() {
                                return ELEMENT_OPERATIONS.constructor().create(segment, 0);
                            }
                
                            @Override
                            public %11$s.Operations<%3$s> getOperations() {
                                return OPERATIONS;
                            }
                
                            @Override
                            public %3$s self() {
                                return %3$s.this;
                            }
                
                            @Override
                            public %11$s.Operations<%4$s> elementOperation() {
                                return ELEMENT_OPERATIONS;
                            }
                
                            @Override
                            public void setPointee(%4$s pointee) {
                                ELEMENT_OPERATIONS.copy().copyTo(pointee, segment, 0);
                            }
                        };
                    }
                
                    @Override
                    public boolean equals(Object o) {
                        if (!(o instanceof %3$s ptr)) return false;
                        return Objects.equals(segment, ptr.segment);
                    }
                
                    @Override
                    public int hashCode() {
                        return Objects.hashCode(segment);
                    }
                }
                """.formatted(path.makePackage(), imports, typeName, pointeeName,
                CommonTypes.BindTypeOperations.PtrOp.typeName(TypeAttr.NameType.RAW), // 5
                pointee.getOperation().getCommonOperation().makeOperation().str(),
                CommonTypes.ValueInterface.PtrI.typeName(TypeAttr.NameType.RAW), // 7
                CommonTypes.BindTypeOperations.PtrOp.operatorTypeName(), // 8
                CommonTypes.SpecificTypes.ArrayOp.typeName(TypeAttr.NameType.RAW), // 9
                CommonTypes.ValueInterface.I64I.typeName(TypeAttr.NameType.RAW), // 10
                CommonTypes.BasicOperations.Info.typeName(TypeAttr.NameType.RAW), // 11
                CommonTypes.SpecificTypes.Array.typeName(TypeAttr.NameType.RAW), // 12
                CommonTypes.BindTypes.Ptr.typeName(TypeAttr.NameType.RAW), // 13
                CommonTypes.BasicOperations.Value.typeName(TypeAttr.NameType.RAW) // 14
        ));
    }
}
