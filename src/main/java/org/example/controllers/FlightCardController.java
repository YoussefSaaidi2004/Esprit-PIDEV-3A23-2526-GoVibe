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
        priceText.setText(String.format("%.0f DT", (double) flight.getPrix()));
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
            double estimatedRevenue = booked * flight.getPrix();
            revenueText.setText(String.format("%.0f DT", estimatedRevenue));
        }

        // Action buttons
        actionBox.getChildren().clear();
        if (isAdmin) {
            javafx.scene.control.Button btnEdit = new javafx.scene.control.Button();
            javafx.scene.shape.SVGPath editIcon = new javafx.scene.shape.SVGPath();
            editIcon.setContent("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z");
            editIcon.setStyle("-fx-fill: white;");
            btnEdit.setGraphic(editIcon);
            btnEdit.setStyle("-fx-background-color: #50C878; -fx-cursor: hand;");
            btnEdit.setTooltip(new javafx.scene.control.Tooltip("Edit Flight"));
            btnEdit.setOnAction(e -> onEdit.accept(flight));
            
            javafx.scene.control.Button btnDelete = new javafx.scene.control.Button();
            javafx.scene.shape.SVGPath deleteIcon = new javafx.scene.shape.SVGPath();
            deleteIcon.setContent("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");
            deleteIcon.setStyle("-fx-fill: white;");
            btnDelete.setGraphic(deleteIcon);
            btnDelete.setStyle("-fx-background-color: #EF476F; -fx-cursor: hand;");
            btnDelete.setTooltip(new javafx.scene.control.Tooltip("Delete Flight"));
            btnDelete.setOnAction(e -> onDelete.accept(flight));
            
            actionBox.getChildren().addAll(btnEdit, btnDelete);
        } else {
            javafx.scene.control.Button btnBook = new javafx.scene.control.Button("✈ Réserver");
            btnBook.setPrefHeight(38);
            btnBook.setMaxWidth(Double.MAX_VALUE);
            btnBook.setTooltip(new javafx.scene.control.Tooltip("Réserver ce vol"));
            
            if (available == 0) {
                btnBook.setDisable(true);
                btnBook.setText("COMPLET");
                btnBook.setStyle("-fx-background-color: #8D99AE; -fx-text-fill: white; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 12; -fx-cursor: default;");
            } else {
                btnBook.setStyle("-fx-background-color: linear-gradient(to bottom, #50C878, #3DAF62); -fx-text-fill: white; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 13; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(80,200,120,0.3), 6, 0, 0, 2);");
                btnBook.setOnAction(e -> onBook.accept(flight));
            }
            
            actionBox.getChildren().add(btnBook);
        }
    }
}
