package moea;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

import moea.App.PixelDirection;

public class NSGA2 {

    final private BufferedImage image;
    final private int N; // Number of pixels in the image

    // Config arguments
    final private int maxGeneration;
    final private double stopThreshold;
    final private double crossoverProbability;
    final private double mutationProbability;
    final private int tournamentSize;
    final private boolean verbose;
    final private int saveInterval;

    final private double[][] edgeValues;
    // TODO maybe create a Pixel class which holds the x, y, RGB and segment ID ?
    final private int[][] neighborArrays;
    List<Chromosome> population;

    public NSGA2(ConfigParser configParser, BufferedImage image) {
        this.image = image;
        this.N = image.getHeight() * image.getWidth();
        this.maxGeneration = configParser.maxGeneration;
        this.stopThreshold = configParser.stopThreshold;
        this.crossoverProbability = configParser.crossoverProbability;
        this.mutationProbability = configParser.mutationProbability;
        this.tournamentSize = configParser.tournamentSize;
        this.verbose = configParser.verbose;
        this.saveInterval = configParser.saveInterval;

        // The population consists of several chromosomes
        this.population = new ArrayList<>();

        this.neighborArrays = Utils.constructNeighborArray(this.image.getWidth(), this.image.getHeight());
        this.edgeValues = Utils.constructEdgeValues(this.image);

        MST mst = new MST(edgeValues, neighborArrays);
        initPopulationByMinimumSpanningTree(configParser.populationSize, mst);
        // initRandomPopulation(configParser.populationSize);

        // TODO multi-thread
        for (int i = 0; i < this.population.size(); i++) {
            System.out.println("Chromosome: " + i);
            Chromosome chromosome = this.population.get(i);
            chromosome.calculateIndexToSegmentIds(this.N, this.neighborArrays);

            chromosome.calculateEdgeValue(this.neighborArrays, this.image);
            chromosome.calculateConnectivityMeasure(this.neighborArrays, this.image);
            chromosome.calculateOverallDeviation(this.image);
            System.out.println("Edge value: " + chromosome.edgeValue);
            System.out.println("Connectivity measure: " + chromosome.connectivityMeasure);
            System.out.println("Overall deviation: " + chromosome.overallDeviation);

            BufferedImage bufferedImage = Utils.createBufferedImageFromChromosome(chromosome, this.image.getWidth(),
                    this.image.getHeight());

            Utils.saveImage(bufferedImage, Integer.toString(i));
        }
    }

    void initPopulationByMinimumSpanningTree(int populationSize, MST mst) {
        for (int i = 0; i < populationSize; i++) {
            System.out.println("Init poopulation for chromosome: " + i);
            int startingNode = ThreadLocalRandom.current().nextInt(this.N);
            PixelDirection[] pixelDirections = mst.findDirections(startingNode);
            Chromosome chromosome = new Chromosome(pixelDirections);
            this.population.add(chromosome);
        }
    }

    /**
     * Return the winners of the crowding tournament. The winners are the one with
     * the best/lowest rank or the one with the best/highest crowding distance
     * compared to an individual with equal rank. Returns the first element if all
     * are equal.
     */
    private Chromosome[] crowdingTournamentSelection(int selection_size, Map<Chromosome, Double> crowdingDistances,
            Map<Chromosome, Integer> ranks) {
        Chromosome[] winners = new Chromosome[selection_size];
        for (int i = 0; i < selection_size; i++) {
            // selection_size number of tournaments
            List<Chromosome> tournamentSet = new ArrayList<>(
                    Helper.getNRandomElementsFromList(this.population, this.tournamentSize));

            Chromosome bestChromosome = tournamentSet.get(0);
            for (Chromosome c : tournamentSet.subList(1, tournamentSet.size())) {
                // Loop all except the first one
                if (ranks.get(c) < ranks.get(bestChromosome) || (ranks.get(c) == ranks.get(bestChromosome)
                        && crowdingDistances.get(c) > crowdingDistances.get(bestChromosome))) {
                    bestChromosome = c;
                }
            }
            winners[i] = bestChromosome;
        }
        return winners;
    }

    /**
     * Returns true if c1 dominates c2.
     */
    boolean dominates(Chromosome c1, Chromosome c2) {
        if (c1.edgeValue < c2.edgeValue && c1.connectivityMeasure < c2.connectivityMeasure
                && c1.overallDeviation < c2.overallDeviation) {
            return true;
        }
        return false;
    }

