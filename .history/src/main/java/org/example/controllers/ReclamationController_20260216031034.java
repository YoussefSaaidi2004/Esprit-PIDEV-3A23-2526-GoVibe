package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.example.entities.Reclamation;
import org.example.entities.personne;
import org.example.services.ServiceReclamation;
import org.example.utils.SceneNavigator;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

public class ReclamationController {

    @FXML private ComboBox<String> comboSujet;
    @FXML private TextField txtAutreSujet;
    @FXML private TextArea txtMessage;
    @FXML private Label lblCharCount;
    @FXML private ComboBox<String> comboFiltre;
    @FXML private VBox reclamationsList;
    @FXML private VBox emptyState;

    private final ServiceReclamation service = new ServiceReclamation();
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private personne currentUser;

    @FXML
    public void initialize() {
        // Setup sujet combobox
        comboSujet.setItems(FXCollections.observableArrayList(
            "Problème avec une réservation",
            "Activité non conforme",
            "Problème de paiement",
            "Compte utilisateur",
            "Bug technique",
            "Autre"
        ));
        
        comboSujet.setOnAction(e -> {
            txtAutreSujet.setVisible("Autre".equals(comboSujet.getValue()));
        });

        // Setup character count
        txtMessage.textProperty().addListener((obs, old, nw) -> {
            int len = nw != null ? nw.length() : 0;
            lblCharCount.setText(len + " / 1000 caractères");
            if (len > 1000) {
                txtMessage.setText(nw.substring(0, 1000));
            }
        });

        // Setup filtre
        comboFiltre.setItems(FXCollections.observableArrayList("Toutes", "En attente", "En cours", "Résolues", "Rejetées"));
        comboFiltre.setOnAction(e -> loadReclamations());

        // Load current user and reclamations
        currentUser = SceneNavigator.getCurrentUser();
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez vous connecter.");
            return;
        }
        
