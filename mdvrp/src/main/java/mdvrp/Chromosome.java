package mdvrp;

import java.util.ArrayList;
import java.util.List;

public class Chromosome {

    List<Depot> depots;
    double fitness; // Less fitness is better, this is a minimization problem

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

    public void updateFitnessByTotalDistance() {
        double fitness = 0.0;
        for (Depot depot : this.depots) {
            fitness += depot.routes.stream().map(x -> x.routeLength).reduce(0.0, Double::sum);
        }
        this.fitness = fitness;
    }
}