    /**
     * Used to find the ranks of each individual.
     */
    void fastNonDominatedSort(List<Chromosome> population, Map<Chromosome, Integer> ranks,
            Map<Integer, Set<Chromosome>> F) {
        // Page 3 in https://ieeexplore.ieee.org/document/996017
        Map<Chromosome, Set<Chromosome>> S = new HashMap<>();
        Map<Chromosome, Integer> n = new HashMap<>();

        F.put(1, new HashSet<>());

        for (Chromosome p : population) {
            S.put(p, new HashSet<>());
            n.put(p, 0);
            for (Chromosome q : population) {
                if (dominates(p, q)) {
                    // p dominates q
                    // Add q to the set of solutions dominated by p
                    S.get(p).add(q);
                } else if (dominates(q, p)) {
                    // q dominates p
                    // Increment the domination counter of p
                    n.put(p, n.get(p) + 1);
                }
            }
            if (n.get(p) == 0) {
                ranks.put(p, 1);
                F.get(1).add(p);
            }
        }

        int i = 1;
        while (!F.get(i).isEmpty()) {
            Set<Chromosome> Q = new HashSet<>();
            for (Chromosome p : F.get(i)) {
                for (Chromosome q : S.get(p)) {
                    n.put(q, n.get(q) - 1);
                    if (n.get(q) == 0) {
                        ranks.put(q, i + 1);
                        Q.add(q);
                    }
                }
            }
            i++;
            F.put(i, Q);
        }
    }

    /**
     * Used to find the crowding distances among individuals with the same rank.
     * 
     * @param sameRankPopulation
     * @param distances
     */
    void findCrowdingDistances(List<Chromosome> sameRankPopulation, Map<Chromosome, Double> distances) {
        for (Chromosome c : sameRankPopulation) {
            distances.put(c, 0.0);
        }

        // Loop the objectives
        for (int objectiveFunction = 0; objectiveFunction < 3; objectiveFunction++) {
            // Sort using each objective value, note that the sort is different for every
            // iteration of the loop.
            double[] objectiveValues;
            if (objectiveFunction == 0) {
                sameRankPopulation.sort(Comparator.comparing(c -> c.edgeValue));
                objectiveValues = sameRankPopulation.stream().mapToDouble(c -> c.edgeValue).toArray();
            } else if (objectiveFunction == 1) {
                sameRankPopulation.sort(Comparator.comparing(c -> c.connectivityMeasure));
                objectiveValues = sameRankPopulation.stream().mapToDouble(c -> c.connectivityMeasure).toArray();
            } else {
                sameRankPopulation.sort(Comparator.comparing(c -> c.overallDeviation));
                objectiveValues = sameRankPopulation.stream().mapToDouble(c -> c.overallDeviation).toArray();
            }

            // Assign a large value to the boundary solutions with respect to this objective
            // function such that boundary points are always selected.
            distances.put(sameRankPopulation.get(0), Double.POSITIVE_INFINITY);
            distances.put(sameRankPopulation.get(sameRankPopulation.size() - 1), Double.POSITIVE_INFINITY);

            double maxF = Arrays.stream(objectiveValues).max().getAsDouble();
            double minF = Arrays.stream(objectiveValues).min().getAsDouble();
            if (maxF == minF) {
                // So we don't divide by zero when the population have entirely equal objective
                // function values. This happens when the population is initialized since both
                // edge value and connectivity measure is 0.0 due to the MST. This also happens
                // some times in the early stages of the GA.
                maxF += 1e-5;
            }

            // For all other points we assign this
            for (int i = 1; i < sameRankPopulation.size() - 1; i++) {
                Chromosome c = sameRankPopulation.get(i);
                double newDistance = distances.get(c)
                        + (objectiveValues[i + 1] - objectiveValues[i - 1]) / (maxF - minF);
                distances.put(c, newDistance);
            }
        }
    }

