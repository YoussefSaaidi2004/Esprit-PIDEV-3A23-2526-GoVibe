package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.example.entities.personne;

import java.io.IOException;

public class AdminDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;

    private personne currentUser;

    public void initData(personne user) {
        this.currentUser = user;
        welcomeLabel.setText("Bonjour, " + user.getPrenom() + " " + user.getNom());
        roleLabel.setText("Administrateur");
    }

    @FXML
    private void handlePersonnes() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/PersonneView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setTitle("GoVibe — Connexion");
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
