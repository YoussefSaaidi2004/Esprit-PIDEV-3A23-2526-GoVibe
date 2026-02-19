package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.entities.Forum;
import org.example.services.ServiceForum;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class ListForumController {

    @FXML
    private Label roleLabel;

    @FXML
    private Button eventsBtn;

    @FXML
    private Button aboutBtn;

    @FXML
    private VBox forumsContainer;

    private final ServiceForum serviceForum = new ServiceForum();

    @FXML
    public void initialize() {
        syncSidebarRole();
        // Hide sidebar items not needed for admin
        if (SessionManager.getCurrentUser() != null) {
            String role = SessionManager.getCurrentUser().getRole();
            if ("admin".equalsIgnoreCase(role)) {
                if (eventsBtn != null) eventsBtn.setVisible(false);
                if (aboutBtn != null) aboutBtn.setVisible(false);
            }
        }
        loadForums();
    }

    private void syncSidebarRole() {
        if (roleLabel != null && SessionManager.getCurrentUser() != null) {
            String role = SessionManager.getCurrentUser().getRole();
            if ("admin".equalsIgnoreCase(role)) {
                roleLabel.setText("Espace admin");
            } else {
                roleLabel.setText("Espace client");
            }
        }
    }

    private void loadForums() {
        forumsContainer.getChildren().clear();
        try {
            List<Forum> list = serviceForum.afficher();
            for (Forum f : list) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/poste-forumviews/ForumItem.fxml"));
                    Node node = loader.load();
                    ForumItemController controller = loader.getController();
                    controller.setData(f);
                    forumsContainer.getChildren().add(node);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAjoutForum(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/poste-forumviews/AjoutForum.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoToPosts(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/poste-forumviews/ListPost.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoToForums(ActionEvent event) {
        loadForums();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            if (SessionManager.getCurrentUser() != null && "admin".equalsIgnoreCase(SessionManager.getCurrentUser().getRole())) {
                org.example.mains.MainApp.switchScene("/org/example/AdminDashboardView.fxml", "Admin Dashboard");
            } else {
                org.example.mains.MainApp.switchScene("/org/example/UserHomeView.fxml", "Accueil");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.clear();
        org.example.mains.MainApp.switchScene("/org/example/LoginView.fxml", "GoVibe - Connexion");
    }

    // ==================== NAVIGATION METHODS ====================

    @FXML
    private void handleGoHome() {
        try {
            if (SessionManager.getCurrentUser() != null && "admin".equalsIgnoreCase(SessionManager.getCurrentUser().getRole())) {
                org.example.mains.MainApp.switchScene("/org/example/AdminDashboardView.fxml", "Admin Dashboard");
            } else {
                org.example.mains.MainApp.switchScene("/org/example/UserHomeView.fxml", "Accueil");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoActivities() {
        org.example.mains.MainApp.switchScene("/UserHome.fxml", "Activités");
    }

    @FXML
    private void handleGoLocations() {
        org.example.mains.MainApp.switchScene("/LocationListView.fxml", "Voitures");
    }

    @FXML
    private void handleGoFlights() {
        org.example.mains.MainApp.switchScene("/views/user-dashboard.fxml", "Vols");
    }

    @FXML
    private void handleGoHotels() {
        org.example.mains.MainApp.switchScene("/views/room-booking.fxml", "Hôtels");
    }

    @FXML
    private void handleProfile() {
        showAlert("Profil", "Fonctionnalité à venir : Gestion du profil utilisateur");
    }

    @FXML
    private void handleMyReservations() {
        org.example.mains.MainApp.switchScene("/views/room-booking.fxml", "Mes Réservations");
    }

    @FXML
    private void handleMyLocations() {
        org.example.mains.MainApp.switchScene("/LocationListView.fxml", "Mes Locations");
    }

    @FXML
    private void handleMyFlights() {
        org.example.mains.MainApp.switchScene("/views/user-dashboard.fxml", "Mes Vols");
    }

    @FXML
    private void handleSettings() {
        showAlert("Paramètres", "Fonctionnalité à venir : Paramètres utilisateur");
    }

    @FXML
    private void handleHelp() {
        showAlert("Aide", "Besoin d'aide ? Contactez-nous à support@govibe.tn");
    }

    @FXML
    private void handleResetFilters() {
        loadForums();
    }

    private void showAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
