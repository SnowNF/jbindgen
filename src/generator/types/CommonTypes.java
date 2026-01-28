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
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

        public MemoryLayouts getMemoryLayout() {
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

        public boolean nonNativeIntegral() {
            return !nativeIntegral;
        }

        @Override
        public long byteSize() {
            return byteSize;
        }
    }

    public enum BasicOperations implements BaseType, TypeAttr.OperationType {
        Operation(false),
        Info(false),
        Value(false),
        PteI(false),//pointee
        ArrayI(true),
        StructI(true),
        ;
        private final boolean destruct;

        BasicOperations(boolean destruct) {
            this.destruct = destruct;
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
        PtrI(Primitives.ADDRESS),
        FP16I(Primitives.FLOAT16),
        FP128I(Primitives.LONG_DOUBLE),
        I128I(Primitives.Integer128);

        private final Primitives primitive;

        ValueInterface(Primitives primitive) {
            this.primitive = primitive;
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
        PtrOp(ValueInterface.PtrI),
        FP16Op(ValueInterface.FP16I),
        FP128Op(ValueInterface.FP128I),
        I128Op(ValueInterface.I128I);
        private final ValueInterface value;

        BindTypeOperations(ValueInterface elementType) {
            this.value = elementType;
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


    public enum BindTypes implements BaseType, SingleGenerationType {
        I8(BindTypeOperations.I8Op),
        I16(BindTypeOperations.I16Op),
        I32(BindTypeOperations.I32Op),
        I64(BindTypeOperations.I64Op),
        FP32(BindTypeOperations.FP32Op),
        FP64(BindTypeOperations.FP64Op),
        Ptr(BindTypeOperations.PtrOp),
        FP16(BindTypeOperations.FP16Op),
        FP128(BindTypeOperations.FP128Op),
        I128(BindTypeOperations.I128Op);
        private final BindTypeOperations operations;

        BindTypes(BindTypeOperations operations) {
            this.operations = operations;
        }

        public static String makePtrGenericName(String t) {
            return Ptr.typeName() + "<%s>".formatted(t);
        }

        public static String makePtrWildcardName(String t) {
            return Ptr.typeName() + "<? extends %s>".formatted(t);
        }

        public BindTypeOperations getOperations() {
            return operations;
        }

        public OperationAttr.Operation getOperation() {
            if (operations.getValue().primitive.noJavaPrimitive) {
                return new NoJavaPrimitiveType<>(this, this);
            }
            return new ValueBased<>(this, this);
        }

        public MemoryLayouts getMemoryLayout() {
            return operations.value.primitive.getMemoryLayout();
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
        FunctionUtils(false),
        MemoryUtils(false),
        ArrayOp(true),
        Array(true),
        FlatArrayOp(true),
        FlatArray(true),
        StructOp(true),
        Str(false),
        ;

        final boolean generic;

        SpecificTypes(boolean generic) {
            this.generic = generic;
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