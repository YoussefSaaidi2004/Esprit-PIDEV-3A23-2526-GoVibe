package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.entities.Contact;
import org.example.entities.personne;
import org.example.services.ServiceContact;
import org.example.utils.SessionManager;

import java.sql.SQLException;

public class NouveauMessageController {

    @FXML private TextField txtSujet;
    @FXML private TextArea txtMessage;
    @FXML private Button btnEnvoyer;
    @FXML private Button btnAnnuler;

    private final ServiceContact serviceContact = new ServiceContact();
    private personne currentUser;

    @FXML
    public void initialize() {
        currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Vous devez être connecté.");
            retourMesMessages();
        }
    }

    @FXML
    private void envoyerMessage() {
        String sujet = txtSujet.getText().trim();
        String message = txtMessage.getText().trim();

        if (sujet.isEmpty() || message.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champs requis", "Veuillez remplir tous les champs.");
            return;
        }

        Contact contact = new Contact(currentUser.getId(), sujet, message);

        try {
            serviceContact.envoyerMessage(contact);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Votre message a été envoyé à l'administrateur!");
            retourMesMessages();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'envoyer le message: " + e.getMessage());
        }
    }

    @FXML
    private void annuler() {
        retourMesMessages();
    }

    private void retourMesMessages() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/MesMessages.fxml"));
            Stage stage = (Stage) btnAnnuler.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation", "Erreur: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
