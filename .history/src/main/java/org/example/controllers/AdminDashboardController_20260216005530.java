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
    private void handleGoPersonnes() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/PersonneView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoVoitures() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VoitureListView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoFlights() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/flight-management.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoForums() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/admin-forum-view.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoHotels() {
        openHotelModule("hotels");
    }

    @FXML
    private void handleGoActivites() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openHotelModule(String initialView) {
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
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            SessionManager.clear();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setTitle("GoVibe Connexion");
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Quick action handlers for AdminDashboardView.fxml
    @FXML
    private void handlePersonnes() {
        handleGoPersonnes();
    }

    @FXML
    private void handleVoitures() {
        handleGoVoitures();
    }

    @FXML
    private void handleFlights() {
        handleGoFlights();
    }

    @FXML
    private void handleHotels() {
        handleGoHotels();
    }

    @FXML
    private void handleActivites() {
        handleGoActivites();
    }

    @FXML
    private void handleForums() {
        handleGoForums();
    }
}
