package unittest.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.DirectoryChooser;


import unittest.annotations.Ordered;
import unittest.annotations.Test;
import unittest.listeners.GUITestListener;
import unittest.results.TestClassResult;
import unittest.results.TestMethodResult;
import unittest.runners.OrderedTestRunner;
import unittest.runners.TestRunner;
import unittest.results.TestClassResult;
import unittest.results.TestMethodResult;


import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class TestGUI extends Application {

    private ListView<CheckBox> classListView;
    private VBox resultBox;
    private ScrollPane resultScroll;;
    private TextField pathInput;
    private Button runButton;
    private TestController controller;


    @Override
    public void start(Stage stage) {

        stage.setTitle("Java Unit Testing Framework");
        controller = new TestController(this);


        HBox topBox = new HBox(10);
        topBox.setPadding(new Insets(10));
        topBox.setStyle("-fx-background-color: #2d2d2d;");
        Label pathLabel = new Label("Class Directory Path:");
        pathLabel.setStyle("-fx-text-fill: #cccccc;");
        pathInput = new TextField("./bin"); // Default path assuming VS Code compiles to 'bin'
        pathInput.setPrefWidth(700);
        pathInput.setStyle("-fx-background-color: #3c3c3c; -fx-text-fill: #ffffff; -fx-border-color: #555555;");


        // to browse directory
        Button browseButton = new Button("Browse");
        browseButton.setStyle("-fx-background-color: #3c3c3c; -fx-text-fill: #cccccc;");
        browseButton.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Your Class Output Directory");
            File chosen = dc.showDialog(stage);
            if (chosen != null) {
                pathInput.setText(chosen.getAbsolutePath());
            }
        });

        Button searchButton = new Button("Find Tests");
        searchButton.setStyle("-fx-background-color: #0078d4; -fx-text-fill: white; -fx-font-weight: bold;");
        searchButton.setOnAction(e -> searchForTestClasses());
        topBox.getChildren().addAll(pathLabel, pathInput, browseButton, searchButton);


        VBox leftBox = new VBox(5);
        leftBox.setPadding(new Insets(10));
        leftBox.setStyle("-fx-background-color: #252525;");
        leftBox.setPrefWidth(220);
        Label leftTitle = new Label("Test Classes Found:");
        leftTitle.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12px; -fx-font-weight: bold;");
        leftBox.getChildren().add(leftTitle);;
        classListView = new ListView<>();
        classListView.setStyle(
                "-fx-background-color: #252525; " + "-fx-border-color: #3c3c3c; " +
                        "-fx-control-inner-background: #252525;"
        );
        classListView.setPrefWidth(220);
        leftBox.getChildren().add(classListView);


        VBox centerBox = new VBox(5);
        centerBox.setPadding(new Insets(10));
        centerBox.setStyle("-fx-background-color: #1e1e1e;");
        Label centerTitle = new Label("Test Execution Output:");
        centerTitle.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12px; -fx-font-weight: bold;");
        centerBox.getChildren().add(centerTitle);

        resultBox = new VBox(4);
        resultBox.setPadding(new Insets(8));
        resultBox.setStyle("-fx-background-color: #1e1e1e;");

        resultScroll = new ScrollPane(resultBox);
        resultScroll.setFitToWidth(true);
        resultScroll.setStyle("-fx-background-color: #1e1e1e; -fx-background: #1e1e1e;");
        centerBox.getChildren().add(resultScroll);
        VBox.setVgrow(resultScroll, Priority.ALWAYS);


        HBox bottomBox = new HBox(10);
        bottomBox.setPadding(new Insets(10));
        bottomBox.setStyle("-fx-background-color: #2d2d2d;");
        runButton = new Button("Run Selected Tests");
        runButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        runButton.setOnAction(e -> runSelectedTests());
        Button clearButton = new Button("Clear Output");
        clearButton.setStyle("-fx-background-color: #3c3c3c; -fx-text-fill: #cccccc;");
        clearButton.setOnAction(e -> clearRes());
        bottomBox.getChildren().addAll(runButton, clearButton);


        BorderPane mainLayout = new BorderPane();
        mainLayout.setTop(topBox);
        mainLayout.setLeft(leftBox);
        mainLayout.setCenter(centerBox);
        mainLayout.setBottom(bottomBox);

        Scene scene = new Scene(mainLayout, 1000, 700);;
        stage.setScene(scene);
        stage.show();
    }

    // Called by GUITestListener to update the screen safely
