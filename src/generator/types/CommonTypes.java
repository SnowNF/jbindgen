package generator.types;

import generator.Generators;
import generator.PackageManager;
import generator.PackagePath;
import generator.types.operations.DestructOnlyOp;
import generator.types.operations.NoJavaPrimitiveType;
import generator.types.operations.OperationAttr;
import generator.types.operations.ValueBased;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.ByteOrder;
import java.util.*;
import java.util.function.Supplier;

public class CommonTypes {
    public enum Primitives implements TypeAttr.SizedType {
        JAVA_BOOLEAN(MemoryLayouts.JAVA_BOOLEAN, "boolean", "Boolean", null, AddressLayout.JAVA_BOOLEAN, false, "Byte", false),
        JAVA_BYTE(MemoryLayouts.JAVA_BYTE, "byte", "Byte", null, AddressLayout.JAVA_BYTE, false, "Byte", true),
        JAVA_SHORT(MemoryLayouts.JAVA_SHORT, "short", "Short", null, AddressLayout.JAVA_SHORT, false, "Short", true),
        JAVA_CHAR(MemoryLayouts.JAVA_CHAR, "char", "Character", null, AddressLayout.JAVA_CHAR, false, "Char", false),
        JAVA_INT(MemoryLayouts.JAVA_INT, "int", "Integer", null, AddressLayout.JAVA_INT, false, "Int", true),
        JAVA_LONG(MemoryLayouts.JAVA_LONG, "long", "Long", null, AddressLayout.JAVA_LONG, false, "Long", true),
        JAVA_FLOAT(MemoryLayouts.JAVA_FLOAT, "float", "Float", null, AddressLayout.JAVA_FLOAT, false, "Float", false),
        JAVA_DOUBLE(MemoryLayouts.JAVA_DOUBLE, "double", "Double", null, AddressLayout.JAVA_DOUBLE, false, "Double", false),
        ADDRESS(MemoryLayouts.ADDRESS, null, "MemorySegment", FFMTypes.MEMORY_SEGMENT, AddressLayout.ADDRESS, false, "Addr", false),
        FLOAT16(MemoryLayouts.JAVA_SHORT, "short", "Short", null, AddressLayout.JAVA_SHORT, false, "Short", false),
        LONG_DOUBLE(MemoryLayouts.structLayout(List.of(MemoryLayouts.JAVA_LONG, MemoryLayouts.JAVA_LONG)), null, null, null, MemoryLayout.structLayout(AddressLayout.JAVA_LONG, AddressLayout.JAVA_LONG), true, null, false),
        Integer128(MemoryLayouts.structLayout(List.of(MemoryLayouts.JAVA_LONG, MemoryLayouts.JAVA_LONG)), null, null, null, MemoryLayout.structLayout(AddressLayout.JAVA_LONG, AddressLayout.JAVA_LONG), true, null, false);

        private final MemoryLayouts memoryLayout;
        private final String primitiveTypeName;
        private final String boxedTypeName;
        private final FFMTypes ffmType;
        private final long byteSize;
        private final long alignment;
        private final boolean noJavaPrimitive;
        private final String memoryUtilName;
        private final boolean nativeIntegral;

        Primitives(MemoryLayouts memoryLayouts, String primitiveTypeName, String boxedTypeName, FFMTypes ffmType, MemoryLayout memoryLayout, boolean noJavaPrimitive, String memoryUtilName, boolean nativeIntegral) {
            this.memoryLayout = memoryLayouts;
            this.primitiveTypeName = primitiveTypeName;
            this.boxedTypeName = boxedTypeName;
            this.ffmType = ffmType;
            this.byteSize = memoryLayout.byteSize();
            this.alignment = memoryLayout.byteAlignment();
            this.noJavaPrimitive = noJavaPrimitive;
            this.memoryUtilName = memoryUtilName;
            this.nativeIntegral = nativeIntegral;
        }

        public MemoryLayouts getMemoryLayout(PackageManager packages) {
            return memoryLayout;
        }

        public String getPrimitiveTypeName() {
            return Objects.requireNonNull(primitiveTypeName);
        }

        public String getBoxedTypeName() {
            return Objects.requireNonNull(boxedTypeName);
        }

        public String useType(PackageManager packages) {
            if (importNeedType().isPresent()) {
                return packages.useClass(importNeedType().get());
            }
            return primitiveTypeName;
        }

        public Optional<FFMTypes> importNeedType() {
            return Optional.ofNullable(ffmType);
        }

