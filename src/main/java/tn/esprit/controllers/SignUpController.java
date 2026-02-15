package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.entities.Personne;
import tn.esprit.services.ServicePersonne;

import java.io.IOException;
import java.sql.SQLException;

public class SignUpController {

    @FXML
    private TextField nomField;

    @FXML
    private TextField prenomField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ComboBox<String> roleComboBox;

    private final ServicePersonne servicePersonne = new ServicePersonne();

    @FXML
    public void initialize() {
        roleComboBox.getItems().addAll("user", "admin");
        roleComboBox.setValue("user");
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        String nom = nomField.getText();
        String prenom = prenomField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champs vides", "Veuillez remplir tous les champs.");
            return;
        }

        if (servicePersonne.checkEmailExists(email)) {
            showAlert(Alert.AlertType.WARNING, "Email existant", "Cet email est déjà utilisé.");
            return;
        }

        Personne p = new Personne(nom, prenom, email, password, role);
        try {
            servicePersonne.ajouter(p);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Inscription réussie ! Veuillez vous connecter.");
            handleGoToLogin(event);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'inscription : " + e.getMessage());
        }
    }

    @FXML
    private void handleGoToLogin(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/poste-forumviews/Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
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
