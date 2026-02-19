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
        if (source == null) {
            // Si source est null, essayer d'obtenir le stage principal
            Stage stage = Stage.getWindows().stream()
                    .filter(w -> w instanceof Stage)
                    .map(w -> (Stage) w)
                    .filter(s -> s.getScene() != null)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Aucune fenêtre active trouvée"));
            switchTo(fxmlPath, stage);
        } else {
            Stage stage = (Stage) source.getScene().getWindow();
            switchTo(fxmlPath, stage);
        }
    }

    public static void switchTo(String fxmlPath, Stage stage) {
        try {
            // Utilise le classLoader racine pour trouver les FXML dans resources/
            String normalizedPath = fxmlPath.startsWith("/") ? fxmlPath.substring(1) : fxmlPath;
            var resource = SceneNavigator.class.getClassLoader().getResource(normalizedPath);
            if (resource == null) {
                // Essai avec le slash initial
                resource = SceneNavigator.class.getClassLoader().getResource(fxmlPath);
            }
            if (resource == null) {
                throw new IOException("FXML non trouvé: " + fxmlPath);
            }
            Parent root = FXMLLoader.load(resource);
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
