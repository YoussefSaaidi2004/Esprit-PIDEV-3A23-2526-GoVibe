package com.example.gestionvol.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Main Controller for the application
 * Manages simple page navigation by loading FXML into the center container
 */
public class MainController {

    @FXML
    private StackPane pageContainer;
    
    @FXML 
    private javafx.scene.control.ToggleButton btnTheme;
    
    @FXML
    public void initialize() {
        System.out.println("✅ Main Controller initialized");
    }

    @FXML
    private void handleThemeToggle() {
        boolean isDark = btnTheme.isSelected();
        if (isDark) {
            pageContainer.getScene().getRoot().getStyleClass().add("dark-mode");
            // Text/Icon handled by CSS
        } else {
            pageContainer.getScene().getRoot().getStyleClass().remove("dark-mode");
        }
    }

    @FXML
    private void showFlightsPage() {
        loadPage("flight-list-view.fxml");
    }

    @FXML
    private void showCheckoutsPage() {
        loadPage("checkout-view.fxml");
    }

    @FXML
    private void showAddCheckoutPage() {
        loadPage("add-checkout-page.fxml");
    }

    @FXML
    private void showDisplayCheckoutsPage() {
        loadPage("display-checkouts-page.fxml");
    }

    private void loadPage(String fxml) {
        try {
            Node page = FXMLLoader.load(getClass().getResource("/" + fxml));
            page.setOpacity(0);
            pageContainer.getChildren().setAll(page);
            FadeTransition ft = new FadeTransition(Duration.millis(300), page);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
