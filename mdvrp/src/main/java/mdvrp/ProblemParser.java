package mdvrp;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class ProblemParser {
    int maxVehicesPerDepot;
    int customerCount;
    int depotCount;
    List<Depot> depots = new ArrayList<>();
    List<Customer> customers = new ArrayList<>();

    private List<Integer> stringToInts(String line) {
        line = line.trim().replaceAll(" +", " ");
        return Arrays.asList(line.split(" ")).stream().map(x -> Integer.parseInt(x)).collect(Collectors.toList());
    }

    public void parseFile(String fileName) {
        try {
            Path path = Paths.get("test_data", fileName);
            File myObj = new File(path.toString());
            Scanner scanner = new Scanner(myObj);

            int lineCounter = 0;
            while (scanner.hasNextLine()) {
                List<Integer> numbers = stringToInts(scanner.nextLine());

                if (lineCounter == 0) {
                    this.maxVehicesPerDepot = numbers.get(0);
                    this.customerCount = numbers.get(1);
                    this.depotCount = numbers.get(2);
                } else if (lineCounter < 1 + this.depotCount) {
                    this.depots.add(new Depot(numbers.get(0), numbers.get(1)));
                } else if (lineCounter < 1 + this.depotCount + this.customerCount) {
                    // TODO ask about whether index 3 (service duration) is required for this assignment
                    // TODO all problem sets have this value set to 0.
                    this.customers.add(new Customer(numbers.get(0), numbers.get(1), numbers.get(2), numbers.get(3),
                            numbers.get(4)));
                } else {
                    int index = lineCounter - (1 + this.depotCount + this.customerCount);
                    Depot depot = this.depots.get(index);
                    depot.id = lineCounter - (this.depotCount + this.customerCount);
                    depot.x = numbers.get(1);
                    depot.y = numbers.get(2);
                    this.depots.set(index, depot);
                }
                lineCounter++;
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
