package mdvrp;

import java.util.List;

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
        return Math.sqrt((x1 - x2) ^ 2 + (y1 - y2) ^ 2);
    }

    public void initDepotAssignment() {
        for (int i = 0; i < this.customers.size(); i++) {
            Customer customer = this.customers.get(i);
            double lowestDistance = Double.POSITIVE_INFINITY;
            Depot bestDepot = null;
            for (Depot depot : this.depots) {
                double distance = euclidianDistance(depot.x, depot.y, customer.x, customer.y);
                if (distance < lowestDistance) {
                    lowestDistance = distance;
                    bestDepot = depot;
                }
            }
            customer.depot = bestDepot;
            this.customers.set(i, customer);

            // TODO add borderline cases as described in section 3.9
        }
    }

    @Override
    public String toString() {
        return Helper.getClassValuesAsString(this);
    }
}
