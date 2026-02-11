package com.example.gestionvol.utils;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * Utility class for displaying toast notifications
 */
public class NotificationUtils {
    
    /**
     * Shows a success toast notification
     */
    public static void showSuccess(StackPane container, String message) {
        showToast(container, message, "✓", "#50C878");
    }
    
    /**
     * Shows an error toast notification
     */
    public static void showError(StackPane container, String message) {
        showToast(container, message, "✗", "#EF476F");
    }
    
    /**
     * Shows an info toast notification
     */
    public static void showInfo(StackPane container, String message) {
        showToast(container, message, "ℹ", "#084E36");
    }
    
    /**
     * Generic toast notification method
     */
    private static void showToast(StackPane container, String message, String icon, String color) {
        VBox toast = new VBox(10);
        toast.setAlignment(Pos.CENTER);
        toast.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 15 25; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 4);",
            color
        ));
        
        Text iconText = new Text(icon);
        iconText.setStyle("-fx-font-size: 24; -fx-fill: white; -fx-font-weight: bold;");
        
        Text messageText = new Text(message);
        messageText.setStyle("-fx-font-size: 14; -fx-fill: white; -fx-font-weight: bold;");
        
        toast.getChildren().addAll(iconText, messageText);
        
        StackPane.setAlignment(toast, Pos.TOP_CENTER);
        StackPane.setMargin(toast, new javafx.geometry.Insets(80, 0, 0, 0));
        
        container.getChildren().add(toast);
        
        // Fade in
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        
        // Pause
        PauseTransition pause = new PauseTransition(Duration.millis(3000));
        
        // Fade out
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> container.getChildren().remove(toast));
        
        // Chain animations
        fadeIn.setOnFinished(e -> pause.play());
        pause.setOnFinished(e -> fadeOut.play());
        
        fadeIn.play();
    }
}
