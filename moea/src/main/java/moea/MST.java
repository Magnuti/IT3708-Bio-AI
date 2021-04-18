package moea;

import java.util.Arrays;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

import moea.App.PixelDirection;

public class MST {
    class Pixel implements Comparable<Pixel> {
        final int index;
        Pixel parent;
        double key;

        Pixel(int index) {
            this.index = index;
            this.key = Double.POSITIVE_INFINITY;
        }

        @Override
        public int compareTo(Pixel other) {
            return Double.compare(this.key, other.key);
        }
    }

    final double[][] edgeValues;
    final int[][] neighborArrays;
    final int V;

    /**
     * Used to compute a minimum spanning tree with Prim's algorithm.
     * 
     * @param adjacencyMatrix which is
     */
    public MST(double[][] edgeValues, int[][] neighborArrays) {
        this.edgeValues = edgeValues;
        this.neighborArrays = neighborArrays;
        this.V = edgeValues.length;
    }

    private Pixel[] primMST(int startingNode) {
        Pixel[] pixels = new Pixel[this.V];
        for (int i = 0; i < this.V; i++) {
            pixels[i] = new Pixel(i);
        }

        Set<Pixel> visitedNodes = new HashSet<>();
        // TODO find out if we can use a Fibonacci heap here istead, as the removing
        // TODO and re-insertion is pretty expensive on the heap which PriorityQueue
        // TODO uses.
        PriorityQueue<Pixel> unvisitedNodes = new PriorityQueue<>(Arrays.asList(pixels));

        pixels[startingNode].key = 0.0;
        // We need to remove and re-insert to update the PriorityQueue
        unvisitedNodes.remove(pixels[startingNode]);
        unvisitedNodes.add(pixels[startingNode]);

        while (!unvisitedNodes.isEmpty()) {
            Pixel nextNode = unvisitedNodes.remove();
            visitedNodes.add(nextNode);

            for (int n = 0; n < this.neighborArrays[nextNode.index].length; n++) {
                int neighborNodeIndex = this.neighborArrays[nextNode.index][n];
                if (neighborNodeIndex == -1) {
                    continue;
                }
                Pixel neighborNode = pixels[neighborNodeIndex];
                // We use a set here because .contains is O(1) on sets, which results in a
                // massive speedup
                if (!visitedNodes.contains(neighborNode)) {
                    double edgeWeight = edgeValues[nextNode.index][n];
                    if (edgeWeight < neighborNode.key) {
                        neighborNode.parent = nextNode;
                        neighborNode.key = edgeWeight;

                        // We need to remove and re-insert to update the PriorityQueue
                        unvisitedNodes.remove(neighborNode);
                        unvisitedNodes.add(neighborNode);
                    }
                }
            }

        }

        // System.out.println("Started from node " + startingNode);
        // System.out.println("From - to \tWeight");
        // for (int i = 0; i < 10; i++) {
        // int toIndex = visitedNodes.get(i);
        // int fromIndex = parents[toIndex];
        // if (fromIndex != -1) {
        // System.out.println(fromIndex + " - " + toIndex + "\t\t" +
        // edgeValues.get(fromIndex).get(toIndex));
        // }
        // }
        // return parents;
        return pixels;
    }

    PixelDirection[] findDirections(int startingNode) {
        Pixel[] pixels = primMST(startingNode);

        PixelDirection[] directions = new PixelDirection[this.V];
        for (int i = 0; i < pixels.length; i++) {
            Pixel parentNode = pixels[i].parent;
            if (parentNode == null) {
                // Happens for the starting node
                directions[i] = PixelDirection.NONE;
                continue;
            }

            // Dumb find index method, fix later
            int directionIndex = -1;
            for (int k = 0; k < this.neighborArrays[i].length; k++) {
                if (this.neighborArrays[i][k] == parentNode.index) {
                    directionIndex = k;
                    break;
                }
            }

            // Make it such that each pixel points to its parent
            directions[i] = PixelDirection.values()[directionIndex];
        }
        return directions;
    }
}
