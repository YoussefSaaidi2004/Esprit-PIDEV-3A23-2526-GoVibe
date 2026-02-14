package com.example.gestionvol;

import com.example.gestionvol.util.ScreenManager;
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
        
        // Initialize ScreenManager with lazy loading and caching
        ScreenManager.initialize(stage, MainApp.class);
        
        stage.setTitle("GoVibe Flight Management System");
        
        // Set minimum window size for responsive design
        stage.setMinWidth(1000);
        stage.setMinHeight(600);
        
        // Maximize window on startup for better user experience
        stage.setMaximized(true);
        
        // Load initial screen through ScreenManager (consistent with all navigation)
        ScreenManager.showScreen("/views/user/user-dashboard.fxml", "GoVibe Flight Management System");
        
        stage.show();
        System.out.println("✅ GoVibe Flight Management System launched successfully!");
        System.out.println("🖥️ Window maximized for optimal viewing");
    }

    public static void main(String[] args) {
        launch();
    }

    /**
     * Switch to a different scene/view (deprecated - use ScreenManager.showScreen instead)
     * @param fxmlPath The FXML file path
     * @param title The window title
     */
    public static void switchScene(String fxmlPath, String title) {
        ScreenManager.showScreen(fxmlPath, title);
    }

    /**
     * Clear cache for a specific screen
     */
    public static void clearScreenCache(String fxmlPath) {
        ScreenManager.clearScreenCache(fxmlPath);
    }

    /**
     * Toggle between light and dark theme
     */
    public static void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        ScreenManager.toggleDarkMode();
        System.out.println("🎨 Theme toggled to: " + (isDarkTheme ? "DARK" : "LIGHT"));
    }
}

