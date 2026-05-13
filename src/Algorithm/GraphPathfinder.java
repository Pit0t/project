package Algorithm;

import Graphs.Edge;
import Graphs.Node;
import Graphs.PascalDAG;

import java.util.*;

// Two pathfinding strategies on the Pascal DAG:
//   DIJKSTRA - finds the cheapest path by Fibonacci edge weights
//   DFS      - finds the first path going depth-first, left child first
//
// Same seed + Dijkstra vs DFS = different paths = different derived keys.
// This demonstrates that key derivation is path-dependent.
public class GraphPathfinder {

    public enum Strategy { DIJKSTRA, DFS }

    private final PascalDAG dag;

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
        String rootKey = nodeKey(root);

        dist.put(rootKey, 0L);
        pq.offer(new long[]{ 0L, root.row, root.col });

        while (!pq.isEmpty()) {
            long[] curr = pq.poll();
            int currRow = (int) curr[1];
            int currCol = (int) curr[2];
            Node currNode = dag.getNode(currRow, currCol);
            if (currNode == null) continue;

            String currKey = nodeKey(currNode);
            if (visited.contains(currKey)) continue;
            visited.add(currKey);

            if (currRow == target.row && currCol == target.col)
                return reconstructPath(prev, target);

            for (Edge edge : dag.getNeighbors(currNode)) {
                String nKey = nodeKey(edge.target);
                if (visited.contains(nKey)) continue;

                long newCost = curr[0] + edge.weight;
                if (newCost < dist.getOrDefault(nKey, Long.MAX_VALUE)) {
                    dist.put(nKey, newCost);
                    prev.put(nKey, edge);
                    pq.offer(new long[]{ newCost, edge.target.row, edge.target.col });
                }
            }
        }

        return new ArrayList<>();
    }

    // -------------------------------------------------------------------
    // DFS - depth first, left child preferred
    // Ignores edge weights entirely - takes the first path it finds
    // -------------------------------------------------------------------
    private List<Node> dfs(Node target) {
        Deque<Object[]> stack = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();

        Node root = dag.getRoot();
        List<Node> rootPath = new ArrayList<>();
        rootPath.add(root);
        stack.push(new Object[]{ root, rootPath });

        while (!stack.isEmpty()) {
            Object[] frame = stack.pop();
            Node curr = (Node) frame[0];
            @SuppressWarnings("unchecked")
            List<Node> currPath = (List<Node>) frame[1];

            String currKey = nodeKey(curr);
            if (visited.contains(currKey)) continue;
            visited.add(currKey);

            if (curr.row == target.row && curr.col == target.col)
                return currPath;

            if (curr.row >= target.row) continue;

            // push right first so left gets popped first (LIFO)
            List<Edge> neighbors = dag.getNeighbors(curr);
            for (int i = neighbors.size() - 1; i >= 0; i--) {
                Edge edge = neighbors.get(i);
                if (!visited.contains(nodeKey(edge.target))) {
                    List<Node> newPath = new ArrayList<>(currPath);
                    newPath.add(edge.target);
                    stack.push(new Object[]{ edge.target, newPath });
                }
            }
        }

        return new ArrayList<>();
    }

    // traces back from target to root using the prev map, then reverses
    private List<Node> reconstructPath(Map<String, Edge> prev, Node target) {
        LinkedList<Node> path    = new LinkedList<>();
        Node             current = target;

        while (current != null) {
            path.addFirst(current);
            Edge e = prev.get(nodeKey(current));
            if (e == null) break;
            current = e.source;
        }

        if (path.isEmpty() || path.getFirst().row != 0)
            path.addFirst(dag.getRoot());

        return new ArrayList<>(path);
    }

    private String nodeKey(Node node) {
        return node.row + "_" + node.col;
    }
}