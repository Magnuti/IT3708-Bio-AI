package moea;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        Solver solver = new Solver(configParser, image);
        solver.simpleGeneticAlgorithm();
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
