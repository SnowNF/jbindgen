package generator.generation.generator;

import generator.PackageManager;
import generator.types.CommonTypes;
import generator.types.FunctionPtrType;
import generator.types.TypeAttr;
import generator.types.VoidType;
import utils.ConflictNameUtils;

import java.util.ArrayList;
import java.util.List;

public class FuncPtrUtils {
    public static final String SEGMENT_ALLOCATOR_PARAMETER_NAME = CommonTypes.FFMTypes.SEGMENT_ALLOCATOR.typeName().toLowerCase();

    private FuncPtrUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String arenaAutoAllocator(PackageManager packages) {
        return packages.useClass(CommonTypes.FFMTypes.ARENA) + ".ofAuto()";
    }

    static FunctionPtrType getNonConflictType(FunctionPtrType function, List<String> forbidNames) {
        ArrayList<String> existingNames = new ArrayList<>(function.getArgs().stream().map(FunctionPtrType.Arg::argName).toList());
        ArrayList<FunctionPtrType.Arg> args = new ArrayList<>();
        for (FunctionPtrType.Arg arg : function.getArgs()) {
            args.add(new FunctionPtrType.Arg(ConflictNameUtils.getNonConflictsNameExt(arg.argName(), forbidNames, existingNames), arg.type()));
        }
        return new FunctionPtrType(function.typeName(), args, (TypeAttr.TypeRefer) function.getReturnType().orElse(VoidType.VOID));
    }
}
