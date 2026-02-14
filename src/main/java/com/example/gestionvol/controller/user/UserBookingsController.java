package com.example.gestionvol.controller.user;

import com.example.gestionvol.entities.Checkout;
import com.example.gestionvol.service.CheckoutService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.stream.Collectors;

public class UserBookingsController {

    @FXML private FlowPane bookingGrid;
    
    private final CheckoutService checkoutService = new CheckoutService();
    private int userId = 1; // Logic placeholder

    @FXML
    public void initialize() {
        loadMyBookings();
    }

    private void loadMyBookings() {
        if (bookingGrid != null) bookingGrid.getChildren().clear();
        
        List<Checkout> bookings = checkoutService.getAllCheckouts().stream()
                .filter(c -> c.getIdUser() == userId)
                .collect(Collectors.toList());
        
        for (Checkout c : bookings) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user/checkout-card.fxml"));
                VBox card = loader.load();
                CheckoutCardController ctrl = loader.getController();
                ctrl.setData(c, false, null, null, this::handleCancel);
                if (bookingGrid != null) bookingGrid.getChildren().add(card);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void handleCancel(Checkout c) {
        if (checkoutService.cancelCheckout(c.getCheckoutId(), 1)) {
            loadMyBookings();
        }
    }
    
    @FXML 
    private void handleRefresh() {
        loadMyBookings();
    }
}
