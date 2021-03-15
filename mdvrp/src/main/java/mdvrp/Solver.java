package mdvrp;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.Collections;

public class Solver {
    // From ConfigParser
    int populationSize;
    int maxGeneration;
    double eliteRatio;
    double crossoverChance;
    double bound;
    double tournamentSelectionNumber;
    double crossoverInsertionNumber;
    int apprate;

    // From ProblemParser
    int maxVehicesPerDepot; // TODO use this somewhere
    List<Depot> depots;
    List<Customer> customers;
    List<Chromosome> population = new ArrayList<>();

    public Solver(ConfigParser configParser, ProblemParser problemParser) {
        this.populationSize = configParser.populationSize;
        this.maxGeneration = configParser.maxGeneration;
        this.eliteRatio = configParser.eliteRatio;
        this.crossoverChance = configParser.crossoverChance;
        this.bound = configParser.bound;
        this.tournamentSelectionNumber = configParser.tournamentSelectionNumber;
        this.crossoverInsertionNumber = configParser.crossoverInsertionNumber;
        this.apprate = configParser.apprate;

        this.maxVehicesPerDepot = problemParser.maxVehicesPerDepot;
        this.depots = problemParser.depots;
        this.customers = problemParser.customers;
    }

    public void initDepotAssignment() {
        // ? parallellize this
        for (Customer customer : this.customers) {
            double lowestDistance = Double.POSITIVE_INFINITY;
            Depot bestDepot = null;
            for (Depot depot : this.depots) {
                double distance = Helper.euclidianDistance(depot.getX(), depot.getY(), customer.getX(),
                        customer.getY());
                if (distance < lowestDistance) {
                    lowestDistance = distance;
                    bestDepot = depot;
                }
            }
            bestDepot.customers.add(customer);

            double bound = this.bound;
            for (Depot depot : this.depots) {
                double min = Helper.euclidianDistance(customer.getX(), customer.getY(), bestDepot.getX(),
                        bestDepot.getY());
                if ((Helper.euclidianDistance(customer.getX(), customer.getY(), depot.getX(), depot.getY()) - min)
                        / min <= bound) {
                    depot.swappableCustomers.add(customer);
                }
            }
        }
    }

    public void initPopulation() {
        // ? try to combine this method with the one above
        if (this.populationSize % 2 == 1) {
            System.out.println(
                    "Warning: Please keep the population size as an even number. Why? Because two parents can reproduce easily, while three is more difficult.");
            this.populationSize--;
            System.out.println("Using a population size of: " + this.populationSize);
        }
        for (int i = 0; i < this.populationSize; i++) {
            // We need to clone depots to the different chromosomes
            List<Depot> depots = new ArrayList<>();
            for (Depot depot : this.depots) {
                Depot depotToAdd = new Depot(depot);
                // Initialize random routes for each depot per chromosome
                Collections.shuffle(depotToAdd.customers);
                depots.add(depotToAdd);
            }
            Chromosome chromosome = new Chromosome(depots);
            chromosome.routeSchedulingFirstPart();
            this.population.add(chromosome);
        }
        this.customers = null;
        this.depots = null;
    }

