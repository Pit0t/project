package Graphs;

public class Server {

    // ── Same seed database as the ATM ─────────────────────────────────────────
    // cardName → PIN (seed)
    private static final String[] CARD_NAMES = { "Alice Cohen", "Bob Levi", "Dana Mizrahi" };
    private static final long[]   CARD_PINS  = { 1234L, 4321L, 9999L };

    // ── Fixed ATM ID (same as ATM side) ───────────────────────────────────────
    public static final long ATM_ID = 1L;

    // ── Operation IDs ─────────────────────────────────────────────────────────
    public static final int OP_BALANCE  = 1;
    public static final int OP_WITHDRAW = 2;
    public static final int OP_DEPOSIT  = 3;

    // ─────────────────────────────────────────────────────────────────────────
    // Main entry point: ATM sends encrypted message, server returns encrypted response
    // ─────────────────────────────────────────────────────────────────────────
    public ServerResponse process(String cardName, String encryptedMessage,
                                  long sessionId, int operationId,
                                  long currentBalance) {

        // 1. Look up seed for this card
        long pin = findPin(cardName);
        if (pin == -1) {
            return new ServerResponse(false, "CARD_NOT_FOUND", 0L, 0L, "");
        }

        // 2. Rebuild the same context seed the ATM used
        long contextSeed = pin ^ (ATM_ID * 31L) ^ (sessionId * 17L) ^ ((long)operationId * 7L);

        // 3. Run State algorithm → derive the same key
        State state = new State(contextSeed);
        state.runAll();
        long derivedKey = state.seed;

        // 4. Decrypt the message
        String decrypted = decryptMsg(encryptedMessage, derivedKey);

        // 5. Verify and process
        boolean approved = false;
        long    newBalance = currentBalance;
        String  responseText = "";

        if (decrypted.startsWith("BALANCE_REQUEST")) {
            approved     = true;
            newBalance   = currentBalance;
            responseText = "BALANCE_OK|" + cardName + "|$" + currentBalance;

        } else if (decrypted.startsWith("WITHDRAW")) {
            // parse amount from "WITHDRAW|name|-$amount"
            long amount = parseAmount(decrypted);
            if (amount > 0 && amount <= currentBalance) {
                approved     = true;
                newBalance   = currentBalance - amount;
                responseText = "WITHDRAW_OK|" + cardName + "|-$" + amount + "|NEW_BAL:$" + newBalance;
            } else {
                approved     = false;
                responseText = "WITHDRAW_DECLINED|INSUFFICIENT_FUNDS";
            }

        } else if (decrypted.startsWith("DEPOSIT")) {
            long amount = parseAmount(decrypted);
            if (amount > 0) {
                approved     = true;
                newBalance   = currentBalance + amount;
                responseText = "DEPOSIT_OK|" + cardName + "|+$" + amount + "|NEW_BAL:$" + newBalance;
            } else {
                approved     = false;
                responseText = "DEPOSIT_DECLINED|INVALID_AMOUNT";
            }
        } else {
            responseText = "ERROR|UNRECOGNIZED_MESSAGE";
        }

        // 6. Encrypt the response with the same derived key
        String encryptedResponse = encryptMsg(responseText, derivedKey);

        return new ServerResponse(approved, decrypted, derivedKey, newBalance, encryptedResponse);
    }

    // ── Decrypt: XOR each byte with cycling key bytes ─────────────────────────
    public String decryptMsg(String encrypted, long derivedKey) {
        String clean    = encrypted.replace(" ", "");
        byte[] keyBytes = Long.toHexString(derivedKey).getBytes();
        byte[] encBytes = new byte[clean.length() / 2];

        for (int i = 0; i < encBytes.length; i++) {
            encBytes[i] = (byte) Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);
        }

        byte[] decBytes = new byte[encBytes.length];
        for (int i = 0; i < encBytes.length; i++) {
            decBytes[i] = (byte)(encBytes[i] ^ keyBytes[i % keyBytes.length]);
        }

        return new String(decBytes);
    }

    // ── Encrypt: XOR each byte with cycling key bytes (same as ATM) ──────────
    public String encryptMsg(String message, long derivedKey) {
        byte[]        keyBytes = Long.toHexString(derivedKey).getBytes();
        byte[]        msgBytes = message.getBytes();
        StringBuilder sb       = new StringBuilder();
        for (int i = 0; i < msgBytes.length; i++) {
            sb.append(String.format("%02X", (msgBytes[i] ^ keyBytes[i % keyBytes.length]) & 0xFF));
            if ((i + 1) % 4 == 0 && i + 1 < msgBytes.length) sb.append(" ");
        }
        return sb.toString();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private long findPin(String cardName) {
        for (int i = 0; i < CARD_NAMES.length; i++) {
            if (CARD_NAMES[i].equals(cardName)) return CARD_PINS[i];
        }
        return -1L;
    }

    private long parseAmount(String msg) {
        try {
            // format: "WITHDRAW|name|-$amount" or "DEPOSIT|name|+$amount"
            String[] parts = msg.split("\\|");
            String   raw   = parts[2].replace("-$", "").replace("+$", "");
            return Long.parseLong(raw);
        } catch (Exception e) {
            return -1L;
        }
    }

    // ── Server response container ─────────────────────────────────────────────
    public static class ServerResponse {
        public boolean approved;
        public String  decryptedMessage;   // what the server saw after decrypting
        public long    serverDerivedKey;   // key the server derived independently
        public long    newBalance;
        public String  encryptedResponse;  // server's encrypted reply

        public ServerResponse(boolean approved, String decrypted,
                              long key, long newBalance, String encResp) {
            this.approved          = approved;
            this.decryptedMessage  = decrypted;
            this.serverDerivedKey  = key;
            this.newBalance        = newBalance;
            this.encryptedResponse = encResp;
        }
    }
}