        public boolean noJavaPrimitive() {
            return noJavaPrimitive;
        }

        public String getMemoryUtilName() {
            return Objects.requireNonNull(memoryUtilName);
        }

        public long alignment() {
            return alignment;
        }

        public boolean nativeIntegral() {
            return nativeIntegral;
        }

        @Override
        public long byteSize() {
            return byteSize;
        }
    }

    public enum BasicOperations implements BaseType, TypeAttr.OperationType {
        Operation(false),
        Info(Set.of(Operation, FFMTypes.MEMORY_SEGMENT, FFMTypes.MEMORY_LAYOUT), false),
        Value(Set.of(Operation), false),
        PteI(Set.of(Value, Operation, FFMTypes.MEMORY_SEGMENT), false),//pointee
        ArrayI(Set.of(Value, FFMTypes.MEMORY_SEGMENT), true),
        StructI(Set.of(Value, FFMTypes.MEMORY_SEGMENT), true),
        ;
        private final Set<TypeAttr.TypeRefer> imports;
        private final boolean destruct;

        BasicOperations(Set<TypeAttr.TypeRefer> imports, boolean destruct) {
            this.imports = imports;
            this.destruct = destruct;
        }

        BasicOperations(boolean destruct) {
            this.destruct = destruct;
            this.imports = Set.of();
        }

        @Override
        public TypeImports getUseImportTypes() {
            return new TypeImports(this);
        }

        @Override
        public String typeName() {
            if (Generators.DEBUG)
                return name() + Generators.DEBUG_NAME_APPEND;
            return name();
        }

        @Override
        public OperationAttr.Operation getOperation() {
            if (!destruct) {
                throw new UnsupportedOperationException();
            }
            return new DestructOnlyOp<>(this, Primitives.ADDRESS);
        }
    }

    public enum ValueInterface implements BaseType, TypeAttr.OperationType {
        I8I(Primitives.JAVA_BYTE),
        I16I(Primitives.JAVA_SHORT),
        I32I(Primitives.JAVA_INT),
        I64I(Primitives.JAVA_LONG),
        FP32I(Primitives.JAVA_FLOAT),
        FP64I(Primitives.JAVA_DOUBLE),
        PtrI(Set.of(BasicOperations.Value, FFMTypes.MEMORY_SEGMENT), Primitives.ADDRESS),
        FP16I(Primitives.FLOAT16),
        FP128I(Primitives.LONG_DOUBLE),
        I128I(Primitives.Integer128);

        private final Set<TypeAttr.TypeRefer> imports;
        private final Primitives primitive;

        ValueInterface(Set<TypeAttr.TypeRefer> imports, Primitives primitive) {
            this.imports = imports;
            this.primitive = primitive;
        }

        ValueInterface(Primitives primitive) {
            this.primitive = primitive;
            this.imports = Set.of(BasicOperations.Value);
        }

        @Override
        public TypeImports getUseImportTypes() {
            return new TypeImports(this);
        }

        @Override
        public String typeName() {
            if (Generators.DEBUG)
                return name() + Generators.DEBUG_NAME_APPEND;
            return name();
        }

        public Primitives getPrimitive() {
            return primitive;
        }

        @Override
        public OperationAttr.Operation getOperation() {
            if (primitive.noJavaPrimitive) {
                return new DestructOnlyOp<>(this, Primitives.ADDRESS);
            }
            return new DestructOnlyOp<>(this, primitive);
        }
    }

    /**
     * interface of {@link BindTypes}
     */
    public enum BindTypeOperations implements BaseType {
        I8Op(ValueInterface.I8I),
        I16Op(ValueInterface.I16I),
        I32Op(ValueInterface.I32I),
        I64Op(ValueInterface.I64I),
        FP32Op(ValueInterface.FP32I),
        FP64Op(ValueInterface.FP64I),
        PtrOp(ValueInterface.PtrI, Set.of(BasicOperations.PteI)),
        FP16Op(ValueInterface.FP16I),
        FP128Op(ValueInterface.FP128I),
        I128Op(ValueInterface.I128I);
        private final ValueInterface value;
        private final Set<TypeAttr.TypeRefer> referenceTypes;

        BindTypeOperations(ValueInterface elementType, Set<TypeAttr.TypeRefer> referenceTypes) {
            this.value = elementType;
            this.referenceTypes = referenceTypes;
        }

        BindTypeOperations(ValueInterface elementType) {
            this.value = elementType;
            this.referenceTypes = Set.of();
        }