        loadReclamations();
    }

    private void loadReclamations() {
        try {
            List<Reclamation> list = service.getByUser(currentUser.getId());
            
            // Apply filter
            String filter = comboFiltre.getValue();
            if (filter != null && !"Toutes".equals(filter)) {
                list.removeIf(r -> !matchesFilter(r, filter));
            }

            reclamationsList.getChildren().clear();
            
            if (list.isEmpty()) {
                emptyState.setVisible(true);
            } else {
                emptyState.setVisible(false);
                for (Reclamation r : list) {
                    reclamationsList.getChildren().add(createReclamationCard(r));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger vos réclamations.");
        }
    }

    private boolean matchesFilter(Reclamation r, String filter) {
        switch (filter) {
            case "En attente": return Reclamation.STATUS_EN_ATTENTE.equals(r.getStatus());
            case "En cours": return Reclamation.STATUS_EN_COURS.equals(r.getStatus());
            case "Résolues": return Reclamation.STATUS_RESOLU.equals(r.getStatus());
            case "Rejetées": return Reclamation.STATUS_REJETE.equals(r.getStatus());
            default: return true;
        }
    }

    private VBox createReclamationCard(Reclamation r) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        
        String statusColor = getStatusColor(r.getStatus());
        
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 4); " +
                     "-fx-border-color: " + statusColor + "; -fx-border-width: 2; -fx-border-radius: 12;");

        // Header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Circle statusCircle = new Circle(8);
        statusCircle.setFill(Color.web(statusColor));
        
        Label lblSujet = new Label(r.getSujet());
        lblSujet.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #013220;");
        
        Label lblDate = new Label(sdf.format(r.getDateEnvoi()));
        lblDate.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");
        
        Label lblStatus = new Label(r.getStatus());
        lblStatus.setStyle("-fx-background-color: " + statusColor + "; -fx-text-fill: white; " +
                          "-fx-padding: 4 12; -fx-background-radius: 15; -fx-font-size: 12; -fx-font-weight: bold;");
        
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        header.getChildren().addAll(statusCircle, lblSujet, spacer, lblDate, lblStatus);

        // Message
        Label lblMessage = new Label(r.getMessage());
        lblMessage.setStyle("-fx-font-size: 14; -fx-text-fill: #333;");
        lblMessage.setWrapText(true);

        card.getChildren().addAll(header, lblMessage);

        // Response section if exists
        if (r.getReponse() != null && !r.getReponse().isEmpty()) {
            VBox responseBox = new VBox(8);
            responseBox.setPadding(new Insets(15));
            responseBox.setStyle("-fx-background-color: #E8F8F5; -fx-background-radius: 8;");
            
            Label lblRepTitle = new Label("💬 Réponse de l'administration");
            lblRepTitle.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #013220;");
            
            Label lblReponse = new Label(r.getReponse());
            lblReponse.setStyle("-fx-font-size: 14; -fx-text-fill: #333;");
            lblReponse.setWrapText(true);
            
            if (r.getDateReponse() != null) {
                Label lblRepDate = new Label("Répondu le: " + sdf.format(r.getDateReponse()));
                lblRepDate.setStyle("-fx-font-size: 11; -fx-text-fill: #666;");
                responseBox.getChildren().addAll(lblRepTitle, lblReponse, lblRepDate);
            } else {
                responseBox.getChildren().addAll(lblRepTitle, lblReponse);
            }
            
            card.getChildren().add(responseBox);
        }

        // Delete button for pending reclamations
        if (r.isEnAttente()) {
            Button btnDelete = new Button("🗑️ Supprimer");
            btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 15; " +
                              "-fx-padding: 8 15; -fx-cursor: hand; -fx-font-size: 12;");
            btnDelete.setOnAction(e -> supprimerReclamation(r));
            
            HBox actions = new HBox(btnDelete);
            actions.setAlignment(Pos.CENTER_RIGHT);
            card.getChildren().add(actions);
        }

        return card;
    }

    private String getStatusColor(String status) {
        switch (status) {
            case Reclamation.STATUS_EN_ATTENTE: return "#f39c12";
            case Reclamation.STATUS_EN_COURS: return "#3498db";
            case Reclamation.STATUS_RESOLU: return "#27ae60";
            case Reclamation.STATUS_REJETE: return "#e74c3c";
            default: return "#95a5a6";
        }
    }

    @FXML
    private void handleEnvoyer() {
        String sujet = comboSujet.getValue();
        if ("Autre".equals(sujet)) {
            sujet = txtAutreSujet.getText().trim();
        }
        String message = txtMessage.getText().trim();

        if (sujet == null || sujet.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un sujet.");
            return;
        }

        if (message.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez décrire votre problème.");
            return;
        }

        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez vous connecter.");
            return;
        }

        Reclamation r = new Reclamation();
        r.setUserId(currentUser.getId());
        r.setSujet(sujet);
        r.setMessage(message);
        r.setCreatedByUser(currentUser.getId());

        try {
            service.ajouter(r);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Votre réclamation a été envoyée!");
            
            // Clear form
            comboSujet.setValue(null);
            txtAutreSujet.clear();
            txtAutreSujet.setVisible(false);
            txtMessage.clear();
            
            // Refresh list
            loadReclamations();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'envoyer la réclamation: " + e.getMessage());
        }
    }

    private void supprimerReclamation(Reclamation r) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer cette réclamation ?");
        confirm.setContentText("Cette action est irréversible.");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    service.supprimer(r.getId());
                    loadReclamations();
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer la réclamation.");
                }
            }
        });
    }

    @FXML
    private void handleRetour() {
        SceneNavigator.switchTo("/org/example/UserHomeView.fxml", null);
    }

    @FXML
    private void handleHome() {
        SceneNavigator.switchTo("/org/example/UserHomeView.fxml", null);
    }

    @FXML
    private void handleActivities() {
        SceneNavigator.switchTo("/org/example/UserHomeView.fxml", null);
    }

    @FXML
    private void handleLocations() {
        SceneNavigator.switchTo("/VoitureListView.fxml", null);
    }

    @FXML
    private void handleFlights() {
        SceneNavigator.switchTo("/views/flight-management.fxml", null);
    }

    @FXML
    private void handleChambres() {
        SceneNavigator.switchTo("/main-layout.fxml", null);
    }

    @FXML
    private void handleForums() {
        SceneNavigator.switchTo("/poste-forumviews/ListForum.fxml", null);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
