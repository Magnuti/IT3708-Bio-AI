package moea;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class ConfigParser {

    String imageDirectory;
    int populationSize;
    int maxGeneration;
    double stopThreshold;
    double crossoverProbability;
    double mutationProbability;
    int tournamentSize;
    int sgaElitismCount;
    boolean verbose;
    int saveInterval;

    Yaml yaml = new Yaml();

    public void parseConfig() {
        try {
            InputStream inputStream = new FileInputStream(new File("config.yaml"));
            Map<String, Object> obj = yaml.load(inputStream);

            this.imageDirectory = obj.get("image_directory").toString();
            this.populationSize = (int) obj.get("population_size");
            if (this.populationSize % 2 == 1) {
                System.out.println(
                        "Warning: Please keep the population size as an even number. Why? Because two parents can reproduce easily, while three is more difficult.");
                this.populationSize--;
                System.out.println("Using a population size of: " + populationSize);
            }

            this.maxGeneration = (int) obj.get("max_generation");
            this.stopThreshold = (double) obj.get("stop_threshold");
            this.crossoverProbability = (double) obj.get("crossover_probability");
            this.mutationProbability = (double) obj.get("mutation_probability");
            this.tournamentSize = (int) obj.get("tournament_size");

            double sgaElitism = (double) obj.get("sga_elitism");
            this.sgaElitismCount = (int) Math.round((double) this.populationSize * sgaElitism);
            if (this.sgaElitismCount == 0) {
                System.out.println(ConsoleColors.YELLOW + "Warning: elitism is not applied." + ConsoleColors.RESET);
            }

            this.verbose = (boolean) obj.get("verbose");
            this.saveInterval = (int) obj.get("save_interval");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
