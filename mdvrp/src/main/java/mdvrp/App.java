package mdvrp;

public class App {
    public static void main(String[] args) {
        // runSingle();
        runAll();
    }

    // TODO add possibility to early stop

    private static void runSingle() {
        ConfigParser configParser = new ConfigParser();
        configParser.parseConfig();
        ProblemParser problemParser = new ProblemParser();
        problemParser.parseFile(configParser.inputFile);

        long start = System.currentTimeMillis();
        Solver solver = new Solver(configParser, problemParser);
        solver.initDepotAssignment();
        solver.initPopulation();
        solver.runGA();
        System.out.println("Elapsed training time: " + (System.currentTimeMillis() - start));

        solver.saveBest();
        System.out.println("Best fitness: " + solver.bestFitness());
    }

    private static void runAll() {
        ConfigParser configParser = new ConfigParser();
        configParser.parseConfig();
        configParser.verbose = false;

        for (int i = 10; i < 24; i++) {
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
            Solver solver = new Solver(configParser, problemParser);
            solver.initDepotAssignment();
            solver.initPopulation();
            solver.runGA();
            System.out.println("Elapsed training time: " + (System.currentTimeMillis() - start));

            solver.saveBest();
            System.out.println("Best fitness: " + solver.bestFitness());
            System.out.println("");
        }
    }
}
