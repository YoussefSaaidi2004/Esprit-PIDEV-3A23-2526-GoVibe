package com.example.gestionvol.controller.user;

import com.example.gestionvol.MainApp;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;

public class UserDashboardController {
    
    @FXML private StackPane contentArea;
    @FXML private VBox navMenu;
    @FXML private Button profileBtn;
    
    // Timer to delay hiding menu
    private javafx.animation.PauseTransition hideTimer;

    @FXML
    public void initialize() {
        // Load default view (Flights)
        showFlights();
        
        // Setup Hover Logic
        setupNavigation();
    }
    
    private void setupNavigation() {
        hideTimer = new javafx.animation.PauseTransition(Duration.millis(200));
        hideTimer.setOnFinished(e -> hideMenu());

        // Profile Button Hover
        profileBtn.setOnMouseEntered(e -> {
            hideTimer.stop();
            showMenu();
        });
        profileBtn.setOnMouseExited(e -> {
            hideTimer.playFromStart();
        });

        // Menu Hover
        navMenu.setOnMouseEntered(e -> hideTimer.stop());
        navMenu.setOnMouseExited(e -> hideTimer.playFromStart());
    }

    private void showMenu() {
        if (navMenu.isVisible()) return;
        
        navMenu.setVisible(true);
        navMenu.setOpacity(0);
        navMenu.setTranslateY(60); // Start slightly higher/lower

        FadeTransition fade = new FadeTransition(Duration.millis(200), navMenu);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition translate = new TranslateTransition(Duration.millis(200), navMenu);
        translate.setFromY(60);
        translate.setToY(70);

        ParallelTransition pt = new ParallelTransition(fade, translate);
        pt.play();
    }

    private void hideMenu() {
        FadeTransition fade = new FadeTransition(Duration.millis(150), navMenu);
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setOnFinished(e -> navMenu.setVisible(false));
        fade.play();
    }

    @FXML
    public void showFlights() {
        loadView("/views/user/user-flights-view.fxml");
        hideMenu();
    }

    @FXML
    public void showBookings() {
        loadView("/views/user/user-bookings-view.fxml");
        hideMenu();
    }

    private void loadView(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML private void handleAdminSwap() { MainApp.switchScene("/views/admin/admin-dashboard.fxml", "Admin Dashboard"); }
    @FXML private void handleThemeToggle() { MainApp.toggleTheme(); }
    @FXML private void handleLogout() { 
        System.out.println("Logout clicked");
        // Implement logout logic here
    }
}
