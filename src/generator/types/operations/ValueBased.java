package generator.types.operations;

import generator.PackageManager;
import generator.types.*;
import generator.types.CommonTypes.SpecificTypes;
import utils.CommonUtils;

import java.util.Optional;

public class ValueBased<T extends TypeAttr.GenerationType & TypeAttr.NamedType & TypeAttr.TypeRefer & TypeAttr.OperationType> implements OperationAttr.ValueBasedOperation {
    private final T type;
    private final String typeName;
    private final CommonTypes.Primitives primitives;
    private final CommonTypes.BindTypes bindTypes;

    public ValueBased(T type, String typeName, CommonTypes.BindTypes bindTypes) {
        this.type = type;
        this.typeName = typeName;
        this.primitives = bindTypes.getPrimitiveType();
        this.bindTypes = bindTypes;
    }

    @Override
    public FuncOperation getFuncOperation(PackageManager packages) {
        return new FuncOperation() {
            @Override
            public Result destructToPara(String varName) {
                return new Result(varName + ".operator().value()", new TypeImports().addUseImports(type));
            }

            @Override
            public Result constructFromRet(String varName) {
                return new Result("new " + typeName + "(" + varName + ")", new TypeImports().addUseImports(type));
            }

            @Override
            public CommonTypes.Primitives getPrimitiveType() {
                return primitives;
            }
        };
    }

    private Optional<CommonTypes.Primitives> selectFitBitSize(long bitOffset, long bitSize) {
        if (primitives.nonNativeIntegral())
            return Optional.empty();
        for (CommonTypes.Primitives primitiveType : CommonTypes.Primitives.values()) {
            if (primitiveType.nonNativeIntegral()) continue;
            long typeBitSize = primitiveType.byteSize() * 8;
            long typeBitAlign = primitiveType.alignment() * 8;
            long shift = bitOffset % typeBitAlign;
            if (shift + bitSize <= typeBitSize) {
                return Optional.of(primitiveType);
            }
        }
        return Optional.empty();
    }

    private static String longToString(long value) {
        if (value > Integer.MAX_VALUE)
            return value + "L";
        return String.valueOf(value);
    }

    private static String unsignedCast(CommonTypes.Primitives src, CommonTypes.Primitives dst, String content) {
        if (src.byteSize() >= dst.byteSize()) return "(" + dst.getPrimitiveTypeName() + ") (" + content + ")";
        // dst > src
        content = "(" + src.getPrimitiveTypeName() + ") (" + content + ")";
        if (dst == CommonTypes.Primitives.JAVA_INT) {
            return "%s.toUnsignedInt(%s)".formatted(src.getBoxedTypeName(), content);
        }
        if (dst == CommonTypes.Primitives.JAVA_SHORT) {
            return "(short) %s.toUnsignedInt(%s)".formatted(src.getBoxedTypeName(), content);
        }
        if (dst == CommonTypes.Primitives.JAVA_LONG) {
            return "%s.toUnsignedLong(%s)".formatted(src.getBoxedTypeName(), content);
        }
        throw CommonUtils.shouldNotReachHere();
    }

