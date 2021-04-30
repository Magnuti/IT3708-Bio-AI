package moea;

import java.io.File;

public class EvaluatorReturnValues {
    final File groundTruthFile;
    final File solutionFileType1;
    final double score;

    public EvaluatorReturnValues(File groundTruthFile, File solutionFileType1, double score) {
        this.groundTruthFile = groundTruthFile;
        this.solutionFileType1 = solutionFileType1;
        this.score = score;
    }
}
