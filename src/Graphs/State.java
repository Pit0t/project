package Graphs;

public class State {
    public FibonacciGraph fibGraph;
    public PascalGraph pascalGraph;
    public Node currentFib;
    public Node currentPascal;
    public long seed;
    public int stepCounter;


    public State(long seed){
        this.seed = seed;
        stepCounter = 0;
        fibGraph = new FibonacciGraph(255);
        pascalGraph = new PascalGraph();
        currentFib = fibGraph.StartPoint();
        currentPascal = pascalGraph.StartPoint();
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
        stepCounter++;


    }

    public long Hash(long seed){
        int modolus = 36389;
        long big = 104729;
        int generator = 341;
        for (int i = 0; i<256;i++){
            seed = seed ^ (seed<<16);
            for(int j = 0; j < generator; j++){
                seed = seed + (modolus * big);
                seed = seed * big;

            }
            seed = seed ^ (seed>>>7);
        }
        return seed;
    }
}
