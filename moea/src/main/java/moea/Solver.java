package moea;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.Color;

public class Solver {
    enum PixelValues {
        LEFT, RIGHT, UP, DOWN, NONE
    }

    private BufferedImage image;
    private int N; // Number of pixels in the image
    List<Map<Integer, Double>> edgeValues = new ArrayList<>();

    public Solver(BufferedImage image) {
        this.image = image;
        this.N = image.getHeight() * image.getWidth();
        this.edgeValues = constructEdgeValues();

        MST mst = new MST(edgeValues);
        mst.primMST(232);
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
            // Loop the neighbour pixels of pixel i
            // if j is not in the same segment then add a number from equation 2
            // edgeValue += getRgbDistance(...)
        }
        return edgeValue;
    }

    double getConnectivityMeasure() {
        double connectivity = 0.0;
        for (int i = 0; i < this.N; i++) {
            // Loop the neighbour pixels of pixel i
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
