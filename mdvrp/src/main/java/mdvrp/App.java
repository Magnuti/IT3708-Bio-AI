package mdvrp;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
        ProblemParser p = new ProblemParser();
        p.parseFile("p01");

        System.out.println(p.maxVehicesPerDepot);
        System.out.println(p.customerCount);
        System.out.println(p.depotCount);

        for (Depot depot : p.depots) {
            System.out.println(depot);
        }
        for (Customer c : p.customers) {
            System.out.println(c);
        }
    }
}
