package moea;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.awt.Color;
import javax.imageio.ImageIO;

import moea.App.PixelDirection;

public class Solver {

    final private BufferedImage image;
    final private int N; // Number of pixels in the image
    final private int maxGeneration;
    final private double[][] edgeValues;

    // TODO maybe create a Pixel class which holds the x, y, RGB and segment ID ?
    final private int[][] neighborArrays;
    List<Chromosome> population;

    public Solver(ConfigParser configParser, BufferedImage image) {
        this.image = image;
        this.N = image.getHeight() * image.getWidth();
        this.maxGeneration = configParser.maxGeneration;

        // The population consists of several chromosomes
        this.population = new ArrayList<>();

        this.neighborArrays = constructNeighborArray();
        this.edgeValues = constructEdgeValues();

        MST mst = new MST(edgeValues, neighborArrays);
        initPopulationByMinimumSpanningTree(configParser.populationSize, mst);
        // initRandomPopulation(configParser.populationSize);

        // TODO multi-thread
        for (int i = 0; i < this.population.size(); i++) {
            System.out.println("Chromosome: " + i);
            Chromosome chromosome = this.population.get(i);
            chromosome.indexToSegmentIds = genotypeToPhenotype(chromosome);

            double edgeValue = edgeValue(chromosome.indexToSegmentIds);
            System.out.println("Edge value: " + edgeValue);
            double connectivityMeasure = connectivityMeasure(chromosome.indexToSegmentIds);
            System.out.println("Connectivity measure: " + connectivityMeasure);
            double overallDeviation = overallDeviation(chromosome.indexToSegmentIds);
            System.out.println("Overall deviation: " + overallDeviation);

            // TODO move somewhere else
            chromosome.fitness = weightedSumFitness(edgeValue, connectivityMeasure, overallDeviation);

            BufferedImage bufferedImage = new BufferedImage(this.image.getWidth(), this.image.getHeight(),
                    // BufferedImage.TYPE_BYTE_BINARY);
                    BufferedImage.TYPE_INT_RGB);

            final int WHITE = new Color(255, 255, 255).getRGB();
            final int BLACK = new Color(0, 0, 0).getRGB();

            // for (int y = 0; y < this.image.getHeight(); y++) {
            // for (int x = 0; x < this.image.getWidth(); x++) {
            // bufferedImage.setRGB(x, y, WHITE);
            // }
            // }

            Map<Integer, Color> colors = new HashMap<>();
            for (int p = 0; p < chromosome.indexToSegmentIds.length; p++) {
                if (!colors.containsKey(chromosome.indexToSegmentIds[p])) {
                    float r = ThreadLocalRandom.current().nextFloat();
                    float g = ThreadLocalRandom.current().nextFloat();
                    float b = ThreadLocalRandom.current().nextFloat();
                    colors.put(chromosome.indexToSegmentIds[p], new Color(r, g, b));
                }
            }

            System.out.println("Segments " + colors.keySet().size());
            System.out.println();

            for (int p = 0; p < this.N; p++) {
                int y = p / this.image.getWidth();
                int x = p % this.image.getWidth();
                bufferedImage.setRGB(x, y, colors.get(chromosome.indexToSegmentIds[p]).getRGB());
            }

            // for (int pixel = 0; pixel < this.N; pixel++) {
            // for (int neighborIndex = 1; neighborIndex <
            // this.neighborArrays[pixel].length; neighborIndex++) {
            // int neighborPixel = this.neighborArrays[pixel][neighborIndex];
            // if (neighborPixel == -1) {
            // continue;
            // }
            // if (indexToSegmentIds[pixel] != indexToSegmentIds[neighborPixel]) {
            // int y = pixel / this.image.getWidth();
            // int x = pixel % this.image.getWidth();
            // bufferedImage.setRGB(x, y, BLACK);
            // break; // No need to check the rest of the neighbors for this pixel
            // }
            // }
            // }

            saveImage(bufferedImage, Integer.toString(i));
        }
    }

