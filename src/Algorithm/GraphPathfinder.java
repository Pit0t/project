package Algorithm;

import Graphs.Edge;
import Graphs.Node;
import Graphs.PascalDAG;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

// Two pathfinding strategies on the Pascal DAG:
//   DIJKSTRA - finds the cheapest path by Fibonacci edge weights
//   DFS      - finds the first path going depth-first, left child first
//
// Same seed + Dijkstra vs DFS = different paths = different derived keys.
// This demonstrates that key derivation is path-dependent.
public class GraphPathfinder {

    public enum Strategy { DIJKSTRA, DFS }

    private final PascalDAG dag;

    private static final long ZIGZAG_PENALTY = 450_000_000L;

    public GraphPathfinder(PascalDAG dag) {
        this.dag = dag;
    }

    public List<Node> findPath(Node target, Strategy strategy) {
        if (strategy == Strategy.DIJKSTRA)
            return dijkstra(target);
        return dfs(target);
    }

    // -------------------------------------------------------------------
    // Dijkstra - minimum cost path using a priority queue
    // Cost of each edge = its Fibonacci-based weight
    // -------------------------------------------------------------------
    private List<Node> dijkstra(Node target) {
        Map<String, Long> dist = new HashMap<>();
        Map<String, Edge> prev = new HashMap<>();
        Set<String> visited = new HashSet<>();
        PriorityQueue<long[]> pq = new PriorityQueue<>(Comparator.comparingLong(a -> a[0]));

        Node root = dag.getRoot();
        dist.put(nodeKey(root), 0L);
        pq.offer(new long[]{0L, root.row, root.col, -1L});
        // -1 means no previous direction yet

        List<Node> result = new ArrayList<>();
        while (!pq.isEmpty() && result.isEmpty()) {
            long[] curr = pq.poll();
            result = processDijkstraNode(curr, target, dist, prev, visited, pq);
        }
        return result;
    }

    private List<Node> processDijkstraNode(long[] curr, Node target, Map<String, Long> dist, Map<String, Edge> prev,
                                           Set<String> visited,
                                           PriorityQueue<long[]> pq)
    {
        int currRow = (int) curr[1];
        int currCol = (int) curr[2];
        int prevCol = (int) curr[3];
        Node currNode = dag.getNode(currRow, currCol);

        if (currNode == null) return new ArrayList<>();

        String currKey = nodeKey(currNode);
        if (visited.contains(currKey)) return new ArrayList<>();

        visited.add(currKey);

        if (currRow == target.row && currCol == target.col)
            return reconstructPath(prev, target);

        relaxEdges(currNode, curr[0], prevCol, dist, prev, visited, pq);
        return new ArrayList<>();
    }

    private void relaxEdges(Node currNode, long currCost, int prevCol,
                            Map<String, Long> dist,
                            Map<String, Edge> prev,
                            Set<String> visited,
                            PriorityQueue<long[]> pq) {
        for (Edge edge : dag.getNeighbors(currNode)) {
            String nKey = nodeKey(edge.target);
            if (!visited.contains(nKey)) {
                boolean sameDirection = (prevCol != -1) &&
                        ((edge.target.col == currNode.col && prevCol == currNode.col) ||
                                (edge.target.col == currNode.col + 1 && prevCol == currNode.col - 1));

                long penalty = sameDirection ? ZIGZAG_PENALTY : 0L;
                long newCost = currCost + edge.weight + penalty;
                if (newCost < dist.getOrDefault(nKey, Long.MAX_VALUE)) {
                    dist.put(nKey, newCost);
                    prev.put(nKey, edge);
                    pq.offer(new long[]{newCost, edge.target.row, edge.target.col, currNode.col});
                }
            }
        }
    }

    // -------------------------------------------------------------------
    // DFS - depth first, left child preferred
    // Ignores edge weights entirely - takes the first path it finds
    // -------------------------------------------------------------------
    private static class DFSFrame {
        Node node;
        List<Node> path;
        DFSFrame(Node n, List<Node> p) { node = n; path = p; }
    }

    private List<Node> dfs(Node target) {
        Deque<DFSFrame> stack = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();

        Node root = dag.getRoot();
        List<Node> rootPath = new ArrayList<>();
        rootPath.add(root);
        stack.push(new DFSFrame(root, rootPath));

        List<Node> result = new ArrayList<>();
        while (!stack.isEmpty() && result.isEmpty()) {
            DFSFrame frame = stack.pop();
            result = processDFSFrame(frame, target, visited, stack);
        }
        return result;
    }

    private List<Node> processDFSFrame(DFSFrame frame, Node target,
                                       Set<String> visited,
                                       Deque<DFSFrame> stack) {
        Node curr = frame.node;
        List<Node> currPath = frame.path;
        String currKey = nodeKey(curr);

        if (visited.contains(currKey)) return new ArrayList<>();

        visited.add(currKey);

        if (curr.row == target.row && curr.col == target.col)
            return currPath;

        if (curr.row < target.row)
            pushNeighbors(curr, currPath, visited, stack);

        return new ArrayList<>();
    }

    private void pushNeighbors(Node curr, List<Node> currPath,
                               Set<String> visited,
                               Deque<DFSFrame> stack) {
        List<Edge> neighbors = dag.getNeighbors(curr);
        for (int i = neighbors.size() - 1; i >= 0; i--) {
            Edge edge = neighbors.get(i);
            // skip backwards/same-level edges in DFS — only go deeper
            if (edge.target.row > curr.row && !visited.contains(nodeKey(edge.target))) {
                List<Node> newPath = new ArrayList<>(currPath);
                newPath.add(edge.target);
                stack.push(new DFSFrame(edge.target, newPath));
            }
        }
    }

    // traces back from target to root using the prev map, then reverses
    private List<Node> reconstructPath(Map<String, Edge> prev, Node target) {
        LinkedList<Node> path = new LinkedList<>();
        Node current = target;

        while (current != null) {
            path.addFirst(current);
            Edge e = prev.get(nodeKey(current));
            if (e == null) {
                current = null;
            } else {
                current = e.source;
            }
        }

        if (path.isEmpty() || path.getFirst().row != 0)
            path.addFirst(dag.getRoot());

        return new ArrayList<>(path);
    }

    private String nodeKey(Node node) {
        return node.row + "_" + node.col;
    }
}
