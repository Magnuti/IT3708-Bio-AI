package mdvrp;

public class App {
    public static void main(String[] args) {
        ProblemParser problemParser = new ProblemParser();
        problemParser.parseFile("p01");
        Solver solver = new Solver(problemParser);
        solver.initDepotAssignment();
        solver.initPopulation(6);
        solver.runGA(3);
        solver.saveBest();
    }
}