        @Override
        public TypeImports getUseImportTypes() {
            return new TypeImports(this);
        }

        public ValueInterface getValue() {
            return value;
        }

        @Override
        public String typeName() {
            if (Generators.DEBUG)
                return name() + Generators.DEBUG_NAME_APPEND;
            return name();
        }

        public String operatorTypeName() {
            return name() + "I";
        }
    }


    private static final Supplier<Set<TypeAttr.TypeRefer>> BIND_TYPE_COMMON_REF = () ->
            Set.of(ValueInterface.I64I, FFMTypes.SEGMENT_ALLOCATOR,
                    BasicOperations.Info, SpecificTypes.Array, BindTypes.Ptr);

    public enum BindTypes implements BaseType, SingleGenerationType {
        I8(BindTypeOperations.I8Op),
        I16(BindTypeOperations.I16Op),
        I32(BindTypeOperations.I32Op),
        I64(BindTypeOperations.I64Op),
        FP32(BindTypeOperations.FP32Op),
        FP64(BindTypeOperations.FP64Op),
        Ptr(BindTypeOperations.PtrOp, () -> Set.of(FFMTypes.MEMORY_SEGMENT, SpecificTypes.MemoryUtils, FFMTypes.SEGMENT_ALLOCATOR,
                FFMTypes.VALUE_LAYOUT, BasicOperations.Info, SpecificTypes.ArrayOp, ValueInterface.PtrI)),
        FP16(BindTypeOperations.FP16Op),
        FP128(BindTypeOperations.FP128Op, () -> Set.of(ValueInterface.I64I, SpecificTypes.MemoryUtils, FFMTypes.SEGMENT_ALLOCATOR,
                BasicOperations.Info, SpecificTypes.Array, BindTypes.Ptr)),
        I128(BindTypeOperations.I128Op, () -> Set.of(ValueInterface.I64I, SpecificTypes.MemoryUtils, FFMTypes.SEGMENT_ALLOCATOR,
                BasicOperations.Info, SpecificTypes.Array, BindTypes.Ptr));
        private final BindTypeOperations operations;
        // lazy init
        private final Supplier<Set<TypeAttr.TypeRefer>> referenceTypes;


        BindTypes(BindTypeOperations operations, Supplier<Set<TypeAttr.TypeRefer>> referenceTypes) {
            this.operations = operations;
            this.referenceTypes = referenceTypes;
        }

        BindTypes(BindTypeOperations operations) {
            this.operations = operations;
            this.referenceTypes = BIND_TYPE_COMMON_REF;
        }

        public static String makePtrGenericName(String t) {
            return Ptr.typeName() + "<%s>".formatted(t);
        }

        public static String makePtrWildcardName(String t) {
            return Ptr.typeName() + "<? extends %s>".formatted(t);
        }

        @Override
        public TypeImports getUseImportTypes() {
            return new TypeImports(this);
        }

        public BindTypeOperations getOperations() {
            return operations;
        }

        public OperationAttr.Operation getOperation() {
            if (operations.getValue().primitive.noJavaPrimitive) {
                return new NoJavaPrimitiveType<>(this, this);
            }
            return new ValueBased<>(this, typeName(), this);
        }

        @Override
        public MemoryLayouts getMemoryLayout(PackageManager packages) {
            return operations.value.primitive.getMemoryLayout(packages);
        }

        @Override
        public long byteSize() {
            return getPrimitiveType().byteSize;
        }

        @Override
        public String typeName() {
            if (Generators.DEBUG)
                return name() + Generators.DEBUG_NAME_APPEND;
            return name();
        }

        public Primitives getPrimitiveType() {
            return operations.value.primitive;
        }
    }

