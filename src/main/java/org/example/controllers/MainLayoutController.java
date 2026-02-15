package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainLayoutController implements Initializable {

    @FXML
    private BorderPane mainContent;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
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
            mainContent.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur lors du chargement de la vue: " + fxmlFile);
        }
    }
}

