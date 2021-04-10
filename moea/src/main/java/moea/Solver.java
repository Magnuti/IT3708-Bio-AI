package moea;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.awt.Color;

import moea.App.PixelDirection;

public class Solver {

    private BufferedImage image;
    private int N; // Number of pixels in the image
    List<Map<Integer, Double>> edgeValues = new ArrayList<>();

    // TODO maybe create a Pixel class which holds the x, y, RGB and segment ID ?
    private int[][] neighborArrays;
    PixelDirection[][] population;

    public Solver(ConfigParser configParser, BufferedImage image) {
        this.image = image;
        this.N = image.getHeight() * image.getWidth();

        // The population consists of several chromosomes
        this.population = new PixelDirection[configParser.populationSize][N];

        this.neighborArrays = constructNeighborArray();
        this.edgeValues = constructEdgeValues();

        // MST mst = new MST(edgeValues);
        // mst.primMST(232);
        initRandomPopulation(configParser.populationSize);

        // TODO multi-thread
        for (int i = 0; i < this.population.length; i++) {
            System.out.println("Chromosome: " + i);
            int[] indexToSegmentIds = genotypeToPhenotype(this.population[i]);
            double edgeValue = edgeValue(indexToSegmentIds);
            System.out.println("Edge value: " + edgeValue);
            double connectivityMeasure = connectivityMeasure(indexToSegmentIds);
            System.out.println("Connectivity measure: " + connectivityMeasure);
            double overallDeviation = overallDeviation(indexToSegmentIds);
            System.out.println("Overall deviation: " + overallDeviation);
            System.out.println();
        }
    }

    void initRandomPopulation(int populationSize) {
        for (int i = 0; i < populationSize; i++) {
            for (int k = 0; k < this.N; k++) {
                int randIndex = ThreadLocalRandom.current().nextInt(PixelDirection.values().length);
                this.population[i][k] = PixelDirection.values()[randIndex];
            }
        }
    }

    int[] genotypeToPhenotype(PixelDirection[] chromosome) {
        int[] indexToSegmentIds = new int[chromosome.length];
        DisjointSets disjointSets = new DisjointSets(this.N);
        for (int pixelIndex = 0; pixelIndex < this.N; pixelIndex++) {
            int neighborDirectionIndex = chromosome[pixelIndex].ordinal();
            int neighborIndex = this.neighborArrays[pixelIndex][neighborDirectionIndex];
            if (neighborIndex == -1 || neighborIndex == pixelIndex) {
                continue;
            }
            disjointSets.union(pixelIndex, neighborIndex);
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

    List<Map<Integer, Double>> constructEdgeValues() {
        List<Map<Integer, Double>> edgeValues = new ArrayList<>();
        for (int h = 0; h < this.image.getHeight(); h++) {
            for (int w = 0; w < this.image.getWidth(); w++) {
                Map<Integer, Double> values = new HashMap<>();

                int i = h * this.image.getWidth() + w;

                int indexRgb = this.image.getRGB(w, h);

                if (w > 0) {
                    int leftNeighbor = i - 1;
                    values.put(leftNeighbor, getRgbDistance(indexRgb, this.image.getRGB(w - 1, h)));
                }
                if (w < this.image.getWidth() - 1) {
                    int rightNeighbor = i + 1;
                    values.put(rightNeighbor, getRgbDistance(indexRgb, this.image.getRGB(w + 1, h)));
                }

                if (h > 0) {
                    int topNeighbor = i - this.image.getWidth();
                    values.put(topNeighbor, getRgbDistance(indexRgb, this.image.getRGB(w, h - 1)));
                }

                if (h < this.image.getHeight() - 1) {
                    int bottomNeighbor = i + this.image.getWidth();
                    values.put(bottomNeighbor, getRgbDistance(indexRgb, this.image.getRGB(w, h + 1)));
                }
                edgeValues.add(values);
            }
        }
        return edgeValues;
    }

    void nonDominatedSortingGeneticAlgorithm2() {

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
}
