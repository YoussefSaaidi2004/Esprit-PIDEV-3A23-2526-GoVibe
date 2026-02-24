package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.example.entities.personne;
import org.example.mains.MainApp;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;

import java.io.IOException;

public class AdminDashboardController {

    @FXML
    private Label welcomeLabel;
    @FXML
    private Label roleLabel;
    @FXML
    private Label profileInitials;
    @FXML
    private Label usersCountLabel;
    @FXML
    private Label locationsCountLabel;
    @FXML
    private Label hotelsCountLabel;
    @FXML
    private Label revenueLabel;

    private personne currentUser;

    public void initData(personne user) {
        this.currentUser = user;
        welcomeLabel.setText(user.getPrenom() + " " + user.getNom());
        roleLabel.setText("Administrateur");
        if (profileInitials != null && user.getPrenom() != null && !user.getPrenom().isEmpty()) {
            profileInitials.setText((user.getPrenom().substring(0, 1) + (user.getNom() != null ? user.getNom().substring(0, 1) : "")).toUpperCase());
        }
    }

    @FXML
    private void handleGoDashboard() {
        refreshDashboardData();
    }

    private void refreshDashboardData() {
        if (usersCountLabel != null) usersCountLabel.setText("--");
        if (locationsCountLabel != null) locationsCountLabel.setText("--");
        if (hotelsCountLabel != null) hotelsCountLabel.setText("--");
        if (revenueLabel != null) revenueLabel.setText("-- TND");
    }

    @FXML
    private void handleGoPersonnes(javafx.event.ActionEvent event) {
        navigateTo("/org/example/PersonneView.fxml", event);
    }

    @FXML
    private void handleGoVoitures(javafx.event.ActionEvent event) {
        navigateTo("/VoitureListView.fxml", event);
    }

    @FXML
    private void handleGoFlights(javafx.event.ActionEvent event) {
        navigateTo("/views/flight-management.fxml", event);
    }

    @FXML
    private void handleGoForums(javafx.event.ActionEvent event) {
        navigateTo("/views/admin-forum-view.fxml", event);
    }

    @FXML
    private void handleGoHotels(javafx.event.ActionEvent event) {
        openHotelModule("hotels", event);
    }

    @FXML
    private void handleGoActivites(javafx.event.ActionEvent event) {
        navigateTo("/Dashboard.fxml", event);
    }

    private void openHotelModule(String initialView, javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main-layout.fxml"));
            Parent root = loader.load();
            MainLayoutController controller = loader.getController();
            if (controller != null) {
                if ("chambres".equalsIgnoreCase(initialView)) {
                    controller.loadChambres();
                } else if ("reservations".equalsIgnoreCase(initialView)) {
                    controller.loadReservations();
                } else {
                    controller.loadHotels();
                }
            }
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateTo(String fxmlPath, javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout(javafx.event.ActionEvent event) {
        try {
            SessionManager.clear();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setTitle("GoVibe Connexion");
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Quick action handlers for AdminDashboardView.fxml
    @FXML
    private void handlePersonnes(javafx.event.ActionEvent event) {
        handleGoPersonnes(event);
    }

    @FXML
    private void handleVoitures(javafx.event.ActionEvent event) {
        handleGoVoitures(event);
    }

    @FXML
    private void handleFlights(javafx.event.ActionEvent event) {
        handleGoFlights(event);
    }

    @FXML
    private void handleHotels(javafx.event.ActionEvent event) {
        handleGoHotels(event);
    }

    @FXML
    private void handleActivites(javafx.event.ActionEvent event) {
        handleGoActivites(event);
    }

    @FXML
    private void handleForums(javafx.event.ActionEvent event) {
        handleGoForums(event);
    }
}
