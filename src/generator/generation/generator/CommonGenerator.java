package generator.generation.generator;

import generator.Generators;
import generator.PackageManager;
import generator.types.CommonTypes;
import generator.types.CommonTypes.BasicOperations;
import generator.types.CommonTypes.BindTypes;
import generator.types.CommonTypes.SpecificTypes;
import generator.types.CommonTypes.ValueInterface;
import generator.types.TypeAttr.NameType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static utils.CommonUtils.Assert;


public class CommonGenerator implements Generator {
    private final List<? extends CommonTypes.BaseType> baseTypes;
    private Generators.Writer writer;

    public CommonGenerator(CommonTypes.BaseType type) {
        this.baseTypes = Collections.singletonList(type);
    }

    public CommonGenerator(List<? extends CommonTypes.BaseType> type) {
        this.baseTypes = type;
    }

    @Override
    public GenerateResult generate(Generators.GenerationProvider locations, Generators.Writer writer) {
        this.writer = writer;
        ArrayList<PackageManager> packageManagers = new ArrayList<>();
        for (CommonTypes.BaseType baseType : baseTypes) {
            PackageManager packages = new PackageManager(locations, baseType);
            switch (baseType) {
                case BindTypes bindTypes -> genBindTypes(packages, bindTypes);
                case CommonTypes.BindTypeOperations btOp -> genBindTypeOp(packages, btOp);
                case ValueInterface v -> genValueInterface(packages, v);
                case SpecificTypes specificTypes -> {
                    switch (specificTypes) {
                        case Array -> genArray(packages);
                        case FlatArray -> genFlatArray(packages);
                        case Str -> genStr(packages);
                        case FunctionUtils -> genFunctionUtils(packages);
                        case ArrayOp -> genArrayOp(packages);
                        case FlatArrayOp -> genFlatArrayOp(packages);
                        case StructOp -> genStructOp(packages);
                        case MemoryUtils -> genMemoryUtils(packages);
                    }
                }
                case CommonTypes.FFMTypes _ -> {
                }
                case BasicOperations ext -> {
                    switch (ext) {
                        case Operation -> genOperation(packages);
                        case Info -> genInfo(packages);
                        case Value -> genValue(packages);
                        case PteI -> genPteI(packages);
                        case ArrayI -> genArrayI(packages);
                        case StructI -> genStructI(packages);
                    }
                }
            }
            packageManagers.add(packages);
        }
        return new GenerateResult(packageManagers, baseTypes);
    }

    private void genStructI(PackageManager packages) {
        packages.useClass(CommonTypes.FFMTypes.MEMORY_SEGMENT);
        writer.write(packages, """
                public interface %4$s<E> extends %3$s<MemorySegment> {
                    static <I> %4$s<I> of(MemorySegment value) {
                        return new %4$s<>() {
                            @Override
                            public ValueOp<MemorySegment> operator() {
                                return () -> value;
                            }
                
                            @Override
                            public String toString() {
                                return String.valueOf(value);
                            }
                        };
                    }
                
                    static <I> %4$s<I> of(%4$s<?> value) {
                        return of(value.operator().value());
                    }
                }
                """.formatted(null, null,
                packages.useClass(BasicOperations.Value), // 3
                packages.useClass(BasicOperations.StructI) // 4
        ));
    }

    private void genArrayI(PackageManager packages) {
        packages.useClass(CommonTypes.FFMTypes.MEMORY_SEGMENT);
        writer.write(packages, """
                public interface %4$s<E> extends %3$s<MemorySegment> {
                    static <I> %4$s<I> of(MemorySegment value) {
                        return new %4$s<>() {
                            @Override
                            public ValueOp<MemorySegment> operator() {
                                return () -> value;
                            }
                
                            @Override
                            public String toString() {
                                return String.valueOf(value);
                            }
                        };
                    }
                
                    static <I> %4$s<I> of(%4$s<?> value) {
                        return of(value.operator().value());
                    }
                }
                """.formatted(null, null,
                packages.useClass(BasicOperations.Value), // 3
                packages.useClass(BasicOperations.ArrayI) // 4
        ));
    }

    private void genBindTypeOp(PackageManager packages, CommonTypes.BindTypeOperations btOp) {
        if (btOp.getValue().getPrimitive().noJavaPrimitive()) {
            var str = """
                    import java.lang.foreign.MemorySegment;
                    
                    public interface %3$s<T> extends %5$s<T>, %4$s<T> {
                        @Override
                        %7$s<T> operator();
                    
                        interface %7$s<T> extends %5$s.InfoOp<T>, %6$s.ValueOp<MemorySegment> {
                    
                        }
                    }
                    """.formatted(null, null,
                    packages.useClass(btOp), // 3
                    packages.useClass(btOp.getValue()),
                    packages.useClass(BasicOperations.Info), // 5
                    packages.useClass(BasicOperations.Value), // 6
                    btOp.operatorTypeName() // 7
            );
            writer.write(packages, str);
            return;
        }
        packages.useClass(CommonTypes.FFMTypes.VALUE_LAYOUT);
        String str = """
                import java.util.function.Function;
                
                public interface %3$s<T> extends %9$s<T>, %4$s<T> {
                    @Override
                    %6$s<T> operator();
                
                    interface %6$s<T> extends %9$s.InfoOp<T>, %10$s.ValueOp<%5$s> {
                
                    }
                
                    static <T extends %4$s<?>> %9$s.Operations<T> makeOperations(Function<%5$s, T> constructor) {
                        return new %9$s.Operations<>(
                                (param, offset) -> constructor.apply(%11$s.get%8$s(param, offset)),
                                (source, dest, offset) -> %11$s.set%8$s(dest, offset, source.operator().value()),
                                %7$s);
                    }
                }
                """.formatted(null, null,
                packages.useClass(btOp), // 3
                packages.useClass(btOp.getValue()),
                btOp.getValue().getPrimitive().getBoxedTypeName(), // 5
                btOp.operatorTypeName(),
                btOp.getValue().getPrimitive().getMemoryLayout(packages).getMemoryLayout(packages), // 7
                btOp.getValue().getPrimitive().getMemoryUtilName(),
                packages.useClass(BasicOperations.Info), // 9
                packages.useClass(BasicOperations.Value), // 10
                packages.useClass(SpecificTypes.MemoryUtils) // 11
        );
        if (btOp == CommonTypes.BindTypeOperations.PtrOp) {
            str = """
                    import java.lang.foreign.MemorySegment;
                    import java.util.function.Function;
                    
                    public interface %3$s<S, E> extends %7$s<S>, %5$s<E>, %6$s<E> {
                        @Override
                        %4$s<S, E> operator();
                    
                        interface %4$s<S, E> extends %7$s.InfoOp<S>, %8$s.ValueOp<MemorySegment>, PointeeOp<E> {
                            %7$s.Operations<E> elementOperation();
                    
                            void setPointee(E pointee);
                        }
                    
                        static <T extends %5$s<?>> %7$s.Operations<T> makeOperations(Function<MemorySegment, T> constructor) {
                            return new %7$s.Operations<>(
                                        (param, offset) -> constructor.apply(%9$s.getAddr(param, offset)),
                                        (source, dest, offset) -> %9$s.setAddr(dest, offset, source.operator().value()),
                                        ValueLayout.ADDRESS);
                        }
                    }
                    """.formatted(null, null,
                    packages.useClass(btOp),
                    btOp.operatorTypeName(),
                    packages.useClass(btOp.getValue()),
                    packages.useClass(BasicOperations.PteI),
                    packages.useClass(BasicOperations.Info), // 7
                    packages.useClass(BasicOperations.Value), // 8
                    packages.useClass(SpecificTypes.MemoryUtils) // 9
            );
        }
        writer.write(packages, str);
    }

    private void genOperation(PackageManager packages) {
        writer.write(packages, """
                public interface %s {
                    Object operator();
                }
                """.formatted(packages.useClass(BasicOperations.Operation)));
    }

