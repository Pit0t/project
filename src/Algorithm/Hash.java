package Algorithm;

public class Hash {
    public static long Hash(long seed) {
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
            if (shiftAmount == 0)
                shiftAmount = (int)((Math.abs(seed ^ PRIME_3) % 60) + 3);
            seed = Long.rotateLeft(seed, shiftAmount);
            seed ^= (E_BITS * i);
            long temp = seed >>> (i % 17 + 1);
            seed  = seed + (temp * PRIME_3);
            long leftHalf = seed & 0xFFFFFFFF00000000L;
            long rightHalf = seed & 0x00000000FFFFFFFFL;
            seed ^= (leftHalf >>> 32) | (rightHalf << 32);
            if (i % 3 == 0)
                seed = ~seed;
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
