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

            int customerCount = 0;
            int depotCount = 0;
            int lineCounter = 0;
            while (scanner.hasNextLine()) {
                List<Integer> numbers = stringToInts(scanner.nextLine());

                if (lineCounter == 0) {
                    this.maxVehicesPerDepot = numbers.get(0);
                    customerCount = numbers.get(1);
                    depotCount = numbers.get(2);
                } else if (lineCounter < 1 + depotCount) {
                    this.depots.add(new Depot(numbers.get(0), numbers.get(1)));
                } else if (lineCounter < 1 + depotCount + customerCount) {
                    this.customers.add(new Customer(numbers.get(0), numbers.get(1), numbers.get(2), numbers.get(4)));
                } else {
                    int index = lineCounter - (1 + depotCount + customerCount);
                    Depot depot = this.depots.get(index);
                    depot.initDepotSecond(lineCounter - (depotCount + customerCount), numbers.get(1), numbers.get(2));
                    this.depots.set(index, depot);
                }
                lineCounter++;
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new Error("Cannot read the given file: " + fileName);
        }
    }
}
