package mdvrp;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Solver {
    int maxVehicesPerDepot;
    int customerCount;
    int depotCount;
    List<Depot> depots;
    List<Customer> customers;

    public Solver(ProblemParser problemParser) {
        this.maxVehicesPerDepot = problemParser.maxVehicesPerDepot;
        this.customerCount = problemParser.customerCount;
        this.depotCount = problemParser.depotCount;
        this.depots = problemParser.depots;
        this.customers = problemParser.customers;
    }

    double euclidianDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
    }

    public void initDepotAssignment() {
        // TODO parallellize this
        for (Customer customer : this.customers) {
            double lowestDistance = Double.POSITIVE_INFINITY;
            Depot bestDepot = null;
            for (Depot depot : this.depots) {
                double distance = euclidianDistance(depot.x, depot.y, customer.x, customer.y);
                if (distance < lowestDistance) {
                    lowestDistance = distance;
                    bestDepot = depot;
                }
            }
            bestDepot.customers.add(customer);

            // TODO add borderline cases as described in section 3.9
        }
    }

    double getRouteLength(Depot depot, List<Customer> route) {
        double length = 0.0;
        int fromX = depot.x;
        int fromY = depot.y;
        for (Customer customer : depot.customers) {
            length += euclidianDistance(fromX, fromY, customer.x, customer.y);
            fromX = customer.x;
            fromY = customer.y;
        }
        length += euclidianDistance(fromX, fromY, depot.x, depot.y);
        return length;
    }

    public void save() {
        try {
            Path path = Paths.get("solutions");
            System.out.println(path.toAbsolutePath());
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            path = Paths.get(path.toString(), "solution.res");
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            FileWriter fr = new FileWriter(path.toString());
            double totalRouteLength = 0.0;
            for (Depot depot : this.depots) {
                totalRouteLength += depot.routes.stream().map(x -> getRouteLength(depot, x)).reduce(0.0, Double::sum);
            }
            fr.write(String.format(Locale.US, "%.2f", totalRouteLength));
            fr.write(System.lineSeparator());
            for (Depot depot : this.depots) {
                for (int i = 0; i < depot.routes.size(); i++) {
                    List<Customer> route = depot.routes.get(i);
                    // for (List<Customer> customers : depot.routes){
                    fr.write(Integer.toString(depot.id));
                    fr.write("\t");
                    fr.write(Integer.toString(i + 1));
                    fr.write("\t");
                    double routeLength = getRouteLength(depot, route);
                    fr.write(String.format(Locale.US, "%.2f", routeLength));
                    fr.write("\t");
                    fr.write(Integer.toString(route.stream().map(x -> x.demand).reduce(0, Integer::sum)));
                    fr.write("\t");
                    fr.write(Integer.toString(depot.id));
                    fr.write("\t");
                    for (Customer c : route) {
                        fr.write(Integer.toString(c.id));
                        fr.write(" ");
                    }
                    fr.write(System.lineSeparator());
                }
            }
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void routeSchedulingFirstPart() {
        // TODO parallellize this
        for (Depot depot : this.depots) {
            Collections.shuffle(depot.customers); // ? Is this required?
            List<Customer> route = new ArrayList<>();
            int fromX = depot.x;
            int fromY = depot.y;
            int remainingCapacity = depot.maxVehicleLoad;
            double remainingRouteLength = depot.maxRouteDuration;
            for (Customer customer : depot.customers) {
                if (remainingCapacity >= customer.demand
                        && remainingRouteLength >= euclidianDistance(fromX, fromY, customer.x, customer.y)
                                + euclidianDistance(customer.x, customer.y, depot.x, depot.y)) {
                    route.add(customer);
                    remainingCapacity -= customer.demand;
                    remainingRouteLength -= euclidianDistance(fromX, fromY, customer.x, customer.y);
                    fromX = customer.x;
                    fromY = customer.y;
                } else {
                    depot.routes.add(route);
                    route = new ArrayList<>();
                    fromX = depot.x;
                    fromY = depot.y;
                    remainingCapacity = depot.maxVehicleLoad - customer.demand;
                    remainingRouteLength = depot.maxRouteDuration
                            - euclidianDistance(fromX, fromY, customer.x, customer.y);
                    route.add(customer);
                    fromX = customer.x;
                    fromY = customer.y;

                    // * Skip sanity check for performance gain
                    if (remainingCapacity < 0 || remainingRouteLength < 0) {
                        throw new Error("A customer is invalid for a depot!");
                    }
                }
            }
            depot.routes.add(route);

            // System.out.println(depot.routes.stream().map(x -> x.stream().map(y ->
            // y.id).collect(Collectors.toList()))
            // .collect(Collectors.toList()));
        }
    }

    @Override
    public String toString() {
        return Helper.getClassValuesAsString(this);
    }
}
