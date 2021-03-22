package mdvrp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Depot {
    private int id;
    private double maxRouteDistance;
    private int maxVehicleLoad;
    private int x;
    private int y;

    private List<Customer> customers;
    private Set<Customer> swappableCustomers;

    List<Route> routes = new ArrayList<>();

    public Depot(int maxRouteDistance, int maxVehicleLoad) {
        // Ignore maxRouteDistance for 0
        if (maxRouteDistance == 0) {
            this.maxRouteDistance = Double.POSITIVE_INFINITY;
        } else {
            this.maxRouteDistance = (double) maxRouteDistance;
        }
        this.maxVehicleLoad = maxVehicleLoad;
        this.customers = new ArrayList<>();
        this.swappableCustomers = new HashSet<>();
    }

    public void initDepotSecond(int id, int x, int y) {
        // We need to split up the constructor in two parts because the problem input
        // file has a dumb format
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public Depot(Depot depotToCopy) {
        this.id = depotToCopy.id;
        this.maxRouteDistance = depotToCopy.maxRouteDistance;
        this.maxVehicleLoad = depotToCopy.maxVehicleLoad;
        this.x = depotToCopy.x;
        this.y = depotToCopy.y;
        if (depotToCopy.customers != null) {
            this.customers = new ArrayList<>(depotToCopy.customers);
        }
        this.swappableCustomers = new HashSet<>(depotToCopy.swappableCustomers);
        this.routes = new ArrayList<>();
        for (Route route : depotToCopy.routes) {
            this.routes.add(new Route(route));
        }
    }

    public void shuffleCustomers() {
        Collections.shuffle(this.customers);
    }

    /**
     * Builds the routes from the information in the depot's customer list, such
     * that the route length and capacity is not breached. Note that this can lead
     * to more routes than allowed for a single depot, so that constraint must be
     * handled somewhere else.
     */
    public void routeSchedulingFirstPart() {
        this.routes.clear();

        Route route = new Route();
        int fromX = this.getX();
        int fromY = this.getY();
        Customer prevCustomer = null;
        double prevCustomersDistanceHome = 0.0;
        for (Customer customer : this.customers) {
            double distance = Helper.euclidianDistance(fromX, fromY, customer.getX(), customer.getY());
            double distanceHome = Helper.euclidianDistance(customer.getX(), customer.getY(), this.getX(), this.getY());
            if (this.getMaxVehicleLoad() >= route.usedCapacity + customer.getDemand()
                    && this.getMaxRouteDistance() >= route.routeLength + distance + distanceHome) {
                // Successfully adds the customer to the current route
                route.customers.add(customer);
                route.routeLength += distance;
                route.usedCapacity += customer.getDemand();
                fromX = customer.getX();
                fromY = customer.getY();
                prevCustomer = customer;
                prevCustomersDistanceHome = distanceHome;
            } else {
                if (prevCustomer == null) {
                    // This happens if the first customer is too far out or too heavy
                    route.customers.add(customer);
                    route.routeLength = distanceHome * 2;
                    route.usedCapacity = customer.getDemand();
                    this.routes.add(route);

                    route = new Route();
                } else {
                    // Sends the current route back to the depot and starts a new one
                    route.routeLength += prevCustomersDistanceHome;
                    this.routes.add(route);

                    route = new Route();
                    route.customers.add(customer);
                    distance = Helper.euclidianDistance(this.getX(), this.getY(), customer.getX(), customer.getY());
                    route.routeLength = distance;
                    route.usedCapacity = customer.getDemand();
                    fromX = customer.getX();
                    fromY = customer.getY();
                    prevCustomer = customer;
                    prevCustomersDistanceHome = distance;
                }

                // // * Skip sanity check for performance gain
                // if (route.usedCapacity > this.getMaxVehicleLoad() || route.routeLength >
                // this.getMaxRouteDistance()) {
                // throw new Error("A customer is invalid for a depot!");
                // }
            }
        }
        if (route.customers.isEmpty()) {
            // This happens if the first customer is illegal and it is the only customer,
            // then we do not want to add an empty route
            return;
        }
        route.routeLength += prevCustomersDistanceHome;
        this.routes.add(route);
    }

    public void routeSchedulingSecondPart() {
        if (this.routes.size() < 2) {
            return;
        }

        for (int i = this.routes.size() - 2; i > -2; i--) {
            // Iterates the routes backwards. The reasoning is that the last route is
            // probably not full, so we should start by appending a possible customer to
            // that route instead of trying to fill routes that are already full as could
            // have happened if we had started with the first route.
            int next_i = i + 1;
            Route route;
            if (i == -1) {
                route = this.routes.get(this.routes.size() - 1);
            } else {
                route = this.routes.get(i);
            }

            Route nextRoute = this.routes.get(next_i);
            double cost = route.routeLength + nextRoute.routeLength;
            Customer customer = route.customers.get(route.customers.size() - 1);

            // Cache stuff
            double routeLength = route.routeLength;
            double routeCapacity = route.usedCapacity;
            double nextRouteLength = nextRoute.routeLength;
            double nextRouteCapacity = nextRoute.usedCapacity;

            route.customers.remove(customer);
            nextRoute.customers.add(0, customer);
            this.recalculateUsedRouteLengthAndCapacity(route);
            this.recalculateUsedRouteLengthAndCapacity(nextRoute);
            double newCost = route.routeLength + nextRoute.routeLength;
            if (nextRoute.routeLength <= this.maxRouteDistance && nextRoute.usedCapacity <= this.maxVehicleLoad
                    && newCost < cost) {
                // Feasible and better, so we keep it
                continue;
            } else {
                // Not feasible or not better --> roll back
                route.customers.add(customer);
                nextRoute.customers.remove(0);
                route.routeLength = routeLength;
                route.usedCapacity = routeCapacity;
                nextRoute.routeLength = nextRouteLength;
                nextRoute.usedCapacity = nextRouteCapacity;
            }
        }

        // Remove all routes that lost all customers
        this.routes = this.routes.stream().filter(x -> !x.customers.isEmpty()).collect(Collectors.toList());

        // We do not need customers after route scheduling is done
        this.customers = null;
    }

    public void pruneEmtpyRoutes() {
        this.routes = this.routes.stream().filter(x -> x.customers.size() > 0).collect(Collectors.toList());
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

    public double getMaxRouteDistance() {
        return this.maxRouteDistance;
    }

    public int getMaxVehicleLoad() {
        return this.maxVehicleLoad;
    }

    public void addCustomer(Customer customer) {
        this.customers.add(customer);
    }

    public void addSwappableCustomer(Customer customer) {
        this.swappableCustomers.add(customer);
    }

    public Set<Customer> getSwappableCustomers() {
        return this.swappableCustomers;
    }

    @Override
    public String toString() {
        return Helper.getClassValuesAsString(this);
    }

}
