package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.scene.effect.BoxBlur;
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
    @FXML
    private StackPane rootStackPane;
    @FXML
    private ImageView bgImageView;
    
    // Overlay Modal properties
    @FXML
    private StackPane formOverlay;
    @FXML
    private VBox formContainer;

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
        setupHeroBackground();
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

    // ==================== ANIMATION POPUP METHODS ====================

    public void showFormOverlay(Parent formRoot) {
        formContainer.getChildren().clear();
        formContainer.getChildren().add(formRoot);
        
        formOverlay.setVisible(true);
        formOverlay.setManaged(true);
        
        // Blur background
        BoxBlur blur = new BoxBlur(10, 10, 3);
        if (rootStackPane.getChildren().size() > 1) {
            // Apply blur to the main content (which is behind the overlay)
            rootStackPane.getChildren().get(rootStackPane.getChildren().size() - 2).setEffect(blur);
        }

        // Fade In
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), formOverlay);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        // Slide Up slightly
        TranslateTransition slideUp = new TranslateTransition(Duration.millis(300), formContainer);
        slideUp.setFromY(50);
        slideUp.setToY(0);

        ParallelTransition pt = new ParallelTransition(fadeIn, slideUp);
        pt.play();
    }

    public void hideFormOverlay() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), formOverlay);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        
        TranslateTransition slideDown = new TranslateTransition(Duration.millis(250), formContainer);
        slideDown.setFromY(0);
        slideDown.setToY(50);

        ParallelTransition pt = new ParallelTransition(fadeOut, slideDown);
        pt.setOnFinished(e -> {
            formOverlay.setVisible(false);
            formOverlay.setManaged(false);
            formContainer.getChildren().clear();
            if (rootStackPane.getChildren().size() > 1) {
                rootStackPane.getChildren().get(rootStackPane.getChildren().size() - 2).setEffect(null);
            }
            loadForums(); // reload in case we added/modified something
        });
        pt.play();
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
                    controller.setParentListController(this);
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/poste-forumviews/AjoutForum.fxml"));
            Parent root = loader.load();
            
            // Allow the loaded controller to close this specific overlay
            AjoutForumController controller = loader.getController();
            controller.setOverlayController(this);
            
            showFormOverlay(root);
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

    private void setupHeroBackground() {
        if (bgImageView != null && rootStackPane != null) {
            bgImageView.fitWidthProperty().bind(rootStackPane.widthProperty());
            bgImageView.fitHeightProperty().bind(rootStackPane.heightProperty());
            
            var resourcePath = "/messages/home-hero5.png";
            var url = getClass().getResource(resourcePath);
            if (url != null) {
                Image img = new Image(url.toExternalForm(), true);
                bgImageView.setImage(img);
                System.out.println("[Background] Hero image loaded successfully in ListForumView");
            } else {
                System.err.println("[Background] ERROR: Resource " + resourcePath + " not found!");
            }
        }
    }
}
