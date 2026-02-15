package org.example.controllers;

import org.example.entities.Checkout;
import org.example.entities.Flight;
import org.example.services.CheckoutService;
import org.example.services.FlightService;
import org.example.utils.NotificationUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

public class CheckoutDetailController {
    @FXML private StackPane rootPane, loadingOverlay;
    @FXML private Label statusBadge;
    @FXML private Text flightRoute, flightDetails, totalPrice;
    @FXML private Text travelClassText, seatPrefText, paymentText;
    @FXML private TextField nameField, emailField, phoneField;
    @FXML private Spinner<Integer> passengerSpinner;
    @FXML private Button editBtn;
    @FXML private Text editBtnText;
    @FXML private HBox editModeButtons;

    private static Checkout selectedCheckout;
    private final CheckoutService checkoutService = new CheckoutService();
    private final FlightService flightService = new FlightService();
    private boolean editMode = false;
    private Flight flight;

    public static void setSelectedCheckout(Checkout checkout) {
        selectedCheckout = checkout;
    }

    @FXML
    public void initialize() {
        if (selectedCheckout == null) {
            NotificationUtils.showError(rootPane, "No booking selected");
            return;
        }

        loadCheckoutData();
    }

    private void loadCheckoutData() {
        flight = flightService.getFlightById(selectedCheckout.getFlightId());
        if (flight != null) {
            flightRoute.setText(flight.getDepartureAirport() + " → " + flight.getDestination());
            flightDetails.setText(flight.getAirline() + " • Flight " + flight.getFlightId() + 
                                 " • " + flight.getDepartureTime() + " - " + flight.getArrivalTime());
        }

        String status = selectedCheckout.getStatusReservation();
        statusBadge.setText(status != null ? status.toUpperCase() : "");
        updateStatusBadgeStyle();

        nameField.setText(selectedCheckout.getPassengerName());
        emailField.setText(selectedCheckout.getPassengerEmail());
        phoneField.setText(selectedCheckout.getPassengerPhone());
        
        passengerSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, selectedCheckout.getPassengerNbr())
        );

        travelClassText.setText(selectedCheckout.getTravelClass() != null ? selectedCheckout.getTravelClass() : "Economy");
        seatPrefText.setText(selectedCheckout.getSeatPreference() != null ? selectedCheckout.getSeatPreference() : "Window");
        paymentText.setText(selectedCheckout.getPaymentMethod() != null ? selectedCheckout.getPaymentMethod() : "Credit Card");

        totalPrice.setText(selectedCheckout.getTotalPrix() + " DT");
    }

    private void updateStatusBadgeStyle() {
        statusBadge.getStyleClass().removeAll("badge-pending", "badge-confirmed", "badge-rejected", "badge-cancelled");
        
        String status = selectedCheckout.getStatusReservation();
        if (status == null) {
            return;
        }

        switch (status.toUpperCase()) {
            case "PENDING": statusBadge.getStyleClass().add("badge-pending"); break;
            case "CONFIRMED": statusBadge.getStyleClass().add("badge-confirmed"); break;
            case "REJECTED": statusBadge.getStyleClass().add("badge-rejected"); break;
            case "CANCELLED": statusBadge.getStyleClass().add("badge-cancelled"); break;
        }
    }

    @FXML
    private void toggleEditMode() {
        editMode = !editMode;
        nameField.setDisable(!editMode);
        emailField.setDisable(!editMode);
        phoneField.setDisable(!editMode);
        passengerSpinner.setDisable(!editMode);
        
        editBtnText.setText(editMode ? "Cancel" : "Edit");
        editModeButtons.setVisible(editMode);
        editModeButtons.setManaged(editMode);
    }

    @FXML
    private void handleUpdate() {
        loadingOverlay.setVisible(true);
        
        new Thread(() -> {
            try {
                Thread.sleep(800);
                
                selectedCheckout.setPassengerName(nameField.getText());
                selectedCheckout.setPassengerEmail(emailField.getText());
                selectedCheckout.setPassengerPhone(phoneField.getText());
                selectedCheckout.setPassengerNbr(passengerSpinner.getValue());
                
                boolean success = checkoutService.updateCheckout(selectedCheckout);
                
                Platform.runLater(() -> {
                    loadingOverlay.setVisible(false);
                    
                    if (success) {
                        NotificationUtils.showSuccess(rootPane, "Booking updated successfully!");
                        toggleEditMode();
                    } else {
                        NotificationUtils.showError(rootPane, "Failed to update booking");
                    }
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingOverlay.setVisible(false);
                    NotificationUtils.showError(rootPane, "Error: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleDelete() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cancel Booking");
        alert.setHeaderText("Are you sure you want to cancel this booking?");
        alert.setContentText("This action cannot be undone. Your seats will be released back to the flight.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deleteBooking();
            }
        });
    }

    private void deleteBooking() {
        loadingOverlay.setVisible(true);
        
        new Thread(() -> {
            try {
                Thread.sleep(800);
                
                boolean success = checkoutService.cancelCheckout(selectedCheckout.getCheckoutId(), 1);
                
                Platform.runLater(() -> {
                    loadingOverlay.setVisible(false);
                    
                    if (success) {
                        NotificationUtils.showSuccess(rootPane, "Booking cancelled successfully!");
                        new Thread(() -> {
                            try {
                                Thread.sleep(1500);
                                Platform.runLater(() -> org.example.mains.MainApp.switchScene("/views/user-dashboard.fxml", "User Dashboard"));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    } else {
                        NotificationUtils.showError(rootPane, "Failed to cancel booking");
                    }
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingOverlay.setVisible(false);
                    NotificationUtils.showError(rootPane, "Error: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleBack() {
        org.example.mains.MainApp.switchScene("/views/user-dashboard.fxml", "User Dashboard");
    }
}
