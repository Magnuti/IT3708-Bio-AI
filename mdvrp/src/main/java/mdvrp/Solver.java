package mdvrp;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.Collections;

public class Solver extends Thread {
    // From ConfigParser
    int maxGeneration;
    double eliteRatio;
    double crossoverChance;
    double bound;
    double tournamentSelectionNumber;
    double crossoverInsertionNumber;
    double intraDepotMutationRate;
    double interDepotMutationRate;
    int apprate;
    boolean verbose;
    int saveInterval;

    // From ProblemParser
    int maxVehicesPerDepot;

    double stopThreshold;

    // int customerCount; // ! temp

    private List<Chromosome> population = new ArrayList<>();

    public Solver(ConfigParser configParser, ProblemParser problemParser, double stopThreshold) {
        this.maxGeneration = configParser.maxGeneration;
        this.eliteRatio = configParser.eliteRatio;
        this.crossoverChance = configParser.crossoverChance;
        this.bound = configParser.bound;
        this.tournamentSelectionNumber = configParser.tournamentSelectionNumber;
        this.crossoverInsertionNumber = configParser.crossoverInsertionNumber;
        this.intraDepotMutationRate = configParser.intraDepotMutationRate;
        this.interDepotMutationRate = configParser.interDepotMutationRate;
        this.apprate = configParser.apprate;
        this.verbose = configParser.verbose;
        this.saveInterval = configParser.saveInterval;

        this.maxVehicesPerDepot = problemParser.maxVehicesPerDepot;
        // this.customerCount = problemParser.customers.size(); // ! Temp

        this.stopThreshold = stopThreshold;

        List<Depot> depots = problemParser.depots;
        List<Customer> customers = problemParser.customers;

        this.initDepotAssignment(depots, customers);
        this.initPopulation(depots, customers, configParser.populationSize);
    }

    private void initDepotAssignment(List<Depot> depots, List<Customer> customers) {
        // Initializes each customer to the nearest depot
        // ? parallellize this
        for (Customer customer : customers) {
            double lowestDistance = Double.POSITIVE_INFINITY;
            Depot bestDepot = null;
            for (Depot depot : depots) {
                double distance = Helper.euclidianDistance(depot.getX(), depot.getY(), customer.getX(),
                        customer.getY());
                if (distance < lowestDistance) {
                    lowestDistance = distance;
                    bestDepot = depot;
                }
            }
            bestDepot.addCustomer(customer);

            double bound = this.bound;
            for (Depot depot : depots) {
                double min = Helper.euclidianDistance(customer.getX(), customer.getY(), bestDepot.getX(),
                        bestDepot.getY());
                if ((Helper.euclidianDistance(customer.getX(), customer.getY(), depot.getX(), depot.getY()) - min)
                        / min <= bound) {
                    depot.addSwappableCustomer(customer);
                }
            }
        }
    }

    private void initPopulation(List<Depot> depots, List<Customer> customers, int populationSize) {
        // ? parallellize this
        if (populationSize % 2 == 1) {
            System.out.println(
                    "Warning: Please keep the population size as an even number. Why? Because two parents can reproduce easily, while three is more difficult.");
            populationSize--;
            System.out.println("Using a population size of: " + populationSize);
        }
        for (int i = 0; i < populationSize; i++) {
            // We need to clone depots to the different chromosomes
            List<Depot> depotsCopy = new ArrayList<>();
            for (Depot depot : depots) {
                Depot depotToAdd = new Depot(depot);
                // Initialize random routes for each depot per chromosome
                depotToAdd.shuffleCustomers();
                depotsCopy.add(depotToAdd);
            }
            Chromosome chromosome = new Chromosome(depotsCopy);
            chromosome.routeSchedulingFirstPart();
            chromosome.routeSchedulingSecondPart();
            chromosome.getLegality(this.maxVehicesPerDepot);
            chromosome.updateFitnessByTotalDistanceWithPenalty(0);

            this.population.add(chromosome);
        }
    }

