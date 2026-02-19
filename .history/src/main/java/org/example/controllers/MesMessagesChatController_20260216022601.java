package org.example.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.example.entities.Conversation;
import org.example.entities.Message;
import org.example.entities.personne;
import org.example.services.ServiceConversation;
import org.example.utils.SessionManager;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

public class MesMessagesController {

    @FXML private Button btnRetour;
    @FXML private Button btnNouvelleConv;
    @FXML private VBox conversationsList;
    @FXML private HBox chatHeader;
    @FXML private Circle avatarCircle;
    @FXML private Label lblClientName;
    @FXML private Label lblStatus;
    @FXML private Button btnFermerConv;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private VBox messagesContainer;
    @FXML private TextField txtMessageInput;
    @FXML private Button btnEnvoyer;

    private final ServiceConversation service = new ServiceConversation();
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm");
    private personne currentUser;
    private Conversation currentConversation;
    private ObservableList<Conversation> conversations = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Vous devez être connecté.");
            retourProfil();
            return;
        }

        lblClientName.setText("Admin GoVibe");
        lblStatus.setText("En ligne");
        
        loadConversations();
        
        // Rafraîchir automatiquement toutes les 5 secondes
        startAutoRefresh();
    }

    private void loadConversations() {
        try {
            conversations.clear();
            conversations.addAll(service.getConversationsClient(currentUser.getId()));
            renderConversationsList();
            
            // Sélectionner automatiquement la première conversation active
            if (!conversations.isEmpty() && currentConversation == null) {
                selectConversation(conversations.get(0));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void renderConversationsList() {
        conversationsList.getChildren().clear();
        
        for (Conversation conv : conversations) {
            HBox card = createConversationCard(conv);
            conversationsList.getChildren().add(card);
        }
    }

    private HBox createConversationCard(Conversation conv) {
        HBox card = new HBox(10);
        card.setPadding(new Insets(12, 15, 12, 15));
        card.setStyle("-fx-background-color: " + (conv == currentConversation ? "#dcf8c6" : "white") + 
                    "; -fx-cursor: hand; -fx-border-radius: 8; -fx-background-radius: 8;" +
                    (conv.getNonLusCount() > 0 ? " -fx-border-color: #50C878; -fx-border-width: 2;" : ""));
        
        // Avatar
        Circle avatar = new Circle(25);
        avatar.setFill(Color.web("#50C878"));
        
        // Infos
        VBox info = new VBox(3);
        info.setAlignment(Pos.CENTER_LEFT);
        
        HBox topRow = new HBox(10);
        Label lblNom = new Label("Admin");
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
            Label badge = new Label(String.valueOf(conv.getNonLusCount()));
            badge.setStyle("-fx-background-color: #50C878; -fx-text-fill: white; -fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;");
            bottomRow.getChildren().add(badge);
        }
        
        info.getChildren().addAll(topRow, lblSujet, bottomRow);
        card.getChildren().addAll(avatar, info);
        
        // Click handler
        card.setOnMouseClicked(e -> selectConversation(conv));
        
        return card;
    }

    private void selectConversation(Conversation conv) {
        this.currentConversation = conv;
        renderConversationsList(); // Refresh to update selection highlight
        loadMessages();
        
        // Marquer comme lus
        try {
            service.marquerMessagesLus(conv.getId(), Message.SENDER_CLIENT);
            conv.setNonLusCount(0);
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
            
            // Scroll to bottom
            Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private HBox createMessageBubble(Message msg) {
        boolean isClient = msg.isFromClient();
        
        HBox container = new HBox();
        container.setAlignment(isClient ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        container.setPadding(new Insets(2, 0, 2, 0));
        
        VBox bubble = new VBox(3);
        bubble.setPadding(new Insets(10, 15, 10, 15));
        bubble.setStyle("-fx-background-color: " + (isClient ? "#dcf8c6" : "white") + 
                       "; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 1);");
        bubble.setMaxWidth(500);
        
        Label lblContent = new Label(msg.getContent());
        lblContent.setStyle("-fx-font-size: 14px; -fx-wrap-text: true;");
        lblContent.setMaxWidth(450);
        
        HBox footer = new HBox(5);
        footer.setAlignment(Pos.CENTER_RIGHT);
        
        Label lblTime = new Label(sdf.format(msg.getCreatedAt()));
        lblTime.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");
        
        footer.getChildren().add(lblTime);
        
        if (isClient) {
            Label lblStatus = new Label(msg.isLu() ? "✓✓" : "✓");
            lblStatus.setStyle("-fx-text-fill: " + (msg.isLu() ? "#34b7f1" : "#999") + "; -fx-font-size: 11px;");
            footer.getChildren().add(lblStatus);
        }
        
        bubble.getChildren().addAll(lblContent, footer);
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
        
        Message msg = new Message(currentConversation.getId(), currentUser.getId(), Message.SENDER_CLIENT, content);
        
        try {
            service.envoyerMessage(msg);
            txtMessageInput.clear();
            loadMessages();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'envoyer: " + e.getMessage());
        }
    }

    @FXML
    private void nouvelleConversation() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nouvelle Conversation");
        dialog.setHeaderText("Démarrer une conversation avec l'admin");
        dialog.setContentText("Sujet:");
        
        dialog.showAndWait().ifPresent(sujet -> {
            if (sujet.trim().isEmpty()) return;
            
            try {
                Conversation conv = service.creerConversation(currentUser.getId(), sujet.trim());
                conversations.add(0, conv);
                selectConversation(conv);
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de créer: " + e.getMessage());
            }
        });
    }

    @FXML
    private void fermerConversation() {
        if (currentConversation == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Fermer");
        confirm.setContentText("Fermer cette conversation ?");
        
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    service.fermerConversation(currentConversation.getId());
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
    private void retourProfil() {
        safeNavigate("/org/example/UserHomeView.fxml");
    }

    private void safeNavigate(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation", "Impossible: " + e.getMessage());
        }
    }

    private void startAutoRefresh() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000);
                    Platform.runLater(this::loadMessages);
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
