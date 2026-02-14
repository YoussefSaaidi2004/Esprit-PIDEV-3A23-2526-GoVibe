package com.example.gestionvol.controller.user;

import com.example.gestionvol.MainApp;
import com.example.gestionvol.entities.Flight;
import com.example.gestionvol.service.FlightService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.List;

public class UserFlightsController {
    
    @FXML private FlowPane flightGrid;
    @FXML private TextField searchField;
    @FXML private Slider priceSlider;
    @FXML private Text priceLabel;

    private final FlightService flightService = new FlightService();
    private List<Flight> cachedFlights;

    @FXML
    public void initialize() {
        refreshData();

        // Listeners for real-time filtering
        if (priceSlider != null) {
            priceSlider.valueProperty().addListener((obs, old, val) -> {
                if (priceLabel != null) {
                    priceLabel.setText(String.format("%.0f DT", val.doubleValue()));
                }
                updateFlightGrid();
            });
        }
        
        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, val) -> {
                updateFlightGrid();
            });
        }
    }
    
    private void refreshData() {
        cachedFlights = flightService.getAvailableFlights();
        updateFlightGrid();
    }

    private void updateFlightGrid() {
        if (flightGrid != null) flightGrid.getChildren().clear();
        
        // Check if cached data is available, reload if necessary
        if (cachedFlights == null) {
            cachedFlights = flightService.getAvailableFlights();
            if (cachedFlights == null) return;
        }

        String rawFilter = (searchField != null && searchField.getText() != null) ? searchField.getText().toLowerCase().trim() : "";
        double maxPrice = (priceSlider != null) ? priceSlider.getValue() : Double.MAX_VALUE;

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
                    if (flightGrid != null) flightGrid.getChildren().add(card);
                } catch (Exception e) { 
                    e.printStackTrace(); 
                }
            }
        }
    }

    private boolean containsIgnoreCase(String source, String filter) {
        return source != null && source.toLowerCase().contains(filter);
    }

    private void handleBook(Flight f) {
        // Pass flight to checkout
        CheckoutController.setSelectedFlight(f);
        MainApp.switchScene("/views/user/checkout-view.fxml", "Confirm Booking");
    }
}