    public void saveBest() {
        Collections.sort(this.population, (a, b) -> Double.compare(a.fitness, b.fitness));
        List<Chromosome> legalPopulation = this.population.stream().filter(x -> x.tooManyRoutes == 0)
                .collect(Collectors.toList());
        if (legalPopulation.isEmpty()) {
            return;
        }

        List<Depot> depots = legalPopulation.get(0).depots;

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
            fr.write(Helper.roundDouble(totalRouteLength));
            fr.write(System.lineSeparator());
            for (Depot depot : depots) {
                for (int i = 0; i < depot.routes.size(); i++) {
                    Route route = depot.routes.get(i);
                    fr.write(Integer.toString(depot.getId()));
                    fr.write("\t");
                    fr.write(Integer.toString(i + 1));
                    fr.write("\t");
                    fr.write(Helper.roundDouble(route.routeLength));
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

    public double bestFitness() {
        // Given that the population is already sorted.
        return this.population.get(0).fitness;
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
                if (icaf.maintainsFeasibility.get(i).get(k) && icaf.insertionCost.get(i).get(k) < bestInsertionCost) {
                    bestInsertionCost = icaf.insertionCost.get(i).get(k);
                    bestRoute = depot.routes.get(i);
                    bestInsertionIndex = k;
                }
            }
        }
        bestRoute.customers.add(bestInsertionIndex, customer);
        depot.recalculateUsedRouteLengthAndCapacity(bestRoute);
    }

    private void crossoverInsertCustomers(List<Customer> customersToAdd, Depot depotToModify) {
        for (Customer customer : customersToAdd) {
            if (ThreadLocalRandom.current().nextDouble() < this.crossoverInsertionNumber) {
                InsertionCostAndFeasibility icaf = getInsertionCostAndFeasibility(customer, depotToModify);
                if (icaf.maintainsFeasibility.stream().flatMap(List::stream).collect(Collectors.toList())
                        .contains(true)) {
                    // Insert at best feasible location
                    insertCustomerAtBestLocation(icaf, depotToModify, customer);
                } else {
                    // Create new route
                    Route route = new Route();
                    route.customers.add(customer);
                    depotToModify.routes.add(route);
                    depotToModify.recalculateUsedRouteLengthAndCapacity(route);
                }
            } else {
                // Insert at first entry in the list
                if (depotToModify.routes.isEmpty()) {
                    depotToModify.routes.add(0, new Route());
                }
                Route route = depotToModify.routes.get(0);
                route.customers.add(0, customer);
                depotToModify.recalculateUsedRouteLengthAndCapacity(route);
            }
        }
    }

    /**
     * Performs crossover between two chromosomes. Note that after crossover, the
     * affected depots in each chromosome can consist of illegal routes (e.g.,
     * routes that are too long, customers that are too far away, too heavy etc.).
     * 
     * @return an arrray of length 2 with the chromosome offsprings.
     */
    Chromosome[] crossover(Chromosome parent1, Chromosome parent2) {
        Chromosome offspring1 = new Chromosome(parent1);
        Chromosome offspring2 = new Chromosome(parent2);

        if (ThreadLocalRandom.current().nextDouble() < this.crossoverChance) {
            Depot depot1 = Helper.getRandomElementFromList(offspring1.depots);
            Depot depot2 = Helper.getRandomElementFromList(offspring2.depots);

            // These needs to be copied because we don't want them to change
            List<Customer> customers1 = depot1.routes.isEmpty() ? new ArrayList<>()
                    : new ArrayList<>(Helper.getRandomElementFromList(depot1.routes).customers);
            List<Customer> customers2 = depot2.routes.isEmpty() ? new ArrayList<>()
                    : new ArrayList<>(Helper.getRandomElementFromList(depot2.routes).customers);

            // Removes the customers from the chromosome
            for (Customer customer : customers1) {
                outer: for (Depot depot : offspring2.depots) {
                    for (Route route : depot.routes) {
                        if (route.customers.remove(customer)) {
                            depot.pruneEmtpyRoutes();
                            break outer;
                        }
                    }
                }
            }

            for (Customer customer : customers2) {
                outer: for (Depot depot : offspring1.depots) {
                    for (Route route : depot.routes) {
                        if (route.customers.remove(customer)) {
                            depot.pruneEmtpyRoutes();
                            break outer;
                        }
                    }
                }
            }

            // ? parallellize these
            crossoverInsertCustomers(customers1, depot2);
            crossoverInsertCustomers(customers2, depot1);
        }
        // If not crossover, we return a copy of the parents without modifications
        Chromosome[] offsprings = { offspring1, offspring2 };
        return offsprings;
    }

    void intraDepotMutation(Depot depot) {
        if (ThreadLocalRandom.current().nextDouble() >= this.intraDepotMutationRate) {
            return;
        }

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
        // This works on a single random route, not the entire depot
        if (depot.routes.isEmpty()) {
            return;
        }
        Route route = Helper.getRandomElementFromList(depot.routes);
        int startIndex = ThreadLocalRandom.current().nextInt(route.customers.size());
        int endIndex = startIndex + 1 + ThreadLocalRandom.current().nextInt(route.customers.size() - startIndex);
        Collections.reverse(route.customers.subList(startIndex, endIndex));
        depot.recalculateUsedRouteLengthAndCapacity(route);
    }

    void singleCustomerReRouting(Depot depot) {
        // Removes a random customer from a random route and places it at the best
        // possible location.
        if (depot.routes.isEmpty()) {
            return;
        }

        Route route = Helper.getRandomElementFromList(depot.routes);
        Customer customer = Helper.getRandomElementFromList(route.customers);
        route.customers.remove(customer);
        depot.pruneEmtpyRoutes();

        InsertionCostAndFeasibility icaf = getInsertionCostAndFeasibility(customer, depot);

        if (icaf.maintainsFeasibility.stream().flatMap(List::stream).collect(Collectors.toList()).contains(true)) {
            // Insert at best feasible location
            insertCustomerAtBestLocation(icaf, depot, customer);
        } else {
            // Create new route
            Route newRoute = new Route();
            newRoute.customers.add(customer);
            depot.routes.add(newRoute);
            depot.recalculateUsedRouteLengthAndCapacity(newRoute);
        }
    }

    void swapping(Depot depot) {
        // Swaps one customer-pair between two random routes in the depot.

        if (depot.routes.size() < 2) {
            return;
        }
        List<Route> routes = Helper.getNRandomElementsFromList(depot.routes, 2);
        Route route1 = routes.get(0);
        Route route2 = routes.get(1);

        Customer customer1 = Helper.getRandomElementFromList(route1.customers);
        Customer customer2 = Helper.getRandomElementFromList(route2.customers);

        int index1 = route1.customers.indexOf(customer1);
        int index2 = route2.customers.indexOf(customer2);

        route1.customers.remove(index1);
        route2.customers.remove(index2);

        route1.customers.add(index1, customer2);
        route2.customers.add(index2, customer1);

        depot.recalculateUsedRouteLengthAndCapacity(route1);
        depot.recalculateUsedRouteLengthAndCapacity(route2);
    }

    void interDepotMutation(Chromosome chromosome) {
        if (ThreadLocalRandom.current().nextDouble() >= this.interDepotMutationRate) {
            return;
        }
        List<Depot> depotsWithSwappableCustomers = chromosome.depots.stream()
                .filter(x -> x.getSwappableCustomers().size() > 0).collect(Collectors.toList());
        List<Depot> fullDepots = new ArrayList<>();
        for (Depot depot : depotsWithSwappableCustomers) {
            List<Customer> customersInDepot = new ArrayList<>();
            for (Route route : depot.routes) {
                customersInDepot.addAll(route.customers);
            }
            if (new HashSet<>(customersInDepot).containsAll(depot.getSwappableCustomers())) {
                // Already full depot, in the sense that it already has all customers it can
                // have
                fullDepots.add(depot);
            }
        }
        depotsWithSwappableCustomers.removeAll(fullDepots);
        if (depotsWithSwappableCustomers.isEmpty()) {
            return;
        }

        Depot toDepot = Helper.getRandomElementFromList(depotsWithSwappableCustomers);
        HashSet<Customer> possibleCustomersToGet = new HashSet<>(toDepot.getSwappableCustomers());
        List<Customer> customersInDepot = new ArrayList<>();
        for (Route route : toDepot.routes) {
            customersInDepot.addAll(route.customers);
        }
        possibleCustomersToGet.removeAll(customersInDepot);
        Customer customerToSwap = Helper.getRandomElementFromList(new ArrayList<>(possibleCustomersToGet));

        // Remove customerToSwap from the depot which contains it
        outer: for (Depot depot : chromosome.depots) {
            for (Route route : depot.routes) {
                if (route.customers.remove(customerToSwap)) {
                    depot.recalculateUsedRouteLengthAndCapacity(route);
                    depot.pruneEmtpyRoutes();
                    break outer;
                }
            }
        }

        InsertionCostAndFeasibility icaf = getInsertionCostAndFeasibility(customerToSwap, toDepot);

        if (icaf.maintainsFeasibility.stream().flatMap(List::stream).collect(Collectors.toList()).contains(true)) {
            // Insert at best feasible location
            insertCustomerAtBestLocation(icaf, toDepot, customerToSwap);
        } else {
            // Create new route
            Route newRoute = new Route();
            newRoute.customers.add(customerToSwap);
            toDepot.routes.add(newRoute);
            toDepot.recalculateUsedRouteLengthAndCapacity(newRoute);
        }
    }

    void elitism(List<Chromosome> newPopulation, int elitismCount) {
        // Randomly replace some % of the population with the best some % from
        // the parent population
        Collections.shuffle(newPopulation);
        Collections.sort(this.population, (a, b) -> Double.compare(a.fitness, b.fitness));
        for (int i = 0; i < elitismCount; i++) {
            newPopulation.set(i, this.population.get(i));
        }
        this.population = newPopulation;
    }

    public void runGA() {
        if (this.stopThreshold == Double.NEGATIVE_INFINITY) {
            System.out
                    .println(ConsoleColors.YELLOW + "Running GA without a threshold stop value." + ConsoleColors.RESET);
        }

        final int elitismCount = (int) Math.round((double) this.population.size() * this.eliteRatio);
        if (elitismCount == 0) {
            System.out.println(ConsoleColors.YELLOW + "Warning: elitism is not applied." + ConsoleColors.RESET);
        }

        if (this.verbose) {
            System.out.println("Population size: " + this.population.size());
            System.out.println("This many legal init chromosomes: "
                    + this.population.stream().filter(x -> x.tooManyRoutes == 0).count());
        }

        for (int generation = 0; generation < this.maxGeneration; generation++) {
            List<Chromosome> newPopulationSync = Collections.synchronizedList(new ArrayList<>());

            AtomicCounter customerPairsLeft = new AtomicCounter(this.population.size() / 2);

            List<Thread> threads = new ArrayList<>();

            final boolean interDepot = generation % this.apprate == 0;
            final int g = generation;
            for (int i = 0; i < 24; i++) {
                threads.add(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (customerPairsLeft.value() > 0) {
                            customerPairsLeft.decrement();
                            Chromosome[] parents = tournamentSelection(2); // Note that these are not copies
                            Chromosome[] offsprings = crossover(parents[0], parents[1]);
                            if (interDepot) {
                                // Apply inter-depot mutation every 10th generation for example
                                // ? parallellize
                                interDepotMutation(offsprings[0]);
                                interDepotMutation(offsprings[1]);
                            } else {
                                // Intra-depot mutation
                                // Selects a random depot to perform mutation on
                                intraDepotMutation(Helper.getRandomElementFromList(offsprings[0].depots));
                                intraDepotMutation(Helper.getRandomElementFromList(offsprings[1].depots));
                            }

                            offsprings[0].getLegality(maxVehicesPerDepot);
                            offsprings[1].getLegality(maxVehicesPerDepot);

                            offsprings[0].updateFitnessByTotalDistanceWithPenalty(g);
                            offsprings[1].updateFitnessByTotalDistanceWithPenalty(g);

                            newPopulationSync.add(offsprings[0]);
                            newPopulationSync.add(offsprings[1]);
                        }
                    }
                }));
            }

