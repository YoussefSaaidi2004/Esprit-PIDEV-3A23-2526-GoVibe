package org.example.controllers;

import org.example.entities.Flight;
import org.example.services.FlightService;
import org.example.services.CheckoutService;
import org.example.entities.Checkout;
import org.example.mains.MainApp;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import java.util.List;
import java.util.stream.Collectors;

public class UserDashboardController {
    @FXML
    private FlowPane flightGrid, bookingGrid;
    @FXML
    private TextField searchField;
    @FXML
    private Slider priceSlider;
    @FXML
    private Label priceLabel;
    @FXML
    private ScrollPane mainScroll;
    @FXML
    private VBox contentRoot;
    @FXML
    private VBox flightsSection;
    @FXML
    private VBox bookingsSection;
    @FXML
    private ToggleButton bookTab;
    @FXML
    private ToggleButton bookingsTab;

    private final FlightService flightService = new FlightService();
    private final CheckoutService checkoutService = new CheckoutService();
    private int userId = -1;

    @FXML
    public void initialize() {
        System.out.println("[UserDashboard] initialize");
        if (SessionManager.getCurrentUser() != null) {
            userId = SessionManager.getCurrentUser().getId();
        }
        ToggleGroup sectionTabs = new ToggleGroup();
        if (bookTab != null) {
            bookTab.setToggleGroup(sectionTabs);
        }
        if (bookingsTab != null) {
            bookingsTab.setToggleGroup(sectionTabs);
        }
        if (bookTab != null) {
            bookTab.setSelected(true);
        }
        sectionTabs.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            showSection(newVal == bookTab);
        });
        showSection(true);
        priceSlider.valueProperty().addListener((obs, old, val) -> {
            priceLabel.setText(String.format("%.0f DT", val.doubleValue()));
            loadAvailableFlights();
        });
        searchField.textProperty().addListener((obs, old, val) -> loadAvailableFlights());
        loadAvailableFlights();
        loadMyBookings();
    }

    private void loadAvailableFlights() {
        flightGrid.getChildren().clear();
        List<Flight> flights = flightService.getAvailableFlights();

        String rawFilter = (searchField != null && searchField.getText() != null)
                ? searchField.getText().toLowerCase().trim()
                : "";
        double maxPrice = (priceSlider != null) ? priceSlider.getValue() : Double.MAX_VALUE;

        for (Flight f : flights) {
            boolean matches = rawFilter.isEmpty() ||
                    containsIgnoreCase(f.getDestination(), rawFilter) ||
                    containsIgnoreCase(f.getAirline(), rawFilter) ||
                    containsIgnoreCase(f.getFlightId(), rawFilter) ||
                    containsIgnoreCase(f.getDepartureAirport(), rawFilter);

            if (f.getPrix() <= maxPrice && matches) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/flight-card.fxml"));
                    VBox card = loader.load();
                    FlightCardController ctrl = loader.getController();
                    ctrl.setData(f, false, this::handleBook, null, null, null);
                    flightGrid.getChildren().add(card);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean containsIgnoreCase(String source, String filter) {
        return source != null && source.toLowerCase().contains(filter);
    }

    private void loadMyBookings() {
        bookingGrid.getChildren().clear();
        if (userId <= 0) {
            return;
        }
        List<Checkout> bookings = checkoutService.getAllCheckouts().stream()
                .filter(c -> c.getIdUser() == userId)
                .collect(Collectors.toList());
        for (Checkout c : bookings) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/checkout-card.fxml"));
                VBox card = loader.load();
                CheckoutCardController ctrl = loader.getController();
                ctrl.setData(c, false, null, null, this::handleCancel);
                bookingGrid.getChildren().add(card);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleBook(Flight f) {
        CheckoutController.setSelectedFlight(f);
        MainApp.switchScene("/views/checkout-view.fxml", "Confirm Booking");
    }

    private void handleCancel(Checkout c) {
        if (checkoutService.cancelCheckout(c.getCheckoutId(), userId)) {
            loadMyBookings();
            loadAvailableFlights();
        }
    }

    @FXML
    private void handleRefresh() {
        loadAvailableFlights();
        loadMyBookings();
    }

    @FXML
    private void handleAdminSwap() {
        org.example.mains.MainApp.switchScene("/org/example/AdminDashboardView.fxml", "Admin Dashboard");
    }

    @FXML
    private void handleThemeToggle() {
        org.example.mains.MainApp.toggleTheme();
    }

    @FXML
    private void handleBookMenu() {
        if (bookTab != null) {
            bookTab.setSelected(true);
        }
        showSection(true);
    }

    @FXML
    private void handleBookingsMenu() {
        if (bookingsTab != null) {
            bookingsTab.setSelected(true);
        }
        showSection(false);
    }

    @FXML
    private void handleHome() {
        org.example.mains.MainApp.switchScene("/UserHome.fxml", "Accueil");
    }

    @FXML
    private void handleLocations() {
        SceneNavigator.switchTo("/LocationListView.fxml", contentRoot);
    }

    @FXML
    private void handleFlights() {
        handleBookMenu();
    }

    @FXML
    private void handleActivities() {
        SceneNavigator.switchTo("/UserHome.fxml", contentRoot);
    }

    @FXML
    private void handleChambres() {
        org.example.mains.MainApp.switchScene("/views/room-booking.fxml", "Chambres Disponibles");
    }

    @FXML
    private void handleForums() {
        SceneNavigator.switchTo("/poste-forumviews/ListForum.fxml", contentRoot);
    }

    @FXML
    private void handleLogout() {
        SessionManager.clear();
        org.example.mains.MainApp.switchScene("/org/example/LoginView.fxml", "GoVibe Connexion");
    }

    // ==================== NAVIGATION METHODS ====================

    @FXML
    private void handleProfile() {
        showInfoAlert("Profil", "Fonctionnalité à venir : Gestion du profil utilisateur");
    }

    @FXML
    private void handleMyReservations() {
        org.example.mains.MainApp.switchScene("/views/room-booking.fxml", "Mes Réservations");
    }

    @FXML
    private void handleMyLocations() {
        SceneNavigator.switchTo("/LocationListView.fxml", contentRoot);
    }

    @FXML
    private void handleMyFlights() {
        handleBookingsMenu();
    }

    @FXML
    private void handleSettings() {
        showInfoAlert("Paramètres", "Fonctionnalité à venir : Paramètres utilisateur");
    }

    @FXML
    private void handleHelp() {
        showInfoAlert("Aide", "Besoin d'aide ? Contactez-nous à support@govibe.tn");
    }

    @FXML
    private void handleGoHome() {
        SceneNavigator.switchTo("/UserHome.fxml", contentRoot);
    }

    @FXML
    private void handleGoActivities() {
        SceneNavigator.switchTo("/UserHome.fxml", contentRoot);
    }

    @FXML
    private void handleGoLocations() {
        SceneNavigator.switchTo("/LocationListView.fxml", contentRoot);
    }

    @FXML
    private void handleGoHotels() {
        org.example.mains.MainApp.switchScene("/views/room-booking.fxml", "Hôtels");
    }

    @FXML
    private void handleGoForums() {
        SceneNavigator.switchTo("/poste-forumviews/ListForum.fxml", contentRoot);
    }

    @FXML
    private void handleResetFilters() {
        if (searchField != null) {
            searchField.clear();
        }
        if (priceSlider != null) {
            priceSlider.setValue(5000);
        }
        loadAvailableFlights();
    }

    private void showInfoAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSection(boolean showFlights) {
        if (flightsSection != null) {
            flightsSection.setVisible(showFlights);
            flightsSection.setManaged(showFlights);
        }
        if (bookingsSection != null) {
            bookingsSection.setVisible(!showFlights);
            bookingsSection.setManaged(!showFlights);
        }
        if (mainScroll != null) {
            mainScroll.setVvalue(0);
        }
    }
}
