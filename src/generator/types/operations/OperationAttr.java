package generator.types.operations;

import generator.PackageManager;

public class OperationAttr {
    public sealed interface Operation {
        FuncOperation getFuncOperation(PackageManager packages);

        MemoryOperation getMemoryOperation(PackageManager packages);

        CommonOperation getCommonOperation();
    }


    /**
     * the type is value based
     */
    public non-sealed interface ValueBasedOperation extends Operation {
    }

    /**
     * the type is MemorySegment stored
     */
    public non-sealed interface MemoryBasedOperation extends Operation {

    }

    // void, incomplete struct
    public non-sealed interface CommonOnlyOperation extends Operation {

    }

    public non-sealed interface DesctructOnlyOperation extends Operation {

    }
}
