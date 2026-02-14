package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.entities.personne;
import org.example.services.ServicePersonne;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {

    @FXML private TextField tfEmail;
    @FXML private PasswordField tfPassword;
    @FXML private Label errorLabel;

    private final ServicePersonne servicePersonne = new ServicePersonne();

    @FXML
    private void handleLogin() {
        String email = tfEmail.getText().trim();
        String password = tfPassword.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        try {
            personne user = servicePersonne.login(email, password);
            if (user != null) {
                SessionManager.setCurrentUser(user);
                // Route based on role
                if ("admin".equalsIgnoreCase(user.getRole())) {
                    loadAdminDashboard(user);
                } else {
                    loadUserHome(user);
                }
            } else {
                showError("Email ou mot de passe incorrect.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur de connexion a la base de donnees.");
        }
    }

    @FXML
    private void handleForgotPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/ForgotPasswordView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tfEmail.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
            showError("Impossible de charger la vue mot de passe oublie.");
        }
    }

    @FXML
    private void handleSignup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/SignupView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tfEmail.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
            showError("Impossible de charger la vue d'inscription.");
        }
    }

    private void loadAdminDashboard(personne user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/AdminDashboardView.fxml"));
            Parent root = loader.load();
            AdminDashboardController controller = loader.getController();
            controller.initData(user);
            Stage stage = (Stage) tfEmail.getScene().getWindow();
            stage.setTitle("GoVibe - Administration");
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors du chargement du dashboard.");
        }
    }

    private void loadUserHome(personne user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/UserHomeView.fxml"));
            Parent root = loader.load();
            UserHomeController controller = loader.getController();
            controller.initData(user);
            Stage stage = (Stage) tfEmail.getScene().getWindow();
            stage.setTitle("GoVibe - Accueil");
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors du chargement de l'accueil.");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