    private void genValue(PackageManager packages) {
        writer.write(packages, """
                public interface %4$s<T> extends %3$s {
                    interface ValueOp<T> {
                        T value();
                    }
                
                    @Override
                    ValueOp<T> operator();
                }
                """.formatted(null, null,
                packages.useClass(BasicOperations.Operation), //3
                packages.useClass(BasicOperations.Value) // 4
        ));
    }

    private void genArrayOp(PackageManager packages) {
        packages.useClass(CommonTypes.FFMTypes.MEMORY_SEGMENT);
        writer.write(packages, """
                import java.util.AbstractList;
                import java.util.List;
                import java.util.RandomAccess;
                
                public interface %3$s<A, E> extends %11$s<A>, %7$s<E>, %4$s<A, E>, List<E> {
                    interface ArrayOpI<A, E> extends %12$s.ValueOp<MemorySegment>, %11$s.InfoOp<A>, %5$s<A, E> {
                        A reinterpret(long length);
                
                        default A reinterpret(%8$s<?> length) {
                            return reinterpret(length.operator().value());
                        }
                
                        default A reinterpret(%10$s<?> length) {
                            return reinterpret(length.operator().value());
                        }
                
                        %6$s<E> pointerAt(long index);
                
                        default %6$s<E> pointerAt(%8$s<?> index) {
                            return pointerAt(index.operator().value());
                        }
                
                        default %6$s<E> pointerAt(%10$s<?> index) {
                            return pointerAt(index.operator().value());
                        }
                
                        List<%6$s<E>> pointerList();
                
                        %9$s longSize();
                    }
                
                    abstract class AbstractRandomAccessList<E> extends AbstractList<E> implements RandomAccess {
                    }
                
                    interface FixedArrayOpI<A, E> extends ArrayOpI<A, E> {
                        A reinterpret();
                    }
                
                    ArrayOpI<A, E> operator();
                }""".formatted(null, null,
                packages.useClass(SpecificTypes.ArrayOp),
                packages.useClass(CommonTypes.BindTypeOperations.PtrOp), // 4
                CommonTypes.BindTypeOperations.PtrOp.operatorTypeName(),
                packages.useClass(BindTypes.Ptr),
                packages.useClass(BasicOperations.ArrayI),// 7
                packages.useClass(ValueInterface.I64I),
                packages.useClass(BindTypes.I64), // 9
                packages.useClass(ValueInterface.I32I), // 10
                packages.useClass(BasicOperations.Info), // 11
                packages.useClass(BasicOperations.Value) // 12
        ));
    }

    private void genFlatArrayOp(PackageManager packages) {
        packages.useClass(CommonTypes.FFMTypes.MEMORY_SEGMENT);
        writer.write(packages, """
                import java.util.AbstractList;
                import java.util.List;
                import java.util.RandomAccess;
                
                public interface %3$s<A, E> extends %11$s<A>, %7$s<E>, List<E> {
                    interface FlatArrayOpI<A, E> extends %12$s.ValueOp<MemorySegment>, %11$s.InfoOp<A> {
                        A reinterpret(long length);
                
                        default A reinterpret(%8$s<?> length) {
                            return reinterpret(length.operator().value());
                        }
                
                        default A reinterpret(%10$s<?> length) {
                            return reinterpret(length.operator().value());
                        }
                
                        %6$s<E> pointerAt(long index);
                
                        default %6$s<E> pointerAt(%8$s<?> index) {
                            return pointerAt(index.operator().value());
                        }
                
                        default %6$s<E> pointerAt(%10$s<?> index) {
                            return pointerAt(index.operator().value());
                        }
                
                        List<%6$s<E>> pointerList();
                
                        %9$s longSize();
                
                        %11$s.Operations<E> elementOperation();
                    }
                
                    abstract class AbstractRandomAccessList<E> extends AbstractList<E> implements RandomAccess {
                    }
                
                    interface FixedFlatArrayOpI<A, E> extends FlatArrayOpI<A, E> {
                        A reinterpret();
                    }
                
                    FlatArrayOpI<A, E> operator();
                }""".formatted(
                null, null,
                packages.useClass(SpecificTypes.FlatArrayOp),
                null, // 4
                null,
                packages.useClass(BindTypes.Ptr),
                packages.useClass(BasicOperations.ArrayI), // 7
                packages.useClass(ValueInterface.I64I),
                packages.useClass(BindTypes.I64), // 9
                packages.useClass(ValueInterface.I32I), // 10
                packages.useClass(BasicOperations.Info), // 11
                packages.useClass(BasicOperations.Value) // 12
        ));
    }

    private void genStructOp(PackageManager packages) {
        packages.useClass(CommonTypes.FFMTypes.MEMORY_SEGMENT);
        packages.useClass(CommonTypes.FFMTypes.MEMORY_LAYOUT);
        writer.write(packages, """
                import java.util.function.Function;
                
                public interface %6$s<E> extends %4$s<E>, %3$s<E> {
                
                    @Override
                    StructOpI<E> operator();
                
                    interface StructOpI<E> extends %4$s.InfoOp<E>, %5$s.ValueOp<MemorySegment> {
                        E reinterpret();
                    }
                
                    static <E extends %6$s<?>> %4$s.Operations<E> makeOperations(Function<MemorySegment, E> constructor, MemoryLayout layout) {
                        return new %4$s.Operations<>(
                                (param, offset) -> constructor.apply(param.asSlice(offset, layout)),
                                (source, dest, offset) -> dest.asSlice(offset).copyFrom(source.operator().value()), layout);
                    }
                }""".formatted(null, null,
                packages.useClass(BasicOperations.StructI), // 3
                packages.useClass(BasicOperations.Info), // 4
                packages.useClass(BasicOperations.Value), // 5
                packages.useClass(SpecificTypes.StructOp) // 6
        ));
    }

    private void genPteI(PackageManager packages) {
        packages.useClass(CommonTypes.FFMTypes.MEMORY_SEGMENT);
        writer.write(packages, """
                public interface %3$s<E> extends %4$s {
                    interface PointeeOp<E> extends %5$s.ValueOp<MemorySegment> {
                        E pointee();
                    }
                
                    @Override
                    PointeeOp<E> operator();
                }""".formatted(null, null,
                packages.useClass(BasicOperations.PteI), // 3
                packages.useClass(BasicOperations.Operation),
                packages.useClass(BasicOperations.Value) // 5
        ));
    }

    private void genInfo(PackageManager packages) {
        packages.useClass(CommonTypes.FFMTypes.MEMORY_SEGMENT);
        packages.useClass(CommonTypes.FFMTypes.MEMORY_LAYOUT);
        writer.write(packages, """
                public interface %3$s<T> extends %4$s {
                    interface InfoOp<S> {
                        Operations<S> getOperations();
                
                        S self();
                    }
                
                    record Operations<T>(Constructor<? extends T, MemorySegment> constructor, Copy<? super T> copy, MemoryLayout memoryLayout) {
                        public interface Constructor<R, P> {
                            R create(P param, long offset);
                        }
                
                        public interface Copy<S> {
                            void copyTo(S source, MemorySegment dest, long offset);
                        }
                    }
                
                    InfoOp<T> operator();
                
                    static <I> Operations<I> makeOperations() {
                        return new Operations<>((_, _) -> {
                            throw new UnsupportedOperationException();
                        }, (_, _, _) -> {
                            throw new UnsupportedOperationException();
                        }, MemoryLayout.structLayout());
                    }
                }
                """.formatted(null, null,
                packages.useClass(BasicOperations.Info), // 3
                packages.useClass(BasicOperations.Operation) // 4
        ));
    }

    public static final String ARRAY_MAKE_OPERATION_METHOD = "makeOperations";

