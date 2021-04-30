package moea;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.awt.image.BufferedImage;

public class SGA implements Runnable {
    final private BufferedImage image;
    final private int N; // Number of pixels in the image

    // Config arguments
    final private int populationSize;
    final private int maxGeneration;
    final private double crossoverProbability;
    final private double mutationProbability;
    final private int tournamentSize;
    final private int sgaElitismCount;
    final private boolean verbose;
    final private int lowerSegmentationCountLimit;
    final private int upperSegmentationCountLimit;

    final private double[][] edgeValues;
    // TODO maybe create a Pixel class which holds the x, y, RGB and segment ID ?
    final private int[][] neighborArrays;
    List<Chromosome> population;

    final FeedbackStation feedbackStation;

    public SGA(ConfigParser configParser, BufferedImage image, FeedbackStation feedbackStation) {
        System.out.println("Running simple genetic algorithm");
        this.image = image;
        this.N = image.getHeight() * image.getWidth();
        this.populationSize = configParser.populationSize;
        this.maxGeneration = configParser.maxGeneration;
        this.crossoverProbability = configParser.crossoverProbability;
        this.mutationProbability = configParser.mutationProbability;
        this.tournamentSize = configParser.tournamentSize;
        this.sgaElitismCount = configParser.sgaElitismCount;
        this.verbose = configParser.verbose;
        this.lowerSegmentationCountLimit = configParser.lowerSegmentationCountLimit;
        this.upperSegmentationCountLimit = configParser.upperSegmentationCountLimit;

        this.neighborArrays = Utils.constructNeighborArray(this.image.getWidth(), this.image.getHeight());
        this.edgeValues = Utils.constructEdgeValues(this.image);

        this.feedbackStation = feedbackStation;

        this.population = Utils.initPopulationByMinimumSpanningTree(this.populationSize,
                new MST(edgeValues, neighborArrays), this.N);

        // Maybe thread this
        for (int i = 0; i < this.population.size(); i++) {
            Chromosome chromosome = this.population.get(i);
            chromosome.recalculateObjectives(this.N, this.neighborArrays, this.image);

            BufferedImage[] images = Utils.createImagesFromChromosome(chromosome, this.image, this.neighborArrays);
            BufferedImage type1Image = images[0];

            Utils.saveImage(type1Image, Integer.toString(i), "initial_images");
        }
    }

    @Override
    public void run() {
        runGA();
    }

    private double weightedSumFitness(Chromosome c) {
        // TODO adjust the weighting parameters
        return 0.01 * c.edgeValue + 1.0 * c.connectivityMeasure + 0.001 * c.overallDeviation;
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
            tournamentSet.sort(Comparator.comparing(c -> weightedSumFitness(c)));
            winners[i] = tournamentSet.get(0);
        }
        return winners;
    }

    private void elitism(List<Chromosome> oldPopulation, List<Chromosome> newPopulation, int elitismCount) {
        // Randomly replace some % of the population with the best some % from
        // the parent population
        Collections.shuffle(newPopulation);
        this.population.sort(Comparator.comparing(c -> weightedSumFitness(c)));
        for (int i = 0; i < elitismCount; i++) {
            newPopulation.set(i, this.population.get(i));
        }
    }

    public void runGA() {
        for (int generation = 0; generation < this.maxGeneration; generation++) {
            System.out.println("Generation: " + generation);
            List<Chromosome> newPopulation = new ArrayList<>(this.population.size());

            while (newPopulation.size() < this.population.size()) {
                Chromosome[] parents = tournamentSelectionSGA(2);
                Chromosome[] offsprings = Utils.crossover(parents[0], parents[1], this.crossoverProbability);
                for (Chromosome chromosome : offsprings) {
                    Utils.mutation(chromosome, this.mutationProbability);
                }

                // Recalculate indexToSegmentIds and fitness
                for (Chromosome chromosome : offsprings) {
                    chromosome.recalculateObjectives(this.N, this.neighborArrays, this.image);
                }

                newPopulation.addAll(Arrays.asList(offsprings));
            }

            elitism(this.population, newPopulation, this.sgaElitismCount);

            // Set the current population to this new, hopefully better, population
            this.population = newPopulation;

            Chromosome bestChromosome = this.population.get(0);
            for (Chromosome chromosome : this.population) {
                if (weightedSumFitness(chromosome) < weightedSumFitness(bestChromosome)) {
                    bestChromosome = chromosome;
                }
            }
            System.out.println(
                    "Edge value: " + this.population.stream().mapToDouble(c -> c.edgeValue).summaryStatistics());
            System.out.println("Connectivity measure: "
                    + this.population.stream().mapToDouble(c -> c.connectivityMeasure).summaryStatistics());
            System.out.println("Overall deviation: "
                    + this.population.stream().mapToDouble(c -> c.overallDeviation).summaryStatistics());
            System.out
                    .println("Segments: " + this.population.stream().mapToDouble(c -> c.segments).summaryStatistics());

            assert (this.population.size() == this.populationSize); // TODO temp

            // Save all images after every generation
            for (int i = 0; i < this.population.size(); i++) {
                Chromosome chromosome = this.population.get(i);
                if (chromosome.segments >= this.lowerSegmentationCountLimit
                        && chromosome.segments <= this.upperSegmentationCountLimit) {
                    BufferedImage[] images = Utils.createImagesFromChromosome(chromosome, this.image,
                            this.neighborArrays);
                    // TODO put this path as a constant
                    Utils.saveImage(images[0], "type_1_" + i, "generation_images", "generation_" + generation,
                            "type_1");
                    Utils.saveImage(images[1], "type_2_" + i, "generation_images", "generation_" + generation,
                            "type_2");
                }
            }

            if (this.feedbackStation.stop) {
                break;
            }

            try {
                // System.out.println("Wanting to put solution location: " +
                // "output_images/generation_images/generation_"
                // + generation);
                this.feedbackStation.solutionLocations
                        .put("output_images/generation_images/generation_" + generation + "/type_1");
                // System.out.println(
                // "Did put solution location: " + "output_images/generation_images/generation_"
                // + generation);
            } catch (InterruptedException e) {
                System.out.println("NSGA was interrupted");
                break;
            }
        }
        this.feedbackStation.stop = true;
        System.out.println("SGA finished");
    }
}
