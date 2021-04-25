package moea;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.image.BufferedImage;

import moea.App.PixelDirection;

public class Chromosome {

    PixelDirection[] pixelDirections;
    int[] indexToSegmentIds;
    double edgeValue;
    double connectivityMeasure;
    double overallDeviation;

    public Chromosome(PixelDirection[] pixelDirections) {
        this.pixelDirections = pixelDirections;
    }

    /**
     * Make a deep copy.
     * 
     * @param chromosomeToCopy
     */
    public Chromosome(Chromosome chromosomeToCopy) {
        this.pixelDirections = chromosomeToCopy.pixelDirections.clone();
    }

    public void recalculateObjectives(int N, int[][] neighborArrays, BufferedImage image) {
        calculateIndexToSegmentIds(N, neighborArrays);
        calculateEdgeValue(neighborArrays, image);
        calculateConnectivityMeasure(neighborArrays, image);
        calculateOverallDeviation(image);
    }

    /**
     * Calculates an array where each index represents the segment ID of a pixel.
     * So, [1, 1, 5, ...] means that pixel 0 and 1 belongs to segment 1 and pixel 2
     * belongs to segment 5. The segment IDs are not incremental, rather each
     * segment ID points to some pixel index in the image. So, a total of three
     * segments may have indices 43, 121 and 9.
     */
    private void calculateIndexToSegmentIds(int N, int[][] neighborArrays) {
        DisjointSets disjointSets = new DisjointSets(N);
        for (int i = 0; i < N; i++) {
            PixelDirection pixelDirection = this.pixelDirections[i];
            int neighborIndex = neighborArrays[i][pixelDirection.ordinal()];
            if (neighborIndex == -1) {
                // Points to a border pixel or itself
                continue;
            }
            disjointSets.union(i, neighborIndex);
        }

        int[] indexToSegmentIds = new int[N];
        // Let each pixel point to its representative, this way all indices pointing to
        // the same number is in the same segment
        for (int i = 0; i < N; i++) {
            indexToSegmentIds[i] = disjointSets.find(i);
        }

        this.indexToSegmentIds = indexToSegmentIds;
    }

    private void calculateEdgeValue(int[][] neighborArrays, BufferedImage image) {
        // Normally we want to maximize this, but since the other two objectives are to
        // be minimized we should also minimize this. So, we use -= instead of +=.
        double edgeValue = 0.0;
        for (int i = 0; i < image.getWidth() * image.getHeight(); i++) {
            // Loop the neighbor pixels of pixel i
            // if j is not in the same segment then add a number from equation 2
            // edgeValue += getRgbDistance(...)
            for (int j = 1; i < neighborArrays[0].length; i++) {
                // Skip the first neighbor index since it points to itself
                int neighborIndex = neighborArrays[i][j];
                if (neighborIndex != -1) {
                    if (this.indexToSegmentIds[i] != this.indexToSegmentIds[neighborIndex]) {
                        edgeValue -= Utils.getRgbDistance(Utils.getRgbFromIndex(image, i),
                                Utils.getRgbFromIndex(image, neighborIndex));
                    }
                }
            }
        }
        this.edgeValue = edgeValue;
    }

    private void calculateConnectivityMeasure(int[][] neighborArrays, BufferedImage image) {
        // We want to minimize this
        double connectivity = 0.0;
        for (int i = 0; i < image.getWidth() * image.getHeight(); i++) {
            // Loop the neighbor pixels of pixel i
            // if j is not in the same segment then add a number from equation 4
            // connectivity += 1/8
            for (int j = 1; i < neighborArrays[0].length; i++) {
                // Skip the first neighbor index since it points to itself
                int neighborIndex = neighborArrays[i][j];
                if (neighborIndex != -1) {
                    if (this.indexToSegmentIds[i] != this.indexToSegmentIds[neighborIndex]) {
                        connectivity += (1.0 / 8.0);
                    }
                }
            }
        }
        this.connectivityMeasure = connectivity;
    }

    private void calculateOverallDeviation(BufferedImage image) {
        // We want to minimize this

        // Calculates centroid for each segment
        Map<Integer, List<Integer>> segmentSumsR = new HashMap<>();
        Map<Integer, List<Integer>> segmentSumsG = new HashMap<>();
        Map<Integer, List<Integer>> segmentSumsB = new HashMap<>();
        for (int i = 0; i < image.getWidth() * image.getHeight(); i++) {
            int segmentId = this.indexToSegmentIds[i];
            int rgb = Utils.getRgbFromIndex(image, i);
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
        // Now we have the centroid for each segment

        double deviation = 0.0;
        for (Integer key : segmentCentroids.keySet()) {
            // Loop all pixels in that segment_set
            for (int i = 0; i < image.getWidth() * image.getHeight(); i++) {
                if (this.indexToSegmentIds[i] == key) {
                    deviation += Utils.getRgbDistance(Utils.getRgbFromIndex(image, i), segmentCentroids.get(key));
                }
            }
        }
        this.overallDeviation = deviation;
    }

}
