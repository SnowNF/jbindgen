package analyser;

import libclang.LibclangFunctionSymbols;
import libclang.common.*;
import libclang.enumerates.CXCursorKind;
import libclang.structs.CXCursor;
import libclang.structs.CXSourceLocation;
import libclang.structs.CXString;
import libclang.structs.CXType;
import libclang.values.CXFile;
import utils.LoggerUtils;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public class Utils {

    public static String cXString2String(CXString cxString) {
        Ptr<I8> str = LibclangFunctionSymbols.clang_getCString(cxString);
        return new Str(str).get();
    }

    public static void printLocation(CXCursor cursor) {
        LoggerUtils.debug("Processing path " + getLocationForDebug(cursor));
    }

    public static String getLocation(CXType type, CXCursor fallback) {
        try (Arena mem = Arena.ofConfined()) {
            CXCursor cursor = LibclangFunctionSymbols.clang_getTypeDeclaration(mem, type);
            if (cursor.kind().equals(CXCursorKind.CXCursor_NoDeclFound)) {
                if (fallback == null)
                    return null;
                return getCursorLocation(fallback);
            }
            return getCursorLocation(cursor);
        }
    }

    public static String getTypeLocation(CXType type) {
        try (Arena mem = Arena.ofConfined()) {
            CXCursor cursor = LibclangFunctionSymbols.clang_getTypeDeclaration(mem, type);
            return getCursorLocation(cursor);
        }
    }

    public static String getCursorLocation(CXCursor cursor) {
        try (Arena mem = Arena.ofConfined()) {
            CXSourceLocation location = LibclangFunctionSymbols.clang_getCursorLocation(mem, cursor);
            PtrI<I32> nullptr = () -> () -> MemorySegment.NULL;
            Array<CXFile> file = CXFile.list(mem, 1);
            LibclangFunctionSymbols.clang_getFileLocation(location, file, nullptr, nullptr, nullptr);
            CXString path = LibclangFunctionSymbols.clang_getFileName(mem, file.getFirst());
            String s = Utils.cXString2String(path);
            LibclangFunctionSymbols.clang_disposeString(path);
            return s;
        }
    }

    public static String getLocationForDebug(CXCursor cursor) {
        try (Arena mem = Arena.ofConfined()) {
            CXSourceLocation location = LibclangFunctionSymbols.clang_getCursorLocation(mem, cursor);
            Array<CXFile> file = CXFile.list(mem, 1);
            Array<I32> line = I32.list(mem, 1);
            Array<I32> column = I32.list(mem, 1);
            Array<I32> offset = I32.list(mem, 1);
            LibclangFunctionSymbols.clang_getFileLocation(location, file, line, column, offset);
            CXString path = LibclangFunctionSymbols.clang_getFileName(mem, file.getFirst());
            String s = Utils.cXString2String(path) + " line " + line.getFirst() + " column " + column.getFirst() + " offset " + offset.getFirst();
            LibclangFunctionSymbols.clang_disposeString(path);
            return s;
        }
    }
}
