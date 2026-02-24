package org.example.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.example.entities.Conversation;
import org.example.entities.Message;
import org.example.services.ServiceConversation;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

public class AdminMessagesChatController {

    @FXML private AdminSidebarController adminSidebarController;
    @FXML private Label lblTotalNonLus;
    @FXML private ComboBox<String> comboFiltre;
    @FXML private TextField txtRecherche;
    @FXML private VBox conversationsList;
    @FXML private Circle clientAvatar;
    @FXML private Label lblClientNom;
    @FXML private Label lblClientEmail;
    @FXML private Button btnArchiver;
    @FXML private Button btnSupprimerConv;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private VBox messagesContainer;
    @FXML private TextField txtMessageInput;
    @FXML private Button btnEnvoyer;

    private final ServiceConversation service = new ServiceConversation();
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm");
    private ObservableList<Conversation> conversations = FXCollections.observableArrayList();
    private Conversation currentConversation;

    @FXML
    public void initialize() {
        if (adminSidebarController != null) {
            adminSidebarController.setActivePage("messages");
        }

        // Configurer filtre
        comboFiltre.setItems(FXCollections.observableArrayList("Toutes", "Non lus", "Archivées"));
        comboFiltre.setValue("Toutes");
        comboFiltre.setOnAction(e -> loadConversations());

        txtRecherche.textProperty().addListener((obs, old, nw) -> filterConversations(nw));

        lblClientNom.setText("Sélectionnez un client");
        lblClientEmail.setText("");

        loadConversations();
        startAutoRefresh();
    }

