package mdvrp;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class Solver {
    int maxVehicesPerDepot;
    int customerCount;
    int depotCount;
    List<Depot> depots;
    List<Customer> customers;
    List<Chromosome> population = new ArrayList<>();

    public Solver(ProblemParser problemParser) {
        this.maxVehicesPerDepot = problemParser.maxVehicesPerDepot;
        this.customerCount = problemParser.customerCount;
        this.depotCount = problemParser.depotCount;
        this.depots = problemParser.depots;
        this.customers = problemParser.customers;
    }

    double euclidianDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
    }

    public void initDepotAssignment() {
        // TODO parallellize this
        for (Customer customer : this.customers) {
            double lowestDistance = Double.POSITIVE_INFINITY;
            Depot bestDepot = null;
            for (Depot depot : this.depots) {
                double distance = euclidianDistance(depot.getX(), depot.getY(), customer.getX(), customer.getY());
                if (distance < lowestDistance) {
                    lowestDistance = distance;
                    bestDepot = depot;
                }
            }
            bestDepot.customers.add(customer);

            // TODO add borderline cases as described in section 3.9
        }
    }

    public void initPopulation(int populationSize) {
        if (populationSize % 2 == 1) {
            System.out.println(
                    "Warning: Please keep the population size as an even number. Why? Because two parents can reproduce easily, while three is more difficult.");
            populationSize = populationSize - 1;
            System.out.println("Using a population size of: " + populationSize);
        }
        for (int i = 0; i < populationSize; i++) {
            // We need to clone depots to the different chromosomes
            List<Depot> depotsCopy = new ArrayList<>();
            for (Depot depot : this.depots) {
                depotsCopy.add(new Depot(depot));
            }
            Chromosome chromosome = new Chromosome(depotsCopy);
            chromosome.routeSchedulingFirstPart();
            this.population.add(chromosome);
        }
    }

    public void saveBest() {
        // TODO save the best from this.population
        List<Depot> depots = this.population.get(0).depots; // TODO

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

    public List<Chromosome> tournamentSelection(int selection_size) {
        List<Chromosome> winners = new ArrayList<>();
        int k = 2; // Binary tournament
        for (int i = 0; i < selection_size; i++) {
            List<Chromosome> tournamentSet = new ArrayList<>();
            Random rand = new Random();
            for (int j = 0; j < k; j++) {
                int index = rand.nextInt(population.size());
                tournamentSet.add(this.population.get(index));
            }

            double r = rand.nextDouble();
            if (r < 0.8) {
                // Add most fit parent
                // ? Should maybe sort here to enable k > 2, but it may affect performance
                if (tournamentSet.get(0).fitness >= tournamentSet.get(1).fitness) {
                    winners.add(tournamentSet.get(0));
                } else {
                    winners.add(tournamentSet.get(1));
                }
            } else {
                // Add random parent
                int index = rand.nextInt(tournamentSet.size());
                winners.add(tournamentSet.get(index));
            }
        }

        // TODO add elitism

        return winners;
    }

    public List<Chromosome> crossover(Chromosome parent1, Chromosome parent2) {
        // Returns 2 offspring ?
        // TODO
        List<Chromosome> offsprings = new ArrayList<>();
        offsprings.add(parent1);
        offsprings.add(parent2);
        return offsprings;
    }

    public List<Chromosome> elitism(List<Chromosome> newPopulation, double ratio) {
        // TODO replace some of the chromosones in offsprings with some of the fittest
        // from this.population
        return newPopulation;
    }

    public void createNewPopulation() {
        List<Chromosome> newPopulation = new ArrayList<>();
        for (int i = 0; i < this.population.size() / 2; i++) {
            List<Chromosome> parents = tournamentSelection(2);
            List<Chromosome> offsprings = crossover(parents.get(0), parents.get(1));
            for (Chromosome offspring : offsprings) {
                offspring.mutate();
            }
            newPopulation.add(offsprings.get(0));
            newPopulation.add(offsprings.get(1));
        }
        newPopulation = elitism(newPopulation, 0.01);
        this.population = newPopulation;
    }

    public void runGA(int maxGeneration) {
        // for (Chromosome chromosome : this.population) {
        // chromosome.updateFitnessByWeightedSum();
        // System.out.println(chromosome.fitness);
        // }
        for (int i = 0; i < maxGeneration; i++) {
            System.out.println(this.population.size());
            createNewPopulation();
            double bestFitness = 0.0;
            for (Chromosome chromosome : this.population) {
                chromosome.updateFitnessByWeightedSum();
                System.out.println(chromosome.fitness);
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
