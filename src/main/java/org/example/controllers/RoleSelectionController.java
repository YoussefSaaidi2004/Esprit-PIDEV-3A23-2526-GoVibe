package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class RoleSelectionController {

    @FXML
    private void goToUserSpace(ActionEvent event) {
        navigateTo(event, "/UserHome.fxml", "GoVibe - Espace Client");
    }

    @FXML
    private void goToAdminSpace(ActionEvent event) {
        navigateTo(event, "/Dashboard.fxml", "GoVibe - Espace Admin");
    }

    private void navigateTo(ActionEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 700));
            stage.setTitle(title);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur de navigation vers : " + fxmlPath);
        }
    }
}
