package org.example.controllers;

import org.example.entities.Flight;
import org.example.services.FlightService;
import org.example.services.CheckoutService;
import org.example.entities.Checkout;
import org.example.mains.MainApp;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    @FXML
    private StackPane rootStackPane;
    @FXML
    private StackPane bookingOverlay;
    @FXML
    private VBox bookingFormContainer;

    private final FlightService flightService = new FlightService();
    private final CheckoutService checkoutService = new CheckoutService();
    private int userId = -1;
    private Flight currentBookingFlight;

    @FXML
    public void initialize() {
        System.out.println("[UserDashboard] initialize");
        if (SessionManager.getCurrentUser() == null) {
            System.out.println("[UserDashboard] User not authenticated, redirecting to login");
            Platform.runLater(() -> {
                org.example.mains.MainApp.switchScene("/org/example/LoginView.fxml", "GoVibe Connexion");
            });
            return;
        }
        userId = SessionManager.getCurrentUser().getId();
        if (userId <= 0) {
            System.out.println("[UserDashboard] Invalid user ID: " + userId);
            Platform.runLater(() -> {
                org.example.mains.MainApp.switchScene("/org/example/LoginView.fxml", "GoVibe Connexion");
            });
            return;
        }
        System.out.println("[UserDashboard] User ID: " + userId);
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
        currentBookingFlight = f;
        showBookingModal(f);
    }

    private void showBookingModal(Flight flight) {
        bookingFormContainer.getChildren().clear();

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setMaxHeight(720);

        VBox formContent = new VBox(20);
        formContent.setPadding(new Insets(30, 35, 30, 35));
        formContent.setStyle("-fx-background-color: white; -fx-background-radius: 20;");

        // === Header ===
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label("✈️ Réserver ce vol");
        titleLabel.setStyle("-fx-font-size: 24; -fx-font-weight: 700; -fx-text-fill: #013220;");
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: #F5F3E7; -fx-font-size: 18; -fx-text-fill: #666; -fx-background-radius: 50; -fx-min-width: 40; -fx-min-height: 40; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> hideBookingModal());
        header.getChildren().addAll(titleLabel, headerSpacer, closeBtn);

        // === Flight Summary Card ===
        VBox flightSummary = new VBox(8);
        flightSummary.setStyle("-fx-background-color: linear-gradient(to right, #D1F2EB, #F0FDF4); -fx-padding: 20; -fx-background-radius: 12;");
        Label routeLabel = new Label(flight.getDepartureAirport() + " → " + flight.getDestination());
        routeLabel.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #013220;");
        Label detailsLabel = new Label(flight.getAirline() + " • " + flight.getDepartureTime() + " - " + flight.getArrivalTime() + " • " + flight.getClasseChaise());
        detailsLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #5a7d6e;");
        Label pricePerTicket = new Label("💰 " + flight.getPrix() + " DT / personne");
        pricePerTicket.setStyle("-fx-font-size: 14; -fx-font-weight: 600; -fx-text-fill: #2E8B57;");
        flightSummary.getChildren().addAll(routeLabel, detailsLabel, pricePerTicket);

        // === Separator ===
        Separator sep1 = new Separator();
        sep1.setStyle("-fx-padding: 5 0;");

        // === Passenger Info ===
        Label passengerTitle = new Label("👤 Informations passager");
        passengerTitle.setStyle("-fx-font-size: 16; -fx-font-weight: 700; -fx-text-fill: #013220;");

        GridPane passengerGrid = new GridPane();
        passengerGrid.setHgap(15);
        passengerGrid.setVgap(12);
        ColumnConstraints col50 = new ColumnConstraints();
        col50.setPercentWidth(50);
        passengerGrid.getColumnConstraints().addAll(col50, new ColumnConstraints() {{ setPercentWidth(50); }});

        TextField nameField = new TextField();
        nameField.setPromptText("Nom complet");
        nameField.setStyle("-fx-background-color: #F5F3E7; -fx-background-radius: 10; -fx-padding: 12; -fx-font-size: 13;");

        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        emailField.setStyle("-fx-background-color: #F5F3E7; -fx-background-radius: 10; -fx-padding: 12; -fx-font-size: 13;");

        TextField phoneField = new TextField();
        phoneField.setPromptText("Téléphone");
        phoneField.setStyle("-fx-background-color: #F5F3E7; -fx-background-radius: 10; -fx-padding: 12; -fx-font-size: 13;");

        Spinner<Integer> passengerSpinner = new Spinner<>(1, 10, 1);
        passengerSpinner.setEditable(true);
        passengerSpinner.setStyle("-fx-background-color: #F5F3E7; -fx-background-radius: 10;");
        passengerSpinner.setPrefHeight(42);

        VBox nameBox = new VBox(4, new Label("NOM COMPLET *") {{ setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #8D99AE;"); }}, nameField);
        VBox emailBox = new VBox(4, new Label("EMAIL *") {{ setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #8D99AE;"); }}, emailField);
        VBox phoneBox = new VBox(4, new Label("TÉLÉPHONE *") {{ setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #8D99AE;"); }}, phoneField);
        VBox spinnerBox = new VBox(4, new Label("PASSAGERS") {{ setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #8D99AE;"); }}, passengerSpinner);

        passengerGrid.add(nameBox, 0, 0);
        passengerGrid.add(emailBox, 1, 0);
        passengerGrid.add(phoneBox, 0, 1);
        passengerGrid.add(spinnerBox, 1, 1);

        // === Travel Preferences ===
        Separator sep2 = new Separator();
        Label prefTitle = new Label("⚙️ Préférences de voyage");
        prefTitle.setStyle("-fx-font-size: 16; -fx-font-weight: 700; -fx-text-fill: #013220;");

        GridPane prefGrid = new GridPane();
        prefGrid.setHgap(15);
        prefGrid.setVgap(12);
        prefGrid.getColumnConstraints().addAll(new ColumnConstraints() {{ setPercentWidth(50); }}, new ColumnConstraints() {{ setPercentWidth(50); }});

        ComboBox<String> classCombo = new ComboBox<>();
        classCombo.getItems().addAll("Economy", "Business", "First Class");
        classCombo.setValue("Economy");
        classCombo.setMaxWidth(Double.MAX_VALUE);
        classCombo.setStyle("-fx-background-color: #F5F3E7; -fx-background-radius: 10;");

        ComboBox<String> paymentCombo = new ComboBox<>();
        paymentCombo.getItems().addAll("Credit Card", "PayPal", "Bank Transfer");
        paymentCombo.setValue("Credit Card");
        paymentCombo.setMaxWidth(Double.MAX_VALUE);
        paymentCombo.setStyle("-fx-background-color: #F5F3E7; -fx-background-radius: 10;");

        VBox classBox = new VBox(4, new Label("CLASSE") {{ setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #8D99AE;"); }}, classCombo);
        VBox paymentBox = new VBox(4, new Label("PAIEMENT") {{ setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #8D99AE;"); }}, paymentCombo);

        prefGrid.add(classBox, 0, 0);
        prefGrid.add(paymentBox, 1, 0);

        // === Price ===
        Label totalPriceLabel = new Label(flight.getPrix() + " DT");
        totalPriceLabel.setStyle("-fx-font-size: 28; -fx-font-weight: bold; -fx-text-fill: #50C878;");

        Runnable updatePrice = () -> {
            int base = flight.getPrix();
            int passengers = passengerSpinner.getValue();
            double mult = 1.0;
            if ("Business".equals(classCombo.getValue())) mult = 1.5;
            else if ("First Class".equals(classCombo.getValue())) mult = 2.0;
            int total = (int)(base * passengers * mult);
            totalPriceLabel.setText(total + " DT");
        };
        passengerSpinner.valueProperty().addListener((o, a, b) -> updatePrice.run());
        classCombo.valueProperty().addListener((o, a, b) -> updatePrice.run());

        HBox priceSection = new HBox();
        priceSection.setAlignment(Pos.CENTER);
        priceSection.setStyle("-fx-background-color: #F0FDF4; -fx-padding: 18; -fx-background-radius: 12;");
        VBox priceLeft = new VBox(2);
        priceLeft.getChildren().addAll(
            new Label("Total") {{ setStyle("-fx-font-size: 14; -fx-text-fill: #5a7d6e;"); }},
            new Label("Taxes et frais inclus") {{ setStyle("-fx-font-size: 11; -fx-text-fill: #8D99AE;"); }}
        );
        Region priceSpacer = new Region();
        HBox.setHgrow(priceSpacer, Priority.ALWAYS);
        priceSection.getChildren().addAll(priceLeft, priceSpacer, totalPriceLabel);

        // === Error label ===
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #EF476F; -fx-font-size: 12; -fx-font-weight: bold;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // === Buttons ===
        Button confirmBtn = new Button("✅ Confirmer la réservation");
        confirmBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #50C878, #3DAF62); -fx-text-fill: white; -fx-font-size: 15; -fx-font-weight: bold; -fx-padding: 14 40; -fx-background-radius: 25; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(80,200,120,0.4), 8, 0, 0, 3);");
        confirmBtn.setMaxWidth(Double.MAX_VALUE);
        confirmBtn.setOnAction(e -> {
            // Validate
            String name = nameField.getText();
            String email = emailField.getText();
            String phone = phoneField.getText();
            if (name == null || name.trim().isEmpty() || email == null || !email.contains("@") || phone == null || phone.trim().length() < 8) {
                errorLabel.setText("Veuillez remplir tous les champs correctement.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }
            confirmBtn.setDisable(true);
            confirmBtn.setText("⏳ Traitement en cours...");

            new Thread(() -> {
                try {
                    Thread.sleep(800);
                    
                    // Vérifier que l'utilisateur est connecté
                    if (userId <= 0) {
                        Platform.runLater(() -> {
                            errorLabel.setText("Erreur: Vous devez être connecté pour réserver.");
                            errorLabel.setVisible(true);
                            errorLabel.setManaged(true);
                            confirmBtn.setDisable(false);
                            confirmBtn.setText("✅ Confirmer la réservation");
                        });
                        return;
                    }
                    
                    Checkout checkout = new Checkout();
                    checkout.setFlightId(flight.getFlightId());
                    checkout.setIdUser(userId);
                    checkout.setReservationDate(LocalDateTime.now());
                    checkout.setPassengerNbr(passengerSpinner.getValue());
                    checkout.setStatusReservation("PENDING");
                    checkout.setPassengerName(name.trim());
                    checkout.setPassengerEmail(email.trim());
                    checkout.setPassengerPhone(phone.trim());
                    checkout.setTravelClass(classCombo.getValue());
                    checkout.setPaymentMethod(paymentCombo.getValue());
                    checkout.setSeatPreference("WINDOW");
                    String priceStr = totalPriceLabel.getText().replace(" DT", "");
                    checkout.setTotalPrix(new BigDecimal(priceStr));
                    boolean success = checkoutService.addCheckout(checkout);
                    Platform.runLater(() -> {
                        if (success) {
                            // Show success state
                            bookingFormContainer.getChildren().clear();
                            VBox successBox = new VBox(15);
                            successBox.setAlignment(Pos.CENTER);
                            successBox.setPadding(new Insets(60));
                            Label checkmark = new Label("✅");
                            checkmark.setStyle("-fx-font-size: 64;");
                            Label successMsg = new Label("Réservation confirmée !");
                            successMsg.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #013220;");
                            Label successDetail = new Label("Votre vol " + flight.getDepartureAirport() + " → " + flight.getDestination() + " a été réservé.");
                            successDetail.setStyle("-fx-font-size: 14; -fx-text-fill: #5a7d6e;");
                            successBox.getChildren().addAll(checkmark, successMsg, successDetail);
                            bookingFormContainer.getChildren().add(successBox);
                            // Auto-close after 2s
                            new Thread(() -> {
                                try { Thread.sleep(2000); } catch (Exception ex) {}
                                Platform.runLater(() -> {
                                    hideBookingModal();
                                    loadAvailableFlights();
                                    loadMyBookings();
                                });
                            }).start();
                        } else {
                            errorLabel.setText("Échec de la réservation. Veuillez réessayer.");
                            errorLabel.setVisible(true);
                            errorLabel.setManaged(true);
                            confirmBtn.setDisable(false);
                            confirmBtn.setText("✅ Confirmer la réservation");
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        errorLabel.setText("Erreur: " + ex.getMessage());
                        errorLabel.setVisible(true);
                        errorLabel.setManaged(true);
                        confirmBtn.setDisable(false);
                        confirmBtn.setText("✅ Confirmer la réservation");
                    });
                }
            }).start();
        });

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: #F5F3E7; -fx-text-fill: #666; -fx-font-size: 13; -fx-padding: 10 30; -fx-background-radius: 20; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> hideBookingModal());

        HBox buttonBar = new HBox(12);
        buttonBar.setAlignment(Pos.CENTER);
        buttonBar.getChildren().addAll(cancelBtn);

        VBox buttonSection = new VBox(10);
        buttonSection.setAlignment(Pos.CENTER);
        buttonSection.getChildren().addAll(errorLabel, confirmBtn, buttonBar);

        // === Assemble ===
        formContent.getChildren().addAll(
            header, flightSummary, sep1,
            passengerTitle, passengerGrid,
            sep2, prefTitle, prefGrid,
            priceSection, buttonSection
        );

        scrollPane.setContent(formContent);
        bookingFormContainer.getChildren().add(scrollPane);

        // Show overlay
        bookingOverlay.setVisible(true);
        bookingOverlay.setManaged(true);

        // Click on backdrop to close
        bookingOverlay.setOnMouseClicked(evt -> {
            if (evt.getTarget() == bookingOverlay) {
                hideBookingModal();
            }
        });
    }

    private void hideBookingModal() {
        bookingOverlay.setVisible(false);
        bookingOverlay.setManaged(false);
        currentBookingFlight = null;
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
        org.example.mains.MainApp.switchScene("/org/example/UserHomeView.fxml", "Accueil");
    }

    @FXML
    private void handleLocations() {
        org.example.mains.MainApp.switchScene("/LocationListView.fxml", "Voitures");
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
