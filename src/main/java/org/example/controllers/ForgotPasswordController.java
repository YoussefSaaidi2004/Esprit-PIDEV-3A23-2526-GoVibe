package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.entities.PasswordReset;
import org.example.services.EmailService;
import org.example.services.ServicePasswordReset;
import org.example.services.ServicePersonne;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

public class ForgotPasswordController {

    @FXML private TextField tfEmail;
    @FXML private Label messageLabel;

    private final ServicePersonne servicePersonne = new ServicePersonne();
    private final ServicePasswordReset servicePasswordReset = new ServicePasswordReset();
    private final EmailService emailService = new EmailService();

    @FXML
    private void handleSendResetLink() {
        String email = tfEmail.getText().trim();
        if (email.isEmpty()) {
            showMessage("Veuillez entrer votre email.", true);
            return;
        }

        try {
            if (servicePersonne.emailExists(email)) {
                // Generate Token
                String token = UUID.randomUUID().toString().substring(0, 6).toUpperCase(); // 6 char code
                LocalDateTime expirationDate = LocalDateTime.now().plusMinutes(15);
                
                PasswordReset pr = new PasswordReset(email, token, expirationDate);
                servicePasswordReset.createToken(pr);
                
                // Send Email
                String subject = "GoVibe - Reinitialisation de mot de passe";
                String body = "Bonjour,\n\nVotre code de reinitialisation est : " + token + "\n\nCe code expire dans 15 minutes.";
                emailService.sendEmail(email, subject, body);
                
                showMessage("Code envoye ! Verifiez votre email.", false);
                
                loadResetPasswordView(email);
                
            } else {
                showMessage("Cet email n'existe pas.", true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Erreur systeme.", true);
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tfEmail.getScene().getWindow();

            // Swap root — smooth transition
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
    
    private void loadResetPasswordView(String email) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/ResetPasswordView.fxml"));
            Parent root = loader.load();
            
            // Pass email to next controller
            ResetPasswordController controller = loader.getController();
            controller.initData(email);
            
            Stage stage = (Stage) tfEmail.getScene().getWindow();

            // Swap root — smooth transition
            if (stage.getScene() != null) {
                stage.getScene().setRoot(root);
            } else {
                stage.setScene(new Scene(root));
            }
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
            showMessage("Erreur de navigation vers la reinitialisation.", true);
        }
    }

    private void showMessage(String message, boolean isError) {
        messageLabel.setText(message);
        messageLabel.setStyle(isError ? "-fx-text-fill: #ff6b6b;" : "-fx-text-fill: #50C878;");
        messageLabel.setVisible(true);
    }
}
