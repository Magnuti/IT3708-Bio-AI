# Project 3 - Image Segmentation With a Multiobjective Evolutionary Algorithm

In this project we implement both a simple genetic algorithm (as in the two previous projects) and a **multiobjective evolutionary algorithm (MOEA)** for color image segmentation. For the SGA we use a weighted-sum fitness function, while for the MOEA we use [Non-dominated Sorting Genetic Algorithm II (NSGA-II)](https://ieeexplore.ieee.org/document/996017).

## Getting started

Put a folder with some images in the `training_images/` directory. Out of theese images one has to be named "Test image.jpg" while the ground truth images must start with "GT\_".

## How to run in VS Code

Open the `./mdvrp` folder as a workspace and run the `App.java` file. Note that it must be run from this workspace for relative paths to work.

## Output images

All images are saved as the algorithm runs. When we find out that all images have a lower score than the baseline, that entire directory is deleted along with the images. The generation images have format generation_x_y_z, where x is the type, y is the individ number and z is the number of segments in that image.
