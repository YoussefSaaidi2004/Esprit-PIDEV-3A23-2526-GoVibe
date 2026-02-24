package tn.esprit.controllers;

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
import tn.esprit.entities.Poste;
import tn.esprit.services.ServicePoste;
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
        if (roleLabel != null && tn.esprit.mains.MainApp.loggedInUser != null) {
            String role = tn.esprit.mains.MainApp.loggedInUser.getRole();
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
        // Personnes logic
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
        // Clear session
        tn.esprit.mains.MainApp.loggedInUser = null;

        try {
            Parent root = FXMLLoader.load(getClass().getResource("/poste-forumviews/Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoToPosts(ActionEvent event) {
        // Déjà sur la page des publications, on peut rafraîchir ou ne rien faire
        loadPosts();
    }
}
