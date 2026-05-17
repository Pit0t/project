package ATM;

import AES.JSONBuilder;
import AES.PF_AES;
import Algorithm.GraphPathfinder;
import Algorithm.Hash;
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
        if (pin == -1) return new ServerResponse(false, "CARD_NOT_FOUND", 0L, 0L, "", 0);

        long contextSeed = Hash.Hash(pin);
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

    private long findPin(String cardName) {
        for (int i = 0; i < CARD_NAMES.length; i++)
            if (CARD_NAMES[i].equals(cardName)) return CARD_PINS[i];
        return -1L;
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