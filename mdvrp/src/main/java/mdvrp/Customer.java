package mdvrp;

public class Customer {

    int id;
    int x;
    int y;
    int serviceDuration;
    int demand; // Demand means how much capacity this customer requires in a vehicle/route

    public Customer(int id, int x, int y, int serviceDuration, int demand) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.serviceDuration = serviceDuration;
        this.demand = demand;
    }

    @Override
    public String toString() {
        return Helper.getClassValuesAsString(this);
    }

}
