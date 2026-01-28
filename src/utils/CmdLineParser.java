package utils;

import analyser.Analyser;
import generator.PackagePath;
import processor.Processor;
import processor.Utils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CmdLineParser {
    private static class Component {
        public String header;
        public String filterString;
        public List<String> args;
        public boolean analyseMacro;
        public boolean greedy = true; // false then track generation by type reference
        public Path outDir;
        public String libPkg;
        public String libName;

        public boolean finished() {
            return header != null &&
                   filterString != null &&
                   args != null &&
                   outDir != null &&
                   libPkg != null &&
                   libName != null;
        }

        @Override
        public String toString() {
            return "Component{" +
                   "header='" + header + '\'' +
                   ", filterString='" + filterString + '\'' +
                   ", args=" + args +
                   ", analyseMacro=" + analyseMacro +
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
        System.out.println("--filter-str=<filter_string>");
    }

    private static List<Component> parse(String[] args) {
        if (args.length == 0) {
            throw new RuntimeException();
        }
        List<Component> components = new ArrayList<>();
        Component current = new Component();
        for (String arg : args) {
            if (current.finished()) {
                components.add(current);
                current = new Component();
                System.out.println("Note: creating new extra component");
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
                case "--filter-str":
                    current.filterString = value;
                    break;
                case "--greedy":
                    current.greedy = Boolean.parseBoolean(value);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid argument:" + arg);
            }
        }
        if (!current.finished())
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
                    Utils.Filter.ofDefault(s -> s.contains(primary.filterString)), primary.greedy);
        }

        while (it.hasNext()) {
            Component extra = it.next();
            Analyser analyser = new Analyser(extra.header, extra.args, extra.analyseMacro);
            analyser.close();
            primaryProc = primaryProc.withExtra(analyser,
                    Utils.DestinationProvider.ofDefault(new PackagePath(extra.outDir).add(extra.libPkg), extra.libName),
                    Utils.Filter.ofDefault(s -> s.contains(extra.filterString)), extra.greedy);
        }
        primaryProc.generate();
    }
}
