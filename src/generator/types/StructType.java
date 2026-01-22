package generator.types;

import generator.types.operations.MemoryBased;
import generator.types.operations.OperationAttr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static utils.CommonUtils.Assert;

public final class StructType implements SingleGenerationType {
    /**
     * the struct member
     *
     * @param type    the member type
     * @param name    member name
     * @param offset  normally equals offsetof(TYPE, MEMBER) * 8
     * @param bitSize when using bitfield
     */
    public record Member(TypeAttr.TypeRefer type, String name, long offset, long bitSize) {
        private String typeName() {
            return ((TypeAttr.NamedType) type).typeName(TypeAttr.NameType.GENERIC);
        }

        // note: to avoid member to be a graph, we should compare type name instead of type
        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Member member = (Member) o;
            return offset == member.offset && bitSize == member.bitSize
                   && Objects.equals(name, member.name)
                   && Objects.equals(typeName(), member.typeName());
        }

        @Override
        public int hashCode() {
            return Objects.hash(typeName(), name, offset, bitSize);
        }

        @Override
        public String toString() {
            return "Member{" +
                   "type=" + ((TypeAttr.NamedType) type).typeName(TypeAttr.NameType.GENERIC) +
                   ", name='" + name + '\'' +
                   ", offset=" + offset +
                   ", bitSize=" + bitSize +
                   '}';
        }

        public boolean bitField() {
            return ((TypeAttr.SizedType) type).byteSize() * 8 != bitSize;
        }
    }

    private final long byteSize;
    private final String typeName;
    private final List<Member> members;
    private final MemoryLayouts memoryLayout;

    public interface MemberProvider {
        List<Member> provide(StructType structType);
    }

    public StructType(long byteSize, String typeName, MemberProvider memberProvider) {
        this.byteSize = byteSize;
        this.typeName = typeName;
        this.members = List.copyOf(memberProvider.provide(this));
        memoryLayout = makeMemoryLayout(members, byteSize);
    }

    private static MemoryLayouts makeMemoryLayout(List<Member> members, long byteSize) {
        if (members.isEmpty() || members.stream().anyMatch(Member::bitField))
            return MemoryLayouts.structLayout(List.of(MemoryLayouts.sequenceLayout(CommonTypes.Primitives.JAVA_BYTE.getMemoryLayout(), byteSize)));

        // merge union via same offset
        HashMap<Long, List<Member>> memberOffset = new HashMap<>();
        for (Member member : members) {
            long offset = member.offset;
            if (!memberOffset.containsKey(offset)) {
                memberOffset.put(offset, new ArrayList<>());
            }
            memberOffset.get(offset).add(member);
        }
        ArrayList<MemoryLayouts> layouts = new ArrayList<>();
        ArrayList<Long> offsets = new ArrayList<>(memberOffset.keySet());
        offsets.sort(Long::compareTo);

        long prevBits = 0;
        for (long offsetBit : offsets) {
            long gap = offsetBit - prevBits;
            if (gap != 0) {
                Assert(gap > 0);
                Assert(gap % 8 == 0);
                layouts.add(MemoryLayouts.paddingLayout(gap / 8L));
            }
            List<Member> union = memberOffset.get(offsetBit);
            long unionBits = 0; // bit sizeof union
            Assert(!union.isEmpty());
            ArrayList<MemoryLayouts> unionLayouts = new ArrayList<>();
            for (Member u : union) {
                unionBits = Math.max(unionBits, u.bitSize);
                MemoryLayouts ml = ((TypeAttr.OperationType) u.type).getOperation().getCommonOperation().makeDirectMemoryLayout();
                unionLayouts.add(MemoryLayouts.withName(ml, u.name));
            }
            layouts.add(unionLayoutMaybe(unionLayouts));
            prevBits = offsetBit + unionBits;
        }
        // the remain gap
        long gap = byteSize * 8 - prevBits;
        if (gap != 0) {
            Assert(gap > 0);
            Assert(gap % 8 == 0);
            layouts.add(MemoryLayouts.paddingLayout(gap / 8L));
        }
        return MemoryLayouts.structLayout(layouts);
    }

    private static MemoryLayouts unionLayoutMaybe(List<MemoryLayouts> layouts) {
        return layouts.size() == 1 ? layouts.getFirst() : MemoryLayouts.unionLayout(layouts);
    }

    @Override
    public OperationAttr.Operation getOperation() {
        return new MemoryBased(this);
    }

    public List<Member> getMembers() {
        return members;
    }

    @Override
    public TypeImports getUseImportTypes() {
        return new TypeImports(this);
    }

    @Override
    public TypeImports getDefineImportTypes() {
        TypeImports imports = CommonTypes.SpecificTypes.Array.getUseImportTypes()
                .addUseImports(CommonTypes.SpecificTypes.Single)
                .addUseImports(CommonTypes.SpecificTypes.StructOp)
                .addUseImports(CommonTypes.BindTypes.Ptr)
                .addUseImports(CommonTypes.BasicOperations.Info);
        for (Member member : members) {
            imports.addUseImports(member.type);
        }
        return imports.removeImport(this);
    }

    @Override
    public String typeName(TypeAttr.NameType nameType) {
        return typeName;
    }

    @Override
    public MemoryLayouts getMemoryLayout() {
        return memoryLayout;
    }

    @Override
    public long byteSize() {
        return byteSize;
    }

    @Override
    public String toString() {
        return "StructType{" +
               "members=" + members +
               ", memoryLayout='" + memoryLayout + '\'' +
               ", typeName='" + typeName + '\'' +
               '}';
    }

    // do not compare memoryLayout and members since which is null
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StructType that)) return false;
        return byteSize == that.byteSize && Objects.equals(typeName, that.typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(byteSize, typeName);
    }
}
