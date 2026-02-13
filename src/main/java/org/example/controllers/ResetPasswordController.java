package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.entities.PasswordReset;
import org.example.services.ServicePasswordReset;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class ResetPasswordController {

    @FXML private TextField tfCode;
    @FXML private PasswordField tfNewPassword;
    @FXML private PasswordField tfConfirmPassword;
    @FXML private Label messageLabel;

    private String userEmail;
    private final ServicePasswordReset servicePasswordReset = new ServicePasswordReset();

    public void initData(String email) {
        this.userEmail = email;
    }

    @FXML
    private void handleResetPassword() {
        String code = tfCode.getText().trim();
        String newPassword = tfNewPassword.getText().trim();
        String confirmPassword = tfConfirmPassword.getText().trim();

        if (code.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showMessage("Veuillez remplir tous les champs.", true);
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showMessage("Les mots de passe ne correspondent pas.", true);
            return;
        }
        
        if (newPassword.length() < 6) {
            showMessage("Le mot de passe doit contenir au moins 6 caractères.", true);
            return;
        }

        try {
            PasswordReset tokenData = servicePasswordReset.getByToken(code);
            
            if (tokenData == null) {
                showMessage("Code invalide.", true);
                return;
            }

            if (!tokenData.getEmail().equals(userEmail)) {
                showMessage("Ce code ne correspond pas à cet email.", true);
                return;
            }

            if (tokenData.getExpirationDate().isBefore(LocalDateTime.now())) {
                showMessage("Code expiré.", true);
                return;
            }

            // Valid code. Update password.
            servicePasswordReset.updatePassword(userEmail, newPassword);
            
            // Delete token
            servicePasswordReset.deleteToken(code);
            
            showMessage("Mot de passe mis à jour avec succès !", false);
            
            // Wait slightly or redirect immediately? Let's redirect to login.
            handleBackToLogin();

        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Erreur lors de la réinitialisation.", true);
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tfCode.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showMessage(String message, boolean isError) {
        messageLabel.setText(message);
        messageLabel.setStyle(isError ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #2ecc71;");
        messageLabel.setVisible(true);
    }
}
