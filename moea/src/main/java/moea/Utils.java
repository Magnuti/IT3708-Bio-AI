package moea;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.awt.Color;
import java.awt.Graphics2D;

import javax.imageio.ImageIO;

import moea.App.PixelDirection;

public class Utils {
    public static List<Chromosome> initPopulationByMinimumSpanningTree(int populationSize, MST mst, int N) {
        System.out.println("Initializing a population of size " + populationSize);
        List<Chromosome> populationSync = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger createdIndividuals = new AtomicInteger(0);
        List<Thread> threads = new ArrayList<>();
        final int threadCount = 24;
        for (int i = 0; i < threadCount; i++) {
            threads.add(new Thread(new Runnable() {
                @Override
                public void run() {
                    while (createdIndividuals.getAndIncrement() < populationSize) {
                        int startingNode = ThreadLocalRandom.current().nextInt(N);
                        PixelDirection[] pixelDirections = mst.findDirections(startingNode);
                        Chromosome chromosome = new Chromosome(pixelDirections);
                        populationSync.add(chromosome);
                    }
                }
            }));
        }
        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Population initialization complete");
        assert populationSync.size() == populationSize;
        return new ArrayList<>(populationSync);
    }

    public static List<Chromosome> initRandomPopulation(int populationSize, int N) {
        List<Chromosome> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            PixelDirection[] pixelDirections = new PixelDirection[N];
            for (int k = 0; k < N; k++) {
                pixelDirections[k] = Helper.getRandomElementFromList(PixelDirection.values());
            }
            Chromosome chromosome = new Chromosome(pixelDirections);
            population.add(chromosome);
        }
        return population;
    }

    public static Chromosome[] crossover(Chromosome parent1, Chromosome parent2, double crossoverProbability) {
        Chromosome offspring1 = new Chromosome(parent1);
        Chromosome offspring2 = new Chromosome(parent2);

        // If not crossover, we return a copy of the parents without modifications
        if (ThreadLocalRandom.current().nextDouble() < crossoverProbability) {
            // Make a random crossover point between [1, this.N) and not [0, this.N)
            // because we want genes from both parents.
            int crossoverPoint = ThreadLocalRandom.current().nextInt(1, parent1.pixelDirections.length);
            for (int i = crossoverPoint; i < parent1.pixelDirections.length; i++) {
                // The left side of pixelDirections is unchanged, while the right side are
                // flipped between chromosomes
                offspring1.pixelDirections[i] = parent2.pixelDirections[i];
                offspring2.pixelDirections[i] = parent1.pixelDirections[i];
            }
        }

        Chromosome[] offsprings = { offspring1, offspring2 };
        return offsprings;
    }

    /**
     * Sets a random value at one random index in the chromosome.
     * 
     * @param chromosome
     */
    public static void mutation(Chromosome chromosome, double mutationProbability) {
        if (ThreadLocalRandom.current().nextDouble() < mutationProbability) {
            int i = ThreadLocalRandom.current().nextInt(chromosome.pixelDirections.length);
            chromosome.pixelDirections[i] = Helper.getRandomElementFromList(PixelDirection.values());
        }
    }

    /**
     * Constructs a 2D array where the inner array corresponds to the neighbor given
     * by PixelDirection. So, [2][3] gives us the index of the pixel 2's upper
     * neighbor.
     */
    public static int[][] constructNeighborArray(int imageWidth, int imageHeight) {
        final int N = imageWidth * imageHeight;
        int[][] neighborArrays = new int[N][PixelDirection.values().length];
        for (int i = 0; i < N; i++) {
            Arrays.fill(neighborArrays[i], -1);
        }

        for (int h = 0; h < imageHeight; h++) {
            for (int w = 0; w < imageWidth; w++) {
                int i = h * imageWidth + w;

                // Right neighbor
                if (w < imageWidth - 1) {
                    neighborArrays[i][1] = i + 1;
                }

                // Left neighbor
                if (w > 0) {
                    neighborArrays[i][2] = i - 1;
                }

                // Top neighbor
                if (h > 0) {
                    neighborArrays[i][3] = i - imageWidth;

                    // Top-right neighbor
                    if (w < imageWidth - 1) {
                        neighborArrays[i][5] = i - imageWidth + 1;
                    }

                    // Top-left neighbor
                    if (w > 0) {
                        neighborArrays[i][7] = i - imageWidth - 1;
                    }
                }

                // Bottom neighbor
                if (h < imageHeight - 1) {
                    neighborArrays[i][4] = i + imageWidth;

                    // Bottom-right neighbor
                    if (w < imageWidth - 1) {
                        neighborArrays[i][6] = i + imageWidth + 1;
                    }

                    // Bottom-left neighbor
                    if (w > 0) {
                        neighborArrays[i][8] = i + imageWidth - 1;
                    }
                }

            }
        }
        return neighborArrays;
    }

    /**
     * Returns a 2D array which holds the edge value between two pixels. [0][2]
     * gives us the edge value (RGB distance) between pixel 0 and the left pixel of
     * pixel 0 according to PixelDirection.
     */
    public static double[][] constructEdgeValues(BufferedImage image) {
        final int N = image.getWidth() * image.getHeight();
        double[][] edgeValues = new double[N][PixelDirection.values().length];
        for (int i = 0; i < N; i++) {
            Arrays.fill(edgeValues[i], Double.POSITIVE_INFINITY);
        }

        for (int h = 0; h < image.getHeight(); h++) {
            for (int w = 0; w < image.getWidth(); w++) {
                int i = h * image.getWidth() + w;

                int indexRgb = image.getRGB(w, h);

                // Right neighbor
                if (w < image.getWidth() - 1) {
                    edgeValues[i][1] = getRgbDistance(indexRgb, image.getRGB(w + 1, h));
                }

                // Left neighbor
                if (w > 0) {
                    edgeValues[i][2] = getRgbDistance(indexRgb, image.getRGB(w - 1, h));
                }

                // Top neighbor
                if (h > 0) {
                    edgeValues[i][3] = getRgbDistance(indexRgb, image.getRGB(w, h - 1));

                    // Top-right neighbor
                    if (w < image.getWidth() - 1) {
                        edgeValues[i][5] = getRgbDistance(indexRgb, image.getRGB(w + 1, h - 1));
                    }

                    // Top-left neighbor
                    if (w > 0) {
                        edgeValues[i][7] = getRgbDistance(indexRgb, image.getRGB(w - 1, h - 1));
                    }
                }

                // Bottom neighbor
                if (h < image.getHeight() - 1) {
                    edgeValues[i][4] = getRgbDistance(indexRgb, image.getRGB(w, h + 1));

                    // Bottom-right neighbor
                    if (w < image.getWidth() - 1) {
                        edgeValues[i][6] = getRgbDistance(indexRgb, image.getRGB(w + 1, h + 1));
                    }

                    // Bottom-left neighbor
                    if (w > 0) {
                        edgeValues[i][8] = getRgbDistance(indexRgb, image.getRGB(w - 1, h + 1));
                    }
                }

            }
        }
        return edgeValues;
    }

    public static int getRgbFromIndex(BufferedImage image, int index) {
        int y = index / image.getWidth();
        int x = index % image.getWidth();
        return image.getRGB(x, y);
    }

    public static double getRgbDistance(int rgb1, int rgb2) {
        Color color1 = new Color(rgb1);
        Color color2 = new Color(rgb2);
        int distanceRed = color1.getRed() - color2.getRed();
        int distanceGreen = color1.getGreen() - color2.getGreen();
        int distanceBlue = color1.getBlue() - color2.getBlue();
        return Math.sqrt(Math.pow(distanceRed, 2) + Math.pow(distanceGreen, 2) + Math.pow(distanceBlue, 2));
    }

    public static void saveImage(BufferedImage bufferedImage, String name, String... outputPaths) {
        File raw_path = new File("output_images"); // TODO take this as an param
        if (!raw_path.exists()) {
            raw_path.mkdir();
        }
        File imagePath = raw_path;
        for (String s : outputPaths) {
            imagePath = new File(Paths.get(imagePath.toString(), s).toString());
            if (!imagePath.exists()) {
                imagePath.mkdir();
            }
        }
        Path path = Paths.get(imagePath.getPath(), name + ".jpg");
        try {
            ImageIO.write(bufferedImage, "jpg", new File(path.toString()));
        } catch (IOException e) {
            e.printStackTrace();
            throw new Error("Cannot save the image: " + path.toString());
        }
    }

    /**
     * Create both type 1 and type 2 images.
     */
    public static BufferedImage[] createImagesFromChromosome(Chromosome chromosome, BufferedImage image,
            int[][] neighborArrays) {
        BufferedImage type1 = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        BufferedImage type2 = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

        // Initialize the entire type 1 image as white
        Graphics2D graphics = type1.createGraphics();
        graphics.setPaint(new Color(255, 255, 255));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();

        // The type 2 image is just a copy of the original image
        graphics = type2.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();

        final int BLACK = new Color(0, 0, 0).getRGB();
        final int GREEN = new Color(50, 255, 0).getRGB();

        for (int pixel = 0; pixel < image.getWidth() * image.getHeight(); pixel++) {
            for (int neighborIndex = 1; neighborIndex < neighborArrays[pixel].length; neighborIndex++) {
                int neighborPixel = neighborArrays[pixel][neighborIndex];
                if (neighborPixel == -1
                        || chromosome.indexToSegmentIds[pixel] != chromosome.indexToSegmentIds[neighborPixel]) {
                    // Set the pixel to black if it is on the image border or the segment border
                    int y = pixel / image.getWidth();
                    int x = pixel % image.getWidth();
                    type1.setRGB(x, y, BLACK);
                    type2.setRGB(x, y, GREEN);
                    break; // No need to check the rest of the neighbors for this pixel
                }
            }
        }

        return new BufferedImage[] { type1, type2 };
    }
}
