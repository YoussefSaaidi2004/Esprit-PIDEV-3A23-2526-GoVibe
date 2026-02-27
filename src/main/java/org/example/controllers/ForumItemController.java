package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.example.entities.Forum;
import org.example.entities.personne;
import org.example.services.ServiceForum;
import org.example.services.ServiceMembre;
import org.example.services.ServicePersonne;
import org.example.utils.SessionManager;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class ForumItemController {

    @FXML
    private Label nameLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private Label membersLabel;
    @FXML
    private Label postsLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Label privateBadge;
    @FXML
    private ImageView forumImageView;

    @FXML
    private Label creatorLabel;
    @FXML
    private javafx.scene.control.Button leaveButton;
    @FXML
    private javafx.scene.control.Button editButton;
    @FXML
    private javafx.scene.control.Button deleteButton;

    private Forum currentForum;
    private final ServiceForum serviceForum = new ServiceForum();
    private final ServiceMembre serviceMembre = new ServiceMembre();
    private final ServicePersonne servicePersonne = new ServicePersonne();

    public void setData(Forum f) {
        this.currentForum = f;
        nameLabel.setText(f.getName());
        descriptionLabel.setText(f.getDescription());
        membersLabel.setText(f.getNbr_members() + " Membres");
        postsLabel.setText(f.getPost_count() + " Publications");
        dateLabel.setText(
                "Créé le " + (f.getDate_creation() != null ? f.getDate_creation().toString().substring(0, 10) : "N/A"));
        privateBadge.setVisible(f.isIs_private());

        // Fetch Creator Name
        personne creator = servicePersonne.getOneById(f.getCreated_by());
        if (creator != null) {
            creatorLabel.setText("Par " + creator.getPrenom() + " " + creator.getNom());
        } else {
            creatorLabel.setText("Par Inconnu");
        }

        if (f.getImage() != null && !f.getImage().isEmpty()) {
            File file = new File(f.getImage());
            if (file.exists()) {
                forumImageView.setImage(new Image(file.toURI().toString()));
            }
        }

        // Access Control Logic
        boolean canManage = false;
        boolean isMember = false;
        boolean isOwner = false;

        if (SessionManager.getCurrentUser() != null) {
            int currentUserId = SessionManager.getCurrentUser().getId();
            isOwner = f.getCreated_by() == currentUserId;
            boolean isAdmin = "admin".equalsIgnoreCase(SessionManager.getCurrentUser().getRole());
            canManage = isOwner || isAdmin;
            isMember = serviceMembre.estMembre(f.getForum_id(), currentUserId);
        }

        // Show Leave button if member and NOT creator
        boolean showLeave = isMember && !isOwner;
        leaveButton.setVisible(showLeave);
        leaveButton.setManaged(showLeave);

        editButton.setVisible(canManage);
        editButton.setManaged(canManage);
        deleteButton.setVisible(canManage);
        deleteButton.setManaged(canManage);
    }

    @FXML
    private void handleLeaveForum(ActionEvent event) {
        if (SessionManager.getCurrentUser() == null)
            return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Quitter le forum");
        alert.setHeaderText("Confirmation");
        alert.setContentText("Voulez-vous vraiment quitter ce forum ?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                serviceMembre.supprimer(currentForum.getForum_id(), SessionManager.getCurrentUser().getId());
                Parent root = FXMLLoader.load(getClass().getResource("/poste-forumviews/ListForum.fxml"));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Scene scene = new Scene(root);
                root.setOpacity(0);
                stage.setScene(scene);
                stage.show();
                javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(400), root);
                ft.setFromValue(0);
                ft.setToValue(1);
                ft.play();
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleViewDetails(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/poste-forumviews/DetailsForum.fxml"));
            Parent root = loader.load();
            DetailsForumController controller = loader.getController();
            controller.initData(currentForum);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            root.setOpacity(0);
            stage.setScene(scene);
            stage.show();
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(400), root);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEditForum(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/poste-forumviews/ModifierForum.fxml"));
            Parent root = loader.load();
            ModifierForumController controller = loader.getController();
            controller.initData(currentForum);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            root.setOpacity(0);
            stage.setScene(scene);
            stage.show();
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(400), root);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteForum(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer le forum ?");
        alert.setContentText("Toutes les publications liées risquent d'être impactées.");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                serviceForum.supprimer(currentForum.getForum_id());
                Parent root = FXMLLoader.load(getClass().getResource("/poste-forumviews/ListForum.fxml"));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Scene scene = new Scene(root);
                root.setOpacity(0);
                stage.setScene(scene);
                stage.show();
                javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(400), root);
                ft.setFromValue(0);
                ft.setToValue(1);
                ft.play();
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }
    }
}
