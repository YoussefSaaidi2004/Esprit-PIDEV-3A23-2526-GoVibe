package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.entities.personne;
import org.example.mains.MainApp;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.sql.SQLException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.example.services.DashboardService;
import org.example.services.ServiceHotel;
import org.example.services.ServiceLocation;
import org.example.services.ServicePersonne;

public class AdminDashboardController {

    @FXML
    private AdminSidebarController adminSidebarController;

    @FXML
    private StackPane rootStack;

    @FXML
    private ImageView bgImageView;

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

    @FXML
    public void initialize() {
        setupHeroBackground();
    }

    private void setupHeroBackground() {
        try {
            if (bgImageView != null && rootStack != null) {
                var url = getClass().getResource("/messages/home-hero5.png");
                if (url != null) {
                    bgImageView.setImage(new Image(url.toExternalForm(), true));
                    bgImageView.setPreserveRatio(false);
                    bgImageView.setEffect(new GaussianBlur(30));
                    bgImageView.fitWidthProperty().bind(rootStack.widthProperty());
                    bgImageView.fitHeightProperty().bind(rootStack.heightProperty());
                }
            }
        } catch (Exception e) {
            System.err.println("[AdminDash] Hero error: " + e);
        }
    }

    public void initData(personne user) {
        this.currentUser = user;
        welcomeLabel.setText(user.getPrenom() + " " + user.getNom());
        roleLabel.setText("Administrateur");
        if (profileInitials != null && user.getPrenom() != null && !user.getPrenom().isEmpty()) {
            profileInitials.setText((user.getPrenom().substring(0, 1) + (user.getNom() != null ? user.getNom().substring(0, 1) : "")).toUpperCase());
        }
        // Set active page in sidebar
        if (adminSidebarController != null) {
            adminSidebarController.setActivePage("dashboard");
        }
        refreshDashboardData();
    }

    @FXML
    private void handleDashboard() {
        refreshDashboardData();
    }

    private void refreshDashboardData() {
        if (usersCountLabel != null) usersCountLabel.setText("...");
        if (locationsCountLabel != null) locationsCountLabel.setText("...");
        if (hotelsCountLabel != null) hotelsCountLabel.setText("...");
        if (revenueLabel != null) revenueLabel.setText("... TND");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ServicePersonne servicePersonne = new ServicePersonne();
                int usersCount = servicePersonne.getAll().size();

                ServiceLocation serviceLocation = new ServiceLocation();
                int locationsCount = serviceLocation.getAll().size();

                ServiceHotel serviceHotel = new ServiceHotel();
                int hotelsCount = 0;
                try {
                    hotelsCount = serviceHotel.show().size();
                } catch (SQLException e) {
                    System.err.println("Error fetching hotels: " + e.getMessage());
                }

                DashboardService dashboardService = new DashboardService();
                double revenue = dashboardService.getTotalRevenue();

                final int finalHotels = hotelsCount;
                Platform.runLater(() -> {
                    if (usersCountLabel != null) usersCountLabel.setText(String.valueOf(usersCount));
                    if (locationsCountLabel != null) locationsCountLabel.setText(String.valueOf(locationsCount));
                    if (hotelsCountLabel != null) hotelsCountLabel.setText(String.valueOf(finalHotels));
                    if (revenueLabel != null) revenueLabel.setText(String.format("%.2f TND", revenue));
                });
                return null;
            }
        };
        new Thread(task).start();
    }

    @FXML
    private void handlePersonnes() {
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
    private void handleVoitures() {
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
    private void handleLocations() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdminLocationListView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleFlights() {
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
    private void handleForums() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/poste-forumviews/ListForum.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCheckouts() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/checkout-management.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleHotels() {
        openHotelModule("hotels");
    }

    @FXML
    private void handleChambres() {
        openHotelModule("chambres");
    }

    @FXML
    private void handleHotelReservations() {
        openHotelModule("reservations");
    }

    @FXML
    private void handleActivites() {
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
            String fxmlPath = "/hotel-view.fxml";
            if ("chambres".equalsIgnoreCase(initialView)) {
                fxmlPath = "/chambre-view.fxml";
            } else if ("reservations".equalsIgnoreCase(initialView)) {
                fxmlPath = "/reservation-view.fxml";
            }
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
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

    @FXML
    private void handleGoProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/UserProfileView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
