package org.example.controllers;

import org.example.entities.Flight;
import org.example.utils.IconUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import java.util.function.Consumer;

public class FlightCardController {
    @FXML private Text routeText, airlineText, timeText, seatsText, priceText,  classText;
    @FXML private Text statusText, occupancyText, revenueText;
    @FXML private HBox actionBox, statusBadge;
    @FXML private VBox revenueBox;
    @FXML private ProgressBar occupancyBar;

    private Flight flight;

    public void setData(Flight flight, boolean isAdmin, Consumer<Flight> onBook, Consumer<Flight> onEdit, Consumer<Flight> onCopy, Consumer<Flight> onDelete) {
        this.flight = flight;
        
        // Basic info
        routeText.setText(flight.getDepartureAirport() + " → " + flight.getDestination());
        airlineText.setText(flight.getAirline());
        timeText.setText(flight.getDepartureTime() + " - " + flight.getArrivalTime());
        priceText.setText(String.format("%.0f DT", flight.getPrix() != null ? flight.getPrix().doubleValue() : 0));
        classText.setText(flight.getClasseChaise());

        // Calculate occupancy
        int total = flight.getTotalSeats();
        int available = flight.getAvailableSeats();
        int booked = total - available;
        double occupancyRate = total > 0 ? (double) booked / total : 0;
        
        seatsText.setText(available + "/" + total + " seats");
        occupancyText.setText(String.format("%.0f%%", occupancyRate * 100));
        occupancyBar.setProgress(occupancyRate);
        
        // Set occupancy bar color based on fill rate
        if (occupancyRate >= 0.8) {
            occupancyText.setStyle("-fx-fill: #EF476F; -fx-font-weight: bold; -fx-font-size: 10;");
        } else if (occupancyRate >= 0.5) {
            occupancyText.setStyle("-fx-fill: #FFD166; -fx-font-weight: bold; -fx-font-size: 10;");
        } else {
            occupancyText.setStyle("-fx-fill: #50C878; -fx-font-weight: bold; -fx-font-size: 10;");
        }

        // Flight status (mock - could be enhanced with real status logic)
        String status = "ON TIME";
        String statusStyle = "-fx-background-color: #D1F2EB; -fx-text-fill: #013220;";
        
        if (available == 0) {
            status = "FULL";
            statusStyle = "-fx-background-color: #EF476F; -fx-text-fill: white;";
        } else if (occupancyRate >= 0.9) {
            status = "FILLING";
            statusStyle = "-fx-background-color: #FFD166; -fx-text-fill: #013220;";
        }
        
        statusText.setText(status);
        statusBadge.setStyle(statusStyle + "; -fx-padding: 5 10; -fx-background-radius: 15;");

        // Admin-specific features
        if (isAdmin) {
            revenueBox.setVisible(true);
            revenueBox.setManaged(true);
            double estimatedRevenue = booked * (flight.getPrix() != null ? flight.getPrix().doubleValue() : 0);
            revenueText.setText(String.format("%.0f DT", estimatedRevenue));
        }

        // Action buttons
        actionBox.getChildren().clear();
        if (isAdmin) {
            javafx.scene.control.Button btnEdit = new javafx.scene.control.Button("Editer");
            btnEdit.getStyleClass().add("fi-btn-edit");
            javafx.scene.control.Label editLabel = new javafx.scene.control.Label("Éditer");
            editLabel.getStyleClass().add("fi-btn-label-edit");
            btnEdit.setGraphic(editLabel);
            btnEdit.setText("");
            btnEdit.setTooltip(new javafx.scene.control.Tooltip("Edit Flight"));
            btnEdit.setOnAction(e -> onEdit.accept(flight));
            
            javafx.scene.control.Button btnDelete = new javafx.scene.control.Button("Supprimer");
            btnDelete.getStyleClass().add("fi-btn-delete");
            javafx.scene.control.Label deleteLabel = new javafx.scene.control.Label("Supprimer");
            deleteLabel.getStyleClass().add("fi-btn-label-delete");
            btnDelete.setGraphic(deleteLabel);
            btnDelete.setText("");
            btnDelete.setTooltip(new javafx.scene.control.Tooltip("Delete Flight"));
            btnDelete.setOnAction(e -> onDelete.accept(flight));
            
            actionBox.getChildren().addAll(btnEdit, btnDelete);
        } else {
            javafx.scene.control.Button btnBook = new javafx.scene.control.Button("Réserver");
            btnBook.setPrefHeight(38);
            btnBook.setMaxWidth(Double.MAX_VALUE);
            btnBook.setTooltip(new javafx.scene.control.Tooltip("Réserver ce vol"));
            
            if (available == 0) {
                btnBook.setDisable(true);
                btnBook.setText("COMPLET");
                btnBook.setStyle("-fx-background-color: #8D99AE; -fx-text-fill: white; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 12; -fx-cursor: default;");
            } else {
                btnBook.getStyleClass().add("fi-btn-view");
                javafx.scene.control.Label bookLabel = new javafx.scene.control.Label("Réserver");
                bookLabel.getStyleClass().add("fi-btn-label-view");
                btnBook.setGraphic(bookLabel);
                btnBook.setText("");
                btnBook.setOnAction(e -> onBook.accept(flight));
            }
            
            actionBox.getChildren().add(btnBook);
        }
    }
}
