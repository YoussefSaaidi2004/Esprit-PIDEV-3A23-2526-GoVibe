package org.example.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.entities.personne;

import java.io.IOException;
import java.util.function.Consumer;

public final class SceneNavigator {

    private static final double APP_WIDTH = 1920;
    private static final double APP_HEIGHT = 1080;

    private SceneNavigator() {
    }

    public static void switchTo(String fxmlPath, Node source) {
        if (source == null || source.getScene() == null) {
            // Si source est null ou n'a pas de scène, essayer d'obtenir le stage principal
            Stage stage = Stage.getWindows().stream()
                    .filter(w -> w instanceof Stage)
                    .map(w -> (Stage) w)
                    .filter(s -> s.getScene() != null)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Aucune fenêtre active trouvée"));
            switchTo(fxmlPath, stage, null);
        } else {
            Stage stage = (Stage) source.getScene().getWindow();
            switchTo(fxmlPath, stage, null);
        }
    }

    public static void switchTo(String fxmlPath, Node source, Consumer<Object> controllerInitializer) {
        if (source == null || source.getScene() == null) {
            Stage stage = Stage.getWindows().stream()
                    .filter(w -> w instanceof Stage)
                    .map(w -> (Stage) w)
                    .filter(s -> s.getScene() != null)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Aucune fenêtre active trouvée"));
            switchTo(fxmlPath, stage, controllerInitializer);
        } else {
            Stage stage = (Stage) source.getScene().getWindow();
            switchTo(fxmlPath, stage, controllerInitializer);
        }
    }

    public static void switchTo(String fxmlPath, Stage stage, Consumer<Object> controllerInitializer) {
        try {
            String normalizedPath = fxmlPath.startsWith("/") ? fxmlPath.substring(1) : fxmlPath;
            var resource = SceneNavigator.class.getClassLoader().getResource(normalizedPath);
            if (resource == null) {
                resource = SceneNavigator.class.getClassLoader().getResource(fxmlPath);
            }
            if (resource == null) {
                throw new IOException("FXML non trouvé: " + fxmlPath);
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            
            // Initialize controller with data if provided
            if (controllerInitializer != null) {
                Object controller = loader.getController();
                controllerInitializer.accept(controller);
            }
            
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
