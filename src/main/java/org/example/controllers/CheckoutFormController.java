package org.example.controllers;

import org.example.entities.Checkout;
import org.example.entities.Flight;
import org.example.services.CheckoutService;
import org.example.services.FlightService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CheckoutFormController {

    @FXML private Text formTitle;
    @FXML private TextField txtUserId;
    @FXML private TextField txtPassengers;
    @FXML private ComboBox<String> cmbStatus;
    @FXML private TextField txtTotalPrice;
    @FXML private TextField txtReservationDate;
    @FXML private ComboBox<String> cmbFlightId;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private final CheckoutService checkoutService = new CheckoutService();
    private final FlightService flightService = new FlightService();
    
    private Checkout checkout;
    private boolean isEditMode = false;

    @FXML
    public void initialize() {
        loadFlightIds();
        updateReservationDateField();
    }

    private void loadFlightIds() {
        List<Flight> flights = flightService.getAllFlights();
        ObservableList<String> flightIds = FXCollections.observableArrayList();
        for (Flight f : flights) {
            flightIds.add(f.getFlightId());
        }
        cmbFlightId.setItems(flightIds);
    }

    private void updateReservationDateField() {
        if (!isEditMode) {
            txtReservationDate.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }
    }

    public void setCheckoutForEdit(Checkout c) {
        this.checkout = c;
        this.isEditMode = true;
        formTitle.setText("Edit Checkout");
        
        txtUserId.setText(String.valueOf(c.getIdUser()));
        cmbFlightId.setValue(c.getFlightId());
        txtPassengers.setText(String.valueOf(c.getPassengerNbr()));
        cmbStatus.setValue(c.getStatusReservation());
        txtTotalPrice.setText(String.valueOf(c.getTotalPrix()));
        txtReservationDate.setText(c.getReservationDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }

    @FXML
    private void handleSave() {
        try {
            if (checkout == null) checkout = new Checkout();

            if (cmbFlightId.getValue() == null) {
                showAlert("Validation Error", "Please select a Flight ID.");
                return;
            }
            checkout.setFlightId(cmbFlightId.getValue());
            
            try {
                checkout.setIdUser(Integer.parseInt(txtUserId.getText().trim()));
            } catch (NumberFormatException e) {
                showAlert("Invalid Input", "User ID must be a valid number.");
                return;
            }
            
            try {
                checkout.setPassengerNbr(Integer.parseInt(txtPassengers.getText().trim()));
            } catch (NumberFormatException e) {
                showAlert("Invalid Input", "Passengers must be a valid number.");
                return;
            }

            checkout.setStatusReservation(cmbStatus.getValue());
            
            try {
                checkout.setTotalPrix(new java.math.BigDecimal(txtTotalPrice.getText().trim()));
            } catch (NumberFormatException e) {
                showAlert("Invalid Input", "Total Price must be a valid number.");
                return;
            }
            
            if (!isEditMode) {
                checkout.setReservationDate(LocalDateTime.now());
            }

            boolean success = isEditMode ? checkoutService.updateCheckout(checkout) : checkoutService.addCheckout(checkout);

            if (success) {
                showAlert("Success", isEditMode ? "Checkout updated successfully!" : "Checkout added successfully!");
                navigateBack();
            } else {
                showAlert("Error", "Failed to save checkout.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "An unexpected error occurred: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        navigateBack();
    }

    private void navigateBack() {
        try {
            javafx.stage.Stage stage = (javafx.stage.Stage) btnSave.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
