package generator.generation.generator;

import generator.Dependency;
import generator.PackagePath;
import generator.TypePkg;
import generator.Utils;
import generator.generation.Common;
import generator.types.CommonTypes;
import generator.types.CommonTypes.BasicOperations;
import generator.types.CommonTypes.BindTypes;
import generator.types.CommonTypes.SpecificTypes;
import generator.types.CommonTypes.ValueInterface;
import generator.types.TypeAttr.NameType;

import static utils.CommonUtils.Assert;


public class CommonGenerator implements Generator {
    private final Common common;
    private final Dependency dependency;

    public CommonGenerator(Common common, Dependency dependency) {
        this.common = common;
        this.dependency = dependency;
    }

    @Override
    public void generate() {
        for (TypePkg<? extends CommonTypes.BaseType> implType : common.getImplTypes()) {
            PackagePath packagePath = dependency.getTypePackagePath(implType.type());
            String imports = Generator.extractImports(common, dependency);
            switch (implType.type()) {
                case BindTypes bindTypes -> genBindTypes(packagePath, bindTypes, imports);
                case CommonTypes.BindTypeOperations btOp -> genBindTypeOp(packagePath, btOp, imports);
                case ValueInterface v -> genValueInterface(packagePath, v, imports);
                case SpecificTypes specificTypes -> {
                    switch (specificTypes) {
                        case Array -> genArray(packagePath, imports);
                        case FlatArray -> genFlatArray(packagePath, imports);
                        case Str -> genStr(packagePath, imports);
                        case FunctionUtils -> genFunctionUtils(packagePath);
                        case ArrayOp -> genArrayOp(packagePath, imports);
                        case FlatArrayOp -> genFlatArrayOp(packagePath, imports);
                        case StructOp -> genStructOp(packagePath, imports);
                        case MemoryUtils -> genMemoryUtils(packagePath, imports);
                    }
                }
                case CommonTypes.FFMTypes _ -> {
                }
                case BasicOperations ext -> {
                    switch (ext) {
                        case Operation -> genOperation(packagePath);
                        case Info -> genInfo(packagePath, imports);
                        case Value -> genValue(packagePath, imports);
                        case PteI -> genPointee(packagePath, imports);
                        case ArrayI -> genArrayI(packagePath, imports);
                        case StructI -> genStructI(packagePath, imports);
                    }
                }
            }
        }
    }

    private void genStructI(PackagePath path, String imports) {
        Utils.write(path, """
                %s
                
                %s
                
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
                """.formatted(path.makePackage(), imports,
                BasicOperations.Value.typeName(NameType.RAW), // 3
                BasicOperations.StructI.typeName(NameType.RAW) // 4
        ));
    }

    private void genArrayI(PackagePath path, String imports) {
        Utils.write(path, """
                %s
                
                %s
                
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
                """.formatted(path.makePackage(), imports,
                BasicOperations.Value.typeName(NameType.RAW), // 3
                BasicOperations.ArrayI.typeName(NameType.RAW) // 4
        ));
    }

    private void genBindTypeOp(PackagePath path, CommonTypes.BindTypeOperations btOp, String imports) {
        if (btOp.getValue().getPrimitive().noJavaPrimitive()) {
            var str = """
                    %1$s
                    
                    %2$s
                    import java.lang.foreign.MemorySegment;
                    
                    public interface %3$s<T> extends %5$s<T>, %4$s<T> {
                        @Override
                        %7$s<T> operator();
                    
                        interface %7$s<T> extends %5$s.InfoOp<T>, %6$s.ValueOp<MemorySegment> {
                    
                        }
                    }
                    """.formatted(path.makePackage(), imports,
                    btOp.typeName(NameType.RAW), // 3
                    btOp.getValue().typeName(NameType.RAW),
                    BasicOperations.Info.typeName(NameType.RAW), // 5
                    BasicOperations.Value.typeName(NameType.RAW), // 6
                    btOp.operatorTypeName() // 7
            );
            Utils.write(path, str);
            return;
        }
        String str = """
                %1$s
                
                %2$s
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
                """.formatted(path.makePackage(), imports,
                btOp.typeName(NameType.RAW), // 3
                btOp.getValue().typeName(NameType.RAW),
                btOp.getValue().getPrimitive().getBoxedTypeName(), // 5
                btOp.operatorTypeName(),
                btOp.getValue().getPrimitive().getMemoryLayout().getMemoryLayout(), // 7
                btOp.getValue().getPrimitive().getMemoryUtilName(),
                BasicOperations.Info.typeName(NameType.RAW), // 9
                BasicOperations.Value.typeName(NameType.RAW), // 10
                SpecificTypes.MemoryUtils.typeName(NameType.RAW) // 11
        );
        if (btOp == CommonTypes.BindTypeOperations.PtrOp)
            str = """
                    %1$s
                    
                    %2$s
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
                    """.formatted(path.makePackage(), imports,
                    btOp.typeName(NameType.RAW),
                    btOp.operatorTypeName(),
                    btOp.getValue().typeName(NameType.RAW),
                    BasicOperations.PteI.typeName(NameType.RAW),
                    BasicOperations.Info.typeName(NameType.RAW), // 7
                    BasicOperations.Value.typeName(NameType.RAW), // 8
                    SpecificTypes.MemoryUtils.typeName(NameType.RAW) // 9
            );
        Utils.write(path, str);
    }

