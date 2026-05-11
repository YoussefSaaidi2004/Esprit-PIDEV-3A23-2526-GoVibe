package org.example.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.example.entities.*;
import org.example.services.ServiceChambre;
import org.example.services.ServiceHotel;
import org.example.services.ServiceReservation;
import org.example.services.WeatherService;
import org.example.services.TranslationService;
import org.example.services.HotelChatbotRAG;
import org.example.services.ServiceAutoAssignment;
import org.example.services.ServiceFidelite;
import org.example.services.ServiceCodePromo;
import org.example.services.HolidayService;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;
import javafx.animation.*;
import javafx.concurrent.Task;
import javafx.scene.image.WritableImage;
import javafx.util.Duration;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class RoomBookingController implements Initializable {

    @FXML private FlowPane chambreCardsContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterHotelCombo;
    @FXML private StackPane rootStackPane;
    @FXML private ImageView bgImageView;

    private final ServiceChambre serviceChambre = new ServiceChambre();
    private final ServiceHotel serviceHotel = new ServiceHotel();
    private final ServiceReservation serviceReservation = new ServiceReservation();
    private final WeatherService weatherService = new WeatherService();
    private final TranslationService translationService = new TranslationService();
    private final ServiceAutoAssignment serviceAutoAssign = new ServiceAutoAssignment();
    private final ServiceFidelite serviceFidelite = new ServiceFidelite();
    private final ServiceCodePromo serviceCodePromo = new ServiceCodePromo();
    private final HolidayService holidayService = new HolidayService();
    private HotelChatbotRAG chatbotRAG;

    private ObservableList<Chambre> chambreList = FXCollections.observableArrayList();
    private ObservableList<Hotel> hotelList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadHotels();
        loadChambres();
        setupHeroBackground();
        // Init chatbot in background
        new Thread(() -> {
            chatbotRAG = new HotelChatbotRAG();
        }, "Chatbot-Init").start();
    }

    private void loadHotels() {
        new Thread(() -> {
            try {
                java.util.List<Hotel> list = serviceHotel.show();
                Platform.runLater(() -> {
                    hotelList.clear();
                    hotelList.addAll(list);
                    if (filterHotelCombo != null) {
                        filterHotelCombo.getItems().clear();
                        filterHotelCombo.getItems().add("Tous les hotels");
                        for (Hotel hotel : list) {
                            filterHotelCombo.getItems().add(hotel.getNom());
                        }
                        filterHotelCombo.setValue("Tous les hotels");
                    }
                });
            } catch (SQLException e) {
                Platform.runLater(() -> showAlert("Erreur", "Impossible de charger les hotels: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        }, "Hotels-Load-Thread").start();
    }

    private void loadChambres() {
        new Thread(() -> {
            try {
                java.util.List<Chambre> list = serviceChambre.show();
                Platform.runLater(() -> {
                    chambreList.clear();
                    chambreList.addAll(list);
                    displayChambres(chambreList);
                });
            } catch (SQLException e) {
                Platform.runLater(() -> showAlert("Erreur", "Impossible de charger les chambres: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        }, "Chambres-Load-Thread").start();
    }

    private void displayChambres(List<Chambre> chambres) {
        chambreCardsContainer.getChildren().clear();
        for (Chambre chambre : chambres) {
            chambreCardsContainer.getChildren().add(createChambreCard(chambre));
        }
    }

    private VBox createChambreCard(Chambre chambre) {
        // ═══ OUTER CARD ═══
        VBox card = new VBox(0);
        card.setPrefWidth(370);
        card.setMaxWidth(370);
        card.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #01200f, #012818);" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: rgba(80,200,120,0.25);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 20;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 18, 0, 0, 6);"
        );

        // ═══ IMAGE BANNER ═══
        String type = chambre.getType() != null ? chambre.getType().toLowerCase() : "";
        String roomEmoji;
        String gradientFrom, gradientTo;
        if (type.contains("suite") || type.contains("royal") || type.contains("prestige")) {
            roomEmoji = "🛎"; gradientFrom = "#1a0533"; gradientTo = "#3d1060";
        } else if (type.contains("double") || type.contains("twin")) {
            roomEmoji = "🛏"; gradientFrom = "#012030"; gradientTo = "#014060";
        } else if (type.contains("family") || type.contains("famille")) {
            roomEmoji = "👨‍👩‍👧"; gradientFrom = "#1a2b00"; gradientTo = "#2e4d00";
        } else if (type.contains("studio") || type.contains("apart")) {
            roomEmoji = "🏠"; gradientFrom = "#2b1a00"; gradientTo = "#4d3000";
        } else {
            roomEmoji = "🛏️"; gradientFrom = "#012818"; gradientTo = "#024030";
        }
        StackPane imageBanner = new StackPane();
        imageBanner.setPrefHeight(160);
        imageBanner.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, " + gradientFrom + ", " + gradientTo + ");" +
            "-fx-background-radius: 18 18 0 0;"
        );
        // Decorative circles for depth
        StackPane circle1 = new StackPane();
        circle1.setPrefSize(120, 120);
        circle1.setStyle("-fx-background-color: rgba(255,255,255,0.04); -fx-background-radius: 60;");
        StackPane.setAlignment(circle1, Pos.TOP_RIGHT);
        StackPane.setMargin(circle1, new Insets(-30, -30, 0, 0));
        StackPane circle2 = new StackPane();
        circle2.setPrefSize(70, 70);
        circle2.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 35;");
        StackPane.setAlignment(circle2, Pos.BOTTOM_LEFT);
        StackPane.setMargin(circle2, new Insets(0, 0, -20, -20));
        // Center content
        VBox bannerContent = new VBox(6);
        bannerContent.setAlignment(Pos.CENTER);
        Label bigEmoji = new Label(roomEmoji);
        bigEmoji.setStyle("-fx-font-size: 48px;");
        // Star rating based on price tier
        double price = chambre.getPrixStandard();
        int stars = price >= 500 ? 5 : price >= 300 ? 4 : price >= 150 ? 3 : price >= 80 ? 2 : 1;
        StringBuilder starStr = new StringBuilder();
        for (int s = 0; s < 5; s++) starStr.append(s < stars ? "★" : "☆");
        Label starsLabel = new Label(starStr.toString());
        starsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #FFD700; -fx-effect: dropshadow(gaussian, rgba(255,215,0,0.5), 6, 0, 0, 0);");
        Label chambreNumLabel = new Label("N° " + chambre.getId() + "  ·  " + chambre.getType());
        chambreNumLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.7); -fx-font-weight: bold;");
        bannerContent.getChildren().addAll(bigEmoji, starsLabel, chambreNumLabel);
        imageBanner.getChildren().addAll(circle1, circle2, bannerContent);
        card.getChildren().add(imageBanner);

        // ═══ TOP BANNER: type + capacity + availability ═══
        HBox banner = new HBox(10);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(18, 18, 12, 18));
        banner.setStyle("-fx-background-color: rgba(80,200,120,0.08); -fx-background-radius: 20 20 0 0;");

        VBox titleCol = new VBox(4);
        HBox.setHgrow(titleCol, Priority.ALWAYS);
        Label typeLabel = new Label(chambre.getType());
        typeLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: white;");
        Label hotelLabel = new Label("📍 " + getHotelName(chambre.getHotelId()));
        hotelLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #A0E0C9;");
        titleCol.getChildren().addAll(typeLabel, hotelLabel);

        VBox rightCol = new VBox(6);
        rightCol.setAlignment(Pos.CENTER_RIGHT);
        // Capacity chip
        Label capLabel = new Label("👤 " + chambre.getCapacite() + " pers.");
        capLabel.setStyle(
            "-fx-background-color: rgba(80,200,120,0.2); -fx-text-fill: #50C878;" +
            "-fx-padding: 4 10; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: bold;"
        );
        // Availability badge (async)
        Label availBadge = new Label("⏳ ...");
        availBadge.setStyle(
            "-fx-background-color: rgba(160,224,201,0.12); -fx-text-fill: #A0E0C9;" +
            "-fx-padding: 4 10; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: bold;"
        );
        rightCol.getChildren().addAll(capLabel, availBadge);
        banner.getChildren().addAll(titleCol, rightCol);

        Task<Boolean> availTask = new Task<>() {
            @Override protected Boolean call() throws Exception {
                return serviceAutoAssign.isChambreDisponible(chambre.getId(), LocalDate.now(), LocalDate.now().plusDays(1));
            }
        };
        availTask.setOnSucceeded(ev -> Platform.runLater(() -> {
            if (Boolean.TRUE.equals(availTask.getValue())) {
                availBadge.setText("✅ Disponible");
                availBadge.setStyle(
                    "-fx-background-color: rgba(80,200,120,0.2); -fx-text-fill: #50C878;" +
                    "-fx-padding: 4 10; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: bold;"
                );
            } else {
                availBadge.setText("🔴 Occupée");
                availBadge.setStyle(
                    "-fx-background-color: rgba(216,78,54,0.2); -fx-text-fill: #FF6B6B;" +
                    "-fx-padding: 4 10; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: bold;"
                );
            }
        }));
        availTask.setOnFailed(ev -> Platform.runLater(() -> availBadge.setText("")));
        new Thread(availTask, "avail-" + chambre.getId()).start();

        // ═══ WEATHER BOX ═══
        VBox weatherBox = new VBox(4);
        weatherBox.setPadding(new Insets(10, 18, 10, 18));
        weatherBox.setStyle(
            "-fx-background-color: rgba(0,150,255,0.08);" +
            "-fx-border-color: rgba(0,180,255,0.15);" +
            "-fx-border-width: 0 0 1 0;"
        );
        HBox weatherInner = new HBox(10);
        weatherInner.setAlignment(Pos.CENTER_LEFT);
        Label weatherIcon = new Label("🌤");
        weatherIcon.setStyle("-fx-font-size: 22px;");
        VBox weatherTextCol = new VBox(2);
        HBox.setHgrow(weatherTextCol, Priority.ALWAYS);
        Label weatherTitle = new Label("MÉTÉO LOCALE");
        weatherTitle.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: rgba(100,180,255,0.7); -fx-letter-spacing: 1px;");
        Label weatherValue = new Label("Chargement...");
        weatherValue.setStyle("-fx-font-size: 13px; -fx-text-fill: #7EC8E3; -fx-font-weight: bold;");
        weatherTextCol.getChildren().addAll(weatherTitle, weatherValue);
        weatherInner.getChildren().addAll(weatherIcon, weatherTextCol);
        weatherBox.getChildren().add(weatherInner);

        Task<WeatherService.WeatherData> weatherTask = new Task<>() {
            @Override protected WeatherService.WeatherData call() throws Exception {
                // Try hotel city first, fall back to hotel name as search term
                String city = getHotelCity(chambre.getHotelId());
                WeatherService.WeatherData result = null;
                if (city != null && !city.isBlank()) {
                    result = weatherService.getWeather(city);
                }
                if (result == null) {
                    // Fallback: try hotel name (may contain city info)
                    String hotelName = getHotelName(chambre.getHotelId());
                    if (hotelName != null && !hotelName.isBlank() && !hotelName.equals("Hôtel inconnu")) {
                        result = weatherService.getWeather(hotelName);
                    }
                }
                if (result == null) {
                    // Last resort: try "Tunis" as default Tunisian city
                    result = weatherService.getWeather("Tunis");
                }
                return result;
            }
        };
        weatherTask.setOnSucceeded(ev -> Platform.runLater(() -> {
            WeatherService.WeatherData w = weatherTask.getValue();
            if (w != null) {
                weatherIcon.setText(w.getEmoji());
                String desc = w.description != null ? w.description : "";
                if (!desc.isEmpty()) desc = Character.toUpperCase(desc.charAt(0)) + desc.substring(1);
                // Show city name + conditions
                String cityLabel = (w.city != null && !w.city.isBlank()) ? w.city + "  " : "";
                weatherValue.setText(cityLabel + desc + "  —  " + String.format("%.1f°C", w.temperature) +
                    "\n💧 Humidité : " + (int) w.humidity + "%   💨 " + String.format("%.1f", w.windSpeed) + " m/s");
                weatherValue.setWrapText(true);
            } else {
                // Keep box visible but show offline state
                weatherIcon.setText("📡");
                weatherValue.setText("Météo non disponible");
                weatherValue.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(160,224,201,0.5); -fx-font-style: italic;");
            }
        }));
        weatherTask.setOnFailed(ev -> Platform.runLater(() -> {
            weatherIcon.setText("📡");
            weatherValue.setText("Météo non disponible");
            weatherValue.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(160,224,201,0.5); -fx-font-style: italic;");
        }));
        new Thread(weatherTask, "weather-" + chambre.getId()).start();

        // ═══ EQUIPMENTS + TRANSLATION ═══
        VBox equipSection = new VBox(10);
        equipSection.setPadding(new Insets(14, 18, 8, 18));

        // Section label
        Label equipSectionLabel = new Label("✨ ÉQUIPEMENTS");
        equipSectionLabel.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: rgba(160,224,201,0.6); -fx-letter-spacing: 1px;");

        final String equipements = chambre.getEquipements() != null ? chambre.getEquipements() : "Aucun équipement spécifié";
        Label equipLabel = new Label(equipements);
        equipLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #D0F0E0; -fx-wrap-text: true;");
        equipLabel.setWrapText(true);
        equipLabel.setMinHeight(44);

        // 🌍 Translation row
        HBox transRow = new HBox(8);
        transRow.setAlignment(Pos.CENTER_LEFT);
        Label transTitle = new Label("🌍 Traduire :");
        transTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #A0E0C9;");

        final Button[] langBtns = new Button[3];
        langBtns[0] = mkLangBtn("🇫🇷 FR", true);
        langBtns[1] = mkLangBtn("🇬🇧 EN", false);
        langBtns[2] = mkLangBtn("🇸🇦 AR", false);

        langBtns[0].setOnAction(e -> {
            equipLabel.setText(equipements);
            equipLabel.setNodeOrientation(javafx.geometry.NodeOrientation.LEFT_TO_RIGHT);
            setActiveLangBtn(langBtns, 0);
        });
        langBtns[1].setOnAction(e -> {
            equipLabel.setText(translationService.translate(equipements, TranslationService.Language.EN));
            equipLabel.setNodeOrientation(javafx.geometry.NodeOrientation.LEFT_TO_RIGHT);
            setActiveLangBtn(langBtns, 1);
        });
        langBtns[2].setOnAction(e -> {
            equipLabel.setText(translationService.translate(equipements, TranslationService.Language.AR));
            equipLabel.setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);
            setActiveLangBtn(langBtns, 2);
        });
        transRow.getChildren().addAll(transTitle, langBtns[0], langBtns[1], langBtns[2]);
        equipSection.getChildren().addAll(equipSectionLabel, equipLabel, transRow);

        // ═══ PRICE ═══
        HBox priceBox = new HBox(10);
        priceBox.setAlignment(Pos.CENTER_LEFT);
        priceBox.setPadding(new Insets(12, 18, 12, 18));
        priceBox.setStyle(
            "-fx-background-color: rgba(80,200,120,0.1);" +
            "-fx-border-color: rgba(80,200,120,0.15); -fx-border-width: 1 0 1 0;"
        );
        VBox priceTextCol = new VBox(2);
        HBox.setHgrow(priceTextCol, Priority.ALWAYS);
        Label priceCaption = new Label("PRIX PAR NUIT");
        priceCaption.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: rgba(80,200,120,0.7); -fx-letter-spacing: 1px;");
        Label priceValue = new Label(String.format("%.2f TND", chambre.getPrixStandard()));
        priceValue.setStyle("-fx-font-size: 26px; -fx-font-weight: 900; -fx-text-fill: #50C878;");
        priceTextCol.getChildren().addAll(priceCaption, priceValue);
        // Season prices mini
        VBox seasonCol = new VBox(2);
        seasonCol.setAlignment(Pos.CENTER_RIGHT);
        Label hauteLabel = new Label("🌞 " + String.format("%.2f TND", chambre.getPrixHauteSaison()));
        hauteLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #FF9800;");
        Label basseLabel = new Label("❄️ " + String.format("%.2f TND", chambre.getPrixBasseSaison()));
        basseLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #7EC8E3;");
        seasonCol.getChildren().addAll(hauteLabel, basseLabel);
        priceBox.getChildren().addAll(priceTextCol, seasonCol);

        // ═══ ACTION BUTTONS ═══
        HBox actionRow = new HBox(10);
        actionRow.setPadding(new Insets(14, 18, 16, 18));
        actionRow.setAlignment(Pos.CENTER);

        Button chatBtn = new Button("🤖 Assistant IA");
        chatBtn.setStyle(
            "-fx-background-color: rgba(80,200,120,0.15);" +
            "-fx-text-fill: #50C878; -fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-padding: 10 14; -fx-background-radius: 12; -fx-cursor: hand;" +
            "-fx-border-color: rgba(80,200,120,0.3); -fx-border-radius: 12; -fx-border-width: 1;"
        );
        chatBtn.setOnAction(e -> openChatbotForChambre(chambre));

        Button reserveBtn = new Button("🛏️ Réserver maintenant");
        reserveBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, #50C878, #2d9e55);" +
            "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-padding: 11 20; -fx-background-radius: 12; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(80,200,120,0.4), 10, 0, 0, 3);"
        );
        HBox.setHgrow(reserveBtn, Priority.ALWAYS);
        reserveBtn.setMaxWidth(Double.MAX_VALUE);
        reserveBtn.setOnAction(e -> reserveChambre(chambre));

        // Hover effects
        reserveBtn.setOnMouseEntered(e -> reserveBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, #5dd68a, #369960);" +
            "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-padding: 11 20; -fx-background-radius: 12; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(80,200,120,0.6), 14, 0, 0, 4);"
        ));
        reserveBtn.setOnMouseExited(e -> reserveBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, #50C878, #2d9e55);" +
            "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-padding: 11 20; -fx-background-radius: 12; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(80,200,120,0.4), 10, 0, 0, 3);"
        ));
        actionRow.getChildren().addAll(chatBtn, reserveBtn);

        // ═══ FEATURES STRIP ═══
        VBox featuresStrip = new VBox(0);
        featuresStrip.setPadding(new Insets(10, 14, 10, 14));
        featuresStrip.setStyle("-fx-background-color: rgba(0,0,0,0.15);");

        // 🏆 Loyalty row
        HBox loyaltyRow = new HBox(8);
        loyaltyRow.setAlignment(Pos.CENTER_LEFT);
        loyaltyRow.setPadding(new Insets(5, 0, 5, 0));
        Label loyaltyIcon = new Label("🏆");
        loyaltyIcon.setStyle("-fx-font-size: 13px;");
        Label loyaltyText = new Label("Chargement fidélité...");
        loyaltyText.setStyle("-fx-font-size: 11px; -fx-text-fill: #FFD700;");
        loyaltyRow.getChildren().addAll(loyaltyIcon, loyaltyText);
        new Thread(() -> {
            try {
                if (SessionManager.getCurrentUser() != null) {
                    ProgrammeFidelite pf = serviceFidelite.getOrCreate(SessionManager.getCurrentUser().getId());
                    if (pf != null) {
                        String txt = pf.getStatutEmoji() + " " + pf.getStatut() + "  ·  " + pf.getPoints() + " pts  ·  -" + pf.getReductionPourcentage() + "%";
                        Platform.runLater(() -> loyaltyText.setText(txt));
                    } else {
                        Platform.runLater(() -> loyaltyText.setText("🌱 Nouveau membre — gagnez des points!"));
                    }
                } else {
                    Platform.runLater(() -> loyaltyText.setText("Connectez-vous pour voir vos points"));
                }
            } catch (Exception ex) {
                Platform.runLater(() -> loyaltyText.setText("Programme fidélité disponible"));
            }
        }, "card-loyalty-" + chambre.getId()).start();

        // Separator
        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background-color: rgba(255,255,255,0.06);");
        sep1.setOpacity(0.4);

        // 🗓 Holiday row
        HBox holidayRow = new HBox(8);
        holidayRow.setAlignment(Pos.CENTER_LEFT);
        holidayRow.setPadding(new Insets(5, 0, 5, 0));
        Label holidayIcon = new Label("🗓");
        holidayIcon.setStyle("-fx-font-size: 13px;");
        Label holidayText = new Label("Vérification jours fériés...");
        holidayText.setStyle("-fx-font-size: 11px; -fx-text-fill: #A0E0C9;");
        holidayRow.getChildren().addAll(holidayIcon, holidayText);
        new Thread(() -> {
            try {
                String summary = holidayService.buildHolidaySummary(LocalDate.now(), LocalDate.now().plusDays(30));
                Platform.runLater(() -> {
                    if (summary != null && !summary.isBlank()) {
                        holidayText.setText("Fériés ce mois: " + summary);
                        holidayText.setStyle("-fx-font-size: 11px; -fx-text-fill: #FF9800;");
                        holidayIcon.setText("⚠️");
                    } else {
                        holidayText.setText("Aucun jour férié ce mois — prix standard");
                        holidayText.setStyle("-fx-font-size: 11px; -fx-text-fill: #50C878;");
                        holidayIcon.setText("✅");
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> holidayText.setText("Tarifs standards applicables"));
            }
        }, "card-holiday-" + chambre.getId()).start();

        // Separator
        Separator sep2 = new Separator();
        sep2.setStyle("-fx-background-color: rgba(255,255,255,0.06);");
        sep2.setOpacity(0.4);

        // 💡 Promo row
        HBox promoRow = new HBox(8);
        promoRow.setAlignment(Pos.CENTER_LEFT);
        promoRow.setPadding(new Insets(5, 0, 5, 0));
        Label promoIcon = new Label("💡");
        promoIcon.setStyle("-fx-font-size: 13px;");
        Label promoText = new Label("Codes promo acceptés — entrez le vôtre à la réservation");
        promoText.setStyle("-fx-font-size: 11px; -fx-text-fill: #7EC8E3;");
        promoRow.getChildren().addAll(promoIcon, promoText);

        featuresStrip.getChildren().addAll(loyaltyRow, sep1, holidayRow, sep2, promoRow);

        // ═══ ASSEMBLE ═══
        card.getChildren().addAll(banner, weatherBox, equipSection, priceBox, featuresStrip, actionRow);

        // Card hover lift
        card.setOnMouseEntered(e -> {
            card.setTranslateY(-6);
            card.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #01280f, #013020);" +
                "-fx-background-radius: 20;" +
                "-fx-border-color: rgba(80,200,120,0.55);" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 20;" +
                "-fx-effect: dropshadow(gaussian, rgba(80,200,120,0.25), 24, 0, 0, 8);"
            );
        });
        card.setOnMouseExited(e -> {
            card.setTranslateY(0);
            card.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #01200f, #012818);" +
                "-fx-background-radius: 20;" +
                "-fx-border-color: rgba(80,200,120,0.25);" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 20;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 18, 0, 0, 6);"
            );
        });

        // Fade-in animation on load
        card.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(350), card);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        return card;
    }

    private void setActiveLangBtn(Button[] btns, int activeIdx) {
        String active = "-fx-background-color: rgba(80,200,120,0.3); -fx-text-fill: #50C878; " +
            "-fx-padding: 4 12; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: bold; " +
            "-fx-border-color: rgba(80,200,120,0.5); -fx-border-radius: 10; -fx-border-width: 1;";
        String inactive = "-fx-background-color: rgba(160,224,201,0.08); -fx-text-fill: #A0E0C9; " +
            "-fx-padding: 4 12; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-size: 11px; " +
            "-fx-border-color: rgba(160,224,201,0.2); -fx-border-radius: 10; -fx-border-width: 1;";
        for (int i = 0; i < btns.length; i++) btns[i].setStyle(i == activeIdx ? active : inactive);
    }

    private Button mkLangBtn(String text, boolean isActive) {
        Button b = new Button(text);
        if (isActive) {
            b.setStyle("-fx-background-color: rgba(80,200,120,0.3); -fx-text-fill: #50C878; " +
                "-fx-padding: 4 12; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: bold; " +
                "-fx-border-color: rgba(80,200,120,0.5); -fx-border-radius: 10; -fx-border-width: 1;");
        } else {
            b.setStyle("-fx-background-color: rgba(160,224,201,0.08); -fx-text-fill: #A0E0C9; " +
                "-fx-padding: 4 12; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-size: 11px; " +
                "-fx-border-color: rgba(160,224,201,0.2); -fx-border-radius: 10; -fx-border-width: 1;");
        }
        return b;
    }

    private void reserveChambre(Chambre chambre) {
        if (SessionManager.getCurrentUser() == null) {
            showAlert("Connexion requise", "Veuillez vous connecter pour réserver une chambre.", Alert.AlertType.WARNING);
            return;
        }

        Dialog<Reservation> dialog = new Dialog<>();
        dialog.setTitle("🏨 Réserver — " + chambre.getType());
        dialog.setHeaderText(null);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #012818;");

        ButtonType saveButtonType = new ButtonType("💳 Confirmer la réservation", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        VBox form = new VBox(14);
        form.setPadding(new Insets(22));
        form.setPrefWidth(520);
        form.setStyle("-fx-background-color: #012818;");

        // ── Header ──────────────────────────────────────────────
        Label title = new Label("✨ " + chambre.getType() + " — " + getHotelName(chambre.getHotelId()));
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #50C878;");
        Label subtitle = new Label("Complétez votre réservation ci-dessous");
        subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(160,224,201,0.6);");
        form.getChildren().addAll(title, subtitle);

        // ── 🏆 LOYALTY PANEL ────────────────────────────────────
        VBox loyaltyPanel = new VBox(8);
        loyaltyPanel.setPadding(new Insets(14, 16, 14, 16));
        loyaltyPanel.setStyle(
            "-fx-background-color: rgba(255,215,0,0.07);" +
            "-fx-border-color: rgba(255,215,0,0.35);" +
            "-fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;"
        );
        Label loyaltyTitle = new Label("🏆  PROGRAMME FIDÉLITÉ");
        loyaltyTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #FFD700; -fx-letter-spacing: 1px;");
        Label loyaltyValue = new Label("⏳  Chargement de votre statut...");
        loyaltyValue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #FFE97A;");
        Label loyaltyHint = new Label("Chaque réservation vous rapporte des points et des réductions.");
        loyaltyHint.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255,215,0,0.55); -fx-wrap-text: true;");
        loyaltyHint.setWrapText(true);
        loyaltyPanel.getChildren().addAll(loyaltyTitle, loyaltyValue, loyaltyHint);
        form.getChildren().add(loyaltyPanel);

        new Thread(() -> {
            try {
                ProgrammeFidelite pf = serviceFidelite.getOrCreate(SessionManager.getCurrentUser().getId());
                if (pf != null) {
                    String txt = pf.getStatutEmoji() + "  " + pf.getStatut() +
                        "   |   " + pf.getPoints() + " pts   |   -" + pf.getReductionPourcentage() + "% de réduction";
                    Platform.runLater(() -> loyaltyValue.setText(txt));
                } else {
                    Platform.runLater(() -> loyaltyValue.setText("🌱  Nouveau membre — bienvenue!"));
                }
            } catch (Exception ex) {
                Platform.runLater(() -> loyaltyValue.setText("🏆  Statut indisponible"));
            }
        }, "loyalty-check").start();

        // ── 📅 DATES PANEL ──────────────────────────────────────
        VBox datesPanel = new VBox(10);
        datesPanel.setPadding(new Insets(14, 16, 14, 16));
        datesPanel.setStyle(
            "-fx-background-color: rgba(80,200,120,0.07);" +
            "-fx-border-color: rgba(80,200,120,0.3);" +
            "-fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;"
        );
        Label datesTitle = new Label("📅  DATES DU SÉJOUR");
        datesTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #50C878; -fx-letter-spacing: 1px;");
        HBox datesRow = new HBox(12);
        datesRow.setAlignment(Pos.CENTER_LEFT);

        VBox arrCol = new VBox(4);
        Label arrLabel = new Label("Arrivée");
        arrLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #A0E0C9;");
        DatePicker startDate = new DatePicker(LocalDate.now().plusDays(1));
        startDate.setStyle("-fx-background-color: rgba(80,200,120,0.12); -fx-text-fill: white;");
        startDate.setPrefWidth(200);
        arrCol.getChildren().addAll(arrLabel, startDate);

        VBox depCol = new VBox(4);
        Label depLabel = new Label("Départ");
        depLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #A0E0C9;");
        DatePicker endDate = new DatePicker(LocalDate.now().plusDays(2));
        endDate.setStyle("-fx-background-color: rgba(80,200,120,0.12); -fx-text-fill: white;");
        endDate.setPrefWidth(200);
        depCol.getChildren().addAll(depLabel, endDate);

        datesRow.getChildren().addAll(arrCol, depCol);
        datesPanel.getChildren().addAll(datesTitle, datesRow);
        form.getChildren().add(datesPanel);

        // ── 🗓 HOLIDAY SURCHARGE PANEL ──────────────────────────
        VBox holidayPanel = new VBox(6);
        holidayPanel.setPadding(new Insets(12, 16, 12, 16));
        holidayPanel.setStyle(
            "-fx-background-color: rgba(255,152,0,0.08);" +
            "-fx-border-color: rgba(255,152,0,0.4);" +
            "-fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;"
        );
        Label holidayTitle = new Label("🗓  JOURS FÉRIÉS & SUPPLÉMENTS");
        holidayTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #FF9800; -fx-letter-spacing: 1px;");
        Label holidayValue = new Label("Sélectionnez vos dates pour vérifier les jours fériés.");
        holidayValue.setStyle("-fx-font-size: 12px; -fx-text-fill: #FFB74D; -fx-wrap-text: true;");
        holidayValue.setWrapText(true);
        holidayPanel.getChildren().addAll(holidayTitle, holidayValue);
        form.getChildren().add(holidayPanel);

        // ── 💰 PRICE SUMMARY PANEL ──────────────────────────────
        VBox pricePanel = new VBox(6);
        pricePanel.setPadding(new Insets(14, 16, 14, 16));
        pricePanel.setStyle(
            "-fx-background-color: rgba(80,200,120,0.12);" +
            "-fx-border-color: rgba(80,200,120,0.45);" +
            "-fx-border-width: 1.5; -fx-border-radius: 12; -fx-background-radius: 12;"
        );
        Label priceTitle = new Label("💰  RÉCAPITULATIF DU PRIX");
        priceTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #50C878; -fx-letter-spacing: 1px;");
        Label priceValue = new Label("—");
        priceValue.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: #50C878;");
        Label priceDetail = new Label("");
        priceDetail.setStyle("-fx-font-size: 11px; -fx-text-fill: #A0E0C9; -fx-wrap-text: true;");
        priceDetail.setWrapText(true);
        pricePanel.getChildren().addAll(priceTitle, priceValue, priceDetail);
        form.getChildren().add(pricePanel);

        // ── 💡 PROMO CODE PANEL ─────────────────────────────────
        VBox promoPanel = new VBox(10);
        promoPanel.setPadding(new Insets(14, 16, 14, 16));
        promoPanel.setStyle(
            "-fx-background-color: rgba(100,180,255,0.07);" +
            "-fx-border-color: rgba(100,180,255,0.3);" +
            "-fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;"
        );
        Label promoTitle = new Label("💡  CODE PROMOTIONNEL");
        promoTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #7EC8E3; -fx-letter-spacing: 1px;");
        HBox promoRow = new HBox(10);
        promoRow.setAlignment(Pos.CENTER_LEFT);
        TextField promoField = new TextField();
        promoField.setPromptText("Ex: HOTEL10, SUMMER2026...");
        promoField.setStyle(
            "-fx-background-color: rgba(100,180,255,0.1); -fx-text-fill: white;" +
            "-fx-prompt-text-fill: rgba(126,200,227,0.5); -fx-padding: 9 12;" +
            "-fx-background-radius: 9; -fx-font-size: 13px;"
        );
        HBox.setHgrow(promoField, Priority.ALWAYS);
        Button applyPromoBtn = new Button("✔ Appliquer");
        applyPromoBtn.setStyle(
            "-fx-background-color: rgba(100,180,255,0.2); -fx-text-fill: #7EC8E3;" +
            "-fx-padding: 9 16; -fx-background-radius: 9; -fx-cursor: hand;" +
            "-fx-font-weight: bold; -fx-border-color: rgba(100,180,255,0.3);" +
            "-fx-border-radius: 9; -fx-border-width: 1;"
        );
        promoRow.getChildren().addAll(promoField, applyPromoBtn);
        Label promoResult = new Label("Entrez un code promo pour obtenir une réduction instantanée.");
        promoResult.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(126,200,227,0.6); -fx-wrap-text: true;");
        promoResult.setWrapText(true);
        promoPanel.getChildren().addAll(promoTitle, promoRow, promoResult);
        form.getChildren().add(promoPanel);

        // ── Shared state ─────────────────────────────────────────
        final double[] promoDiscount = {0};
        final String[] appliedCode = {null};

        Runnable updatePrice = () -> {
            LocalDate s = startDate.getValue();
            LocalDate e2 = endDate.getValue();
            if (s != null && e2 != null && e2.isAfter(s)) {
                long nights = ChronoUnit.DAYS.between(s, e2);
                double base = nights * chambre.getPrixStandard();
                double surcharge = 0;
                String holidaySummary = null;
                try { surcharge = holidayService.calculerSurcharge(chambre.getPrixStandard(), s, e2); } catch (Exception ex) {}
                try { holidaySummary = holidayService.buildHolidaySummary(s, e2); } catch (Exception ex) {}
                double total = base + surcharge - promoDiscount[0];
                if (total < 0) total = 0;
                priceValue.setText(String.format("%.2f TND", total));
                StringBuilder detail = new StringBuilder();
                detail.append(nights).append(" nuit(s) × ").append(String.format("%.2f", chambre.getPrixStandard())).append(" TND");
                if (surcharge > 0) detail.append("\n📆 Supplément jours fériés: +").append(String.format("%.2f", surcharge)).append(" TND");
                if (promoDiscount[0] > 0) detail.append("\n🎫 Réduction promo: -").append(String.format("%.2f", promoDiscount[0])).append(" TND");
                priceDetail.setText(detail.toString());
                if (holidaySummary != null && !holidaySummary.isBlank()) {
                    holidayValue.setText("⚠️  " + holidaySummary);
                    holidayPanel.setStyle(
                        "-fx-background-color: rgba(255,152,0,0.14);" +
                        "-fx-border-color: rgba(255,152,0,0.6);" +
                        "-fx-border-width: 1.5; -fx-border-radius: 12; -fx-background-radius: 12;"
                    );
                } else {
                    holidayValue.setText("✅  Aucun jour férié sur cette période — pas de supplément.");
                    holidayPanel.setStyle(
                        "-fx-background-color: rgba(80,200,120,0.05);" +
                        "-fx-border-color: rgba(80,200,120,0.2);" +
                        "-fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;"
                    );
                    holidayValue.setStyle("-fx-font-size: 12px; -fx-text-fill: #50C878; -fx-wrap-text: true;");
                }
            }
        };
        startDate.setOnAction(e -> updatePrice.run());
        endDate.setOnAction(e -> updatePrice.run());
        updatePrice.run();

        applyPromoBtn.setOnAction(ev -> {
            String code = promoField.getText().trim().toUpperCase();
            if (code.isBlank()) return;
            try {
                CodePromo cp = serviceCodePromo.validerCode(code);
                if (cp != null) {
                    LocalDate s = startDate.getValue();
                    LocalDate e2 = endDate.getValue();
                    long nights = (s != null && e2 != null && e2.isAfter(s)) ? ChronoUnit.DAYS.between(s, e2) : 1;
                    double base = nights * chambre.getPrixStandard();
                    promoDiscount[0] = cp.calculerRemise(base);
                    appliedCode[0] = code;
                    promoResult.setText("✅  Code \"" + code + "\" accepté! Réduction: -" + String.format("%.2f TND", promoDiscount[0]));
                    promoResult.setStyle("-fx-font-size: 12px; -fx-text-fill: #50C878; -fx-font-weight: bold;");
                    promoPanel.setStyle(
                        "-fx-background-color: rgba(80,200,120,0.1);" +
                        "-fx-border-color: rgba(80,200,120,0.5);" +
                        "-fx-border-width: 1.5; -fx-border-radius: 12; -fx-background-radius: 12;"
                    );
                    updatePrice.run();
                } else {
                    promoResult.setText("❌  Code invalide ou expiré. Vérifiez et réessayez.");
                    promoResult.setStyle("-fx-font-size: 12px; -fx-text-fill: #FF6B6B; -fx-font-weight: bold;");
                    promoDiscount[0] = 0;
                    appliedCode[0] = null;
                    updatePrice.run();
                }
            } catch (Exception ex) {
                promoResult.setText("⚠️  Erreur: " + ex.getMessage());
            }
        });

        ScrollPane scrollPane = new ScrollPane(form);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setPrefHeight(500);
        dialogPane.setContent(scrollPane);

        Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (buildReservation(startDate, endDate, chambre, promoDiscount[0]) == null) event.consume();
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) return buildReservation(startDate, endDate, chambre, promoDiscount[0]);
            return null;
        });

        dialog.showAndWait().ifPresent(reservation -> {
            try {
                serviceReservation.insert(reservation);
                // ── Award loyalty points ──
                int pointsGagnes = 0;
                try {
                    serviceFidelite.ajouterPointsReservation(SessionManager.getCurrentUser().getId(), reservation.getPrixTotal());
                    pointsGagnes = (int)(reservation.getPrixTotal() / 10);
                } catch (Exception ex) {}
                // ── Increment promo usage ──
                if (appliedCode[0] != null) { try { serviceCodePromo.incrementerUtilisation(appliedCode[0]); } catch (Exception ex) {} }

                // ── Success dialog with points summary ──
                String successMsg =
                    "🏨  " + getHotelName(chambre.getHotelId()) + "\n" +
                    "🛏️  " + chambre.getType() + "\n" +
                    "📅  " + reservation.getDateDebut() + "  →  " + reservation.getDateFin() + "\n" +
                    "💰  " + String.format("%.2f TND", reservation.getPrixTotal()) + "\n\n" +
                    "🏆  +" + pointsGagnes + " points de fidélité ajoutés à votre compte!\n" +
                    "🎟  Un QR code de check-in sera disponible auprès de la réception.";
                showAlert("✅  Réservation confirmée!", successMsg, Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                showAlert("Erreur", "Erreur lors de la réservation: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private Reservation buildReservation(DatePicker startDate, DatePicker endDate, Chambre chambre, double discount) {
        LocalDate start = startDate.getValue();
        LocalDate end = endDate.getValue();
        if (start == null || end == null) {
            showAlert("Validation", "Veuillez sélectionner les dates d'arrivée et de départ.", Alert.AlertType.WARNING);
            return null;
        }
        long nights = ChronoUnit.DAYS.between(start, end);
        if (nights <= 0) {
            showAlert("Validation", "La date de départ doit être après la date d'arrivée.", Alert.AlertType.WARNING);
            return null;
        }
        double base = nights * chambre.getPrixStandard();
        double surcharge = 0;
        try { surcharge = holidayService.calculerSurcharge(chambre.getPrixStandard(), start, end); } catch (Exception ex) {}
        double totalPrice = Math.max(0, base + surcharge - discount);
        int userId = SessionManager.getCurrentUser().getId();
        return new Reservation(userId, chambre.getId(), chambre.getHotelId(), start, end, totalPrice, "EN_ATTENTE");
    }

    private String getHotelName(int hotelId) {
        for (Hotel h : hotelList) {
            if (h.getId() == hotelId) return h.getNom();
        }
        return "Hôtel inconnu";
    }

    private String getHotelCity(int hotelId) {
        for (Hotel h : hotelList) {
            if (h.getId() == hotelId) return h.getVille();
        }
        return null;
    }

    private void openChatbotForChambre(Chambre chambre) {
        Hotel hotel = null;
        for (Hotel h : hotelList) { if (h.getId() == chambre.getHotelId()) { hotel = h; break; } }
        openChatbotDialog(hotel);
    }

    @FXML
    public void openChatbot() {
        openChatbotDialog(null);
    }

    private void openChatbotDialog(Hotel hotel) {
        if (chatbotRAG == null) {
            showAlert("Assistant", "L'assistant IA est en cours d'initialisation. Veuillez réessayer dans quelques secondes.", Alert.AlertType.INFORMATION);
            return;
        }
        String hotelContext = hotel != null ? hotel.getNom() : "GoVibe Hotels";
        chatbotRAG.clearHistory();

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("🤖 Assistant GoVibe" + (hotel != null ? " — " + hotelContext : ""));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox chatArea = new VBox(10);
        chatArea.setPadding(new Insets(15));
        chatArea.setPrefWidth(480);
        chatArea.setStyle("-fx-background-color: #012818;");

        ScrollPane chatScroll = new ScrollPane(chatArea);
        chatScroll.setFitToWidth(true);
        chatScroll.setStyle("-fx-background: #012818; -fx-background-color: #012818;");
        chatScroll.setPrefHeight(340);

        // Welcome message
        addChatMessage(chatArea, chatScroll, "🤖",
            "Bonjour! Je suis l'assistant IA de GoVibe 🌟\n" +
            (hotel != null ? "Je peux vous aider avec des informations sur " + hotelContext + ".\n" : "") +
            "Posez-moi vos questions sur les chambres, prix, équipements...", false);

        // Quick questions
        Label quickLabel = new Label("Questions rapides:");
        quickLabel.setStyle("-fx-text-fill: #A0E0C9; -fx-font-size: 11px; -fx-padding: 5 0 2 0;");
        HBox quickBox = new HBox(6);
        quickBox.setAlignment(Pos.CENTER_LEFT);
        quickBox.setPadding(new Insets(0, 0, 5, 0));
        String[] quickQuestions = {"Prix des chambres", "Équipements", "Disponibilités", "Codes promo"};
        for (String q : quickQuestions) {
            Button qBtn = new Button(q);
            qBtn.setStyle("-fx-background-color: rgba(80,200,120,0.15); -fx-text-fill: #50C878; " +
                "-fx-padding: 4 10; -fx-background-radius: 15; -fx-cursor: hand; -fx-font-size: 10px;");
            qBtn.setOnAction(e -> sendChatMessage(q, chatArea, chatScroll));
            quickBox.getChildren().add(qBtn);
        }

        // Input area
        HBox inputRow = new HBox(8);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        inputRow.setPadding(new Insets(8, 0, 0, 0));
        TextField inputField = new TextField();
        inputField.setPromptText("Posez votre question...");
        inputField.setStyle("-fx-background-color: rgba(80,200,120,0.1); -fx-text-fill: white; " +
            "-fx-prompt-text-fill: #A0E0C9; -fx-padding: 10; -fx-background-radius: 20;");
        HBox.setHgrow(inputField, Priority.ALWAYS);
        Button sendBtn = new Button("➤");
        sendBtn.setStyle("-fx-background-color: #50C878; -fx-text-fill: #013220; " +
            "-fx-padding: 10 14; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-weight: bold;");

        Runnable sendAction = () -> {
            String text = inputField.getText().trim();
            if (!text.isBlank()) {
                inputField.clear();
                sendChatMessage(text, chatArea, chatScroll);
            }
        };
        sendBtn.setOnAction(e -> sendAction.run());
        inputField.setOnAction(e -> sendAction.run());
        inputRow.getChildren().addAll(inputField, sendBtn);

        VBox root = new VBox(8, chatScroll, quickLabel, quickBox, inputRow);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #012818;");
        root.setPrefWidth(500);
        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setStyle("-fx-background-color: #012818;");
        dialog.showAndWait();
    }

    private void sendChatMessage(String text, VBox chatArea, ScrollPane chatScroll) {
        addChatMessage(chatArea, chatScroll, "👤", text, true);
        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                return chatbotRAG.chat(text);
            }
        };
        task.setOnSucceeded(ev -> Platform.runLater(() -> {
            String reply = task.getValue();
            if (reply != null) addChatMessage(chatArea, chatScroll, "🤖", reply, false);
        }));
        task.setOnFailed(ev -> Platform.runLater(() -> addChatMessage(chatArea, chatScroll, "🤖", "⚠️ Erreur de connexion à l'assistant.", false)));
        new Thread(task, "chat-task").start();
    }

    private void addChatMessage(VBox chatArea, ScrollPane chatScroll, String avatar, String text, boolean isUser) {
        HBox row = new HBox(8);
        row.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        Label avatarLabel = new Label(avatar);
        avatarLabel.setStyle("-fx-font-size: 16px;");
        Label msgLabel = new Label(text);
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(340);
        msgLabel.setPadding(new Insets(10, 14, 10, 14));
        msgLabel.setStyle(isUser
            ? "-fx-background-color: rgba(80,200,120,0.25); -fx-text-fill: white; -fx-background-radius: 18 18 4 18; -fx-font-size: 12px;"
            : "-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: #E0E0E0; -fx-background-radius: 18 18 18 4; -fx-font-size: 12px;");
        if (isUser) row.getChildren().addAll(msgLabel, avatarLabel);
        else row.getChildren().addAll(avatarLabel, msgLabel);
        FadeTransition ft = new FadeTransition(Duration.millis(250), row);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
        Platform.runLater(() -> {
            chatArea.getChildren().add(row);
            chatScroll.setVvalue(1.0);
        });
    }

    @FXML
    public void filterByHotel() {
        applyFilters();
    }

    @FXML
    public void searchChambres() {
        applyFilters();
    }

    private void applyFilters() {
        String filterText = searchField != null && searchField.getText() != null
                ? searchField.getText().trim().toLowerCase() : "";
        String hotelFilter = filterHotelCombo != null ? filterHotelCombo.getValue() : "Tous les hotels";

        List<Chambre> filtered = chambreList.stream()
            .filter(chambre -> filterText.isEmpty() ||
                safeLower(chambre.getType()).contains(filterText) ||
                safeLower(chambre.getEquipements()).contains(filterText) ||
                safeLower(getHotelName(chambre.getHotelId())).contains(filterText))
                .filter(chambre -> "Tous les hotels".equals(hotelFilter) ||
                        getHotelName(chambre.getHotelId()).equals(hotelFilter))
                .collect(Collectors.toList());

        displayChambres(filtered);
    }

    @FXML
    private void handleLogout() {
        SessionManager.clear();
        org.example.mains.MainApp.switchScene("/org/example/LoginView.fxml", "GoVibe Connexion");
    }

    // ==================== NAVIGATION METHODS ====================

    @FXML
    private void handleGoHome() {
        org.example.mains.MainApp.switchScene("/org/example/UserHomeView.fxml", "Accueil");
    }

    @FXML
    private void handleGoActivities() {
        SceneNavigator.switchTo("/UserHome.fxml", chambreCardsContainer);
    }

    @FXML
    private void handleGoLocations() {
        SceneNavigator.switchTo("/LocationListView.fxml", chambreCardsContainer);
    }

    @FXML
    private void handleGoFlights() {
        org.example.mains.MainApp.switchScene("/views/user-dashboard.fxml", "Vols");
    }

    @FXML
    private void handleGoHotels() {
        loadChambres();
    }

    @FXML
    private void handleGoForums() {
        SceneNavigator.switchTo("/poste-forumviews/ListForum.fxml", chambreCardsContainer);
    }

    @FXML
    private void handleHome() {
        org.example.mains.MainApp.switchScene("/org/example/UserHomeView.fxml", "Accueil");
    }

    @FXML
    private void handleFlights() {
        org.example.mains.MainApp.switchScene("/views/user-dashboard.fxml", "Vols");
    }

    @FXML
    private void handleLocations() {
        org.example.mains.MainApp.switchScene("/LocationListView.fxml", "Voitures");
    }

    @FXML
    private void handleChambres() {
        loadChambres();
    }

    @FXML
    private void handleActivities() {
        SceneNavigator.switchTo("/UserHome.fxml", chambreCardsContainer);
    }

    @FXML
    private void handleForums() {
        SceneNavigator.switchTo("/poste-forumviews/ListForum.fxml", chambreCardsContainer);
    }

    @FXML
    private void handleProfile() {
        showAlert("Profil", "Fonctionnalité à venir : Gestion du profil utilisateur", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleMyReservations() {
        org.example.mains.MainApp.switchScene("/views/room-booking.fxml", "Mes Réservations");
    }

    @FXML
    private void handleMyLocations() {
        SceneNavigator.switchTo("/LocationListView.fxml", chambreCardsContainer);
    }

    @FXML
    private void handleMyFlights() {
        org.example.mains.MainApp.switchScene("/views/user-dashboard.fxml", "Mes Vols");
    }

    @FXML
    private void handleSettings() {
        showAlert("Paramètres", "Fonctionnalité à venir : Paramètres utilisateur", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleHelp() {
        showAlert("Aide", "Besoin d'aide ? Contactez-nous à support@govibe.tn", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleResetFilters() {
        if (searchField != null) {
            searchField.clear();
        }
        if (filterHotelCombo != null) {
            filterHotelCombo.setValue("Tous les hotels");
        }
        loadChambres();
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
                System.out.println("[Background] Hero image loaded successfully in RoomBookingView");
            } else {
                System.err.println("[Background] ERROR: Resource " + resourcePath + " not found!");
            }
        }
    }
    @FXML
    private void goToMyReservations() {
        org.example.mains.MainApp.switchScene("/views/my-reservations.fxml", "Mes Réservations");
    }
}
