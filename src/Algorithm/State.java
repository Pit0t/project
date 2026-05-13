package Algorithm;

import Graphs.FibonacciGraph;
import Graphs.Node;
import Graphs.PascalDAG;

import java.util.List;

// The core engine of PF-GKD.
//
// On construction:
//   1. seed determines a target node at the bottom of Pascal's triangle
//   2. pathfinder (Dijkstra or DFS) finds the path to that target
//   3. nextStep() walks the path, hashing each node's value into the seed
//
// After all steps, state.seed is the derived key.
public class State {

    public FibonacciGraph           fibGraph;
    public PascalDAG                pascalDAG;
    private GraphPathfinder         pathfinder;

    public Node                     currentFib;
    public Node                     currentPascal;
    public long                     seed;
    public int                      stepCounter;
    public int                      totalSteps;
    public long                     currentMask = 255L;

    private List<Node>              goldenPath;

    public Node[]                   pathHistory;
    public Node[]                   remoteHistory;

    public GraphPathfinder.Strategy strategy;
    public Node                     targetNode;
    public int                      targetRow;

    public State(long seed, GraphPathfinder.Strategy strategy) {
        this.seed     = seed;
        this.strategy = strategy;
        stepCounter   = 0;

        fibGraph   = new FibonacciGraph(1000);
        pascalDAG  = new PascalDAG(fibGraph);
        pathfinder = new GraphPathfinder(pascalDAG);
        currentFib = fibGraph.StartPoint();

        targetNode = null;
        targetRow = (int)(Hash(seed) % 256);
        if (targetRow < 100)
            targetRow += 150;
        targetNode = pascalDAG.getTargetNode(seed, targetRow);
        goldenPath = pathfinder.findPath(targetNode, strategy);

        totalSteps = Math.max(goldenPath.size() - 1, 1);

        pathHistory   = new Node[totalSteps + 1];
        remoteHistory = new Node[totalSteps + 1];

        currentPascal  = pascalDAG.getRoot();
        pathHistory[0] = currentPascal;
    }

    // defaults to Dijkstra
    public State(long seed) {
        this(seed, GraphPathfinder.Strategy.DIJKSTRA);
    }

    public void runAll() {
        for (int i = 0; i < totalSteps; i++) nextStep();
    }

    public void nextStep() {
        if (stepCounter >= goldenPath.size() - 1) return;

        // move to next node on the pre-computed path
        currentPascal = goldenPath.get(stepCounter + 1);

        int r      = currentPascal.row;
        int pasVal = currentPascal.data;

        // mix Pascal value into seed
        seed = Hash(seed ^ pasVal);

        // advance Fibonacci in sync with the mask
        long masked     = seed & currentMask;
        int  fibChooser = Long.bitCount(masked) % 2;
        currentFib      = fibGraph.NextPoint(fibChooser, currentFib);
        int fibValue    = currentFib.data;

        // evolve mask
        currentMask = Long.rotateLeft(currentMask, 1) ^ fibValue;
        if (currentMask == 0) currentMask = 255L;

        // telepathic read - pick a remote cell in the same row
        long cReadHash = Hash(seed ^ fibValue);
        int  cRead     = (int)(Math.abs(cReadHash) % (r + 1));
        int  remoteVal = pascalDAG.computePascalValue(r, cRead);

        seed = Hash(seed ^ remoteVal ^ (cRead * 31L) ^ (r * 17L));

        stepCounter++;
        pathHistory[stepCounter]   = currentPascal;
        remoteHistory[stepCounter] = new Node(remoteVal, r, cRead);
    }

    public long Hash(long seed) {
        long GOLDEN_RATIO = 0x9E3779B97F4A7C15L;
        long PI_BITS      = 0x3243F6A8885A308DL;
        long E_BITS       = 0x2A14701F6DE7C26EL;
        long PRIME_1      = 0xBF58476D1CE4E5B9L;
        long PRIME_2      = 0x94D049BB133111EBL;
        long PRIME_3      = 0x85EBCA77C2B2AE63L;

        seed ^= GOLDEN_RATIO;
        seed  = (seed ^ (seed >>> 29)) * PRIME_1;
        seed  = (seed ^ (seed >>> 17)) * PRIME_2;
        seed ^= PI_BITS;

        for (int i = 1; i <= 64; i++) {
            int shiftAmount = (int)(Math.abs(seed) & 63);
            if (shiftAmount == 0) shiftAmount = 13;
            seed  = Long.rotateLeft(seed, shiftAmount);
            seed ^= (E_BITS * i);
            long temp      = seed >>> (i % 17 + 1);
            seed  = seed + (temp * PRIME_3);
            long leftHalf  = seed & 0xFFFFFFFF00000000L;
            long rightHalf = seed & 0x00000000FFFFFFFFL;
            seed ^= (leftHalf >>> 32) | (rightHalf << 32);
            if (i % 3 == 0) seed = ~seed;
            seed *= (seed | 1L);
            seed ^= (seed >>> 19);
        }

        seed ^= seed >>> 33;
        seed *= 0xff51afd7ed558ccdL;
        seed ^= seed >>> 33;
        seed *= 0xc4ceb9fe1a85ec53L;
        seed ^= seed >>> 33;

        return seed & Long.MAX_VALUE;
    }
}