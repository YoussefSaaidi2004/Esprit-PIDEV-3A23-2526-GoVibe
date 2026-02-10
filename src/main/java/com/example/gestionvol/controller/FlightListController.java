package com.example.gestionvol.controller;

import com.example.gestionvol.entities.Flight;
import com.example.gestionvol.service.FlightService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class FlightListController {

    @FXML private FlowPane cardsFlow;
    @FXML private StackPane flightCenterStack;
    @FXML private TextField searchDest;
    @FXML private DatePicker searchDate;
    @FXML private ComboBox<String> searchClass;

    private final FlightService flightService = new FlightService();
    private final ObservableList<Flight> flightList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupFilters();
        loadFlights();
        
        // Responsive listener logic
        if (flightCenterStack != null && cardsFlow != null) {
            flightCenterStack.widthProperty().addListener((obs, oldW, newW) -> {
                double w = newW.doubleValue();
                cardsFlow.setPrefWrapLength(Math.max(320, w - 40));
            });
        }
    }

    private void setupFilters() {
        // Initialize Class ComboBox
        ObservableList<String> classes = FXCollections.observableArrayList("Economy", "Business", "First Class");
        if (searchClass != null) {
            searchClass.setItems(classes);
            searchClass.valueProperty().addListener((obs, o, n) -> applyFilters());
        }

        // Add Listeners to other inputs
        if (searchDest != null) {
            searchDest.textProperty().addListener((obs, o, n) -> applyFilters());
        }
        if (searchDate != null) {
            searchDate.valueProperty().addListener((obs, o, n) -> applyFilters());
        }
    }

    private void applyFilters() {
        String dest = searchDest.getText() != null ? searchDest.getText().toLowerCase().trim() : "";
        String sClass = searchClass.getValue();
        java.time.LocalDate date = searchDate.getValue();

        ObservableList<Flight> filtered = flightList.filtered(f -> {
            boolean matchDest = dest.isEmpty() || 
                                f.getDestination().toLowerCase().contains(dest) ||
                                f.getDepartureAirport().toLowerCase().contains(dest);
            
            boolean matchClass = sClass == null || sClass.isEmpty() || 
                                 (f.getClasseChaise() != null && f.getClasseChaise().equalsIgnoreCase(sClass));

            boolean matchDate = date == null || 
                                (f.getDepartureTime() != null && f.getDepartureTime().toString().startsWith(date.toString())); // Basic string date check

            return matchDest && matchClass && matchDate;
        });

        renderFilteredCards(filtered);
    }

    private void loadFlights() {
        flightList.clear();
        flightList.addAll(flightService.getAllFlights());
        applyFilters(); // Initial render via filter logic
    }
    
    private void renderFilteredCards(ObservableList<Flight> flights) {
        if (cardsFlow == null) return;
        cardsFlow.getChildren().clear();
        for (Flight f : flights) {
            cardsFlow.getChildren().add(createCardForFlight(f));
        }
    }

    private VBox createCardForFlight(Flight f) {
        VBox card = new VBox(8);
        card.getStyleClass().add("flight-card"); 
        
        // CSS Driven styling is preferred, but keeping inline for specific overrides if needed
        // Using "ticket stub" style from CSS, so we minimize inline styles here
        
        // Header: Flight ID & Airline
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label lblAirline = new Label(f.getAirline());
        lblAirline.getStyleClass().add("card-field");
        header.getChildren().addAll(lblAirline);

        // Route: Dep -> Dest
        HBox route = new HBox(5);
        route.setAlignment(Pos.CENTER_LEFT);
        Label lblDep = new Label(f.getDepartureAirport());
        lblDep.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        Label arrow = new Label("✈");
        arrow.setStyle("-fx-text-fill: #aaa;");
        Label lblDest = new Label(f.getDestination());
        lblDest.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        route.getChildren().addAll(lblDep, arrow, lblDest);

        // Times
        Label lblTimes = new Label("🕒 " + f.getDepartureTime() + " - " + f.getArrivalTime());
        lblTimes.getStyleClass().add("card-field");
        
        // Price & Class
        HBox details = new HBox(10);
        Label lblPrice = new Label(f.getPrix() + " DT");
        lblPrice.getStyleClass().add("card-price-badge");
        
        Label lblClass = new Label(f.getClasseChaise());
        lblClass.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; -fx-padding: 2 6; -fx-background-radius: 4;");
        details.getChildren().addAll(lblPrice, lblClass);

        // Seats & Desc
        Label lblSeats = new Label("Seats: " + f.getAvailableSeats());
        lblSeats.getStyleClass().add("card-field");
        
        Text desc = new Text(f.getDescription());
        desc.setWrappingWidth(300);
        desc.setStyle("-fx-fill: #555;");

        // Actions
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        
        Button btnEdit = new Button("✏️");
        btnEdit.getStyleClass().addAll("button", "btn-edit");
        btnEdit.setOnAction(e -> handleEditFlight(f));
        
        Button btnDelete = new Button("🗑️");
        btnDelete.getStyleClass().addAll("button", "btn-delete");
        btnDelete.setOnAction(e -> handleDeleteFlight(f));
        
        actions.getChildren().addAll(btnEdit, btnDelete);

        // Animation
        javafx.animation.ScaleTransition scaleTransition = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(200), card);
        card.setOnMouseEntered(e -> {
            scaleTransition.setToX(1.03);
            scaleTransition.setToY(1.03);
            scaleTransition.playFromStart();
        });
        card.setOnMouseExited(e -> {
            scaleTransition.setToX(1.0);
            scaleTransition.setToY(1.0);
            scaleTransition.playFromStart();
        });

        card.getChildren().addAll(header, route, lblTimes, details, lblSeats, desc, new Separator(), actions);
        return card;
    }


    @FXML
    private void handleClearFilters() {
        if(searchDest != null) searchDest.clear();
        if(searchDate != null) searchDate.setValue(null);
        if(searchClass != null) searchClass.setValue(null);
        applyFilters();
    }

    @FXML
    private void handleAddFlight() {
        navigateToForm(null);
    }

    @FXML
    private void handleRefresh() {
        // Clear filters first so user sees all flights
        handleClearFilters();
        loadFlights();
    }

    private void handleEditFlight(Flight flight) {
        navigateToForm(flight);
    }

    private void handleDeleteFlight(Flight flight) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Delete");
        confirmation.setHeaderText("Delete Flight");
        confirmation.setContentText("Are you sure you want to delete flight " + flight.getFlightId() + "?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (flightService.deleteFlight(flight.getFlightId())) {
                loadFlights();
            } else {
                showAlert("Error", "Failed to delete flight.", Alert.AlertType.ERROR);
            }
        }
    }

    private void navigateToForm(Flight flight) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/flight-form-view.fxml"));
            Node formView = loader.load();
            
            FlightFormController controller = loader.getController();
            if (flight != null) {
                controller.setFlightForEdit(flight);
            }

            StackPane pageContainer = (StackPane) cardsFlow.getScene().lookup("#pageContainer");
            if (pageContainer != null) {
                pageContainer.getChildren().setAll(formView);
            } else {
                System.err.println("Could not find #pageContainer to navigate");
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not load form view: " + e.getMessage(), Alert.AlertType.ERROR);
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