    private void genArray(PackageManager packages) {
        packages.useClass(CommonTypes.FFMTypes.SEGMENT_ALLOCATOR);
        packages.useClass(CommonTypes.FFMTypes.MEMORY_SEGMENT);
        packages.useClass(CommonTypes.FFMTypes.VALUE_LAYOUT);
        writer.write(packages, """
                import java.util.*;
                
                public class %12$s<E> extends %3$s.AbstractRandomAccessList<E> implements %3$s<%12$s<E>, E>, %10$s<%12$s<E>> {
                    public static <I> %10$s.Operations<%12$s<I>> makeOperations(Operations<I> operation, %8$s<?> len) {
                        return makeOperations(operation, len.operator().value());
                    }
                
                    public static <I> %10$s.Operations<%12$s<I>> makeOperations(Operations<I> operation, long len) {
                        return new %10$s.Operations<>((param, offset) -> new %12$s<>(%11$s.getAddr(param, offset).reinterpret(len * operation.memoryLayout().byteSize()),
                                operation), (source, dest, offset) -> %11$s.setAddr(dest, offset, source.ptr), ValueLayout.ADDRESS);
                    }
                
                    public static <I> %10$s.Operations<%12$s<I>> makeOperations(Operations<I> operation) {
                        return new %10$s.Operations<>((param, offset) -> new %12$s<>(%11$s.getAddr(param, offset),
                                operation), (source, dest, offset) -> %11$s.setAddr(dest, offset, source.ptr), ValueLayout.ADDRESS);
                    }
                
                    protected final MemorySegment ptr;
                    protected final %10$s.Operations<E> operations;
                
                    public %12$s(%5$s<E> ptr, %10$s.Operations<E> operations) {
                        this.ptr = ptr.operator().value();
                        this.operations = operations;
                    }
                
                    public %12$s(%4$s<?, E> ptr) {
                        this.ptr = ptr.operator().value();
                        this.operations = ptr.operator().elementOperation();
                    }
                
                    public %12$s(MemorySegment ptr, %10$s.Operations<E> operations) {
                        this.ptr = ptr;
                        this.operations = operations;
                    }
                
                    public %12$s(SegmentAllocator allocator, %10$s.Operations<E> operations, Collection<E> elements) {
                        this.operations = operations;
                        this.ptr = allocator.allocate(operations.memoryLayout(), elements.size());
                        int i = 0;
                        for (E element : elements) {
                            operations.copy().copyTo(element, ptr, operations.memoryLayout().byteSize() * i);
                            i++;
                        }
                    }
                
                    public %12$s(SegmentAllocator allocator, %10$s.Operations<E> operations, long len) {
                        this.operations = operations;
                        this.ptr = allocator.allocate(operations.memoryLayout(), len);
                    }
                
                    public %12$s(SegmentAllocator allocator, %10$s.Operations<E> operations, %9$s<?> len) {
                        this(allocator, operations, len.operator().value());
                    }
                
                    public %12$s(SegmentAllocator allocator, %10$s.Operations<E> operations, %8$s<?> len) {
                        this(allocator, operations, len.operator().value());
                    }
                
                    public E get(int index) {
                        Objects.checkIndex(index, size());
                        return operations.constructor().create(ptr, index * operations.memoryLayout().byteSize());
                    }
                
                    private E get(long index) {
                        Objects.checkIndex(index, sizeLong());
                        return operations.constructor().create(ptr, index * operations.memoryLayout().byteSize());
                    }
                
                    public E get(%9$s<?> index) {
                        return get(index.operator().value());
                    }
                
                    public E get(%8$s<?> index) {
                        return get(index.operator().value());
                    }
                
                    @Override
                    public E set(int index, E element) {
                        Objects.checkIndex(index, size());
                        operations.copy().copyTo(element, ptr, index * operations.memoryLayout().byteSize());
                        return element;
                    }
                
                    public E set(%9$s<?> index, E element) {
                        return set(index.operator().value(), element);
                    }
                
                    public E set(%8$s<?> index, E element) {
                        return set(index.operator().value(), element);
                    }
                
                    public E set(long index, E element) {
                        Objects.checkIndex(index, sizeLong());
                        operations.copy().copyTo(element, ptr, index * operations.memoryLayout().byteSize());
                        return element;
                    }
                
                    @Override
                    public ArrayOpI<%12$s<E>, E> operator() {
                        return new ArrayOpI<>() {
                            @Override
                            public %10$s.Operations<E> elementOperation() {
                                return operations;
                            }
                
                            @Override
                            public void setPointee(E pointee) {
                                set(0, pointee);
                            }
                
                            @Override
                            public %12$s<E> reinterpret(long length) {
                                return new %12$s<>(ptr.reinterpret(length * operations.memoryLayout().byteSize()), operations);
                            }
                
                            @Override
                            public %12$s<E> reinterpret(%8$s<?> length) {
                                return reinterpret(length.operator().value());
                            }
                
                            @Override
                            public %6$s<E> pointerAt(long index) {
                                Objects.checkIndex(index, size());
                                return new %6$s<>(ptr.asSlice(index * operations.memoryLayout().byteSize(), operations.memoryLayout()), operations);
                            }
                
                            @Override
                            public %6$s<E> pointerAt(%8$s<?> index) {
                                return pointerAt(index.operator().value());
                            }
                
                            @Override
                            public List<%6$s<E>> pointerList() {
                                return new %3$s.AbstractRandomAccessList<>() {
                                    @Override
                                    public %6$s<E> get(int index) {
                                        return pointerAt(index);
                                    }
                
                                    @Override
                                    public int size() {
                                        return %12$s.this.size();
                                    }
                                };
                            }
                
                            @Override
                            public %10$s.Operations<%12$s<E>> getOperations() {
                                return makeOperations(operations, sizeLong());
                            }
                
                            @Override
                            public %12$s<E> self() {
                                return %12$s.this;
                            }
                
                            @Override
                            public E pointee() {
                                return get(0);
                            }
                
                            @Override
                            public MemorySegment value() {
                                return ptr;
                            }
                
                            @Override
                            public %7$s longSize() {
                                return new %7$s(ptr.byteSize() / operations.memoryLayout().byteSize());
                            }
                        };
                    }
                
                    public %6$s<E> pointerAt(long index) {
                        return operator().pointerAt(index);
                    }
                
                    public %6$s<E> pointerAt(%8$s<?> index) {
                        return operator().pointerAt(index.operator().value());
                    }
                
                    public List<%6$s<E>> pointerList() {
                        return operator().pointerList();
                    }
                
                    @Override
                    public int size() {
                        return (int) (ptr.byteSize() / operations.memoryLayout().byteSize());
                    }
                
                    private long sizeLong() {
                        return longSize().operator().value();
                    }
                
                    public %7$s longSize() {
                        return operator().longSize();
                    }
                
                    @Override
                    public %12$s<E> subList(int fromIndex, int toIndex) {
                        Objects.checkFromToIndex(fromIndex, toIndex, size());
                        return new %12$s<E>(ptr.asSlice(fromIndex * operations.memoryLayout().byteSize(),
                                (toIndex - fromIndex) * operations.memoryLayout().byteSize()), operations);
                    }
                
                    public %12$s<E> subList(%8$s<?> fromIndex, %8$s<?> toIndex) {
                        Objects.checkFromToIndex(fromIndex.operator().value(), toIndex.operator().value(), sizeLong());
                        return new %12$s<E>(ptr.asSlice(fromIndex.operator().value() * operations.memoryLayout().byteSize(),
                                (toIndex.operator().value() - fromIndex.operator().value()) * operations.memoryLayout().byteSize()), operations);
                    }
                }
                """.formatted(null, null,
                packages.useClass(SpecificTypes.ArrayOp),
                packages.useClass(CommonTypes.BindTypeOperations.PtrOp),
                packages.useClass(ValueInterface.PtrI), // 5
                packages.useClass(BindTypes.Ptr),  // 6
                packages.useClass(BindTypes.I64), // 7
                packages.useClass(ValueInterface.I64I), // 8
                packages.useClass(ValueInterface.I32I), // 9
                packages.useClass(BasicOperations.Info), // 10
                packages.useClass(SpecificTypes.MemoryUtils), // 11
                packages.useClass(SpecificTypes.Array) // 12
        ));
    }

