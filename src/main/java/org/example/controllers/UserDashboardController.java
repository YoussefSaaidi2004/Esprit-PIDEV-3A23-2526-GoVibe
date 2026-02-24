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
import org.example.assistant.CommandRouter;
import org.example.assistant.VoiceAssistantService;
import org.example.services.AirSignatureService;
import org.example.services.GestureRecognitionService;
import org.example.services.WebcamManager;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import org.example.config.UnifiedDatabaseManager;
import org.example.dao.CheckoutDAO;
import org.example.services.PaymentCallbackServer;
import org.example.services.StripeService;

import javafx.scene.effect.GaussianBlur;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    @FXML
    private VBox emptyFlightsState;
    @FXML
    private VBox emptyBookingsState;
    @FXML
    private ImageView bgImageView;
    
    // Stats Labels
    @FXML private Label totalFlightsLabel, availableSeatsLabel, onTimeLabel;
    @FXML private Label destCountLabel, compCountLabel;
    
    // Filter Controls
    @FXML private ComboBox<String> destinationFilter, classFilter;
    @FXML private DatePicker departureDatePicker;

    private final FlightService flightService = new FlightService();
    private final CheckoutService checkoutService = new CheckoutService();
    private int userId = -1;
    private Flight currentBookingFlight;

    @FXML
    public void initialize() {
        System.out.println("[UserDashboard] initialize");
        ensureSchemaUpToDate();
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
        // Pre-warm the webcam immediately after login so it is ready
        // before the user reaches the gesture confirmation screen.
        WebcamManager.prewarm();
        // Initialise the offline voice assistant (Vosk STT + Windows SAPI TTS).
        initVoiceAssistant();
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
        setupHeroBackground();
        setupFilterListeners();
        loadFilters();
        updateFlightStats();
    }

    // ── Voice Assistant ───────────────────────────────────────────────────────
    /**
     * Initialises the offline voice assistant and wires it to this controller.
     *
     * <p>TTS (Windows SAPI via PowerShell) is always available.
     * STT (Vosk) requires a Vosk model at {@code ~/.govibe/vosk-model}.
     * See {@code src/main/resources/vosk-model/README.txt} for setup instructions.
     */
    private void initVoiceAssistant() {
        // The VoiceAssistantService is already started and listening from MainApp.
        // We only need to register this controller's proxy so voice commands
        // can drive the dashboard UI actions.
        VoiceAssistantService vas = MainApp.getVoiceAssistant();
        if (vas == null) return;  // assistant not initialised yet (should not happen)

        CommandRouter.ControllerProxy proxy = new CommandRouter.ControllerProxy() {

            @Override
            public void openBooking() {
                vas.speak("Hold on — pick a flight from the list first. I can't book thin air.");
            }

            /**
             * Called when Ollama extracted a destination (and optionally a date)
             * from the user's speech — pre-fills the search field so the user
             * gets instant results without typing.
             */
            @Override
            public void openBookingWith(String destination, String date) {
                javafx.application.Platform.runLater(() -> {
                    // Switch to the search/flights tab.
                    if (bookTab != null) {
                        bookTab.setSelected(true);
                        showSection(true);
                    }
                    // Pre-fill search field with the destination.
                    if (destination != null && !destination.isBlank() && searchField != null) {
                        searchField.setText(destination);
                        // loadAvailableFlights() is triggered automatically via the
                        // textProperty listener set in initialize().
                    }
                });
            }

            @Override
            public void cancelBooking() {
                hideBookingModal();
            }

            @Override
            public void showBookingsTab() {
                if (bookingsTab != null) {
                    bookingsTab.setSelected(true);
                    showSection(false);
                }
            }

            @Override
            public void showSearchTab() {
                if (bookTab != null) {
                    bookTab.setSelected(true);
                    showSection(true);
                }
            }

            @Override
            public void triggerPayment() {
                vas.speak("Almost there! Just confirm what's on screen and we'll handle the rest.");
            }

            @Override
            public String describeScreen() {
                boolean onFlights = flightsSection != null && flightsSection.isVisible();
                if (onFlights) {
                    int count = (flightGrid != null) ? flightGrid.getChildren().size() : 0;
                    String filter = (searchField != null && !searchField.getText().isBlank())
                            ? "filtered by \"" + searchField.getText().trim() + "\"" : "all destinations";
                    if (count == 0) {
                        return "You're on the GoVibe dashboard, flight search tab. "
                                + "No flights match your current filter. "
                                + "Try adjusting the search or price slider. "
                                + "Say My Bookings to see your confirmed bookings. "
                                + "Say Help for all commands.";
                    } else {
                        return "You're on the GoVibe dashboard, flight search tab. "
                                + "Showing " + count + " flight" + (count > 1 ? "s" : "") + ", " + filter + ". "
                                + "Say Book after selecting a flight to reserve your seat. "
                                + "Say My Bookings to view your reservations. "
                                + "Say Checkouts to view your payment history. "
                                + "Say Help for all commands.";
                    }
                } else {
                    int count = (bookingGrid != null) ? bookingGrid.getChildren().size() : 0;
                    if (count == 0) {
                        return "You're on the GoVibe dashboard, my bookings tab. "
                                + "You have no bookings yet — nothing booked, nothing lost. "
                                + "Say Search to browse available flights. "
                                + "Say Help for all commands.";
                    } else {
                        return "You're on the GoVibe dashboard, my bookings tab. "
                                + "You have " + count + " booking" + (count > 1 ? "s" : "") + " on record. "
                                + "Each card shows your flight details, status, and payment options. "
                                + "Say Search to browse more flights. "
                                + "Say Help for all commands.";
                    }
                }
            }
        };

        // Register proxy with the global router so commands reach this controller.
        MainApp.setVoiceProxy(proxy);

        // Add accessibility labels to key UI elements for screen-reader support.
        if (searchField != null) searchField.setAccessibleText("Champ de recherche de vols");
        if (bookTab     != null) bookTab.setAccessibleText("Onglet : Rechercher un vol");
        if (bookingsTab != null) bookingsTab.setAccessibleText("Onglet : Mes réservations");
        if (priceSlider != null) priceSlider.setAccessibleText("Filtre prix maximum");

        // Announce dashboard is ready (TTS only — STT already running globally).
        Thread t = new Thread(() -> {
            try { Thread.sleep(800); } catch (InterruptedException ignored) {}
            vas.speak("Dashboard loaded. Try not to break anything — say Help for commands.");
        }, "VoiceAssistant-DashboardAnnounce");
        t.setDaemon(true);
        t.start();
    }

    private void setupFilterListeners() {
        if (destinationFilter != null) destinationFilter.setOnAction(e -> loadAvailableFlights());
        if (classFilter != null) classFilter.setOnAction(e -> loadAvailableFlights());
        if (departureDatePicker != null) departureDatePicker.setOnAction(e -> loadAvailableFlights());
    }

    private void loadFilters() {
        List<Flight> flights = flightService.getAvailableFlights();
        Set<String> destinations = new HashSet<>();
        Set<String> classes = new HashSet<>();
        
        destinations.add("Toutes");
        classes.add("Toutes");
        
        for (Flight f : flights) {
            if (f.getDestination() != null) destinations.add(f.getDestination());
            if (f.getClasseChaise() != null) classes.add(f.getClasseChaise());
        }
        
        if (destinationFilter != null) {
            destinationFilter.getItems().setAll(destinations);
            destinationFilter.setValue("Toutes");
        }
        if (classFilter != null) {
            classFilter.getItems().setAll(classes);
            classFilter.setValue("Toutes");
        }
    }

    private void updateFlightStats() {
        List<Flight> flights = flightService.getAvailableFlights();
        int total = flights.size();
        int seats = flights.stream().mapToInt(Flight::getAvailableSeats).sum();
        long uniqueDests = flights.stream().map(Flight::getDestination).distinct().count();
        long uniqueComps = flights.stream().map(Flight::getAirline).distinct().count();

        Platform.runLater(() -> {
            if (totalFlightsLabel != null) totalFlightsLabel.setText(String.valueOf(total));
            if (availableSeatsLabel != null) availableSeatsLabel.setText(String.valueOf(seats));
            if (onTimeLabel != null) onTimeLabel.setText(String.valueOf(total)); // Assuming all on time for now
            if (destCountLabel != null) destCountLabel.setText(uniqueDests + "+");
            if (compCountLabel != null) compCountLabel.setText(String.valueOf(uniqueComps));
        });
    }

    private void loadAvailableFlights() {
        flightGrid.getChildren().clear();
        List<Flight> flights = flightService.getAvailableFlights();

        String rawFilter = (searchField != null && searchField.getText() != null)
                ? searchField.getText().toLowerCase().trim()
                : "";
        double maxPrice = (priceSlider != null) ? priceSlider.getValue() : Double.MAX_VALUE;
        String destSel = (destinationFilter != null) ? destinationFilter.getValue() : "Toutes";
        String classSel = (classFilter != null) ? classFilter.getValue() : "Toutes";

        for (Flight f : flights) {
            boolean matchesSearch = rawFilter.isEmpty() ||
                    containsIgnoreCase(f.getDestination(), rawFilter) ||
                    containsIgnoreCase(f.getAirline(), rawFilter) ||
                    containsIgnoreCase(f.getFlightId(), rawFilter) ||
                    containsIgnoreCase(f.getDepartureAirport(), rawFilter);
            
            boolean matchesDest = destSel == null || destSel.equals("Toutes") || f.getDestination().equals(destSel);
            boolean matchesClass = classSel == null || classSel.equals("Toutes") || f.getClasseChaise().equals(classSel);

            if (f.getPrix() <= maxPrice && matchesSearch && matchesDest && matchesClass) {
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
            if (emptyBookingsState != null) {
                emptyBookingsState.setVisible(true);
                emptyBookingsState.setManaged(true);
            }
            return;
        }
        List<Checkout> bookings = checkoutService.getAllCheckouts().stream()
                .filter(c -> c.getIdUser() == userId)
                .collect(Collectors.toList());

        if (bookings.isEmpty()) {
            if (emptyBookingsState != null) {
                emptyBookingsState.setVisible(true);
                emptyBookingsState.setManaged(true);
            }
        } else {
            if (emptyBookingsState != null) {
                emptyBookingsState.setVisible(false);
                emptyBookingsState.setManaged(false);
            }
            for (Checkout c : bookings) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/checkout-card.fxml"));
                    VBox card = loader.load();
                    CheckoutCardController ctrl = loader.getController();
                    ctrl.setData(c, false, null, null, this::handleCancel);
                    ctrl.setOnPaymentSuccess(() -> {
                        loadMyBookings();
                        loadAvailableFlights();
                    });
                    bookingGrid.getChildren().add(card);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleBook(Flight f) {
        currentBookingFlight = f;
        showBookingModal(f);
    }

    private void showBookingModal(Flight flight) {
        bookingFormContainer.getChildren().clear();

        // === Glassmorphism card ===
        StackPane glassCard = new StackPane();
        glassCard.setMaxWidth(660);
        glassCard.setMaxHeight(780);
        glassCard.setStyle(
            "-fx-background-color: linear-gradient(145deg,rgba(255,255,255,0.14),rgba(255,255,255,0.06));" +
            "-fx-background-radius: 28; -fx-border-color: rgba(255,255,255,0.28);" +
            "-fx-border-width: 1.5; -fx-border-radius: 28;" +
            "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.55),44,0,0,14);"
        );

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        scrollPane.setMaxHeight(775);

        VBox formContent = new VBox(22);
        formContent.setPadding(new Insets(32, 36, 32, 36));
        formContent.setStyle("-fx-background-color: transparent;");

        // === Header ===
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titleBlock = new VBox(2);
        Label titleLabel = new Label("Reserver ce vol");
        titleLabel.setStyle("-fx-font-size: 22; -fx-font-weight: 800; -fx-text-fill: #0d2b1a;" +
            "-fx-effect: dropshadow(gaussian,rgba(0,180,90,0.18),6,0,0,1);");
        Label subTitle = new Label(flight.getDepartureAirport() + "  ->  " + flight.getDestination());
        subTitle.setStyle("-fx-font-size: 13; -fx-text-fill: #2d7a50;");
        titleBlock.getChildren().addAll(titleLabel, subTitle);
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        Button closeBtn = new Button("x");
        closeBtn.setStyle(
            "-fx-background-color: rgba(0,0,0,0.08); -fx-text-fill: #1a1a1a; -fx-font-size: 15; -fx-font-weight: bold;" +
            "-fx-background-radius: 50; -fx-min-width: 36; -fx-min-height: 36; -fx-cursor: hand;" +
            "-fx-border-color: rgba(0,0,0,0.18); -fx-border-radius: 50; -fx-border-width: 1;"
        );
        closeBtn.setOnAction(e -> hideBookingModal());
        header.getChildren().addAll(titleBlock, headerSpacer, closeBtn);

        // === Flight Summary (glass) ===
        VBox flightSummary = new VBox(6);
        flightSummary.setStyle(
            "-fx-background-color: linear-gradient(135deg,rgba(0,200,120,0.2),rgba(0,150,80,0.1));" +
            "-fx-padding: 18 20; -fx-background-radius: 18;" +
            "-fx-border-color: rgba(0,255,140,0.28); -fx-border-width: 1; -fx-border-radius: 18;"
        );
        Label routeLabel = new Label(flight.getDepartureAirport() + "  ->  " + flight.getDestination());
        routeLabel.setStyle("-fx-font-size: 19; -fx-font-weight: 800; -fx-text-fill: #0d2b1a;");
        Label detailsLabel = new Label(flight.getAirline() + "  |  " + flight.getDepartureTime() + " - " + flight.getArrivalTime() + "  |  " + flight.getClasseChaise());
        detailsLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #2d6b47;");
        Label pricePerTicket = new Label(flight.getPrix() + " DT / personne");
        pricePerTicket.setStyle("-fx-font-size: 15; -fx-font-weight: 700; -fx-text-fill: #007a4d;");
        flightSummary.getChildren().addAll(routeLabel, detailsLabel, pricePerTicket);

        Separator sep1 = glassSeparator();
        Label passengerTitle = sectionLabel("Informations passager");

        // === Inputs ===
        GridPane passengerGrid = new GridPane();
        passengerGrid.setHgap(14); passengerGrid.setVgap(14);
        ColumnConstraints col50 = new ColumnConstraints(); col50.setPercentWidth(50);
        passengerGrid.getColumnConstraints().addAll(col50, new ColumnConstraints() {{ setPercentWidth(50); }});

        TextField nameField  = glassField("Nom complet");
        TextField emailField = glassField("Email");
        TextField phoneField = glassField("Telephone");
        Spinner<Integer> passengerSpinner = new Spinner<>(1, 10, 1);
        passengerSpinner.setEditable(true);
        passengerSpinner.setStyle(
            "-fx-background-color: rgba(255,255,255,0.10);" +
            "-fx-border-color: rgba(255,255,255,0.22); -fx-border-radius: 12; -fx-border-width: 1;" +
            "-fx-background-radius: 12;"
        );
        passengerSpinner.setPrefHeight(44);

        passengerGrid.add(fieldBox("NOM COMPLET", nameField),  0, 0);
        passengerGrid.add(fieldBox("EMAIL", emailField),        1, 0);
        passengerGrid.add(fieldBox("TELEPHONE", phoneField),    0, 1);
        passengerGrid.add(fieldBox("PASSAGERS", passengerSpinner), 1, 1);

        Separator sep2 = glassSeparator();
        Label prefTitle = sectionLabel("Preferences de voyage");

        GridPane prefGrid = new GridPane();
        prefGrid.setHgap(14); prefGrid.setVgap(12);
        prefGrid.getColumnConstraints().addAll(
            new ColumnConstraints() {{ setPercentWidth(50); }},
            new ColumnConstraints() {{ setPercentWidth(50); }}
        );
        ComboBox<String> classCombo   = glassCombo("Economy", "Economy", "Business", "First Class");
        ComboBox<String> paymentCombo = glassCombo("Credit Card", "Credit Card", "PayPal", "Bank Transfer");
        prefGrid.add(fieldBox("CLASSE", classCombo), 0, 0);
        prefGrid.add(fieldBox("MODE PAIEMENT", paymentCombo), 1, 0);

        // === Price ===
        Label totalPriceLabel = new Label(flight.getPrix() + " DT");
        totalPriceLabel.setStyle("-fx-font-size: 32; -fx-font-weight: 900; -fx-text-fill: #007a4d;" +
            "-fx-effect: dropshadow(gaussian,rgba(0,150,80,0.18),4,0,0,0);");

        Runnable updatePrice = () -> {
            int base = flight.getPrix(); int pax = passengerSpinner.getValue();
            double mul = "Business".equals(classCombo.getValue()) ? 1.5 :
                         "First Class".equals(classCombo.getValue()) ? 2.0 : 1.0;
            totalPriceLabel.setText((int)(base * pax * mul) + " DT");
        };
        passengerSpinner.valueProperty().addListener((o, a, b) -> updatePrice.run());
        classCombo.valueProperty().addListener((o, a, b) -> updatePrice.run());

        HBox priceSection = new HBox();
        priceSection.setAlignment(Pos.CENTER);
        priceSection.setPadding(new Insets(16, 20, 16, 20));
        priceSection.setStyle(
            "-fx-background-color: rgba(0,255,140,0.07);" +
            "-fx-background-radius: 16; -fx-border-color: rgba(0,255,140,0.22);" +
            "-fx-border-width: 1; -fx-border-radius: 16;"
        );
        VBox priceLeft = new VBox(3,
            glassLabel("Total", "-fx-font-size: 13; -fx-text-fill: #1a4a2e; -fx-font-weight: 700;"),
            glassLabel("Taxes et frais inclus", "-fx-font-size: 11; -fx-text-fill: #4a7a5e;")
        );
        Region priceSpacer = new Region(); HBox.setHgrow(priceSpacer, Priority.ALWAYS);
        priceSection.getChildren().addAll(priceLeft, priceSpacer, totalPriceLabel);

        // === Error ===
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ff6b8a; -fx-font-size: 12; -fx-font-weight: bold;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // === Pay Now button ===
        Button confirmBtn = new Button("Payer Maintenant");
        confirmBtn.setMaxWidth(Double.MAX_VALUE);
        confirmBtn.setStyle(
            "-fx-background-color: linear-gradient(to right,#00c875,#00e5a0);" +
            "-fx-text-fill: #002b1a; -fx-font-size: 16; -fx-font-weight: 900;" +
            "-fx-padding: 16 40; -fx-background-radius: 30; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian,rgba(0,220,130,0.7),22,0,0,6);"
        );
        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle(
            "-fx-background-color: rgba(0,0,0,0.06); -fx-text-fill: #555555;" +
            "-fx-font-size: 13; -fx-padding: 10 30; -fx-background-radius: 20; -fx-cursor: hand;" +
            "-fx-border-color: rgba(0,0,0,0.15); -fx-border-radius: 20; -fx-border-width: 1;"
        );
        cancelBtn.setOnAction(e -> hideBookingModal());

        VBox buttonSection = new VBox(10);
        buttonSection.setAlignment(Pos.CENTER);
        buttonSection.getChildren().addAll(errorLabel, confirmBtn, cancelBtn);

        // === Assemble ===
        formContent.getChildren().addAll(
            header, flightSummary, sep1,
            passengerTitle, passengerGrid,
            sep2, prefTitle, prefGrid,
            priceSection, buttonSection
        );
        scrollPane.setContent(formContent);
        glassCard.getChildren().add(scrollPane);
        bookingFormContainer.getChildren().add(glassCard);

        // Show overlay
        bookingOverlay.setVisible(true);
        bookingOverlay.setManaged(true);
        if (contentRoot != null) contentRoot.setEffect(new GaussianBlur(18));
        bookingOverlay.setOnMouseClicked(evt -> {
            if (evt.getTarget() == bookingOverlay) hideBookingModal();
        });

        // === Button logic ===
        confirmBtn.setOnAction(e -> {
            String name  = nameField.getText();
            String email = emailField.getText();
            String phone = phoneField.getText();
            if (name == null || name.trim().isEmpty()
                    || email == null || !email.contains("@")
                    || phone == null || phone.trim().length() < 8) {
                errorLabel.setText("Veuillez remplir tous les champs correctement.");
                errorLabel.setVisible(true); errorLabel.setManaged(true);
                return;
            }
            if (userId <= 0) {
                errorLabel.setText("Vous devez etre connecte pour reserver.");
                errorLabel.setVisible(true); errorLabel.setManaged(true);
                return;
            }
            confirmBtn.setDisable(true);
            confirmBtn.setText("Preparation du paiement...");
            String priceStr = totalPriceLabel.getText().replace(" DT", "").trim();

            new Thread(() -> {
                try {
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
                    checkout.setTotalPrix(new BigDecimal(priceStr));

                    boolean saved = checkoutService.addCheckout(checkout);
                    if (!saved) {
                        Platform.runLater(() -> {
                            errorLabel.setText("Echec de la sauvegarde. Reessayez.");
                            errorLabel.setVisible(true); errorLabel.setManaged(true);
                            confirmBtn.setDisable(false); confirmBtn.setText("Payer Maintenant");
                        });
                        return;
                    }

                    PaymentCallbackServer server = new PaymentCallbackServer();
                    server.start();
                    int port = server.getPort();
                    String successUrl = "http://localhost:" + port + "/success";
                    String cancelUrl  = "http://localhost:" + port + "/cancel";

                    BigDecimal amount = checkout.getTotalPrix();
                    if (amount == null || amount.compareTo(new BigDecimal("0.50")) < 0)
                        amount = new BigDecimal("10.00");

                    String stripeUrl;
                    try {
                        stripeUrl = StripeService.getInstance().createCheckoutSession(
                            checkout.getCheckoutId(), amount,
                            "GoVibe - " + flight.getDepartureAirport() + " -> " + flight.getDestination(),
                            successUrl, cancelUrl
                        );
                    } catch (Exception stripeEx) {
                        System.err.println("[Stripe] " + stripeEx.getMessage());
                        new CheckoutDAO().updateStatus(checkout.getCheckoutId(), "CONFIRMED");
                        server.stop();
                        Platform.runLater(() -> showGestureConfirmation(flight, checkout.getCheckoutId()));
                        return;
                    }

                    final String fSuccess = successUrl;
                    final String fCancel  = cancelUrl;
                    final PaymentCallbackServer fServer  = server;
                    final Checkout fCheckout = checkout;

                    Platform.runLater(() -> {
                        bookingFormContainer.getChildren().clear();

                        StackPane payCard = new StackPane();
                        payCard.setMaxWidth(660);
                        payCard.setStyle(
                            "-fx-background-color: linear-gradient(145deg,rgba(0,18,12,0.94),rgba(0,38,24,0.90));" +
                            "-fx-background-radius: 28; -fx-border-color: rgba(0,255,140,0.28);" +
                            "-fx-border-width: 1.5; -fx-border-radius: 28;" +
                            "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.65),44,0,0,14);"
                        );
                        VBox payContent = new VBox(0);

                        HBox payHeader = new HBox(10);
                        payHeader.setAlignment(Pos.CENTER_LEFT);
                        payHeader.setPadding(new Insets(16, 22, 16, 22));
                        payHeader.setStyle(
                            "-fx-background-color: linear-gradient(to right,rgba(0,200,120,0.28),rgba(0,80,40,0.15));" +
                            "-fx-background-radius: 26 26 0 0;"
                        );
                        Label payTitle = new Label("Paiement securise - Stripe");
                        payTitle.setStyle("-fx-font-size: 15; -fx-font-weight: 800; -fx-text-fill: white;");
                        Region ph = new Region(); HBox.setHgrow(ph, Priority.ALWAYS);
                        Button cancelPay = new Button("Annuler");
                        cancelPay.setStyle(
                            "-fx-background-color: rgba(255,80,80,0.18); -fx-text-fill: #ff8888;" +
                            "-fx-font-size: 12; -fx-background-radius: 20; -fx-cursor: hand;" +
                            "-fx-border-color: rgba(255,80,80,0.35); -fx-border-radius: 20; -fx-border-width: 1; -fx-padding: 6 16;"
                        );
                        cancelPay.setOnAction(ev -> { fServer.stop(); hideBookingModal(); });
                        payHeader.getChildren().addAll(payTitle, ph, cancelPay);

                        WebView webView = new WebView();
                        webView.setPrefHeight(600);
                        webView.getEngine().load(stripeUrl);
                        webView.getEngine().locationProperty().addListener((obs, old, newLoc) -> {
                            if (newLoc == null) return;
                            if (newLoc.startsWith(fSuccess)) {
                                webView.getEngine().load("about:blank");
                                new Thread(() -> {
                                    new CheckoutDAO().updateStatus(fCheckout.getCheckoutId(), "CONFIRMED");
                                    fServer.stop();
                                    Platform.runLater(() -> showGestureConfirmation(flight, fCheckout.getCheckoutId()));
                                }).start();
                            } else if (newLoc.startsWith(fCancel)) {
                                fServer.stop();
                                Platform.runLater(() -> { hideBookingModal(); showBookingModal(flight); });
                            }
                        });

                        payContent.getChildren().addAll(payHeader, webView);
                        payCard.getChildren().add(payContent);
                        bookingFormContainer.getChildren().add(payCard);
                    });

                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        errorLabel.setText("Erreur: " + ex.getMessage());
                        errorLabel.setVisible(true); errorLabel.setManaged(true);
                        confirmBtn.setDisable(false); confirmBtn.setText("Payer Maintenant");
                    });
                }
            }).start();
        });
    }

    // helper methods for glassmorphism booking form
    private TextField glassField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle(
            "-fx-background-color: rgba(255,255,255,0.75);" +
            "-fx-border-color: rgba(0,0,0,0.15); -fx-border-radius: 12; -fx-border-width: 1;" +
            "-fx-background-radius: 12; -fx-text-fill: #1a1a1a;" +
            "-fx-prompt-text-fill: #999999; -fx-padding: 12 14; -fx-font-size: 13;"
        );
        return tf;
    }

    @SuppressWarnings("unchecked")
    private ComboBox<String> glassCombo(String def, String... items) {
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().addAll(items); cb.setValue(def); cb.setMaxWidth(Double.MAX_VALUE);
        cb.setStyle(
            "-fx-background-color: rgba(255,255,255,0.75);" +
            "-fx-border-color: rgba(0,0,0,0.15); -fx-border-radius: 12; -fx-border-width: 1;" +
            "-fx-background-radius: 12; -fx-text-fill: #1a1a1a;"
        );
        return cb;
    }

    private VBox fieldBox(String labelText, javafx.scene.Node field) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 10; -fx-font-weight: 800; -fx-text-fill: #2d7a50; -fx-letter-spacing: 1;");
        return new VBox(5, lbl, field);
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 14; -fx-font-weight: 800; -fx-text-fill: #0d2b1a; -fx-padding: 4 0 0 0;");
        return l;
    }

    private Label glassLabel(String text, String style) {
        Label l = new Label(text); l.setStyle(style); return l;
    }

    private Separator glassSeparator() {
        Separator s = new Separator();
        s.setStyle("-fx-background-color: rgba(0,0,0,0.12); -fx-padding: 2 0;");
        return s;
    }
    private void hideBookingModal() {
        bookingOverlay.setVisible(false);
        bookingOverlay.setManaged(false);
        // Restore container defaults so re-opening the booking form works correctly
        bookingFormContainer.setStyle(
            "-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 0;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 30, 0, 0, 10);"
        );
        bookingFormContainer.setMaxWidth(650);
        bookingFormContainer.setMaxHeight(750);
        if (contentRoot != null) {
            contentRoot.setEffect(null);
        }
        currentBookingFlight = null;
    }

    // ── Gesture Confirmation (AI — Vision par ordinateur) ──────────────────────
    private void showGestureConfirmation(Flight flight, int checkoutId) {
        // Ensure the overlay is fully visible (may be called from multiple paths)
        bookingOverlay.setVisible(true);
        bookingOverlay.setManaged(true);
        // Disable outside-click dismissal while camera is active
        bookingOverlay.setOnMouseClicked(null);

        // Expand the container: remove white card background and max-size constraints
        // so the dark gesture card can fill the overlay properly
        bookingFormContainer.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        bookingFormContainer.setMaxWidth(Double.MAX_VALUE);
        bookingFormContainer.setMaxHeight(Double.MAX_VALUE);

        bookingFormContainer.getChildren().clear();

        VBox gestureCard = new VBox(0);
        gestureCard.setMaxWidth(700);
        gestureCard.setStyle(
            "-fx-background-color: #0d1f1a;" +
            "-fx-background-radius: 28; -fx-border-color: rgba(0,220,140,0.32);" +
            "-fx-border-width: 2; -fx-border-radius: 28;" +
            "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.65),44,0,0,14);"
        );

        // ── Header ─────────────────────────────────────────────────────────────
        VBox headerBox = new VBox(6);
        headerBox.setPadding(new Insets(24, 32, 18, 32));
        headerBox.setStyle(
            "-fx-background-color: linear-gradient(135deg,#001a0f,#002e1b);" +
            "-fx-background-radius: 26 26 0 0;"
        );
        Label aiTag = new Label("IA \u2014 Vision par ordinateur  \u00b7  Reconnaissance gestuelle");
        aiTag.setStyle(
            "-fx-font-size: 10; -fx-font-weight: 800; -fx-text-fill: #00e0a0;" +
            "-fx-background-color: rgba(0,224,160,0.12); -fx-padding: 4 12;" +
            "-fx-background-radius: 10;"
        );
        Label gTitle = new Label("Confirmation par geste");
        gTitle.setStyle("-fx-font-size: 22; -fx-font-weight: 900; -fx-text-fill: white;");
        Label gSubtitle = new Label(
            "Maintenez un geste valide pendant 1.5 secondes pour confirmer votre reservation"
        );
        gSubtitle.setStyle("-fx-font-size: 12; -fx-text-fill: #88a898;");
        Label gFlightInfo = new Label(
            flight.getDepartureAirport() + "  \u2192  " + flight.getDestination()
            + "   |   Reservation #" + checkoutId
        );
        gFlightInfo.setStyle("-fx-font-size: 11; -fx-text-fill: #00e0a0; -fx-font-weight: 700;");
        headerBox.getChildren().addAll(aiTag, gTitle, gSubtitle, gFlightInfo);

        // ── Camera pane ─────────────────────────────────────────────────────────
        StackPane camPane = new StackPane();
        camPane.setStyle("-fx-background-color: #050f0a;");
        camPane.setPrefSize(700, 394);

        ImageView camView = new ImageView();
        camView.setFitWidth(700);
        camView.setFitHeight(394);
        camView.setPreserveRatio(false);

        Canvas overlayCanvas = new Canvas(700, 394);
        GraphicsContext oc = overlayCanvas.getGraphicsContext2D();
        overlayCanvas.setMouseTransparent(true);

        Label camLoading = new Label("Demarrage de la camera IA...");
        camLoading.setStyle("-fx-text-fill: #88a898; -fx-font-size: 14;");

        camPane.getChildren().addAll(camView, overlayCanvas, camLoading);

        // ── Gesture hint pills ───────────────────────────────────────────────────
        HBox hintsRow = new HBox(14);
        hintsRow.setPadding(new Insets(14, 30, 4, 30));
        hintsRow.setAlignment(Pos.CENTER_LEFT);

        Label hintsLabel = new Label("Gestes valides :");
        hintsLabel.setStyle("-fx-font-size: 12; -fx-font-weight: 800; -fx-text-fill: #88a898;");

        String HINT_IDLE =
            "-fx-background-color: rgba(255,255,255,0.07); -fx-text-fill: #aaa;" +
            "-fx-font-size: 12; -fx-padding: 6 16; -fx-background-radius: 20;" +
            "-fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 20; -fx-border-width: 1;";
        String HINT_ACTIVE =
            "-fx-background-color: rgba(0,220,120,0.22); -fx-text-fill: #00e080;" +
            "-fx-font-size: 12; -fx-font-weight: 900; -fx-padding: 6 16;" +
            "-fx-background-radius: 20; -fx-border-color: #00e080;" +
            "-fx-border-radius: 20; -fx-border-width: 1.5;";

        Label thumbHint = new Label("Pouce leve  (Thumbs Up)");
        Label palmHint  = new Label("Paume ouverte  (Open Palm)");
        thumbHint.setStyle(HINT_IDLE);
        palmHint.setStyle(HINT_IDLE);
        hintsRow.getChildren().addAll(hintsLabel, thumbHint, palmHint);

        // ── Bottom status row ───────────────────────────────────────────────────
        HBox bottomRow = new HBox(12);
        bottomRow.setPadding(new Insets(10, 30, 24, 30));
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        Label currentGestureLabel = new Label("En attente d'un geste...");
        currentGestureLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #88a898; -fx-font-weight: 700;");

        Region gSpacer = new Region();
        HBox.setHgrow(gSpacer, Priority.ALWAYS);

        Button gCancelBtn = new Button("Annuler");
        gCancelBtn.setStyle(
            "-fx-background-color: rgba(255,80,80,0.15); -fx-text-fill: #ff8888;" +
            "-fx-font-size: 12; -fx-padding: 8 22; -fx-background-radius: 20; -fx-cursor: hand;" +
            "-fx-border-color: rgba(255,80,80,0.35); -fx-border-radius: 20; -fx-border-width: 1;"
        );
        bottomRow.getChildren().addAll(currentGestureLabel, gSpacer, gCancelBtn);

        // ── AI service ──────────────────────────────────────────────────────────
        GestureRecognitionService[] gsvc = { new GestureRecognitionService() };
        long[]    firstGestureMs = { -1L };
        boolean[] gConfirmed     = { false };

        // Progress arc position (bottom-right of camera pane)
        final double arcX = 658, arcY = 358, arcR = 36;

        gCancelBtn.setOnAction(ev -> {
            gsvc[0].stop();
            hideBookingModal();
        });

        gsvc[0].start(
            // Frame update
            fxImg -> {
                camView.setImage(fxImg);
                camLoading.setVisible(false);
            },
            // Gesture classification (on FX thread)
            gesture -> {
                if (gConfirmed[0]) return;

                boolean isValid =
                    gesture == GestureRecognitionService.Gesture.THUMBS_UP
                    || gesture == GestureRecognitionService.Gesture.OPEN_PALM;

                // Update hint pills
                thumbHint.setStyle(gesture == GestureRecognitionService.Gesture.THUMBS_UP
                    ? HINT_ACTIVE : HINT_IDLE);
                palmHint.setStyle(gesture == GestureRecognitionService.Gesture.OPEN_PALM
                    ? HINT_ACTIVE : HINT_IDLE);

                // Gesture label + color
                String gNameStr;
                String gColorStr;
                if (gesture == GestureRecognitionService.Gesture.THUMBS_UP) {
                    gNameStr = "Pouce leve (Thumbs Up)";    gColorStr = "#00e080";
                } else if (gesture == GestureRecognitionService.Gesture.OPEN_PALM) {
                    gNameStr = "Paume ouverte (Open Palm)"; gColorStr = "#00e080";
                } else if (gesture == GestureRecognitionService.Gesture.POINTING) {
                    gNameStr = "Index pointe (Pointing)";   gColorStr = "#60c0ff";
                } else if (gesture == GestureRecognitionService.Gesture.FIST) {
                    gNameStr = "Poing (Fist)";              gColorStr = "#ffaa44";
                } else {
                    gNameStr = "En attente...";             gColorStr = "#88a898";
                }

                currentGestureLabel.setText(
                    isValid ? "Geste detecte : " + gNameStr + "  — Maintenez !" : gNameStr
                );
                currentGestureLabel.setStyle(
                    "-fx-font-size: 13; -fx-font-weight: 800; -fx-text-fill: " + gColorStr + ";"
                );
                oc.clearRect(0, 0, 700, 394);

                if (isValid) {
                    if (firstGestureMs[0] < 0) firstGestureMs[0] = System.currentTimeMillis();
                    double progress = Math.min(1.0,
                        (System.currentTimeMillis() - firstGestureMs[0]) / 1500.0);

                    // Gesture name badge (top-left)
                    oc.setFill(javafx.scene.paint.Color.rgb(0, 30, 18, 0.82));
                    oc.fillRoundRect(14, 14, 270, 38, 14, 14);
                    oc.setStroke(javafx.scene.paint.Color.rgb(0, 220, 120, 0.7));
                    oc.setLineWidth(1.5);
                    oc.strokeRoundRect(14, 14, 270, 38, 14, 14);
                    oc.setFill(javafx.scene.paint.Color.rgb(0, 224, 128));
                    oc.setFont(javafx.scene.text.Font.font(
                        "System", javafx.scene.text.FontWeight.BOLD, 13));
                    oc.fillText(gNameStr, 26, 38);

                    // Progress circle track (bottom-right)
                    oc.setStroke(javafx.scene.paint.Color.rgb(255, 255, 255, 0.15));
                    oc.setLineWidth(7);
                    oc.strokeOval(arcX - arcR, arcY - arcR, arcR * 2, arcR * 2);

                    // Progress arc (clockwise from 12 o'clock)
                    oc.setStroke(javafx.scene.paint.Color.rgb(0, 220, 120, 0.95));
                    oc.setLineWidth(7);
                    oc.strokeArc(
                        arcX - arcR, arcY - arcR, arcR * 2, arcR * 2,
                        90, -360.0 * progress,
                        javafx.scene.shape.ArcType.OPEN
                    );

                    // Percentage in arc center
                    String pct = (int)(progress * 100) + "%";
                    oc.setFill(javafx.scene.paint.Color.WHITE);
                    oc.setFont(javafx.scene.text.Font.font(
                        "System", javafx.scene.text.FontWeight.BOLD, 13));
                    oc.fillText(pct, arcX - (pct.length() > 2 ? 16 : 12), arcY + 5);

                    if (progress >= 1.0) {
                        gConfirmed[0] = true;
                        gsvc[0].stop();
                        showSignaturePad(flight, checkoutId);
                    }

                } else {
                    firstGestureMs[0] = -1L;
                    // Non-confirming gesture badge (orange)
                    if (gesture != GestureRecognitionService.Gesture.NONE) {
                        oc.setFill(javafx.scene.paint.Color.rgb(30, 18, 0, 0.78));
                        oc.fillRoundRect(14, 14, 250, 38, 14, 14);
                        oc.setStroke(javafx.scene.paint.Color.rgb(255, 160, 50, 0.65));
                        oc.setLineWidth(1.5);
                        oc.strokeRoundRect(14, 14, 250, 38, 14, 14);
                        oc.setFill(javafx.scene.paint.Color.rgb(255, 175, 70));
                        oc.setFont(javafx.scene.text.Font.font(
                            "System", javafx.scene.text.FontWeight.BOLD, 13));
                        oc.fillText(gNameStr, 26, 38);
                    }
                }
            }
        );

        gestureCard.getChildren().addAll(headerBox, camPane, hintsRow, bottomRow);
        bookingFormContainer.getChildren().add(gestureCard);
    }

    // ── Signature Pad (mouse + camera air-sign) ─────────────────────────────
    private void showSignaturePad(Flight flight, int checkoutId) {
        // Restore container to white card style (used by signature pad and booking form)
        bookingFormContainer.setStyle(
            "-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 0;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 30, 0, 0, 10);"
        );
        bookingFormContainer.setMaxWidth(650);
        bookingFormContainer.setMaxHeight(750);
        bookingFormContainer.getChildren().clear();

        VBox sigCard = new VBox(0);
        sigCard.setMaxWidth(660);
        sigCard.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 28; -fx-border-color: rgba(0,180,100,0.35);" +
            "-fx-border-width: 2; -fx-border-radius: 28;" +
            "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.28),32,0,0,10);"
        );

        // ─ Header ─
        VBox headerBox = new VBox(4);
        headerBox.setPadding(new Insets(24, 32, 16, 32));
        headerBox.setStyle("-fx-background-color: linear-gradient(to right,#00c875,#00e5a0); -fx-background-radius: 26 26 0 0;");
        Label sigTitle = new Label("Signature electronique");
        sigTitle.setStyle("-fx-font-size: 20; -fx-font-weight: 900; -fx-text-fill: #002b1a;");
        Label sigSub = new Label("Signez pour confirmer definitivement votre reservation");
        sigSub.setStyle("-fx-font-size: 12; -fx-text-fill: #004d2e;");
        Label flightInfo = new Label(flight.getDepartureAirport() + "  ->  " + flight.getDestination() + "   |   Reservation #" + checkoutId);
        flightInfo.setStyle("-fx-font-size: 11; -fx-text-fill: #006640; -fx-font-weight: 700;");
        headerBox.getChildren().addAll(sigTitle, sigSub, flightInfo);

        // ─ Mode toggle ─
        HBox modeRow = new HBox(10);
        modeRow.setPadding(new Insets(16, 32, 8, 32));
        modeRow.setAlignment(Pos.CENTER_LEFT);
        Label modeLabel = new Label("Mode :");
        modeLabel.setStyle("-fx-font-size: 12; -fx-font-weight: 800; -fx-text-fill: #2d7a50;");

        String ACTIVE_STYLE =
            "-fx-background-color: linear-gradient(to right,#00c875,#00e5a0);" +
            "-fx-text-fill: #002b1a; -fx-font-size: 12; -fx-font-weight: 900;" +
            "-fx-padding: 8 20; -fx-background-radius: 20; -fx-cursor: hand;";
        String IDLE_STYLE =
            "-fx-background-color: rgba(0,0,0,0.06); -fx-text-fill: #555;" +
            "-fx-font-size: 12; -fx-padding: 8 20; -fx-background-radius: 20; -fx-cursor: hand;" +
            "-fx-border-color: rgba(0,0,0,0.14); -fx-border-radius: 20; -fx-border-width: 1;";

        Button mouseBtn  = new Button("Souris");
        Button cameraBtn = new Button("Camera (Air Sign)");
        mouseBtn.setStyle(ACTIVE_STYLE);
        cameraBtn.setStyle(IDLE_STYLE);
        modeRow.getChildren().addAll(modeLabel, mouseBtn, cameraBtn);

        // ─ Shared signature canvas ─
        Label canvasLabel = new Label("Votre signature");
        canvasLabel.setStyle("-fx-font-size: 11; -fx-font-weight: 800; -fx-text-fill: #2d7a50; -fx-padding: 0 0 6 0;");

        StackPane sigCanvasWrap = new StackPane();
        sigCanvasWrap.setStyle(
            "-fx-background-color: #f9fffe;" +
            "-fx-border-color: #b2dfc8; -fx-border-width: 2; -fx-border-radius: 14;" +
            "-fx-background-radius: 14;"
        );
        Canvas sigCanvas = new Canvas(590, 180);
        GraphicsContext sgc = sigCanvas.getGraphicsContext2D();
        Runnable resetSigCanvas = () -> {
            sgc.setFill(javafx.scene.paint.Color.rgb(249, 255, 254));
            sgc.fillRect(0, 0, sigCanvas.getWidth(), sigCanvas.getHeight());
            sgc.setStroke(javafx.scene.paint.Color.rgb(180, 220, 200, 0.55));
            sgc.setLineWidth(1);
            sgc.strokeLine(30, 140, sigCanvas.getWidth() - 30, 140);
            sgc.setFill(javafx.scene.paint.Color.rgb(150, 190, 170, 0.65));
            sgc.setFont(javafx.scene.text.Font.font(11));
            sgc.fillText("Signez ici", 30, 155);
            sgc.setStroke(javafx.scene.paint.Color.rgb(0, 60, 30));
            sgc.setLineWidth(2.5);
            sgc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            sgc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        };
        resetSigCanvas.run();
        sigCanvasWrap.getChildren().add(sigCanvas);

        VBox sigRow = new VBox(6);
        sigRow.setPadding(new Insets(12, 32, 0, 32));
        sigRow.getChildren().addAll(canvasLabel, sigCanvasWrap);

        final boolean[] hasSigned = {false};

        // ─ Mouse drawing handlers ─
        sigCanvas.setOnMousePressed(ev -> {
            sgc.beginPath(); sgc.moveTo(ev.getX(), ev.getY());
        });
        sigCanvas.setOnMouseDragged(ev -> {
            sgc.lineTo(ev.getX(), ev.getY()); sgc.stroke();
            hasSigned[0] = true;
        });

        // ─ Camera section (hidden by default) ─
        VBox cameraSection = new VBox(8);
        cameraSection.setPadding(new Insets(12, 32, 0, 32));
        cameraSection.setVisible(false);
        cameraSection.setManaged(false);

        Label camHint = new Label("Levez le doigt devant la camera pour dessiner");
        camHint.setStyle("-fx-font-size: 11; -fx-text-fill: #555; -fx-padding: 0 0 4 0;");

        // Camera feed with fingertip dot overlay
        StackPane camPane = new StackPane();
        camPane.setStyle("-fx-background-color: #111; -fx-background-radius: 14;");
        camPane.setPrefSize(590, 320);
        camPane.setMaxSize(590, 320);

        ImageView camView = new ImageView();
        camView.setFitWidth(590); camView.setFitHeight(320);
        camView.setPreserveRatio(true);

        // Fingertip dot on camera view
        Canvas dotCanvas = new Canvas(590, 320);
        GraphicsContext dc = dotCanvas.getGraphicsContext2D();

        Label camStatus = new Label("Demarrage de la camera...");
        camStatus.setStyle("-fx-font-size: 12; -fx-text-fill: #aaa;");
        camPane.getChildren().addAll(camView, dotCanvas, camStatus);

        cameraSection.getChildren().addAll(camHint, camPane);

        // AirSignatureService instance kept per-session
        final AirSignatureService[] airSvc = {null};
        final double[] lastTipXY = {-1, -1};
        final boolean[] penDown = {false};

        // ─ Switch MOUSE mode ─
        mouseBtn.setOnAction(ev -> {
            mouseBtn.setStyle(ACTIVE_STYLE);
            cameraBtn.setStyle(IDLE_STYLE);
            cameraSection.setVisible(false);
            cameraSection.setManaged(false);
            sigCanvas.setMouseTransparent(false);
            if (airSvc[0] != null) { airSvc[0].stop(); airSvc[0] = null; }
        });

        // ─ Switch CAMERA mode ─
        cameraBtn.setOnAction(ev -> {
            cameraBtn.setStyle(ACTIVE_STYLE);
            mouseBtn.setStyle(IDLE_STYLE);
            cameraSection.setVisible(true);
            cameraSection.setManaged(true);
            sigCanvas.setMouseTransparent(true); // camera controls drawing
            camStatus.setVisible(true);

            AirSignatureService svc = new AirSignatureService();
            airSvc[0] = svc;

            svc.start(
                // Frame callback
                fxImg -> {
                    camView.setImage(fxImg);
                    camStatus.setVisible(false);
                },
                // Fingertip callback (normalised coords, -1 = not found, -2 = no camera)
                (nx, ny) -> {
                    dc.clearRect(0, 0, dotCanvas.getWidth(), dotCanvas.getHeight());

                    if (nx == -2.0) {
                        // No camera found
                        camStatus.setText("Aucune camera detectee");
                        camStatus.setVisible(true);
                        return;
                    }

                    if (nx < 0 || ny < 0) {
                        // No fingertip — pen up
                        penDown[0] = false;
                        lastTipXY[0] = -1;
                        return;
                    }

                    // Draw fingertip dot on camera view
                    double dotX = nx * dotCanvas.getWidth();
                    double dotY = ny * dotCanvas.getHeight();
                    dc.setFill(javafx.scene.paint.Color.rgb(0, 220, 120, 0.85));
                    dc.fillOval(dotX - 10, dotY - 10, 20, 20);
                    dc.setStroke(javafx.scene.paint.Color.WHITE);
                    dc.setLineWidth(2);
                    dc.strokeOval(dotX - 10, dotY - 10, 20, 20);

                    // Map fingertip position to signature canvas
                    double sigX = nx * sigCanvas.getWidth();
                    double sigY = ny * sigCanvas.getHeight() * 0.85 + 10;

                    if (!penDown[0] || lastTipXY[0] < 0) {
                        sgc.beginPath();
                        sgc.moveTo(sigX, sigY);
                    } else {
                        sgc.lineTo(sigX, sigY);
                        sgc.stroke();
                        hasSigned[0] = true;
                    }
                    penDown[0] = true;
                    lastTipXY[0] = sigX;
                    lastTipXY[1] = sigY;
                }
            );
        });

        // ─ Buttons ─
        HBox btnRow = new HBox(14);
        btnRow.setPadding(new Insets(16, 32, 24, 32));
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        Button clearBtn = new Button("Effacer");
        clearBtn.setStyle(
            "-fx-background-color: rgba(0,0,0,0.06); -fx-text-fill: #555;" +
            "-fx-font-size: 13; -fx-padding: 10 28; -fx-background-radius: 20; -fx-cursor: hand;" +
            "-fx-border-color: rgba(0,0,0,0.14); -fx-border-radius: 20; -fx-border-width: 1;"
        );
        clearBtn.setOnAction(ev -> {
            resetSigCanvas.run();
            hasSigned[0] = false;
            penDown[0] = false;
            lastTipXY[0] = -1;
        });

        Label sigError = new Label("Veuillez apposer votre signature avant de confirmer.");
        sigError.setStyle("-fx-text-fill: #cc3333; -fx-font-size: 11; -fx-font-weight: bold;");
        sigError.setVisible(false); sigError.setManaged(false);

        Button confirmSigBtn = new Button("Confirmer la reservation");
        confirmSigBtn.setStyle(
            "-fx-background-color: linear-gradient(to right,#00c875,#00e5a0);" +
            "-fx-text-fill: #002b1a; -fx-font-size: 14; -fx-font-weight: 900;" +
            "-fx-padding: 12 36; -fx-background-radius: 24; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian,rgba(0,200,120,0.5),16,0,0,4);"
        );
        confirmSigBtn.setOnAction(ev -> {
            if (!hasSigned[0]) {
                sigError.setVisible(true); sigError.setManaged(true);
                return;
            }
            // Stop camera
            if (airSvc[0] != null) { airSvc[0].stop(); airSvc[0] = null; }
            // Save signature PNG
            try {
                File sigDir = new File(System.getProperty("user.home") + "/govibe_signatures");
                sigDir.mkdirs();
                File sigFile = new File(sigDir, "signature_" + checkoutId + "_" + System.currentTimeMillis() + ".png");
                WritableImage snapshot = sigCanvas.snapshot(null, null);
                int sw = (int) snapshot.getWidth(), sh = (int) snapshot.getHeight();
                PixelReader pr = snapshot.getPixelReader();
                BufferedImage bufImg = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
                for (int y = 0; y < sh; y++)
                    for (int x = 0; x < sw; x++)
                        bufImg.setRGB(x, y, pr.getArgb(x, y));
                ImageIO.write(bufImg, "png", sigFile);
                System.out.println("[Signature] Saved: " + sigFile.getAbsolutePath());
            } catch (Exception ex) {
                System.err.println("[Signature] Save failed: " + ex.getMessage());
            }
            showBookingSuccess(flight);
        });

        btnRow.getChildren().addAll(sigError, clearBtn, confirmSigBtn);
        HBox.setHgrow(sigError, Priority.ALWAYS);

        sigCard.getChildren().addAll(headerBox, modeRow, cameraSection, sigRow, btnRow);
        bookingFormContainer.getChildren().add(sigCard);
    }

    private void showBookingSuccess(Flight flight) {
        bookingFormContainer.getChildren().clear();
        VBox successBox = new VBox(20);
        successBox.setAlignment(Pos.CENTER);
        successBox.setPadding(new Insets(70, 50, 70, 50));
        successBox.setMaxWidth(560);
        successBox.setStyle(
            "-fx-background-color: linear-gradient(145deg,rgba(0,200,120,0.18),rgba(0,60,30,0.35));" +
            "-fx-background-radius: 28; -fx-border-color: rgba(0,255,140,0.35);" +
            "-fx-border-width: 1.5; -fx-border-radius: 28;" +
            "-fx-effect: dropshadow(gaussian,rgba(0,255,140,0.25),30,0,0,8);"
        );

        Label checkmark = new Label("OK");
        checkmark.setStyle(
            "-fx-font-size: 56; -fx-font-weight: 900; -fx-text-fill: #00ffaa;" +
            "-fx-background-color: rgba(0,255,140,0.15); -fx-background-radius: 50;" +
            "-fx-padding: 16 24; -fx-border-color: rgba(0,255,140,0.4);" +
            "-fx-border-radius: 50; -fx-border-width: 2;" +
            "-fx-effect: dropshadow(gaussian,rgba(0,255,140,0.6),20,0,0,0);"
        );

        Label successMsg = new Label("Paiement confirme !");
        successMsg.setStyle("-fx-font-size: 26; -fx-font-weight: 900; -fx-text-fill: white;" +
            "-fx-effect: dropshadow(gaussian,rgba(0,255,140,0.5),10,0,0,2);");

        Label routeMsg = new Label(flight.getDepartureAirport() + "  ->  " + flight.getDestination());
        routeMsg.setStyle("-fx-font-size: 17; -fx-text-fill: #00ffaa; -fx-font-weight: 700;");

        Label detail = new Label("Reservation CONFIRMEE.\nConsultez vos reservations dans l'onglet Mes reservations.");
        detail.setStyle("-fx-font-size: 13; -fx-text-fill: rgba(190,240,215,0.8); -fx-text-alignment: center;");
        detail.setWrapText(true);
        detail.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        successBox.getChildren().addAll(checkmark, successMsg, routeMsg, detail);
        bookingFormContainer.getChildren().add(successBox);

        // Auto-close after 2.5s and refresh
        new Thread(() -> {
            try { Thread.sleep(2500); } catch (Exception ignored) {}
            Platform.runLater(() -> {
                hideBookingModal();
                loadAvailableFlights();
                loadMyBookings();
            });
        }).start();
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

    private void setupHeroBackground() {
        if (bgImageView != null && rootStackPane != null) {
            bgImageView.fitWidthProperty().bind(rootStackPane.widthProperty());
            bgImageView.fitHeightProperty().bind(rootStackPane.heightProperty());
            
            var resourcePath = "/messages/home-hero5.png";
            var url = getClass().getResource(resourcePath);
            if (url != null) {
                Image img = new Image(url.toExternalForm(), true);
                bgImageView.setImage(img);
                System.out.println("[Background] Hero image loaded successfully in UserDashboard");
            } else {
                System.err.println("[Background] ERROR: Resource " + resourcePath + " not found!");
            }
        }
    }

    private void ensureSchemaUpToDate() {
        System.out.println("🚀 [UserDashboard] Checking database schema...");
        String[] alterStatements = {
            "ALTER TABLE checkout ADD COLUMN IF NOT EXISTS passenger_name VARCHAR(255)",
            "ALTER TABLE checkout ADD COLUMN IF NOT EXISTS passenger_email VARCHAR(255)",
            "ALTER TABLE checkout ADD COLUMN IF NOT EXISTS passenger_phone VARCHAR(50)",
            "ALTER TABLE checkout ADD COLUMN IF NOT EXISTS payment_method VARCHAR(50) DEFAULT 'CREDIT_CARD'",
            "ALTER TABLE checkout ADD COLUMN IF NOT EXISTS seat_preference VARCHAR(20) DEFAULT 'WINDOW'",
            "ALTER TABLE checkout ADD COLUMN IF NOT EXISTS travel_class VARCHAR(20) DEFAULT 'Economy'"
        };

        try (Connection conn = UnifiedDatabaseManager.getConnection()) {
            if (conn != null) {
                try (Statement stmt = conn.createStatement()) {
                    for (String sql : alterStatements) {
                        try {
                            stmt.execute(sql);
                        } catch (SQLException e) {
                            if (e.getErrorCode() != 1060) { // If not 'Duplicate column'
                                System.err.println("[SchemaFix] Error: " + e.getMessage());
                            }
                        }
                    }
                }
                System.out.println("✅ [UserDashboard] Schema check complete.");
            }
        } catch (SQLException e) {
            System.err.println("❌ [UserDashboard] Schema check failed: " + e.getMessage());
        }
    }
}
