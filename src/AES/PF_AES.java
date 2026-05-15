package AES;

import Algorithm.Hash;

// AES block cipher customized for PF-GKD.
// Based on standard AES (StrongAES) but with two key modifications:
//   1. Dynamic S-box: shuffled using the derived key, so substitution
//      table is different every session.
//   2. Dynamic rounds: 10 to 15 rounds based on derived key,
//      so attacker doesn't even know how many rounds were used.
//
// Takes the derived key (long) from State and expands it into round keys.
// Operates on 16-byte (4x4) blocks.
public class PF_AES {

    private int[] Sbox;
    private int[] RSbox;
    private byte[][][] roundKeys;
    private int rounds;

    // standard AES S-box — we shuffle this based on the derived key
    private static final int[] STANDARD_SBOX = {
            0x63,0x7c,0x77,0x7b,0xf2,0x6b,0x6f,0xc5,0x30,0x01,0x67,0x2b,0xfe,0xd7,0xab,0x76,
            0xca,0x82,0xc9,0x7d,0xfa,0x59,0x47,0xf0,0xad,0xd4,0xa2,0xaf,0x9c,0xa4,0x72,0xc0,
            0xb7,0xfd,0x93,0x26,0x36,0x3f,0xf7,0xcc,0x34,0xa5,0xe5,0xf1,0x71,0xd8,0x31,0x15,
            0x04,0xc7,0x23,0xc3,0x18,0x96,0x05,0x9a,0x07,0x12,0x80,0xe2,0xeb,0x27,0xb2,0x75,
            0x09,0x83,0x2c,0x1a,0x1b,0x6e,0x5a,0xa0,0x52,0x3b,0xd6,0xb3,0x29,0xe3,0x2f,0x84,
            0x53,0xd1,0x00,0xed,0x20,0xfc,0xb1,0x5b,0x6a,0xcb,0xbe,0x39,0x4a,0x4c,0x58,0xcf,
            0xd0,0xef,0xaa,0xfb,0x43,0x4d,0x33,0x85,0x45,0xf9,0x02,0x7f,0x50,0x3c,0x9f,0xa8,
            0x51,0xa3,0x40,0x8f,0x92,0x9d,0x38,0xf5,0xbc,0xb6,0xda,0x21,0x10,0xff,0xf3,0xd2,
            0xcd,0x0c,0x13,0xec,0x5f,0x97,0x44,0x17,0xc4,0xa7,0x7e,0x3d,0x64,0x5d,0x19,0x73,
            0x60,0x81,0x4f,0xdc,0x22,0x2a,0x90,0x88,0x46,0xee,0xb8,0x14,0xde,0x5e,0x0b,0xdb,
            0xe0,0x32,0x3a,0x0a,0x49,0x06,0x24,0x5c,0xc2,0xd3,0xac,0x62,0x91,0x95,0xe4,0x79,
            0xe7,0xc8,0x37,0x6d,0x8d,0xd5,0x4e,0xa9,0x6c,0x56,0xf4,0xea,0x65,0x7a,0xae,0x08,
            0xba,0x78,0x25,0x2e,0x1c,0xa6,0xb4,0xc6,0xe8,0xdd,0x74,0x1f,0x4b,0xbd,0x8b,0x8a,
            0x70,0x3e,0xb5,0x66,0x48,0x03,0xf6,0x0e,0x61,0x35,0x57,0xb9,0x86,0xc1,0x1d,0x9e,
            0xe1,0xf8,0x98,0x11,0x69,0xd9,0x8e,0x94,0x9b,0x1e,0x87,0xe9,0xce,0x55,0x28,0xdf,
            0x8c,0xa1,0x89,0x0d,0xbf,0xe6,0x42,0x68,0x41,0x99,0x2d,0x0f,0xb0,0x54,0xbb,0x16
    };