    private void genOperation(PackagePath path) {
        Utils.write(path, """
                %s
                
                public interface %s {
                    Object operator();
                }
                """.formatted(path.makePackage(),
                BasicOperations.Operation.typeName(NameType.RAW)));
    }

    private void genValue(PackagePath path, String imports) {
        Utils.write(path, """
                %s
                
                %s
                
                public interface %4$s<T> extends %3$s {
                    interface ValueOp<T> {
                        T value();
                    }
                
                    @Override
                    ValueOp<T> operator();
                }
                """.formatted(path.makePackage(), imports,
                BasicOperations.Operation.typeName(NameType.RAW), //3
                BasicOperations.Value.typeName(NameType.RAW) // 4
        ));
    }

    private void genArrayOp(PackagePath path, String imports) {
        Utils.write(path, """
                %s
                
                %s
                import java.util.AbstractList;
                import java.util.List;
                import java.util.RandomAccess;
                
                public interface %s<A, E> extends %11$s<A>, %7$s<E>, %4$s<A, E>, List<E> {
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
                }""".formatted(path.makePackage(), imports,
                SpecificTypes.ArrayOp.typeName(NameType.RAW),
                CommonTypes.BindTypeOperations.PtrOp.typeName(NameType.RAW), // 4
                CommonTypes.BindTypeOperations.PtrOp.operatorTypeName(),
                BindTypes.Ptr.typeName(NameType.RAW),
                BasicOperations.ArrayI.typeName(NameType.RAW),// 7
                ValueInterface.I64I.typeName(NameType.RAW),
                BindTypes.I64.typeName(NameType.RAW), // 9
                ValueInterface.I32I.typeName(NameType.RAW), // 10
                BasicOperations.Info.typeName(NameType.RAW), // 11
                BasicOperations.Value.typeName(NameType.RAW) // 12
        ));
    }

    private void genFlatArrayOp(PackagePath path, String imports) {
        Utils.write(path, """
                %s
                
                %s
                import java.util.AbstractList;
                import java.util.List;
                import java.util.RandomAccess;
                
                public interface %s<A, E> extends %11$s<A>, %7$s<E>, List<E> {
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
                }""".formatted(path.makePackage(), imports,
                SpecificTypes.FlatArrayOp.typeName(NameType.RAW),
                CommonTypes.BindTypeOperations.PtrOp.typeName(NameType.RAW), // 4
                CommonTypes.BindTypeOperations.PtrOp.operatorTypeName(),
                BindTypes.Ptr.typeName(NameType.RAW),
                BasicOperations.ArrayI.typeName(NameType.RAW), // 7
                ValueInterface.I64I.typeName(NameType.RAW),
                BindTypes.I64.typeName(NameType.RAW), // 9
                ValueInterface.I32I.typeName(NameType.RAW), // 10
                BasicOperations.Info.typeName(NameType.RAW), // 11
                BasicOperations.Value.typeName(NameType.RAW) // 12
        ));
    }

    private void genStructOp(PackagePath path, String imports) {
        Utils.write(path, """
                %1$s
                
                %2$s
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
                }""".formatted(path.makePackage(), imports,
                BasicOperations.StructI.typeName(NameType.RAW), // 3
                BasicOperations.Info.typeName(NameType.RAW), // 4
                BasicOperations.Value.typeName(NameType.RAW), // 5
                SpecificTypes.StructOp.typeName(NameType.RAW) // 6
        ));
    }

    private void genPointee(PackagePath path, String imports) {
        Utils.write(path, """
                %s
                
                %s
                
                public interface %3$s<E> extends %4$s {
                    interface PointeeOp<E> extends %5$s.ValueOp<MemorySegment> {
                        E pointee();
                    }
                
                    @Override
                    PointeeOp<E> operator();
                }""".formatted(path.makePackage(), imports,
                BasicOperations.PteI.typeName(NameType.RAW), // 3
                BasicOperations.Operation.typeName(NameType.RAW),
                BasicOperations.Value.typeName(NameType.RAW) // 5
        ));
    }

