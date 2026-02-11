package com.example.gestionvol.controller;

import com.example.gestionvol.components.CheckoutCard;
import com.example.gestionvol.entities.Checkout;
import com.example.gestionvol.service.CheckoutService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.FlowPane;
import java.util.List;

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
        List<Checkout> checkouts = checkoutService.getAllCheckouts();
        for (Checkout c : checkouts) {
            CheckoutCard card = new CheckoutCard(c, 
                this::handleApprove, 
                this::handleReject, 
                this::handleCancel);
            displayContainer.getChildren().add(card);
        }
    }

    private void handleApprove(Checkout c) {
        if (checkoutService.approveCheckout(c.getCheckoutId(), 1)) {
            loadAllCheckouts();
        } else {
            showAlert("Error", "Could not approve checkout.", Alert.AlertType.ERROR);
        }
    }

    private void handleReject(Checkout c) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reject Reservation");
        dialog.setHeaderText("Reason for rejection:");
        dialog.showAndWait().ifPresent(reason -> {
            checkoutService.rejectCheckout(c.getCheckoutId(), 1, reason);
            loadAllCheckouts();
        });
    }

    private void handleCancel(Checkout c) {
        if (checkoutService.cancelCheckout(c.getCheckoutId(), 1)) {
            loadAllCheckouts();
        }
    }

    private void showAlert(String t, String c, Alert.AlertType type) {
        Alert a = new Alert(type); a.setTitle(t); a.setContentText(c); a.showAndWait();
    }
}