    private void genFlatArray(PackageManager packages) {
        packages.useClass(CommonTypes.FFMTypes.SEGMENT_ALLOCATOR);
        packages.useClass(CommonTypes.FFMTypes.MEMORY_SEGMENT);
        packages.useClass(CommonTypes.FFMTypes.MEMORY_LAYOUT);
        writer.write(packages, """
                import java.util.*;
                
                public class %12$s<E> extends %3$s.AbstractRandomAccessList<E> implements %3$s<%12$s<E>, E>, %10$s<%12$s<E>> {
                    public static <I> Operations<%12$s<I>> makeOperations(Operations<I> operation, %8$s<?> len) {
                        return makeOperations(operation, len.operator().value());
                    }
                    public static <I> Operations<%12$s<I>> makeOperations(Operations<I> operation, long len) {
                        return new Operations<>((param, offset) -> new %12$s<>(param.asSlice(offset, len * operation.memoryLayout().byteSize()),
                                operation), (source, dest, offset) -> %11$s.memcpy(source.ptr, 0, dest, offset, len * operation.memoryLayout().byteSize()),
                                MemoryLayout.sequenceLayout(len, operation.memoryLayout()));
                    }
                
                    protected final MemorySegment ptr;
                    protected final %10$s.Operations<E> operations;
                
                    public %12$s(%5$s<E> ptr, %10$s.Operations<E> operations) {
                        this.ptr = ptr.operator().value();
                        this.operations = operations;
                    }
                
                    public %12$s(%4$s<?, E> ptr) {
                        this.ptr = ptr.operator().value();
                        this.operations = ptr.operator().elementOperation();
                    }
                
                    public %12$s(MemorySegment ptr, %10$s.Operations<E> operations) {
                        this.ptr = ptr;
                        this.operations = operations;
                    }
                
                    public %12$s(SegmentAllocator allocator, %10$s.Operations<E> operations, Collection<E> elements) {
                        this.operations = operations;
                        this.ptr = allocator.allocate(operations.memoryLayout(), elements.size());
                        int i = 0;
                        for (E element : elements) {
                            operations.copy().copyTo(element, ptr, operations.memoryLayout().byteSize() * i);
                            i++;
                        }
                    }
                
                    public %12$s(SegmentAllocator allocator, %10$s.Operations<E> operations, %9$s<?> len) {
                        this(allocator, operations, len.operator().value());
                    }
                
                    public %12$s(SegmentAllocator allocator, %10$s.Operations<E> operations, long len) {
                        this.operations = operations;
                        this.ptr = allocator.allocate(operations.memoryLayout(), len);
                    }
                
                    public %12$s(SegmentAllocator allocator, %10$s.Operations<E> operations, %8$s<?> len) {
                        this(allocator, operations, len.operator().value());
                    }
                
                    public E get(int index) {
                        Objects.checkIndex(index, size());
                        return operations.constructor().create(ptr, index * operations.memoryLayout().byteSize());
                    }
                
                    private E get(long index) {
                        Objects.checkIndex(index, sizeLong());
                        return operations.constructor().create(ptr, index * operations.memoryLayout().byteSize());
                    }
                
                    public E get(%9$s<?> index) {
                        return get(index.operator().value());
                    }
                
                    public E get(%8$s<?> index) {
                        return get(index.operator().value());
                    }
                
                    @Override
                    public E set(int index, E element) {
                        Objects.checkIndex(index, size());
                        operations.copy().copyTo(element, ptr, index * operations.memoryLayout().byteSize());
                        return element;
                    }
                
                    public E set(%9$s<?> index, E element) {
                        return set(index.operator().value(), element);
                    }
                
                    public E set(%8$s<?> index, E element) {
                        return set(index.operator().value(), element);
                    }
                
                    public E set(long index, E element) {
                        Objects.checkIndex(index, sizeLong());
                        operations.copy().copyTo(element, ptr, index * operations.memoryLayout().byteSize());
                        return element;
                    }
                
                    @Override
                    public FlatArrayOpI<%12$s<E>, E> operator() {
                        return new FlatArrayOpI<>() {
                            @Override
                            public %10$s.Operations<E> elementOperation() {
                                return operations;
                            }
                
                            @Override
                            public %12$s<E> reinterpret(long length) {
                                return new %12$s<>(ptr.reinterpret(length * operations.memoryLayout().byteSize()), operations);
                            }
                
                            @Override
                            public %12$s<E> reinterpret(%8$s<?> length) {
                                return reinterpret(length.operator().value());
                            }
                
                            @Override
                            public %6$s<E> pointerAt(long index) {
                                Objects.checkIndex(index, size());
                                return new %6$s<>(ptr.asSlice(index * operations.memoryLayout().byteSize(), operations.memoryLayout()), operations);
                            }
                
                            @Override
                            public %6$s<E> pointerAt(%8$s<?> index) {
                                return pointerAt(index.operator().value());
                            }
                
                            @Override
                            public List<%6$s<E>> pointerList() {
                                return new %3$s.AbstractRandomAccessList<>() {
                                    @Override
                                    public %6$s<E> get(int index) {
                                        return pointerAt(index);
                                    }
                
                                    @Override
                                    public int size() {
                                        return %12$s.this.size();
                                    }
                                };
                            }
                
                            @Override
                            public %10$s.Operations<%12$s<E>> getOperations() {
                                return makeOperations(operations, sizeLong());
                            }
                
                            @Override
                            public %12$s<E> self() {
                                return %12$s.this;
                            }
                
                            @Override
                            public MemorySegment value() {
                                return ptr;
                            }
                
                            @Override
                            public %7$s longSize() {
                                return new %7$s(ptr.byteSize() / operations.memoryLayout().byteSize());
                            }
                        };
                    }
                
                    public %6$s<E> pointerAt(long index) {
                        return operator().pointerAt(index);
                    }
                
                    public %6$s<E> pointerAt(%8$s<?> index) {
                        return operator().pointerAt(index.operator().value());
                    }
                
                    public List<%6$s<E>> pointerList() {
                        return operator().pointerList();
                    }
                
                    @Override
                    public int size() {
                        return (int) (ptr.byteSize() / operations.memoryLayout().byteSize());
                    }
                
                    private long sizeLong() {
                        return longSize().operator().value();
                    }
                
                    public %7$s longSize() {
                        return operator().longSize();
                    }
                
                    @Override
                    public %12$s<E> subList(int fromIndex, int toIndex) {
                        Objects.checkFromToIndex(fromIndex, toIndex, size());
                        return new %12$s<E>(ptr.asSlice(fromIndex * operations.memoryLayout().byteSize(),
                                (toIndex - fromIndex) * operations.memoryLayout().byteSize()), operations);
                    }
                
                    public %12$s<E> subList(%8$s<?> fromIndex, %8$s<?> toIndex) {
                        Objects.checkFromToIndex(fromIndex.operator().value(), toIndex.operator().value(), sizeLong());
                        return new %12$s<E>(ptr.asSlice(fromIndex.operator().value() * operations.memoryLayout().byteSize(),
                                (toIndex.operator().value() - fromIndex.operator().value()) * operations.memoryLayout().byteSize()), operations);
                    }
                }
                """.formatted(null, null,
                packages.useClass(SpecificTypes.FlatArrayOp),
                packages.useClass(CommonTypes.BindTypeOperations.PtrOp),
                packages.useClass(ValueInterface.PtrI), // 5
                packages.useClass(BindTypes.Ptr),
                packages.useClass(BindTypes.I64), // 7
                packages.useClass(ValueInterface.I64I), // 8
                packages.useClass(ValueInterface.I32I), // 9
                packages.useClass(BasicOperations.Info), // 10
                packages.useClass(SpecificTypes.MemoryUtils), // 11
                packages.useClass(SpecificTypes.FlatArray) // 12
        ));
    }

