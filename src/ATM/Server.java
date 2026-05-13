package ATM;

import Algorithm.GraphPathfinder;
import Algorithm.State;

public class Server {

    private static final String[] CARD_NAMES = { "Alice Cohen", "Bob Levi", "Dana Mizrahi" };
    private static final long[]   CARD_PINS  = { 1234L, 4321L, 9999L };

    public static final long ATM_ID      = 1L;
    public static final int  OP_BALANCE  = 1;
    public static final int  OP_WITHDRAW = 2;
    public static final int  OP_DEPOSIT  = 3;

    public ServerResponse process(String cardName, String encryptedMessage,
                                  long sessionId, int operationId,
                                  long currentBalance) {
        long pin = findPin(cardName);
        if (pin == -1) return new ServerResponse(false, "CARD_NOT_FOUND", 0L, 0L, "");

        long contextSeed = pin ^ (ATM_ID * 31L) ^ (sessionId * 17L) ^ ((long)operationId * 7L);

        // server derives the same key independently
        State state = new State(contextSeed, GraphPathfinder.Strategy.DIJKSTRA);
        state.runAll();
        long derivedKey = state.seed;

        String decrypted = decryptMsg(encryptedMessage, derivedKey);

        boolean approved     = false;
        long    newBalance   = currentBalance;
        String  responseText = "";

        if (decrypted.startsWith("BALANCE_REQUEST")) {
            approved     = true;
            newBalance   = currentBalance;
            responseText = "BALANCE_OK|" + cardName + "|$" + currentBalance;

        } else if (decrypted.startsWith("WITHDRAW")) {
            long amount = parseAmount(decrypted);
            if (amount > 0 && amount <= currentBalance) {
                approved     = true;
                newBalance   = currentBalance - amount;
                responseText = "WITHDRAW_OK|" + cardName + "|-$" + amount + "|NEW_BAL:$" + newBalance;
            } else {
                responseText = "WITHDRAW_DECLINED|INSUFFICIENT_FUNDS";
            }

        } else if (decrypted.startsWith("DEPOSIT")) {
            long amount = parseAmount(decrypted);
            if (amount > 0) {
                approved     = true;
                newBalance   = currentBalance + amount;
                responseText = "DEPOSIT_OK|" + cardName + "|+$" + amount + "|NEW_BAL:$" + newBalance;
            } else {
                responseText = "DEPOSIT_DECLINED|INVALID_AMOUNT";
            }
        } else {
            responseText = "ERROR|UNRECOGNIZED_MESSAGE";
        }

        String encryptedResponse = encryptMsg(responseText, derivedKey);
        return new ServerResponse(approved, decrypted, derivedKey, newBalance, encryptedResponse);
    }

    public String decryptMsg(String encrypted, long derivedKey) {
        String clean    = encrypted.replace(" ", "");
        byte[] keyBytes = Long.toHexString(derivedKey).getBytes();
        byte[] encBytes = new byte[clean.length() / 2];

        for (int i = 0; i < encBytes.length; i++)
            encBytes[i] = (byte) Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);

        byte[] decBytes = new byte[encBytes.length];
        for (int i = 0; i < encBytes.length; i++)
            decBytes[i] = (byte)(encBytes[i] ^ keyBytes[i % keyBytes.length]);

        return new String(decBytes);
    }

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

    private long findPin(String cardName) {
        for (int i = 0; i < CARD_NAMES.length; i++)
            if (CARD_NAMES[i].equals(cardName)) return CARD_PINS[i];
        return -1L;
    }

    private long parseAmount(String msg) {
        try {
            String[] parts = msg.split("\\|");
            return Long.parseLong(parts[2].replace("-$", "").replace("+$", ""));
        } catch (Exception e) {
            return -1L;
        }
    }

    public static class ServerResponse {
        public boolean approved;
        public String  decryptedMessage;
        public long    serverDerivedKey;
        public long    newBalance;
        public String  encryptedResponse;

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