    private void genInfo(PackagePath path, String imports) {
        Utils.write(path, """
                %1$s
                
                %2$s
                
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
                """.formatted(path.makePackage(), imports,
                BasicOperations.Info.typeName(NameType.RAW), // 3
                BasicOperations.Operation.typeName(NameType.RAW) // 4
        ));
    }

    public static final String ARRAY_MAKE_OPERATION_METHOD = "makeOperations";

    private void genSingle(PackagePath path, String imports) {
        Utils.write(path, """
                %1$s
                
                %2$s
                import java.util.List;
                import java.util.Objects;
                import java.util.function.Consumer;
                
                public class %12$s<E> extends %3$s.AbstractRandomAccessList<E> implements %3$s<%12$s<E>, E>, %10$s<%12$s<E>> {
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
                
                    public %12$s(SegmentAllocator allocator, %10$s.Operations<E> operations, E element) {
                        this.operations = operations;
                        this.ptr = allocator.allocate(operations.memoryLayout());
                        operations.copy().copyTo(element, ptr, 0);
                    }
                
                    public %12$s(SegmentAllocator allocator, %10$s<E> info) {
                        this(allocator, info.operator().getOperations(), info.operator().self());
                    }
                
                    public %12$s(SegmentAllocator allocator, %10$s.Operations<E> operations) {
                        this.operations = operations;
                        this.ptr = allocator.allocate(operations.memoryLayout());
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
                                return makeOperations(operations);
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
                
                    public %12$s<E> apply(Consumer<E> consumer) {
                        consumer.accept(getFirst());
                        return this;
                    }
                
                    public E get() {
                        return getFirst();
                    }
                }
                """.formatted(path.makePackage(), imports,
                SpecificTypes.ArrayOp.typeName(NameType.RAW),
                CommonTypes.BindTypeOperations.PtrOp.typeName(NameType.RAW),
                ValueInterface.PtrI.typeName(NameType.RAW), // 5
                BindTypes.Ptr.typeName(NameType.RAW),  // 6
                BindTypes.I64.typeName(NameType.RAW), // 7
                ValueInterface.I64I.typeName(NameType.RAW), // 8
                ValueInterface.I32I.typeName(NameType.RAW), // 9
                BasicOperations.Info.typeName(NameType.RAW), // 10
                SpecificTypes.MemoryUtils.typeName(NameType.RAW), // 11
                CommonTypes.BindTypes.Ptr.typeName(NameType.RAW) // 12
        ));
    }

    private void genArray(PackagePath path, String imports) {
        Utils.write(path, """
                %1$s
                
                %2$s
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
                """.formatted(path.makePackage(), imports,
                SpecificTypes.ArrayOp.typeName(NameType.RAW),
                CommonTypes.BindTypeOperations.PtrOp.typeName(NameType.RAW),
                ValueInterface.PtrI.typeName(NameType.RAW), // 5
                BindTypes.Ptr.typeName(NameType.RAW),  // 6
                BindTypes.I64.typeName(NameType.RAW), // 7
                ValueInterface.I64I.typeName(NameType.RAW), // 8
                ValueInterface.I32I.typeName(NameType.RAW), // 9
                BasicOperations.Info.typeName(NameType.RAW), // 10
                SpecificTypes.MemoryUtils.typeName(NameType.RAW), // 11
                SpecificTypes.Array.typeName(NameType.RAW) // 12
        ));
    }

    private void genFlatArray(PackagePath path, String imports) {
        Utils.write(path, """
                %1$s
                
                %2$s
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
                """.formatted(path.makePackage(), imports,
                SpecificTypes.FlatArrayOp.typeName(NameType.RAW),
                CommonTypes.BindTypeOperations.PtrOp.typeName(NameType.RAW),
                ValueInterface.PtrI.typeName(NameType.RAW), // 5
                BindTypes.Ptr.typeName(NameType.RAW),
                BindTypes.I64.typeName(NameType.RAW), // 7
                ValueInterface.I64I.typeName(NameType.RAW), // 8
                ValueInterface.I32I.typeName(NameType.RAW), // 9
                BasicOperations.Info.typeName(NameType.RAW), // 10
                SpecificTypes.MemoryUtils.typeName(NameType.RAW), // 11
                SpecificTypes.FlatArray.typeName(NameType.RAW) // 12
        ));
    }

