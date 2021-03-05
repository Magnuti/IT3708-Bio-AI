package mdvrp;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
        ProblemParser p = new ProblemParser();
        String x = p.parseFile("p01");

        System.out.println(x);
    }
}
