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

    public FibonacciGraph fibGraph;
    public PascalDAG pascalDAG;
    private GraphPathfinder pathfinder;

    public Node currentFib;
    public Node currentPascal;
    public long seed;
    public int stepCounter;
    public int totalSteps;
    public long currentMask = 255L;

    private List<Node> goldenPath;

    public Node[] pathHistory;
    public Node[] remoteHistory;

    public GraphPathfinder.Strategy strategy;
    public Node targetNode;
    public int targetRow;

    public State(long seed, GraphPathfinder.Strategy strategy) {
        this.seed = seed;
        this.strategy = strategy;
        stepCounter = 0;

        fibGraph = new FibonacciGraph(1000);
        pascalDAG = new PascalDAG(fibGraph);
        pathfinder = new GraphPathfinder(pascalDAG);
        currentFib = fibGraph.StartPoint();

        targetNode = null;
        targetRow = (int)(Hash.Hash(seed) % 256);
        if (targetRow < 100)
            targetRow += 150;
        targetNode = pascalDAG.getTargetNode(seed, targetRow);
        goldenPath = pathfinder.findPath(targetNode, strategy);

        totalSteps = Math.max(goldenPath.size() - 1, 1);

        pathHistory = new Node[totalSteps + 1];
        remoteHistory = new Node[totalSteps + 1];

        currentPascal = pascalDAG.getRoot();
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

        int r = currentPascal.row;
        int pasVal = currentPascal.data;

        // mix Pascal value into seed
        seed = Hash.Hash(seed ^ pasVal);

        // advance Fibonacci in sync with the mask
        long masked = seed & currentMask;
        int  fibChooser = Long.bitCount(masked) % 2;
        currentFib = fibGraph.NextPoint(fibChooser, currentFib);
        int fibValue = currentFib.data;

        // evolve mask
        currentMask = Long.rotateLeft(currentMask, 1) ^ fibValue;
        if (currentMask == 0) currentMask = 255L;

        // telepathic read - pick a remote cell in the same row
        long cReadHash = Hash.Hash(seed ^ fibValue);
        int cRead = (int)(Math.abs(cReadHash) % (r + 1));
        int remoteVal = pascalDAG.computePascalValue(r, cRead);

        seed = Hash.Hash(seed ^ remoteVal ^ (cRead * 31L) ^ (r * 17L));

        stepCounter++;
        pathHistory[stepCounter] = currentPascal;
        remoteHistory[stepCounter] = new Node(remoteVal, r, cRead);
    }

}