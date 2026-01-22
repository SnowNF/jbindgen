package generator.generation.generator;

import generator.Dependency;
import generator.Utils;
import generator.generation.Enumerate;
import generator.types.CommonTypes;
import generator.types.TypeAttr;

public class EnumGenerator implements Generator {
    private final Enumerate enumerate;
    private final Dependency dependency;

    public EnumGenerator(Enumerate enumerate, Dependency dependency) {
        this.enumerate = enumerate;
        this.dependency = dependency;
    }

    @Override
    public void generate() {
        Utils.write(enumerate.getTypePkg().packagePath(), makeEnum(enumerate, dependency));
    }

    private static String makeEnum(Enumerate e, Dependency dependency) {
        String enumName = Generator.getTypeName(e.getTypePkg().type());
        var members = e.getTypePkg().type().getMembers().stream()
                .map(member -> "public static final %s %s = new %s(%s);".formatted(enumName, member.name(), enumName, member.val())).toList();
        return """
                %2$s
                %3$s
                
                import java.lang.foreign.SegmentAllocator;
                
                public final class %1$s implements I32Op<%1$s>, Info<%1$s> {
                    public static final Info.Operations<%1$s> OPERATIONS = I32Op.makeOperations(%1$s::new);
                    private final int val;
                
                    public %1$s(int val) {
                        this.val = val;
                    }
                
                    public %1$s(%6$s<?> val) {
                        this.val = val.operator().value();
                    }
                
                    public static Array<%1$s> list(SegmentAllocator allocator, long len) {
                        return new Array<>(allocator, OPERATIONS, len);
                    }
                
                    public static Array<%1$s> list(SegmentAllocator allocator, %7$s<?> len) {
                        return new Array<>(allocator, OPERATIONS, len);
                    }
                
                    public int value() {
                        return val;
                    }
                
                    @Override
                    public I32OpI<%1$s> operator() {
                        return new I32OpI<>() {
                            @Override
                            public Info.Operations<%1$s> getOperations() {
                                return OPERATIONS;
                            }
                
                            @Override
                            public Integer value() {
                                return val;
                            }
                        };
                    }
                
                    private String str;
                
                    @Override
                    public String toString() {
                        if (str == null) {
                            str = enumToString(this);
                            if (str == null) str = String.valueOf(val);
                        }
                        return str;
                    }
                
                    public static String enumToString(%1$s e) {
                        return %5$s.enumToString(%1$s.class, e);
                    }
                
                    @Override
                    public boolean equals(Object obj) {
                        return obj instanceof %1$s that && that.val == val;
                    }
                
                    %4$s
                }""".formatted(enumName, e.getTypePkg().packagePath().makePackage(), // 2
                Generator.extractImports(e, dependency), String.join("\n    ", members), // 4
                CommonTypes.SpecificTypes.FunctionUtils.getRawName(), // 5
                e.getTypePkg().type().getType().getOperations().getValue().typeName(TypeAttr.NameType.RAW), // 6
                CommonTypes.ValueInterface.I64I.typeName(TypeAttr.NameType.RAW) // 7

        );
    }
}
