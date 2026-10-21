
package minidb;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * MiniDB Studio
 * JavaFX user interface for the existing MiniDB engine.
 */
public class MiniDBJavaFX extends Application {

    // Existing MiniDB database engine
    private Database database;

    // SQL query editor
    private TextArea queryArea;

    // Query output and error messages
    private TextArea resultArea;

    // Status message shown at the bottom
    private Label statusLabel;

    /**
     * Creates the main JavaFX window.
     */
    @Override
    public void start(Stage stage) {

        // Load the existing database and its saved tables
        database = new Database("minidb_data");

        // Main window layout
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f1f5f9;");

        // Application heading
        Label title = new Label("MiniDB Studio");
        title.setStyle(
                "-fx-font-size: 25px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;"
        );

        Label subtitle = new Label("SQL Database Workbench");
        subtitle.setStyle(
                "-fx-font-size: 12px;" +
                        "-fx-text-fill: #cbd5e1;"
        );

        VBox heading = new VBox(4, title, subtitle);
        heading.setPadding(new Insets(18));

        HBox header = new HBox(heading);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #172554;");

        root.setTop(header);

        // Query editor heading
        Label editorTitle = new Label("SQL QUERY EDITOR");
        editorTitle.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #334155;"
        );

        // Create the SQL editor
        queryArea = new TextArea();
        queryArea.setPromptText(
                "Write your SQL query here...\n\n" +
                        "Example: SHOW TABLES;"
        );
        queryArea.setText("SHOW TABLES;");
        queryArea.setWrapText(false);
        queryArea.setPrefRowCount(10);
        queryArea.setStyle(
                "-fx-font-family: 'Consolas';" +
                        "-fx-font-size: 14px;" +
                        "-fx-control-inner-background: #0f172a;" +
                        "-fx-text-fill: #e2e8f0;" +
                        "-fx-highlight-fill: #2563eb;" +
                        "-fx-highlight-text-fill: white;"
        );

        // Run SQL query button
        Button runButton = new Button("▶  Run Query");
        runButton.setDefaultButton(false);
        runButton.setStyle(
                "-fx-background-color: #15803d;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 18 10 18;" +
                        "-fx-cursor: hand;"
        );

        // Clear query editor button
        Button clearButton = new Button("Clear Editor");
        clearButton.setStyle(
                "-fx-background-color: #e2e8f0;" +
                        "-fx-text-fill: #334155;" +
                        "-fx-padding: 10 16 10 16;" +
                        "-fx-cursor: hand;"
        );

        // Put the query buttons in one row
        HBox buttonBar = new HBox(10, runButton, clearButton);
        buttonBar.setAlignment(Pos.CENTER_LEFT);
        buttonBar.setPadding(new Insets(10, 0, 10, 0));

        VBox editorPanel = new VBox(
                10,
                editorTitle,
                queryArea,
                buttonBar
        );

        editorPanel.setPadding(new Insets(16));
        editorPanel.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 8;"
        );

        // Results panel heading
        Label resultTitle = new Label("QUERY RESULTS / MESSAGES");
        resultTitle.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #334155;"
        );

        // Displays the output returned by Database.execute()
        resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setWrapText(false);
        resultArea.setPromptText(
                "Query results and error messages will appear here."
        );
        resultArea.setPrefRowCount(10);
        resultArea.setStyle(
                "-fx-font-family: 'Consolas';" +
                        "-fx-font-size: 13px;" +
                        "-fx-control-inner-background: #ffffff;" +
                        "-fx-text-fill: #334155;"
        );

        VBox resultPanel = new VBox(10, resultTitle, resultArea);
        resultPanel.setPadding(new Insets(16));
        resultPanel.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 8;"
        );

        // Allow the editor and results panels to be resized
        SplitPane splitPane = new SplitPane(editorPanel, resultPanel);
        splitPane.setOrientation(
                javafx.geometry.Orientation.VERTICAL
        );
        splitPane.setDividerPositions(0.48);
        splitPane.setStyle("-fx-background-color: #f1f5f9;");

        root.setCenter(splitPane);
        BorderPane.setMargin(splitPane, new Insets(14));

        // Bottom status bar
        statusLabel = new Label("Ready | MiniDB connected");
        statusLabel.setStyle(
                "-fx-text-fill: #475569;" +
                        "-fx-font-size: 12px;"
        );

        HBox footer = new HBox(statusLabel);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(12, 18, 12, 18));
        footer.setStyle(
                "-fx-background-color: #e2e8f0;"
        );

        root.setBottom(footer);

        // Execute the query when Run Query is clicked
        runButton.setOnAction(event -> runQuery());

        // Clear the editor and output
        clearButton.setOnAction(event -> {
            queryArea.clear();
            resultArea.clear();
            statusLabel.setText("Editor cleared");
        });

        // Ctrl + Enter also runs the query
        queryArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown()
                    && event.getCode() == KeyCode.ENTER) {
                runQuery();
                event.consume();
            }
        });

        // Create and display the window
        Scene scene = new Scene(root, 1000, 700);

        stage.setTitle("MiniDB Studio");
        stage.setMinWidth(700);
        stage.setMinHeight(500);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Sends the SQL query to the existing database engine
     * and displays its returned result.
     */
    private void runQuery() {

        String sql = queryArea.getText().trim();

        // Do not execute an empty query
        if (sql.isEmpty()) {
            resultArea.setText("Please enter a SQL query.");
            statusLabel.setText("No query entered");
            return;
        }

        try {
            // Call the existing MiniDB execution method
            String result = database.execute(sql);

            // Display the actual result from MiniDB
            resultArea.setText(result.isEmpty()
                    ? "Query executed successfully."
                    : result);

            statusLabel.setText("Query executed successfully");

        } catch (Exception e) {

            // Show the database error in the results panel
            resultArea.setText("ERROR: " + e.getMessage());
            statusLabel.setText("Query failed");
        }
    }

    /**
     * Starts the JavaFX application.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