    private static void genStr(PackagePath packagePath, String imports) {
        Utils.write(packagePath, """
                %1$s
                
                %2$s
                import java.lang.foreign.MemorySegment;
                import java.lang.foreign.SegmentAllocator;
                import java.lang.foreign.ValueLayout;
                import java.nio.charset.StandardCharsets;
                import java.util.Arrays;
                import java.util.Collection;
                import java.util.List;
                import java.util.stream.Stream;
                
                public class %5$s extends %3$s.AbstractRandomAccessList<%11$s> implements %3$s<%5$s, %11$s>, %10$s<%5$s> {
                    public static final %10$s.Operations<%5$s> OPERATIONS = new %10$s.Operations<>(
                            (param, offset) -> new %5$s(fitByteSize(param.get(ValueLayout.ADDRESS, offset))),
                            (source, dest, offset) -> dest.set(ValueLayout.ADDRESS, offset, source.ptr), ValueLayout.ADDRESS);
                    private final MemorySegment ptr;
                
                    @Override
                    public %11$s get(int index) {
                        return new %11$s(ptr.getAtIndex(ValueLayout.JAVA_BYTE, index));
                    }
                
                    private static %12$s<%5$s> makeArray(SegmentAllocator allocator, Stream<String> ss) {
                        List<%5$s> list = ss.map(s -> new %5$s(allocator, s)).toList();
                        return new %12$s<>(allocator, list.getFirst().operator().getOperations(), list);
                    }
                
                    private static final long HIMAGIC_FOR_BYTES = 0x8080_8080_8080_8080L;
                    private static final long LOMAGIC_FOR_BYTES = 0x0101_0101_0101_0101L;
                
                    private static boolean containZeroByte(long l) {
                        return ((l - LOMAGIC_FOR_BYTES) & (~l) & HIMAGIC_FOR_BYTES) != 0;
                    }
                
                    private static int strlen(MemorySegment segment) {
                        int count = 0;
                        while (!containZeroByte(segment.get(ValueLayout.JAVA_LONG_UNALIGNED, count))) {
                            count += 4;
                        }
                        while (segment.get(ValueLayout.JAVA_BYTE, count) != 0) {
                            segment.get(ValueLayout.JAVA_BYTE, count);
                            count++;
                        }
                        return count;
                    }
                
                    private static MemorySegment fitByteSize(MemorySegment segment) {
                        return !segment.isNative() || segment.address() == 0 ? segment : segment.reinterpret(1 + strlen(segment.reinterpret(Long.MAX_VALUE)));
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
                
                    protected %5$s(MemorySegment ptr) {
                        this.ptr = ptr;
                    }
                
                    public %5$s(%6$s<? extends %7$s<?>> ptr) {
                        this(fitByteSize(ptr.operator().value()));
                    }
                
                    public %5$s(SegmentAllocator allocator, String s) {
                        this(allocator.allocateFrom(s, StandardCharsets.UTF_8));
                    }
                
                    public String get() {
                        return MemorySegment.NULL.address() == ptr.address() ? null : toString();
                    }
                
                    @Override
                    public int size() {
                        return (int) ptr.byteSize();
                    }
                
                    @Override
                    public String toString() {
                        return MemorySegment.NULL.address() == ptr.address()
                                ? "%5$s{ptr=" + ptr + '}'
                                : ptr.getString(0, StandardCharsets.UTF_8);
                    }
                
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
                                return new %9$s(ptr.byteSize());
                            }
                        };
                    }
                }
                """.formatted(packagePath.makePackage(), imports,
                SpecificTypes.ArrayOp.typeName(NameType.RAW),// 3
                BindTypes.Ptr.typeName(NameType.RAW),
                SpecificTypes.Str.typeName(NameType.RAW),// 5
                ValueInterface.PtrI.typeName(NameType.RAW),
                ValueInterface.I8I.typeName(NameType.RAW),
                ValueInterface.I64I.typeName(NameType.RAW),//8
                BindTypes.I64.typeName(NameType.RAW),
                BasicOperations.Info.typeName(NameType.RAW), // 10
                BindTypes.I8.typeName(NameType.RAW), // 11
                SpecificTypes.Array.typeName(NameType.RAW), // 12
                CommonTypes.BindTypes.Ptr.typeName(NameType.RAW) // 13
        ));
    }

