package Graphs;

public class State {
    public FibonacciGraph fibGraph;
    public PascalGraph pascalGraph;
    public Node currentFib;
    public Node currentPascal;
    public long seed;
    public int stepCounter;
    public Node[] pathHistory;
    public long currentMask;
    public Node[] remoteHistory;


    public State(long seed) {
        this.seed = seed;
        stepCounter = 0;
        fibGraph = new FibonacciGraph(255);
        pascalGraph = new PascalGraph();
        currentFib = fibGraph.StartPoint();
        currentPascal = pascalGraph.StartPoint();
        pathHistory = new Node[256];
        pathHistory[0] = pascalGraph.StartPoint();
        this.currentMask = 255L; // 11111111 - bin
        remoteHistory = new Node[256];
    }

    public void nextStep() {
        seed = Hash(seed);

        long masked = seed & currentMask;
        int bitCount = Long.bitCount(masked);
        int fibChooser = bitCount % 2;

        this.currentFib = fibGraph.NextPoint(fibChooser, this.currentFib);
        int fibValue = this.currentFib.data;

        currentMask = Long.rotateLeft(currentMask, 1) ^ fibValue;
        if (currentMask == 0) currentMask = 255L; // prevent dead state

        seed = Hash(seed ^ fibValue);

        long maskedPascal = seed & currentMask;
        int pascalBitCount = Long.bitCount(maskedPascal);
        int pascalChooser = pascalBitCount % 2;

        this.currentPascal = pascalGraph.NextPoint(pascalChooser, this.currentPascal);

        int r = this.currentPascal.row;
        int c = this.currentPascal.col;
        long cReadHash = Hash(seed ^ fibValue);
        int cRead = (int)(cReadHash % (r + 1));
        int remoteValue = pascalGraph.calculatePascalValue(r, cRead);
        seed = Hash(seed ^ remoteValue);

        this.stepCounter++;
        this.pathHistory[this.stepCounter] = this.currentPascal;
        this.remoteHistory[this.stepCounter] = new Node(remoteValue, r, cRead);
    }

    public long Hash(long seed) {

        long GOLDEN_RATIO = 0x9E3779B97F4A7C15L;
        long PI_BITS      = 0x3243F6A8885A308DL;
        long E_BITS       = 0x2A14701F6DE7C26EL;
        long PRIME_1      = 0xBF58476D1CE4E5B9L;
        long PRIME_2      = 0x94D049BB133111EBL;
        long PRIME_3      = 0x85EBCA77C2B2AE63L;

        seed ^= GOLDEN_RATIO;
        seed = (seed ^ (seed >>> 29)) * PRIME_1;
        seed = (seed ^ (seed >>> 17)) * PRIME_2;
        seed ^= PI_BITS;

        for (int i = 1; i <= 64; i++) {


            int shiftAmount = (int) (Math.abs(seed) & 63);
            if (shiftAmount == 0) shiftAmount = 13;

            seed = Long.rotateLeft(seed, shiftAmount);

            seed ^= (E_BITS * i);

            long temp = seed >>> (i % 17 + 1);
            seed = seed + (temp * PRIME_3);

            long leftHalf  = seed & 0xFFFFFFFF00000000L;
            long rightHalf = seed & 0x00000000FFFFFFFFL;
            seed ^= (leftHalf >>> 32) | (rightHalf << 32);

            if (i % 3 == 0) {
                seed = ~seed;
            }

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