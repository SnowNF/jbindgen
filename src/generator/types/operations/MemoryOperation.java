package generator.types.operations;

import generator.types.TypeImports;

import java.util.Optional;

public interface MemoryOperation {

    record Setter(String para, String codeSegment, TypeImports imports) {
        public Setter(String para, String codeSegment) {
            this(para, codeSegment, new TypeImports());
        }
    }

    record Getter(String para, String ret, String codeSegment, TypeImports imports) {
        public Getter(String para, String ret, String codeSegment) {
            this(para, ret, codeSegment, new TypeImports());
        }
    }

    /**
     * return the string that construct the type
     */
    Getter getter(String ms, long offset);

    default Optional<Getter> getterBitfield(String ms, long bitOffset, long bitSize) {
        return Optional.empty();
    }

    /**
     * return the string that copy the type to memory segment
     */
    Setter setter(String ms, long offset, String varName);

    default Optional<Setter> setterBitfield(String ms, long bitOffset, long bitSize, String varName) {
        return Optional.empty();
    }
}
