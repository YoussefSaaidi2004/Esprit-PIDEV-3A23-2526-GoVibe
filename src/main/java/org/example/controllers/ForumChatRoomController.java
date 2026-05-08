package org.example.controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.example.services.GeminiChatService;
import org.example.services.GeminiChatService.ChatMessage;

import java.util.ArrayList;
import java.util.List;

import org.example.assistant.PythonVoiceAgent;

public class ForumChatRoomController {

    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox messagesContainer;
    @FXML private TextField inputField;
    @FXML private Button sendButton;
    @FXML private HBox typingIndicator;
    @FXML private VBox suggestionsPanel;
    @FXML private Label statusLabel;
    @FXML private Label dot1, dot2, dot3;
    @FXML private ImageView bgImageView;
    @FXML private StackPane rootStack;

    private Timeline typingAnimation;
    
    // Direct Gemini Integration
    private final GeminiChatService geminiService = new GeminiChatService();
    private final List<ChatMessage> chatHistory = new ArrayList<>();

    @FXML
    public void initialize() {
        setupBackground();
        startTypingAnimation();

        // Welcome message from the independent AI
        Platform.runLater(() -> addAiBubble(
                "Bonjour! Je suis votre guide de voyage personnel dévoué ! 🌍✈️\n\n" +
                "Posez-moi vos questions sur les destinations fascinantes du monde entier.\n" +
                "Je suis là pour vous aider avec passion et enthousiasme ! 😊"
        ));
    }

    @FXML
    private void handleSend() {
        String text = inputField.getText().trim();
        if (text.isBlank()) return;

        inputField.clear();
        sendButton.setDisable(true);

        // Hide suggestions after first interaction
        if (suggestionsPanel.isVisible()) {
            suggestionsPanel.setVisible(false);
            suggestionsPanel.setManaged(false);
        }

        // Add user bubble
        addUserBubble(text);

        // Show typing indicator
        showTyping(true);

        // Call Gemini API in background without using the Python Agent
        new Thread(() -> {
            try {
                // Get AI response
                String responseText = geminiService.sendMessage(chatHistory, text);

                Platform.runLater(() -> {
                    showTyping(false);
                    
                    // Add AI response to UI
                    addAiBubble(responseText);
                    
                    // Add to history
                    chatHistory.add(new ChatMessage("user", text));
                    chatHistory.add(new ChatMessage("model", responseText));
                    
                    sendButton.setDisable(false);
                    scrollToBottom();
                });
            } catch (Exception e) {
                e.printStackTrace();
                // Gemini unreachable — silently fall back to local RAG
                String ragResponse = geminiService.getFallback().chat(text);
                Platform.runLater(() -> {
                    showTyping(false);
                    addAiBubble(ragResponse);
                    chatHistory.add(new ChatMessage("user", text));
                    chatHistory.add(new ChatMessage("model", ragResponse));
                    sendButton.setDisable(false);
                    scrollToBottom();
                });
            }
        }).start();
    }

    @FXML
    private void handleSuggestion(ActionEvent event) {
        Button src = (Button) event.getSource();
        // Strip leading emoji
        String text = src.getText().replaceAll("^[^a-zA-ZÀ-ÿ]+", "").trim();
        inputField.setText("Parle-moi de " + text);
        handleSend();
    }

    @FXML
    private void handleBack() {
        org.example.mains.MainApp.switchScene("/poste-forumviews/ListForum.fxml", "Forums");
    }

    // ─── Bubble Builders ──────────────────────────────────────────────────────

    private void addUserBubble(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);

        VBox bubble = new VBox(4);
        bubble.setMaxWidth(500);
        bubble.getStyleClass().add("chat-bubble-user");

        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.getStyleClass().add("chat-bubble-text-user");
        bubble.getChildren().add(msg);

        row.getChildren().add(bubble);
        animateIn(row, true);
        messagesContainer.getChildren().add(row);
        scrollToBottom();
    }

    private void addAiBubble(String text) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        // Voya avatar
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("voya-avatar-tiny");
        Label globe = new Label("🌍");
        globe.setStyle("-fx-font-size: 16;");
        avatar.getChildren().add(globe);

        VBox bubble = new VBox(4);
        bubble.setMaxWidth(520);
        bubble.getStyleClass().add("chat-bubble-ai");

        // Parse markdown-lite (bold **)
        Label msg = new Label(text.replace("**", ""));
        msg.setWrapText(true);
        msg.getStyleClass().add("chat-bubble-text-ai");
        bubble.getChildren().add(msg);

        row.getChildren().addAll(avatar, bubble);
        animateIn(row, false);
        messagesContainer.getChildren().add(row);
        scrollToBottom();
    }

    private void animateIn(HBox row, boolean fromRight) {
        row.setOpacity(0);
        row.setTranslateX(fromRight ? 40 : -40);
        FadeTransition ft = new FadeTransition(Duration.millis(350), row);
        ft.setToValue(1.0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(350), row);
        tt.setToX(0);
        tt.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, tt).play();
    }

    // ─── Typing Indicator ─────────────────────────────────────────────────────

    private void showTyping(boolean show) {
        typingIndicator.setVisible(show);
        typingIndicator.setManaged(show);
        if (show) scrollToBottom();
    }

    private void startTypingAnimation() {
        typingAnimation = new Timeline(
                new KeyFrame(Duration.ZERO, e -> {
                    dot1.setStyle("-fx-opacity: 1; -fx-text-fill: #50C878;");
                    dot2.setStyle("-fx-opacity: 0.3; -fx-text-fill: #50C878;");
                    dot3.setStyle("-fx-opacity: 0.3; -fx-text-fill: #50C878;");
                }),
                new KeyFrame(Duration.millis(400), e -> {
                    dot1.setStyle("-fx-opacity: 0.3; -fx-text-fill: #50C878;");
                    dot2.setStyle("-fx-opacity: 1; -fx-text-fill: #50C878;");
                    dot3.setStyle("-fx-opacity: 0.3; -fx-text-fill: #50C878;");
                }),
                new KeyFrame(Duration.millis(800), e -> {
                    dot1.setStyle("-fx-opacity: 0.3; -fx-text-fill: #50C878;");
                    dot2.setStyle("-fx-opacity: 0.3; -fx-text-fill: #50C878;");
                    dot3.setStyle("-fx-opacity: 1; -fx-text-fill: #50C878;");
                }),
                new KeyFrame(Duration.millis(1200))
        );
        typingAnimation.setCycleCount(Timeline.INDEFINITE);
        typingAnimation.play();
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    private void scrollToBottom() {
        Platform.runLater(() ->
            chatScrollPane.setVvalue(1.0));
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
}
