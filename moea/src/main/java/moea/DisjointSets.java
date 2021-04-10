package moea;

// https://www.geeksforgeeks.org/disjoint-set-data-structures/

// TODO see if we can optimize the disjoint set with path compression or union-by-rank

public class DisjointSets {
    int[] rank, parent;
    int n;

    // Constructor
    public DisjointSets(int n) {
        rank = new int[n];
        parent = new int[n];
        this.n = n;

        // Creates n sets with single item in each
        for (int i = 0; i < n; i++) {
            // Initially, all elements are in their own set.
            parent[i] = i;
        }
    }

    // Returns representative of x's set
    int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]);
        }

        return parent[x];
    }

    // Unites the set that includes x and the set that includes x
    void union(int x, int y) {
        // Find representatives of two sets
        int xRoot = find(x), yRoot = find(y);

        // Elements are in the same set, no need to unite anything.
        if (xRoot == yRoot)
            return;

        if (rank[xRoot] < rank[yRoot]) {
            // If x's rank is less than y's rank, then move x under y so that depth of tree
            // remains less
            parent[xRoot] = yRoot;
        } else if (rank[yRoot] < rank[xRoot]) {
            // Else if y's rank is less than x's rank, then move y under x so that depth of
            // tree remains less
            parent[yRoot] = xRoot;
        } else {
            // If ranks are the same, then move y under x (doesn't matter which one goes
            // where)
            parent[yRoot] = xRoot;

            // And increment the result tree's rank by 1
            rank[xRoot] = rank[xRoot] + 1;
        }
    }
}
