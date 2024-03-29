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
    double stopThreshold;
    double eliteRatio;
    double crossoverChance;
    double bound;
    double tournamentSelectionNumber;
    double crossoverInsertionNumber;
    double intraDepotMutationRate;
    double interDepotMutationRate;
    int apprate;
    boolean verbose;
    int saveInterval;

    Yaml yaml = new Yaml();

    public void parseConfig() {
        try {
            InputStream inputStream = new FileInputStream(new File("config.yaml"));
            Map<String, Object> obj = yaml.load(inputStream);
            this.inputFile = obj.get("input_file").toString();
            this.populationSize = (int) obj.get("population_size");
            this.maxGeneration = (int) obj.get("max_generation");
            this.stopThreshold = (double) obj.get("stop_threshold");
            this.eliteRatio = (double) obj.get("elite_ratio");
            this.crossoverChance = (double) obj.get("crossover_chance");
            this.bound = (double) obj.get("bound");
            this.tournamentSelectionNumber = (double) obj.get("tournament_selection_number");
            this.crossoverInsertionNumber = (double) obj.get("crossover_insertion_number");
            this.intraDepotMutationRate = (double) obj.get("intra_depot_mutation_rate");
            this.interDepotMutationRate = (double) obj.get("inter_depot_mutation_rate");
            this.apprate = (int) obj.get("APPRATE");
            this.verbose = (boolean) obj.get("verbose");
            this.saveInterval = (int) obj.get("save_interval");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