    private void genStr(PackageManager packages) {
        writer.write(packages, """
                import java.lang.foreign.*;
                import java.lang.invoke.MethodHandle;
                import java.nio.charset.StandardCharsets;
                import java.util.Arrays;
                import java.util.Collection;
                import java.util.List;
                import java.util.Optional;
                import java.util.stream.Stream;
                
                public class %5$s extends %3$s.AbstractRandomAccessList<%11$s> implements %3$s<%5$s, %11$s>, %10$s<%5$s> {
                    public static final %10$s.Operations<%5$s> OPERATIONS = new %10$s.Operations<>(
                            (param, offset) -> new %5$s(param.get(ValueLayout.ADDRESS, offset)),
                            (source, dest, offset) -> dest.set(ValueLayout.ADDRESS, offset, source.ptr), ValueLayout.ADDRESS);
                    private static final MethodHandle STRLEN;
                
                    static {
                        Optional<MemorySegment> strlen = Linker.nativeLinker().defaultLookup().find("strlen");
                        if (strlen.isEmpty()) {
                            STRLEN = null;
                        } else {
                            FunctionDescriptor fd = FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS);
                            STRLEN = Linker.nativeLinker().downcallHandle(strlen.get(), fd, Linker.Option.critical(false));
                        }
                    }
                
                    private final MemorySegment ptr;
                
                    @Override
                    public %11$s get(int index) {
                        return new %11$s(ptr.getAtIndex(ValueLayout.JAVA_BYTE, index));
                    }
                
                    private static %12$s<%5$s> makeArray(SegmentAllocator allocator, Stream<String> ss) {
                        List<%5$s> list = ss.map(s -> new %5$s(allocator, s)).toList();
                        return new %12$s<>(allocator, list.getFirst().operator().getOperations(), list);
                    }
                
                    private static long strlen(MemorySegment segment) {
                        if (STRLEN != null) {
                            try {
                                return (long) STRLEN.invokeExact(segment);
                            } catch (Throwable _) {
                            }
                        }
                        long count = 0;
                        while (segment.get(ValueLayout.JAVA_BYTE, count) != 0) {
                            count++;
                        }
                        return count;
                    }
                
                
                    public static %12$s<%5$s> array(SegmentAllocator allocator, String[] strings) {
                        return makeArray(allocator, Arrays.stream(strings));
                    }
                
                    public static %12$s<%5$s> array(SegmentAllocator allocator, Collection<String> strings) {
                        return makeArray(allocator, strings.stream());
                    }
                
                    public static %13$s<%5$s> ptr(SegmentAllocator allocator, String string) {
                        return new %13$s<>(allocator, new %5$s(allocator, string));
                    }
                
                    public %5$s(MemorySegment ptr) {
                        if (ptr.address() != 0 && ptr.isNative()) {
                            ptr = ptr.reinterpret(Long.MAX_VALUE);
                        }
                        this.ptr = ptr;
                    }
                
                    public %5$s(%6$s<? extends %7$s<?>> ptr) {
                        this(ptr.operator().value());
                    }
                
                    public %5$s(SegmentAllocator allocator, String s) {
                        this(allocator.allocateFrom(s, StandardCharsets.UTF_8));
                    }
                
                    public String get() {
                        return ptr.address() == 0 && ptr.isNative() ? null : ptr.getString(0, StandardCharsets.UTF_8);
                    }
                
                    private String safeToString() {
                        if (ptr.address() == 0 && ptr.isNative()) {
                            return null;
                        }
                        try {
                            return ptr.getString(0, StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            return null;
                        }
                    }
                
                    @Override
                    public int size() {
                        return (int) operator().longSize().value();
                    }
                
                    @Override
                    public String toString() {
                        String s = safeToString();
                        if (s != null)
                            return s;
                        return "Str{ptr=" + ptr + '}';
                    }
                
                    private long strlen = -1;
                
                    @Override
                    public ArrayOpI<%5$s, %11$s> operator() {
                        return new ArrayOpI<>() {
                            @Override
                            public %10$s.Operations<%11$s> elementOperation() {
                                return %11$s.OPERATIONS;
                            }
                
                            @Override
                            public %5$s reinterpret(long length) {
                                return new %5$s(ptr.reinterpret(length));
                            }
                
                            @Override
                            public %5$s reinterpret(%8$s<?> length) {
                                return reinterpret(length.operator().value());
                            }
                
                            @Override
                            public %4$s<%11$s> pointerAt(%8$s<?> index) {
                                return pointerAt(index.operator().value());
                            }
                
                            @Override
                            public %4$s<%11$s> pointerAt(long index) {
                                return new %4$s<>(ptr.asSlice(index, 1), %11$s.OPERATIONS);
                            }
                
                            @Override
                            public %10$s.Operations<%5$s> getOperations() {
                                return OPERATIONS;
                            }
                
                            @Override
                            public %5$s self() {
                                return %5$s.this;
                            }
                
                            @Override
                            public %11$s pointee() {
                                return get(0);
                            }
                
                            @Override
                            public void setPointee(%11$s pointee) {
                                set(0, pointee);
                            }
                
                            @Override
                            public List<%4$s<%11$s>> pointerList() {
                                return new %3$s.AbstractRandomAccessList<>() {
                                    @Override
                                    public %4$s<%11$s> get(int index) {
                                        return pointerAt(index);
                                    }
                
                                    @Override
                                    public int size() {
                                        return %5$s.this.size();
                                    }
                                };
                            }
                
                            @Override
                            public MemorySegment value() {
                                return ptr;
                            }
                
                            @Override
                            public %9$s longSize() {
                                if (strlen == -1 && ptr.isNative() && ptr.address() != 0) {
                                    strlen = 1 + strlen(ptr);
                                    return new %9$s(strlen);
                                }
                                return new %9$s(ptr.byteSize());
                            }
                        };
                    }
                }
                """.formatted(null, null,
                packages.useClass(SpecificTypes.ArrayOp),// 3
                packages.useClass(BindTypes.Ptr),
                packages.useClass(SpecificTypes.Str),// 5
                packages.useClass(ValueInterface.PtrI),
                packages.useClass(ValueInterface.I8I),
                packages.useClass(ValueInterface.I64I),//8
                packages.useClass(BindTypes.I64),
                packages.useClass(BasicOperations.Info), // 10
                packages.useClass(BindTypes.I8), // 11
                packages.useClass(SpecificTypes.Array), // 12
                packages.useClass(CommonTypes.BindTypes.Ptr) // 13
        ));
    }

    private void genValueInterface(PackageManager packages, ValueInterface type) {
        if (type.getPrimitive().noJavaPrimitive()) {
            writer.write(packages, """
                    import java.lang.foreign.MemorySegment;
                    
                    public interface %3$s<I> {
                        %4$s.ValueOp<MemorySegment> operator();
                    }
                    """.formatted(null, null,
                    packages.useClass(type),
                    packages.useClass(BasicOperations.Value) // 4
            ));
            return;
        }
        writer.write(packages, """
                public interface %3$s<I> extends %6$s<%4$s> {
                    static <I> %3$s<I> of(%5$s value) {
                        return new %3$s<>() {
                            @Override
                            public %6$s.ValueOp<%4$s> operator() {
                                return () -> value;
                            }
                
                            @Override
                            public String toString() {
                                return String.valueOf(value);
                            }
                        };
                    }
                
                    static <I> %3$s<I> of(%3$s<?> value) {
                        return of(value.operator().value());
                    }
                }
                """.formatted(null, null,
                packages.useClass(type),
                type.getPrimitive().getBoxedTypeName(),
                type.getPrimitive().useType(packages), // 5
                packages.useClass(BasicOperations.Value)
        ));
    }

