import Graphs.State;
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

    private static final String BG_DARK      = "#0d0f14";
    private static final String BG_PANEL     = "#13161e";
    private static final String BG_INPUT     = "#1a1e2a";
    private static final String ACCENT       = "#4f8ef7";
    private static final String ACCENT_HOVER = "#6aa3ff";
    private static final String BORDER       = "#2a2f40";
    private static final String TEXT_PRIMARY = "#e8ecf4";
    private static final String TEXT_MUTED   = "#5a6180";
    private static final String SUCCESS      = "#3dd68c";
    private static final String WARNING      = "#f0a04a";
    private static final String REMOTE_COLOR = "#ff4444";

    private static final int STEP_DELAY_MS = 30;

    private static final double CANVAS_W  = 6000;
    private static final double CANVAS_H  = 4000;
    private static final double START_Y   = 60;
    private static final double X_SPACING = 20;
    private static final double Y_SPACING = 15;
    private static final double RADIUS    = 3;

    private Label       statusLabel;
    private ProgressBar progressBar;
    private Button      startBtn;
    private TextField   seedInput;
    private Canvas      canvas;
    private ScrollPane  scroll;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG_DARK + ";");
        root.setTop(buildHeader());
        root.setCenter(buildCenter());
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 960, 680);
        scene.setFill(Color.web(BG_DARK));

        primaryStage.setTitle("Pascal · Fibonacci · Key Derivation");
        primaryStage.setMinWidth(720);
        primaryStage.setMinHeight(500);
        primaryStage.setScene(scene);
        primaryStage.show();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // HEADER
    // ──────────────────────────────────────────────────────────────────────────
    private VBox buildHeader() {
        Text icon = new Text("⬡");
        icon.setStyle("-fx-fill: " + ACCENT + "; -fx-font-size: 22px;");

        Label title = new Label("Pascal–Fibonacci Key Derivation");
        title.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");

        Label badge = new Label("v1.2");
        badge.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-text-fill: " + ACCENT + "; -fx-border-color: " + ACCENT + "; -fx-border-radius: 3; -fx-padding: 1 6 1 6;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox titleRow = new HBox(12, icon, title, badge, spacer);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label seedLabel = new Label("SEED");
        seedLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_MUTED + ";");

        seedInput = new TextField();
        seedInput.setPromptText("Enter seed value…");
        seedInput.setPrefWidth(240);
        seedInput.setPrefHeight(36);
        seedInput.setStyle(
                "-fx-font-family: 'Courier New'; -fx-font-size: 13px;" +
                        "-fx-background-color: " + BG_INPUT + "; -fx-text-fill: " + TEXT_PRIMARY + ";" +
                        "-fx-prompt-text-fill: " + TEXT_MUTED + "; -fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 0 12;"
        );

        startBtn = buildPrimaryButton("▶  Run Derivation", e -> handleEncrypt());

        Button clearBtn = buildGhostButton("Clear", e -> {
            seedInput.clear();
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.clearRect(0, 0, CANVAS_W, CANVAS_H);
            gc.setFill(Color.web(BG_DARK));
            gc.fillRect(0, 0, CANVAS_W, CANVAS_H);
            drawGrid(gc);
            setStatus("Canvas cleared.", TEXT_MUTED);
        });

        HBox controls = new HBox(10, seedInput, startBtn, clearBtn);
        controls.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(14, titleRow, new VBox(6, seedLabel, controls));
        header.setPadding(new Insets(20, 24, 18, 24));
        header.setStyle(
                "-fx-background-color: " + BG_PANEL + ";" +
                        "-fx-border-color: transparent transparent " + BORDER + " transparent;" +
                        "-fx-border-width: 0 0 1 0;"
        );
        return header;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CENTER CANVAS
    // ──────────────────────────────────────────────────────────────────────────
    private VBox buildCenter() {
        canvas = new Canvas(CANVAS_W, CANVAS_H);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web(BG_DARK));
        gc.fillRect(0, 0, CANVAS_W, CANVAS_H);
        drawGrid(gc);

        scroll = new ScrollPane(canvas);
        scroll.setPannable(true);
        scroll.setStyle("-fx-background-color: " + BG_DARK + "; -fx-background: " + BG_DARK + ";");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox center = new VBox(scroll);
        center.setStyle("-fx-background-color: " + BG_DARK + ";");
        return center;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // STATUS BAR
    // ──────────────────────────────────────────────────────────────────────────
    private HBox buildStatusBar() {
        statusLabel = new Label("Ready.");
        statusLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: " + TEXT_MUTED + ";");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(140);
        progressBar.setPrefHeight(6);
        progressBar.setVisible(false);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label coords = new Label("Canvas: " + (int) CANVAS_W + " × " + (int) CANVAS_H + " px");
        coords.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: " + TEXT_MUTED + ";");

        HBox bar = new HBox(14, statusLabel, progressBar, spacer, coords);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 20, 8, 20));
        bar.setStyle(
                "-fx-background-color: " + BG_PANEL + ";" +
                        "-fx-border-color: " + BORDER + " transparent transparent transparent;" +
                        "-fx-border-width: 1 0 0 0;"
        );
        return bar;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // DRAWING HELPERS
    // ──────────────────────────────────────────────────────────────────────────
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

    private void drawLegend(GraphicsContext gc) {
        double lx = 30, ly = 60;
        gc.setFont(Font.font("Courier New", FontWeight.BOLD, 11));

        // Blue — physical walk
        gc.setStroke(Color.web(ACCENT));
        gc.setLineWidth(2.5);
        gc.setLineDashes(0);
        gc.strokeLine(lx, ly, lx + 24, ly);
        gc.setFill(Color.web(SUCCESS));
        gc.fillOval(lx + 24 - RADIUS, ly - RADIUS, RADIUS * 2, RADIUS * 2);
        gc.setFill(Color.web(TEXT_PRIMARY));
        gc.fillText("Physical walk (left / right child)", lx + 34, ly + 4);

        ly += 20;

        // Red — remote read
        gc.setStroke(Color.web(REMOTE_COLOR));
        gc.setLineWidth(1.2);
        gc.setLineDashes(4, 4);
        gc.strokeLine(lx, ly, lx + 24, ly);
        gc.setLineDashes(0);
        gc.setFill(Color.web(REMOTE_COLOR, 0.8));
        gc.fillOval(lx + 24 - 2, ly - 2, 4, 4);
        gc.setFill(Color.web(TEXT_PRIMARY));
        gc.fillText("Remote read (telepathic jump)", lx + 34, ly + 4);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // BUTTON BUILDERS
    // ──────────────────────────────────────────────────────────────────────────
    private Button buildPrimaryButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.setPrefHeight(36);
        String base =
                "-fx-font-family: 'Courier New'; -fx-font-size: 13px; -fx-font-weight: bold;" +
                        "-fx-background-color: " + ACCENT + "; -fx-text-fill: #ffffff;" +
                        "-fx-background-radius: 6; -fx-padding: 0 20; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace(ACCENT, ACCENT_HOVER)));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        btn.setOnAction(handler);
        return btn;
    }

    private Button buildGhostButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.setPrefHeight(36);
        String base =
                "-fx-font-family: 'Courier New'; -fx-font-size: 12px;" +
                        "-fx-background-color: transparent; -fx-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-border-color: " + BORDER + "; -fx-border-radius: 6;" +
                        "-fx-background-radius: 6; -fx-padding: 0 16; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace(TEXT_MUTED, TEXT_PRIMARY).replace(BORDER, TEXT_MUTED)));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        btn.setOnAction(handler);
        return btn;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // MAIN LOGIC
    // ──────────────────────────────────────────────────────────────────────────
    private void handleEncrypt() {
        String raw = seedInput.getText().trim();
        if (raw.isEmpty()) {
            setStatus("⚠  Please enter a seed value.", WARNING);
            shake(seedInput);
            return;
        }
        long seed;
        try {
            seed = Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            setStatus("⚠  Invalid seed format.", WARNING);
            shake(seedInput);
            return;
        }

        startBtn.setDisable(true);
        progressBar.setProgress(0);
        progressBar.setVisible(true);
        setStatus("Computing path…", ACCENT);

        State state = new State(seed);
        state.runAll();

        Node[] path   = state.pathHistory;
        Node[] remote = state.remoteHistory;

        int maxRow = 0, totalSteps = 0;
        for (int i = 0; i < state.totalSteps; i++) {
            if (path[i] == null || path[i + 1] == null) break;
            if (path[i + 1].row > maxRow) maxRow = path[i + 1].row;
            totalSteps++;
        }
        final int    total  = totalSteps;
        final double startX = CANVAS_W / 2.0;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web(BG_DARK));
        gc.fillRect(0, 0, CANVAS_W, CANVAS_H);
        drawGrid(gc);
        drawPascalTriangle(gc, startX, maxRow);

        scroll.setHvalue(0.5);
        scroll.setVvalue(0.0);

        int[] stepHolder = {0};
        Timeline drawAnim = new Timeline();
        drawAnim.setCycleCount(total);
        drawAnim.getKeyFrames().add(new KeyFrame(Duration.millis(STEP_DELAY_MS), e -> {
            int i = stepHolder[0];

            // ── Blue: physical walk ──────────────────────────────────────────
            double x1 = startX + (path[i].col     - path[i].row     / 2.0) * X_SPACING;
            double y1 = START_Y +  path[i].row     * Y_SPACING;
            double x2 = startX + (path[i + 1].col - path[i + 1].row / 2.0) * X_SPACING;
            double y2 = START_Y +  path[i + 1].row * Y_SPACING;

            gc.setStroke(Color.web(ACCENT));
            gc.setLineWidth(2.5);
            gc.setLineDashes(0);
            gc.strokeLine(x1, y1, x2, y2);
            gc.setFill(Color.web(SUCCESS));
            gc.fillOval(x1 - RADIUS, y1 - RADIUS, RADIUS * 2, RADIUS * 2);

            // ── Red: remote read ─────────────────────────────────────────────
            if (remote[i] != null && remote[i + 1] != null) {
                double rx1 = startX + (remote[i].col     - remote[i].row     / 2.0) * X_SPACING;
                double ry1 = START_Y +  remote[i].row     * Y_SPACING;
                double rx2 = startX + (remote[i + 1].col - remote[i + 1].row / 2.0) * X_SPACING;
                double ry2 = START_Y +  remote[i + 1].row * Y_SPACING;

                gc.setStroke(Color.web(REMOTE_COLOR));
                gc.setLineWidth(1.2);
                gc.setLineDashes(4, 4);
                gc.strokeLine(rx1, ry1, rx2, ry2);
                gc.setLineDashes(0);
                gc.setFill(Color.web(REMOTE_COLOR, 0.8));
                gc.fillOval(rx1 - 2, ry1 - 2, 4, 4);
            }

            progressBar.setProgress((double)(i + 1) / total);
            stepHolder[0]++;
        }));

        drawAnim.setOnFinished(e -> {
            gc.setFill(Color.web(SUCCESS, 0.9));
            gc.setFont(Font.font("Courier New", FontWeight.BOLD, 14));
            gc.fillText("Seed: " + seed + "  →  Key derivation path rendered.", 30, 38);

            drawLegend(gc);

            gc.setFont(Font.font("Courier New", FontWeight.NORMAL, 10));
            gc.setFill(Color.web(TEXT_MUTED));
            gc.fillText("derived key: " + state.seed, 30, CANVAS_H - 30);

            progressBar.setVisible(false);
            startBtn.setDisable(false);
            setStatus("✓ Done — seed " + seed + " | derived key: " + state.seed, SUCCESS);
        });

        drawAnim.play();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // UTILITIES
    // ──────────────────────────────────────────────────────────────────────────
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