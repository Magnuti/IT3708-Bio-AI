package moea;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MST {
    final List<Map<Integer, Double>> edgeValues;
    final int V;

    /**
     * Used to compute a minimum spanning tree with Prim's algorithm.
     * 
     * @param adjacencyMatrix which is
     */
    public MST(List<Map<Integer, Double>> edgeValues) {
        this.edgeValues = edgeValues;
        this.V = edgeValues.size();
    }

    void primMST(int startingNode) {
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
            int u = -1;
            double min = Double.POSITIVE_INFINITY;

            for (Integer i : unvisitedNodes) {
                if (keys[i] < min) {
                    min = keys[i];
                    u = i;
                }
            }

            visitedNodes.add(u);
            unvisitedNodes.remove(u);

            for (Integer neighborIndex : edgeValues.get(u).keySet()) {
                if (unvisitedNodes.contains(neighborIndex)) {
                    double edgeWeight = edgeValues.get(u).get(neighborIndex);
                    if (edgeWeight < keys[neighborIndex]) {
                        parents[neighborIndex] = u;
                        keys[neighborIndex] = edgeWeight;
                    }
                }
            }

        }

        System.out.println("Started from node " + startingNode);
        System.out.println("From - to \tWeight");
        for (int i = 0; i < 10; i++) {
            int toIndex = visitedNodes.get(i);
            int fromIndex = parents[toIndex];
            if (fromIndex != -1) {
                System.out.println(fromIndex + " - " + toIndex + "\t\t" + edgeValues.get(fromIndex).get(toIndex));
            }
        }
    }
}
