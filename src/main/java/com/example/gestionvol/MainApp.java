package com.example.gestionvol;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main JavaFX Application
 * Launches the GoVibe Flight Management System
 */
public class MainApp extends Application {

    /**
     * Static reference to primary stage for scene switching
     */
    private static Stage primaryStage;
    private static boolean isDarkTheme = false;

    public static boolean isDarkTheme() {
        return isDarkTheme;
    }
    
    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;  // Store stage reference
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("/views/user-dashboard.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        
        // Apply unified CSS stylesheet for consistent modern styling
        scene.getStylesheets().add(getClass().getResource("/styles/unified-styles.css").toExternalForm());
        
        stage.setTitle("GoVibe Flight Management System");
        stage.setScene(scene);
        
        // Set minimum window size for responsive design
        stage.setMinWidth(1000);
        stage.setMinHeight(600);
        
        // Maximize window on startup for better user experience
        stage.setMaximized(true);
        
        // Set application icon (if available)
        try {
            // stage.getIcons().add(new Image(getClass().getResourceAsStream("/icon.png")));
        } catch (Exception e) {
            System.out.println("Icon not found, using default");
        }
        
        stage.show();
        System.out.println("✅ GoVibe Flight Management System launched successfully!");
        System.out.println("🖥️ Window maximized for optimal viewing");
    }

    public static void main(String[] args) {
        launch();
    }

    /**
     * Switch to a different scene/view
     * @param fxmlPath The FXML file path
     * @param title The window title
     */
    public static void switchScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(fxmlPath));
            Scene scene = new Scene(loader.load());
            
            // Apply unified CSS stylesheet
            scene.getStylesheets().add(MainApp.class.getResource("/styles/unified-styles.css").toExternalForm());
            
            // Apply dark mode class if enabled
            if (isDarkTheme) {
                scene.getRoot().getStyleClass().add("dark-mode");
            }
            
            if (primaryStage != null) {
                primaryStage.setScene(scene);
                primaryStage.setTitle(title);
                // Enforce full screen with runLater to ensure it applies after layout pass
                javafx.application.Platform.runLater(() -> {
                    primaryStage.setMaximized(true);
                });
            }
        } catch (IOException e) {
            System.err.println("Error switching scene: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Toggle between light and dark theme
     */
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

