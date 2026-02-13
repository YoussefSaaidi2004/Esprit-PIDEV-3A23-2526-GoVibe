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
            showError("Veuillez sélectionner un rôle.", true);
            return;
        }

        // Validation: Password length
        if (password.length() < 6) {
            showError("Le mot de passe doit contenir au moins 6 caractères.", true);
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
                showError("Cet email est déjà utilisé.", true);
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

            showError("Compte créé avec succès ! Connectez-vous.", false);
            
            // Navigate to Login
            handleLoginLink();

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur lors de la création du compte.", true);
        }
    }

    @FXML
    private void handleLoginLink() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tfEmail.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String message, boolean isError) {
        errorLabel.setText(message);
        errorLabel.setStyle(isError ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #2ecc71;");
        errorLabel.setVisible(true);
    }
}
