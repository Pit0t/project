import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class MainGUI extends Application {

    @Override
    public void start(Stage primaryStage) {


        BorderPane root = new BorderPane();

        HBox topPanel = new HBox(15);
        topPanel.setAlignment(Pos.CENTER);
        topPanel.setStyle("-fx-padding: 15px; -fx-background-color: #e0e0e0;");

        TextField seedInput = new TextField();
        seedInput.setPromptText("Enter Seed...");

        Button startBtn = new Button("start encryption");


        topPanel.getChildren().addAll(seedInput, startBtn);
        root.setTop(topPanel);


        Canvas canvas = new Canvas(4000, 6000);
        GraphicsContext gc = canvas.getGraphicsContext2D(); // נשמור אותו לעתיד כדי לצייר

        ScrollPane scrollPane = new ScrollPane(canvas);
        scrollPane.setPannable(true); // מאפשר גרירה עם העכבר
        root.setCenter(scrollPane);

        // 4. הרמת המסך (Showtime)
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Pascal-Fibonacci-encryption");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}