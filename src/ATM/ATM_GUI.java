package ATM;

import AES.JSONBuilder;
import AES.PF_AES;
import Algorithm.GraphPathfinder;
import Algorithm.State;
import Algorithm.Hash;
import Graphs.Node;
import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ATM_GUI extends Application {

    private static final String BG_DARK      = "#0d0f14";
    private static final String BG_PANEL     = "#13161e";
    private static final String BG_INPUT     = "#1a1e2a";
    private static final String ACCENT       = "#4f8ef7";
    private static final String ACCENT_HOVER = "#6aa3ff";
    private static final String BORDER       = "#2a2f40";
    private static final String TEXT_PRIMARY = "#e8ecf4";
    private static final String TEXT_MUTED   = "#5a6180";
    private static final String SUCCESS      = "#3dd68c";
    private static final String DANGER       = "#ff4444";
    private static final String WARNING      = "#f0a04a";

    private static final double CANVAS_W   = 3000;
    private static final double CANVAS_H   = 2500;
    private static final double START_Y    = 60;
    private static final double X_SPACING  = 8;
    private static final double Y_SPACING  = 6;
    private static final double RADIUS     = 3;
    private static final int    STEP_DELAY = 20;
    private static final String WORMHOLE_COLOR = "#ff9900";
    private static final String REMOTE_COLOR   = "#ff4444";

    // ── Change this to switch ATM pathfinding strategy ────────────────────────
    private static final GraphPathfinder.Strategy STRATEGY = GraphPathfinder.Strategy.DIJKSTRA;
    // private static final GraphPathfinder.Strategy STRATEGY = GraphPathfinder.Strategy.DFS;


    private static final long ATM_ID = Server.ATM_ID;

    static class Account {
        String name; long pin; long balance; int failedAttempts;
        Account(String n, long p, long b) { name = n; pin = p; balance = b; failedAttempts = 0; }
    }

    private final Account[] accounts = {
            new Account("Alice Cohen",  1234L,  5000L),
            new Account("Bob Levi",     4321L, 12500L),
            new Account("Dana Mizrahi", 9999L,   750L)
    };

    private Account currentAccount = null;
    private String  enteredPin     = "";
    private long    derivedKey     = 0L;
    private long    sessionId      = 0L;
    private int     aesRounds      = 0;

    private BorderPane root;
    private final Server server = new Server();

    @Override
    public void start(Stage stage) {
        root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG_DARK + ";");
        root.setTop(buildHeader());
        showCardSelect();

        Scene scene = new Scene(root, 920, 720);
        scene.setFill(Color.web(BG_DARK));
        stage.setTitle("PF-GKD Bank ATM");
        stage.setMinWidth(720);
        stage.setMinHeight(580);
        stage.setScene(scene);
        stage.show();

        FadeTransition ft = new FadeTransition(Duration.millis(500), root);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private HBox buildHeader() {
        Label title = new Label("⬡  PF-GKD Bank");
        title.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + ACCENT + ";");

        Label sub = new Label("Pascal–Fibonacci Key Derivation ATM");
        sub.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-text-fill: " + TEXT_MUTED + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label secure = new Label("● SECURE CONNECTION  |  ATM ID: " + ATM_ID);
        secure.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-text-fill: " + SUCCESS + ";");

        HBox header = new HBox(14, title, spacer, sub, secure);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 24, 14, 24));
        header.setStyle("-fx-background-color: " + BG_PANEL + "; -fx-border-color: transparent transparent " + BORDER + " transparent; -fx-border-width: 0 0 1 0;");
        return header;
    }

    // ── Card Select ───────────────────────────────────────────────────────────
    private void showCardSelect() {
        currentAccount = null;
        enteredPin     = "";
        derivedKey     = 0L;

        VBox screen = new VBox(28);
        screen.setAlignment(Pos.CENTER);
        screen.setPadding(new Insets(50, 80, 50, 80));
        screen.setStyle("-fx-background-color: " + BG_DARK + ";");

        Label title = new Label("Welcome");
        title.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");

        Label sub = new Label("Select your card to continue");
        sub.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 13px; -fx-text-fill: " + TEXT_MUTED + ";");

        VBox cards = new VBox(12);
        cards.setAlignment(Pos.CENTER);
        cards.setMaxWidth(420);
        for (Account acc : accounts) cards.getChildren().add(buildCardButton(acc));

        screen.getChildren().addAll(title, sub, cards);
        switchScreen(screen);
    }

    private Button buildCardButton(Account acc) {
        String initials = "" + acc.name.charAt(0) + acc.name.split(" ")[1].charAt(0);

        Label avatar = new Label(initials);
        avatar.setMinSize(44, 44); avatar.setMaxSize(44, 44);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: " + ACCENT + "; -fx-background-radius: 22;");

        Label name    = new Label(acc.name);
        name.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");
        Label cardNum = new Label("Card  •••• •••• " + String.format("%04d", acc.pin % 10000));
        cardNum.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: " + TEXT_MUTED + ";");

        VBox info    = new VBox(3, name, cardNum);
        HBox content = new HBox(16, avatar, info);
        content.setAlignment(Pos.CENTER_LEFT);

        Button btn = new Button();
        btn.setGraphic(content);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(68);
        btn.setPadding(new Insets(0, 20, 0, 20));

        String base = "-fx-background-color: " + BG_PANEL + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace(BORDER, ACCENT)));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        btn.setOnAction(e -> {
            if (acc.failedAttempts >= 3) { currentAccount = acc; showPinEntry("This card is blocked."); return; }
            currentAccount = acc; sessionId++; showPinEntry(null);
        });
        return btn;
    }

    // ── PIN Entry ─────────────────────────────────────────────────────────────
    private void showPinEntry(String errorMsg) {
        enteredPin = "";

        VBox screen = new VBox(20);
        screen.setAlignment(Pos.CENTER);
        screen.setPadding(new Insets(40));
        screen.setStyle("-fx-background-color: " + BG_DARK + ";");

        Label cardLabel    = new Label("Card holder: " + currentAccount.name);
        cardLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-text-fill: " + TEXT_MUTED + ";");
        Label sessionLabel = new Label("Session #" + sessionId);
        sessionLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: " + TEXT_MUTED + ";");
        Label title = new Label("Enter PIN");
        title.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");

        Label[] dots = new Label[4];
        HBox dotsBox = new HBox(16);
        dotsBox.setAlignment(Pos.CENTER);
        for (int i = 0; i < 4; i++) {
            dots[i] = new Label("○");
            dots[i].setStyle("-fx-font-size: 26px; -fx-text-fill: " + TEXT_MUTED + ";");
            dotsBox.getChildren().add(dots[i]);
        }

        Label errLabel = new Label(errorMsg != null ? errorMsg : "");
        errLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-text-fill: " + DANGER + ";");

        GridPane keypad = new GridPane();
        keypad.setHgap(10); keypad.setVgap(10);
        keypad.setAlignment(Pos.CENTER);

        String[][] keys = {{"1","2","3"},{"4","5","6"},{"7","8","9"},{"C","0","OK"}};
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 3; c++) {
                String key = keys[r][c];
                String bg  = key.equals("OK") ? ACCENT : key.equals("C") ? "#2a1a1a" : BG_PANEL;
                String fg  = key.equals("C") ? DANGER : TEXT_PRIMARY;
                Button btn = buildKeypadBtn(key, bg, fg);
                btn.setOnAction(e -> {
                    if (key.equals("C")) {
                        if (!enteredPin.isEmpty())
                            enteredPin = enteredPin.substring(0, enteredPin.length() - 1);
                    } else if (key.equals("OK")) {
                        handlePinSubmit();
                    } else if (enteredPin.length() < 4) {
                        enteredPin += key;
                    }
                    for (int i = 0; i < 4; i++) {
                        boolean filled = i < enteredPin.length();
                        dots[i].setText(filled ? "●" : "○");
                        dots[i].setStyle("-fx-font-size: 26px; -fx-text-fill: " + (filled ? TEXT_PRIMARY : TEXT_MUTED) + ";");
                    }
                });
                keypad.add(btn, c, r);
            }
        }

        Button backBtn = buildGhostButton("← Back", e -> showCardSelect());
        screen.getChildren().addAll(cardLabel, sessionLabel, title, dotsBox, errLabel, keypad, backBtn);
        switchScreen(screen);
    }

    private Button buildKeypadBtn(String text, String bg, String fg) {
        Button btn = new Button(text);
        btn.setPrefWidth(76); btn.setPrefHeight(58);
        String base = "-fx-font-family: 'Courier New'; -fx-font-size: 18px; -fx-font-weight: bold; -fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace(bg, bg.equals(ACCENT) ? ACCENT_HOVER : BG_INPUT)));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    private void handlePinSubmit() {
        if (enteredPin.length() < 4) { showPinEntry("Please enter all 4 digits."); return; }
        long entered = Long.parseLong(enteredPin);
        if (entered == currentAccount.pin) {
            currentAccount.failedAttempts = 0;
            showLoadingScreen(Server.OP_BALANCE);
        } else {
            currentAccount.failedAttempts++;
            if (currentAccount.failedAttempts >= 3)
                showCardSelect();
            else
                showPinEntry("Incorrect PIN. " + (3 - currentAccount.failedAttempts) + " attempts remaining.");
        }
    }

    // ── Pascal Loading Screen ─────────────────────────────────────────────────
    private void showLoadingScreen(int operationId) {
        String strategyName = STRATEGY == GraphPathfinder.Strategy.DIJKSTRA ? "Dijkstra" : "DFS";

        VBox screen = new VBox(14);
        screen.setAlignment(Pos.CENTER);
        screen.setPadding(new Insets(20));
        screen.setStyle("-fx-background-color: " + BG_DARK + ";");

        Label status = new Label("Deriving session key via " + strategyName + "…  [ATM_ID=" + ATM_ID + "  SESSION=" + sessionId + "]");
        status.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-text-fill: " + ACCENT + ";");

        ProgressBar pb = new ProgressBar(0);
        pb.setPrefWidth(500); pb.setPrefHeight(6);

        Canvas canvas = new Canvas(CANVAS_W, CANVAS_H);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web(BG_DARK));
        gc.fillRect(0, 0, CANVAS_W, CANVAS_H);
        drawGrid(gc);

        ScrollPane sp = new ScrollPane(canvas);
        sp.setPannable(true);
        sp.setStyle("-fx-background-color: " + BG_DARK + "; -fx-background: " + BG_DARK + ";");
        VBox.setVgrow(sp, Priority.ALWAYS);

        screen.getChildren().addAll(status, pb, sp);
        switchScreen(screen);

        long contextSeed = Hash.Hash(currentAccount.pin);
        contextSeed = Hash.Hash(contextSeed ^ ATM_ID);
        contextSeed = Hash.Hash(contextSeed ^ sessionId);
        contextSeed = Hash.Hash(contextSeed ^ operationId);

        State state = new State(contextSeed, STRATEGY);
        state.runAll();
        derivedKey = state.seed;

        PF_AES aes = new PF_AES(derivedKey);
        aesRounds = aes.getRounds();

        Node[] path   = state.pathHistory;
        Node[] remote = state.remoteHistory;

        int maxRow = 0, cnt = 0;
        for (int i = 0; i < state.totalSteps; i++) {
            if (path[i] == null || path[i + 1] == null) break;
            if (path[i + 1].row > maxRow) maxRow = path[i + 1].row;
            cnt++;
        }
        final int total = cnt;
        final double startX = CANVAS_W / 2.0;

        int wormholes = 0;
        for (int i = 0; i <= total; i++)
            if (path[i] != null && path[i].isWormhole) wormholes++;
        final int wormholeCount = wormholes;

        drawPascalTriangle(gc, startX, maxRow);
        sp.setHvalue(0.5); sp.setVvalue(0.0);

        int[] stepHolder = {0};
        Timeline anim = new Timeline();
        anim.setCycleCount(total);
        anim.getKeyFrames().add(new KeyFrame(Duration.millis(STEP_DELAY), e -> {
            int i = stepHolder[0];
            double x1 = startX + (path[i].col     - path[i].row     / 2.0) * X_SPACING;
            double y1 = START_Y +  path[i].row     * Y_SPACING;
            double x2 = startX + (path[i + 1].col - path[i + 1].row / 2.0) * X_SPACING;
            double y2 = START_Y +  path[i + 1].row * Y_SPACING;

            gc.setStroke(Color.web(ACCENT));
            gc.setLineWidth(2.5); gc.setLineDashes(0);
            gc.strokeLine(x1, y1, x2, y2);

            String dotColor = path[i].isWormhole ? WORMHOLE_COLOR : SUCCESS;
            gc.setFill(Color.web(dotColor));
            gc.fillOval(x1 - RADIUS, y1 - RADIUS, RADIUS * 2, RADIUS * 2);

            if (remote[i] != null && remote[i + 1] != null) {
                double rx1 = startX + (remote[i].col     - remote[i].row     / 2.0) * X_SPACING;
                double ry1 = START_Y +  remote[i].row     * Y_SPACING;
                double rx2 = startX + (remote[i + 1].col - remote[i + 1].row / 2.0) * X_SPACING;
                double ry2 = START_Y +  remote[i + 1].row * Y_SPACING;
                gc.setStroke(Color.web(REMOTE_COLOR));
                gc.setLineWidth(1.2); gc.setLineDashes(4, 4);
                gc.strokeLine(rx1, ry1, rx2, ry2);
                gc.setLineDashes(0);
                gc.setFill(Color.web(REMOTE_COLOR, 0.8));
                gc.fillOval(rx1 - 2, ry1 - 2, 4, 4);
            }

            pb.setProgress((double)(i + 1) / total);
            stepHolder[0]++;
        }));

        long finalContextSeed = contextSeed;
        anim.setOnFinished(e -> {
            gc.setFill(Color.web(SUCCESS, 0.9));
            gc.setFont(Font.font("Courier New", FontWeight.BOLD, 14));
            gc.fillText("Seed: " + finalContextSeed + "   Strategy: " + strategyName
                    + "   Target: row=" + state.targetNode.row + " col=" + state.targetNode.col
                    + "   Steps: " + total, 30, 38);
            drawLegend(gc, strategyName, wormholeCount, 30, 60);
            gc.setFont(Font.font("Courier New", FontWeight.NORMAL, 10));
            gc.setFill(Color.web(TEXT_MUTED));
            gc.fillText("Derived key: " + Long.toHexString(derivedKey).toUpperCase(), 30, CANVAS_H - 30);

            status.setText("✓  Key derived!  [" + strategyName + "]  steps=" + total
                    + "  wormholes=" + wormholeCount + "  Key: " + formatKey(derivedKey)
                    + "  |  AES rounds: " + aesRounds);
            status.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-text-fill: " + SUCCESS + ";");
            pb.setVisible(false);
            new Timeline(new KeyFrame(Duration.millis(900), ev -> showMainMenu())).play();
        });

        anim.play();
    }

    // ── Main Menu ─────────────────────────────────────────────────────────────
    private void showMainMenu() {
        VBox screen = new VBox(18);
        screen.setAlignment(Pos.CENTER);
        screen.setPadding(new Insets(50, 80, 50, 80));
        screen.setStyle("-fx-background-color: " + BG_DARK + ";");

        Label welcome = new Label("Welcome, " + currentAccount.name.split(" ")[0] + "!");
        welcome.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");

        Label keyLabel = new Label("Session Key:  " + formatKey(derivedKey) + "   |   AES Rounds: " + aesRounds);
        keyLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: " + TEXT_MUTED + "; -fx-background-color: " + BG_PANEL + "; -fx-padding: 8 14; -fx-background-radius: 6;");

        String strategyName = STRATEGY == GraphPathfinder.Strategy.DIJKSTRA ? "Dijkstra" : "DFS";
        Label sessionLabel = new Label("Session #" + sessionId + "   |   ATM ID: " + ATM_ID + "   |   Strategy: " + strategyName);
        sessionLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: " + TEXT_MUTED + ";");

        Separator sep = new Separator(); sep.setMaxWidth(380);

        Label menuTitle = new Label("SELECT TRANSACTION");
        menuTitle.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_MUTED + ";");

        Button checkBtn    = buildMenuButton("💰   Check Balance");
        Button withdrawBtn = buildMenuButton("📤   Withdraw");
        Button depositBtn  = buildMenuButton("📥   Deposit");

        checkBtn.setOnAction(e    -> handleTransaction("BALANCE",  0));
        withdrawBtn.setOnAction(e -> showAmountScreen("Withdraw"));
        depositBtn.setOnAction(e  -> showAmountScreen("Deposit"));

        Button ejectBtn = buildGhostButton("⏏   Eject Card", e -> showCardSelect());

        VBox btns = new VBox(10, checkBtn, withdrawBtn, depositBtn);
        btns.setAlignment(Pos.CENTER); btns.setMaxWidth(360);

        screen.getChildren().addAll(welcome, keyLabel, sessionLabel, sep, menuTitle, btns, ejectBtn);
        switchScreen(screen);
    }

    // ── Amount Input ──────────────────────────────────────────────────────────
    private void showAmountScreen(String type) {
        VBox screen = new VBox(18);
        screen.setAlignment(Pos.CENTER);
        screen.setPadding(new Insets(50, 80, 50, 80));
        screen.setStyle("-fx-background-color: " + BG_DARK + ";");

        Label title    = new Label(type);
        title.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");
        Label balLabel = new Label("Available: $" + String.format("%,d", currentAccount.balance));
        balLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 13px; -fx-text-fill: " + TEXT_MUTED + ";");
        Label sub = new Label("Enter amount ($):");
        sub.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 13px; -fx-text-fill: " + TEXT_MUTED + ";");

        TextField amountField = new TextField();
        amountField.setPromptText("0");
        amountField.setPrefWidth(220); amountField.setPrefHeight(52);
        amountField.setAlignment(Pos.CENTER);
        amountField.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 22px; -fx-background-color: " + BG_PANEL + "; -fx-text-fill: " + TEXT_PRIMARY + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label errLabel = new Label("");
        errLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-text-fill: " + DANGER + ";");

        Button confirmBtn = buildPrimaryButton("Confirm");
        confirmBtn.setOnAction(e -> {
            String input = amountField.getText().trim();
            if (input.isEmpty()) { errLabel.setText("Please enter an amount."); return; }
            try {
                long amount = Long.parseLong(input);
                if (amount <= 0) { errLabel.setText("Amount must be greater than 0."); return; }
                if (type.equals("Withdraw") && amount > currentAccount.balance) {
                    errLabel.setText("Insufficient funds."); return;
                }
                handleTransaction(type.toUpperCase(), amount);
            } catch (NumberFormatException ex) {
                errLabel.setText("Invalid amount. Numbers only.");
            }
        });

        Button backBtn = buildGhostButton("← Back", e -> showMainMenu());
        screen.getChildren().addAll(title, balLabel, sub, amountField, errLabel, confirmBtn, backBtn);
        switchScreen(screen);
    }

    // ── Transaction Handler ───────────────────────────────────────────────────
    private void handleTransaction(String type, long amount) {
        int operationId;
        switch (type) {
            case "BALANCE":  operationId = Server.OP_BALANCE;  break;
            case "WITHDRAW": operationId = Server.OP_WITHDRAW; break;
            default:         operationId = Server.OP_DEPOSIT;  break;
        }

        // build JSON message
        String plainJson = JSONBuilder.build(
                "name",   currentAccount.name,
                "op",     type,
                "amount", String.valueOf(amount)
        );

        // derive key and encrypt with PF_AES
        long contextSeed = Hash.Hash(currentAccount.pin);
        contextSeed = Hash.Hash(contextSeed ^ ATM_ID);
        contextSeed = Hash.Hash(contextSeed ^ sessionId);
        contextSeed = Hash.Hash(contextSeed ^ operationId);

        State state = new State(contextSeed, STRATEGY);
        state.runAll();
        derivedKey = state.seed;

        PF_AES aes = new PF_AES(derivedKey);
        aesRounds = aes.getRounds();
        String encryptedMessage = aes.encrypt(plainJson);

        // send to server
        Server.ServerResponse resp = server.process(
                currentAccount.name, encryptedMessage,
                sessionId, operationId, currentAccount.balance
        );

        if (resp.approved) currentAccount.balance = resp.newBalance;

        // decrypt server response
        String decryptedResponse = aes.decrypt(resp.encryptedResponse);

        String valueColor = resp.approved ? SUCCESS : DANGER;
        String mainValue  = type.equals("BALANCE") ? "" : (resp.approved ? "APPROVED" : "✗ DECLINED");

        switchScreen(buildResultScreen(
                plainJson, encryptedMessage,
                resp, decryptedResponse, mainValue, valueColor, type.equals("BALANCE")
        ));
    }

    // ── Result Screen ─────────────────────────────────────────────────────────
    private VBox buildResultScreen(String plain, String encMsg,
                                   Server.ServerResponse resp, String decResp,
                                   String mainValue, String valueColor, boolean isBalance) {
        VBox screen = new VBox(14);
        screen.setAlignment(Pos.CENTER);
        screen.setPadding(new Insets(30, 60, 30, 60));
        screen.setStyle("-fx-background-color: " + BG_DARK + ";");

        Label titleLabel = new Label(mainValue);
        titleLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + valueColor + ";");

        String balColor = currentAccount.balance >= 0 ? SUCCESS : DANGER;
        String balText  = isBalance
                ? "$" + String.format("%,d", currentAccount.balance)
                : "Balance: $" + String.format("%,d", currentAccount.balance);
        String balSize  = isBalance ? "40px" : "15px";
        Label balLabel = new Label(balText);
        balLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: " + balSize + "; -fx-font-weight: bold; -fx-text-fill: " + balColor + ";");

        VBox logBox = new VBox(10);
        logBox.setPadding(new Insets(16, 20, 16, 20));
        logBox.setMaxWidth(700);
        logBox.setStyle("-fx-background-color: " + BG_PANEL + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 8; -fx-background-radius: 8;");

        logBox.getChildren().addAll(
                logTitle("🔐  Transaction Encryption Log  [PF-AES  rounds=" + aesRounds + "]"),
                logSeparator(),
                logRow("ATM",    "Session Key",      formatKey(derivedKey),           TEXT_MUTED),
                logRow("ATM",    "JSON message",      plain,                            TEXT_MUTED),
                logRow("ATM",    "AES encrypted →",   encMsg,                           WARNING),
                logSeparator(),
                logRow("SERVER", "Derived same key",  formatKey(resp.serverDerivedKey),
                        resp.serverDerivedKey == derivedKey ? SUCCESS : DANGER),
                logRow("SERVER", "AES decrypted ✓",   resp.decryptedMessage,            SUCCESS),
                logRow("SERVER", "Response JSON",      decResp,                          TEXT_MUTED),
                logRow("SERVER", "Response enc →",     resp.encryptedResponse,           WARNING),
                logSeparator(),
                logRow("ATM",    "Response dec ✓",     decResp,                          SUCCESS)
        );

        ScrollPane logScroll = new ScrollPane(logBox);
        logScroll.setFitToWidth(true);
        logScroll.setPrefHeight(300);
        logScroll.setStyle("-fx-background-color: " + BG_PANEL + "; -fx-background: " + BG_PANEL + ";");

        Button backBtn = buildPrimaryButton("← Back to Menu");
        backBtn.setOnAction(e -> showMainMenu());

        screen.getChildren().addAll(titleLabel, balLabel, logScroll, backBtn);
        return screen;
    }

    // ── Log helpers ───────────────────────────────────────────────────────────
    private Label logTitle(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + ACCENT + ";");
        return l;
    }

    private Separator logSeparator() {
        Separator s = new Separator();
        s.setStyle("-fx-background-color: " + BORDER + ";");
        return s;
    }

    private HBox logRow(String side, String label, String value, String valueColor) {
        Label sideLabel = new Label("[" + side + "]");
        sideLabel.setMinWidth(80);
        sideLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + (side.equals("SERVER") ? WARNING : ACCENT) + ";");

        Label keyLabel = new Label(label + ":");
        keyLabel.setMinWidth(140);
        keyLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-text-fill: " + TEXT_MUTED + ";");

        Label valLabel = new Label(value);
        valLabel.setWrapText(true);
        valLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-text-fill: " + valueColor + ";");
        HBox.setHgrow(valLabel, Priority.ALWAYS);

        HBox row = new HBox(8, sideLabel, keyLabel, valLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────
    private String formatKey(long key) {
        String hex = Long.toHexString(key).toUpperCase();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hex.length(); i++) {
            if (i > 0 && i % 4 == 0) sb.append("-");
            sb.append(hex.charAt(i));
        }
        return sb.toString();
    }

    private void switchScreen(javafx.scene.Node screen) {
        FadeTransition ft = new FadeTransition(Duration.millis(180), screen);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
        root.setCenter(screen);
    }

    private void drawGrid(GraphicsContext gc) {
        gc.setStroke(Color.web(BORDER, 0.35));
        gc.setLineWidth(0.5);
        for (int x = 0; x < CANVAS_W; x += 80) gc.strokeLine(x, 0, x, CANVAS_H);
        for (int y = 0; y < CANVAS_H; y += 80) gc.strokeLine(0, y, CANVAS_W, y);
    }

    private void drawPascalTriangle(GraphicsContext gc, double startX, int maxRow) {
        gc.setStroke(Color.web(TEXT_MUTED, 0.55));
        gc.setLineWidth(0.8);
        for (int row = 0; row < maxRow; row++) {
            for (int col = 0; col <= row; col++) {
                double px  = startX + (col     - row       / 2.0) * X_SPACING;
                double py  = START_Y +  row     * Y_SPACING;
                double lcx = startX + (col     - (row + 1) / 2.0) * X_SPACING;
                double lcy = START_Y + (row + 1) * Y_SPACING;
                double rcx = startX + (col + 1 - (row + 1) / 2.0) * X_SPACING;
                double rcy = START_Y + (row + 1) * Y_SPACING;
                gc.strokeLine(px, py, lcx, lcy);
                gc.strokeLine(px, py, rcx, rcy);
            }
        }
    }

    private void drawLegend(GraphicsContext gc, String strategy, int wormholeCount, double lx, double ly) {
        gc.setFont(Font.font("Courier New", FontWeight.BOLD, 11));
        gc.setFill(Color.web(ACCENT));
        gc.fillText("Strategy: " + strategy, lx, ly - 14);

        gc.setStroke(Color.web(ACCENT)); gc.setLineWidth(2.5); gc.setLineDashes(0);
        gc.strokeLine(lx, ly, lx + 24, ly);
        gc.setFill(Color.web(SUCCESS));
        gc.fillOval(lx + 24 - RADIUS, ly - RADIUS, RADIUS * 2, RADIUS * 2);
        gc.setFill(Color.web(TEXT_PRIMARY));
        gc.fillText("Path walk", lx + 34, ly + 4);

        ly += 18;
        gc.setFill(Color.web(WORMHOLE_COLOR));
        gc.fillOval(lx, ly - RADIUS, RADIUS * 2, RADIUS * 2);
        gc.setFill(Color.web(TEXT_PRIMARY));
        gc.fillText("Wormhole (" + wormholeCount + ")", lx + 14, ly + 4);

        ly += 18;
        gc.setStroke(Color.web(REMOTE_COLOR)); gc.setLineWidth(1.2); gc.setLineDashes(4, 4);
        gc.strokeLine(lx, ly, lx + 24, ly);
        gc.setLineDashes(0);
        gc.setFill(Color.web(REMOTE_COLOR, 0.8));
        gc.fillOval(lx + 24 - 2, ly - 2, 4, 4);
        gc.setFill(Color.web(TEXT_PRIMARY));
        gc.fillText("Remote read", lx + 34, ly + 4);
    }

    private Button buildPrimaryButton(String text) {
        Button btn = new Button(text);
        btn.setPrefHeight(42); btn.setPrefWidth(220);
        String base = "-fx-font-family: 'Courier New'; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-color: " + ACCENT + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace(ACCENT, ACCENT_HOVER)));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    private Button buildMenuButton(String text) {
        Button btn = new Button(text);
        btn.setPrefHeight(52); btn.setMaxWidth(Double.MAX_VALUE);
        String base = "-fx-font-family: 'Courier New'; -fx-font-size: 14px; -fx-background-color: " + BG_PANEL + "; -fx-text-fill: " + TEXT_PRIMARY + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace(BORDER, ACCENT)));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    private Button buildGhostButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.setPrefHeight(36);
        String base = "-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-background-color: transparent; -fx-text-fill: " + TEXT_MUTED + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 0 16; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace(TEXT_MUTED, TEXT_PRIMARY)));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        btn.setOnAction(handler);
        return btn;
    }

    public static void main(String[] args) { launch(args); }
}