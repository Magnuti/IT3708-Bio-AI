package mdvrp;

import java.util.ArrayList;
import java.util.List;

public class Depot {
    int id;
    double maxRouteDuration;
    int maxVehicleLoad;
    int x;
    int y;
    List<Customer> customers = new ArrayList<>();
    List<List<Customer>> routes = new ArrayList<>();

    public Depot(int maxRouteDuration, int maxVehicleLoad) {
        // Ignore maxRouteDuration for 0
        if (maxRouteDuration == 0) {
            this.maxRouteDuration = Double.POSITIVE_INFINITY;
        } else {
            this.maxRouteDuration = (double) maxRouteDuration;
        }
        this.maxVehicleLoad = maxVehicleLoad;
    }

    @Override
    public String toString() {
        return Helper.getClassValuesAsString(this);
    }

}
