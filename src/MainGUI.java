import Algorithm.GraphPathfinder;
import Algorithm.Hash;
import Algorithm.State;
import Graphs.Node;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainGUI extends Application {

    private static final String BG_DARK       = "#0d0f14";
    private static final String BG_PANEL      = "#13161e";
    private static final String BG_INPUT      = "#1a1e2a";
    private static final String ACCENT        = "#4f8ef7";
    private static final String ACCENT_HOVER  = "#6aa3ff";
    private static final String BORDER        = "#2a2f40";
    private static final String TEXT_PRIMARY  = "#e8ecf4";
    private static final String TEXT_MUTED    = "#5a6180";
    private static final String SUCCESS       = "#3dd68c";
    private static final String WARNING       = "#f0a04a";
    private static final String REMOTE_COLOR  = "#ff4444";
    private static final String WORMHOLE_COLOR = "#ff9900";
    private static final String ACCENT2       = "#c97bff"; // purple for flipped seed

    private static final int    STEP_DELAY_MS = 20;
    private static final double CANVAS_W = 3000;
    private static final double CANVAS_H = 2500;
    private static final double SPLIT_W = 1400;
    private static final double SPLIT_H = 2000;
    private static final double START_Y       = 60;
    private static final double X_SPACING = 8;
    private static final double Y_SPACING = 6;
    private static final double RADIUS        = 3;

    private Label       statusLabel;
    private ProgressBar progressBar;
    private Button      startBtn;
    private Button      avalancheBtn;
    private TextField   seedInput;
    private Canvas      canvas;
    private ScrollPane  scroll;
    private BorderPane  root;

    private GraphPathfinder.Strategy selectedStrategy = GraphPathfinder.Strategy.DIJKSTRA;
    private Button dijkstraBtn;
    private Button dfsBtn;

    @Override
    public void start(Stage primaryStage) {
        root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG_DARK + ";");
        root.setTop(buildHeader());
        root.setCenter(buildCenter());
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 960, 680);
        scene.setFill(Color.web(BG_DARK));
        primaryStage.setTitle("PF-GKD  ·  Key Derivation Visualizer");
        primaryStage.setMinWidth(720);
        primaryStage.setMinHeight(500);
        primaryStage.setScene(scene);
        primaryStage.show();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), root);
        fadeIn.setFromValue(0); fadeIn.setToValue(1); fadeIn.play();
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private VBox buildHeader() {
        Text icon = new Text("⬡");
        icon.setStyle("-fx-fill: " + ACCENT + "; -fx-font-size: 22px;");

        Label title = new Label("Pascal–Fibonacci Key Derivation");
        title.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");

        Label badge = new Label("v2.0");
        badge.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-text-fill: " + ACCENT + "; -fx-border-color: " + ACCENT + "; -fx-border-radius: 3; -fx-padding: 1 6 1 6;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox titleRow = new HBox(12, icon, title, badge, spacer);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label seedLabel = new Label("SEED");
        seedLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_MUTED + ";");

        seedInput = new TextField();
        seedInput.setPromptText("Enter seed value…");
        seedInput.setPrefWidth(200); seedInput.setPrefHeight(36);
        seedInput.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 13px;" +
                "-fx-background-color: " + BG_INPUT + "; -fx-text-fill: " + TEXT_PRIMARY + ";" +
                "-fx-prompt-text-fill: " + TEXT_MUTED + "; -fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 0 12;");

        dijkstraBtn = buildStrategyBtn("Dijkstra", true);
        dfsBtn      = buildStrategyBtn("DFS", false);

        dijkstraBtn.setOnAction(e -> { selectedStrategy = GraphPathfinder.Strategy.DIJKSTRA; updateStrategyButtons(); });
        dfsBtn.setOnAction(e      -> { selectedStrategy = GraphPathfinder.Strategy.DFS;      updateStrategyButtons(); });

        startBtn     = buildPrimaryButton("▶  Run",            e -> handleRun());
        avalancheBtn = buildAvalancheButton("⚡  Avalanche Demo", e -> handleAvalanche());

        Button clearBtn = buildGhostButton("Clear", e -> {
            seedInput.clear();
            root.setCenter(buildCenter());
            setStatus("Canvas cleared.", TEXT_MUTED);
        });

        HBox controls = new HBox(10, seedInput, dijkstraBtn, dfsBtn, startBtn, avalancheBtn, clearBtn);
        controls.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(14, titleRow, new VBox(6, seedLabel, controls));
        header.setPadding(new Insets(20, 24, 18, 24));
        header.setStyle("-fx-background-color: " + BG_PANEL + ";" +
                "-fx-border-color: transparent transparent " + BORDER + " transparent;" +
                "-fx-border-width: 0 0 1 0;");
        return header;
    }

    private Button buildStrategyBtn(String text, boolean active) {
        Button btn = new Button(text);
        btn.setPrefHeight(36);
        btn.setStyle(getStrategyStyle(active));
        return btn;
    }

    private String getStrategyStyle(boolean active) {
        if (active)
            return "-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-font-weight: bold;" +
                    "-fx-background-color: " + ACCENT + "; -fx-text-fill: white;" +
                    "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 0 14; -fx-cursor: hand;";
        return "-fx-font-family: 'Courier New'; -fx-font-size: 12px;" +
                "-fx-background-color: " + BG_PANEL + "; -fx-text-fill: " + TEXT_MUTED + ";" +
                "-fx-border-color: " + BORDER + "; -fx-border-radius: 6; -fx-background-radius: 6;" +
                "-fx-padding: 0 14; -fx-cursor: hand;";
    }

    private void updateStrategyButtons() {
        dijkstraBtn.setStyle(getStrategyStyle(selectedStrategy == GraphPathfinder.Strategy.DIJKSTRA));
        dfsBtn.setStyle(getStrategyStyle(selectedStrategy == GraphPathfinder.Strategy.DFS));
    }

    // ── Center ────────────────────────────────────────────────────────────────
    private VBox buildCenter() {
        canvas = new Canvas(CANVAS_W, CANVAS_H);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web(BG_DARK));
        gc.fillRect(0, 0, CANVAS_W, CANVAS_H);
        drawGrid(gc, CANVAS_W, CANVAS_H);

        scroll = new ScrollPane(canvas);
        scroll.setPannable(true);
        scroll.setStyle("-fx-background-color: " + BG_DARK + "; -fx-background: " + BG_DARK + ";");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox center = new VBox(scroll);
        center.setStyle("-fx-background-color: " + BG_DARK + ";");
        return center;
    }

    // ── Status Bar ────────────────────────────────────────────────────────────
    private HBox buildStatusBar() {
        statusLabel = new Label("Ready — select a strategy and enter a seed.");
        statusLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: " + TEXT_MUTED + ";");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(140); progressBar.setPrefHeight(6);
        progressBar.setVisible(false);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label coords = new Label("Canvas: " + (int)CANVAS_W + " × " + (int)CANVAS_H + " px");
        coords.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: " + TEXT_MUTED + ";");

        HBox bar = new HBox(14, statusLabel, progressBar, spacer, coords);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 20, 8, 20));
        bar.setStyle("-fx-background-color: " + BG_PANEL + ";" +
                "-fx-border-color: " + BORDER + " transparent transparent transparent;" +
                "-fx-border-width: 1 0 0 0;");
        return bar;
    }

    // ── Normal Run ────────────────────────────────────────────────────────────
    private void handleRun() {
        String raw = seedInput.getText().trim();
        if (raw.isEmpty()) { setStatus("⚠  Please enter a seed value.", WARNING); shake(seedInput); return; }
        long seed;
        try { seed = Long.parseLong(raw); }
        catch (NumberFormatException ex) { setStatus("⚠  Invalid seed format.", WARNING); shake(seedInput); return; }

        // restore single canvas if coming from split screen
        root.setCenter(buildCenter());

        startBtn.setDisable(true);
        avalancheBtn.setDisable(true);
        progressBar.setProgress(0); progressBar.setVisible(true);

        String strategyName = selectedStrategy == GraphPathfinder.Strategy.DIJKSTRA ? "Dijkstra" : "DFS";
        setStatus("Running " + strategyName + "…", ACCENT);

        State state = new State(seed, selectedStrategy);
        state.runAll();

        Node[] path   = state.pathHistory;
        Node[] remote = state.remoteHistory;

        int maxRow = 0, totalSteps = 0;
        for (int i = 0; i < state.totalSteps; i++) {
            if (path[i] == null || path[i + 1] == null) break;
            if (path[i + 1].row > maxRow) maxRow = path[i + 1].row;
            totalSteps++;
        }
        final int total = totalSteps;
        final double startX = CANVAS_W / 2.0;

        int wormholes = 0;
        for (int i = 0; i <= totalSteps; i++)
            if (path[i] != null && path[i].isWormhole) wormholes++;
        final int wormholeCount = wormholes;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web(BG_DARK));
        gc.fillRect(0, 0, CANVAS_W, CANVAS_H);
        drawGrid(gc, CANVAS_W, CANVAS_H);
        drawPascalTriangle(gc, startX, maxRow, CANVAS_W, CANVAS_H);

        scroll.setHvalue(0.5); scroll.setVvalue(0.0);

        int[] stepHolder = {0};
        Timeline anim = new Timeline();
        anim.setCycleCount(total);
        anim.getKeyFrames().add(new KeyFrame(Duration.millis(STEP_DELAY_MS), e -> {
            int i = stepHolder[0];
            drawStep(gc, path, remote, i, startX, ACCENT);
            progressBar.setProgress((double)(i + 1) / total);
            stepHolder[0]++;
        }));

        anim.setOnFinished(e -> {
            gc.setFill(Color.web(SUCCESS, 0.9));
            gc.setFont(Font.font("Courier New", FontWeight.BOLD, 14));
            gc.fillText("Seed: " + seed + "   Strategy: " + strategyName
                    + "   Steps: " + total
                    + "   Target: row=" + state.targetNode.row + " col=" + state.targetNode.col, 30, 38);
            drawLegend(gc, strategyName, wormholeCount, 30, 60);
            gc.setFont(Font.font("Courier New", FontWeight.NORMAL, 10));
            gc.setFill(Color.web(TEXT_MUTED));
            gc.fillText("Derived key: " + Long.toHexString(state.seed).toUpperCase(), 30, CANVAS_H - 30);
            progressBar.setVisible(false);
            startBtn.setDisable(false);
            avalancheBtn.setDisable(false);
            setStatus("✓ Done  [" + strategyName + "]  seed=" + seed
                    + "  steps=" + total + "  wormholes=" + wormholeCount
                    + "  target=row:" + state.targetNode.row + " col:" + state.targetNode.col
                    + "  key=" + Long.toHexString(state.seed).toUpperCase(), SUCCESS);
        });

        anim.play();
    }

    // ── Avalanche Demo ────────────────────────────────────────────────────────
    private void handleAvalanche() {
        String raw = seedInput.getText().trim();
        if (raw.isEmpty()) { setStatus("⚠  Please enter a seed value.", WARNING); shake(seedInput); return; }
        long seedA;
        try { seedA = Long.parseLong(raw); }
        catch (NumberFormatException ex) { setStatus("⚠  Invalid seed format.", WARNING); shake(seedInput); return; }

        // flip bit 0 for seed B
        long seedB = seedA ^ 1L;

        startBtn.setDisable(true);
        avalancheBtn.setDisable(true);
        progressBar.setProgress(0); progressBar.setVisible(true);
        setStatus("⚡ Running Avalanche Demo  [seed=" + seedA + "  vs  seed=" + seedB + "]…", WARNING);

        // run both states
        State stateA = new State(seedA, selectedStrategy);
        State stateB = new State(seedB, selectedStrategy);
        stateA.runAll();
        stateB.runAll();

        // build two canvases side by side
        Canvas canvasA = new Canvas(SPLIT_W, SPLIT_H);
        Canvas canvasB = new Canvas(SPLIT_W, SPLIT_H);
        GraphicsContext gcA = canvasA.getGraphicsContext2D();
        GraphicsContext gcB = canvasB.getGraphicsContext2D();

        gcA.setFill(Color.web(BG_DARK)); gcA.fillRect(0, 0, SPLIT_W, SPLIT_H);
        gcB.setFill(Color.web(BG_DARK)); gcB.fillRect(0, 0, SPLIT_W, SPLIT_H);
        drawGrid(gcA, SPLIT_W, SPLIT_H);
        drawGrid(gcB, SPLIT_W, SPLIT_H);

        // labels above each canvas
        Label labelA = new Label("Seed: " + seedA);
        labelA.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + ACCENT + ";");
        Label labelB = new Label("Seed: " + seedB + "  (bit 0 flipped)");
        labelB.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + ACCENT2 + ";");

        ScrollPane scrollA = new ScrollPane(canvasA);
        ScrollPane scrollB = new ScrollPane(canvasB);
        scrollA.setPannable(true); scrollB.setPannable(true);
        scrollA.setStyle("-fx-background-color: " + BG_DARK + "; -fx-background: " + BG_DARK + ";");
        scrollB.setStyle("-fx-background-color: " + BG_DARK + "; -fx-background: " + BG_DARK + ";");
        HBox.setHgrow(scrollA, Priority.ALWAYS);
        HBox.setHgrow(scrollB, Priority.ALWAYS);

        VBox panelA = new VBox(6, labelA, scrollA);
        VBox panelB = new VBox(6, labelB, scrollB);
        panelA.setStyle("-fx-background-color: " + BG_DARK + ";");
        panelB.setStyle("-fx-background-color: " + BG_DARK + ";");
        HBox.setHgrow(panelA, Priority.ALWAYS);
        HBox.setHgrow(panelB, Priority.ALWAYS);

        // divider
        Region divider = new Region();
        divider.setPrefWidth(2);
        divider.setStyle("-fx-background-color: " + BORDER + ";");

        HBox splitPane = new HBox(divider, panelA, panelB);
        splitPane.setStyle("-fx-background-color: " + BG_DARK + ";");
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        root.setCenter(new VBox(splitPane));

        // draw triangles
        int maxRowA = getMaxRow(stateA);
        int maxRowB = getMaxRow(stateB);
        double startXA = SPLIT_W / 2.0;
        double startXB = SPLIT_W / 2.0;

        drawPascalTriangle(gcA, startXA, maxRowA, SPLIT_W, SPLIT_H);
        drawPascalTriangle(gcB, startXB, maxRowB, SPLIT_W, SPLIT_H);

        scrollA.setHvalue(0.5); scrollA.setVvalue(0.0);
        scrollB.setHvalue(0.5); scrollB.setVvalue(0.0);

        Node[] pathA   = stateA.pathHistory;
        Node[] remoteA = stateA.remoteHistory;
        Node[] pathB   = stateB.pathHistory;
        Node[] remoteB = stateB.remoteHistory;

        final int totalA = stateA.totalSteps;
        final int totalB = stateB.totalSteps;
        final int totalMax = Math.max(totalA, totalB);

        int[] stepHolder = {0};
        Timeline anim = new Timeline();
        anim.setCycleCount(totalMax);
        anim.getKeyFrames().add(new KeyFrame(Duration.millis(STEP_DELAY_MS), e -> {
            int i = stepHolder[0];
            if (i < totalA - 1 && pathA[i] != null && pathA[i + 1] != null)
                drawStep(gcA, pathA, remoteA, i, startXA, ACCENT);
            if (i < totalB - 1 && pathB[i] != null && pathB[i + 1] != null)
                drawStep(gcB, pathB, remoteB, i, startXB, ACCENT2);
            progressBar.setProgress((double)(i + 1) / totalMax);
            stepHolder[0]++;
        }));

        anim.setOnFinished(e -> {
            // draw final key labels on canvases
            String keyA = Long.toHexString(stateA.seed).toUpperCase();
            String keyB = Long.toHexString(stateB.seed).toUpperCase();
            int bitDiff = Long.bitCount(stateA.seed ^ stateB.seed);

            gcA.setFill(Color.web(SUCCESS, 0.9));
            gcA.setFont(Font.font("Courier New", FontWeight.BOLD, 13));
            gcA.fillText("Key: " + keyA, 30, 38);

            gcB.setFill(Color.web(ACCENT2, 0.9));
            gcB.setFont(Font.font("Courier New", FontWeight.BOLD, 13));
            gcB.fillText("Key: " + keyB, 30, 38);

            progressBar.setVisible(false);
            startBtn.setDisable(false);
            avalancheBtn.setDisable(false);

            setStatus("⚡ Avalanche complete!  Key A: " + keyA
                    + "  |  Key B: " + keyB
                    + "  |  Bits differ: " + bitDiff + "/64"
                    + "  (" + (bitDiff * 100 / 64) + "% of bits changed by flipping 1 bit!)", SUCCESS);

            // update labels
            labelA.setText("Seed: " + seedA + "  →  Key: " + keyA
                    + "   |   Target: row=" + stateA.targetNode.row + " col=" + stateA.targetNode.col);
            labelB.setText("Seed: " + seedB + "  (1 bit diff)  →  Key: " + keyB
                    + "   |   Target: row=" + stateB.targetNode.row + " col=" + stateB.targetNode.col);
        });

        anim.play();
    }

    private int getMaxRow(State state) {
        int maxRow = 0;
        for (int i = 0; i < state.totalSteps; i++) {
            if (state.pathHistory[i] == null || state.pathHistory[i + 1] == null) break;
            if (state.pathHistory[i + 1].row > maxRow) maxRow = state.pathHistory[i + 1].row;
        }
        return maxRow;
    }

    // ── Drawing helpers ───────────────────────────────────────────────────────
    private void drawStep(GraphicsContext gc, Node[] path, Node[] remote, int i, double startX, String lineColor) {
        double x1 = startX + (path[i].col     - path[i].row     / 2.0) * X_SPACING;
        double y1 = START_Y +  path[i].row     * Y_SPACING;
        double x2 = startX + (path[i+1].col   - path[i+1].row   / 2.0) * X_SPACING;
        double y2 = START_Y +  path[i+1].row   * Y_SPACING;

        gc.setStroke(Color.web(lineColor));
        gc.setLineWidth(2.5); gc.setLineDashes(0);
        gc.strokeLine(x1, y1, x2, y2);

        String dotColor = path[i].isWormhole ? WORMHOLE_COLOR : SUCCESS;
        gc.setFill(Color.web(dotColor));
        gc.fillOval(x1 - RADIUS, y1 - RADIUS, RADIUS * 2, RADIUS * 2);

        if (remote[i] != null && remote[i + 1] != null) {
            double rx1 = startX + (remote[i].col     - remote[i].row     / 2.0) * X_SPACING;
            double ry1 = START_Y +  remote[i].row     * Y_SPACING;
            double rx2 = startX + (remote[i+1].col   - remote[i+1].row   / 2.0) * X_SPACING;
            double ry2 = START_Y +  remote[i+1].row   * Y_SPACING;
            gc.setStroke(Color.web(REMOTE_COLOR));
            gc.setLineWidth(1.2); gc.setLineDashes(4, 4);
            gc.strokeLine(rx1, ry1, rx2, ry2);
            gc.setLineDashes(0);
            gc.setFill(Color.web(REMOTE_COLOR, 0.8));
            gc.fillOval(rx1 - 2, ry1 - 2, 4, 4);
        }
    }

    private void drawGrid(GraphicsContext gc, double w, double h) {
        gc.setStroke(Color.web(BORDER, 0.35));
        gc.setLineWidth(0.5);
        for (int x = 0; x < w; x += 80) gc.strokeLine(x, 0, x, h);
        for (int y = 0; y < h; y += 80) gc.strokeLine(0, y, w, y);
    }

    private void drawPascalTriangle(GraphicsContext gc, double startX, int maxRow, double w, double h) {
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

    // ── Button builders ───────────────────────────────────────────────────────
    private Button buildPrimaryButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.setPrefHeight(36);
        String base = "-fx-font-family: 'Courier New'; -fx-font-size: 13px; -fx-font-weight: bold;" +
                "-fx-background-color: " + ACCENT + "; -fx-text-fill: #ffffff;" +
                "-fx-background-radius: 6; -fx-padding: 0 20; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace(ACCENT, ACCENT_HOVER)));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        btn.setOnAction(handler);
        return btn;
    }

    private Button buildAvalancheButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.setPrefHeight(36);
        String base = "-fx-font-family: 'Courier New'; -fx-font-size: 13px; -fx-font-weight: bold;" +
                "-fx-background-color: #7c2fa8; -fx-text-fill: #ffffff;" +
                "-fx-background-radius: 6; -fx-padding: 0 20; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace("#7c2fa8", "#9b3fd4")));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        btn.setOnAction(handler);
        return btn;
    }

    private Button buildGhostButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.setPrefHeight(36);
        String base = "-fx-font-family: 'Courier New'; -fx-font-size: 12px;" +
                "-fx-background-color: transparent; -fx-text-fill: " + TEXT_MUTED + ";" +
                "-fx-border-color: " + BORDER + "; -fx-border-radius: 6;" +
                "-fx-background-radius: 6; -fx-padding: 0 16; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace(TEXT_MUTED, TEXT_PRIMARY).replace(BORDER, TEXT_MUTED)));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        btn.setOnAction(handler);
        return btn;
    }

    private void setStatus(String msg, String hex) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: " + hex + ";");
    }

    private void shake(TextField field) {
        double ox = field.getTranslateX();
        new Timeline(
                new KeyFrame(Duration.millis(0),   ev -> field.setTranslateX(ox)),
                new KeyFrame(Duration.millis(55),  ev -> field.setTranslateX(ox - 7)),
                new KeyFrame(Duration.millis(110), ev -> field.setTranslateX(ox + 7)),
                new KeyFrame(Duration.millis(165), ev -> field.setTranslateX(ox - 5)),
                new KeyFrame(Duration.millis(220), ev -> field.setTranslateX(ox + 5)),
                new KeyFrame(Duration.millis(275), ev -> field.setTranslateX(ox))
        ).play();
    }

    public static void main(String[] args) { launch(args); }
}