    @Override
    public MemoryOperation getMemoryOperation(PackageManager packages) {
        return new MemoryOperation() {
            @Override
            public Getter getter(String ms, long offset) {
                return new Getter("", typeName, "new %s(%s)".formatted(typeName,
                        "%s.get%s(%s, %s)".formatted(packages.useClass(SpecificTypes.MemoryUtils),
                                primitives.getMemoryUtilName(), ms, offset)),
                        new TypeImports().addUseImports(type));
            }

            @Override
            public Optional<Getter> getterBitfield(String ms, long bitOffset, long bitSize) {
                Optional<CommonTypes.Primitives> type_ = selectFitBitSize(bitOffset, bitSize);
                if (type_.isEmpty())
                    return Optional.empty();
                CommonTypes.Primitives p = type_.get();
                long typeBitSize = p.byteSize() * 8;
                long bitAlign = p.alignment() * 8;
                long mask = bitSize == typeBitSize ? -1L : (1L << bitSize) - 1L;
                long shift = bitOffset % bitAlign;
                if (mask == -1 && shift == 0) {
                    String value = "%s.get%s(%s, %s)".formatted(
                            packages.useClass(SpecificTypes.MemoryUtils),
                            p.getMemoryUtilName(),
                            ms,
                            bitOffset / 8);
                    value = unsignedCast(p, primitives, value);
                    return Optional.of(new Getter("", typeName,
                            "        return new %s(%s);".formatted(typeName, value),
                            new TypeImports().addUseImports(type)));
                }
                long offset = bitOffset - shift;
                //long get = ms.get(ValueLayout.JAVA_LONG, offset / 8);
                //result = (get >>> shift) & mask;
                String checkByteOrder = """
                                if (%1$s.nativeOrder() == %1$s.BIG_ENDIAN) throw new UnsupportedOperationException();
                        """.formatted(packages.useClass(CommonTypes.FFMTypes.BYTE_ORDER));
                String value = "%s.get%s(%s, %s)".formatted(
                        packages.useClass(SpecificTypes.MemoryUtils),
                        p.getMemoryUtilName(),
                        ms,
                        offset / 8);
                value = "(%s >>> %s) & %s".formatted(value, shift, longToString(mask));
                value = unsignedCast(p, primitives, value);
                var ret = "        return new %s(%s);".formatted(typeName, value);
                return Optional.of(new Getter("", typeName, checkByteOrder + ret,
                        new TypeImports().addUseImports(type).addUseImports(SpecificTypes.MemoryUtils)
                                .addUseImports(CommonTypes.FFMTypes.BYTE_ORDER)));
            }

            @Override
            public Setter setter(String ms, long offset, String varName) {
                CommonOperation.UpperType upperType = getCommonOperation().getUpperType(packages);
                return new Setter(upperType.typeName(packages, TypeAttr.NameType.WILDCARD) + " " + varName,
                        "%s.set%s(%s, %s, %s.operator().value())".formatted(
                                packages.useClass(SpecificTypes.MemoryUtils),
                                primitives.getMemoryUtilName(), ms, offset, varName), new TypeImports());
            }

            @Override
            public Optional<Setter> setterBitfield(String ms, long bitOffset, long bitSize, String varName) {
                Optional<CommonTypes.Primitives> type_ = selectFitBitSize(bitOffset, bitSize);
                if (type_.isEmpty())
                    return Optional.empty();
                CommonTypes.Primitives p = type_.get();
                long typeBitSize = p.byteSize() * 8;
                long bitAlign = p.alignment() * 8;
                long mask = bitSize == typeBitSize ? -1L : (1L << bitSize) - 1L;
                long shift = bitOffset % bitAlign;
                if (mask == -1 && shift == 0) {
                    var typeValue = p == primitives ? "" : ".%sValue() ".formatted(p.getPrimitiveTypeName());
                    CommonOperation.UpperType upperType = getCommonOperation().getUpperType(packages);
                    return Optional.of(new Setter(upperType.typeName(packages, TypeAttr.NameType.WILDCARD) + " " + varName,
                            "        %s.set%s(%s, %s, %s.operator().value()%s);".formatted(
                                    packages.useClass(SpecificTypes.MemoryUtils),
                                    p.getMemoryUtilName(), ms, bitOffset / 8, varName, typeValue), new TypeImports()));
                }
                long offset = bitOffset - shift;
                CommonOperation.UpperType upperType = getCommonOperation().getUpperType(packages);
                var get = """
                                if (%1$s.nativeOrder() == %1$s.BIG_ENDIAN) throw new UnsupportedOperationException();
                        """.formatted(packages.useClass(CommonTypes.FFMTypes.BYTE_ORDER));
                var preGet = "%s.get%s(%s, %s)".formatted(
                        packages.useClass(SpecificTypes.MemoryUtils),
                        p.getMemoryUtilName(),
                        ms,
                        offset / 8);
                var userSet = "%s.operator().value()".formatted(varName);
                var value = "((%s & %s) << %s) | (%s & ~(%s))".formatted(
                        userSet,
                        longToString(mask),
                        shift,
                        preGet,
                        longToString(mask << shift));
                value = "(%s) (%s)".formatted(
                        p.getPrimitiveTypeName(),
                        value);
                String set = "        %s.set%s(%s, %s, %s);".formatted(
                        packages.useClass(SpecificTypes.MemoryUtils),
                        p.getMemoryUtilName(),
                        ms,
                        offset / 8, value);
                return Optional.of(new Setter(upperType.typeName(packages, TypeAttr.NameType.WILDCARD) + " " + varName,
                        get + set, new TypeImports()));
            }
        };
    }

    @Override
    public CommonOperation getCommonOperation() {
        return new CommonOperation() {
            @Override
            public Operation makeOperation(PackageManager packages) {
                return CommonOperation.makeStaticOperation(packages, type);
            }

            @Override
            public UpperType getUpperType(PackageManager packages) {
                if (type instanceof CommonTypes.BindTypes) {
                    return new Warp<>(bindTypes.getOperations().getValue(), new Reject<>(type));
                }
                End<?> end = new End<>(type, packages);
                if (type instanceof ValueBasedType v && v.getPointerType().isPresent()) {
                    // make Ptr<Void> -> Type
                    TypeAttr.TypeRefer pointee = v.getPointerType().get().pointee();
                    if (pointee instanceof VoidType) {
                        return end;
                    }
                    // PtrI<pointee>, get pointee as inner Wildacrd type
                    return new Warp<>(bindTypes.getOperations().getValue(), new UpperType() {
                        @Override
                        public String typeName(PackageManager packages, TypeAttr.NameType nameType) {
                            return ((TypeAttr.NamedType) pointee).typeName(packages, nameType);
                        }

                        @Override
                        public TypeAttr.OperationType type() {
                            return ((TypeAttr.OperationType) pointee);
                        }
                    });
                }
                return new Warp<>(bindTypes.getOperations().getValue(), end);
            }
        };
    }

}
