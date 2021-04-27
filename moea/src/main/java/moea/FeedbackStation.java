package moea;

public class FeedbackStation {
    volatile EvaluatorReturnValues[] evaluatorReturnValues;

    public void reset() {
        this.evaluatorReturnValues = null;
    }
}
