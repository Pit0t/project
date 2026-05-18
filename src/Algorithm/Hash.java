package Algorithm;

public class Hash {
    public static long Hash(long seed) {
        int[] primes = {
                17,19,23,29,31,37,41,43,47,53,
                59,61,67,71,73,79,83,89,97,101,
                103,107,109,113,127,131,137,139,
                149,151, 157,163,167,173,179,181,
                191,193,197,199};

        long GOLDEN_RATIO = 0x9E3779B97F4A7C15L;
        long PI_BITS = 0x3243F6A8885A308DL;
        long E_BITS = 0x2A14701F6DE7C26EL;
        long PRIME_1 = 0xBF58476D1CE4E5B9L;
        long PRIME_2 = 0x94D049BB133111EBL;
        long PRIME_3 = 0x85EBCA77C2B2AE63L;

        seed ^= GOLDEN_RATIO;
        seed = (seed ^ (seed >>> primes[(int)(Math.abs(seed) % primes.length)])) * PRIME_1;
        seed = (seed ^ (seed >>> primes[(int)(Math.abs(seed) % primes.length)])) * PRIME_2;
        seed ^= PI_BITS;

        for (int i = 1; i <= 64; i++) {
            int shiftAmount = (int)(Math.abs(seed) & 63);
            if (shiftAmount == 0)
                shiftAmount = (int)((Math.abs(seed ^ PRIME_3) % 60) + 3);
            seed = Long.rotateLeft(seed, shiftAmount);
            seed ^= (E_BITS * i);
            long temp = seed >>> primes[(int)((Math.abs(seed) ^ i) % primes.length)];
            seed  = seed + (temp * PRIME_3);
            long leftHalf = seed & 0xFFFFFFFF00000000L;
            long rightHalf = seed & 0x00000000FFFFFFFFL;
            seed ^= (leftHalf >>> 32) | (rightHalf << 32);
            if (i % 3 == 0)
                seed = ~seed;
            seed *= (seed | 1L);
            seed ^= (seed >>> primes[(int)((Math.abs(seed) ^ i) % primes.length)]);
        }

        seed ^= seed >>> 33;
        seed *= 0xff51afd7ed558ccdL;
        seed ^= seed >>> 33;
        seed *= 0xc4ceb9fe1a85ec53L;
        seed ^= seed >>> 33;

        return seed & Long.MAX_VALUE;
    }
}
