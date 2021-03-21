package mdvrp;

import java.util.ArrayList;
import java.util.List;

public class Chromosome {

    List<Depot> depots;
    double fitness; // Less fitness is better, this is a minimization problem
    int tooManyRoutes;

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
        for (Depot depot : this.depots) {
            depot.routeSchedulingSecondPart();
        }
    }

    // Dynamic
    public void updateFitnessByTotalDistanceWithPenalty(int generation) {
        double fitness = 0.0;
        for (Depot depot : this.depots) {
            fitness += depot.routes.stream().map(x -> x.routeLength).reduce(0.0, Double::sum);
        }
        // Distance-based penalty

        // TODO experiment with theese, static vs. dynamic (i.e., with time) and weights
        double penaltyWeight = 300.0;
        // fitness += penaltyWeight * Math.pow((double) this.tooManyRoutes, 2);
        double time = ((double) generation + 2000) / 2000.0; // * 4350
        // double time = 1.0 + (double) generation / 10000.0;
        fitness += penaltyWeight * (double) this.tooManyRoutes * time;
        this.fitness = fitness;
    }

    public void getLegality(int maxVehicesPerDepot) {
        this.tooManyRoutes = (int) this.depots.stream().filter(x -> x.routes.size() > maxVehicesPerDepot).count();
    }
}
