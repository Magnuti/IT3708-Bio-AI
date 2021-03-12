package mdvrp;

import java.util.ArrayList;
import java.util.List;

public class Depot {
    private int id;
    private double maxRouteDuration;
    private int maxVehicleLoad;
    private int x;
    private int y;
    // TODO encapsulate these
    List<Customer> customers = new ArrayList<>();
    List<Customer> swappableCustomers = new ArrayList<>();
    List<Route> routes = new ArrayList<>();

    public Depot(int maxRouteDuration, int maxVehicleLoad) {
        // Ignore maxRouteDuration for 0
        if (maxRouteDuration == 0) {
            this.maxRouteDuration = Double.POSITIVE_INFINITY;
        } else {
            this.maxRouteDuration = (double) maxRouteDuration;
        }
        this.maxVehicleLoad = maxVehicleLoad;
    }

    public Depot(Depot depotToCopy) {
        this.id = depotToCopy.id;
        this.maxRouteDuration = depotToCopy.maxRouteDuration;
        this.maxVehicleLoad = depotToCopy.maxVehicleLoad;
        this.x = depotToCopy.x;
        this.y = depotToCopy.y;
        this.customers = new ArrayList<>(depotToCopy.customers);
        this.swappableCustomers = new ArrayList<>(depotToCopy.swappableCustomers);
        this.routes = new ArrayList<>();
        for (Route route : depotToCopy.routes) {
            this.routes.add(new Route(route));
        }
    }

    public void initDepotSecond(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public void routeSchedulingFirstPart() {
        this.routes.clear();

        Route route = new Route();
        int fromX = this.getX();
        int fromY = this.getY();
        double routeLength = 0.0;
        double usedCapacity = 0.0;
        Customer prevCustomer = null;
        for (Customer customer : this.customers) {
            double distance = Helper.euclidianDistance(fromX, fromY, customer.getX(), customer.getY());
            if (this.getMaxVehicleLoad() >= usedCapacity + customer.getDemand()
                    && this.getMaxRouteDuration() >= routeLength + distance
                            + Helper.euclidianDistance(customer.getX(), customer.getY(), this.getX(), this.getY())) {
                route.customers.add(customer);
                usedCapacity += customer.getDemand();
                routeLength += distance;
                fromX = customer.getX();
                fromY = customer.getY();
                prevCustomer = customer;
            } else {
                routeLength += Helper.euclidianDistance(prevCustomer.getX(), prevCustomer.getY(), this.getX(),
                        this.getY()); // Distance back to the depot
                route.routeLength = routeLength;
                route.usedCapacity = usedCapacity;
                this.routes.add(route);
                route = new Route();
                fromX = this.getX();
                fromY = this.getY();
                usedCapacity = customer.getDemand();
                routeLength = distance;
                route.customers.add(customer);
                fromX = customer.getX();
                fromY = customer.getY();

                // * Skip sanity check for performance gain
                if (usedCapacity > this.getMaxVehicleLoad() || routeLength > this.getMaxRouteDuration()) {
                    throw new Error("A customer is invalid for a depot!");
                }
            }
        }
        route.routeLength = routeLength;
        this.routes.add(route);
    }

    public void routeSchedulingSecondPart() {
        // TODO
    }

    public List<Customer> getCustomers() {
        this.customers.clear();
        for (Route route : this.routes) {
            for (Customer customer : route.customers) {
                this.customers.add(customer);
            }
        }
        return this.customers;
    }

    public void rebuildCustomerList() {
        getCustomers();
    }

    public void recalculateUsedRouteLengthAndCapacity(Route route) {
        int fromX = this.getX();
        int fromY = this.getY();
        double routeLength = 0.0;
        double usedCapacity = 0.0;
        for (Customer customer : route.customers) {
            routeLength += Helper.euclidianDistance(fromX, fromY, customer.getX(), customer.getY());
            usedCapacity += customer.getDemand();
            fromX = customer.getX();
            fromY = customer.getY();
        }
        routeLength += Helper.euclidianDistance(fromX, fromY, this.getX(), this.getY());
        route.routeLength = routeLength;
        route.usedCapacity = usedCapacity;
    }

    public int getId() {
        return this.id;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public double getMaxRouteDuration() {
        return this.maxRouteDuration;
    }

    public int getMaxVehicleLoad() {
        return this.maxVehicleLoad;
    }

    @Override
    public String toString() {
        return Helper.getClassValuesAsString(this);
    }

}
