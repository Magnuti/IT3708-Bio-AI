package mdvrp;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Chromosome {

    List<Depot> depots;
    double fitness;

    public Chromosome(List<Depot> depots) {
        this.depots = depots;
    }

    double euclidianDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
    }

    public void routeSchedulingFirstPart() {
        // TODO parallellize this
        for (Depot depot : this.depots) {
            Collections.shuffle(depot.customers);
            Route route = new Route();
            int fromX = depot.getX();
            int fromY = depot.getY();
            int remainingCapacity = depot.getMaxVehicleLoad();
            // double remainingRouteLength = depot.getMaxRouteDuration();
            double routeLength = 0.0;
            for (Customer customer : depot.customers) {
                double distance = euclidianDistance(fromX, fromY, customer.getX(), customer.getY());
                if (remainingCapacity >= customer.getDemand() && depot.getMaxRouteDuration() >= routeLength + distance
                        + euclidianDistance(customer.getX(), customer.getY(), depot.getX(), depot.getY())) {
                    route.customers.add(customer);
                    remainingCapacity -= customer.getDemand();
                    // remainingRouteLength -= routeLength;
                    routeLength += distance;
                    fromX = customer.getX();
                    fromY = customer.getY();
                } else {
                    depot.routes.add(route);
                    route.routeLength = routeLength;
                    route = new Route();
                    fromX = depot.getX();
                    fromY = depot.getY();
                    remainingCapacity = depot.getMaxVehicleLoad() - customer.getDemand();
                    routeLength = distance;
                    // remainingRouteLength = depot.getMaxRouteDuration() - distance;
                    route.customers.add(customer);
                    fromX = customer.getX();
                    fromY = customer.getY();

                    // * Skip sanity check for performance gain
                    if (remainingCapacity < 0 || routeLength > depot.getMaxRouteDuration()) {
                        throw new Error("A customer is invalid for a depot!");
                    }
                }
            }
            depot.routes.add(route);
            route.routeLength = routeLength;

            // System.out.println(depot.routes.stream().map(x -> x.stream().map(y ->
            // y.id).collect(Collectors.toList()))
            // .collect(Collectors.toList()));
        }
    }

    public void routeSchedulingSecondPart() {
        // TODO
        return;
    }

    public void updateFitnessByWeightedSum(double alpha, double beta) {
        double fitness = 0.0;
        for (Depot depot : this.depots) {
            fitness += alpha * (double) depot.routes.size()
                    + beta * depot.routes.stream().map(x -> x.routeLength).reduce(0.0, Double::sum);
        }
        this.fitness = fitness;
    }

    public void updateFitnessByWeightedSum() {
        // The default values care very much about vehicle count and very little about
        // total distance.
        // TODO experiment with different values here, our goal is to minimize the
        // distance.
        // updateFitnessByWeightedSum(100, 0.001); // Default values in the PDF
        updateFitnessByWeightedSum(0, 1);
    }

    public void mutate() {
        // TODO add mutation
    }

}
