package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

import javafx.scene.layout.VBox;
import org.example.entities.Poste;
import org.example.services.ServicePoste;
import org.example.utils.SessionManager;
import java.sql.SQLException;
import java.util.List;

public class ListPostController {

    @FXML
    private Label roleLabel;

    @FXML
    private VBox postsContainer;

    private final ServicePoste servicePoste = new ServicePoste();

    @FXML
    public void initialize() {
        syncSidebarRole();
        loadPosts();
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

    private void loadPosts() {
        postsContainer.getChildren().clear();
        try {
            List<Poste> list = servicePoste.afficherOrphelins();
            for (Poste p : list) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/poste-forumviews/PostItem.fxml"));
                    Node node = loader.load();
                    PostItemController controller = loader.getController();
                    controller.setData(p);
                    postsContainer.getChildren().add(node);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAjoutPost(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/poste-forumviews/AjoutPost.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handlePersonnes() {
    }

    @FXML
    private void handleGoToForums(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/poste-forumviews/ListForum.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.clear();
        org.example.mains.MainApp.switchScene("/org/example/LoginView.fxml", "GoVibe - Connexion");
    }

    @FXML
    private void handleGoToPosts(ActionEvent event) {
        loadPosts();
    }
}
