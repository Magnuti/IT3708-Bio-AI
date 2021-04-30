package moea;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

public class App {

    // Note that the order matters
    public enum PixelDirection {
        NONE, RIGHT, LEFT, UP, DOWN, TOP_RIGHT, BOTTOM_RIGHT, TOP_LEFT, BOTTOM_LEFT
    }

    public static void main(String[] args) {
        ConfigParser configParser = new ConfigParser();
        configParser.parseConfig();

        BufferedImage image = openImage(configParser.imageDirectory);

        // TODO pass this to the ones who uses it
        File outputPath = new File("output_images");
        if (!outputPath.exists()) {
            outputPath.mkdir();
        }

        // Capacity 1 because we want a simple, stupid producer/consumer pattern
        FeedbackStation feedbackStation = new FeedbackStation(1);

        Thread gaThread = new Thread(new NSGA2(configParser, image, feedbackStation));
        Thread evaluatorThread = new Thread(
                new Evaluator(feedbackStation, "training_images/" + configParser.imageDirectory));

        gaThread.start();
        evaluatorThread.start();

        EvaluatorReturnValues finalEvalObject = null;

        while (true) {
            EvaluatorReturnValues[] evaluationResults = null;
            try {
                evaluationResults = feedbackStation.evaluatorReturnValues.take();
                System.out.println("Take eval results");
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }

            EvaluatorReturnValues bestEvalObject = evaluationResults[0];
            for (EvaluatorReturnValues e : evaluationResults) {
                if (e.score > bestEvalObject.score) {
                    bestEvalObject = e;
                }
            }

            System.out.println("Best score: " + bestEvalObject.score);
            System.out.println("Solution file: " + bestEvalObject.solutionFileType1.toPath());
            System.out.println("GT file: " + bestEvalObject.groundTruthFile.toPath());

            List<Double> scores = Arrays.stream(evaluationResults).map(c -> c.score).collect(Collectors.toList());
            System.out.println(
                    "Score statisticss: " + scores.stream().mapToDouble(Double::doubleValue).summaryStatistics());

            if (bestEvalObject.score > configParser.stopThreshold) {
                feedbackStation.stop = true;
                System.out.print(ConsoleColors.GREEN);
                System.out.println("Set stop at score: " + bestEvalObject.score);
                System.out.print(ConsoleColors.RESET);
                finalEvalObject = bestEvalObject;
                break;
            }
        }

        gaThread.interrupt();

        String type2Path = finalEvalObject.solutionFileType1.getPath();
        type2Path = type2Path.replace("type_1", "type_2");
        Path solutionFileType2Path = new File(type2Path).toPath();

        System.out.println(ConsoleColors.GREEN + "Best score: " + finalEvalObject.score);
        System.out.println("Solution file type 1: " + finalEvalObject.solutionFileType1.toPath());
        System.out.println("Solution file type 2: " + solutionFileType2Path);
        System.out.println("GT file: " + finalEvalObject.groundTruthFile.toPath() + ConsoleColors.RESET);

        // Save the final images
        File finalPath = new File(outputPath, "final_images");
        if (!finalPath.exists()) {
            finalPath.mkdir();
        }
        try {
            Files.copy(finalEvalObject.groundTruthFile.toPath(), Paths.get(finalPath.getPath(), "GT_image" + ".jpg"),
                    StandardCopyOption.REPLACE_EXISTING);
            Files.copy(finalEvalObject.solutionFileType1.toPath(),
                    Paths.get(finalPath.getPath(), "solution_type_1" + ".jpg"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(solutionFileType2Path, Paths.get(finalPath.getPath(), "solution_type_2" + ".jpg"),
                    StandardCopyOption.REPLACE_EXISTING);
            ImageIO.write(image, "jpg", new File(Paths.get(finalPath.getPath(), "test_image" + ".jpg").toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }

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