    public static final String PTR_MAKE_OPERATION_METHOD = "makeOperations";

    private void genBindTypes(PackageManager packages, BindTypes bindTypes) {
        if (bindTypes != BindTypes.Ptr) {
            genValueBasedTypes(packages, bindTypes, bindTypes.typeName(NameType.RAW), writer);
            return;
        }
        packages.useClass(CommonTypes.FFMTypes.SEGMENT_ALLOCATOR);
        packages.useClass(CommonTypes.FFMTypes.MEMORY_SEGMENT);
        packages.useClass(CommonTypes.FFMTypes.VALUE_LAYOUT);
        var str = """
                import java.util.Objects;
                import java.util.function.Consumer;
                
                public class %3$s<E> implements %5$s<%3$s<E>, E>, %8$s<%3$s<E>> {
                    public static <I> %8$s.Operations<%3$s<I>> makeOperations(%8$s.Operations<I> operation) {
                        return new %8$s.Operations<>(
                                (param, offset) -> new %3$s<>(%10$s.getAddr(param, offset), operation),
                                (source, dest, offset) -> %10$s.setAddr(dest, offset, source.segment), ValueLayout.ADDRESS);
                    }
                
                    private final MemorySegment segment;
                    private final %8$s.Operations<E> operation;
                
                    private MemorySegment fitByteSize(MemorySegment segment) {
                        return segment.byteSize() == operation.memoryLayout().byteSize() ? segment : segment.reinterpret(operation.memoryLayout().byteSize());
                    }
                
                    public %3$s(MemorySegment segment, %8$s.Operations<E> operation) {
                        this.operation = operation;
                        this.segment = fitByteSize(segment);
                    }
                
                    public %3$s(%6$s<?, E> arr) {
                        this.operation = arr.operator().elementOperation();
                        this.segment = fitByteSize(arr.operator().value());
                    }
                
                    public %3$s(%4$s<E> ptr, %8$s.Operations<E> operation) {
                        this.operation = operation;
                        this.segment = fitByteSize(ptr.operator().value());
                    }
                
                    public %3$s(SegmentAllocator allocator, %8$s.Operations<E> operations, E element) {
                        this(allocator, operations);
                        operations.copy().copyTo(element, segment, 0);
                    }
                
                    public %3$s(SegmentAllocator allocator, %8$s.Operations<E> operations) {
                        this.operation = operations;
                        this.segment = allocator.allocate(operations.memoryLayout());
                    }
                
                    public %3$s(SegmentAllocator allocator, %8$s<E> info) {
                        this(allocator, info.operator().getOperations(), info.operator().self());
                    }
                
                    @Override
                    public String toString() {
                        return "%3$s{" +
                                "segment=" + segment +
                                '}';
                    }
                
                    public %8$s.Operations<E> getElementOperation() {
                        return operation;
                    }
                
                    public MemorySegment value() {
                        return segment;
                    }
                
                    public E pte() {
                        return operator().pointee();
                    }
                
                    public %3$s<E> pte(E element) {
                        operator().setPointee(element);
                        return this;
                    }
                
                    public %3$s<E> apply(Consumer<E> element) {
                        element.accept(pte());
                        return this;
                    }
                
                    public %3$s<E> self(Consumer<%3$s<E>> element) {
                        element.accept(this);
                        return this;
                    }
                
                    @Override
                    public %7$s<%3$s<E>, E> operator() {
                        return new %7$s<>() {
                            @Override
                            public %8$s.Operations<E> elementOperation() {
                                return operation;
                            }
                
                            @Override
                            public void setPointee(E pointee) {
                                operation.copy().copyTo(pointee, segment, 0);
                            }
                
                            @Override
                            public E pointee() {
                                return operation.constructor().create(segment, 0);
                            }
                
                            @Override
                            public %8$s.Operations<%3$s<E>> getOperations() {
                                return makeOperations(operation);
                            }
                
                            @Override
                            public %9$s<E> self() {
                                return %9$s.this;
                            }
                
                            @Override
                            public MemorySegment value() {
                                return segment;
                            }
                        };
                    }
                
                    @Override
                    public boolean equals(Object o) {
                        if (!(o instanceof %9$s<?> ptr)) return false;
                        return Objects.equals(segment, ptr.segment);
                    }
                
                    @Override
                    public int hashCode() {
                        return Objects.hashCode(segment);
                    }
                }
                """.formatted(null, null,
                packages.useClass(bindTypes),
                packages.useClass(bindTypes.getOperations().getValue()), // 4
                packages.useClass(bindTypes.getOperations()),
                packages.useClass(SpecificTypes.ArrayOp), // 6
                bindTypes.getOperations().operatorTypeName(),
                packages.useClass(BasicOperations.Info), // 8
                packages.useClass(BindTypes.Ptr), // 9
                packages.useClass(SpecificTypes.MemoryUtils) // 10
        );
        writer.write(packages, str);
    }

