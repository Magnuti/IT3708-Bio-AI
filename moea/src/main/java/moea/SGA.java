package moea;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.awt.image.BufferedImage;

public class SGA {
    final private BufferedImage image;
    final private int N; // Number of pixels in the image

    // Config arguments
    final private int maxGeneration;
    final private double stopThreshold;
    final private double crossoverProbability;
    final private double mutationProbability;
    final private int tournamentSize;
    final private int sgaElitismCount;
    final private boolean verbose;
    final private int saveInterval;

    final private double[][] edgeValues;
    // TODO maybe create a Pixel class which holds the x, y, RGB and segment ID ?
    final private int[][] neighborArrays;
    List<Chromosome> population;

    public SGA(ConfigParser configParser, BufferedImage image) {
        this.image = image;
        this.N = image.getHeight() * image.getWidth();
        this.maxGeneration = configParser.maxGeneration;
        this.stopThreshold = configParser.stopThreshold;
        this.crossoverProbability = configParser.crossoverProbability;
        this.mutationProbability = configParser.mutationProbability;
        this.tournamentSize = configParser.tournamentSize;
        this.sgaElitismCount = configParser.sgaElitismCount;
        this.verbose = configParser.verbose;
        this.saveInterval = configParser.saveInterval;

        // The population consists of several chromosomes
        this.population = new ArrayList<>();

        this.neighborArrays = Utils.constructNeighborArray(this.image.getWidth(), this.image.getHeight());
        this.edgeValues = Utils.constructEdgeValues(this.image);

        Utils.initRandomPopulation(this.population, configParser.populationSize, this.N);

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

    private double weightedSumFitness(double edgeValue, double connectivityMeasure, double overallDeviation) {
        // TODO adjust the weighting parameters
        return 1.0 * edgeValue + 1.0 * connectivityMeasure + 0.001 * overallDeviation;
    }

    /**
     * Tournament selection based on fitness.
     */
    Chromosome[] tournamentSelectionSGA(int selection_size) {
        Chromosome[] winners = new Chromosome[selection_size];
        for (int i = 0; i < selection_size; i++) {
            // selection_size number of tournaments
            List<Chromosome> tournamentSet = new ArrayList<>(
                    Helper.getNRandomElementsFromList(this.population, this.tournamentSize));

            // Select the best parent without any chance to select a random winner. This
            // differs from the MDVRP project.
            tournamentSet.sort(Comparator.comparing(c -> c.fitness));
            winners[i] = tournamentSet.get(0);
        }
        return winners;
    }

    private void elitism(List<Chromosome> oldPopulation, List<Chromosome> newPopulation, int elitismCount) {
        // Randomly replace some % of the population with the best some % from
        // the parent population
        Collections.shuffle(newPopulation);
        this.population.sort(Comparator.comparing(c -> c.fitness));
        for (int i = 0; i < elitismCount; i++) {
            newPopulation.set(i, this.population.get(i));
        }
    }

    public void runGA() {
        for (int generation = 0; generation < this.maxGeneration; generation++) {
            List<Chromosome> newPopulation = new ArrayList<>(this.population.size());

            while (newPopulation.size() < this.population.size()) {
                Chromosome[] parents = tournamentSelectionSGA(2);
                Chromosome[] offsprings = crossover(parents[0], parents[1]);
                for (Chromosome chromosome : offsprings) {
                    mutation(chromosome);
                }

                // Recalculate indexToSegmentIds and fitness
                for (Chromosome chromosome : offsprings) {
                    chromosome.indexToSegmentIds = genotypeToPhenotype(chromosome);

                    double edgeValue = edgeValue(chromosome.indexToSegmentIds);
                    double connectivityMeasure = connectivityMeasure(chromosome.indexToSegmentIds);
                    double overallDeviation = overallDeviation(chromosome.indexToSegmentIds);

                    chromosome.fitness = weightedSumFitness(edgeValue, connectivityMeasure, overallDeviation);
                }

                newPopulation.addAll(Arrays.asList(offsprings));
            }

            elitism(this.population, newPopulation, this.sgaElitismCount);

            // Set the current population to this new, hopefully better, population
            this.population = newPopulation;

            Chromosome bestChromosome = this.population.get(0);
            for (Chromosome chromosome : this.population) {
                if (chromosome.fitness < bestChromosome.fitness) {
                    bestChromosome = chromosome;
                }
            }
            System.out.println("Best fitness:" + bestChromosome.fitness);
            double edgeValue = edgeValue(bestChromosome.indexToSegmentIds);
            double connectivityMeasure = connectivityMeasure(bestChromosome.indexToSegmentIds);
            double overallDeviation = overallDeviation(bestChromosome.indexToSegmentIds);
            System.out.println("\tEdge value: " + edgeValue);
            System.out.println("\tConnectivity measure: " + connectivityMeasure);
            System.out.println("\tOverall deviation: " + overallDeviation);

        }

        this.population.sort(Comparator.comparing(c -> c.fitness));
        Chromosome bestChromosome = this.population.get(0);
        BufferedImage bufferedImage = createBufferedImageFromChromosome(bestChromosome);

        saveImage(bufferedImage, "best_one");
    }
}
