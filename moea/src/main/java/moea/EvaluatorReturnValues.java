package moea;

import java.io.File;

public class EvaluatorReturnValues {
    final File groundTruthFile;
    final File solutionFile;
    final double score;

    public EvaluatorReturnValues(File groundTruthFile, File solutionFile, double score) {
        this.groundTruthFile = groundTruthFile;
        this.solutionFile = solutionFile;
        this.score = score;
    }
}
