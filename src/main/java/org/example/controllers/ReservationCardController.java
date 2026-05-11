package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import org.example.entities.Reservation;
import org.example.entities.Hotel;
import org.example.entities.Chambre;
import org.example.services.ServiceReservation;
import org.example.services.ServiceHotel;
import org.example.services.ServiceChambre;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ReservationCardController {

    @FXML private Label lblHotelName, lblRoomType, lblDateDebut, lblDateFin, lblStatus, lblPrixTotal;
    @FXML private Button btnCancel;

    private Reservation reservation;
    private final ServiceReservation serviceReservation = new ServiceReservation();
    private final ServiceHotel serviceHotel = new ServiceHotel();
    private final ServiceChambre serviceChambre = new ServiceChambre();
    private Runnable onCancelSuccess;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public void setData(Reservation r, Runnable onCancelSuccess) {
        this.reservation = r;
        this.onCancelSuccess = onCancelSuccess;

        lblDateDebut.setText(r.getDateDebut().format(formatter));
        lblDateFin.setText(r.getDateFin().format(formatter));
        lblStatus.setText(r.getStatut());
        lblPrixTotal.setText(String.format("%.0f DT", r.getPrixTotal()));

        // Status styling
        if ("ANNULEE".equals(r.getStatut())) {
            lblStatus.setStyle("-fx-background-color: rgba(231,76,60,0.2); -fx-text-fill: #c0392b; -fx-padding: 6 12; -fx-background-radius: 20; -fx-font-size: 10; -fx-font-weight: 800;");
            btnCancel.setVisible(false);
            btnCancel.setManaged(false);
        } else if ("CONFIRMEE".equals(r.getStatut())) {
            lblStatus.setStyle("-fx-background-color: rgba(80,200,120,0.2); -fx-text-fill: #2d7a4d; -fx-padding: 6 12; -fx-background-radius: 20; -fx-font-size: 10; -fx-font-weight: 800;");
        } else {
            lblStatus.setStyle("-fx-background-color: rgba(255,152,0,0.2); -fx-text-fill: #e67e22; -fx-padding: 6 12; -fx-background-radius: 20; -fx-font-size: 10; -fx-font-weight: 800;");
        }

        // Load Hotel and Room info asynchronously
        new Thread(() -> {
            try {
                Hotel hotel = serviceHotel.findById(r.getHotelId());
                Chambre chambre = serviceChambre.findById(r.getChambreId());
                javafx.application.Platform.runLater(() -> {
                    if (hotel != null) lblHotelName.setText(hotel.getNom());
                    if (chambre != null) lblRoomType.setText(chambre.getType());
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void handleCancel() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Annuler la réservation");
        alert.setHeaderText("Êtes-vous sûr de vouloir annuler cette réservation ?");
        alert.setContentText("Cette action est irréversible.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                reservation.setStatut("ANNULEE");
                serviceReservation.update(reservation);
                if (onCancelSuccess != null) onCancelSuccess.run();
            } catch (SQLException e) {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Erreur");
                error.setContentText("Impossible d'annuler la réservation: " + e.getMessage());
                error.show();
            }
        }
    }
}