    static void genValueBasedTypes(PackageManager packages, BindTypes bindTypes, String typeName, Generators.Writer writer) {
        Assert(bindTypes != BindTypes.Ptr);
        if (bindTypes.getOperations().getValue().getPrimitive().noJavaPrimitive()) {
            packages.useClass(CommonTypes.FFMTypes.SEGMENT_ALLOCATOR);
            packages.addImport(bindTypes.getOperation().getCommonOperation().makeDirectMemoryLayout(packages).getTypeImports());
            Assert(bindTypes.getOperations().getValue().getPrimitive().byteSize() == 16, " sizeof %s must be 16".formatted(bindTypes));
            var str = """
                    import java.lang.foreign.MemorySegment;
                    import java.lang.foreign.ValueLayout;
                    import java.nio.ByteOrder;
                    import java.util.Arrays;
                    
                    public class %3$s implements %4$s<%3$s>, %8$s<%3$s> {
                        public static final %8$s.Operations<%3$s> OPERATIONS = new %8$s.Operations<>((param, offset) -> new %3$s(param.asSlice(offset)),
                                (source, dest, offset) -> %12$s.memcpy(source.val, 0, dest, offset, %5$s.byteSize()), %5$s);
                        private final MemorySegment val;
                    
                        public %3$s(MemorySegment val) {
                            this.val = val;
                        }
                    
                        public %3$s(%6$s<?> val) {
                            this.val = val.operator().value();
                        }
                    
                        public %3$s(long low, long high) {
                            this.val = MemorySegment.ofArray(new long[2]);
                            val.asByteBuffer().order(ByteOrder.nativeOrder()).putLong(low).putLong(high);
                        }
                    
                        public static %9$s<%3$s> array(SegmentAllocator allocator, %7$s<?> len) {
                            return array(allocator, len.operator().value());
                        }
                    
                        public static %9$s<%3$s> array(SegmentAllocator allocator, long len) {
                            return new %9$s<>(allocator, OPERATIONS, len);
                        }
                    
                        public static %10$s<%3$s> ptr(SegmentAllocator allocator) {
                            return new %10$s<>(allocator, OPERATIONS);
                        }
                    
                        @Override
                        public %11$s<%3$s> operator() {
                            return new %11$s<>() {
                                @Override
                                public %8$s.Operations<%3$s> getOperations() {
                                    return OPERATIONS;
                                }
                    
                                @Override
                                public %3$s self() {
                                    return %3$s.this;
                                }
                    
                                @Override
                                public MemorySegment value() {
                                    return val;
                                }
                            };
                        }
                    
                        @Override
                        public String toString() {
                            return String.valueOf(val);
                        }
                    
                        @Override
                        public boolean equals(Object o) {
                            if (!(o instanceof %3$s i)) return false;
                            return Arrays.equals(val.toArray(ValueLayout.JAVA_LONG), i.val.toArray(ValueLayout.JAVA_LONG));
                        }
                    
                        @Override
                        public int hashCode() {
                            return Arrays.hashCode(val.toArray(ValueLayout.JAVA_LONG));
                        }
                    }
                    """.formatted(null, null, typeName, // 3
                    packages.useClass(bindTypes.getOperations()),
                    bindTypes.getOperation().getCommonOperation().makeDirectMemoryLayout(packages).getMemoryLayout(packages), //5
                    packages.useClass(bindTypes.getOperations().getValue()), //6
                    packages.useClass(ValueInterface.I64I), // 7
                    packages.useClass(BasicOperations.Info), // 8
                    packages.useClass(SpecificTypes.Array), // 9
                    packages.useClass(CommonTypes.BindTypes.Ptr), // 10
                    bindTypes.getOperations().operatorTypeName(), // 11
                    packages.useClass(SpecificTypes.MemoryUtils) // 12
            );
            writer.write(packages, str);
            return;
        }
        packages.useClass(CommonTypes.FFMTypes.SEGMENT_ALLOCATOR);
        var str = """
                import java.util.Objects;
                
                public class %3$s implements %5$s<%3$s>, %10$s<%3$s> {
                    public static final %10$s.Operations<%3$s> OPERATIONS = %5$s.makeOperations(%3$s::new);;
                    private final %6$s val;
                
                    public %3$s(%6$s val) {
                        this.val = val;
                    }
                
                    public %3$s(%8$s<?> val) {
                        this.val = val.operator().value();
                    }
                
                    public static %11$s<%3$s> array(SegmentAllocator allocator, %9$s<?> len) {
                        return array(allocator, len.operator().value());
                    }
                
                    public static %11$s<%3$s> array(SegmentAllocator allocator, long len) {
                        return new %11$s<>(allocator, OPERATIONS, len);
                    }
                
                    public static %12$s<%3$s> ptr(SegmentAllocator allocator) {
                        return new %12$s<>(allocator, OPERATIONS);
                    }
                
                    @Override
                    public %13$s<%3$s> operator() {
                        return new %13$s<>() {
                            @Override
                            public %10$s.Operations<%3$s> getOperations() {
                                return OPERATIONS;
                            }
                
                            @Override
                            public %3$s self() {
                                return %3$s.this;
                            }
                
                            @Override
                            public %7$s value() {
                                return val;
                            }
                        };
                    }
                
                    public %6$s value() {
                        return val;
                    }
                
                    @Override
                    public String toString() {
                        return String.valueOf(val);
                    }
                
                    @Override
                    public boolean equals(Object o) {
                        if (!(o instanceof %3$s b)) return false;
                        return val == b.val;
                    }
                
                    @Override
                    public int hashCode() {
                        return Objects.hashCode(val);
                    }
                }
                """.formatted(null, null, typeName,
                bindTypes.getPrimitiveType().getMemoryLayout(packages).getMemoryLayout(packages), // 4
                packages.useClass(bindTypes.getOperations()), // 5
                bindTypes.getPrimitiveType().useType(packages),
                bindTypes.getPrimitiveType().getBoxedTypeName(),// 7
                packages.useClass(bindTypes.getOperations().getValue()), // 8
                packages.useClass(ValueInterface.I64I), // 9
                packages.useClass(BasicOperations.Info), // 10
                packages.useClass(SpecificTypes.Array), // 11
                packages.useClass(CommonTypes.BindTypes.Ptr), // 12
                bindTypes.getOperations().operatorTypeName() // 13
        );
        writer.write(packages, str);
    }

    private void genFunctionUtils(PackageManager packages) {
        writer.write(packages, """
                import java.lang.foreign.Arena;
                import java.lang.foreign.FunctionDescriptor;
                import java.lang.foreign.Linker;
                import java.lang.foreign.MemorySegment;
                import java.lang.invoke.MethodHandle;
                import java.lang.reflect.Modifier;
                import java.util.Optional;
                
                public class %s {
                    public static class SymbolNotFound extends RuntimeException {
                        public SymbolNotFound(String cause) {
                            super(cause);
                        }
                
                        public SymbolNotFound(Throwable t) {
                            super(t);
                        }
                
                        public SymbolNotFound() {
                        }
                    }
                
                    public static class InvokeException extends RuntimeException {
                        public InvokeException(Throwable cause) {
                            super(cause);
                        }
                    }
                
                    public static <E> String enumToString(Class<?> klass, E e) {
                        for (var field : klass.getFields()) {
                            try {
                                if (Modifier.isStatic(field.getModifiers()) && e.equals(field.get(null))) {
                                    return field.getName();
                                }
                            } catch (IllegalAccessException _) {
                            }
                        }
                        return null;
                    }
                
                    public sealed interface Symbol {
                        MemorySegment getSymbol();
                    }
                
                    public record FunctionSymbol(MemorySegment ms, boolean critical) implements Symbol {
                        public FunctionSymbol(MemorySegment ms) {
                            this(ms, false);
                        }
                
                        @Override
                        public MemorySegment getSymbol() {
                            return ms;
                        }
                    }
                
                    public record VariableSymbol(MemorySegment ms) implements Symbol {
                        @Override
                        public MemorySegment getSymbol() {
                            return ms;
                        }
                    }
                
                
                    public interface SymbolProvider {
                        Optional<Symbol> provide(String name);
                    }
                
                    public static MemorySegment upcallStub(Arena arena, MethodHandle methodHandle, FunctionDescriptor functionDescriptor) {
                        return Linker.nativeLinker().upcallStub(methodHandle, functionDescriptor, arena);
                    }
                
                    public static MethodHandle downcallHandle(MemorySegment ms, FunctionDescriptor fd, boolean critical) {
                        return critical ?
                                Linker.nativeLinker().downcallHandle(ms, fd, Linker.Option.critical(true))
                                : Linker.nativeLinker().downcallHandle(ms, fd);
                    }
                }
                """.formatted(packages.useClass(SpecificTypes.FunctionUtils)));
    }

