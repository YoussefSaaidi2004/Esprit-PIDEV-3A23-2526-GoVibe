package com.example.gestionvol.controller;

import com.example.gestionvol.entities.Flight;
import com.example.gestionvol.service.FlightService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class FlightFormController {

    @FXML private Text formTitle;
    @FXML private TextField txtFlightId;
    @FXML private TextField txtDepartureAirport;
    @FXML private TextField txtDestination;
    @FXML private TextField txtDepartureTime;
    @FXML private TextField txtArrivalTime;
    @FXML private ComboBox<String> cmbClass;
    @FXML private TextField txtAirline;
    @FXML private TextField txtPrice;
    @FXML private TextField txtSeats;
    @FXML private TextArea txtDescription;
    
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private final FlightService flightService = new FlightService();
    private Flight currentFlight = null;
    private boolean isEditMode = false;

    @FXML
    public void initialize() {
        // Initialize logic if needed
    }

    public void setFlightForEdit(Flight flight) {
        this.currentFlight = flight;
        this.isEditMode = true;
        this.formTitle.setText("Edit Flight");
        this.txtFlightId.setEditable(false);
        populateForm(flight);
    }

    private void populateForm(Flight flight) {
        txtFlightId.setText(flight.getFlightId());
        txtDepartureAirport.setText(flight.getDepartureAirport());
        txtDestination.setText(flight.getDestination());
        txtDepartureTime.setText(flight.getDepartureTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        txtArrivalTime.setText(flight.getArrivalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        cmbClass.setValue(flight.getClasseChaise());
        txtAirline.setText(flight.getAirline());
        txtPrice.setText(String.valueOf(flight.getPrix()));
        txtSeats.setText(String.valueOf(flight.getAvailableSeats()));
        txtDescription.setText(flight.getDescription());
    }

    @FXML
    private void handleSave() {
        System.out.println("DEBUG: handleSave called");
        try {
            Flight flight = isEditMode ? currentFlight : new Flight();

            String id = txtFlightId.getText(); 
            if (id == null || id.trim().isEmpty()) {
                 showAlert("Validation Error", "Flight ID is required.", Alert.AlertType.WARNING);
                 return;
            }
            flight.setFlightId(id.trim());
            
            flight.setDepartureAirport(txtDepartureAirport.getText().trim());
            flight.setDestination(txtDestination.getText().trim());
            flight.setDepartureTime(parseTime(txtDepartureTime.getText()));
            flight.setArrivalTime(parseTime(txtArrivalTime.getText()));
            flight.setClasseChaise(cmbClass.getValue());
            flight.setAirline(txtAirline.getText().trim());
            
            try {
                flight.setPrix(Integer.parseInt(txtPrice.getText().trim()));
            } catch (NumberFormatException e) {
                showAlert("Invalid Input", "Price must be a valid integer.", Alert.AlertType.WARNING);
                return;
            }

            try {
                flight.setAvailableSeats(Integer.parseInt(txtSeats.getText().trim()));
            } catch (NumberFormatException e) {
                showAlert("Invalid Input", "Seats must be a valid integer.", Alert.AlertType.WARNING);
                return;
            }
            
            flight.setDescription(txtDescription.getText().trim());

            System.out.println("DEBUG: Attempting to save flight: " + flight.getFlightId());
            boolean success = isEditMode ? flightService.updateFlight(flight) : flightService.addFlight(flight);

            if (success) {
                showAlert("Success", isEditMode ? "Flight updated successfully!" : "Flight added successfully!", 
                         Alert.AlertType.INFORMATION);
                navigateBack();
            } else {
                showAlert("Error", "Failed to save flight. Check database connection or constraints.", Alert.AlertType.ERROR);
            }

        } catch (IllegalArgumentException e) {
            showAlert("Validation Error", e.getMessage(), Alert.AlertType.WARNING);
        } catch (Exception e) {
            System.err.println("DEBUG: Exception in handleSave: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "An unexpected error occurred: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleCancel() {
        navigateBack();
    }

    private void navigateBack() {
        try {
            Node listView = FXMLLoader.load(getClass().getResource("/flight-list-view.fxml"));
            StackPane pageContainer = (StackPane) txtFlightId.getScene().lookup("#pageContainer");
            if (pageContainer != null) {
                pageContainer.getChildren().setAll(listView);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not load flight list: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private LocalTime parseTime(String timeStr) {
        try {
            // Try ISO first (HH:mm or HH:mm:ss)
            return LocalTime.parse(timeStr);
        } catch (DateTimeParseException e) {
            try {
                // Try simple HH:mm
                return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("H:mm"));
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Invalid time format. Use HH:mm (e.g., 14:30)");
            }
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
