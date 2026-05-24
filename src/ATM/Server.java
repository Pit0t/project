package ATM;

import AES.JSONBuilder;
import AES.PF_AES;
import Algorithm.GraphPathfinder;
import Algorithm.Hash;
import Algorithm.State;

public class Server {

    private static final long[]   CARD_IDS   = { 217389790L, 218886547L, 306227365L };
    private static final String[] CARD_NAMES = { "Alice Cohen", "Bob Levi", "Dana Mizrahi" };
    private static final long[]   CARD_PINS  = { 1234L, 4321L, 9999L };

    public static final long ATM_ID      = 1L;
    public static final int  OP_BALANCE  = 1;
    public static final int  OP_WITHDRAW = 2;
    public static final int  OP_DEPOSIT  = 3;

    public ServerResponse process(long cardId, String encryptedMessage,
                                  long sessionId, int operationId,
                                  long currentBalance) {
        long[] entry = findEntry(cardId);
        if (entry == null) return new ServerResponse(false, "CARD_NOT_FOUND", 0L, 0L, "", 0);
        long pin = entry[0];
        long id  = entry[1];

        long contextSeed = Hash.Hash(pin ^ id);
        contextSeed = Hash.Hash(contextSeed ^ ATM_ID);
        contextSeed = Hash.Hash(contextSeed ^ sessionId);
        contextSeed = Hash.Hash(contextSeed ^ operationId);

        // server independently derives the same key
        State state = new State(contextSeed, GraphPathfinder.Strategy.DIJKSTRA);
        state.runAll();
        long derivedKey = state.seed;

        // decrypt using PF_AES
        PF_AES aes = new PF_AES(derivedKey);
        String decrypted = aes.decrypt(encryptedMessage);

        // parse JSON fields
        String op     = JSONBuilder.getValue(decrypted, "op");
        String amount = JSONBuilder.getValue(decrypted, "amount");

        boolean approved   = false;
        long    newBalance = currentBalance;
        String  response   = "";

        if (op.equals("BALANCE")) {
            approved   = true;
            newBalance = currentBalance;
            response   = JSONBuilder.build(
                    "status", "OK",
                    "op", "BALANCE",
                    "balance", String.valueOf(currentBalance)
            );

        } else if (op.equals("WITHDRAW")) {
            long amt = parseLong(amount);
            if (amt > 0 && amt <= currentBalance) {
                approved   = true;
                newBalance = currentBalance - amt;
                response   = JSONBuilder.build(
                        "status", "OK",
                        "op", "WITHDRAW",
                        "balance", String.valueOf(newBalance)
                );
            } else {
                response = JSONBuilder.build(
                        "status", "DECLINED",
                        "op", "WITHDRAW",
                        "reason", "INSUFFICIENT_FUNDS"
                );
            }

        } else if (op.equals("DEPOSIT")) {
            long amt = parseLong(amount);
            if (amt > 0) {
                approved   = true;
                newBalance = currentBalance + amt;
                response   = JSONBuilder.build(
                        "status", "OK",
                        "op", "DEPOSIT",
                        "balance", String.valueOf(newBalance)
                );
            } else {
                response = JSONBuilder.build(
                        "status", "DECLINED",
                        "op", "DEPOSIT",
                        "reason", "INVALID_AMOUNT"
                );
            }
        } else {
            response = JSONBuilder.build("status", "ERROR", "reason", "UNKNOWN_OP");
        }

        String encryptedResponse = aes.encrypt(response);
        return new ServerResponse(approved, decrypted, derivedKey, newBalance, encryptedResponse, aes.getRounds());
    }

    private long parseLong(String s) {
        try { return Long.parseLong(s); }
        catch (NumberFormatException e) { return -1L; }
    }

    private long[] findEntry(long cardId) {
        for (int i = 0; i < CARD_IDS.length; i++)
            if (CARD_IDS[i] == cardId) return new long[]{ CARD_PINS[i], CARD_IDS[i] };
        return null;
    }

    public static boolean idExists(long cardId) {
        for (long id : CARD_IDS)
            if (id == cardId) return true;
        return false;
    }

    public static class ServerResponse {
        public boolean approved;
        public String  decryptedMessage;
        public long    serverDerivedKey;
        public long    newBalance;
        public String  encryptedResponse;
        public int     aesRounds;

        public ServerResponse(boolean approved, String decrypted,
                              long key, long newBalance, String encResp, int rounds) {
            this.approved          = approved;
            this.decryptedMessage  = decrypted;
            this.serverDerivedKey  = key;
            this.newBalance        = newBalance;
            this.encryptedResponse = encResp;
            this.aesRounds         = rounds;
        }
    }
}