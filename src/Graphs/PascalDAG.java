package Graphs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Algorithm.Hash;

// Lazy DAG representation of Pascal's triangle.
// Nodes are only created when an algorithm actually visits them,
// Edge weights are pulled from the Fibonacci graph dynamically.
public class PascalDAG {

    public static final int MAX_ROW = 256;

    // cache of already-created nodes, keyed by "row_col"
    private final Map<String, Node> nodeCache = new HashMap<>();

    private final FibonacciGraph fibGraph;
    private Node currentFibNode;

    private long seed;

    public PascalDAG(FibonacciGraph fibGraph, long seed) {
        this.fibGraph = fibGraph;
        this.seed = seed;
        this.currentFibNode = fibGraph.StartPoint();
        nodeCache.put(key(0, 0), new Node(1, 0, 0));
    }

    // returns an existing node or creates one on the spot
    public Node getNode(int row, int col) {
        if (col < 0 || col > row || row >= MAX_ROW)
            return null;

        String k = key(row, col);
        if (nodeCache.containsKey(k))
            return nodeCache.get(k);

        Node node = new Node(computePascalValue(row, col), row, col);
        long wormholeCheck = Hash.Hash(seed ^ (long)row ^ (long)col);
        long dynamicPrecent = 5 + (Math.abs(Hash.Hash(seed)) % 16);

        if (Math.abs(wormholeCheck) % 100 < dynamicPrecent) {
            node.isWormhole = true;
        }
        nodeCache.put(k, node);
        return node;
    }

    // returns the two children of a node as edges with Fibonacci-based weights
    public List<Edge> getNeighbors(Node node) {
        List<Edge> edges = new ArrayList<>();
        int nextRow = node.row + 1;
        if (nextRow >= MAX_ROW) return edges;

        // always add normal children
        Node left  = getNode(nextRow, node.col);
        Node right = getNode(nextRow, node.col + 1);
        long fibWeight = nextFibWeight();
        long wLeft  = Math.abs(Hash.Hash(seed ^ nextRow ^ node.col) ^ fibWeight) % 1000000007;
        long wRight = Math.abs(Hash.Hash(seed ^ nextRow ^ (node.col + 1)) ^ fibWeight) % 1000000007;
        if (left  != null) edges.add(new Edge(node, left,  wLeft));
        if (right != null) edges.add(new Edge(node, right, wRight));

        // wormhole adds an EXTRA backwards jump edge (doesn't replace children)
        if (node.isWormhole) {
            long jumpHash = Hash.Hash(seed ^ node.row ^ node.col);
            int jumpRow = (int)(Math.abs(jumpHash) % (node.row + 1));
            int jumpCol = (int)(Math.abs(Hash.Hash(jumpHash)) % (jumpRow + 1));
            Node jumpTarget = getNode(jumpRow, jumpCol);
            if (jumpTarget != null)
                edges.add(new Edge(node, jumpTarget, nextFibWeight()));
        }

        return edges;
    }

    // hashes the seed first so similar seeds land at very different targets
    public Node getTargetNode(long seed, int targetRow) {
        long hashed = Hash.Hash(seed);
        int col = (int)(Math.abs(hashed) % (targetRow + 1));
        return getNode(targetRow, col);
    }

    public Node getRoot() {
        return nodeCache.get(key(0, 0));
    }

    public int getCacheSize() {
        return nodeCache.size();
    }

    // C(n, k) computed iteratively, kept bounded with mod
    public int computePascalValue(int n, int k) {
        if (k == 0 || k == n)
            return 1;
        if (k < 0  || k > n)
            return 0;
        if (k > n / 2)
            k = n - k;

        long result = 1;
        for (int i = 1; i <= k; i++) {
            result = result * (n - i + 1) / i;
            result = result % 1000000007;
        }
        return (int) result;
    }

    private long nextFibWeight() {
        currentFibNode = fibGraph.NextPoint(0, currentFibNode);
        return currentFibNode.data;
    }

    private String key(int row, int col) {
        return row + "_" + col;
    }

}