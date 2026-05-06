package Graphs;

public class State {
    public FibonacciGraph fibGraph;
    public PascalGraph pascalGraph;
    public Node currentFib;
    public Node currentPascal;
    public long seed;
    public int stepCounter;
    public int totalSteps;
    public Node[] pathHistory;
    public Node[] remoteHistory;
    public long currentMask = 255L;

    public State(long seed) {
        this.seed        = seed;
        this.stepCounter = 0;

        fibGraph = new FibonacciGraph(255);
        pascalGraph = new PascalGraph();

        // dynamic step count
        int raw = 200 + (int)(seed % 56);
        if (raw < 0) raw = 200;
        this.totalSteps = raw;

        pathHistory = new Node[totalSteps + 1];
        remoteHistory = new Node[totalSteps + 1];

        currentFib = fibGraph.StartPoint();
        currentPascal = pascalGraph.StartPoint();
        pathHistory[0] = pascalGraph.StartPoint();
    }

    public void runAll() {
        for (int i = 0; i < totalSteps; i++) {
            nextStep();
        }
    }

    public void nextStep() {
        seed = Hash(seed);

        // === Fibonacci direction via dynamic bitmask ===
        long masked = seed & currentMask;
        int  bitCount = Long.bitCount(masked);
        int  fibChooser = bitCount % 2;

        currentFib = fibGraph.NextPoint(fibChooser, currentFib);
        int fibValue = currentFib.data;

        currentMask = Long.rotateLeft(currentMask, 1) ^ fibValue;
        if (currentMask == 0) currentMask = 255L;

        seed = Hash(seed ^ fibValue);

        // === Pascal direction via dynamic bitmask ===
        long maskedPascal = seed & currentMask;
        int  pascalBitCount = Long.bitCount(maskedPascal);
        int  pascalChooser = pascalBitCount % 2;

        currentPascal = pascalGraph.NextPoint(pascalChooser, currentPascal);

        // === Offset reading (telepathic read) ===
        int r = currentPascal.row;
        int c = currentPascal.col;

        long cReadHash = Hash(seed ^ fibValue);
        int  cRead = (int)(Math.abs(cReadHash) % (r + 1));

        int remoteValue = pascalGraph.calculatePascalValue(r, cRead);

        seed = Hash(seed ^ remoteValue ^ (cRead * 31L) ^ (r * 17L));

        stepCounter++;
        pathHistory[stepCounter] = currentPascal;
        remoteHistory[stepCounter] = new Node(remoteValue, r, cRead);
    }

    public long Hash(long seed) {
        long GOLDEN_RATIO = 0x9E3779B97F4A7C15L;
        long PI_BITS = 0x3243F6A8885A308DL;
        long E_BITS = 0x2A14701F6DE7C26EL;
        long PRIME_1 = 0xBF58476D1CE4E5B9L;
        long PRIME_2 = 0x94D049BB133111EBL;
        long PRIME_3 = 0x85EBCA77C2B2AE63L;

        seed ^= GOLDEN_RATIO;
        seed = (seed ^ (seed >>> 29)) * PRIME_1;
        seed = (seed ^ (seed >>> 17)) * PRIME_2;
        seed ^= PI_BITS;

        for (int i = 1; i <= 64; i++) {
            int shiftAmount = (int)(Math.abs(seed) & 63);
            if (shiftAmount == 0) shiftAmount = 13;
            seed = Long.rotateLeft(seed, shiftAmount);
            seed ^= (E_BITS * i);
            long temp = seed >>> (i % 17 + 1);
            seed = seed + (temp * PRIME_3);

            long leftHalf = seed & 0xFFFFFFFF00000000L;
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