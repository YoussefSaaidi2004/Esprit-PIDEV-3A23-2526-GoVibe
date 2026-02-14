package com.example.gestionvol.controller.admin;

import com.example.gestionvol.controller.user.CheckoutCardController;
import com.example.gestionvol.dao.FlightDAO;
import com.example.gestionvol.entities.Checkout;
import com.example.gestionvol.entities.Flight;
import com.example.gestionvol.service.CheckoutService;
import com.example.gestionvol.service.NotificationService;
import com.example.gestionvol.service.PDFService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import java.util.List;
import java.util.stream.Collectors;

public class CheckoutManagementController {
    @FXML private FlowPane checkoutGrid;

    private final CheckoutService checkoutService = new CheckoutService();
    private final FlightDAO flightDAO = new FlightDAO();

    @FXML
    public void initialize() {
        loadCheckouts();
    }

    private void loadCheckouts() {
        checkoutGrid.getChildren().clear();
        List<Checkout> list = checkoutService.getAllCheckouts();

        for (Checkout c : list) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user/checkout-card.fxml"));
                VBox card = loader.load();
                CheckoutCardController controller = loader.getController();
                controller.setData(c, true, this::handleApprove, this::handleReject, this::handleCancel);
                checkoutGrid.getChildren().add(card);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void handleApprove(Checkout c) {
        if (checkoutService.approveCheckout(c.getCheckoutId(), 1)) {
            // Generate booking confirmation file
            Flight flight = flightDAO.findById(c.getFlightId());
            String pdfPath = PDFService.generateBookingConfirmation(c.getIdUser(), c, flight);
            
            // Send notification to user
            NotificationService.onBookingAccepted(c.getIdUser(), c.getCheckoutId(), pdfPath);
            
            loadCheckouts();
        }
    }

    private void handleReject(Checkout c) {
        if (checkoutService.rejectCheckout(c.getCheckoutId(), 1, "Rejected by Admin")) {
            // Send rejection notification
            NotificationService.onBookingRejected(c.getIdUser(), c.getCheckoutId());
            
            loadCheckouts();
        }
    }

    private void handleCancel(Checkout c) {
        if (checkoutService.cancelCheckout(c.getCheckoutId(), 1)) loadCheckouts();
    }

    @FXML private void navDashboard() { com.example.gestionvol.MainApp.switchScene("/views/admin/admin-dashboard.fxml", "Admin Dashboard"); }
    @FXML private void navFlights() { com.example.gestionvol.MainApp.switchScene("/views/admin/flight-management.fxml", "Flight Management"); }
    @FXML private void navUser() { com.example.gestionvol.MainApp.switchScene("/views/user/user-dashboard.fxml", "User Dashboard"); }
    @FXML private void handleThemeToggle() { com.example.gestionvol.MainApp.toggleTheme(); }
}