    public enum SpecificTypes implements BaseType {
        FunctionUtils(false, Set::of),
        MemoryUtils(false, () -> Set.of(FFMTypes.MEMORY_SEGMENT, FFMTypes.VALUE_LAYOUT, FFMTypes.ARENA, FFMTypes.SEGMENT_ALLOCATOR)),
        ArrayOp(true, () -> Set.of(BindTypeOperations.PtrOp, BasicOperations.Value, BasicOperations.Info,
                FFMTypes.MEMORY_SEGMENT, BasicOperations.ArrayI, BindTypes.Ptr, ValueInterface.I64I, ValueInterface.I32I, BindTypes.I64)),
        Array(true, () -> Set.of(FFMTypes.MEMORY_SEGMENT, FFMTypes.VALUE_LAYOUT, FFMTypes.SEGMENT_ALLOCATOR, ArrayOp,
                BasicOperations.Info, ValueInterface.PtrI, BindTypes.Ptr, BindTypeOperations.PtrOp,
                SpecificTypes.MemoryUtils, ValueInterface.I64I, BindTypes.I64, ValueInterface.I32I)),
        FlatArrayOp(true, () -> Set.of(BasicOperations.Value, BasicOperations.Info,
                FFMTypes.MEMORY_SEGMENT, BasicOperations.ArrayI, BindTypes.Ptr, ValueInterface.I64I, BindTypes.I64, ValueInterface.I32I)),
        FlatArray(true, () -> Set.of(FFMTypes.MEMORY_SEGMENT, FFMTypes.MEMORY_LAYOUT, FFMTypes.SEGMENT_ALLOCATOR,
                FlatArrayOp, BasicOperations.Info, ValueInterface.PtrI, BindTypes.Ptr, BindTypeOperations.PtrOp,
                SpecificTypes.MemoryUtils, ValueInterface.I64I, ValueInterface.I32I, BindTypes.I64)),
        StructOp(true, () -> Set.of(BasicOperations.Value, BasicOperations.Info,
                FFMTypes.MEMORY_SEGMENT, FFMTypes.MEMORY_LAYOUT, BasicOperations.StructI)),
        Str(false, () -> Set.of(ArrayOp, BasicOperations.Info, Array, BindTypes.I8, BindTypes.Ptr,
                ValueInterface.PtrI, ValueInterface.I8I, ValueInterface.I64I, BindTypes.I64)),
        ;

        final boolean generic;
        // lazy init
        private final Supplier<Set<TypeAttr.TypeRefer>> referenceTypes;

        SpecificTypes(boolean generic, Supplier<Set<TypeAttr.TypeRefer>> referenceTypes) {
            this.generic = generic;
            this.referenceTypes = referenceTypes;
        }

        @Override
        public TypeImports getUseImportTypes() {
            return new TypeImports(this);
        }

        public String getGenericName(String t) {
            if (!generic) {
                throw new IllegalStateException("Cannot get generic name for non-generic type");
            }
            return typeName() + "<%s>".formatted(t);
        }

        public String getWildcardName(String t) {
            if (!generic) {
                throw new IllegalStateException("Cannot get wildcard name for non-generic type");
            }
            return typeName() + "<? extends %s>".formatted(t);
        }

        @Override
        public String typeName() {
            if (Generators.DEBUG)
                return name() + Generators.DEBUG_NAME_APPEND;
            return name();
        }
    }


    public enum FFMTypes implements BaseType {
        MEMORY_SEGMENT(MemorySegment.class),
        MEMORY_LAYOUT(MemoryLayout.class),
        VALUE_LAYOUT(ValueLayout.class),
        ADDRESS_LAYOUT(AddressLayout.class),
        ARENA(Arena.class),
        METHOD_HANDLES(MethodHandles.class),
        FUNCTION_DESCRIPTOR(FunctionDescriptor.class),
        SEGMENT_ALLOCATOR(SegmentAllocator.class),
        BYTE_ORDER(ByteOrder.class),
        METHOD_HANDLE(MethodHandle.class);

        private final Class<?> type;

        FFMTypes(Class<?> type) {
            this.type = type;
        }

        @Override
        public TypeImports getUseImportTypes() {
            return new TypeImports(this);
        }

        public Class<?> getType() {
            return type;
        }

        @Override
        public String typeName() {
            return type.getSimpleName();
        }

        public PackagePath packagePath() {
            String packageName = type.getPackageName();
            PackagePath packagePath = new PackagePath();
            for (String s : packageName.split("\\.")) {
                packagePath = packagePath.add(s);
            }
            return packagePath.close(type.getSimpleName());
        }

        public static HashMap<FFMTypes, PackagePath> packagePaths() {
            HashMap<FFMTypes, PackagePath> map = new HashMap<>();
            for (FFMTypes value : FFMTypes.values()) {
                map.put(value, value.packagePath());
            }
            return map;
        }
    }


    /**
     * generated, essential types
     */
    public sealed interface BaseType extends TypeAttr.TypeRefer, TypeAttr.GenerationType, TypeAttr.NamedType permits BindTypes, FFMTypes, BindTypeOperations, BasicOperations, SpecificTypes, ValueInterface {
    }
}