    private static final int[] STANDARD_RSBOX = {
            0x52,0x09,0x6a,0xd5,0x30,0x36,0xa5,0x38,0xbf,0x40,0xa3,0x9e,0x81,0xf3,0xd7,0xfb,
            0x7c,0xe3,0x39,0x82,0x9b,0x2f,0xff,0x87,0x34,0x8e,0x43,0x44,0xc4,0xde,0xe9,0xcb,
            0x54,0x7b,0x94,0x32,0xa6,0xc2,0x23,0x3d,0xee,0x4c,0x95,0x0b,0x42,0xfa,0xc3,0x4e,
            0x08,0x2e,0xa1,0x66,0x28,0xd9,0x24,0xb2,0x76,0x5b,0xa2,0x49,0x6d,0x8b,0xd1,0x25,
            0x72,0xf8,0xf6,0x64,0x86,0x68,0x98,0x16,0xd4,0xa4,0x5c,0xcc,0x5d,0x65,0xb6,0x92,
            0x6c,0x70,0x48,0x50,0xfd,0xed,0xb9,0xda,0x5e,0x15,0x46,0x57,0xa7,0x8d,0x9d,0x84,
            0x90,0xd8,0xab,0x00,0x8c,0xbc,0xd3,0x0a,0xf7,0xe4,0x58,0x05,0xb8,0xb3,0x45,0x06,
            0xd0,0x2c,0x1e,0x8f,0xca,0x3f,0x0f,0x02,0xc1,0xaf,0xbd,0x03,0x01,0x13,0x8a,0x6b,
            0x3a,0x91,0x11,0x41,0x4f,0x67,0xdc,0xea,0x97,0xf2,0xcf,0xce,0xf0,0xb4,0xe6,0x73,
            0x96,0xac,0x74,0x22,0xe7,0xad,0x35,0x85,0xe2,0xf9,0x37,0xe8,0x1c,0x75,0xdf,0x6e,
            0x47,0xf1,0x1a,0x71,0x1d,0x29,0xc5,0x89,0x6f,0xb7,0x62,0x0e,0xaa,0x18,0xbe,0x1b,
            0xfc,0x56,0x3e,0x4b,0xc6,0xd2,0x79,0x20,0x9a,0xdb,0xc0,0xfe,0x78,0xcd,0x5a,0xf4,
            0x1f,0xdd,0xa8,0x33,0x88,0x07,0xc7,0x31,0xb1,0x12,0x10,0x59,0x27,0x80,0xec,0x5f,
            0x60,0x51,0x7f,0xa9,0x19,0xb5,0x4a,0x0d,0x2d,0xe5,0x7a,0x9f,0x93,0xc9,0x9c,0xef,
            0xa0,0xe0,0x3b,0x4d,0xae,0x2a,0xf5,0xb0,0xc8,0xeb,0xbb,0x3c,0x83,0x53,0x99,0x61,
            0x17,0x2b,0x04,0x7e,0xba,0x77,0xd6,0x26,0xe1,0x69,0x14,0x63,0x55,0x21,0x0c,0x7d
    };

    public PF_AES(long derivedKey) {
        // dynamic rounds: 10 to 15 based on derived key
        rounds = 10 + (int)(Math.abs(derivedKey) % 6);

        // build dynamic S-box by shuffling standard one with derived key
        Sbox  = buildDynamicSbox(derivedKey);
        RSbox = buildInverseSbox(Sbox);

        // expand derived key into round keys
        roundKeys = expandKey(derivedKey);
    }

    // ── S-box generation ──────────────────────────────────────────────────────

    // Fisher-Yates shuffle using the derived key as seed
    private int[] buildDynamicSbox(long derivedKey) {
        int[] box = new int[256];
        for (int i = 0; i < 256; i++)
            box[i] = STANDARD_SBOX[i];

        long state = derivedKey;
        for (int i = 255; i > 0; i--) {
            state = Hash.Hash(state);
            int j = (int)(Math.abs(state) % (i + 1));
            int tmp = box[i];
            box[i] = box[j];
            box[j] = tmp;
        }
        return box;
    }

    // build inverse S-box from the shuffled S-box (needed for decryption)
    private int[] buildInverseSbox(int[] sbox) {
        int[] inv = new int[256];
        for (int i = 0; i < 256; i++)
            inv[sbox[i]] = i;
        return inv;
    }

    // ── Key expansion ─────────────────────────────────────────────────────────

