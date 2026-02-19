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
    @FXML private Button btnNouveauMessage;
    @FXML private TableView<Contact> tableMessages;
    @FXML private TableColumn<Contact, String> colSujet;
    @FXML private TableColumn<Contact, String> colMessage;
    @FXML private TableColumn<Contact, String> colStatus;
    @FXML private TableColumn<Contact, String> colDate;
    @FXML private TableColumn<Contact, String> colReponse;
    @FXML private Button btnVoirDetails;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;

    private final ServiceContact serviceContact = new ServiceContact();
    private final ObservableList<Contact> data = FXCollections.observableArrayList();
    private personne currentUser;

    @FXML
    public void initialize() {
        // Vérifier si utilisateur connecté
        currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Vous devez être connecté pour voir vos messages.");
            goToLogin();
            return;
        }

        // Configurer les colonnes
        colSujet.setCellValueFactory(new PropertyValueFactory<>("sujet"));
        colMessage.setCellValueFactory(new PropertyValueFactory<>("message"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDate.setCellValueFactory(cell -> {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            return new javafx.beans.property.SimpleStringProperty(
                cell.getValue().getDateEnvoi() != null ? 
                sdf.format(cell.getValue().getDateEnvoi()) : ""
            );
        });
        colReponse.setCellValueFactory(cell -> {
            return new javafx.beans.property.SimpleStringProperty(
                cell.getValue().hasReponse() ? "✅ Répondu" : "⏳ En attente"
            );
        });

        // Style des status
        colStatus.setCellFactory(column -> new TableCell<Contact, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "EN_ATTENTE" -> setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                        case "LU" -> setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                        case "REPONDU" -> setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        case "FERME" -> setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
                        default -> setStyle("");
                    }
                }
            }
        });

        tableMessages.setItems(data);
        chargerMessages();

        // Désactiver les boutons si pas de sélection
        tableMessages.getSelectionModel().selectedItemProperty().addListener((obs, old, nw) -> {
            boolean hasSelection = nw != null;
            boolean canEdit = hasSelection && Contact.STATUS_EN_ATTENTE.equals(nw.getStatus());
            btnModifier.setDisable(!canEdit);
            btnSupprimer.setDisable(!canEdit);
            btnVoirDetails.setDisable(!hasSelection);
        });

        btnModifier.setDisable(true);
        btnSupprimer.setDisable(true);
        btnVoirDetails.setDisable(true);
    }

    private void chargerMessages() {
        try {
            data.clear();
            data.addAll(serviceContact.getMessagesByUser(currentUser.getId()));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les messages: " + e.getMessage());
        }
    }

    @FXML
    private void retourProfil() {
        safeNavigate("/org/example/UserProfileView.fxml");
    }

    @FXML
    private void nouveauMessage() {
        safeNavigate("/NouveauMessage.fxml");
    }

    @FXML
    private void voirDetails() {
        Contact selected = tableMessages.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un message.");
            return;
        }
        // Ouvrir une boîte de dialogue avec les détails
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails du Message");
        alert.setHeaderText(selected.getSujet());

        String content = "📧 Votre Message:\n" + selected.getMessage() + "\n\n";
        if (selected.hasReponse()) {
            content += "✅ Réponse de l'Admin:\n" + selected.getReponse();
        } else {
            content += "⏳ En attente de réponse...";
        }

        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void modifierMessage() {
        Contact selected = tableMessages.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un message.");
            return;
        }

        if (!Contact.STATUS_EN_ATTENTE.equals(selected.getStatus())) {
            showAlert(Alert.AlertType.WARNING, "Impossible", 
                "Vous ne pouvez modifier que les messages en attente.");
            return;
        }

        // Ouvrir une boîte de dialogue pour modifier
        Dialog<Contact> dialog = new Dialog<>();
        dialog.setTitle("Modifier le Message");
        dialog.setHeaderText("Modifier votre message");

        ButtonType saveButton = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        // Champs
        TextField txtSujet = new TextField(selected.getSujet());
        TextArea txtMessage = new TextArea(selected.getMessage());
        txtMessage.setPrefRowCount(5);
        txtMessage.setPrefColumnCount(30);

        VBox vbox = new VBox(10,
            new Label("Sujet:"), txtSujet,
            new Label("Message:"), txtMessage
        );
        dialog.getDialogPane().setContent(vbox);

        dialog.setResultConverter(btn -> {
            if (btn == saveButton) {
                selected.setSujet(txtSujet.getText());
                selected.setMessage(txtMessage.getText());
                return selected;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(contact -> {
            try {
                serviceContact.modifierMessage(contact);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Message modifié avec succès!");
                chargerMessages();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de modifier: " + e.getMessage());
            }
        });
    }

    @FXML
    private void supprimerMessage() {
        Contact selected = tableMessages.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un message.");
            return;
        }

        if (!Contact.STATUS_EN_ATTENTE.equals(selected.getStatus())) {
            showAlert(Alert.AlertType.WARNING, "Impossible", 
                "Vous ne pouvez supprimer que les messages en attente.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer ce message ?");
        confirm.setContentText("Cette action est irréversible.");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    serviceContact.supprimerMessage(selected.getId(), currentUser.getId());
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Message supprimé!");
                    chargerMessages();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer: " + e.getMessage());
                }
            }
        });
    }

    private void safeNavigate(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation", "Impossible de charger la page: " + e.getMessage());
        }
    }

    private void goToLogin() {
        safeNavigate("/org/example/LoginView.fxml");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