    void saveImage(BufferedImage bufferedImage, String name) {
        File image_path = new File("output_images");
        if (!image_path.exists()) {
            image_path.mkdir();
        }
        Path path = Paths.get(image_path.getPath(), name + ".jpg");
        try {
            ImageIO.write(bufferedImage, "jpg", new File(path.toString()));
        } catch (IOException e) {
            e.printStackTrace();
            throw new Error("Cannot save the image: " + path.toString());
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

    void initRandomPopulation(int populationSize) {
        for (int i = 0; i < populationSize; i++) {
            PixelDirection[] pixelDirections = new PixelDirection[this.N];
            for (int k = 0; k < this.N; k++) {
                pixelDirections[k] = Helper.getRandomElementFromList(PixelDirection.values());
            }
            Chromosome chromosome = new Chromosome(pixelDirections);
            this.population.add(chromosome);
        }
    }

    /**
     * Returns an array where each index represents the segment ID of a pixel. So,
     * [1, 1, 5, ...] means that pixel 0 and 1 belongs to segment 1 and pixel 2
     * belongs to segment 5.
     * 
     * @param chromosome
     * @return
     */
    int[] genotypeToPhenotype(Chromosome chromosome) {
        int[] indexToSegmentIds = new int[this.N];
        DisjointSets disjointSets = new DisjointSets(this.N);
        for (int i = 0; i < this.N; i++) {
            PixelDirection pixelDirection = chromosome.pixelDirections[i];
            int neighborIndex = this.neighborArrays[i][pixelDirection.ordinal()];
            if (neighborIndex == -1 || neighborIndex == i) {
                continue;
            }
            disjointSets.union(i, neighborIndex);
        }

        // Let each pixel point to its representative, this way all indices pointing to
        // the same number is in the same segment
        for (int i = 0; i < this.N; i++) {
            indexToSegmentIds[i] = disjointSets.find(i);
        }

        return indexToSegmentIds;
    }

    /**
     * Constructs a 2D array where the inner array corresponds to the neighbor given
     * by PixelDirection. So, [2][3] gives us the index of the pixel 2's upper
     * neighbor.
     * 
     * @return
     */
    int[][] constructNeighborArray() {
        int[][] neighborArrays = new int[this.N][PixelDirection.values().length];
        for (int i = 0; i < this.N; i++) {
            Arrays.fill(neighborArrays[i], -1);
        }

        for (int h = 0; h < this.image.getHeight(); h++) {
            for (int w = 0; w < this.image.getWidth(); w++) {
                int i = h * this.image.getWidth() + w;

                // Right neighbor
                if (w < this.image.getWidth() - 1) {
                    neighborArrays[i][1] = i + 1;
                }

                // Left neighbor
                if (w > 0) {
                    neighborArrays[i][2] = i - 1;
                }

                // Top neighbor
                if (h > 0) {
                    neighborArrays[i][3] = i - this.image.getWidth();

                    // Top-right neighbor
                    if (w < this.image.getWidth() - 1) {
                        neighborArrays[i][5] = i - this.image.getWidth() + 1;
                    }

                    // Top-left neighbor
                    if (w > 0) {
                        neighborArrays[i][7] = i - this.image.getWidth() - 1;
                    }
                }

                // Bottom neighbor
                if (h < this.image.getHeight() - 1) {
                    neighborArrays[i][4] = i + this.image.getWidth();

                    // Bottom-right neighbor
                    if (w < this.image.getWidth() - 1) {
                        neighborArrays[i][6] = i + this.image.getWidth() + 1;
                    }

                    // Bottom-left neighbor
                    if (w > 0) {
                        neighborArrays[i][8] = i + this.image.getWidth() - 1;
                    }
                }

            }
        }
        return neighborArrays;
    }

    /**
     * Returns a 2D array which holds the edge value between two pixels. [0][2]
     * gives us the edge value (RGB distance) between pixel 0 and the left pixel of
     * pixel 0 according to PixelDirection.
     * 
     * @return
     */
    double[][] constructEdgeValues() {
        double[][] edgeValues = new double[this.N][PixelDirection.values().length];
        for (int i = 0; i < this.N; i++) {
            Arrays.fill(edgeValues[i], Double.POSITIVE_INFINITY);
        }

        for (int h = 0; h < this.image.getHeight(); h++) {
            for (int w = 0; w < this.image.getWidth(); w++) {
                int i = h * this.image.getWidth() + w;

                int indexRgb = this.image.getRGB(w, h);

                // Right neighbor
                if (w < this.image.getWidth() - 1) {
                    edgeValues[i][1] = getRgbDistance(indexRgb, this.image.getRGB(w + 1, h));
                }

                // Left neighbor
                if (w > 0) {
                    edgeValues[i][2] = getRgbDistance(indexRgb, this.image.getRGB(w - 1, h));
                }

                // Top neighbor
                if (h > 0) {
                    edgeValues[i][3] = getRgbDistance(indexRgb, this.image.getRGB(w, h - 1));

                    // Top-right neighbor
                    if (w < this.image.getWidth() - 1) {
                        edgeValues[i][5] = getRgbDistance(indexRgb, this.image.getRGB(w + 1, h - 1));
                    }

                    // Top-left neighbor
                    if (w > 0) {
                        edgeValues[i][7] = getRgbDistance(indexRgb, this.image.getRGB(w - 1, h - 1));
                    }
                }

                // Bottom neighbor
                if (h < this.image.getHeight() - 1) {
                    edgeValues[i][4] = getRgbDistance(indexRgb, this.image.getRGB(w, h + 1));

                    // Bottom-right neighbor
                    if (w < this.image.getWidth() - 1) {
                        edgeValues[i][6] = getRgbDistance(indexRgb, this.image.getRGB(w + 1, h + 1));
                    }

                    // Bottom-left neighbor
                    if (w > 0) {
                        edgeValues[i][8] = getRgbDistance(indexRgb, this.image.getRGB(w - 1, h + 1));
                    }
                }

            }
        }
        return edgeValues;
    }

    void nonDominatedSortingGeneticAlgorithm2() {

    }

    double weightedSumFitness(double edgeValue, double connectivityMeasure, double overallDeviation) {
        // We want to maximize edgeValue and minimize the two others, so we need to
        // reverse it.
        // TODO adjust the weighting parameters
        return -1.0 * edgeValue + 1.0 * connectivityMeasure + 0.001 * overallDeviation;
    }

    double edgeValue(int[] indexToSegmentIds) {
        // We want to maximize this
        double edgeValue = 0.0;
        for (int i = 0; i < this.N; i++) {
            // Loop the neighbor pixels of pixel i
            // if j is not in the same segment then add a number from equation 2
            // edgeValue += getRgbDistance(...)
            for (int j = 1; i < this.neighborArrays[0].length; i++) {
                // Skip the first neighbor index since it points to itself
                int neighborIndex = this.neighborArrays[i][j];
                if (neighborIndex != -1) {
                    if (indexToSegmentIds[i] != indexToSegmentIds[neighborIndex]) {
                        edgeValue += getRgbDistance(getRgbFromIndex(i), getRgbFromIndex(neighborIndex));
                    }
                }
            }
        }
        return edgeValue;
    }

    double connectivityMeasure(int[] indexToSegmentIds) {
        // We want to minimize this
        double connectivity = 0.0;
        for (int i = 0; i < this.N; i++) {
            // Loop the neighbor pixels of pixel i
            // if j is not in the same segment then add a number from equation 4
            // connectivity += 1/8
            // TODO ask about this
            // ? 1/8 or 1/F(j) here?
            for (int j = 1; i < this.neighborArrays[0].length; i++) {
                // Skip the first neighbor index since it points to itself
                int neighborIndex = this.neighborArrays[i][j];
                if (neighborIndex != -1) {
                    if (indexToSegmentIds[i] != indexToSegmentIds[neighborIndex]) {
                        connectivity += 1 / j;
                    }
                }
            }
        }
        return connectivity;
    }

    double overallDeviation(int[] indexToSegmentIds) {
        // We want to minimize this

        // Calculates centroid for each segment
        Map<Integer, List<Integer>> segmentSumsR = new HashMap<>();
        Map<Integer, List<Integer>> segmentSumsG = new HashMap<>();
        Map<Integer, List<Integer>> segmentSumsB = new HashMap<>();
        for (int i = 0; i < indexToSegmentIds.length; i++) {
            int segmentId = indexToSegmentIds[i];
            int rgb = getRgbFromIndex(i);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = (rgb >> 0) & 0xFF;
            if (segmentSumsR.containsKey(segmentId)) {
                segmentSumsR.get(segmentId).add(r);
                segmentSumsG.get(segmentId).add(g);
                segmentSumsB.get(segmentId).add(b);
            } else {
                List<Integer> valuesR = new ArrayList<>();
                List<Integer> valuesG = new ArrayList<>();
                List<Integer> valuesB = new ArrayList<>();
                valuesR.add(r);
                valuesG.add(g);
                valuesB.add(b);
                segmentSumsR.put(segmentId, valuesR);
                segmentSumsG.put(segmentId, valuesG);
                segmentSumsB.put(segmentId, valuesB);
            }
        }

        Map<Integer, Integer> segmentCentroidsR = new HashMap<>();
        Map<Integer, Integer> segmentCentroidsG = new HashMap<>();
        Map<Integer, Integer> segmentCentroidsB = new HashMap<>();
        for (Integer key : segmentSumsR.keySet()) {
            int sumR = segmentSumsR.get(key).stream().reduce(0, Integer::sum);
            int sumG = segmentSumsG.get(key).stream().reduce(0, Integer::sum);
            int sumB = segmentSumsB.get(key).stream().reduce(0, Integer::sum);
            int averageR = sumR / segmentSumsR.get(key).size();
            int averageG = sumG / segmentSumsG.get(key).size();
            int averageB = sumB / segmentSumsB.get(key).size();
            segmentCentroidsR.put(key, averageR);
            segmentCentroidsG.put(key, averageG);
            segmentCentroidsB.put(key, averageB);
        }

        Map<Integer, Integer> segmentCentroids = new HashMap<>();
        for (Integer key : segmentSumsR.keySet()) {
            int rgb = segmentCentroidsR.get(key);
            rgb = (rgb << 8) + segmentCentroidsG.get(key);
            rgb = (rgb << 8) + segmentCentroidsB.get(key);
            segmentCentroids.put(key, rgb);
        }

        double deviation = 0.0;
        // Loop all segment_sets
        for (Integer key : segmentCentroids.keySet()) {
            // Loop all pixels in that segment_set
            for (int i = 0; i < indexToSegmentIds.length; i++) {
                if (indexToSegmentIds[i] == key) {
                    deviation += getRgbDistance(getRgbFromIndex(i), segmentCentroids.get(key));
                }
            }
        }
        return deviation;
    }

    int getRgbFromIndex(int index) {
        int y = index / this.image.getWidth();
        int x = index % this.image.getWidth();
        return this.image.getRGB(x, y);
    }

    double getRgbDistance(int rgb1, int rgb2) {
        Color color1 = new Color(rgb1);
        Color color2 = new Color(rgb2);
        int distanceRed = color1.getRed() - color2.getRed();
        int distanceGreen = color1.getGreen() - color2.getGreen();
        int distanceBlue = color1.getBlue() - color2.getBlue();
        return Math.sqrt(Math.pow(distanceRed, 2) + Math.pow(distanceGreen, 2) + Math.pow(distanceBlue, 2));
    }

    Chromosome[] tournamentSelection(int selection_size) {
        Chromosome[] winners = new Chromosome[selection_size];
        // TODO take 4 as tournament size param
        int tournamentSize = 4; // How many we compare
        for (int i = 0; i < selection_size; i++) {
            // selection_size number of tournaments
            List<Chromosome> tournamentSet = new ArrayList<>(tournamentSize);
            for (int j = 0; j < tournamentSize; j++) {
                // ? Duplicates allowed in the tournamentSet now, should this be allowed?
                tournamentSet.add(Helper.getRandomElementFromList(this.population));
            }

            // TODO take 0.8 as tournamentSelectionNumber from config
            if (ThreadLocalRandom.current().nextDouble() < 0.8) {
                tournamentSet.sort(Comparator.comparing(c -> c.fitness));
                winners[i] = tournamentSet.get(0);
            } else {
                // Add random parent
                winners[i] = Helper.getRandomElementFromList(tournamentSet);
            }
        }
        return winners;
    }

    Chromosome[] crossover(Chromosome parent1, Chromosome parent2) {
        Chromosome offspring1 = new Chromosome(parent1);
        Chromosome offspring2 = new Chromosome(parent2);

        // If not crossover, we return a copy of the parents without modifications
        // TODO take crossoverChance as config
        if (ThreadLocalRandom.current().nextDouble() < 0.7) {
            // Make a random crossover point between [1, this.N) and not [0, this.N)
            // because we want genes from both parents.
            int crossoverPoint = ThreadLocalRandom.current().nextInt(1, this.N);
            for (int i = crossoverPoint; i < this.N; i++) {
                // The left side of pixelDirections is unchanged, while the right side are
                // flipped between chromosomes
                offspring1.pixelDirections[i] = parent2.pixelDirections[i];
                offspring2.pixelDirections[i] = parent1.pixelDirections[i];
            }
        }

        Chromosome[] offsprings = { offspring1, offspring2 };
        return offsprings;
    }

    /**
     * Adds a 1-element mutation at a random index in the chromosome.
     * 
     * @param chromosome
     */
    void mutation(Chromosome chromosome) {
        for (int i = 0; i < chromosome.pixelDirections.length; i++) {
            // TODO mutation rate config param
            // ? Should we use bitwise mutation or only one mutated element in the array
            // ? as per the paper?
            if (ThreadLocalRandom.current().nextDouble() < 0.01) {
                chromosome.pixelDirections[i] = Helper.getRandomElementFromList(PixelDirection.values());
            }
        }
    }

    void elitism(List<Chromosome> oldPopulation, List<Chromosome> newPopulation, int elitismCount) {
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
                Chromosome[] parents = tournamentSelection(2);
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

            // Generational with elitism for now
            // TODO elitism ratio config param
            elitism(this.population, newPopulation, 5);
            this.population = newPopulation;

            double bestFitness = Double.POSITIVE_INFINITY;
            // Chromosome bestChromosome = null;
            for (Chromosome chromosome : this.population) {
                if (chromosome.fitness < bestFitness) {
                    bestFitness = chromosome.fitness;
                    // bestChromosome = chromosome;
                }
            }
            System.out.println("Best fitness:" + bestFitness);

        }

        this.population.sort(Comparator.comparing(c -> c.fitness));
        Chromosome bestChromosome = this.population.get(0);

        BufferedImage bufferedImage = new BufferedImage(this.image.getWidth(), this.image.getHeight(),
                // BufferedImage.TYPE_BYTE_BINARY);
                BufferedImage.TYPE_INT_RGB);

        final int WHITE = new Color(255, 255, 255).getRGB();
        final int BLACK = new Color(0, 0, 0).getRGB();

        // for (int y = 0; y < this.image.getHeight(); y++) {
        // for (int x = 0; x < this.image.getWidth(); x++) {
        // bufferedImage.setRGB(x, y, WHITE);
        // }
        // }

        Map<Integer, Color> colors = new HashMap<>();
        for (int j = 0; j < bestChromosome.indexToSegmentIds.length; j++) {
            if (!colors.containsKey(bestChromosome.indexToSegmentIds[j])) {
                float r = ThreadLocalRandom.current().nextFloat();
                float g = ThreadLocalRandom.current().nextFloat();
                float b = ThreadLocalRandom.current().nextFloat();
                colors.put(bestChromosome.indexToSegmentIds[j], new Color(r, g, b));
            }
        }

        System.out.println("Segments " + colors.keySet().size());
        System.out.println();

        for (int pixel = 0; pixel < this.N; pixel++) {
            int y = pixel / this.image.getWidth();
            int x = pixel % this.image.getWidth();
            bufferedImage.setRGB(x, y, colors.get(bestChromosome.indexToSegmentIds[pixel]).getRGB());
        }

        saveImage(bufferedImage, "best_one");
    }

}