    // expand the 8-byte derived key into (rounds+1) round keys, each 4x4 bytes
    private byte[][][] expandKey(long derivedKey) {
        byte[][][] keys = new byte[rounds + 1][4][4];
        long state = derivedKey;
        for (int r = 0; r <= rounds; r++) {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    state = Hash.Hash(state);
                    keys[r][i][j] = (byte)(state & 0xFF);
                }
            }
        }
        return keys;
    }

    // ── Encryption ────────────────────────────────────────────────────────────

    public String encrypt(String message) {
        byte[][] block = stringToBlock(message);
        addRoundKey(block, roundKeys[0]);

        for (int i = 1; i < rounds; i++) {
            byteSub(block);
            shiftRows(block);
            mixColumns(block);
            addRoundKey(block, roundKeys[i]);
        }

        byteSub(block);
        shiftRows(block);
        addRoundKey(block, roundKeys[rounds]);
        return blockToHex(block);
    }

    // ── Decryption ────────────────────────────────────────────────────────────

    public String decrypt(String hexMessage) {
        byte[][] block = hexToBlock(hexMessage);
        addRoundKey(block, roundKeys[rounds]);

        for (int i = rounds - 1; i > 0; i--) {
            iShiftRows(block);
            iByteSub(block);
            addRoundKey(block, roundKeys[i]);
            iMixColumns(block);
        }

        iShiftRows(block);
        iByteSub(block);
        addRoundKey(block, roundKeys[0]);
        return blockToString(block);
    }

    // ── AES operations ────────────────────────────────────────────────────────

    private void byteSub(byte[][] block) {
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++)
                block[i][j] = sub(block[i][j]);
    }

    private void iByteSub(byte[][] block) {
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++)
                block[i][j] = isub(block[i][j]);
    }

    private byte sub(byte b) {
        int index = b < 0 ? b + 256 : b;
        return (byte) Sbox[index];
    }

    private byte isub(byte b) {
        int index = b < 0 ? b + 256 : b;
        return (byte) RSbox[index];
    }

    private void shiftRows(byte[][] block) {
        for (int i = 1; i < 4; i++) {
            for (int j = 0; j < i; j++) {
                byte tmp = block[i][0];
                for (int k = 0; k < 3; k++)
                    block[i][k] = block[i][k + 1];
                block[i][3] = tmp;
            }
        }
    }

    private void iShiftRows(byte[][] block) {
        for (int i = 1; i < 4; i++) {
            for (int j = 0; j < i; j++) {
                byte tmp = block[i][3];
                for (int k = 3; k > 0; k--)
                    block[i][k] = block[i][k - 1];
                block[i][0] = tmp;
            }
        }
    }

    private void mixColumns(byte[][] block) {
        for (int i = 0; i < 4; i++) {
            byte[] col = new byte[]{block[0][i], block[1][i], block[2][i], block[3][i]};
            block[0][i] = (byte)(gmul(col[0],(byte)2) ^ gmul(col[1],(byte)3) ^ col[2] ^ col[3]);
            block[1][i] = (byte)(col[0] ^ gmul(col[1],(byte)2) ^ gmul(col[2],(byte)3) ^ col[3]);
            block[2][i] = (byte)(col[0] ^ col[1] ^ gmul(col[2],(byte)2) ^ gmul(col[3],(byte)3));
            block[3][i] = (byte)(gmul(col[0],(byte)3) ^ col[1] ^ col[2] ^ gmul(col[3],(byte)2));
        }
    }

    private void iMixColumns(byte[][] block) {
        for (int i = 0; i < 4; i++) {
            byte[] col = new byte[]{block[0][i], block[1][i], block[2][i], block[3][i]};
            block[0][i] = (byte)(gmul(col[0],(byte)0x0E) ^ gmul(col[1],(byte)0x0B) ^ gmul(col[2],(byte)0x0D) ^ gmul(col[3],(byte)0x09));
            block[1][i] = (byte)(gmul(col[0],(byte)0x09) ^ gmul(col[1],(byte)0x0E) ^ gmul(col[2],(byte)0x0B) ^ gmul(col[3],(byte)0x0D));
            block[2][i] = (byte)(gmul(col[0],(byte)0x0D) ^ gmul(col[1],(byte)0x09) ^ gmul(col[2],(byte)0x0E) ^ gmul(col[3],(byte)0x0B));
            block[3][i] = (byte)(gmul(col[0],(byte)0x0B) ^ gmul(col[1],(byte)0x0D) ^ gmul(col[2],(byte)0x09) ^ gmul(col[3],(byte)0x0E));
        }
    }

    private void addRoundKey(byte[][] block, byte[][] key) {
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++)
                block[i][j] ^= key[i][j];
    }

    // Galois field multiplication — core math of AES MixColumns
    private byte gmul(byte a, byte b) {
        byte p = 0;
        byte hiBitSet;
        for (int i = 0; i < 8; i++) {
            if ((b & 1) != 0)
                p ^= a;
            hiBitSet = (byte)(a & 0x80);
            a <<= 1;
            if (hiBitSet != 0)
                a ^= 0x1b;
            b >>= 1;
        }
        return p;
    }

    // ── Block conversion helpers ───────────────────────────────────────────────

    // string → 4x4 byte block (padded with spaces if shorter than 16 bytes)
    private byte[][] stringToBlock(String s) {
        byte[][] block = new byte[4][4];
        byte[] bytes = s.getBytes();
        for (int i = 0; i < 16; i++) {
            int row = i / 4;
            int col = i % 4;
            block[row][col] = i < bytes.length ? bytes[i] : (byte)' ';
        }
        return block;
    }

    // 4x4 byte block → hex string for storage/display
    private String blockToHex(byte[][] block) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++)
                sb.append(String.format("%02X", block[i][j] & 0xFF));
        return sb.toString();
    }

    // hex string → 4x4 byte block
    private byte[][] hexToBlock(String hex) {
        byte[][] block = new byte[4][4];
        for (int i = 0; i < 16; i++) {
            int row = i / 4;
            int col = i % 4;
            block[row][col] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return block;
    }

    // 4x4 byte block → readable string (trim padding)
    private String blockToString(byte[][] block) {
        byte[] bytes = new byte[16];
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++)
                bytes[i * 4 + j] = block[i][j];
        return new String(bytes).trim();
    }

    public int getRounds() { return rounds; }
}