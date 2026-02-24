package org.example.controllers;

import org.example.entities.Checkout;
import org.example.entities.Flight;
import org.example.services.CheckoutService;
import org.example.utils.NotificationUtils;
import org.example.utils.SessionManager;
import org.example.utils.SceneNavigator;
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
    @FXML private Text nameError, emailError, phoneError;
    
    @FXML private Spinner<Integer> passengerSpinner;
    @FXML private ComboBox<String> classCombo;
    @FXML private ComboBox<String> paymentCombo;
    @FXML private ToggleButton windowBtn, aisleBtn;
    @FXML private Text totalPrice;
    @FXML private StackPane loadingOverlay;
    
    private Flight selectedFlight;
    private final CheckoutService checkoutService = new CheckoutService();
    private int userId = -1;
    private ToggleGroup seatGroup;

    @FXML
    public void initialize() {
        if (SessionManager.getCurrentUser() != null) {
            userId = SessionManager.getCurrentUser().getId();
        }
        selectedFlight = CheckoutController.getSelectedFlight();
        
        if (selectedFlight != null) {
            flightRoute.setText(selectedFlight.getDepartureAirport() + " → " + selectedFlight.getDestination());
            flightDetails.setText(selectedFlight.getAirline() + " • " + selectedFlight.getDepartureTime());
        }

        passengerSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
        passengerSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updatePrice());

        classCombo.getItems().addAll("Economy", "Business", "First Class");
        classCombo.setValue("Economy");
        classCombo.valueProperty().addListener((obs, oldVal, newVal) -> updatePrice());

        paymentCombo.getItems().addAll("Credit Card", "PayPal", "Bank Transfer");
        paymentCombo.setValue("Credit Card");

        seatGroup = new ToggleGroup();
        windowBtn.setToggleGroup(seatGroup);
        aisleBtn.setToggleGroup(seatGroup);
        windowBtn.setSelected(true);

        updatePrice();
    }

    private void updatePrice() {
        if (selectedFlight == null) return;
        
        int basePrice = selectedFlight.getPrix();
        int passengers = passengerSpinner.getValue();
        
        double multiplier = 1.0;
        String travelClass = classCombo.getValue();
        if ("Business".equals(travelClass)) multiplier = 1.5;
        else if ("First Class".equals(travelClass)) multiplier = 2.0;

        int total = (int) (basePrice * passengers * multiplier);
        totalPrice.setText(total + " DT");
    }

    @FXML
    private void handleConfirm() {
        if (selectedFlight == null) {
            NotificationUtils.showError(rootPane, "No flight selected.");
            return;
        }
        if (userId <= 0) {
            NotificationUtils.showError(rootPane, "Please login to book a flight.");
            return;
        }
        if (!validateInput()) return;
        
        loadingOverlay.setVisible(true);
        
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                
                Checkout checkout = new Checkout();
                checkout.setFlightId(selectedFlight.getFlightId());
                checkout.setIdUser(userId);
                checkout.setReservationDate(LocalDateTime.now());
                checkout.setPassengerNbr(passengerSpinner.getValue());
                checkout.setStatusReservation("PENDING");
                checkout.setPassengerName(nameField.getText().trim());
                checkout.setPassengerEmail(emailField.getText().trim());
                checkout.setPassengerPhone(phoneField.getText().trim());
                checkout.setTravelClass(classCombo.getValue());
                checkout.setPaymentMethod(paymentCombo.getValue());
                checkout.setSeatPreference(windowBtn.isSelected() ? "WINDOW" : "AISLE");
                
                String priceText = totalPrice.getText().replace(" DT", "");
                checkout.setTotalPrix(new java.math.BigDecimal(priceText));

                boolean success = checkoutService.addCheckout(checkout);
                
                Platform.runLater(() -> {
                    loadingOverlay.setVisible(false);
                    if (success) {
                        NotificationUtils.showSuccess(rootPane, "Booking Confirmed!");
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
        org.example.mains.MainApp.switchScene("/views/user-dashboard.fxml", "User Dashboard");
    }

    @FXML
    private void handleHome() {
        org.example.mains.MainApp.switchScene("/UserHome.fxml", "Accueil");
    }

    @FXML
    private void handleFlights() {
        org.example.mains.MainApp.switchScene("/views/user-dashboard.fxml", "User Dashboard");
    }

    @FXML
    private void handleLocations() {
        SceneNavigator.switchTo("/LocationListView.fxml", rootPane);
    }

    @FXML
    private void handleLogout() {
        SessionManager.clear();
        org.example.mains.MainApp.switchScene("/org/example/LoginView.fxml", "GoVibe Connexion");
    }
}
