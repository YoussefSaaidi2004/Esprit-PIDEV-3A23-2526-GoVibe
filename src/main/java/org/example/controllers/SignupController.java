package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.entities.personne;
import org.example.services.ServicePersonne;

import java.io.IOException;
import java.sql.SQLException;

public class SignupController {

    @FXML private TextField tfNom;
    @FXML private TextField tfPrenom;
    @FXML private TextField tfEmail;
    @FXML private PasswordField tfPassword;
    @FXML private Label errorLabel;
    
    @FXML private RadioButton rbUser;
    @FXML private RadioButton rbAdmin;
    @FXML private ToggleGroup roleGroup;

    private final ServicePersonne servicePersonne = new ServicePersonne();

    @FXML
    private void handleSignup() {
        String nom = tfNom.getText().trim();
        String prenom = tfPrenom.getText().trim();
        String email = tfEmail.getText().trim();
        String password = tfPassword.getText().trim();
        
        // Validation: Empty fields
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.", true);
            return;
        }

        // Validation: Role selected
        if (roleGroup.getSelectedToggle() == null) {
            showError("Veuillez selectionner un role.", true);
            return;
        }

        // Validation: Password length
        if (password.length() < 6) {
            showError("Le mot de passe doit contenir au moins 6 caracteres.", true);
            return;
        }
        
        // Validation: Email format
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            showError("Format d'email invalide.", true);
            return;
        }

        try {
            // Validation: Email uniqueness
            if (servicePersonne.emailExists(email)) {
                showError("Cet email est deja utilise.", true);
                return;
            }

            // Get selected role
            String role = "user"; // Default
            if (rbAdmin.isSelected()) {
                role = "admin";
            } else if (rbUser.isSelected()) {
                role = "user";
            }
            
            // Create new user
            personne newUser = new personne(nom, prenom, email, password, role);
            servicePersonne.ajouter(newUser);

            showError("Compte cree avec succes ! Connectez-vous.", false);
            
            // Navigate to Login
            handleLoginLink();

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur lors de la creation du compte.", true);
        }
    }

    @FXML
    private void handleLoginLink() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tfEmail.getScene().getWindow();

            // Swap root — smooth transition, no flicker
            if (stage.getScene() != null) {
                stage.getScene().setRoot(root);
            } else {
                stage.setScene(new Scene(root));
            }
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String message, boolean isError) {
        errorLabel.setText(message);
        errorLabel.setStyle(isError ? "-fx-text-fill: #ff6b6b;" : "-fx-text-fill: #50C878;");
        errorLabel.setVisible(true);
    }
}
