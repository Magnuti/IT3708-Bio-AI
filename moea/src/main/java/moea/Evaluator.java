package moea;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

/**
 * How to use: Requires JavaFX. After having placed the ground truth images and
 * your segmentations in their appropriate folders, call runSameThread(). If
 * using from a GUI or other multithreaded contexts, i recommend uncommenting
 * the lines referencing the "feedbackStation" object, wrapping this object in a
 * Thread object, then calling start() on that thread. You will need to
 * implement the FeedbackStation interface yourselves.
 */
public final class Evaluator implements Runnable {
	String optFolder;
	String studFolder;

	final double colorValueSlackRange = 40.0 / 255.0;
	final double blackValueThreshold = 100.0 / 255.0;
	final int pixelCheckRange = 4;
	final boolean checkEightSurroundingPixels = true;

	List<File> optFiles = new ArrayList<>();
	List<File> studFiles = new ArrayList<>();
	List<Image> optImages = new ArrayList<>();
	List<Image> studImages = new ArrayList<>();

	private final FeedbackStation feedbackStation;

	public Evaluator(FeedbackStation feedbackStation, String inputPath) {
		this.feedbackStation = feedbackStation;
		this.optFolder = inputPath;
		Platform.startup(() -> {
			System.out.println("Platform start");
		});
	}

	@Override
	public void run() {
		Platform.runLater(() -> {
			while (!feedbackStation.stop) {
				try {
					// System.out.println("Want to take location");
					this.studFolder = feedbackStation.solutionLocations.take();
					// System.out.println("Took location " + studFolder);
				} catch (InterruptedException e1) {
					System.out.println("Evaluator was interrupted");
					break;
				}
				updateOptimalFiles();
				updateStudentFiles();
				updateImageLists();
				EvaluatorReturnValues[] results = evaluate();
				try {
					// System.out.println("Want to put eval results");
					this.feedbackStation.evaluatorReturnValues.put(results);
					// System.out.println("Did put eval results");
				} catch (InterruptedException e) {
					System.out.println("Evaluator was interrupted");
					break;
				}
			}
			System.out.println("Evaluator finished");
			Platform.exit();
		});
	}

	// public double[] runSameThread() {
	// updateOptimalFiles();
	// updateStudentFiles();
	// updateImageLists();
	// double[] scores = evaluate();
	// return scores;
	// }

	public EvaluatorReturnValues[] evaluate() {
		EvaluatorReturnValues[] evalObjects = new EvaluatorReturnValues[studImages.size()];
		for (int i = 0; i < studImages.size(); i++) {
			Image studImg = studImages.get(i);
			double highestScore = 0.0;
			File gtFile = null;
			for (int k = 0; k < optImages.size(); k++) {
				// for (Image optImg : optImages) {
				Image optImg = optImages.get(k);
				double res1 = compare(studImg, optImg);
				double res2 = compare(optImg, studImg);
				double result = Math.min(res1, res2);
				if (result > highestScore) {
					highestScore = result;
					gtFile = optFiles.get(k);
				}
			}
			evalObjects[i] = new EvaluatorReturnValues(gtFile, studFiles.get(i), highestScore);
		}
		return evalObjects;
	}

	private double compare(Image optImg, Image studImg) {
		PixelReader opt = optImg.getPixelReader();
		PixelReader stud = studImg.getPixelReader();
		while (opt == null || stud == null) {
			opt = optImg.getPixelReader();
			stud = studImg.getPixelReader();
		}

		int numBlackPixels = 0;
		int counter = 0; // number of similar pixels.

		for (int w = 0; w < optImg.getWidth(); w += 1) {
			for (int h = 0; h < optImg.getHeight(); h++) {
				double cOpt = opt.getColor(w, h).getBrightness();
				double cStud = stud.getColor(w, h).getBrightness();

				if (cStud < blackValueThreshold) {
					numBlackPixels++;
					if (cOpt < blackValueThreshold) {
						counter++;
					} else if (checkEightSurroundingPixels) {
						boolean correctFound = false;
						for (int w2 = w - pixelCheckRange; w2 <= w + pixelCheckRange; w2++) {
							if (correctFound)
								break;
							if (w2 < 0 || w2 >= optImg.getWidth())
								continue;

							for (int h2 = h - pixelCheckRange; h2 <= h + pixelCheckRange; h2++) {
								if (h2 < 0 || h2 >= optImg.getHeight())
									continue;

								cOpt = opt.getColor(w2, h2).getBrightness();
								if (cStud - colorValueSlackRange < cOpt && cOpt < cStud + colorValueSlackRange) {
									correctFound = true;
									counter++;
									break;
								}
							}
						}
					}
				}
			}
		}
		return counter / Math.max(numBlackPixels, 1.0);
	}

	private List<File> getGroundTruthFiles(String directory) {
		File dir = new File(directory);
		List<File> files = new ArrayList<>();
		for (File f : dir.listFiles()) {
			if (f.getName().substring(0, 2).equals("GT")) {
				files.add(f);
			}
		}
		return files;
	}

	private List<File> getSolutionFiles(String directory) {
		File dir = new File(directory);
		if (!dir.exists()) {
			// This happens when all the individuals had too few/many segments, so the
			// generation_x directory does not even exist.
			return new ArrayList<File>();
		}
		return Arrays.asList(dir.listFiles());
	}

	public void updateOptimalFiles() {
		optFiles = getGroundTruthFiles(optFolder);
	}

	public void updateStudentFiles() {
		studFiles = getSolutionFiles(studFolder);
	}

	private void updateImageLists() {
		optImages.clear();
		for (File f : optFiles) {
			optImages.add(new Image(f.toURI().toString(), false)); // true is for background loading
		}
		studImages.clear();
		for (File f : studFiles) {
			studImages.add(new Image(f.toURI().toString(), false)); // true is for background loading
		}
	}
}