package org.example.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/LoginView.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles/unified-styles.css").toExternalForm());

        primaryStage.setTitle("GoVibe - Connexion");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static Stage primaryStage;
    private static boolean isDarkTheme = false;

    public static boolean isDarkTheme() {
        return isDarkTheme;
    }

    public static void switchScene(String fxmlPath, String title) {
        try {
            System.out.println("[Nav] Switching scene to " + fxmlPath);
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(fxmlPath));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(MainApp.class.getResource("/styles/unified-styles.css").toExternalForm());
            
            if (isDarkTheme) {
                scene.getRoot().getStyleClass().add("dark-mode");
            }
            
            if (primaryStage != null) {
                primaryStage.setScene(scene);
                primaryStage.setTitle(title);
                javafx.application.Platform.runLater(() -> primaryStage.setMaximized(true));
                System.out.println("[Nav] Scene loaded: " + title);
            }
        } catch (Exception e) {
            System.err.println("Error switching scene: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        if (primaryStage != null && primaryStage.getScene() != null) {
            if (isDarkTheme) {
                if (!primaryStage.getScene().getRoot().getStyleClass().contains("dark-mode")) {
                    primaryStage.getScene().getRoot().getStyleClass().add("dark-mode");
                }
            } else {
                primaryStage.getScene().getRoot().getStyleClass().remove("dark-mode");
            }
        }
    }
}
