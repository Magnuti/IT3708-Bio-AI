package mdvrp;

public class Customer {

    private int id;
    private int x;
    private int y;
    private int demand; // Demand means how much capacity this customer requires in a vehicle/route

    public Customer(int id, int x, int y, int demand) {
        this.id = id;
        this.x = x;
        this.y = y;
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

    public int getDemand() {
        return this.demand;
    }

}