            for (Thread t : threads) {
                t.start();
            }

            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            List<Chromosome> newPopulation = new ArrayList<>(newPopulationSync);

            // Avoids some funky race condition. Can be fixed by adding a proper
            // read-and-set-if thread safe variable
            newPopulation = newPopulation.subList(0, this.population.size());

            elitism(newPopulation, elitismCount);

            if (generation % this.saveInterval == 0 && generation > 0) {
                this.saveBest();
            }

            // Run every 50th time for speedup
            if (generation % 100 == 0 && generation > 0) {
                double bestLegalFitness = Double.POSITIVE_INFINITY;
                double averageFitness = 0.0;
                for (Chromosome chromosome : this.population) {
                    averageFitness += chromosome.fitness;
                    // We only measure the legal individuals in the population
                    if (chromosome.fitness < bestLegalFitness && chromosome.tooManyRoutes == 0) {
                        bestLegalFitness = chromosome.fitness;
                    }
                }
                if (bestLegalFitness <= this.stopThreshold) {
                    System.out.println(
                            ConsoleColors.GREEN + "\nEarly stopped at generation: " + generation + ConsoleColors.RESET);
                    return;
                }
                if (this.verbose) {
                    System.out.println("Generation: " + generation + ", Best fitness: "
                            + Helper.roundDouble(bestLegalFitness) + ", average fitness: "
                            + Helper.roundDouble(averageFitness / (double) this.population.size()));
                } else {
                    System.out.print("\rGeneration: " + generation + ", Best fitness: "
                            + Helper.roundDouble(bestLegalFitness) + ", average fitness: "
                            + Helper.roundDouble(averageFitness / (double) this.population.size()));
                }
            }

            // // ! Test start
            // for (Chromosome chromosome : this.population) {
            // // Set<Customer> customers = new HashSet<>();
            // Set<Customer> customersInRoutes = new HashSet<>();
            // for (Depot depot : chromosome.depots) {
            // for (Route route : depot.routes) {
            // for (Customer c : route.customers) {
            // if (customersInRoutes.contains(c)) {
            // // System.err.println("Duplicate customer in route...");
            // throw new Error("Duplicate customer in route...");
            // } else {
            // customersInRoutes.add(c);
            // }
            // }
            // }
            // // for (Customer customer : depot.customers) {
            // // if (customers.contains(customer)) {
            // // // System.err.println("Duplicate customer...");
            // // throw new Error("Duplicate customer...");
            // // } else {
            // // customers.add(customer);
            // // }
            // // }
            // }
            // if (customersInRoutes.size() != this.customerCount) {
            // System.err.println("Invalid customer count");
            // System.err.println(this.customerCount);
            // // System.err.println(customers.size());
            // System.err.println(customersInRoutes.size());
            // throw new Error();
            // }
            // }
            // // ! Test end
        }
    }

    @Override
    public String toString() {
        return Helper.getClassValuesAsString(this);
    }
}
