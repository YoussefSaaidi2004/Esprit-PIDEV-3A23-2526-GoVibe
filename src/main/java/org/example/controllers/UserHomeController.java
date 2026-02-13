package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.example.entities.personne;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;

import java.io.IOException;

public class UserHomeController {

    @FXML private Label welcomeLabel;

    private personne currentUser;

    public void initData(personne user) {
        this.currentUser = user;
        welcomeLabel.setText("Bonjour, " + user.getPrenom() + " !");
    }

    @FXML
    private void handleLogout() {
        try {
            SessionManager.clear();
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

    @FXML
    private void handleLocations() {
        SceneNavigator.switchTo("/LocationListView.fxml", welcomeLabel);
    }

    @FXML
    private void handleHome() {
        SceneNavigator.switchTo("/org/example/UserHomeView.fxml", welcomeLabel);
    }
}
