package com.example.gestionvol.util;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * ScreenManager: Centralized screen/scene management with lazy loading
 * 
 * Features:
 * - Lazy loading: Load FXML only when needed
 * - Smart caching: Only caches static screens, always reloads data-dependent screens
 * - Maximized state: Ensures window stays maximized on scene switches
 * - Dark mode support: Applies theme consistently across all screens
 */
public class ScreenManager {
    private static Stage primaryStage;
    private static Scene currentScene;
    private static final Map<String, Parent> screenCache = new HashMap<>();
    private static boolean isDarkMode = false;
    private static Class<?> appClass;

    // Data-dependent screens that should NEVER be cached (always reload fresh)
    private static final Set<String> NO_CACHE_SCREENS = Set.of(
        "/views/admin/flight-management.fxml",
        "/views/admin/checkout-management.fxml",
        "/views/admin/admin-dashboard.fxml",
        "/views/user/user-dashboard.fxml",
        "/views/user/checkout-view.fxml",
        "/views/user/checkout-detail.fxml"
    );

    /**
     * Initialize ScreenManager with primary stage and app class
     */
    public static void initialize(Stage stage, Class<?> applicationClass) {
        primaryStage = stage;
        appClass = applicationClass;
        System.out.println("✅ ScreenManager initialized");
    }

    /**
     * Load and display a screen
     * Data-dependent screens are always loaded fresh; static screens use cache
     */
    public static void showScreen(String fxmlPath, String title) {
        try {
            Parent root;

            if (NO_CACHE_SCREENS.contains(fxmlPath)) {
                // Always load fresh for data-dependent screens
                root = loadFresh(fxmlPath);
            } else {
                // Use cache for static screens
                root = getScreen(fxmlPath);
            }

            if (currentScene == null) {
                // First time - create new scene
                currentScene = new Scene(root);
                applyStylesheet(currentScene);
                primaryStage.setScene(currentScene);
            } else {
                // Swap root to keep the same scene (preserves stylesheets)
                currentScene.setRoot(root);
            }

            // Re-apply dark mode to the new root
            applyDarkMode(currentScene);

            primaryStage.setTitle(title);

            // Ensure maximized state persists
            Platform.runLater(() -> {
                primaryStage.setMaximized(true);
            });

            System.out.println("✅ Screen shown: " + title + " (Fresh: " + NO_CACHE_SCREENS.contains(fxmlPath) + ")");

        } catch (IOException e) {
            System.err.println("❌ Error loading screen: " + fxmlPath);
            e.printStackTrace();
        }
    }

    /**
     * Load a fresh FXML (no cache) — guarantees initialize() runs
     */
    private static Parent loadFresh(String fxmlPath) throws IOException {
        System.out.println("📂 Loading fresh FXML: " + fxmlPath);
        FXMLLoader loader = new FXMLLoader(appClass.getResource(fxmlPath));
        return loader.load();
    }

    /**
     * Get a screen — loads from cache if available, otherwise loads from FXML
     */
    public static Parent getScreen(String fxmlPath) throws IOException {
        if (screenCache.containsKey(fxmlPath)) {
            System.out.println("📦 Loading cached FXML: " + fxmlPath);
            return screenCache.get(fxmlPath);
        }

        Parent root = loadFresh(fxmlPath);
        screenCache.put(fxmlPath, root);
        return root;
    }

    /**
     * Clear cache for a specific screen (use if you need to force reload)
     */
    public static void clearScreenCache(String fxmlPath) {
        screenCache.remove(fxmlPath);
        System.out.println("🗑️ Cleared cache for: " + fxmlPath);
    }

    /**
     * Clear all cached screens
     */
    public static void clearAllCache() {
        screenCache.clear();
        System.out.println("🗑️ Cleared all cached screens");
    }

    /**
     * Apply stylesheet to scene
     */
    private static void applyStylesheet(Scene scene) {
        String stylesheet = appClass.getResource("/styles/unified-styles.css").toExternalForm();
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
    }

    /**
     * Apply dark mode class to scene root
     */
    private static void applyDarkMode(Scene scene) {
        if (isDarkMode) {
            if (!scene.getRoot().getStyleClass().contains("dark-mode")) {
                scene.getRoot().getStyleClass().add("dark-mode");
            }
        } else {
            scene.getRoot().getStyleClass().remove("dark-mode");
        }
    }

    /**
     * Toggle dark mode
     */
    public static void toggleDarkMode() {
        isDarkMode = !isDarkMode;
        if (currentScene != null) {
            applyDarkMode(currentScene);
        }
        System.out.println("🌙 Dark mode: " + (isDarkMode ? "ON" : "OFF"));
    }

    /**
     * Get current dark mode state
     */
    public static boolean isDarkMode() {
        return isDarkMode;
    }

    /**
     * Get primary stage
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Get current scene
     */
    public static Scene getCurrentScene() {
        return currentScene;
    }
}
