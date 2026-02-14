package com.example.gestionvol.controller.user;

import com.example.gestionvol.MainApp;
import com.example.gestionvol.entities.Checkout;
import com.example.gestionvol.entities.Flight;
import com.example.gestionvol.service.CheckoutService;
import com.example.gestionvol.service.FlightService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import java.util.List;
import java.util.stream.Collectors;

public class UserDashboardController {
    @FXML private FlowPane flightGrid, bookingGrid;
    @FXML private TextField searchField;
    @FXML private Slider priceSlider;
    @FXML private Text priceLabel;

    private final FlightService flightService = new FlightService();
    private final CheckoutService checkoutService = new CheckoutService();
    private int userId = 1; // Logic placeholder

    private List<Flight> cachedFlights;

    @FXML
    public void initialize() {
        // Load data once
        refreshData();

        // Listeners for real-time filtering
        priceSlider.valueProperty().addListener((obs, old, val) -> {
            priceLabel.setText(String.format("%.0f DT", val.doubleValue()));
            updateFlightGrid();
        });
        
        searchField.textProperty().addListener((obs, old, val) -> {
            updateFlightGrid();
        });
        
        loadMyBookings();
    }
    
    // Fetch data from DB
    private void refreshData() {
        cachedFlights = flightService.getAvailableFlights();
        updateFlightGrid();
    }

    // Filter and update UI
    private void updateFlightGrid() {
        flightGrid.getChildren().clear();
        
        if (cachedFlights == null) return;

        String rawFilter = (searchField.getText() != null) ? searchField.getText().toLowerCase().trim() : "";
        double maxPrice = (priceSlider != null) ? priceSlider.getValue() : Double.MAX_VALUE;
        
        // Check if cached data is available, reload if necessary
        if (cachedFlights == null) {
            cachedFlights = flightService.getAvailableFlights();
            if (cachedFlights == null) return;
        }

        for (Flight f : cachedFlights) {
            boolean matches = false;
            
            if (rawFilter.isEmpty()) {
                matches = true;
            } else {
                if (containsIgnoreCase(f.getDestination(), rawFilter) ||
                    containsIgnoreCase(f.getAirline(), rawFilter) ||
                    containsIgnoreCase(f.getFlightId(), rawFilter) ||
                    containsIgnoreCase(f.getDepartureAirport(), rawFilter)) {
                    matches = true;
                }
            }
            
            if (f.getPrix() <= maxPrice && matches) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user/flight-card.fxml"));
                    VBox card = loader.load();
                    FlightCardController ctrl = loader.getController();
                    ctrl.setData(f, false, this::handleBook, null, null, null);
                    flightGrid.getChildren().add(card);
                } catch (Exception e) { 
                    e.printStackTrace(); 
                }
            }
        }
    }

    private boolean containsIgnoreCase(String source, String filter) {
        return source != null && source.toLowerCase().contains(filter);
    }

    private void loadMyBookings() {
        bookingGrid.getChildren().clear();
        List<Checkout> bookings = checkoutService.getAllCheckouts().stream()
                .filter(c -> c.getIdUser() == userId)
                .collect(Collectors.toList());
        for (Checkout c : bookings) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user/checkout-card.fxml"));
                VBox card = loader.load();
                CheckoutCardController ctrl = loader.getController();
                ctrl.setData(c, false, null, null, this::handleCancel);
                bookingGrid.getChildren().add(card);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void handleBook(Flight f) {
        // Pass flight to checkout
        CheckoutController.setSelectedFlight(f);
        MainApp.switchScene("/views/user/checkout-view.fxml", "Confirm Booking");
    }

    private void handleCancel(Checkout c) {
        if (checkoutService.cancelCheckout(c.getCheckoutId(), 1)) {
            loadMyBookings();
            refreshData(); // Refresh flights to show updated seat availability
        }
    }
    
    @FXML 
    private void handleRefresh() {
        refreshData();
        loadMyBookings();
    }

    @FXML private void handleAdminSwap() { MainApp.switchScene("/views/admin/admin-dashboard.fxml", "Admin Dashboard"); }
    @FXML private void handleThemeToggle() { MainApp.toggleTheme(); }
    @FXML private void handleLogout() { 
        // Placeholder for logout functionality
        System.out.println("Logout clicked");
    }
}
