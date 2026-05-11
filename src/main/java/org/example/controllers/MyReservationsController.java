package org.example.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.example.entities.Reservation;
import org.example.entities.personne;
import org.example.services.ServiceReservation;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class MyReservationsController implements Initializable {

    @FXML private FlowPane reservationsGrid;
    @FXML private Label lblTotalReservations;
    @FXML private VBox emptyState;
    @FXML private ImageView bgImageView;

    private final ServiceReservation serviceReservation = new ServiceReservation();
    private int userId;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        personne currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            org.example.mains.MainApp.switchScene("/org/example/LoginView.fxml", "Connexion");
            return;
        }
        userId = currentUser.getId();
        loadReservations();
        setupHeroBackground();
    }

    private void setupHeroBackground() {
        if (bgImageView != null) {
            // Reusing a high-quality hotel background
            try {
                bgImageView.setImage(new javafx.scene.image.Image("https://images.unsplash.com/photo-1566073771259-6a8506099945?q=80&w=2070&auto=format&fit=crop"));
            } catch (Exception e) {
                System.err.println("Could not load background: " + e.getMessage());
            }
        }
    }

    @FXML
    public void loadReservations() {
        new Thread(() -> {
            try {
                List<Reservation> reservations = serviceReservation.findByUserId(userId);
                Platform.runLater(() -> {
                    reservationsGrid.getChildren().clear();
                    lblTotalReservations.setText(String.valueOf(reservations.size()));
                    
                    if (reservations.isEmpty()) {
                        emptyState.setVisible(true);
                        emptyState.setManaged(true);
                    } else {
                        emptyState.setVisible(false);
                        emptyState.setManaged(false);
                        for (Reservation r : reservations) {
                            addReservationCard(r);
                        }
                    }
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void addReservationCard(Reservation r) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/reservation-card.fxml"));
            VBox card = loader.load();
            ReservationCardController controller = loader.getController();
            controller.setData(r, this::loadReservations);
            reservationsGrid.getChildren().add(card);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        org.example.mains.MainApp.switchScene("/views/room-booking.fxml", "Hôtels & Chambres");
    }
}
