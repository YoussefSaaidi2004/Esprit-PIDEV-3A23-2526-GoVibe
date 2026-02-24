package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.example.utils.SessionManager;

import java.io.IOException;

/**
 * Contrôleur pour le menu Admin Sidebar - réutilisable sur toutes les pages admin
 * Ce contrôleur gère la navigation pour toutes les pages d'administration
 */
public class AdminSidebarController {

    @FXML
    private Button navDashboard;
    @FXML
    private Button navPersonnes;
    @FXML
    private Button navVoitures;
    @FXML
    private Button navVols;
    @FXML
    private Button navHotels;
    @FXML
    private Button navActivites;
    @FXML
    private Button navForums;
    @FXML
    private Button navMessages;

    private String currentPage = "";

    /**
     * Définit la page active pour mettre en surbrillance le bon bouton
     */
    public void setActivePage(String page) {
        this.currentPage = page;
        updateActiveButton();
    }

    private void updateActiveButton() {
        // Reset all buttons
        resetButtonStyle(navDashboard);
        resetButtonStyle(navPersonnes);
        resetButtonStyle(navVoitures);
        resetButtonStyle(navVols);
        resetButtonStyle(navHotels);
        resetButtonStyle(navActivites);
        resetButtonStyle(navForums);
        resetButtonStyle(navMessages);

        // Set active button
        switch (currentPage) {
            case "dashboard":
                setActiveButtonStyle(navDashboard);
                break;
            case "personnes":
                setActiveButtonStyle(navPersonnes);
                break;
            case "voitures":
                setActiveButtonStyle(navVoitures);
                break;
            case "vols":
                setActiveButtonStyle(navVols);
                break;
            case "hotels":
                setActiveButtonStyle(navHotels);
                break;
            case "activites":
                setActiveButtonStyle(navActivites);
                break;
            case "forums":
                setActiveButtonStyle(navForums);
                break;
            case "messages":
                setActiveButtonStyle(navMessages);
                break;
        }
    }

    private void resetButtonStyle(Button button) {
        if (button != null) {
            button.setStyle("-fx-background-color: transparent; -fx-text-fill: #A0E0C9; -fx-font-size: 14px; -fx-padding: 12 16; -fx-background-radius: 10; -fx-cursor: hand; -fx-alignment: CENTER_LEFT; -fx-graphic-text-gap: 12;");
        }
    }

    private void setActiveButtonStyle(Button button) {
        if (button != null) {
            button.setStyle("-fx-background-color: rgba(80,200,120,0.15); -fx-text-fill: #D1F2EB; -fx-font-size: 14px; -fx-padding: 12 16; -fx-background-radius: 10; -fx-cursor: hand; -fx-alignment: CENTER_LEFT; -fx-graphic-text-gap: 12;");
        }
    }

    @FXML
    private void handleGoDashboard() {
        if ("dashboard".equals(currentPage)) return;
        navigateTo("/org/example/AdminDashboardView.fxml");
    }

    @FXML
    private void handleGoPersonnes() {
        if ("personnes".equals(currentPage)) return;
        navigateTo("/org/example/PersonneView.fxml");
    }

    @FXML
    private void handleGoVoitures() {
        if ("voitures".equals(currentPage)) return;
        navigateTo("/VoitureListView.fxml");
    }

    @FXML
    private void handleGoFlights() {
        if ("vols".equals(currentPage)) return;
        navigateTo("/views/flight-management.fxml");
    }

    @FXML
    private void handleGoHotels() {
        if ("hotels".equals(currentPage)) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main-layout.fxml"));
            Parent root = loader.load();
            MainLayoutController controller = loader.getController();
            if (controller != null) {
                controller.loadHotels();
            }
            Stage stage = getCurrentStage();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoActivites() {
        if ("activites".equals(currentPage)) return;
        navigateTo("/Dashboard.fxml");
    }

    @FXML
    private void handleGoForums() {
        if ("forums".equals(currentPage)) return;
        navigateTo("/poste-forumviews/ListForum.fxml");
    }

    @FXML
    private void handleGoMessages() {
        if ("messages".equals(currentPage)) return;
        navigateTo("/AdminMessagesChat.fxml");
    }

    @FXML
    private void handleLogout() {
        SessionManager.clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage = getCurrentStage();
            stage.setTitle("GoVibe Connexion");
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = getCurrentStage();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Stage getCurrentStage() {
        if (navDashboard != null && navDashboard.getScene() != null) {
            return (Stage) navDashboard.getScene().getWindow();
        }
        return null;
    }
}
