package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainLayoutController implements Initializable {

    @FXML
    private HBox mainContent;

    @FXML
    private AdminSidebarController adminSidebarController;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set active page in sidebar
        if (adminSidebarController != null) {
            adminSidebarController.setActivePage("hotels");
        }
        // Load Hotels view by default
        loadHotels();
    }

    @FXML
    public void loadHotels() {
        loadView("/hotel-view.fxml");
    }

    @FXML
    public void loadChambres() {
        loadView("/chambre-view.fxml");
    }

    @FXML
    public void loadReservations() {
        loadView("/reservation-view.fxml");
    }

    @FXML
    public void deconnexion() {
        org.example.mains.MainApp.switchScene("/org/example/LoginView.fxml", "GoVibe Connexion");
    }

    @FXML
    public void backToAdmin() {
        org.example.mains.MainApp.switchScene("/org/example/AdminDashboardView.fxml", "Admin Dashboard");
    }

    private void loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            VBox view = loader.load();
            HBox.setHgrow(view, Priority.ALWAYS);
            view.setMinWidth(0);
            if (mainContent.getChildren().size() > 1) {
                mainContent.getChildren().set(1, view);
            } else {
                mainContent.getChildren().add(view);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur lors du chargement de la vue: " + fxmlFile);
        }
    }
}