    private void genValueInterface(PackagePath path, ValueInterface type, String imports) {
        if (type.getPrimitive().noJavaPrimitive()) {
            Utils.write(path, """
                    %1$s
                    %2$s
                    import java.lang.foreign.MemorySegment;
                    
                    public interface %3$s<I> {
                        %4$s.ValueOp<MemorySegment> operator();
                    }
                    """.formatted(path.makePackage(), imports,
                    type.typeName(NameType.RAW),
                    BasicOperations.Value.typeName(NameType.RAW) // 4
            ));
            return;
        }
        Utils.write(path, """
                %1$s
                %2$s
                
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
                """.formatted(path.makePackage(), imports, type.typeName(NameType.RAW),
                type.getPrimitive().getBoxedTypeName(), type.getPrimitive().getPrimitiveTypeName(), // 5
                BasicOperations.Value.typeName(NameType.RAW)
        ));
    }

    public static final String PTR_MAKE_OPERATION_METHOD = "makeOperations";

    private void genBindTypes(PackagePath path, BindTypes bindTypes, String imports) {
        if (bindTypes != BindTypes.Ptr) {
            genValueBasedTypes(path, bindTypes, imports, bindTypes.typeName(NameType.RAW));
            return;
        }
        var str = """
                %1$s
                
                %2$s
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
                
                    public E pointee() {
                        return operator().pointee();
                    }

                    public E get() {
                        return operator().pointee();
                    }
                
                    public %3$s<E> pointee(E element) {
                        operator().setPointee(element);
                        return this;
                    }
                
                    public %3$s<E> apply(Consumer<E> element) {
                        element.accept(pointee());
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
                """.formatted(path.makePackage(), imports,
                bindTypes.typeName(NameType.RAW),
                bindTypes.getOperations().getValue().typeName(NameType.RAW), // 4
                bindTypes.getOperations().typeName(NameType.RAW),
                SpecificTypes.ArrayOp.typeName(NameType.RAW), // 6
                bindTypes.getOperations().operatorTypeName(),
                BasicOperations.Info.typeName(NameType.RAW), // 8
                BindTypes.Ptr.typeName(NameType.RAW), // 9
                SpecificTypes.MemoryUtils.typeName(NameType.RAW) // 10
        );
        Utils.write(path, str);
    }

    static void genValueBasedTypes(PackagePath path, BindTypes bindTypes, String imports, String typeName) {
        Assert(bindTypes != BindTypes.Ptr);
        if (bindTypes.getOperations().getValue().getPrimitive().noJavaPrimitive()) {
            Assert(bindTypes.getOperations().getValue().getPrimitive().byteSize() == 16, " sizeof %s must be 16".formatted(bindTypes));
            var str = """
                    %1$s
                    
                    %2$s
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
                    """.formatted(path.makePackage(), imports, typeName, // 3
                    bindTypes.getOperations().typeName(NameType.RAW),
                    bindTypes.getOperation().getCommonOperation().makeDirectMemoryLayout().getMemoryLayout(), //5
                    bindTypes.getOperations().getValue().typeName(NameType.RAW), //6
                    ValueInterface.I64I.typeName(NameType.RAW), // 7
                    BasicOperations.Info.typeName(NameType.RAW), // 8
                    SpecificTypes.Array.typeName(NameType.RAW), // 9
                    CommonTypes.BindTypes.Ptr.typeName(NameType.RAW), // 10
                    bindTypes.getOperations().operatorTypeName(), // 11
                    SpecificTypes.MemoryUtils.typeName(NameType.RAW) // 12
            );
            Utils.write(path, str);
            return;
        }
        var str = """
                %1$s
                
                %2$s
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
                """.formatted(path.makePackage(), imports, typeName,
                bindTypes.getPrimitiveType().getMemoryLayout().getMemoryLayout(), // 4
                bindTypes.getOperations().typeName(NameType.RAW), // 5
                bindTypes.getPrimitiveType().getPrimitiveTypeName(),
                bindTypes.getPrimitiveType().getBoxedTypeName(),// 7
                bindTypes.getOperations().getValue().typeName(NameType.RAW), // 8
                ValueInterface.I64I.typeName(NameType.RAW), // 9
                BasicOperations.Info.typeName(NameType.RAW), // 10
                SpecificTypes.Array.typeName(NameType.RAW), // 11
                CommonTypes.BindTypes.Ptr.typeName(NameType.RAW), // 12
                bindTypes.getOperations().operatorTypeName() // 13
        );
        Utils.write(path, str);
    }

    private void genFunctionUtils(PackagePath path) {
        Utils.write(path, """
                %s
                
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
                """.formatted(path.makePackage(), SpecificTypes.FunctionUtils.typeName(NameType.RAW)));
    }

    private void genMemoryUtils(PackagePath path, String imports) {
        Utils.write(path, """
                                  %1$s
                                  
                                  %3$s
                                  import java.util.Objects;
                                  
                                  public final class %2$s {
                                  """.formatted(path.makePackage(), path.getClassName(), imports) + """
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
