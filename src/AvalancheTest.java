import Algorithm.GraphPathfinder;
import Algorithm.Hash;
import Algorithm.State;

public class AvalancheTest {

    private static final int PAIRS = 100;

    public static void main(String[] args) {
        System.out.println("=== PF-GKD Avalanche Statistical Test ===");
        System.out.println("Pairs: " + PAIRS + "  |  1-bit difference per pair  |  64-bit output key");
        System.out.println("-----------------------------------------");

        long id = 217389790;
        long baseSeed = 9876 ;
        System.out.println("id: " + id);
        System.out.println("baseSeed: " + baseSeed);
        long seed = baseSeed ^ id;
        System.out.println("seed: " + seed);
        long totalBits = 0;
        int minBits = 64;
        int maxBits = 0;
        int[] counts = new int[65];

        for (int i = 0; i < PAIRS; i++) {
            long seedA = Math.abs(Hash.Hash(baseSeed ^ i));
            long seedB = seedA ^ (1L << (i % 64));

            State stateA = new State(seedA, GraphPathfinder.Strategy.DIJKSTRA);
            State stateB = new State(seedB, GraphPathfinder.Strategy.DIJKSTRA);
            stateA.runAll();
            stateB.runAll();

            int bits = Long.bitCount(stateA.seed ^ stateB.seed);
            counts[bits]++;
            totalBits += bits;
            if (bits < minBits) minBits = bits;
            if (bits > maxBits) maxBits = bits;

            if ((i + 1) % 100 == 0)
                System.out.println("Progress: " + (i + 1) + "/" + PAIRS);
        }

        double avg = (double) totalBits / PAIRS;
        double pct = avg / 64.0 * 100.0;

        System.out.println("-----------------------------------------");
        System.out.printf("Avg bits changed : %.2f / 64%n", avg);
        System.out.printf("Avg %% changed    : %.1f%%%n", pct);
        System.out.println("Min bits changed : " + minBits);
        System.out.println("Max bits changed : " + maxBits);
        System.out.println("-----------------------------------------");
        System.out.println(avg >= 28 && avg <= 36 ? "PASS - Avalanche effect confirmed." : "WARN - Avg outside expected range.");
    }
}
