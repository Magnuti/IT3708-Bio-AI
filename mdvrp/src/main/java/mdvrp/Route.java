package mdvrp;

import java.util.ArrayList;
import java.util.List;

public class Route {
    List<Customer> customers = new ArrayList<>(); // TODO check if we can use an ordered set here instead of a list
    double routeLength;
    double usedCapacity;

    public Route() {

    }

    public Route(Route routeToCopy) {
        this.customers = new ArrayList<>(routeToCopy.customers);
        this.routeLength = routeToCopy.routeLength;
        this.usedCapacity = routeToCopy.usedCapacity;
    }
}