    void runGA() {
        Map<Chromosome, Integer> ranks = new HashMap<>();
        Map<Integer, Set<Chromosome>> F = new HashMap<>();
        fastNonDominatedSort(this.population, ranks, F);

        for (Integer f : F.keySet()) {
            if (F.get(f).isEmpty()) {
                // On init the last segment is always empty. This is a temporarily patch, may
                // fix later.
                System.out.println(ConsoleColors.YELLOW + "Removed empty set from F." + ConsoleColors.RESET);
                F.remove(f);
            }
        }

        Map<Chromosome, Double> crowdingDistances = new HashMap<>();
        for (Integer i : F.keySet()) {
            System.out.println("Init crowding distance for rank " + i + " with size " + F.get(i).size());
            findCrowdingDistances(new ArrayList<>(F.get(i)), crowdingDistances);
        }

        // for (Chromosome c : this.population) {
        // System.out.println(c);
        // System.out.println("Rank: " + ranks.get(c));
        // System.out.println("Distance: " + crowdingDistances.get(c));
        // }

        for (int generation = 0; generation < this.maxGeneration; generation++) {
            System.out.println("Generation: " + generation);
            List<Chromosome> newPopulation = new ArrayList<>(this.population.size() * 2);

            while (newPopulation.size() < this.population.size()) {
                // Select two parents
                Chromosome[] parents = crowdingTournamentSelection(2, crowdingDistances, ranks);
                Chromosome[] offsprings = Utils.crossover(parents[0], parents[1], this.crossoverProbability);
                for (Chromosome chromosome : offsprings) {
                    Utils.mutation(chromosome, this.mutationProbability);
                }

                // Recalculate indexToSegmentIds and fitness
                for (Chromosome chromosome : offsprings) {
                    chromosome.calculateIndexToSegmentIds(this.N, this.neighborArrays);
                    chromosome.calculateEdgeValue(this.neighborArrays, this.image);
                    chromosome.calculateConnectivityMeasure(this.neighborArrays, this.image);
                    chromosome.calculateOverallDeviation(this.image);
                }

                newPopulation.addAll(Arrays.asList(offsprings));
            }

            newPopulation.addAll(this.population);
            // The population now consists of all the parents and all the offsprings.

            // First combine, then sort on rank, then sort on cr. distance. We need to
            // combine the populations then do sorting.
            ranks.clear();
            F.clear();
            fastNonDominatedSort(newPopulation, ranks, F);

            for (Integer f : F.keySet()) {
                if (F.get(f).isEmpty()) {
                    // On init the last segment is always empty. This is a temporarily patch, may
                    // fix later.
                    System.out.println(ConsoleColors.YELLOW + "Removed empty set from F." + ConsoleColors.RESET);
                    F.remove(f);
                }
            }

            // Need to clear since the ranks are changed, thus we need to recalculate the
            // distances because they are based on ranks.
            crowdingDistances.clear();
            for (Integer i : F.keySet()) {
                System.out.println("Init crowding distance for rank:" + i);
                System.out.println(F.get(i).size());
                findCrowdingDistances(new ArrayList<>(F.get(i)), crowdingDistances);
            }

            // Set the population of the next generation
            int popSize = this.population.size(); // TODO make a final at the top
            this.population.clear();
            for (Integer f : F.keySet()) {
                if (F.get(f).size() <= popSize - this.population.size()) {
                    // Add all individuals in this rank because we can
                    this.population.addAll(F.get(f));
                } else {
                    // Add some of the individuals from this rank
                    List<Chromosome> sameRankPopulation = new ArrayList<>(F.get(f));

                    // TODO look over this. Is it correct to pick the ones with highest distance?
                    sameRankPopulation.sort(Comparator.comparing(c -> crowdingDistances.get(c)));
                    // We want the ones with the highest crowding distances
                    Collections.reverse(sameRankPopulation);
                    // The population is now full
                    this.population.addAll(sameRankPopulation.subList(0, popSize - this.population.size()));
                    break;
                }
            }

            System.out.println(
                    "Edge value: " + this.population.stream().mapToDouble(c -> c.edgeValue).summaryStatistics());
            System.out.println("Connectivity measure: "
                    + this.population.stream().mapToDouble(c -> c.connectivityMeasure).summaryStatistics());
            System.out.println("Overall deviation: "
                    + this.population.stream().mapToDouble(c -> c.overallDeviation).summaryStatistics());

            assert (this.population.size() == popSize); // TODO temp
        }

        // Save all images once the GA is finished
        for (int i = 0; i < this.population.size(); i++) {
            BufferedImage bufferedImage = Utils.createBufferedImageFromChromosome(this.population.get(i),
                    this.image.getWidth(), this.image.getHeight());
            Utils.saveImage(bufferedImage, "last_" + i);
        }
    }
}
