package mdvrp;

public class App {
    public static void main(String[] args) {
        ConfigParser configParser = new ConfigParser();
        configParser.parseConfig();
        ProblemParser problemParser = new ProblemParser();
        problemParser.parseFile(configParser.inputFile);
        Solver solver = new Solver(problemParser);
        solver.initDepotAssignment();
        solver.initPopulation(configParser.populationSize);
        solver.runGA(configParser.maxGeneration);
        solver.saveBest();
    }
}
