import Graphs.State;
import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ATM_GUI extends Application {

    // ── Colors ────────────────────────────────────────────────────────────────
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

    // ── Canvas settings ───────────────────────────────────────────────────────
    private static final double CANVAS_W     = 780;
    private static final double CANVAS_H     = 440;
    private static final double START_Y      = 30;
    private static final double X_SPACING    = 12;
    private static final double Y_SPACING    = 10;
    private static final double RADIUS       = 2.5;
    private static final int    STEP_DELAY   = 12;

    // ── Accounts ──────────────────────────────────────────────────────────────
    static class Account {
        String name;
        long   pin;
        long   balance;
        Account(String n, long p, long b) { name = n; pin = p; balance = b; }
    }

    private final Account[] accounts = {
            new Account("Alice Cohen",   1234L,  5000L),
            new Account("Bob Levi",      4321L, 12500L),
            new Account("Dana Mizrahi",  9999L,   750L)
    };

    // ── Session state ─────────────────────────────────────────────────────────
    private Account currentAccount = null;
    private String  enteredPin     = "";
    private long    derivedKey     = 0L;

    // ── Root ──────────────────────────────────────────────────────────────────
    private BorderPane root;

    // ─────────────────────────────────────────────────────────────────────────
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

    // ── Persistent header ─────────────────────────────────────────────────────
    private HBox buildHeader() {
        Label title = new Label("⬡  PF-GKD Bank");
        title.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + ACCENT + ";");

        Label sub = new Label("Pascal–Fibonacci Key Derivation ATM");
        sub.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-text-fill: " + TEXT_MUTED + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label secure = new Label("● SECURE CONNECTION");
        secure.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-text-fill: " + SUCCESS + ";");

        HBox header = new HBox(14, title, spacer, sub, secure);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 24, 14, 24));
        header.setStyle("-fx-background-color: " + BG_PANEL + "; -fx-border-color: transparent transparent " + BORDER + " transparent; -fx-border-width: 0 0 1 0;");
        return header;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SCREEN 1 — Card Selection
    // ══════════════════════════════════════════════════════════════════════════
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

        for (Account acc : accounts) {
            cards.getChildren().add(buildCardButton(acc));
        }

        screen.getChildren().addAll(title, sub, cards);
        switchScreen(screen);
    }

    private Button buildCardButton(Account acc) {
        String initials = "" + acc.name.charAt(0) + acc.name.split(" ")[1].charAt(0);

        Label avatar = new Label(initials);
        avatar.setMinSize(44, 44);
        avatar.setMaxSize(44, 44);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: " + ACCENT + "; -fx-background-radius: 22;");

        Label name = new Label(acc.name);
        name.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");

        Label cardNum = new Label("Card  •••• •••• " + String.format("%04d", acc.pin % 10000));
        cardNum.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: " + TEXT_MUTED + ";");

        VBox info = new VBox(3, name, cardNum);
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
        btn.setOnAction(e -> { currentAccount = acc; showPinEntry(null); });
        return btn;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SCREEN 2 — PIN Entry
    // ══════════════════════════════════════════════════════════════════════════
    private void showPinEntry(String errorMsg) {
        enteredPin = "";

        VBox screen = new VBox(20);
        screen.setAlignment(Pos.CENTER);
        screen.setPadding(new Insets(40));
        screen.setStyle("-fx-background-color: " + BG_DARK + ";");

        Label cardLabel = new Label("Card holder: " + currentAccount.name);
        cardLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-text-fill: " + TEXT_MUTED + ";");

        Label title = new Label("Enter PIN");
        title.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");

        // 4 dots
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

        // Keypad
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

        screen.getChildren().addAll(cardLabel, title, dotsBox, errLabel, keypad, backBtn);
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
            showLoadingScreen();
        } else {
            showPinEntry("Incorrect PIN. Please try again.");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SCREEN 3 — Pascal Loading / Key Derivation
    // ══════════════════════════════════════════════════════════════════════════
    private void showLoadingScreen() {
        VBox screen = new VBox(14);
        screen.setAlignment(Pos.CENTER);
        screen.setPadding(new Insets(20));
        screen.setStyle("-fx-background-color: " + BG_DARK + ";");

        Label status = new Label("Deriving session key from PIN…");
        status.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 13px; -fx-text-fill: " + ACCENT + ";");

        ProgressBar pb = new ProgressBar(0);
        pb.setPrefWidth(500); pb.setPrefHeight(6);

        Canvas canvas = new Canvas(CANVAS_W, CANVAS_H);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web(BG_DARK));
        gc.fillRect(0, 0, CANVAS_W, CANVAS_H);
        drawGrid(gc);

        // scroll pane so canvas fits nicely
        ScrollPane sp = new ScrollPane(canvas);
        sp.setPannable(true);
        sp.setPrefHeight(CANVAS_H);
        sp.setStyle("-fx-background-color: " + BG_DARK + "; -fx-background: " + BG_DARK + ";");

        screen.getChildren().addAll(status, pb, sp);
        switchScreen(screen);

        // run algorithm
        State state = new State(currentAccount.pin);
        state.runAll();
        derivedKey = state.seed;

        Graphs.Node[] path   = state.pathHistory;
        Graphs.Node[] remote = state.remoteHistory;

        int maxRow = 0, cnt = 0;
        for (int i = 0; i < state.totalSteps; i++) {
            if (path[i] == null || path[i + 1] == null) break;
            if (path[i + 1].row > maxRow) maxRow = path[i + 1].row;
            cnt++;
        }
        final int    total  = cnt;
        final double startX = CANVAS_W / 2.0;

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
            gc.setLineWidth(2.0); gc.setLineDashes(0);
            gc.strokeLine(x1, y1, x2, y2);
            gc.setFill(Color.web(SUCCESS));
            gc.fillOval(x1 - RADIUS, y1 - RADIUS, RADIUS * 2, RADIUS * 2);

            if (remote[i] != null && remote[i + 1] != null) {
                double rx1 = startX + (remote[i].col     - remote[i].row     / 2.0) * X_SPACING;
                double ry1 = START_Y +  remote[i].row     * Y_SPACING;
                double rx2 = startX + (remote[i + 1].col - remote[i + 1].row / 2.0) * X_SPACING;
                double ry2 = START_Y +  remote[i + 1].row * Y_SPACING;
                gc.setStroke(Color.web(DANGER)); gc.setLineWidth(1.0); gc.setLineDashes(3, 3);
                gc.strokeLine(rx1, ry1, rx2, ry2);
                gc.setLineDashes(0);
                gc.setFill(Color.web(DANGER, 0.8));
                gc.fillOval(rx1 - 2, ry1 - 2, 4, 4);
            }

            pb.setProgress((double)(i + 1) / total);
            stepHolder[0]++;
        }));

        anim.setOnFinished(e -> {
            status.setText("✓  Key derived successfully!");
            status.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 13px; -fx-text-fill: " + SUCCESS + ";");
            new Timeline(new KeyFrame(Duration.millis(900), ev -> showMainMenu())).play();
        });

        anim.play();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SCREEN 4 — Main Menu
    // ══════════════════════════════════════════════════════════════════════════
    private void showMainMenu() {
        VBox screen = new VBox(18);
        screen.setAlignment(Pos.CENTER);
        screen.setPadding(new Insets(50, 80, 50, 80));
        screen.setStyle("-fx-background-color: " + BG_DARK + ";");

        Label welcome = new Label("Welcome, " + currentAccount.name.split(" ")[0] + "!");
        welcome.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");

        // Session key display
        Label keyLabel = new Label("Session Key:  " + formatKey(derivedKey));
        keyLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: " + TEXT_MUTED + "; -fx-background-color: " + BG_PANEL + "; -fx-padding: 8 14; -fx-background-radius: 6;");

        // Balance
        Label balTitle = new Label("Current Balance");
        balTitle.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-text-fill: " + TEXT_MUTED + ";");

        Label balLabel = new Label("$" + String.format("%,d", currentAccount.balance));
        balLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 34px; -fx-font-weight: bold; -fx-text-fill: " + SUCCESS + ";");

        Separator sep = new Separator();
        sep.setMaxWidth(380);
        sep.setStyle("-fx-background-color: " + BORDER + ";");

        Label menuTitle = new Label("SELECT TRANSACTION");
        menuTitle.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_MUTED + ";");

        Button checkBtn    = buildMenuButton("💰   Check Balance");
        Button withdrawBtn = buildMenuButton("📤   Withdraw");
        Button depositBtn  = buildMenuButton("📥   Deposit");

        checkBtn.setOnAction(e    -> showCheckBalance());
        withdrawBtn.setOnAction(e -> showAmountScreen("Withdraw"));
        depositBtn.setOnAction(e  -> showAmountScreen("Deposit"));

        Button ejectBtn = buildGhostButton("⏏   Eject Card", e -> showCardSelect());

        VBox btns = new VBox(10, checkBtn, withdrawBtn, depositBtn);
        btns.setAlignment(Pos.CENTER);
        btns.setMaxWidth(360);

        screen.getChildren().addAll(welcome, keyLabel, balTitle, balLabel, sep, menuTitle, btns, ejectBtn);
        switchScreen(screen);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SCREEN 5a — Check Balance
    // ══════════════════════════════════════════════════════════════════════════
    private void showCheckBalance() {
        String plain = "BALANCE_REQUEST|" + currentAccount.name + "|$" + currentAccount.balance;
        String enc   = encryptMsg(plain);
        switchScreen(buildResultScreen(
                "Balance Inquiry",
                "Your current balance",
                "$" + String.format("%,d", currentAccount.balance),
                SUCCESS,
                plain, enc
        ));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SCREEN 5b — Withdraw / Deposit
    // ══════════════════════════════════════════════════════════════════════════
    private void showAmountScreen(String type) {
        VBox screen = new VBox(18);
        screen.setAlignment(Pos.CENTER);
        screen.setPadding(new Insets(50, 80, 50, 80));
        screen.setStyle("-fx-background-color: " + BG_DARK + ";");

        Label title = new Label(type);
        title.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");

        Label balLabel = new Label("Available: $" + String.format("%,d", currentAccount.balance));
        balLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 13px; -fx-text-fill: " + TEXT_MUTED + ";");

        Label sub = new Label("Enter amount ($):");
        sub.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 13px; -fx-text-fill: " + TEXT_MUTED + ";");

        TextField amountField = new TextField();
        amountField.setPromptText("0");
        amountField.setPrefWidth(220); amountField.setPrefHeight(52);
        amountField.setAlignment(Pos.CENTER);
        amountField.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 22px; -fx-background-color: " + BG_PANEL + "; -fx-text-fill: " + TEXT_PRIMARY + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-alignment: center;");

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
                if (type.equals("Withdraw")) currentAccount.balance -= amount;
                else                         currentAccount.balance += amount;

                String sign  = type.equals("Withdraw") ? "-" : "+";
                String color = type.equals("Withdraw") ? DANGER : SUCCESS;
                String plain = type.toUpperCase() + "|" + currentAccount.name + "|" + sign + "$" + amount;
                String enc   = encryptMsg(plain);

                switchScreen(buildResultScreen(
                        type + " Successful",
                        "New balance: $" + String.format("%,d", currentAccount.balance),
                        sign + "$" + String.format("%,d", amount),
                        color, plain, enc
                ));
            } catch (NumberFormatException ex) {
                errLabel.setText("Invalid amount. Numbers only.");
            }
        });

        Button backBtn = buildGhostButton("← Back", e -> showMainMenu());

        screen.getChildren().addAll(title, balLabel, sub, amountField, errLabel, confirmBtn, backBtn);
        switchScreen(screen);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RESULT SCREEN (shared)
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildResultScreen(String title, String subtitle, String mainValue, String valueColor, String plain, String encrypted) {
        VBox screen = new VBox(16);
        screen.setAlignment(Pos.CENTER);
        screen.setPadding(new Insets(40, 80, 40, 80));
        screen.setStyle("-fx-background-color: " + BG_DARK + ";");

        Label titleLabel = new Label("✓  " + title);
        titleLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");

        Label subLabel = new Label(subtitle);
        subLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 13px; -fx-text-fill: " + TEXT_MUTED + ";");

        Label valueLabel = new Label(mainValue);
        valueLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: " + valueColor + ";");

        // Encryption log box
        VBox encBox = new VBox(8);
        encBox.setPadding(new Insets(16, 20, 16, 20));
        encBox.setMaxWidth(600);
        encBox.setStyle("-fx-background-color: " + BG_PANEL + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label encTitle = new Label("🔐  Transaction Encryption Log");
        encTitle.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + ACCENT + ";");

        Label keyLine = new Label("Key:        " + formatKey(derivedKey));
        keyLine.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-text-fill: " + TEXT_MUTED + ";");
        keyLine.setWrapText(true);

        Label plainLine = new Label("Plain:      " + plain);
        plainLine.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-text-fill: " + TEXT_MUTED + ";");
        plainLine.setWrapText(true);

        Label encLine = new Label("Encrypted:  " + encrypted);
        encLine.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-text-fill: " + SUCCESS + ";");
        encLine.setWrapText(true);

        encBox.getChildren().addAll(encTitle, keyLine, plainLine, encLine);

        Button backBtn = buildPrimaryButton("← Back to Menu");
        backBtn.setOnAction(e -> showMainMenu());

        screen.getChildren().addAll(titleLabel, subLabel, valueLabel, encBox, backBtn);
        return screen;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ══════════════════════════════════════════════════════════════════════════
    private String encryptMsg(String message) {
        byte[] keyBytes = Long.toHexString(derivedKey).getBytes();
        byte[] msgBytes = message.getBytes();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < msgBytes.length; i++) {
            sb.append(String.format("%02X", (msgBytes[i] ^ keyBytes[i % keyBytes.length]) & 0xFF));
            if ((i + 1) % 4 == 0 && i + 1 < msgBytes.length) sb.append(" ");
        }
        return sb.toString();
    }

    private String formatKey(long key) {
        String hex = Long.toHexString(key).toUpperCase();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hex.length(); i++) {
            if (i > 0 && i % 4 == 0) sb.append("-");
            sb.append(hex.charAt(i));
        }
        return sb.toString();
    }

    private void switchScreen(Node screen) {
        FadeTransition ft = new FadeTransition(Duration.millis(180), screen);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
        root.setCenter(screen);
    }

    private void drawGrid(GraphicsContext gc) {
        gc.setStroke(Color.web(BORDER, 0.3));
        gc.setLineWidth(0.5);
        for (int x = 0; x < CANVAS_W; x += 60) gc.strokeLine(x, 0, x, CANVAS_H);
        for (int y = 0; y < CANVAS_H; y += 60) gc.strokeLine(0, y, CANVAS_W, y);
    }

    private void drawPascalTriangle(GraphicsContext gc, double startX, int maxRow) {
        gc.setStroke(Color.web(TEXT_MUTED, 0.4));
        gc.setLineWidth(0.6);
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

    // ── Button builders ───────────────────────────────────────────────────────
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