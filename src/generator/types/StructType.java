package generator.types;

import generator.PackageManager;
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
            return ((TypeAttr.NamedType) type).typeName();
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
                   "type=" + ((TypeAttr.NamedType) type).typeName() +
                   ", name='" + name + '\'' +
                   ", offset=" + offset +
                   ", bitSize=" + bitSize +
                   '}';
        }

        public boolean bitField() {
            return ((TypeAttr.SizedType) type).byteSize() * 8 != bitSize || offset % 8 != 0;
        }
    }

    private final long byteSize;
    private final long byteAlign;
    private final String typeName;
    private final List<Member> members;

    public interface MemberProvider {
        List<Member> provide(StructType structType);
    }

    public StructType(long byteSize, long byteAlign, String typeName, MemberProvider memberProvider) {
        this.byteSize = byteSize;
        this.byteAlign = byteAlign;
        this.typeName = typeName;
        this.members = List.copyOf(memberProvider.provide(this));
    }

    private static MemoryLayouts makeMemoryLayouts(CommonTypes.Primitives primitives, long len, long bytePadding, PackageManager packages) {
        if (bytePadding != 0) {
            return MemoryLayouts.structLayout(List.of(
                    MemoryLayouts.sequenceLayout(primitives.getMemoryLayout(), len),
                    MemoryLayouts.paddingLayout(bytePadding)));
        }
        return MemoryLayouts.structLayout(List.of(MemoryLayouts.sequenceLayout(primitives.getMemoryLayout(), len)));
    }

    private static MemoryLayouts makeMemoryLayout(List<Member> members, long byteSize, long byteAlign, PackageManager packages) {
        if (members.isEmpty() || members.stream().anyMatch(Member::bitField)) {
            if (byteAlign == 1)
                return makeMemoryLayouts(CommonTypes.Primitives.JAVA_BYTE, byteSize, 0, packages);
            if (byteAlign == 2)
                return makeMemoryLayouts(CommonTypes.Primitives.JAVA_SHORT, byteSize / 2, byteSize % 2, packages);
            if (byteAlign == 4)
                return makeMemoryLayouts(CommonTypes.Primitives.JAVA_INT, byteSize / 4, byteSize % 4, packages);
            if (byteAlign == 8)
                return makeMemoryLayouts(CommonTypes.Primitives.JAVA_LONG, byteSize / 8, byteSize % 8, packages);
            if (byteAlign == 16)
                return makeMemoryLayouts(CommonTypes.Primitives.Integer128, byteSize / 16, byteSize % 16, packages);
            throw new IllegalArgumentException("unknown byteAlign: " + byteAlign);
        }

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
                MemoryLayouts ml = ((TypeAttr.OperationType) u.type).getOperation().getCommonOperation().makeMemoryLayout(packages);
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
    public String typeName() {
        return typeName;
    }

    public MemoryLayouts getMemoryLayout(PackageManager packages) {
        return makeMemoryLayout(members, byteSize, byteAlign, packages);
    }

    @Override
    public long byteSize() {
        return byteSize;
    }

    @Override
    public String toString() {
        return "StructType{" +
               "members=" + members +
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