    private void loadConversations() {
        try {
            conversations.clear();
            conversations.addAll(service.getAllConversationsAdmin());
            renderConversationsList();
            updateTotalNonLus();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateTotalNonLus() {
        int total = conversations.stream().mapToInt(Conversation::getNonLusCount).sum();
        lblTotalNonLus.setText(String.valueOf(total));
        lblTotalNonLus.setVisible(total > 0);
    }

    private void filterConversations(String search) {
        renderConversationsList();
    }

    private void renderConversationsList() {
        conversationsList.getChildren().clear();
        String filter = txtRecherche.getText().toLowerCase();
        String statusFilter = comboFiltre.getValue();

        for (Conversation conv : conversations) {
            // Appliquer filtres
            if (!filter.isEmpty()) {
                String clientName = (conv.getClientPrenom() + " " + conv.getClientNom()).toLowerCase();
                if (!clientName.contains(filter)) continue;
            }

            if ("Non lus".equals(statusFilter) && conv.getNonLusCount() == 0) continue;

            HBox card = createConversationCard(conv);
            conversationsList.getChildren().add(card);
        }
    }

    private HBox createConversationCard(Conversation conv) {
        HBox card = new HBox(10);
        card.setPadding(new Insets(12, 15, 12, 15));
        card.setStyle("-fx-background-color: " + (conv == currentConversation ? "#dcf8c6" : "white") +
                    "; -fx-cursor: hand; -fx-border-radius: 8; -fx-background-radius: 8;" +
                    (conv.getNonLusCount() > 0 ? " -fx-border-color: #e74c3c; -fx-border-width: 2;" : ""));

        // Avatar
        Circle avatar = new Circle(25);
        avatar.setFill(Color.web(conv.getNonLusCount() > 0 ? "#e74c3c" : "#50C878"));

        // Infos
        VBox info = new VBox(3);
        info.setAlignment(Pos.CENTER_LEFT);

        HBox topRow = new HBox(10);
        Label lblNom = new Label(conv.getClientPrenom() + " " + conv.getClientNom());
        lblNom.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label lblDate = new Label(sdf.format(conv.getLastMessageAt()));
        lblDate.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        topRow.getChildren().addAll(lblNom, spacer, lblDate);

        Label lblSujet = new Label(conv.getSujet() != null ? conv.getSujet() : "Conversation");
        lblSujet.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        lblSujet.setMaxWidth(200);

        // Badge non lus
        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.CENTER_RIGHT);
        if (conv.getNonLusCount() > 0) {
            Label badge = new Label(conv.getNonLusCount() + " nouveau" + (conv.getNonLusCount() > 1 ? "x" : ""));
            badge.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;");
            bottomRow.getChildren().add(badge);
        }

        info.getChildren().addAll(topRow, lblSujet, bottomRow);
        card.getChildren().addAll(avatar, info);

        card.setOnMouseClicked(e -> selectConversation(conv));

        return card;
    }

    private void selectConversation(Conversation conv) {
        this.currentConversation = conv;
        renderConversationsList();

        lblClientNom.setText(conv.getClientPrenom() + " " + conv.getClientNom());
        lblClientEmail.setText(conv.getClientEmail());

        loadMessages();

        // Marquer comme lus
        try {
            service.marquerMessagesLus(conv.getId(), Message.SENDER_ADMIN);
            conv.setNonLusCount(0);
            updateTotalNonLus();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadMessages() {
        if (currentConversation == null) return;

        messagesContainer.getChildren().clear();

        try {
            List<Message> messages = service.getMessagesConversation(currentConversation.getId());

            for (Message msg : messages) {
                HBox bubble = createMessageBubble(msg);
                messagesContainer.getChildren().add(bubble);
            }

            Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private HBox createMessageBubble(Message msg) {
        boolean isAdmin = msg.isFromAdmin();

        HBox container = new HBox();
        container.setAlignment(isAdmin ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        container.setPadding(new Insets(2, 0, 2, 0));

        VBox bubble = new VBox(3);
        bubble.setPadding(new Insets(10, 15, 10, 15));
        bubble.setStyle("-fx-background-color: " + (isAdmin ? "#dcf8c6" : "white") +
                       "; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 1);");
        bubble.setMaxWidth(500);

        // Nom expéditeur
        Label lblNom = new Label(msg.getSenderNomComplet());
        lblNom.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #666;");

        Label lblContent = new Label(msg.getContent());
        lblContent.setStyle("-fx-font-size: 14px; -fx-wrap-text: true;");
        lblContent.setMaxWidth(450);

        HBox footer = new HBox(5);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Label lblTime = new Label(sdf.format(msg.getCreatedAt()));
        lblTime.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");

        footer.getChildren().add(lblTime);

        if (isAdmin) {
            Label lblStatus = new Label(msg.isLu() ? "✓✓ Lu" : "✓ Envoyé");
            lblStatus.setStyle("-fx-text-fill: " + (msg.isLu() ? "#34b7f1" : "#999") + "; -fx-font-size: 11px;");
            footer.getChildren().add(lblStatus);
        }

        bubble.getChildren().addAll(lblNom, lblContent, footer);
        container.getChildren().add(bubble);

        return container;
    }

    @FXML
    private void envoyerMessage() {
        if (currentConversation == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Sélectionnez une conversation.");
            return;
        }

        String content = txtMessageInput.getText().trim();
        if (content.isEmpty()) return;

        // Récupérer l'ID admin depuis SessionManager
        int adminId = 1; // TODO: Récupérer depuis SessionManager

        Message msg = new Message(currentConversation.getId(), adminId, Message.SENDER_ADMIN, content);

        try {
            service.envoyerMessage(msg);
            txtMessageInput.clear();
            loadMessages();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'envoyer: " + e.getMessage());
        }
    }

    @FXML
    private void archiverConversation() {
        if (currentConversation == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Archiver");
        confirm.setContentText("Archiver cette conversation ?");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    service.archiverConversation(currentConversation.getId());
                    conversations.remove(currentConversation);
                    currentConversation = null;
                    loadConversations();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void supprimerConversation() {
        if (currentConversation == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer");
        confirm.setHeaderText("⚠️ Supprimer définitivement ?");
        confirm.setContentText("Cette action est irréversible.");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    service.supprimerConversation(currentConversation.getId());
                    conversations.remove(currentConversation);
                    currentConversation = null;
                    loadConversations();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void startAutoRefresh() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(3000);
                    Platform.runLater(() -> {
                        loadConversations();
                        if (currentConversation != null) {
                            loadMessages();
                        }
                    });
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
