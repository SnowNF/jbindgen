package generator.types.operations;

import generator.types.CommonTypes;

public interface FuncOperation {
    record Result(String codeSegment) {
    }

    /**
     * func(Type.destruct())
     */
    Result destructToPara(String varName);

    /**
     * var type = construct(func());
     */
    Result constructFromRet(String varName);


    CommonTypes.Primitives getPrimitiveType();
}