    public void saveBest() {
        Collections.sort(this.population, (a, b) -> Double.compare(a.fitness, b.fitness));
        List<Depot> depots = this.population.get(0).depots;

        try {
            Path path = Paths.get("solutions");
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            path = Paths.get(path.toString(), "solution.res");
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            FileWriter fr = new FileWriter(path.toString());
            double totalRouteLength = 0.0;
            for (Depot depot : depots) {
                totalRouteLength += depot.routes.stream().map(x -> x.routeLength).reduce(0.0, Double::sum);
            }
            fr.write(String.format(Locale.US, "%.2f", totalRouteLength));
            fr.write(System.lineSeparator());
            for (Depot depot : depots) {
                for (int i = 0; i < depot.routes.size(); i++) {
                    Route route = depot.routes.get(i);
                    fr.write(Integer.toString(depot.getId()));
                    fr.write("\t");
                    fr.write(Integer.toString(depot.getId()));
                    fr.write("\t");
                    fr.write(String.format(Locale.US, "%.2f", route.routeLength));
                    fr.write("\t");
                    fr.write(
                            Integer.toString(route.customers.stream().map(x -> x.getDemand()).reduce(0, Integer::sum)));
                    fr.write("\t");
                    // Prepend the depot ID for compatibility reasons
                    fr.write(Integer.toString(depot.getId()));
                    fr.write("\t");
                    for (Customer c : route.customers) {
                        fr.write(Integer.toString(c.getId()));
                        fr.write(" ");
                    }
                    fr.write(System.lineSeparator());
                }
            }
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Chromosome[] tournamentSelection(int selection_size) {
        Chromosome[] winners = new Chromosome[selection_size];
        int tournamentSize = 2; // Binary tournament
        for (int i = 0; i < selection_size; i++) {
            // selection_size number of tournaments
            Chromosome[] tournamentSet = new Chromosome[tournamentSize];
            for (int j = 0; j < tournamentSize; j++) {
                tournamentSet[j] = Helper.getRandomElementFromList(this.population);
            }

            if (ThreadLocalRandom.current().nextDouble() < tournamentSelectionNumber) {
                // Add most fit parent
                // ? Should maybe sort here to enable k > 2, but it may affect performance
                if (tournamentSet[0].fitness <= tournamentSet[1].fitness) {
                    winners[i] = tournamentSet[0];
                } else {
                    winners[i] = tournamentSet[1];
                }
            } else {
                // Add random parent
                winners[i] = Helper.getRandomElementFromList(tournamentSet);
            }
        }
        return winners;
    }

    private InsertionCostAndFeasibility getInsertionCostAndFeasibility(Customer customer, Depot depotToModify) {
        InsertionCostAndFeasibility icaf = new InsertionCostAndFeasibility();
        for (Route route : depotToModify.routes) {
            List<Double> routeInsertionCost = new ArrayList<>();
            List<Boolean> routeMaintainsFeasibility = new ArrayList<>();
            for (int i = 0; i < route.customers.size() + 1; i++) {
                route.customers.add(i, customer);
                depotToModify.recalculateUsedRouteLengthAndCapacity(route);
                routeInsertionCost.add(route.routeLength);
                if (route.routeLength <= depotToModify.getMaxRouteDistance()
                        && route.usedCapacity <= depotToModify.getMaxVehicleLoad()) {
                    routeMaintainsFeasibility.add(true);
                } else {
                    routeMaintainsFeasibility.add(false);
                }
                route.customers.remove(customer);
            }
            icaf.insertionCost.add(routeInsertionCost);
            icaf.maintainsFeasibility.add(routeMaintainsFeasibility);
            depotToModify.recalculateUsedRouteLengthAndCapacity(route); // ? maybe cache this instead
        }
        return icaf;
    }

    private void insertCustomerAtBestLocation(InsertionCostAndFeasibility icaf, Depot depot, Customer customer) {
        double bestInsertionCost = Double.POSITIVE_INFINITY;
        Route bestRoute = null;
        int bestInsertionIndex = 0;
        for (int i = 0; i < icaf.insertionCost.size(); i++) {
            for (int k = 0; k < icaf.insertionCost.get(i).size(); k++) {
                if (icaf.maintainsFeasibility.get(i).get(k)) {
                    if (icaf.insertionCost.get(i).get(k) < bestInsertionCost) {
                        bestInsertionCost = icaf.insertionCost.get(i).get(k);
                        bestRoute = depot.routes.get(i);
                        bestInsertionIndex = k;
                    }
                }
            }
        }
        bestRoute.customers.add(bestInsertionIndex, customer);
        depot.recalculateUsedRouteLengthAndCapacity(bestRoute);
    }

    private void crossoverInsertCustomers(List<Customer> customersToAdd, Depot depotToModify) {
        for (Customer customer : customersToAdd) {
            InsertionCostAndFeasibility icaf = getInsertionCostAndFeasibility(customer, depotToModify);

            if (ThreadLocalRandom.current().nextDouble() < this.crossoverInsertionNumber) {
                if (icaf.maintainsFeasibility.stream().flatMap(List::stream).collect(Collectors.toList())
                        .contains(true)) {
                    // Insert at best feasible location
                    insertCustomerAtBestLocation(icaf, depotToModify, customer);
                } else {
                    // Create new route
                    Route route = new Route();
                    route.customers.add(customer);
                    // TODO check that maxRoutes is not breached
                    depotToModify.routes.add(route);
                    depotToModify.recalculateUsedRouteLengthAndCapacity(route);
                }
            } else {
                // Insert at first entry in the list
                // TODO, call routeScheduler when we add to the first route
                // TODO and check that no constraints are being breached
                throw new Error();
            }
        }
    }

    Chromosome[] crossover(Chromosome parent1, Chromosome parent2) {
        Chromosome offspring1 = new Chromosome(parent1);
        Chromosome offspring2 = new Chromosome(parent2);

        if (ThreadLocalRandom.current().nextDouble() < this.crossoverChance) {
            Depot depot1 = Helper.getRandomElementFromList(offspring1.depots);
            Depot depot2 = Helper.getRandomElementFromList(offspring2.depots);

            // These needs to be copied because we don't want them to change
            List<Customer> customers1 = new ArrayList<>(Helper.getRandomElementFromList(depot1.routes).customers);
            List<Customer> customers2 = new ArrayList<>(Helper.getRandomElementFromList(depot2.routes).customers);

            // Removes the customers from the chromosome
            for (Customer customer : customers1) {
                outer: for (Depot depot : offspring2.depots) {
                    for (Route route : depot.routes) {
                        if (route.customers.remove(customer)) {
                            break outer;
                        }
                    }
                }
            }

            for (Customer customer : customers2) {
                outer: for (Depot depot : offspring1.depots) {
                    for (Route route : depot.routes) {
                        if (route.customers.remove(customer)) {
                            break outer;
                        }
                    }
                }
            }

            // TODO parallellize these
            crossoverInsertCustomers(customers1, depot2);
            crossoverInsertCustomers(customers2, depot1);

            for (Depot depot : offspring1.depots) {
                depot.rebuildCustomerList();
            }

            for (Depot depot : offspring2.depots) {
                depot.rebuildCustomerList();
            }

        }
        // If not crossover, we return a copy of the parents without modifications
        Chromosome[] offsprings = { offspring1, offspring2 };
        return offsprings;
    }

    void intraDepotMutation(Depot depot) {
        switch (ThreadLocalRandom.current().nextInt(3)) {
        case 0:
            reversalMutation(depot);
            break;
        case 1:
            singleCustomerReRouting(depot);
            break;
        case 2:
            swapping(depot);
            break;
        default:
            throw new Error();
        }
    }

    void reversalMutation(Depot depot) {
        if (depot.customers.isEmpty()) {
            return;
        }
        int startIndex = ThreadLocalRandom.current().nextInt(depot.customers.size());
        int endIndex = startIndex + 1 + ThreadLocalRandom.current().nextInt(depot.customers.size() - startIndex);
        Collections.reverse(depot.customers.subList(startIndex, endIndex));
        depot.routeSchedulingFirstPart();
        depot.routeSchedulingSecondPart();
    }

    void singleCustomerReRouting(Depot depot) {
        if (depot.customers.isEmpty()) {
            return;
        }

        Customer customer = Helper.getRandomElementFromList(depot.customers);
        for (Route route : depot.routes) {
            if (route.customers.remove(customer)) {
                break;
            }
        }

        InsertionCostAndFeasibility icaf = getInsertionCostAndFeasibility(customer, depot);

        // No need to check if there are feasible locations here because it must be at
        // least one, since the removed location was feasible.
        insertCustomerAtBestLocation(icaf, depot, customer);

        depot.rebuildCustomerList();
    }

    void swapping(Depot depot) {
        if (depot.routes.size() < 2) {
            return;
        }
        List<Route> routes = Helper.getNRandomElementsFromList(depot.routes, 2);
        Route route1 = routes.get(0);
        Route route2 = routes.get(1);

        if (route1.customers.isEmpty() || route2.customers.isEmpty()) {
            return;
        }
        Customer customer1 = Helper.getRandomElementFromList(route1.customers);
        Customer customer2 = Helper.getRandomElementFromList(route2.customers);

        route1.customers.remove(customer1);
        route2.customers.remove(customer2);
        route1.customers.add(customer2);
        route2.customers.add(customer1);
        depot.rebuildCustomerList();
        depot.routeSchedulingFirstPart();
        depot.routeSchedulingSecondPart();
    }

    void interDepotMutation(Chromosome chromosome) {
        List<Depot> depotsWithSwappableCustomers = chromosome.depots.stream()
                .filter(x -> x.swappableCustomers.size() > 0).collect(Collectors.toList());
        List<Depot> toRemove = new ArrayList<>();
        for (Depot depot : depotsWithSwappableCustomers) {
            // Already full depot
            if (new HashSet<>(depot.customers).containsAll(new HashSet<>(depot.swappableCustomers))) {
                toRemove.add(depot);
            }
        }
        depotsWithSwappableCustomers.removeAll(toRemove);
        if (depotsWithSwappableCustomers.isEmpty()) {
            return;
        }

        Depot toDepot = Helper.getRandomElementFromList(depotsWithSwappableCustomers);
        HashSet<Customer> possibleCustomersToGet = new HashSet<>(toDepot.swappableCustomers);
        possibleCustomersToGet.removeAll(toDepot.customers);
        Customer customerToSwap = Helper.getRandomElementFromList(new ArrayList<>(possibleCustomersToGet));

        // Remove customerToSwap from the depot which contains it
        for (Depot depot : chromosome.depots) {
            if (depot.customers.remove(customerToSwap)) {
                depot.routeSchedulingFirstPart();
                depot.routeSchedulingSecondPart();
            }
        }

        // TODO check if not this.maxDexVehiclesPerDepot is not breached

        // TODO Maybe we should insert at the best feasible location here instead
        toDepot.customers.add(customerToSwap);
        toDepot.routeSchedulingFirstPart();
        toDepot.routeSchedulingSecondPart();

    }

    void elitism(List<Chromosome> newPopulation) {
        // Randomly replace some % of the population with the best some % from
        // the parent population
        Collections.shuffle(newPopulation);
        Collections.sort(this.population, (a, b) -> Double.compare(a.fitness, b.fitness));
        int toSwap = (int) Math.round((double) this.population.size() * this.eliteRatio);
        if (toSwap == 0) {
            System.out.println("Warning: elitism is not applied.");
        }
        for (int i = 0; i < toSwap; i++) {
            newPopulation.set(i, this.population.get(i));
        }
        this.population = newPopulation;
    }

    public void runGA() {
        System.out.println("Population size: " + this.population.size());
        for (int generation = 0; generation < this.maxGeneration; generation++) {
            List<Chromosome> newPopulation = new ArrayList<>();
            // TODO parallellize this, one thread for each i
            for (int i = 0; i < this.population.size() / 2; i++) {
                Chromosome[] parents = tournamentSelection(2); // Note that these are not copies
                Chromosome[] offsprings = crossover(parents[0], parents[1]);
                if (generation % this.apprate == 0) {
                    // Apply inter-depot mutation every 10th generation for example
                    // TODO parallellize
                    interDepotMutation(offsprings[0]);
                    interDepotMutation(offsprings[1]);
                } else {
                    // Intra-depot mutation
                    // Selects a random depot to perform mutation on
                    intraDepotMutation(Helper.getRandomElementFromList(offsprings[0].depots));
                    intraDepotMutation(Helper.getRandomElementFromList(offsprings[1].depots));
                }
                newPopulation.add(offsprings[0]);
                newPopulation.add(offsprings[1]);
            }

            for (Chromosome chromosome : newPopulation) {
                chromosome.updateFitnessByTotalDistance();
            }

            elitism(newPopulation);

            double bestFitness = Double.POSITIVE_INFINITY;
            for (Chromosome chromosome : this.population) {
                chromosome.updateFitnessByTotalDistance();
                if (chromosome.fitness < bestFitness) {
                    bestFitness = chromosome.fitness;
                }
            }

            System.out.println("Best fitness: " + bestFitness);
        }
    }

    @Override
    public String toString() {
        return Helper.getClassValuesAsString(this);
    }
}
