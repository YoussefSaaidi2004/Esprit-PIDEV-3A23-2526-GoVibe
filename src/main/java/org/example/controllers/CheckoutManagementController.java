package org.example.controllers;

import org.example.entities.Checkout;
import org.example.services.CheckoutService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.List;

public class CheckoutManagementController {
    @FXML private FlowPane checkoutGrid;

    private final CheckoutService checkoutService = new CheckoutService();

    @FXML
    public void initialize() {
        loadCheckouts();
    }

    @FXML
    public void handleDashboard() {
        try {
            Stage currentStage = (Stage) checkoutGrid.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/AdminDashboardView.fxml"));
            Parent root = loader.load();
            currentStage.setScene(new Scene(root));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void handlePersonnes() {
        try {
            Stage currentStage = (Stage) checkoutGrid.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/PersonneView.fxml"));
            Parent root = loader.load();
            currentStage.setScene(new Scene(root));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void handleVoitures() {
        try {
            Stage currentStage = (Stage) checkoutGrid.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VoitureListView.fxml"));
            Parent root = loader.load();
            currentStage.setScene(new Scene(root));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void handleLocations() {
        try {
            Stage currentStage = (Stage) checkoutGrid.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdminLocationListView.fxml"));
            Parent root = loader.load();
            currentStage.setScene(new Scene(root));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void handleFlights() {
        try {
            Stage currentStage = (Stage) checkoutGrid.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/flight-management.fxml"));
            Parent root = loader.load();
            currentStage.setScene(new Scene(root));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void handleLogout() {
        try {
            Stage currentStage = (Stage) checkoutGrid.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/LoginView.fxml"));
            Parent root = loader.load();
            currentStage.setScene(new Scene(root));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadCheckouts() {
        checkoutGrid.getChildren().clear();
        List<Checkout> list = checkoutService.getAllCheckouts();

        for (Checkout c : list) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/checkout-card.fxml"));
                VBox card = loader.load();
                CheckoutCardController controller = loader.getController();
                controller.setData(c, true, this::handleApprove, this::handleReject, this::handleCancel);
                checkoutGrid.getChildren().add(card);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void handleApprove(Checkout c) {
        if (checkoutService.approveCheckout(c.getCheckoutId(), 1)) loadCheckouts();
    }

    private void handleReject(Checkout c) {
        if (checkoutService.rejectCheckout(c.getCheckoutId(), 1, "Rejected by Admin")) loadCheckouts();
    }

    private void handleCancel(Checkout c) {
        if (checkoutService.cancelCheckout(c.getCheckoutId(), 1)) loadCheckouts();
    }
}
