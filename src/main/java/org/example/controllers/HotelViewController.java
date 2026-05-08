package org.example.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.util.Duration;
import org.example.entities.Hotel;
import org.example.services.HotelChatbotRAG;
import org.example.services.ServiceHotel;
import org.example.services.TranslationService;
import org.example.services.WeatherService;

import java.net.URL;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.stage.FileChooser;
import java.io.File;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class HotelViewController implements Initializable {

    @FXML private StackPane rootStack;
    @FXML private ImageView bgImageView;
    @FXML private FlowPane hotelCardsContainer;
    @FXML private TextField searchField;

    private ServiceHotel serviceHotel;
    private ObservableList<Hotel> hotelList;

    // New services
    private final WeatherService weatherService = new WeatherService();
    private final TranslationService translationService = new TranslationService();
    private HotelChatbotRAG chatbotRAG;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        serviceHotel = new ServiceHotel();
        hotelList = FXCollections.observableArrayList();
        setupBackground();
        loadHotels();
        // Initialize chatbot in background
        Task<HotelChatbotRAG> chatbotTask = new Task<>() {
            @Override protected HotelChatbotRAG call() { return new HotelChatbotRAG(); }
        };
        chatbotTask.setOnSucceeded(e -> chatbotRAG = chatbotTask.getValue());
        new Thread(chatbotTask, "chatbot-init").start();
    }

    private void setupBackground() {
        if (bgImageView != null && rootStack != null) {
            bgImageView.fitWidthProperty().bind(rootStack.widthProperty());
            bgImageView.fitHeightProperty().bind(rootStack.heightProperty());
            var url = getClass().getResource("/messages/home-hero5.png");
            if (url != null) {
                bgImageView.setImage(new Image(url.toExternalForm(), true));
            }
        }
    }

    private void loadHotels() {
        try {
            hotelList.clear();
            hotelList.addAll(serviceHotel.show());
            displayHotelCards();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les hôtels: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void displayHotelCards() {
        hotelCardsContainer.getChildren().clear();

        for (Hotel hotel : hotelList) {
            VBox card = createHotelCard(hotel);
            hotelCardsContainer.getChildren().add(card);
        }
    }

    private VBox createHotelCard(Hotel hotel) {
        // ── CARD SHELL ───────────────────────────────────────────────────────
        VBox card = new VBox(0);
        card.setPrefWidth(320);
        card.setMaxWidth(320);
        card.setStyle(
            "-fx-background-color: #0b2a1c;" +
            "-fx-background-radius: 18;" +
            "-fx-border-radius: 18;" +
            "-fx-border-color: rgba(50,180,100,0.22);" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 20, 0, 0, 6);" +
            "-fx-cursor: hand;");

        // ── IMAGE HEADER (edge-to-edge, clipped to top radius) ───────────────
        StackPane imageWrapper = new StackPane();
        imageWrapper.setPrefHeight(180);
        imageWrapper.setMaxHeight(180);
        // Clip image to top corners only
        Rectangle clip = new Rectangle(320, 180);
        clip.setArcWidth(36); clip.setArcHeight(36);
        imageWrapper.setClip(clip);

        ImageView imageView = new ImageView();
        imageView.setFitHeight(180);
        imageView.setFitWidth(320);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);

        String url = hotel.getPhotoUrl();
        // Set a placeholder immediately so the card renders without blocking
        java.net.URL phRes = getClass().getResource("/images/placeholder.png");
        Image placeholderImg = phRes != null
            ? new Image(phRes.toExternalForm(), 320, 180, false, true, true)
            : new Image("https://placehold.co/320x180/0a1810/50C878?text=Hotel", 320, 180, false, true, true);
        imageView.setImage(placeholderImg);

        if (url != null && !url.isBlank()) {
            // Hotel has its own photo — load it asynchronously
            Image hotelImg = new Image(
                url.startsWith("http") ? url : (url.startsWith("file:") ? url : "file:" + url),
                320, 180, false, true, true);
            hotelImg.progressProperty().addListener((obs, o, n) -> {
                if (n.doubleValue() >= 1.0 && !hotelImg.isError()) {
                    javafx.application.Platform.runLater(() -> imageView.setImage(hotelImg));
                }
            });
        } else {
            // No photo stored — fetch a city photo from Tallyfy Denizen API (free, no key)
            String city = hotel.getVille() != null ? hotel.getVille() : hotel.getNom();
            Task<String> denizenTask = new Task<>() {
                @Override protected String call() throws Exception {
                    String apiUrl = "https://denizen.tallyfy.com?city="
                        + java.net.URLEncoder.encode(city, java.nio.charset.StandardCharsets.UTF_8)
                        + "&country=Tunisia";
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                        new java.net.URL(apiUrl).openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setRequestProperty("Accept", "application/json");
                    if (conn.getResponseCode() != 200) return null;
                    try (java.io.InputStream is = conn.getInputStream()) {
                        String body = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        // Parse photo_url from JSON manually (no extra deps)
                        int idx = body.indexOf("\"photo_url\"");
                        if (idx < 0) return null;
                        int q1 = body.indexOf('"', idx + 11) + 1;
                        int q2 = body.indexOf('"', q1);
                        return (q1 > 0 && q2 > q1) ? body.substring(q1, q2) : null;
                    }
                }
            };
            denizenTask.setOnSucceeded(ev -> {
                String photoUrl = denizenTask.getValue();
                if (photoUrl != null && !photoUrl.isBlank()) {
                    Image cityImg = new Image(photoUrl, 320, 180, false, true, true);
                    cityImg.progressProperty().addListener((obs, o, n) -> {
                        if (n.doubleValue() >= 1.0 && !cityImg.isError()) {
                            imageView.setImage(cityImg);
                        }
                    });
                }
            });
            new Thread(denizenTask, "denizen-" + hotel.getId()).start();
        }

        // Strong bottom gradient so name is always readable
        Region overlay = new Region();
        overlay.setStyle("-fx-background-color: linear-gradient(to top," +
            "rgba(11,42,28,1.0) 0%," +
            "rgba(11,42,28,0.55) 45%," +
            "transparent 100%);");
        overlay.setPrefHeight(180);

        // Hotel name + stars pinned to bottom-left
        VBox titleBox = new VBox(4);
        titleBox.setPadding(new Insets(0, 14, 14, 14));
        titleBox.setAlignment(Pos.BOTTOM_LEFT);
        StackPane.setAlignment(titleBox, Pos.BOTTOM_LEFT);

        Label nameLabel = new Label(hotel.getNom());
        nameLabel.setStyle(
            "-fx-text-fill: white;" +
            "-fx-font-size: 17px;" +
            "-fx-font-weight: bold;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.9), 6, 0, 0, 1);");
        nameLabel.setWrapText(true);

        HBox starsBox = new HBox(2);
        starsBox.setAlignment(Pos.CENTER_LEFT);
        int numStars = Math.max(0, Math.min(5, hotel.getNombreEtoiles()));
        for (int i = 0; i < 5; i++) {
            Label star = new Label(i < numStars ? "★" : "☆");
            star.setStyle(i < numStars
                ? "-fx-text-fill: #FFD700; -fx-font-size: 14px;"
                : "-fx-text-fill: rgba(255,255,255,0.25); -fx-font-size: 14px;");
            starsBox.getChildren().add(star);
        }

        titleBox.getChildren().addAll(nameLabel, starsBox);
        imageWrapper.getChildren().addAll(imageView, overlay, titleBox);

        // ── CONTENT SECTION ──────────────────────────────────────────────────
        VBox content = new VBox(0);
        content.setPadding(new Insets(14, 16, 16, 16));

        // ── Location row ─────────────────────────────────────────────────────
        HBox locRow = new HBox(5);
        locRow.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(locRow, new Insets(0, 0, 12, 0));
        Label locIcon = new Label("📍");
        locIcon.setStyle("-fx-font-size: 11px; -fx-opacity: 0.85;");
        String locText = hotel.getVille() != null ? hotel.getVille() : "";
        if (hotel.getAdresse() != null && !hotel.getAdresse().isBlank())
            locText += "  ·  " + hotel.getAdresse();
        Label locLabel = new Label(locText);
        locLabel.setStyle("-fx-text-fill: #7ecfa0; -fx-font-size: 12px;");
        locLabel.setWrapText(true);
        HBox.setHgrow(locLabel, Priority.ALWAYS);
        locRow.getChildren().addAll(locIcon, locLabel);

        // ── Price + weather row ───────────────────────────────────────────────
        HBox metaRow = new HBox(10);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(metaRow, new Insets(0, 0, 14, 0));

        Label priceLabel = new Label(String.format("%.2f DT", hotel.getBudget()));
        priceLabel.setStyle(
            "-fx-background-color: rgba(50,200,100,0.15);" +
            "-fx-text-fill: #4de88a;" +
            "-fx-padding: 5 14 5 14;" +
            "-fx-background-radius: 20;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 13px;");

        Region metaSpacer = new Region();
        HBox.setHgrow(metaSpacer, Priority.ALWAYS);

        Label weatherLabel = new Label("");
        weatherLabel.setStyle("-fx-text-fill: #6bbf8a; -fx-font-size: 11px;");
        metaRow.getChildren().addAll(priceLabel, metaSpacer, weatherLabel);

        if (hotel.getVille() != null && !hotel.getVille().isBlank()) {
            Task<WeatherService.WeatherData> wTask = new Task<>() {
                @Override protected WeatherService.WeatherData call() {
                    return weatherService.getWeather(hotel.getVille());
                }
            };
            wTask.setOnSucceeded(ev -> {
                WeatherService.WeatherData wd = wTask.getValue();
                weatherLabel.setText(wd != null ? wd.getSummary() : "");
            });
            wTask.setOnFailed(ev -> weatherLabel.setText(""));
            new Thread(wTask, "weather-" + hotel.getId()).start();
        }

        // ── Divider ──────────────────────────────────────────────────────────
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: rgba(80,200,120,0.12);");
        VBox.setMargin(divider, new Insets(0, 0, 12, 0));

        // ── Description (max 2 lines) ─────────────────────────────────────────
        String originalDesc = hotel.getDescription() != null ? hotel.getDescription() : "";
        Label descLabel = new Label(originalDesc.isBlank() ? "Aucune description disponible." : originalDesc);
        descLabel.setStyle(
            "-fx-text-fill: rgba(200,235,215,0.75);" +
            "-fx-font-size: 12px;" +
            "-fx-line-spacing: 2;");
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(38);
        descLabel.setEllipsisString("…");
        VBox.setMargin(descLabel, new Insets(0, 0, 10, 0));

        // ── Language pills (right-aligned) ───────────────────────────────────
        HBox langRow = new HBox(6);
        langRow.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(langRow, new Insets(0, 0, 14, 0));
        String pillStyle =
            "-fx-background-color: rgba(50,160,90,0.13);" +
            "-fx-text-fill: rgba(100,220,150,0.85);" +
            "-fx-font-size: 10px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 3 10;" +
            "-fx-background-radius: 12;" +
            "-fx-cursor: hand;";
        String pillActive =
            "-fx-background-color: rgba(50,200,100,0.28);" +
            "-fx-text-fill: #50e896;" +
            "-fx-font-size: 10px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 3 10;" +
            "-fx-background-radius: 12;" +
            "-fx-cursor: hand;";
        Button btnFR = new Button("FR");
        Button btnEN = new Button("EN");
        Button btnAR = new Button("AR");
        btnFR.setStyle(pillActive);  // FR is default active
        btnEN.setStyle(pillStyle);
        btnAR.setStyle(pillStyle);
        btnFR.setOnAction(e -> {
            descLabel.setText(originalDesc.isBlank() ? "Aucune description disponible." : originalDesc);
            descLabel.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
            btnFR.setStyle(pillActive); btnEN.setStyle(pillStyle); btnAR.setStyle(pillStyle);
        });
        btnEN.setOnAction(e -> {
            descLabel.setText(translationService.translate(originalDesc, TranslationService.Language.EN));
            descLabel.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
            btnEN.setStyle(pillActive); btnFR.setStyle(pillStyle); btnAR.setStyle(pillStyle);
        });
        btnAR.setOnAction(e -> {
            descLabel.setText(translationService.translate(originalDesc, TranslationService.Language.AR));
            descLabel.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
            btnAR.setStyle(pillActive); btnFR.setStyle(pillStyle); btnEN.setStyle(pillStyle);
        });
        langRow.getChildren().addAll(btnFR, btnEN, btnAR);

        // ── Footer ───────────────────────────────────────────────────────────
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(4, 0, 0, 0));

        Button viewRoomsBtn = new Button("Voir Chambres");
        viewRoomsBtn.setStyle(
            "-fx-background-color: #1a8f4e;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 8 18;" +
            "-fx-background-radius: 14;" +
            "-fx-cursor: hand;");
        viewRoomsBtn.setOnMouseEntered(ev -> viewRoomsBtn.setStyle(
            "-fx-background-color: #22b860;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 8 18;" +
            "-fx-background-radius: 14;" +
            "-fx-cursor: hand;"));
        viewRoomsBtn.setOnMouseExited(ev -> viewRoomsBtn.setStyle(
            "-fx-background-color: #1a8f4e;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 8 18;" +
            "-fx-background-radius: 14;" +
            "-fx-cursor: hand;"));
        viewRoomsBtn.setOnAction(e -> goToChambres());

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        String iconBtnStyle =
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-text-fill: #6bcf9a;" +
            "-fx-font-size: 14px;" +
            "-fx-padding: 6 10;" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;";
        String iconBtnHover =
            "-fx-background-color: rgba(80,200,120,0.18);" +
            "-fx-text-fill: #50e896;" +
            "-fx-font-size: 14px;" +
            "-fx-padding: 6 10;" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;";
        String iconBtnDangerStyle =
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-text-fill: #f07070;" +
            "-fx-font-size: 14px;" +
            "-fx-padding: 6 10;" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;";
        String iconBtnDangerHover =
            "-fx-background-color: rgba(220,60,60,0.18);" +
            "-fx-text-fill: #ff6b6b;" +
            "-fx-font-size: 14px;" +
            "-fx-padding: 6 10;" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;";

        Button chatBtn = new Button("🤖");
        chatBtn.setStyle(iconBtnStyle);
        chatBtn.setTooltip(new Tooltip("Assistant IA"));
        chatBtn.setOnMouseEntered(ev -> chatBtn.setStyle(iconBtnHover));
        chatBtn.setOnMouseExited(ev -> chatBtn.setStyle(iconBtnStyle));
        chatBtn.setOnAction(e -> openChatbot(hotel));

        Button editBtn = new Button("✏");
        editBtn.setStyle(iconBtnStyle);
        editBtn.setTooltip(new Tooltip("Modifier"));
        editBtn.setOnMouseEntered(ev -> editBtn.setStyle(iconBtnHover));
        editBtn.setOnMouseExited(ev -> editBtn.setStyle(iconBtnStyle));
        editBtn.setOnAction(e -> editHotel(hotel));

        Button deleteBtn = new Button("🗑");
        deleteBtn.setStyle(iconBtnDangerStyle);
        deleteBtn.setTooltip(new Tooltip("Supprimer"));
        deleteBtn.setOnMouseEntered(ev -> deleteBtn.setStyle(iconBtnDangerHover));
        deleteBtn.setOnMouseExited(ev -> deleteBtn.setStyle(iconBtnDangerStyle));
        deleteBtn.setOnAction(e -> deleteHotel(hotel));

        footer.getChildren().addAll(viewRoomsBtn, footerSpacer, chatBtn, editBtn, deleteBtn);

        // ── ASSEMBLE ─────────────────────────────────────────────────────────
        content.getChildren().addAll(locRow, metaRow, divider, descLabel, langRow, footer);
        card.getChildren().addAll(imageWrapper, content);

        // Hover: lift + brighter border glow
        String baseStyle =
            "-fx-background-color: #0b2a1c;" +
            "-fx-background-radius: 18;" +
            "-fx-border-radius: 18;" +
            "-fx-border-color: rgba(50,180,100,0.22);" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 20, 0, 0, 6);" +
            "-fx-cursor: hand;";
        String hoverStyle =
            "-fx-background-color: #0e3323;" +
            "-fx-background-radius: 18;" +
            "-fx-border-radius: 18;" +
            "-fx-border-color: rgba(60,220,120,0.55);" +
            "-fx-border-width: 1.5;" +
            "-fx-effect: dropshadow(gaussian, rgba(50,200,100,0.3), 28, 0, 0, 8);" +
            "-fx-translate-y: -3;" +
            "-fx-cursor: hand;";
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));

        return card;
    }

    /**
     * 🤖 Open AI Chatbot dialog for the hotel (RAG — no external API)
     */
    private void openChatbot(Hotel hotel) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("🤖 Assistant IA — " + hotel.getNom());
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        DialogPane dp = dialog.getDialogPane();
        dp.getStylesheets().add(getClass().getResource("/styles/unified-styles.css").toExternalForm());

        // ── Layout ─────────────────────────────────────────────
        VBox root = new VBox(15);
        root.setPrefSize(520, 560);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: rgba(1,30,20,0.97); -fx-background-radius: 12;");

        // Header
        Label header = new Label("🤖 Assistant Hôtel GoVibe");
        header.setStyle("-fx-text-fill: #50C878; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label sub = new Label("💬 Posez vos questions sur " + hotel.getNom() + " et nos services");
        sub.setStyle("-fx-text-fill: #A0E0C9; -fx-font-size: 11px;");

        // Chat area
        VBox chatArea = new VBox(10);
        chatArea.setPadding(new Insets(10));
        ScrollPane chatScroll = new ScrollPane(chatArea);
        chatScroll.setFitToWidth(true);
        chatScroll.setStyle("-fx-background: transparent; -fx-background-color: rgba(255,255,255,0.03); " +
                            "-fx-border-color: rgba(80,200,120,0.15); -fx-border-radius: 8;");
        chatScroll.setPrefHeight(380);
        VBox.setVgrow(chatScroll, Priority.ALWAYS);

        // Input row
        HBox inputRow = new HBox(10);
        inputRow.setAlignment(Pos.CENTER);
        TextField inputField = new TextField();
        inputField.setPromptText("Écrivez votre message...");
        inputField.getStyleClass().add("form-field");
        HBox.setHgrow(inputField, Priority.ALWAYS);
        Button sendBtn = new Button("➤ Envoyer");
        sendBtn.getStyleClass().add("premium-button-small");

        // Quick question buttons
        HBox quickBox = new HBox(8);
        quickBox.setAlignment(Pos.CENTER_LEFT);
        Label quickLabel = new Label("💡");
        quickLabel.setStyle("-fx-text-fill: #A0E0C9;");
        String[] quickQuestions = {"Prix des chambres", "Équipements", "Réserver", "Codes promo"};
        for (String q : quickQuestions) {
            Button qBtn = new Button(q);
            qBtn.setStyle("-fx-background-color: rgba(80,200,120,0.1); -fx-text-fill: #A0E0C9; " +
                         "-fx-font-size: 10px; -fx-padding: 4 8; -fx-background-radius: 12; -fx-cursor: hand;");
            qBtn.setOnAction(ev -> {
                inputField.setText(q);
                sendBtn.fire();
            });
            quickBox.getChildren().add(qBtn);
        }
        quickBox.getChildren().add(0, quickLabel);

        inputRow.getChildren().addAll(inputField, sendBtn);
        root.getChildren().addAll(header, sub, chatScroll, quickBox, inputRow);

        // ── Welcome message ────────────────────────────────────
        addChatMessage(chatArea, chatScroll, "🤖", "Bonjour ! Bienvenue chez **" + hotel.getNom() +
                "** à " + hotel.getVille() + ". Je suis votre assistant hôtelier. " +
                "Comment puis-je vous aider ? 😊", false);

        // ── Send action ────────────────────────────────────────
        Runnable sendAction = () -> {
            String msg = inputField.getText().trim();
            if (msg.isBlank()) return;
            addChatMessage(chatArea, chatScroll, "👤", msg, true);
            inputField.clear();

            // Process in background
            String question = msg;
            Task<String> responseTask = new Task<>() {
                @Override protected String call() {
                    if (chatbotRAG == null) {
                        return "⚠️ L'assistant est en cours d'initialisation. Veuillez réessayer dans un moment.";
                    }
                    return chatbotRAG.chat(question);
                }
            };
            responseTask.setOnSucceeded(ev -> {
                Platform.runLater(() ->
                    addChatMessage(chatArea, chatScroll, "🤖", responseTask.getValue(), false));
            });
            new Thread(responseTask, "chatbot-response").start();
        };

        sendBtn.setOnAction(e -> sendAction.run());
        inputField.setOnAction(e -> sendAction.run());

        dp.setContent(root);
        dp.setPrefWidth(560);
        dialog.showAndWait();
    }

    private void addChatMessage(VBox chatArea, ScrollPane chatScroll, String avatar, String text, boolean isUser) {
        HBox row = new HBox(10);
        row.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 8, 2, 8));

        Label avatarLabel = new Label(avatar);
        avatarLabel.setStyle("-fx-font-size: 18px;");

        // Support basic **bold** markdown
        Label msgLabel = new Label(text.replace("**", ""));
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(360);
        msgLabel.setStyle(isUser
            ? "-fx-background-color: rgba(80,200,120,0.2); -fx-text-fill: white; " +
              "-fx-padding: 10 14; -fx-background-radius: 14 14 3 14; -fx-font-size: 12px;"
            : "-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #E0F5EC; " +
              "-fx-padding: 10 14; -fx-background-radius: 14 14 14 3; -fx-font-size: 12px;"
        );

        if (isUser) {
            row.getChildren().addAll(msgLabel, avatarLabel);
        } else {
            row.getChildren().addAll(avatarLabel, msgLabel);
        }

        chatArea.getChildren().add(row);

        // Scroll to bottom
        chatScroll.layout();
        chatScroll.setVvalue(1.0);

        // Fade in
        FadeTransition ft = new FadeTransition(Duration.millis(300), row);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    @FXML
    public void openGlobalChatbot() {
        // Open chatbot without hotel context
        if (chatbotRAG == null) {
            showAlert("Info", "L'assistant est en cours d'initialisation. Quelques secondes...", Alert.AlertType.INFORMATION);
            return;
        }
        openChatbot(new Hotel(0, "GoVibe Hotels", "", "", 5, "", "", 0));
    }

    @FXML
    public void addHotel() {
        Dialog<Hotel> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un Hôtel");
        dialog.setHeaderText("✨ Nouvel Hôtel");

        // Style the dialog
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles/unified-styles.css").toExternalForm());
        dialogPane.getStyleClass().add("form-dialog");

        ButtonType saveButtonType = new ButtonType("💾 Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("❌ Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        // Style buttons
        Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
        saveButton.getStyleClass().add("premium-button");
        Button cancelButton = (Button) dialogPane.lookupButton(cancelButtonType);
        cancelButton.getStyleClass().add("card-action-btn-danger");

        ScrollPane form = createHotelForm(null);
        dialog.getDialogPane().setContent(form);

        // Prevent closing on validation error
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            Hotel hotel = getHotelFromForm(form, null);
            if (hotel == null) {
                event.consume(); // Prevent dialog from closing
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return getHotelFromForm(form, null);
            }
            return null;
        });

        Optional<Hotel> result = dialog.showAndWait();
        result.ifPresent(hotel -> {
            try {
                serviceHotel.insert(hotel);
                loadHotels();
                showAlert("Succès", "Hôtel ajouté avec succès!", Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                showAlert("Erreur", "Erreur lors de l'ajout: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void editHotel(Hotel hotel) {
        Dialog<Hotel> dialog = new Dialog<>();
        dialog.setTitle("Modifier l'Hôtel");
        dialog.setHeaderText("✏️ Modifier: " + hotel.getNom());

        // Style the dialog
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles/unified-styles.css").toExternalForm());
        dialogPane.getStyleClass().add("form-dialog");

        ButtonType saveButtonType = new ButtonType("💾 Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("❌ Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        // Style buttons
        Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
        saveButton.getStyleClass().add("premium-button");
        Button cancelButton = (Button) dialogPane.lookupButton(cancelButtonType);
        cancelButton.getStyleClass().add("card-action-btn-danger");

        ScrollPane form = createHotelForm(hotel);
        dialog.getDialogPane().setContent(form);

        // Prevent closing on validation error
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            Hotel updatedHotel = getHotelFromForm(form, hotel);
            if (updatedHotel == null) {
                event.consume();
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return getHotelFromForm(form, hotel);
            }
            return null;
        });

        Optional<Hotel> result = dialog.showAndWait();
        result.ifPresent(updatedHotel -> {
            try {
                serviceHotel.update(updatedHotel);
                loadHotels();
                showAlert("Succès", "Hôtel modifié avec succès!", Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                showAlert("Erreur", "Erreur lors de la modification: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void deleteHotel(Hotel hotel) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer l'hôtel");
        confirmation.setContentText("Êtes-vous sûr de vouloir supprimer l'hôtel \"" + hotel.getNom() + "\" ?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                serviceHotel.delete(hotel.getId());
                loadHotels();
                showAlert("Succès", "Hôtel supprimé avec succès!", Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                showAlert("Erreur", "Erreur lors de la suppression: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private ScrollPane createHotelForm(Hotel hotel) {
        VBox container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setPrefWidth(500);
        container.getStyleClass().add("form-card-glass");

        Label titleLabel = new Label("✨ Détails de l'Hôtel");
        titleLabel.getStyleClass().add("hero-title");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #0B6E4F;");

        // Nom
        VBox nomBox = new VBox(5);
        Label nomLabel = new Label("Nom de l'hôtel *");
        nomLabel.getStyleClass().add("form-label");
        TextField nomField = new TextField(hotel != null ? hotel.getNom() : "");
        nomField.setPromptText("Ex: Hôtel Royal Palace");
        nomField.setId("nomField");
        nomField.getStyleClass().add("form-field");
        Label nomError = org.example.utils.FormValidator.createErrorLabel();
        nomError.setId("nomError");
        nomBox.getChildren().addAll(nomLabel, nomField, nomError);

        // Adresse
        VBox adresseBox = new VBox(5);
        Label adresseLabel = new Label("Adresse *");
        adresseLabel.getStyleClass().add("form-label");
        TextField adresseField = new TextField(hotel != null ? hotel.getAdresse() : "");
        adresseField.setPromptText("Ex: 123 Avenue Habib Bourguiba");
        adresseField.setId("adresseField");
        adresseField.getStyleClass().add("form-field");
        Label adresseError = org.example.utils.FormValidator.createErrorLabel();
        adresseError.setId("adresseError");
        adresseBox.getChildren().addAll(adresseLabel, adresseField, adresseError);

        // Ville
        VBox villeBox = new VBox(5);
        Label villeLabel = new Label("Ville *");
        villeLabel.getStyleClass().add("form-label");
        TextField villeField = new TextField(hotel != null ? hotel.getVille() : "");
        villeField.setPromptText("Ex: Tunis");
        villeField.setId("villeField");
        villeField.getStyleClass().add("form-field");
        Label villeError = org.example.utils.FormValidator.createErrorLabel();
        villeError.setId("villeError");
        villeBox.getChildren().addAll(villeLabel, villeField, villeError);

        // Étoiles
        VBox etoilesBox = new VBox(5);
        Label etoilesLabel = new Label("Nombre d'étoiles (1-5) *");
        etoilesLabel.getStyleClass().add("form-label");
        Spinner<Integer> etoilesSpinner = new Spinner<>(1, 5, hotel != null ? hotel.getNombreEtoiles() : 3);
        etoilesSpinner.setId("etoilesSpinner");
        etoilesSpinner.getStyleClass().add("form-field");
        etoilesSpinner.setEditable(true);
        etoilesBox.getChildren().addAll(etoilesLabel, etoilesSpinner);

        // Budget
        VBox budgetBox = new VBox(5);
        Label budgetLabel = new Label("Budget (DT) *");
        budgetLabel.getStyleClass().add("form-label");
        TextField budgetField = new TextField(hotel != null ? String.valueOf(hotel.getBudget()) : "");
        budgetField.setPromptText("Ex: 200.00");
        budgetField.setId("budgetField");
        budgetField.getStyleClass().add("form-field");
        Label budgetError = org.example.utils.FormValidator.createErrorLabel();
        budgetError.setId("budgetError");
        budgetBox.getChildren().addAll(budgetLabel, budgetField, budgetError);

        // Description
        VBox descBox = new VBox(5);
        Label descLabel = new Label("Description *");
        descLabel.getStyleClass().add("form-label");
        TextArea descArea = new TextArea(hotel != null ? hotel.getDescription() : "");
        descArea.setPromptText("Décrivez votre hôtel...");
        descArea.setPrefRowCount(3);
        descArea.setId("descArea");
        descArea.getStyleClass().add("form-field");
        Label descError = org.example.utils.FormValidator.createErrorLabel();
        descError.setId("descError");
        descBox.getChildren().addAll(descLabel, descArea, descError);

        // Photo URL + Browse button
        VBox photoBox = new VBox(5);
        Label photoLabel = new Label("Image de l'hôtel (fichier local ou URL)");
        photoLabel.getStyleClass().add("form-label");

        HBox photoRow = new HBox(8);
        photoRow.setAlignment(Pos.CENTER_LEFT);

        TextField photoField = new TextField(hotel != null ? hotel.getPhotoUrl() : "");
        photoField.setPromptText("Choisissez une image ou collez une URL...");
        photoField.setId("photoField");
        photoField.getStyleClass().add("form-field");
        HBox.setHgrow(photoField, Priority.ALWAYS);

        Button browseBtn = new Button("Parcourir...");
        browseBtn.getStyleClass().add("form-button-ghost");
        browseBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choisir une image d'hôtel");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
            );
            File file = fileChooser.showOpenDialog(photoRow.getScene().getWindow());
            if (file != null) {
                // Enregistrer le chemin absolu, qui sera chargé avec "file:" dans createHotelCard
                photoField.setText(file.getAbsolutePath());
            }
        });

        photoRow.getChildren().addAll(photoField, browseBtn);
        photoBox.getChildren().addAll(photoLabel, photoRow);

        // Info text
        Label infoLabel = new Label("* Champs obligatoires");
        infoLabel.getStyleClass().add("form-help");

        GridPane formGrid = new GridPane();
        formGrid.getStyleClass().add("form-grid");
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        col1.setHgrow(Priority.ALWAYS);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        col2.setHgrow(Priority.ALWAYS);
        formGrid.getColumnConstraints().addAll(col1, col2);

        formGrid.add(nomBox, 0, 0);
        formGrid.add(villeBox, 1, 0);

        formGrid.add(adresseBox, 0, 1);
        GridPane.setColumnSpan(adresseBox, 2);

        formGrid.add(etoilesBox, 0, 2);
        formGrid.add(budgetBox, 1, 2);

        formGrid.add(descBox, 0, 3);
        GridPane.setColumnSpan(descBox, 2);

        formGrid.add(photoBox, 0, 4);
        GridPane.setColumnSpan(photoBox, 2);

        container.getChildren().addAll(titleLabel, formGrid, infoLabel);

        // Wrap in ScrollPane
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setPrefHeight(550);
        scrollPane.setMaxHeight(550);

        return scrollPane;
    }

    private Hotel getHotelFromForm(ScrollPane scrollPane, Hotel existingHotel) {
        VBox container = (VBox) scrollPane.getContent();
        TextField nomField = (TextField) container.lookup("#nomField");
        TextField adresseField = (TextField) container.lookup("#adresseField");
        TextField villeField = (TextField) container.lookup("#villeField");
        Spinner<Integer> etoilesSpinner = (Spinner<Integer>) container.lookup("#etoilesSpinner");
        TextField budgetField = (TextField) container.lookup("#budgetField");
        TextArea descArea = (TextArea) container.lookup("#descArea");
        TextField photoField = (TextField) container.lookup("#photoField");

        Label nomError = (Label) container.lookup("#nomError");
        Label adresseError = (Label) container.lookup("#adresseError");
        Label villeError = (Label) container.lookup("#villeError");
        Label budgetError = (Label) container.lookup("#budgetError");
        Label descError = (Label) container.lookup("#descError");

        // Validation
        boolean valid = true;
        valid &= org.example.utils.FormValidator.validateRequired(nomField, nomError, "Le nom");
        valid &= org.example.utils.FormValidator.validateRequired(adresseField, adresseError, "L'adresse");
        valid &= org.example.utils.FormValidator.validateRequired(villeField, villeError, "La ville");
        valid &= org.example.utils.FormValidator.validateDouble(budgetField, budgetError, "Le budget");

        if (descArea.getText() == null || descArea.getText().trim().isEmpty()) {
            org.example.utils.FormValidator.showError(descError, "La description est obligatoire");
            descArea.setStyle("-fx-border-color: #E74C3C; -fx-border-width: 2;");
            valid = false;
        } else {
            org.example.utils.FormValidator.hideError(descError);
            descArea.setStyle("");
        }

        if (!valid) {
            return null;
        }

        if (existingHotel != null) {
            existingHotel.setNom(nomField.getText().trim());
            existingHotel.setAdresse(adresseField.getText().trim());
            existingHotel.setVille(villeField.getText().trim());
            existingHotel.setNombreEtoiles(etoilesSpinner.getValue());
            existingHotel.setBudget(Double.parseDouble(budgetField.getText().trim()));
            existingHotel.setDescription(descArea.getText().trim());
            existingHotel.setPhotoUrl(photoField.getText().trim());
            return existingHotel;
        } else {
            return new Hotel(
                nomField.getText().trim(),
                adresseField.getText().trim(),
                villeField.getText().trim(),
                etoilesSpinner.getValue(),
                descArea.getText().trim(),
                photoField.getText().trim(),
                Double.parseDouble(budgetField.getText().trim())
            );
        }
    }

    @FXML
    public void searchHotels() {
        String searchText = searchField.getText().toLowerCase();
        if (searchText.isEmpty()) {
            displayHotelCards();
            return;
        }

        hotelCardsContainer.getChildren().clear();
        for (Hotel hotel : hotelList) {
            if (hotel.getNom().toLowerCase().contains(searchText) ||
                hotel.getVille().toLowerCase().contains(searchText)) {
                hotelCardsContainer.getChildren().add(createHotelCard(hotel));
            }
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ===== Navigation Methods =====

    @FXML
    public void goToHotels() {
        // Already on hotels page - refresh
        loadHotels();
    }

    @FXML
    public void goToChambres() {
        navigateTo("/chambre-view.fxml");
    }

    @FXML
    public void goToReservations() {
        navigateTo("/reservation-view.fxml");
    }

    @FXML
    public void backToDashboard() {
        navigateTo("/org/example/AdminDashboardView.fxml");
    }

    private void navigateTo(String fxmlPath) {
        try {
            Stage stage = (Stage) hotelCardsContainer.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de naviguer: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}

