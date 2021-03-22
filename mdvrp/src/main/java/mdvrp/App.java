package mdvrp;

import java.util.HashMap;
import java.util.Map;

public class App {

    private static Map<String, Double> stopThreshold = new HashMap<>(32);

    private static void printHelp() {
        System.out.println(
                "Run the given file in config.yaml with --run-file or -r, or run all tests with --run-tests or -t");
    }

    public static void main(String[] args) {
        stopThreshold.put("p01", 611.1);
        stopThreshold.put("p02", 519.75);
        stopThreshold.put("p03", 756.0);
        stopThreshold.put("p04", 1063.9);
        stopThreshold.put("p05", 834.75);
        stopThreshold.put("p06", 966.0);
        stopThreshold.put("p07", 1008.0);
        stopThreshold.put("p08", 4830.0);
        stopThreshold.put("p09", 4233.6);
        stopThreshold.put("p10", 4014.15);
        stopThreshold.put("p11", 4046.7);
        stopThreshold.put("p12", 1369.9);
        stopThreshold.put("p13", 1384.8);
        stopThreshold.put("p14", 1428.0);
        stopThreshold.put("p15", 2889.6);
        stopThreshold.put("p16", 2733.15);
        stopThreshold.put("p17", 2817.15);
        stopThreshold.put("p18", 4165.35);
        stopThreshold.put("p19", 4098.15);
        stopThreshold.put("p20", 4378.5);
        stopThreshold.put("p21", 6426.0);
        stopThreshold.put("p22", 6230.7);
        stopThreshold.put("p23", 6520.5);

        if (args.length == 0) {
            printHelp();
            return;
        }

        String argument = args[0];
        if (argument.equals("--help") || argument.equals("-h")) {
            printHelp();
        } else if (argument.equals("--run-file") || argument.equals("-r")) {
            runSingle();
        } else if (argument.equals("--run-tests") || argument.equals("-t")) {
            runAll();
        } else {
            printHelp();
        }
    }

    private static double getThreshold(String fileName, double default_threshold) {
        if (stopThreshold.containsKey(fileName)) {
            return stopThreshold.get(fileName);
        }
        return default_threshold;
    }

    private static void runSingle() {
        ConfigParser configParser = new ConfigParser();
        configParser.parseConfig();
        System.out.println("Input file: " + configParser.inputFile);

        ProblemParser problemParser = new ProblemParser();
        problemParser.parseFile(configParser.inputFile);

        long start = System.currentTimeMillis();
        Solver solver = new Solver(configParser, problemParser,
                getThreshold(configParser.inputFile, configParser.stopThreshold));
        solver.runGA();
        System.out.println(
                "Elapsed training time: " + Helper.roundDouble((System.currentTimeMillis() - start) / 1000.0) + " s");

        solver.saveBest();
        System.out.println("Best final fitness: " + solver.bestFitness());
    }

    private static void runAll() {
        ConfigParser configParser = new ConfigParser();
        configParser.parseConfig();
        configParser.verbose = false;

        for (int i = 1; i < 24; i++) {
            // TODO read file names in test_data instead
            if (i < 10) {
                configParser.inputFile = "p0" + i;
            } else {
                configParser.inputFile = "p" + i;
            }
            System.out.println("Input file: " + configParser.inputFile);
            ProblemParser problemParser = new ProblemParser();
            problemParser.parseFile(configParser.inputFile);

            long start = System.currentTimeMillis();
            Solver solver = new Solver(configParser, problemParser,
                    getThreshold(configParser.inputFile, configParser.stopThreshold));
            solver.runGA();
            System.out.println("Elapsed training time: "
                    + Helper.roundDouble((System.currentTimeMillis() - start) / 1000.0) + " s");

            solver.saveBest();
            System.out.println("Best final fitness: " + solver.bestFitness());
            System.out.println("");
        }
    }
}
