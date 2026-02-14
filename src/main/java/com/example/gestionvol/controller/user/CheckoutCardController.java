package com.example.gestionvol.controller.user;

import com.example.gestionvol.dao.FlightDAO;
import com.example.gestionvol.entities.Checkout;
import com.example.gestionvol.entities.Flight;
import com.example.gestionvol.utils.IconUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import java.util.function.Consumer;

public class CheckoutCardController {
    @FXML private Text routeText, passengerText, priceText, dateText, airlineText;
    @FXML private Text statusText;
    @FXML private HBox actionBox;

    private Checkout checkout;
    private final FlightDAO flightDAO = new FlightDAO();

    public void setData(Checkout checkout, boolean isAdmin, 
                        Consumer<Checkout> onApprove, Consumer<Checkout> onReject, 
                        Consumer<Checkout> onCancel) {
        this.checkout = checkout;
        
        Flight flight = flightDAO.findById(checkout.getFlightId());
        if (flight != null) {
            routeText.setText(flight.getDepartureAirport() + " → " + flight.getDestination());
            airlineText.setText(flight.getAirline());
        }
        
        passengerText.setText(checkout.getPassengerNbr() + " passenger(s)");
        dateText.setText(checkout.getReservationDate().toString().split("T")[0]);
        priceText.setText(checkout.getTotalPrix() + " DT");

        statusText.setText(checkout.getStatusReservation());
        updateStatusStyle();

        actionBox.getChildren().clear();
        if (isAdmin) {
            if (checkout.isPending()) {
                // Icon-only approve button
                Button btnApprove = IconUtils.createIconButton("✓", "Approve", "#50C878");
                btnApprove.setPrefWidth(40);
                btnApprove.setPrefHeight(40);
                btnApprove.setOnAction(e -> onApprove.accept(checkout));
                
                // Icon-only reject button
                Button btnReject = IconUtils.createIconButton("❌", "Reject", "#EF476F");
                btnReject.setPrefWidth(40);
                btnReject.setPrefHeight(40);
                btnReject.setOnAction(e -> onReject.accept(checkout));
                
                actionBox.getChildren().addAll(btnApprove, btnReject);
            }
        } else {
            // User view - allow viewing details
            Button btnView = IconUtils.createIconButton("👁", "View Details", "#50C878");
            btnView.setOnAction(e -> handleViewDetails());
            actionBox.getChildren().add(btnView);
            
            if (checkout.isPending() || checkout.isConfirmed()) {
                Button btnCancel = IconUtils.createIconButton("🗑", "Cancel", "#EF476F");
                btnCancel.setOnAction(e -> onCancel.accept(checkout));
                actionBox.getChildren().add(btnCancel);
            }
        }
    }

    private void updateStatusStyle() {
        statusText.getStyleClass().removeAll("badge-pending", "badge-confirmed", "badge-rejected", "badge-cancelled");
        
        String status = checkout.getStatusReservation();
        if ("PENDING".equalsIgnoreCase(status)) {
            statusText.getStyleClass().add("badge-pending");
        } else if ("CONFIRMED".equalsIgnoreCase(status)) {
            statusText.getStyleClass().add("badge-confirmed");
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            statusText.getStyleClass().add("badge-rejected");
        } else if ("CANCELLED".equalsIgnoreCase(status)) {
            statusText.getStyleClass().add("badge-cancelled");
        }
    }

    private void handleViewDetails() {
        CheckoutDetailController.setSelectedCheckout(checkout);
        com.example.gestionvol.MainApp.switchScene("/views/user/checkout-detail.fxml", "Booking Details");
    }
}
