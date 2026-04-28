package Graphs;

public class State {
    public FibonacciGraph fibGraph;
    public PascalGraph pascalGraph;
    public Node currentFib;
    public Node currentPascal;
    public long seed;
    public int stepCounter;
    public Node[] pathHistory;


    public State(long seed){
        this.seed = seed;
        stepCounter = 0;
        fibGraph = new FibonacciGraph(255);
        pascalGraph = new PascalGraph();
        currentFib = fibGraph.StartPoint();
        currentPascal = pascalGraph.StartPoint();
        pathHistory = new Node[256];
        pathHistory[0] = pascalGraph.StartPoint();
    }

    public void nextStep(){
        seed = Hash(seed);
        int fibChooser = (int)(seed & 1);
        this.currentFib = fibGraph.NextPoint(fibChooser, this.currentFib);
        int fibValue = this.currentFib.data;
        long combinedSeed = this.seed ^ fibValue;
        this.seed = Hash(combinedSeed);
        int pascalChooser = (int)(this.seed & 1);
        this.currentPascal = pascalGraph.NextPoint(pascalChooser, this.currentPascal);
        this.stepCounter++;
        this.pathHistory[this.stepCounter] = this.currentPascal;


    }

    public long Hash(long seed){
        int modolus = 36389;
        long big = 104729;
        long magicNumber = 0x9E3779B97F4A7C15L;
        int generator = 341;
        for (int i = 0; i<256;i++){
            seed = Long.rotateLeft(seed, 13);
            for(int j = 0; j < generator; j++){
                seed = seed + (modolus * big);
                seed = seed * big;

                seed = seed ^ (seed + magicNumber);
            }
            seed = seed ^ (seed>>>(((Math.abs(seed))%31) + 1));
        }
        return seed;
    }
}
