package generator.types.operations;

import generator.types.*;

public class ValueBased<T extends TypeAttr.NamedType & TypeAttr.TypeRefer & TypeAttr.OperationType> implements OperationAttr.ValueBasedOperation {
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
    public FuncOperation getFuncOperation() {
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

    @Override
    public MemoryOperation getMemoryOperation() {
        return new MemoryOperation() {
            @Override
            public Getter getter(String ms, long offset) {
                return new Getter("", typeName, "new %s(%s)".formatted(typeName,
                        "%s.get%s(%s, %s)".formatted(CommonTypes.SpecificTypes.MemoryUtils.typeName(TypeAttr.NameType.RAW),
                                primitives.getMemoryUtilName(), ms, offset)),
                        new TypeImports().addUseImports(type).addUseImports(CommonTypes.SpecificTypes.MemoryUtils));
            }

            @Override
            public Setter setter(String ms, long offset, String varName) {
                CommonOperation.UpperType upperType = getCommonOperation().getUpperType();
                return new Setter(upperType.typeName(TypeAttr.NameType.WILDCARD) + " " + varName,
                        "%s.set%s(%s, %s, %s.operator().value())".formatted(
                                CommonTypes.SpecificTypes.MemoryUtils.typeName(TypeAttr.NameType.RAW),
                                primitives.getMemoryUtilName(), ms, offset, varName),
                        upperType.typeImports().addUseImports(CommonTypes.SpecificTypes.MemoryUtils));
            }
        };
    }

    @Override
    public CommonOperation getCommonOperation() {
        return new CommonOperation() {
            @Override
            public Operation makeOperation() {
                return CommonOperation.makeStaticOperation(type, typeName);
            }

            @Override
            public UpperType getUpperType() {
                if (type instanceof CommonTypes.BindTypes) {
                    return new Warp<>(bindTypes.getOperations().getValue(), new Reject<>(type));
                }
                End<?> end = new End<>(type);
                if (type instanceof ValueBasedType v && v.getPointerType().isPresent()) {
                    // make Ptr<Void> -> Type
                    TypeAttr.TypeRefer pointee = v.getPointerType().get().pointee();
                    if (pointee instanceof VoidType) {
                        return end;
                    }
                    // PtrI<ppintee>, get pointee as inner Wildacrd type
                    return new Warp<>(bindTypes.getOperations().getValue(), new UpperType() {
                        @Override
                        public String typeName(TypeAttr.NameType nameType) {
                            return ((TypeAttr.NamedType) pointee).typeName(nameType);
                        }

                        @Override
                        public TypeImports typeImports() {
                            return new TypeImports().addUseImports(pointee);
                        }

                        @Override
                        public TypeAttr.OperationType typeOp() {
                            return ((TypeAttr.OperationType) pointee);
                        }
                    });
                }
                return new Warp<>(bindTypes.getOperations().getValue(), end);
            }
        };
    }

}
