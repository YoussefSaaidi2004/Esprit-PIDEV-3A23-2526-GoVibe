package org.example.controllers;

import org.example.entities.Checkout;
import org.example.services.CheckoutService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.layout.FlowPane;

public class DisplayCheckoutsController {
    @FXML private FlowPane displayContainer;

    private final CheckoutService checkoutService = new CheckoutService();

    @FXML
    public void initialize() {
        loadAllCheckouts();
    }

    public void loadAllCheckouts() {
        if (displayContainer == null) return;
        displayContainer.getChildren().clear();
        //var checkouts = checkoutService.getAllCheckouts();
        // Display logic to be enhanced
    }

    private void handleApprove(Checkout c) {
        checkoutService.approveCheckout(c.getCheckoutId(), 1);
    }

    private void handleReject(Checkout c) {
        checkoutService.rejectCheckout(c.getCheckoutId(), 1, "Rejected");
    }

    private void handleCancel(Checkout c) {
        checkoutService.cancelCheckout(c.getCheckoutId(), 1);
    }

    private void showAlert(String t, String c, Alert.AlertType type) {
        Alert a = new Alert(type); 
        a.setTitle(t); 
        a.setContentText(c); 
        a.showAndWait();
    }
}
