package moea;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import moea.App.PixelDirection;

public class MST {
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

    private int[] primMST(int startingNode) {
        int parents[] = new int[V];
        double keys[] = new double[V];

        List<Integer> visitedNodes = new ArrayList<>();
        Set<Integer> unvisitedNodes = new HashSet<>();
        for (int i = 0; i < V; i++) {
            keys[i] = Double.POSITIVE_INFINITY;
            unvisitedNodes.add(i);
        }

        keys[startingNode] = 0.0;
        parents[startingNode] = -1;

        while (!unvisitedNodes.isEmpty()) {
            // Extract min node from the graph of unvisited nodes
            // Note that we should this EXTRACT_MIN function more efficienly if we are to
            // optimize the code. Maybe with a binary heap or a Fibonacci heap.
            int nextNode = -1;
            double min = Double.POSITIVE_INFINITY;
            for (Integer i : unvisitedNodes) {
                if (keys[i] < min) {
                    min = keys[i];
                    nextNode = i;
                }
            }

            visitedNodes.add(nextNode);
            unvisitedNodes.remove(nextNode);

            for (int n = 0; n < this.neighborArrays[nextNode].length; n++) {
                int neighborNode = this.neighborArrays[nextNode][n];
                if (unvisitedNodes.contains(neighborNode)) {
                    double edgeWeight = edgeValues[nextNode][n];
                    if (edgeWeight < keys[neighborNode]) {
                        parents[neighborNode] = nextNode;
                        keys[neighborNode] = edgeWeight;
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
        return parents;
    }

    PixelDirection[] findDirections(int startingNode) {
        int[] parents = primMST(startingNode);

        PixelDirection[] directions = new PixelDirection[this.V];
        for (int i = 0; i < parents.length; i++) {
            int parentNode = parents[i];

            // Dumb find index method, fix later
            int x = -1;
            for (int k = 0; k < this.neighborArrays[i].length; k++) {
                if (this.neighborArrays[i][k] == parentNode) {
                    x = k;
                }
            }
            // int x = Arrays.asList(this.neighborArrays[i]).indexOf(parentNode);
            directions[i] = PixelDirection.values()[x];
        }
        return directions;
    }
}
