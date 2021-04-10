package moea;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
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
        this.population = new PixelDirection[configParser.populationSize][N];

        this.neighborArrays = constructNeighborArray();
        this.edgeValues = constructEdgeValues();

        // MST mst = new MST(edgeValues);
        // mst.primMST(232);
        initRandomPopulation(configParser.populationSize);

        genotypeToPhenotype();
    }

    void initRandomPopulation(int populationSize) {
        for (int i = 0; i < populationSize; i++) {
            for (int k = 0; k < this.N; k++) {
                int randIndex = ThreadLocalRandom.current().nextInt(PixelDirection.values().length);
                this.population[i][k] = PixelDirection.values()[randIndex];
            }
        }
    }

    List<List<Set<Integer>>> genotypeToPhenotype() {
        // A list of segments for each individual in th population
        List<List<Set<Integer>>> segmentsPerPopulation = new ArrayList<>();
        for (int population = 0; population < this.population.length; population++) {
            DisjointSets disjointSets = new DisjointSets(this.N);
            for (int pixelIndex = 0; pixelIndex < this.N; pixelIndex++) {
                int neighborDirectionIndex = this.population[population][pixelIndex].ordinal();
                int neighborIndex = this.neighborArrays[pixelIndex][neighborDirectionIndex];
                if (neighborIndex == -1 || neighborIndex == pixelIndex) {
                    continue;
                }
                disjointSets.union(pixelIndex, neighborIndex);
            }

            Map<Integer, Set<Integer>> m = new HashMap<>();
            for (int j = 0; j < this.N; j++) {
                int parent = disjointSets.find(j);
                if (m.containsKey(parent)) {
                    m.get(parent).add(j);
                } else {
                    Set<Integer> set = new HashSet<>();
                    set.add(parent);
                    set.add(j); // If the parent is itself the set handles this
                    m.put(parent, set);
                }
            }
        }
        return segmentsPerPopulation;
    }

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

    double getEdgeValue() {
        double edgeValue = 0.0;
        for (int i = 0; i < this.N; i++) {
            // Loop the neighbor pixels of pixel i
            // if j is not in the same segment then add a number from equation 2
            // edgeValue += getRgbDistance(...)
        }
        return edgeValue;
    }

    double getConnectivityMeasure() {
        double connectivity = 0.0;
        for (int i = 0; i < this.N; i++) {
            // Loop the neighbor pixels of pixel i
            // if j is not in the same segment then add a number from equation 4
            // connectivity += 1/8
            // ? 1/8 or 1/F(j) here? ask about this
        }
        return connectivity;
    }

    double getOverallDeviation() {
        double deviation = 0.0;
        // loop all segment_sets
        // loop all pixels in that segment_set
        // deviation += getRgbDistance(i, centroid_of_the_current_segment_set)
        return deviation;
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
