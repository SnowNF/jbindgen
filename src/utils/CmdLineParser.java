package utils;

import analyser.Analyser;
import generator.PackagePath;
import processor.Processor;
import processor.Utils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class CmdLineParser {
    private static class Component {
        public String header;
        public Pattern headerFilterRegex = Pattern.compile(".*");  // allow all headers
        public Pattern declFilterRegex = Pattern.compile(".*"); // allow all decl name
        public List<String> args;
        public boolean analyseMacro;
        public boolean greedy = true; // false then track generation by type reference
        public Path outDir;
        public String libPkg;
        public String libName;

        public boolean unfinished() {
            return header == null ||
                   headerFilterRegex == null ||
                   declFilterRegex == null ||
                   args == null ||
                   outDir == null ||
                   libPkg == null ||
                   libName == null;
        }

        @Override
        public String toString() {
            return "Component{" +
                   "header='" + header + '\'' +
                   ", headerFilterRegex='" + headerFilterRegex + '\'' +
                   ", declFilterRegex='" + declFilterRegex + '\'' +
                   ", args=" + args +
                   ", analyseMacro=" + analyseMacro +
                   ", greedy=" + greedy +
                   ", outDir=" + outDir +
                   ", libPkg='" + libPkg + '\'' +
                   ", libName='" + libName + '\'' +
                   '}';
        }
    }

    private static void printHelp() {
        System.out.println("Usage:");
        System.out.println("--header=<header_file_path>");
        System.out.println("--args=<arg1>;<arg2>;...");
        System.out.println("--enable-macro=<true|false>");
        System.out.println("--out=<output_directory>");
        System.out.println("--pkg-name=<package_name>");
        System.out.println("--name=<library_name>");
        System.out.println("--filter-header=<filter_regex>");
        System.out.println("--filter-decl=<decl_regex>");
        System.out.println("--next-component");
    }

    private static List<Component> parse(String[] args) {
        if (args.length == 0) {
            throw new RuntimeException();
        }
        List<Component> components = new ArrayList<>();
        Component current = new Component();
        for (String arg : args) {
            if (arg.equals("--next-component")) {
                if (current.unfinished())
                    throw new RuntimeException("Incomplete argument detected: " + current);
                components.add(current);
                current = new Component();
                System.out.println("Note: creating new extra component");
                continue;
            }
            String[] kv = arg.split("=");
            if (arg.equals("--header=")) {
                kv = new String[]{"--header", ""};
                System.out.println("Note: creating the common component");
            }
            if (kv.length != 2) {
                throw new IllegalArgumentException("Invalid argument:" + arg + ", should be --key=value");
            }
            var key = kv[0];
            var value = kv[1];

            switch (key) {
                case "--header":
                    current.header = value;
                    break;
                case "--args":
                    current.args = Arrays.stream(value.split(";")).filter(s -> !s.isEmpty()).toList();
                    break;
                case "--enable-macro":
                    current.analyseMacro = Boolean.parseBoolean(value);
                    break;
                case "--out":
                    current.outDir = Path.of(value);
                    break;
                case "--pkg-name":
                    current.libPkg = value;
                    break;
                case "--name":
                    current.libName = value;
                    break;
                case "--filter-header":
                    current.headerFilterRegex = Pattern.compile(value);
                    break;
                case "--filter-decl":
                    current.declFilterRegex = Pattern.compile(value);
                    break;
                case "--greedy":
                    current.greedy = Boolean.parseBoolean(value);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid argument:" + arg);
            }
        }
        if (current.unfinished())
            throw new RuntimeException("Incomplete argument detected: " + current);
        components.add(current);
        return components;
    }

    private static boolean checkArgs(String[] args) {
        if (args.length == 0) {
            printHelp();
            return false;
        }
        for (String arg : args) {
            if (arg.equals("--help") || arg.equals("-h")) {
                printHelp();
                return false;
            }
        }
        return true;
    }

    public static void solveAndGen(String[] args) {
        if (!checkArgs(args))
            return;

        List<Component> components = parse(args);
        System.out.println("Parsed " + components.size() + " component(s):");
        for (Component component : components) {
            System.out.println(component);
        }

        var it = components.iterator();
        Component primary = it.next();
        Processor primaryProc = new Processor(Utils.DestinationProvider.ofDefault(new PackagePath(primary.outDir).add(primary.libPkg), primary.libName)
        );
        if (!primary.header.isEmpty()) {
            Analyser primaryAnalyser = new Analyser(primary.header, primary.args, primary.analyseMacro);
            primaryAnalyser.close();
            primaryProc = primaryProc.withExtra(primaryAnalyser,
                    Utils.DestinationProvider.ofDefault(new PackagePath(primary.outDir).add(primary.libPkg), primary.libName),
                    primary.greedy, Utils.Filter.ofDefault(
                            s -> primary.headerFilterRegex.matcher(s).matches(),
                            s -> primary.declFilterRegex.matcher(s).matches())
            );
        }
        while (it.hasNext()) {
            Component extra = it.next();
            Analyser analyser = new Analyser(extra.header, extra.args, extra.analyseMacro);
            analyser.close();
            primaryProc = primaryProc.withExtra(analyser,
                    Utils.DestinationProvider.ofDefault(new PackagePath(extra.outDir).add(extra.libPkg), extra.libName),
                    extra.greedy, Utils.Filter.ofDefault(
                            s -> primary.headerFilterRegex.matcher(s).matches(),
                            s -> primary.declFilterRegex.matcher(s).matches()));
        }
        primaryProc.generate();
    }
}
