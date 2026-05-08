package org.example.controllers;

import org.example.utils.StatusMapper;

import org.example.entities.Checkout;
import org.example.dao.FlightDAO;
import org.example.entities.Flight;
import org.example.utils.IconUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.effect.GaussianBlur;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.control.Alert;
import java.util.function.Consumer;

public class CheckoutCardController {
    @FXML private Label routeText, passengerText, priceText, dateText, airlineText;
    @FXML private Label statusText;
    @FXML private HBox actionBox;

    private Checkout checkout;
    private Flight flight;
    private final FlightDAO flightDAO = new FlightDAO();
    private Runnable onPaymentSuccess;
    private boolean isAdminView;
    private Consumer<Checkout> onCancelAction;

    public void setOnPaymentSuccess(Runnable onPaymentSuccess) {
        this.onPaymentSuccess = onPaymentSuccess;
    }

    public void setData(Checkout checkout, boolean isAdmin, 
                        Consumer<Checkout> onApprove, Consumer<Checkout> onReject, 
                        Consumer<Checkout> onCancel) {
        this.checkout = checkout;
        this.isAdminView = isAdmin;
        this.onCancelAction = onCancel;
        this.flight = flightDAO.findById(checkout.getFlightId());
        
        if (flight != null) {
            routeText.setText(flight.getDepartureAirport() + " → " + flight.getDestination());
            airlineText.setText(flight.getAirline());
        }
        
        passengerText.setText(checkout.getPassengerNbr() + " passager(s)");
        dateText.setText(checkout.getReservationDate().toString().split("T")[0]);
        priceText.setText(checkout.getTotalPrix() != null ? checkout.getTotalPrix().toPlainString() : "0");

        String statusValue = checkout.getStatusReservation();
        statusText.setText(StatusMapper.toDisplayLabel(statusValue));
        updateStatusStyle();

        actionBox.getChildren().clear();
        if (isAdmin) {
            if (checkout.isPending()) {
                // Icon-only approve button
                Button btnApprove = IconUtils.createIconButton("✓", "Approve", "#50C878");
                btnApprove.setPrefWidth(40);
                btnApprove.setPrefHeight(40);
                btnApprove.setOnAction(e -> onApprove.accept(checkout));
                
                // Icon-only reject button
                Button btnReject = IconUtils.createIconButton("✗", "Reject", "#EF476F");
                btnReject.setPrefWidth(40);
                btnReject.setPrefHeight(40);
                btnReject.setOnAction(e -> onReject.accept(checkout));
                
                actionBox.getChildren().addAll(btnApprove, btnReject);
            }
        } else {
            // User view - allow viewing details
            Button btnView = IconUtils.createIconButton("👁", "View Details", "#50C878");
            btnView.setOnAction(e -> handleViewDetails());
            actionBox.getChildren().add(btnView);
            
            if (checkout.isPending() || checkout.isConfirmed()) {
                Button btnCancel = IconUtils.createIconButton("🗑", "Cancel", "#EF476F");
                btnCancel.setOnAction(e -> onCancel.accept(checkout));
                actionBox.getChildren().add(btnCancel);
            }
        }
    }

    private void updateStatusStyle() {
        String status = checkout.getStatusReservation();
        String baseStyle = "-fx-font-size: 9; -fx-font-weight: 900; -fx-background-radius: 50;"
            + "-fx-border-radius: 50; -fx-border-width: 1; -fx-padding: 3 10 3 10;";
        if (StatusMapper.isPending(status)) {
            statusText.setStyle(baseStyle
                + "-fx-text-fill: #f0a500; -fx-background-color: rgba(240,165,0,0.15);"
                + "-fx-border-color: rgba(240,165,0,0.35);");
        } else if (StatusMapper.isConfirmed(status)) {
            statusText.setStyle(baseStyle
                + "-fx-text-fill: #4de88a; -fx-background-color: rgba(78,232,138,0.15);"
                + "-fx-border-color: rgba(78,232,138,0.35);");
        } else if (StatusMapper.isRejected(status)) {
            statusText.setStyle(baseStyle
                + "-fx-text-fill: #ef476f; -fx-background-color: rgba(239,71,111,0.15);"
                + "-fx-border-color: rgba(239,71,111,0.35);");
        } else if (StatusMapper.isCancelled(status)) {
            statusText.setStyle(baseStyle
                + "-fx-text-fill: rgba(255,255,255,0.5); -fx-background-color: rgba(255,255,255,0.08);"
                + "-fx-border-color: rgba(255,255,255,0.2);");
        } else {
            statusText.setStyle(baseStyle
                + "-fx-text-fill: rgba(255,255,255,0.6); -fx-background-color: rgba(255,255,255,0.08);"
                + "-fx-border-color: rgba(255,255,255,0.2);");
        }
    }

    @FXML
    public void handleViewDetails() {
        javafx.scene.effect.GaussianBlur blur = new javafx.scene.effect.GaussianBlur(15);
        try {
            if (checkout == null) {
                return;
            }

            // Create the blurred background effect
            actionBox.getScene().getRoot().setEffect(blur);

            if (flight == null && checkout.getFlightId() != null) {
                flight = flightDAO.findById(checkout.getFlightId());
            }

            if (flight == null) {
                throw new IllegalStateException("Flight not found for checkout " + checkout.getCheckoutId());
            }

            // Load the modal
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/views/city-payment-modal.fxml"));
            javafx.scene.Parent root = loader.load();
            
            CityPaymentModalController controller = loader.getController();
            controller.setData(checkout, flight, () -> {
                checkout.setStatusReservation(StatusMapper.toDbValue("CONFIRMED"));
                statusText.setText(StatusMapper.toDisplayLabel(checkout.getStatusReservation()));
                updateStatusStyle();
                refreshUserActions();
                if (onPaymentSuccess != null) {
                    onPaymentSuccess.run();
                }
            });

            javafx.stage.Stage modalStage = new javafx.stage.Stage();
            modalStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            modalStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            modalStage.initOwner(actionBox.getScene().getWindow());
            
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            modalStage.setScene(scene);
            
            modalStage.setOnHidden(e -> {
                if (actionBox.getScene() != null && actionBox.getScene().getRoot() != null) {
                    actionBox.getScene().getRoot().setEffect(null);
                }
            });
            
            modalStage.showAndWait();
        } catch (Exception e) {
            if (actionBox.getScene() != null && actionBox.getScene().getRoot() != null) {
                actionBox.getScene().getRoot().setEffect(null);
            }
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Unable to open details");
            alert.setHeaderText("Payment modal could not be opened");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void refreshUserActions() {
        if (isAdminView) {
            return;
        }

        actionBox.getChildren().clear();

        Button btnView = IconUtils.createIconButton("👁", "View Details", "#50C878");
        btnView.setOnAction(e -> handleViewDetails());
        actionBox.getChildren().add(btnView);

        if ((checkout.isPending() || checkout.isConfirmed()) && onCancelAction != null) {
            Button btnCancel = IconUtils.createIconButton("🗑", "Cancel", "#EF476F");
            btnCancel.setOnAction(e -> onCancelAction.accept(checkout));
            actionBox.getChildren().add(btnCancel);
        }
    }
}
