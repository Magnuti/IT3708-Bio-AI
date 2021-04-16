package moea;

import moea.App.PixelDirection;

public class Chromosome {

    PixelDirection[] pixelDirections;
    double fitness; // Less fitness is better, this is a minimization problem
    int[] indexToSegmentIds;

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
        this.fitness = chromosomeToCopy.fitness;
        this.indexToSegmentIds = chromosomeToCopy.indexToSegmentIds.clone();
    }

}
