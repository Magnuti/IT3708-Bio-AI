package mdvrp;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class AppTest {
    Customer customer1 = new Customer(1, 5, 5, 10);
    Customer customer2 = new Customer(2, 6, 9, 13);
    Customer customer3 = new Customer(3, -3, -10, 12);

    Route route = new Route();

    Depot depot1 = new Depot(40, 80);
    Depot depot2 = new Depot(40, 80);

    @Test
    public void copyWorking() {
        depot1.addCustomer(customer1);
        depot1.addCustomer(customer2);
        depot1.routeSchedulingFirstPart();
        depot1.routeSchedulingSecondPart();
        List<Depot> depots = new ArrayList<>();
        depots.add(depot1);
        depots.add(depot2);

        Chromosome parent = new Chromosome(depots);
        Chromosome offspring = new Chromosome(parent);

        assertNotEquals(parent, offspring);

        assertEquals(parent.depots.size(), offspring.depots.size());

        offspring.depots.get(0).addCustomer(customer3);

        // assertNotEquals(parent.depots.get(0).customers.size(),
        // offspring.depots.get(0).customers.size());

        route.customers.add(customer3);
        offspring.depots.get(0).routes.add(route);
        assertNotEquals(parent.depots.get(0).routes.size(), offspring.depots.get(0).routes.size());
    }
}
