package mdvrp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class ConfigParser {

    String inputFile;
    int populationSize;
    int maxGeneration;
    double crossoverChance;
    double bound;
    double tournamentSelectionNumber;
    double crossoverInsertionNumber;
    int apprate;

    Yaml yaml = new Yaml();

    public void parseConfig() {
        try {
            InputStream inputStream = new FileInputStream(new File("config.yaml"));
            Map<String, Object> obj = yaml.load(inputStream);
            this.inputFile = obj.get("input_file").toString();
            this.populationSize = (int) obj.get("population_size");
            this.maxGeneration = (int) obj.get("max_generation");
            this.crossoverChance = (double) obj.get("crossover_chance");
            this.bound = (double) obj.get("bound");
            this.tournamentSelectionNumber = (double) obj.get("tournament_selection_number");
            this.crossoverInsertionNumber = (double) obj.get("crossover_insertion_number");
            this.apprate = (int) obj.get("APPRATE");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
