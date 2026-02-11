package com.example.gestionvol.controller;

import com.example.gestionvol.entities.Checkout;
import com.example.gestionvol.service.CheckoutService;
import com.example.gestionvol.service.DashboardService;
import com.example.gestionvol.MainApp;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import java.util.List;

public class AdminDashboardController {
    @FXML private Text pendingCount, flightCount, revenueText, occupancyText;
    @FXML private FlowPane pendingGrid;

    private final DashboardService dashService = new DashboardService();
    private final CheckoutService checkoutService = new CheckoutService();

    @FXML
    public void initialize() {
        refreshMetrics();
        loadPending();
    }

    private void refreshMetrics() {
        pendingCount.setText(String.valueOf(dashService.getPendingApprovalsCount()));
        flightCount.setText(String.valueOf(dashService.getTotalFlights()));
        revenueText.setText(String.format("%.0f DT", dashService.getTotalRevenue()));
        occupancyText.setText(String.format("%.1f%%", dashService.getAverageOccupancyRate()));
    }

    private void loadPending() {
        pendingGrid.getChildren().clear();
        List<Checkout> pending = checkoutService.getPendingCheckouts();
        for (Checkout c : pending) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/checkout-card.fxml"));
                VBox card = loader.load();
                CheckoutCardController controller = loader.getController();
                controller.setData(c, true, this::handleApprove, this::handleReject, null);
                pendingGrid.getChildren().add(card);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void handleApprove(Checkout c) {
        if (checkoutService.approveCheckout(c.getCheckoutId(), 1)) {
            refreshMetrics();
            loadPending();
        }
    }

    private void handleReject(Checkout c) {
        if (checkoutService.rejectCheckout(c.getCheckoutId(), 1, "Rejected")) {
            refreshMetrics();
            loadPending();
        }
    }

    @FXML private void navPending() { navBookings(); }
    @FXML private void navDashboard() { refreshMetrics(); loadPending(); }
    @FXML private void navFlights() { MainApp.switchScene("/views/flight-management.fxml", "Flight Management"); }
    @FXML private void navBookings() { MainApp.switchScene("/views/checkout-management.fxml", "All Reservations"); }
    @FXML private void navUser() { MainApp.switchScene("/views/user-dashboard.fxml", "User Dashboard"); }
    @FXML private void handleThemeToggle() { MainApp.toggleTheme(); }
}
