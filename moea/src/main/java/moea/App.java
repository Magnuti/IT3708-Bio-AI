package moea;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

/**
 * Hello world!
 *
 */
public class App {

    // Note that the order matters
    public enum PixelDirection {
        NONE, RIGHT, LEFT, UP, DOWN, TOP_RIGHT, BOTTOM_RIGHT, TOP_LEFT, BOTTOM_LEFT
    }

    public static void main(String[] args) {
        ConfigParser configParser = new ConfigParser();
        configParser.parseConfig();

        BufferedImage image = openImage(configParser.imageDirectory);

        NSGA2 nsga2 = new NSGA2(configParser, image);
        nsga2.runGA();

        FeedbackStation feedbackStation = new FeedbackStation();
        Thread evaluatorThread = new Thread(new Evaluator(feedbackStation));
        evaluatorThread.start();

        // Active poll the scores
        while (feedbackStation.evaluatorReturnValues == null) {
            continue;
        }

        EvaluatorReturnValues bestEvalObject = feedbackStation.evaluatorReturnValues[0];
        for (EvaluatorReturnValues e : feedbackStation.evaluatorReturnValues) {
            if (e.score > bestEvalObject.score) {
                bestEvalObject = e;
            }
        }

        System.out.println("Best score: " + bestEvalObject.score);
        System.out.println("Solution file: " + bestEvalObject.solutionFile.toPath());
        System.out.println("GT file: " + bestEvalObject.groundTruthFile.toPath());

        List<Double> scores = Arrays.stream(feedbackStation.evaluatorReturnValues).map(c -> c.score).collect(Collectors.toList());
        System.out
                .println("Score statisticss: " + scores.stream().mapToDouble(Double::doubleValue).summaryStatistics());
    }

    static BufferedImage openImage(String image_directory) {
        Path path = Paths.get("training_images", image_directory, "Test image.jpg");
        try {
            return ImageIO.read(new File(path.toString()));
        } catch (IOException e) {
            e.printStackTrace();
            throw new Error("Cannot open the image: " + path.toString());
        }
    }
}
