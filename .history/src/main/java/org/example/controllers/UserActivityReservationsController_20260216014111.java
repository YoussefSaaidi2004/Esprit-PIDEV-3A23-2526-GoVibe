package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.entities.ReservationSession;
import org.example.entities.Session;
import org.example.entities.Activite;
import org.example.services.ServiceReservationSession;
import org.example.services.ServiceSession;
import org.example.services.ServiceActivite;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class UserActivityReservationsController implements Initializable {

    @FXML
    private Label lblTotalReservations;

    @FXML
    private VBox emptyStateBox;

    @FXML
    private FlowPane reservationsContainer;

    private ServiceReservationSession serviceReservation;
    private ServiceSession serviceSession;
    private ServiceActivite serviceActivite;
    private ObservableList<ReservationSession> reservationList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        serviceReservation = new ServiceReservationSession();
        serviceSession = new ServiceSession();
        serviceActivite = new ServiceActivite();
        reservationList = FXCollections.observableArrayList();

        loadReservations();
    }

    private void loadReservations() {
        try {
            reservationList.clear();

            // Get current user reference from session
            String userRef = SessionManager.getCurrentUserRef();
            if (userRef == null || userRef.isEmpty()) {
                userRef = "USER001"; // Default for testing
            }

            // Load user's reservations
            List<ReservationSession> userReservations = serviceReservation.getByUserRef(userRef);
            reservationList.addAll(userReservations);

            lblTotalReservations.setText(userReservations.size() + " réservation(s)");

            if (userReservations.isEmpty()) {
                emptyStateBox.setVisible(true);
                emptyStateBox.setManaged(true);
                reservationsContainer.setVisible(false);
                reservationsContainer.setManaged(false);
            } else {
                emptyStateBox.setVisible(false);
                emptyStateBox.setManaged(false);
                reservationsContainer.setVisible(true);
                reservationsContainer.setManaged(true);
                displayReservationCards();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger vos réservations: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void displayReservationCards() {
        reservationsContainer.getChildren().clear();

        for (ReservationSession reservation : reservationList) {
            try {
                // Get session details
                Session session = serviceSession.getById(reservation.getSession_id());
                if (session != null) {
                    // Get activity details
                    Activite activite = serviceActivite.getById(session.getActivite_id());
                    VBox card = createReservationCard(reservation, session, activite);
                    reservationsContainer.getChildren().add(card);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private VBox createReservationCard(ReservationSession reservation, Session session, Activite activite) {
        VBox card = new VBox(12);
        card.setPrefWidth(320);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                "-fx-effect: dropshadow(gaussian, rgba(1,50,32,0.15), 15, 0, 0, 5); " +
                "-fx-padding: 20; -fx-cursor: hand;");

        // Activity name
        Label activityLabel = new Label("🎭 " + (activite != null ? activite.getName() : "Activité"));
        activityLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #013220;");

        // Session date and time
        HBox dateTimeBox = new HBox(10);
        dateTimeBox.setAlignment(Pos.CENTER_LEFT);
        dateTimeBox.setStyle("-fx-background-color: #D1F2EB; -fx-background-radius: 10; -fx-padding: 12;");

        Label dateLabel = new Label("📅 " + session.getDate().toString());
        dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #0B6E4F; -fx-font-weight: bold;");

        Label timeLabel = new Label("🕐 " + session.getHeure().toString());
        timeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #0B6E4F;");

        dateTimeBox.getChildren().addAll(dateLabel, timeLabel);

        // Places info
        Label placesLabel = new Label("👥 Places réservées: " + reservation.getNb_places());
        placesLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");

        // Capacity info
        Label capacityLabel = new Label("📊 Capacité totale: " + session.getCapacite() + " places");
        capacityLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        // Price if available
        HBox priceBox = new HBox(8);
        priceBox.setAlignment(Pos.CENTER_LEFT);
        priceBox.setStyle("-fx-background-color: #F5F3E7; -fx-background-radius: 8; -fx-padding: 10;");

        if (activite != null && activite.getPrix() != null) {
            double totalPrice = activite.getPrix().doubleValue() * reservation.getNb_places();
            Label priceLabel = new Label("💰 Prix total:");
            priceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #013220; -fx-font-weight: bold;");
            Label priceValue = new Label(String.format("%.2f DT", totalPrice));
            priceValue.setStyle("-fx-font-size: 16px; -fx-text-fill: #0B6E4F; -fx-font-weight: bold;");
            priceBox.getChildren().addAll(priceLabel, priceValue);
        }

        // Action buttons
        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER);

        Button reclamationBtn = new Button("⚠️ Réclamation");
        reclamationBtn.setStyle("-fx-background-color: #FFA500; -fx-text-fill: white; -fx-background-radius: 8; " +
                "-fx-padding: 10 16; -fx-font-size: 12px; -fx-cursor: hand; -fx-font-weight: bold;");
        reclamationBtn.setOnAction(e -> openReclamationForm(reservation, session, activite));

        Button cancelBtn = new Button("❌ Annuler");
        cancelBtn.setStyle("-fx-background-color: #D84E36; -fx-text-fill: white; -fx-background-radius: 8; " +
                "-fx-padding: 10 16; -fx-font-size: 12px; -fx-cursor: hand; -fx-font-weight: bold;");
        cancelBtn.setOnAction(e -> cancelReservation(reservation));

        actionButtons.getChildren().addAll(reclamationBtn, cancelBtn);

        card.getChildren().addAll(activityLabel, dateTimeBox, placesLabel, capacityLabel, priceBox, actionButtons);

        return card;
    }

    private void openReclamationForm(ReservationSession reservation, Session session, Activite activite) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/reclamation-form.fxml"));
            Parent root = loader.load();

            ReclamationFormController controller = loader.getController();
            controller.setReservationData(reservation, session, activite);

            Stage stage = new Stage();
            stage.setTitle("Faire une Réclamation");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le formulaire de réclamation", Alert.AlertType.ERROR);
        }
    }

    private void cancelReservation(ReservationSession reservation) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Annuler la réservation");
        confirmation.setContentText("Êtes-vous sûr de vouloir annuler cette réservation ?");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Add cancel logic here if the service supports it
                    showAlert("Info", "Fonctionnalité d'annulation à implémenter", Alert.AlertType.INFORMATION);
                    loadReservations(); // Refresh
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Erreur", "Erreur lors de l'annulation: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    @FXML
    private void goToHome() {
        navigateTo("/UserHome.fxml");
    }

    @FXML
    private void goToActivities() {
        navigateTo("/UserActivite.fxml");
    }

    private void navigateTo(String fxmlPath) {
        try {
            Stage stage = (Stage) lblTotalReservations.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de naviguer: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
