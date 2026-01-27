package generator.generation.generator;

import generator.Dependency;
import generator.Generators;
import generator.PackageManager;
import generator.generation.Enumerate;
import generator.types.CommonTypes;
import generator.types.EnumType;

public class EnumGenerator implements Generator {
    private final EnumType enumerate;
    private final PackageManager packages;
    private final Generators.Writer writer;

    public EnumGenerator(Enumerate enumerate, Dependency dependency, Generators.Writer writer) {
        this.enumerate = enumerate.getTypePkg().type();
        this.packages = new PackageManager(dependency, enumerate.getTypePkg().packagePath());
        this.writer = writer;
    }

    @Override
    public void generate() {
        writer.write(packages, makeEnum(enumerate, packages));
    }

    private static String makeEnum(EnumType e, PackageManager packages) {
        packages.useClass(CommonTypes.FFMTypes.SEGMENT_ALLOCATOR);
        String enumName = packages.getCurrentClass();
        var members = e.getMembers().stream()
                .map(member -> "public static final %s %s = new %s(%s);".formatted(enumName, member.name(), enumName, member.val())).toList();
        return """
                public final class %1$s implements %9$s<%1$s>, %8$s<%1$s> {
                    public static final %8$s.Operations<%1$s> OPERATIONS = %9$s.makeOperations(%1$s::new);
                    private final int val;
                
                    public %1$s(int val) {
                        this.val = val;
                    }
                
                    public %1$s(%6$s<?> val) {
                        this.val = val.operator().value();
                    }
                
                    public static %11$s<%1$s> array(SegmentAllocator allocator, long len) {
                        return new %11$s<>(allocator, OPERATIONS, len);
                    }
                
                    public static %11$s<%1$s> array(SegmentAllocator allocator, %7$s<?> len) {
                        return array(allocator, len.operator().value());
                    }
                
                    public static %12$s<%1$s> ptr(SegmentAllocator allocator) {
                        return new %12$s<>(allocator, OPERATIONS);
                    }
                
                    public int value() {
                        return val;
                    }
                
                    @Override
                    public %10$s<%1$s> operator() {
                        return new %10$s<>() {
                            @Override
                            public %8$s.Operations<%1$s> getOperations() {
                                return OPERATIONS;
                            }
                
                            @Override
                            public %1$s self() {
                                return %1$s.this;
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
                }""".formatted(enumName, null, null,
                String.join("\n    ", members), // 4
                packages.useClass(CommonTypes.SpecificTypes.FunctionUtils), // 5
                packages.useClass(e.getType().getOperations().getValue()), // 6
                packages.useClass(CommonTypes.ValueInterface.I64I), // 7
                packages.useClass(CommonTypes.BasicOperations.Info), // 8
                packages.useClass(CommonTypes.BindTypeOperations.I32Op), // 9
                CommonTypes.BindTypeOperations.I32Op.operatorTypeName(), // 10
                packages.useClass(CommonTypes.SpecificTypes.Array), // 11
                packages.useClass(CommonTypes.BindTypes.Ptr) // 12
        );
    }
}
