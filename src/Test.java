import Algorithm.GraphPathfinder;
import Algorithm.State;

public class Test {

    public static void main(String[] args) {

        long seed = 69L;

        System.out.println("========================================");
        System.out.println("  PF-GKD Key Derivation Test");
        System.out.println("========================================");
        System.out.println("Seed: " + seed);
        System.out.println();

        // ── Run 1: Dijkstra ───────────────────────────────────────────────────
        State dijkstra = new State(seed, GraphPathfinder.Strategy.DIJKSTRA);
        dijkstra.runAll();

        System.out.println("--- DIJKSTRA ---");
        System.out.println("Target node:  row=" + dijkstra.targetNode.row + "  col=" + dijkstra.targetNode.col);
        System.out.println("Path length:  " + dijkstra.totalSteps + " steps");
        System.out.println("Derived key:  " + dijkstra.seed);
        System.out.println("Key (hex):    " + Long.toHexString(dijkstra.seed).toUpperCase());
        System.out.println();

        // ── Run 2: DFS ────────────────────────────────────────────────────────
        State dfs = new State(seed, GraphPathfinder.Strategy.DFS);
        dfs.runAll();

        System.out.println("--- DFS ---");
        System.out.println("Target node:  row=" + dfs.targetNode.row + "  col=" + dfs.targetNode.col);
        System.out.println("Path length:  " + dfs.totalSteps + " steps");
        System.out.println("Derived key:  " + dfs.seed);
        System.out.println("Key (hex):    " + Long.toHexString(dfs.seed).toUpperCase());
        System.out.println();

        // ── Compare ───────────────────────────────────────────────────────────
        System.out.println("--- COMPARISON ---");
        System.out.println("Same target?  " + (dijkstra.targetNode.col == dfs.targetNode.col));
        System.out.println("Same key?     " + (dijkstra.seed == dfs.seed));
        System.out.println("Keys differ:  " + (dijkstra.seed != dfs.seed ? "YES (path-dependent)" : "NO "));
        System.out.println();

        // ── Determinism check: run Dijkstra again with same seed ──────────────
        State dijkstra2 = new State(seed, GraphPathfinder.Strategy.DIJKSTRA);
        dijkstra2.runAll();

        System.out.println("--- DETERMINISM CHECK ---");
        System.out.println("Run 1 key:    " + Long.toHexString(dijkstra.seed).toUpperCase());
        System.out.println("Run 2 key:    " + Long.toHexString(dijkstra2.seed).toUpperCase());
        System.out.println("Deterministic? " + (dijkstra.seed == dijkstra2.seed ? "YES ✓" : "NO ✗"));
        System.out.println();

        // ── Avalanche check: change seed by 1 bit ─────────────────────────────
        State dijkstra3 = new State(seed + 1, GraphPathfinder.Strategy.DIJKSTRA);
        dijkstra3.runAll();

        System.out.println("--- AVALANCHE CHECK (seed + 1) ---");
        System.out.println("Original key: " + Long.toHexString(dijkstra.seed).toUpperCase());
        System.out.println("Modified key: " + Long.toHexString(dijkstra3.seed).toUpperCase());
        System.out.println("Keys differ?  " + (dijkstra.seed != dijkstra3.seed ? "YES ✓" : "NO ✗"));
        System.out.println();

        // ── Print the actual path ─────────────────────────────────────────────
        System.out.println("--- DIJKSTRA PATH (first 10 steps) ---");
        for (int i = 0; i <= Math.min(10, dijkstra.totalSteps); i++) {
            if (dijkstra.pathHistory[i] != null) {
                Graphs.Node n = dijkstra.pathHistory[i];
                System.out.println("  step " + i + ": row=" + n.row + "  col=" + n.col + "  val=" + n.data);
            }
        }

        System.out.println();
        System.out.println("--- DFS PATH (first 10 steps) ---");
        for (int i = 0; i <= Math.min(10, dfs.totalSteps); i++) {
            if (dfs.pathHistory[i] != null) {
                Graphs.Node n = dfs.pathHistory[i];
                System.out.println("  step " + i + ": row=" + n.row + "  col=" + n.col + "  val=" + n.data);
            }
        }

        System.out.println();
        System.out.println("Nodes created in DAG (Dijkstra): " + dijkstra.pascalDAG.getCacheSize());
        System.out.println("Nodes created in DAG (DFS):      " + dfs.pascalDAG.getCacheSize());
        System.out.println("========================================");



        System.out.println("--- WORMHOLE CHECK ---");
        int wormholeCount = 0;
        for (int i = 0; i <= dijkstra.totalSteps; i++) {
            if (dijkstra.pathHistory[i] != null && dijkstra.pathHistory[i].isWormhole) {
                wormholeCount++;
                Graphs.Node n = dijkstra.pathHistory[i];
                System.out.println("  Wormhole at step " + i + ": row=" + n.row + " col=" + n.col);
            }
        }
        System.out.println("Total wormholes on path: " + wormholeCount);
        System.out.println("Total nodes in DAG: " + dijkstra.pascalDAG.getCacheSize());


        System.out.println("--- WORMHOLE CHECK (DFS) ---");
        wormholeCount = 0;
        for (int i = 0; i <= dfs.totalSteps; i++) {
            if (dfs.pathHistory[i] != null && dfs.pathHistory[i].isWormhole) {
                wormholeCount++;
                Graphs.Node n = dfs.pathHistory[i];
                System.out.println("  Wormhole at step " + i + ": row=" + n.row + " col=" + n.col);
            }
        }
        System.out.println("Total wormholes on DFS path: " + wormholeCount);
    }
}