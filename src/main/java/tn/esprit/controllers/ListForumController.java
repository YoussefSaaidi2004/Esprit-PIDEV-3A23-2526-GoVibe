package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.Forum;
import tn.esprit.services.ServiceForum;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class ListForumController {

    @FXML
    private Label roleLabel;

    @FXML
    private VBox forumsContainer;

    private final ServiceForum serviceForum = new ServiceForum();

    @FXML
    public void initialize() {
        syncSidebarRole();
        loadForums();
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
}
