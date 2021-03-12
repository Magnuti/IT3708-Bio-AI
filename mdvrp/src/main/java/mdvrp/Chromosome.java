package mdvrp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Chromosome {

    List<Depot> depots;
    double fitness;

    public Chromosome(List<Depot> depots) {
        this.depots = depots;
    }

    public Chromosome(Chromosome chromosomeToCopy) {
        this.depots = new ArrayList<>();
        for (Depot depot : chromosomeToCopy.depots) {
            this.depots.add(new Depot(depot));
        }
        this.fitness = chromosomeToCopy.fitness;
    }

    public void routeSchedulingFirstPart() {
        // TODO parallellize this
        for (Depot depot : this.depots) {
            Collections.shuffle(depot.customers); // ! Move this out of here, it should not shuffle every time this
                                                  // ! method is called, only on init
            Route route = new Route();
            int fromX = depot.getX();
            int fromY = depot.getY();
            double routeLength = 0.0;
            double usedCapacity = 0.0;
            Customer prevCustomer = null;
            for (Customer customer : depot.customers) {
                double distance = Helper.euclidianDistance(fromX, fromY, customer.getX(), customer.getY());
                if (depot.getMaxVehicleLoad() >= usedCapacity + customer.getDemand()
                        && depot.getMaxRouteDuration() >= routeLength + distance + Helper
                                .euclidianDistance(customer.getX(), customer.getY(), depot.getX(), depot.getY())) {
                    route.customers.add(customer);
                    usedCapacity += customer.getDemand();
                    routeLength += distance;
                    fromX = customer.getX();
                    fromY = customer.getY();
                    prevCustomer = customer;
                } else {
                    routeLength += Helper.euclidianDistance(prevCustomer.getX(), prevCustomer.getY(), depot.getX(),
                            depot.getY()); // Distance back to the depot
                    route.routeLength = routeLength;
                    route.usedCapacity = usedCapacity;
                    depot.routes.add(route);
                    route = new Route();
                    fromX = depot.getX();
                    fromY = depot.getY();
                    usedCapacity = customer.getDemand();
                    routeLength = distance;
                    route.customers.add(customer);
                    fromX = customer.getX();
                    fromY = customer.getY();

                    // * Skip sanity check for performance gain
                    if (usedCapacity > depot.getMaxVehicleLoad() || routeLength > depot.getMaxRouteDuration()) {
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
