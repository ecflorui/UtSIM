package unittest.listeners;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import unittest.gui.TestGUI;
import unittest.results.TestMethodResult;

public class GUITestListener implements TestListener {

    private TestGUI gui;
    private Label statusLabel;
    private Label nameLabel;
    private String currentClassName;

    public GUITestListener(TestGUI gui) {
        this.gui = gui;
    }

    public void setCurrentClass(String className) {
        this.currentClassName = className;
    }

    @Override
    public void testStarted(String testMethod) {
        nameLabel = new Label(currentClassName + "." + testMethod);
        nameLabel.setStyle(
                "-fx-text-fill: #cccccc; -fx-font-family: monospace; -fx-font-size: 13px;"
        );

        statusLabel = new Label("⏳ RUNNING");
        statusLabel.setStyle(
                "-fx-text-fill: #ff9800; -fx-font-family: monospace; " + "-fx-font-weight: bold; -fx-font-size: 13px;"
        );
        statusLabel.setMinWidth(100);

        Label sLabel = statusLabel;
        Label nLabel = nameLabel;

        //delay till render, just so it's visible
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

        Platform.runLater(() -> {
            HBox row = new HBox(10);
            row.setPadding(new Insets(8, 12, 8, 12));
            row.setStyle(
                    "-fx-background-color: #2a2a2a; " +
                            "-fx-background-radius: 6; " +
                            "-fx-border-color: #3a3a3a; " +
                            "-fx-border-radius: 6;"
            );
            row.getChildren().addAll(sLabel, nLabel);
            gui.addCard(row);
            latch.countDown(); // signal that card is added
        });

        try {
            latch.await();
            Thread.sleep(100); // small extra pause i added so user can see RUNNING
        } catch (InterruptedException e) {}
    }

    @Override
    public void testSucceeded(TestMethodResult result) {
        Label s = statusLabel;
        Platform.runLater(() -> {
            s.setText("✨ PASS");
            s.setStyle(
                    "-fx-text-fill: #b39ddb; -fx-font-family: monospace; " +
                            "-fx-font-weight: bold; -fx-font-size: 13px;"
            );
        });
    }

    @Override
    public void testFailed(TestMethodResult result) {
        Label s = statusLabel;
        Platform.runLater(() -> {
            s.setText("❌ FAIL");
            s.setStyle(
                    "-fx-text-fill: #f44336; -fx-font-family: monospace; " +
                            "-fx-font-weight: bold; -fx-font-size: 13px;"
            );
        });
    }
}