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
    double mutationRate;
    boolean verbose;
    int saveInterval;

    Yaml yaml = new Yaml();

    public void parseConfig() {
        try {
            InputStream inputStream = new FileInputStream(new File("config.yaml"));
            Map<String, Object> obj = yaml.load(inputStream);

            this.imageDirectory = obj.get("image_directory").toString();
            this.populationSize = (int) obj.get("population_size");
            this.maxGeneration = (int) obj.get("max_generation");
            this.stopThreshold = (double) obj.get("stop_threshold");
            this.crossoverProbability = (double) obj.get("crossover_probability");
            this.mutationRate = (double) obj.get("mutation_rate");
            this.verbose = (boolean) obj.get("verbose");
            this.saveInterval = (int) obj.get("save_interval");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
