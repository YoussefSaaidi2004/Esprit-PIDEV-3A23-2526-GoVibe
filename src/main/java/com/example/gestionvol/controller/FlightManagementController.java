package com.example.gestionvol.controller;

import com.example.gestionvol.entities.Flight;
import com.example.gestionvol.service.FlightService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FlightManagementController {
    @FXML private FlowPane flightGrid;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Text totalFlightsText, totalSeatsText, avgPriceText;

    private final FlightService flightService = new FlightService();
    private List<Flight> allFlights;

    @FXML
    public void initialize() {
        // Initialize sort options
        sortCombo.getItems().addAll(
            "Destination (A-Z)",
            "Destination (Z-A)",
            "Price (Low to High)",
            "Price (High to Low)",
            "Occupancy (Low to High)",
            "Occupancy (High to Low)",
            "Airline (A-Z)"
        );
        sortCombo.setValue("Destination (A-Z)");
        
        // Listeners
        searchField.textProperty().addListener((obs, old, newValue) -> loadFlights());
        sortCombo.valueProperty().addListener((obs, old, newValue) -> loadFlights());
        
        // Keyboard shortcuts
        searchField.setOnKeyPressed(event -> {
            KeyCombination ctrlN = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
            if (ctrlN.match(event)) {
                handleAdd();
                event.consume();
            }
        });
        
        loadFlights();
    }

    private void loadFlights() {
        flightGrid.getChildren().clear();
        allFlights = flightService.getAllFlights();
        String filter = (searchField != null && searchField.getText() != null) ? searchField.getText().toLowerCase().trim() : "";

        // Filter flights
        List<Flight> filteredFlights = allFlights.stream()
            .filter(f -> filter.isEmpty() || 
                        (f.getDestination() != null && f.getDestination().toLowerCase().contains(filter)) || 
                        (f.getAirline() != null && f.getAirline().toLowerCase().contains(filter)) ||
                        (f.getFlightId() != null && f.getFlightId().toLowerCase().contains(filter)) ||
                        (f.getDepartureAirport() != null && f.getDepartureAirport().toLowerCase().contains(filter)))
            .collect(Collectors.toList());
        
        // Sort flights
        String sortOption = sortCombo.getValue();
        if (sortOption != null) {
            Comparator<Flight> comparator = null;
            
            switch (sortOption) {
                case "Destination (A-Z)":
                    comparator = Comparator.comparing(Flight::getDestination, String.CASE_INSENSITIVE_ORDER);
                    break;
                case "Destination (Z-A)":
                    comparator = Comparator.comparing(Flight::getDestination, String.CASE_INSENSITIVE_ORDER).reversed();
                    break;
                case "Price (Low to High)":
                    comparator = Comparator.comparingDouble(Flight::getPrix);
                    break;
                case "Price (High to Low)":
                    comparator = Comparator.comparingDouble(Flight::getPrix).reversed();
                    break;
                case "Occupancy (Low to High)":
                    comparator = Comparator.comparingDouble((Flight f) -> 
                        f.getTotalSeats() > 0 ? (double)(f.getTotalSeats() - f.getAvailableSeats()) / f.getTotalSeats() : 0);
                    break;
                case "Occupancy (High to Low)":
                    comparator = Comparator.comparingDouble((Flight f) -> 
                        f.getTotalSeats() > 0 ? (double)(f.getTotalSeats() - f.getAvailableSeats()) / f.getTotalSeats() : 0).reversed();
                    break;
                case "Airline (A-Z)":
                    comparator = Comparator.comparing(Flight::getAirline, String.CASE_INSENSITIVE_ORDER);
                    break;
            }
            
            if (comparator != null) {
                filteredFlights.sort(comparator);
            }
        }
        
        // Update statistics
        updateStats(filteredFlights);
        
        // Display flight cards
        for (Flight f : filteredFlights) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/flight-card.fxml"));
                VBox card = loader.load();
                FlightCardController controller = loader.getController();
                controller.setData(f, true, null, this::handleEdit, this::handleCopy, this::handleDelete);
                flightGrid.getChildren().add(card);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void updateStats(List<Flight> flights) {
        totalFlightsText.setText(String.valueOf(flights.size()));
        
        int totalSeats = flights.stream()
            .mapToInt(Flight::getAvailableSeats)
            .sum();
        totalSeatsText.setText(String.valueOf(totalSeats));
        
        double avgPrice = flights.isEmpty() ? 0 : flights.stream()
            .mapToDouble(Flight::getPrix)
            .average()
            .orElse(0);
        avgPriceText.setText(String.format("%.0f DT", avgPrice));
    }

    @FXML private void handleAdd() { openForm(null); }
    @FXML private void handleRefresh() { loadFlights(); }
    private void handleEdit(Flight f) { openForm(f); }

    private void handleCopy(Flight f) {
        Flight copy = new Flight();
        copy.setFlightId(""); // Clear ID so user must enter a new one
        copy.setDepartureAirport(f.getDepartureAirport());
        copy.setDestination(f.getDestination());
        copy.setDepartureTime(f.getDepartureTime());
        copy.setArrivalTime(f.getArrivalTime());
        copy.setClasseChaise(f.getClasseChaise());
        copy.setAirline(f.getAirline() + " (Copy)");
        copy.setPrix(f.getPrix());
        copy.setAvailableSeats(f.getAvailableSeats());
        copy.setTotalSeats(f.getTotalSeats());
        copy.setDescription(f.getDescription());
        openForm(copy);
    }

    private void handleDelete(Flight f) {
        if (flightService.deleteFlight(f.getFlightId())) loadFlights();
    }

    private void openForm(Flight f) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/flight-form.fxml"));
            Parent root = loader.load();
            if (f != null) ((FlightFormController)loader.getController()).setFlightForEdit(f);
            Stage stage = new Stage();
            Scene scene = new Scene(root);
            // Apply unified styles
            scene.getStylesheets().add(com.example.gestionvol.MainApp.class.getResource("/styles/unified-styles.css").toExternalForm());
            // Apply dark mode if active
            if (com.example.gestionvol.MainApp.isDarkTheme()) {
                scene.getRoot().getStyleClass().add("dark-mode");
            }
            
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setMaximized(true);
            stage.showAndWait();
            loadFlights();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void navDashboard() { com.example.gestionvol.MainApp.switchScene("/views/admin-dashboard.fxml", "Admin Dashboard"); }
    @FXML private void navBookings() { com.example.gestionvol.MainApp.switchScene("/views/checkout-management.fxml", "All Reservations"); }
    @FXML private void navUser() { com.example.gestionvol.MainApp.switchScene("/views/user-dashboard.fxml", "User Dashboard"); }
    @FXML private void handleThemeToggle() { com.example.gestionvol.MainApp.toggleTheme(); }
}
