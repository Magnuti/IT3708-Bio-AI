package mdvrp;

public class Depot {
    int id;
    int maxRouteDuration;
    int maxVehicleLoad;
    int x;
    int y;

    public Depot(int maxRouteDuration, int maxVehicleLoad) {
        this.maxRouteDuration = maxRouteDuration;
        this.maxVehicleLoad = maxVehicleLoad;
    }

    @Override
    public String toString() {
        return Helper.getClassValuesAsString(this);
    }

}
