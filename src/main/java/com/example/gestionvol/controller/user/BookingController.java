package com.example.gestionvol.controller.user;

import com.example.gestionvol.entities.Checkout;
import com.example.gestionvol.entities.Flight;
import com.example.gestionvol.service.CheckoutService;
import com.example.gestionvol.utils.NotificationUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

import java.time.LocalDateTime;

public class BookingController {

    @FXML private StackPane rootPane;
    @FXML private Text flightRoute;
    @FXML private Text flightDetails;
    
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private Text nameError;
    @FXML private Text emailError;
    @FXML private Text phoneError;
    
    @FXML private Spinner<Integer> passengerSpinner;
    
    @FXML private ComboBox<String> classCombo;
    @FXML private ComboBox<String> paymentCombo;
    
    @FXML private ToggleButton windowBtn;
    @FXML private ToggleButton aisleBtn;
    private ToggleGroup seatGroup;
    
    @FXML private Text totalPrice;
    @FXML private StackPane loadingOverlay;
    
    private Flight selectedFlight;
    private final CheckoutService checkoutService = new CheckoutService();
    private final int DEFAULT_USER_ID = 1; // Placeholder until auth is implemented

    @FXML
    public void initialize() {
        // Load selected flight
        selectedFlight = CheckoutController.getSelectedFlight();
        
        if (selectedFlight != null) {
            flightRoute.setText(selectedFlight.getDepartureAirport() + " → " + selectedFlight.getDestination());
            flightDetails.setText(selectedFlight.getAirline() + " • " + selectedFlight.getDepartureTime());
        } else {
            // Handle case where no flight is selected (shouldn't happen in normal flow)
            flightRoute.setText("No Flight Selected");
        }

        // Setup Spinner
        passengerSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
        passengerSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updatePrice());

        // Setup ComboBoxes
        classCombo.getItems().addAll("Economy", "Business", "First Class");
        classCombo.setValue("Economy");
        classCombo.valueProperty().addListener((obs, oldVal, newVal) -> updatePrice());

        paymentCombo.getItems().addAll("Credit Card", "PayPal", "Bank Transfer");
        paymentCombo.setValue("Credit Card");

        // Setup Toggle Group for Seats
        seatGroup = new ToggleGroup();
        windowBtn.setToggleGroup(seatGroup);
        aisleBtn.setToggleGroup(seatGroup);
        
        // Default selection
        windowBtn.setSelected(true);

        updatePrice();
        System.out.println("✅ Booking Controller initialized");
    }

    private void updatePrice() {
        if (selectedFlight == null) return;
        
        int basePrice = selectedFlight.getPrix();
        int passengers = passengerSpinner.getValue();
        
        // Multiplier based on class
        double multiplier = 1.0;
        String travelClass = classCombo.getValue();
        if ("Business".equals(travelClass)) multiplier = 1.5;
        else if ("First Class".equals(travelClass)) multiplier = 2.0;

        int total = (int) (basePrice * passengers * multiplier);
        totalPrice.setText(total + " DT");
    }

    @FXML
    private void handleConfirm() {
        if (!validateInput()) return;
        
        loadingOverlay.setVisible(true);
        
        new Thread(() -> {
            try {
                // Simulate network delay
                Thread.sleep(1000);
                
                Checkout checkout = new Checkout();
                checkout.setFlightId(selectedFlight.getFlightId());
                checkout.setIdUser(DEFAULT_USER_ID);
                checkout.setReservationDate(LocalDateTime.now());
                checkout.setPassengerNbr(passengerSpinner.getValue());
                checkout.setStatusReservation("PENDING");
                
                // Calculate Total Price again to be safe
                String priceText = totalPrice.getText().replace(" DT", "");
                checkout.setTotalPrix(Integer.parseInt(priceText));
                
                // Additional Details
                checkout.setPassengerName(nameField.getText());
                checkout.setPassengerEmail(emailField.getText());
                checkout.setPassengerPhone(phoneField.getText());
                checkout.setTravelClass(classCombo.getValue());
                checkout.setPaymentMethod(paymentCombo.getValue());
                
                if (windowBtn.isSelected()) checkout.setSeatPreference("Window");
                else if (aisleBtn.isSelected()) checkout.setSeatPreference("Aisle");
                else checkout.setSeatPreference("Any");

                boolean success = checkoutService.addCheckout(checkout);
                
                Platform.runLater(() -> {
                    loadingOverlay.setVisible(false);
                    if (success) {
                        NotificationUtils.showSuccess(rootPane, "Booking Confirmed!");
                        // Slight delay before going back
                        new Thread(() -> {
                            try { Thread.sleep(1500); } catch (Exception e) {}
                            Platform.runLater(this::handleBack);
                        }).start();
                    } else {
                        NotificationUtils.showError(rootPane, "Booking failed. Please try again.");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    loadingOverlay.setVisible(false);
                    NotificationUtils.showError(rootPane, "Error: " + e.getMessage());
                });
            }
        }).start();
    }

    private boolean validateInput() {
        boolean valid = true;
        
        nameError.setVisible(false);
        emailError.setVisible(false);
        phoneError.setVisible(false);
        
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            nameError.setVisible(true);
            valid = false;
        }
        
        if (emailField.getText() == null || !emailField.getText().contains("@")) {
            emailError.setVisible(true);
            valid = false;
        }
        
        if (phoneField.getText() == null || phoneField.getText().trim().length() < 8) {
            phoneError.setVisible(true);
            valid = false;
        }
        
        return valid;
    }

    @FXML
    private void handleBack() {
        // Return to User Dashboard
        com.example.gestionvol.MainApp.switchScene("/views/user/user-dashboard.fxml", "User Dashboard");
    }
}