    private void genMemoryUtils(PackageManager packages) {
        packages.useClass(CommonTypes.FFMTypes.MEMORY_SEGMENT);
        packages.useClass(CommonTypes.FFMTypes.VALUE_LAYOUT);
        packages.useClass(CommonTypes.FFMTypes.SEGMENT_ALLOCATOR);
        packages.useClass(CommonTypes.FFMTypes.ARENA);
        writer.write(packages, """
                                       import java.util.Objects;
                                       
                                       public final class %1$s {
                                       """.formatted(packages.getClassName()) + """
                                           public interface MemorySupport {
                                               void setByte(MemorySegment ms, long offset, byte val);
                                       
                                               void setShort(MemorySegment ms, long offset, short val);
                                       
                                               void setInt(MemorySegment ms, long offset, int val);
                                       
                                               void setLong(MemorySegment ms, long offset, long val);
                                       
                                               void setAddr(MemorySegment ms, long offset, MemorySegment val);
                                       
                                               void setFloat(MemorySegment ms, long offset, float val);
                                       
                                               void setDouble(MemorySegment ms, long offset, double val);
                                       
                                               byte getByte(MemorySegment ms, long offset);
                                       
                                               short getShort(MemorySegment ms, long offset);
                                       
                                               int getInt(MemorySegment ms, long offset);
                                       
                                               long getLong(MemorySegment ms, long offset);
                                       
                                               MemorySegment getAddr(MemorySegment ms, long offset);
                                       
                                               float getFloat(MemorySegment ms, long offset);
                                       
                                               double getDouble(MemorySegment ms, long offset);
                                       
                                               void memcpy(MemorySegment src, long srcOffset, MemorySegment dest, long destOffset, long byteSize);
                                           }
                                       
                                           public static MemorySupport memorySupport = new MemorySupport() {
                                               @Override
                                               public void setByte(MemorySegment ms, long offset, byte val) {
                                                   ms.set(ValueLayout.JAVA_BYTE, offset, val);
                                               }
                                       
                                               @Override
                                               public void setShort(MemorySegment ms, long offset, short val) {
                                                   ms.set(ValueLayout.JAVA_SHORT, offset, val);
                                               }
                                       
                                               @Override
                                               public void setInt(MemorySegment ms, long offset, int val) {
                                                   ms.set(ValueLayout.JAVA_INT, offset, val);
                                               }
                                       
                                               @Override
                                               public void setLong(MemorySegment ms, long offset, long val) {
                                                   ms.set(ValueLayout.JAVA_LONG, offset, val);
                                               }
                                       
                                               @Override
                                               public void setAddr(MemorySegment ms, long offset, MemorySegment val) {
                                                   ms.set(ValueLayout.ADDRESS, offset, val);
                                               }
                                       
                                               @Override
                                               public void setFloat(MemorySegment ms, long offset, float val) {
                                                   ms.set(ValueLayout.JAVA_FLOAT, offset, val);
                                               }
                                       
                                               @Override
                                               public void setDouble(MemorySegment ms, long offset, double val) {
                                                   ms.set(ValueLayout.JAVA_DOUBLE, offset, val);
                                               }
                                       
                                               @Override
                                               public byte getByte(MemorySegment ms, long offset) {
                                                   return ms.get(ValueLayout.JAVA_BYTE, offset);
                                               }
                                       
                                               @Override
                                               public short getShort(MemorySegment ms, long offset) {
                                                   return ms.get(ValueLayout.JAVA_SHORT, offset);
                                               }
                                       
                                               @Override
                                               public int getInt(MemorySegment ms, long offset) {
                                                   return ms.get(ValueLayout.JAVA_INT, offset);
                                               }
                                       
                                               @Override
                                               public long getLong(MemorySegment ms, long offset) {
                                                   return ms.get(ValueLayout.JAVA_LONG, offset);
                                               }
                                       
                                               @Override
                                               public MemorySegment getAddr(MemorySegment ms, long offset) {
                                                   return ms.get(ValueLayout.ADDRESS, offset);
                                               }
                                       
                                               @Override
                                               public float getFloat(MemorySegment ms, long offset) {
                                                   return ms.get(ValueLayout.JAVA_FLOAT, offset);
                                               }
                                       
                                               @Override
                                               public double getDouble(MemorySegment ms, long offset) {
                                                   return ms.get(ValueLayout.JAVA_DOUBLE, offset);
                                               }
                                       
                                               @Override
                                               public void memcpy(MemorySegment src, long srcOffset, MemorySegment dest, long destOffset, long byteSize) {
                                                   MemorySegment.copy(src, srcOffset, dest, destOffset, byteSize);
                                               }
                                           };
                                       
                                           private static final class MemorySupportHolder {
                                               private static final MemorySupport MEMORY_SUPPORT = Objects.requireNonNull(memorySupport);
                                           }
                                       
                                           public static void setByte(MemorySegment ms, long offset, byte val) {
                                               MemorySupportHolder.MEMORY_SUPPORT.setByte(ms, offset, val);
                                           }
                                       
                                           public static void setShort(MemorySegment ms, long offset, short val) {
                                               MemorySupportHolder.MEMORY_SUPPORT.setShort(ms, offset, val);
                                           }
                                       
                                           public static void setInt(MemorySegment ms, long offset, int val) {
                                               MemorySupportHolder.MEMORY_SUPPORT.setInt(ms, offset, val);
                                           }
                                       
                                           public static void setLong(MemorySegment ms, long offset, long val) {
                                               MemorySupportHolder.MEMORY_SUPPORT.setLong(ms, offset, val);
                                           }
                                       
                                           public static void setAddr(MemorySegment ms, long offset, MemorySegment val) {
                                               MemorySupportHolder.MEMORY_SUPPORT.setAddr(ms, offset, val);
                                           }
                                       
                                           public static void setFloat(MemorySegment ms, long offset, float val) {
                                               MemorySupportHolder.MEMORY_SUPPORT.setFloat(ms, offset, val);
                                           }
                                       
                                           public static void setDouble(MemorySegment ms, long offset, double val) {
                                               MemorySupportHolder.MEMORY_SUPPORT.setDouble(ms, offset, val);
                                           }
                                       
                                           public static byte getByte(MemorySegment ms, long offset) {
                                               return MemorySupportHolder.MEMORY_SUPPORT.getByte(ms, offset);
                                           }
                                       
                                           public static short getShort(MemorySegment ms, long offset) {
                                               return MemorySupportHolder.MEMORY_SUPPORT.getShort(ms, offset);
                                           }
                                       
                                           public static int getInt(MemorySegment ms, long offset) {
                                               return MemorySupportHolder.MEMORY_SUPPORT.getInt(ms, offset);
                                           }
                                       
                                           public static long getLong(MemorySegment ms, long offset) {
                                               return MemorySupportHolder.MEMORY_SUPPORT.getLong(ms, offset);
                                           }
                                       
                                           public static MemorySegment getAddr(MemorySegment ms, long offset) {
                                               return MemorySupportHolder.MEMORY_SUPPORT.getAddr(ms, offset);
                                           }
                                       
                                           public static float getFloat(MemorySegment ms, long offset) {
                                               return MemorySupportHolder.MEMORY_SUPPORT.getFloat(ms, offset);
                                           }
                                       
                                           public static double getDouble(MemorySegment ms, long offset) {
                                               return MemorySupportHolder.MEMORY_SUPPORT.getDouble(ms, offset);
                                           }
                                       
                                           public static void memcpy(MemorySegment src, long srcOffset, MemorySegment dest, long destOffset, long byteSize) {
                                               MemorySupportHolder.MEMORY_SUPPORT.memcpy(src, srcOffset, dest, destOffset, byteSize);
                                           }
                                       
                                           private static final class OnHeapAllocatorHolder {
                                               private static final SegmentAllocator ON_HEAP_ALLOCATOR = Objects.requireNonNull(onHeapAllocator);
                                           }
                                       
                                           public static SegmentAllocator onHeapAllocator() {
                                               return OnHeapAllocatorHolder.ON_HEAP_ALLOCATOR;
                                           }
                                       
                                           public static SegmentAllocator onHeapAllocator = new SegmentAllocator() {
                                               @Override
                                               public MemorySegment allocate(long byteSize, long byteAlignment) {
                                                   if (byteAlignment <= 0 || (byteAlignment & (byteAlignment - 1)) != 0) {
                                                       throw new IllegalArgumentException("Alignment must be a power of two: " + byteAlignment);
                                                   }
                                                   if (byteSize % byteAlignment != 0) {
                                                       throw new IllegalArgumentException("Size must be multiple of alignment: size=" + byteSize + ", alignment=" + byteAlignment);
                                                   }
                                                   try {
                                                       return switch ((int) byteAlignment) {
                                                           case 1 -> MemorySegment.ofArray(new byte[Math.toIntExact(byteSize)]);
                                                           case 2 -> MemorySegment.ofArray(new short[Math.toIntExact(byteSize / 2)]);
                                                           case 4 -> MemorySegment.ofArray(new int[Math.toIntExact(byteSize / 4)]);
                                                           case 8 -> MemorySegment.ofArray(new long[Math.toIntExact(byteSize / 8)]);
                                                           // fallback to auto arena
                                                           default -> Arena.ofAuto().allocate(byteSize, byteAlignment);
                                                       };
                                                   } catch (ArithmeticException e) {
                                                       throw new OutOfMemoryError("Requested memory size too large: " + byteSize + " bytes");
                                                   } catch (NegativeArraySizeException e) {
                                                       throw new OutOfMemoryError("Negative array size: " + byteSize);
                                                   }
                                               }
                                           };
                                       }
                                       """);
    }
}
