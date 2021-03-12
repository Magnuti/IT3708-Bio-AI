package mdvrp;

import java.util.ArrayList;
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
            depot.routeSchedulingFirstPart();
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

}