//    public void logToOutput(String message) {
//        outputArea.appendText(message);
//    }

    public void addResult(Label res) {
        Platform.runLater(() -> {
            HBox card = new HBox();
            card.setPadding(new Insets(6, 10, 6, 10));
            card.setStyle("-fx-background-color: #2d2d2d; -fx-background-radius: 6;");
            card.getChildren().add(res);
            resultBox.getChildren().add(card);
            resultScroll.setVvalue(1.0);
        });
    }

    public void clearRes() {
        resultBox.getChildren().clear();
    }

    public void addCard(HBox card) {
        Platform.runLater(() -> {
            resultBox.getChildren().add(card);
            resultScroll.setVvalue(1.0);
        });
    }

    public void addMessage(String message) {
        Platform.runLater(() -> {
            Label label = new Label(message);
            label.setStyle("-fx-text-fill: #888888; -fx-font-family: monospace; -fx-font-size: 11px;");
            resultBox.getChildren().add(label);
            resultScroll.setVvalue(1.0);
        });
    }

    private void searchForTestClasses() {
        resultBox.getChildren().clear();
        classListView.getItems().clear();

        String basePath = pathInput.getText();
        addMessage("Searching in: " + basePath);

        List<String> found = controller.findTestClasses(basePath);

        if (found.isEmpty()) {
            addMessage("No test classes found.");
            return;
        }

        for (String className : found) {
            CheckBox cb = new CheckBox(className);
            cb.setSelected(true);
            cb.setStyle("-fx-text-fill: #cccccc; -fx-mark-color: #b39ddb;");
            classListView.getItems().add(cb);
        }

        addMessage("Found " + found.size() + " test classes.");
    }


    private void runSelectedTests() {
        List<String> classesToRun = new ArrayList<>();
        for (CheckBox cb : classListView.getItems()) {
            if (cb.isSelected()) {
                classesToRun.add(cb.getText());
            }
        }

        if (classesToRun.isEmpty()) {
            addMessage("No test classes selected.");
            return;
        }

        runButton.setDisable(true);
        clearRes();
        addMessage("--- STARTING TEST EXECUTION ---");
        controller.runTests(classesToRun);
    }

    public void showSummary(List<TestClassResult> allResults, int totalRun, int totalFail) {
        javafx.application.Platform.runLater(() -> {
            addMessage("════════════════════════════════");
            addMessage("Tests run: " + totalRun + ", Failures: " + totalFail);

            if (totalFail > 0) {
                addMessage("--- FAILURES ---");
                for (TestClassResult classResult : allResults) {
                    for (TestMethodResult methodResult : classResult.getTestMethodResults()) {
                        if (!methodResult.isPass()) {
                            javafx.scene.control.Label failLabel = new javafx.scene.control.Label(
                                    "  " + classResult.getTestClassName() + "." + methodResult.getName() + ":"
                            );
                            failLabel.setStyle("-fx-text-fill: #f44336; -fx-font-family: monospace;");
                            addResult(failLabel);

                            javafx.scene.control.Label exLabel = new javafx.scene.control.Label(
                                    "    " + methodResult.getException().getClass().getName()
                            );
                            exLabel.setStyle("-fx-text-fill: #ff9800; -fx-font-family: monospace;");
                            addResult(exLabel);

                            StackTraceElement[] trace = methodResult.getException().getStackTrace();
                            for (StackTraceElement element : trace) {
                                javafx.scene.control.Label traceLabel = new javafx.scene.control.Label(
                                        "      at " + element.toString()
                                );
                                traceLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-family: monospace;");
                                addResult(traceLabel);
                                if (!element.getClassName().startsWith("unittest.assertions")) break;
                            }
                        }
                    }
                }
            }

            addMessage("════════════════════════════════");
            runButton.setDisable(false);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}