package mdvrp;

public class Customer {

    private int id;
    private int x;
    private int y;
    private int serviceDuration;
    private int demand; // Demand means how much capacity this customer requires in a vehicle/route

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

    public int getId() {
        return this.id;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getServiceDuration() {
        return this.serviceDuration;
    }

    public int getDemand() {
        return this.demand;
    }

}
