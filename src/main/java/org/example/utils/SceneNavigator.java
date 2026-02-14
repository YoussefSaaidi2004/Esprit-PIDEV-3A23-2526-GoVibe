package org.example.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public final class SceneNavigator {

    private static final double APP_WIDTH = 1920;
    private static final double APP_HEIGHT = 1080;

    private SceneNavigator() {
    }

    public static void switchTo(String fxmlPath, Node source) {
        Stage stage = (Stage) source.getScene().getWindow();
        switchTo(fxmlPath, stage);
    }

    public static void switchTo(String fxmlPath, Stage stage) {
        try {
            Parent root = FXMLLoader.load(SceneNavigator.class.getResource(fxmlPath));
            Scene scene = new Scene(root, APP_WIDTH, APP_HEIGHT);
            stage.setScene(scene);
            stage.setMinWidth(APP_WIDTH);
            stage.setMinHeight(APP_HEIGHT);
            stage.setWidth(APP_WIDTH);
            stage.setHeight(APP_HEIGHT);
        } catch (IOException e) {
            throw new RuntimeException("Erreur de navigation vers " + fxmlPath, e);
        }
    }
}
