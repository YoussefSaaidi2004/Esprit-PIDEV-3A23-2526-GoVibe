package org.example.controllers;

import org.example.entities.Flight;
import org.example.services.FlightService;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

public class FlightListController {
    @FXML private Text flightListText;
    @FXML private TextField searchField;

    private final FlightService flightService = new FlightService();

    @FXML
    public void initialize() {
        loadFlights();
    }

    private void loadFlights() {
        var flights = flightService.getAllFlights();
        if (flights.isEmpty()) {
            flightListText.setText("No flights available");
        } else {
            StringBuilder sb = new StringBuilder();
            for (Flight f : flights) {
                sb.append(f.getFlightId()).append(": ").append(f.getDestination()).append("\n");
            }
            flightListText.setText(sb.toString());
        }
    }
}
