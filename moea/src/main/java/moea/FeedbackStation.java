package moea;

import java.util.concurrent.ArrayBlockingQueue;

public class FeedbackStation {
    ArrayBlockingQueue<EvaluatorReturnValues[]> evaluatorReturnValues;
    ArrayBlockingQueue<String> solutionLocations;
    volatile boolean stop = false;

    public FeedbackStation(int size) {
        this.evaluatorReturnValues = new ArrayBlockingQueue<>(size);
        this.solutionLocations = new ArrayBlockingQueue<>(size);
    }
}
