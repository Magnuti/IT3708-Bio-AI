package mdvrp;

public class App {
    public static void main(String[] args) {
        runSingle();
        // runAll();
    }

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

        for (int i = 1; i < 24; i++) {
            configParser.inputFile = "p0" + i;
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
