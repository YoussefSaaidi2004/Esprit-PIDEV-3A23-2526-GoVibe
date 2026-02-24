package org.example.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Centralized scene navigator that swaps the root of the existing scene
 * to avoid full-screen flicker. Always re-maximizes and applies CSS.
 */
public final class SceneNavigator {

    private static final String AUTH_CSS = "/org/example/styles.css";
    private static final String UNIFIED_CSS = "/styles/unified-styles.css";

    private SceneNavigator() {
    }

    // ── Navigate from a source Node ──────────────────────────────────

    public static void switchTo(String fxmlPath, Node source) {
        switchTo(fxmlPath, source, null);
    }

    public static void switchTo(String fxmlPath, Node source, Consumer<Object> controllerInitializer) {
        Stage stage;
        if (source != null && source.getScene() != null) {
            stage = (Stage) source.getScene().getWindow();
        } else {
            stage = Stage.getWindows().stream()
                    .filter(w -> w instanceof Stage)
                    .map(w -> (Stage) w)
                    .filter(s -> s.getScene() != null)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Aucune fenêtre active trouvée"));
        }
        switchTo(fxmlPath, stage, controllerInitializer);
    }

    // ── Core navigation — swaps root to stay maximized ───────────────

    public static void switchTo(String fxmlPath, Stage stage, Consumer<Object> controllerInitializer) {
        try {
            // Use class-relative resource loading which handles absolute paths (starting with /) correctly
            var resource = SceneNavigator.class.getResource(fxmlPath);
            if (resource == null) {
                // Try without leading slash if present
                String alternativePath = fxmlPath.startsWith("/") ? fxmlPath.substring(1) : "/" + fxmlPath;
                resource = SceneNavigator.class.getResource(alternativePath);
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

            // Swap root on existing scene to prevent full-screen flicker
            if (stage.getScene() != null) {
                Scene scene = stage.getScene();
                
                // --- Smooth Transition Animation ---
                Parent oldRoot = scene.getRoot();
                javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), oldRoot);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(event -> {
                    scene.setRoot(root);
                    javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), root);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();
                });
                fadeOut.play();

                // Ensure both stylesheets are present
                if (!scene.getStylesheets().contains(SceneNavigator.class.getResource(AUTH_CSS).toExternalForm())) {
                    scene.getStylesheets().add(SceneNavigator.class.getResource(AUTH_CSS).toExternalForm());
                }
                if (SceneNavigator.class.getResource(UNIFIED_CSS) != null) {
                    String unifiedUrl = SceneNavigator.class.getResource(UNIFIED_CSS).toExternalForm();
                    if (!scene.getStylesheets().contains(unifiedUrl)) {
                        scene.getStylesheets().add(unifiedUrl);
                    }
                }
            } else {
                // First time — create a scene
                Scene scene = new Scene(root);
                scene.getStylesheets().add(SceneNavigator.class.getResource(AUTH_CSS).toExternalForm());
                if (SceneNavigator.class.getResource(UNIFIED_CSS) != null) {
                    scene.getStylesheets().add(SceneNavigator.class.getResource(UNIFIED_CSS).toExternalForm());
                }
                stage.setScene(scene);
                
                // Initial fade in
                root.setOpacity(0);
                javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(500), root);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            }

            // Always ensure maximized
            stage.setMaximized(true);

        } catch (IOException e) {
            throw new RuntimeException("Erreur de navigation vers " + fxmlPath, e);
        }
    }
}
