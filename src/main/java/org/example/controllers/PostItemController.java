package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import org.example.entities.Poste;
import org.example.entities.personne;
import org.example.services.ServicePoste;
import org.example.services.ServicePersonne;
import org.example.utils.SessionManager;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class PostItemController {

    private final ServicePersonne servicePersonne = new ServicePersonne();

    @FXML
    private Label initialsLabel;

    @FXML
    private Label userNameLabel;

    @FXML
    private Label userHandleLabel;

    @FXML
    private Label dateLabel;

    @FXML
    private Label contentLabel;

    @FXML
    private ImageView postImageView;

    @FXML
    private Rectangle imageClip;

    @FXML
    private VBox mediaPlaceholder;

    @FXML
    private Label likesLabel;

    @FXML
    private StackPane mediaPane;

    private Poste currentPost;

    // Reference to parent to trigger overlays
    private DetailsForumController parentDetailsController;
    public void setParentDetailsController(DetailsForumController parent) {
        this.parentDetailsController = parent;
    }

    @FXML
    private void initialize() {
        // Make image responsive to the media pane width (subtract padding)
        try {
            postImageView.fitWidthProperty().bind(mediaPane.widthProperty().subtract(50));
            imageClip.widthProperty().bind(mediaPane.widthProperty().subtract(50));
            imageClip.heightProperty().bind(mediaPane.heightProperty().subtract(40));
        } catch (Exception e) {
            // fail silently if bindings cannot be established at load
        }
    }

    @FXML
    private javafx.scene.control.Button editButton;
    @FXML
    private javafx.scene.control.Button deleteButton;

    public void setData(Poste p) {
        this.currentPost = p;
        contentLabel.setText(p.getContenu());
        likesLabel.setText(p.getLikes() + " Likes");

        dateLabel.setText(p.getDate_creation() != null ? p.getDate_creation().toString() : "A l'instant");

        // Fetch user info
        personne author = servicePersonne.getOneById(p.getUser_id());
        if (author != null) {
            userNameLabel.setText(author.getPrenom() + " " + author.getNom());
            userHandleLabel.setText("@" + author.getRole() + "_" + author.getNom().toLowerCase());
            String initials = (author.getPrenom().substring(0, 1) + author.getNom().substring(0, 1)).toUpperCase();
            initialsLabel.setText(initials);
        } else {
            userNameLabel.setText("Utilisateur Inconnu");
            userHandleLabel.setText("@inconnu");
            initialsLabel.setText("?");
        }

        if (p.getUrl() != null && !p.getUrl().isEmpty()) {
            try {
                File file = new File(p.getUrl());
                if (file.exists()) {
                    Image img = new Image(file.toURI().toString());
                    postImageView.setImage(img);
                    mediaPlaceholder.setVisible(false);
                    mediaPlaceholder.setManaged(false);
                    postImageView.setVisible(true);
                    postImageView.setManaged(true);
                } else {
                    hideMedia();
                }
            } catch (Exception e) {
                hideMedia();
            }
        } else {
            hideMedia();
        }

        // Access Control Logic
        boolean canManage = false;
        if (SessionManager.getCurrentUser() != null) {
            boolean isOwner = p.getUser_id() == SessionManager.getCurrentUser().getId();
            boolean isAdmin = "admin".equalsIgnoreCase(SessionManager.getCurrentUser().getRole());
            canManage = isOwner || isAdmin;
        }

        editButton.setVisible(canManage);
        editButton.setManaged(canManage);
        deleteButton.setVisible(canManage);
        deleteButton.setManaged(canManage);
    }

    private final ServicePoste servicePoste = new ServicePoste();

    @FXML
    private void handleEditPost(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/poste-forumviews/ModifierPost.fxml"));
            Parent root = loader.load();
            ModifierPostController controller = loader.getController();

            if (parentDetailsController != null) {
                // Modal mode
                controller.setOverlayController(parentDetailsController);
                controller.initData(currentPost);
                parentDetailsController.showFormOverlay(root);
            } else {
                // Fallback scene mode
                controller.initData(currentPost);
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeletePost(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText(null);
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cette publication ?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                servicePoste.supprimer(currentPost.getPost_id());

                // Refresh the list by reloading the scene
                Parent root = FXMLLoader.load(getClass().getResource("/poste-forumviews/ListPost.fxml"));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();

            } catch (SQLException | IOException e) {
                e.printStackTrace();
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Erreur");
                errorAlert.setHeaderText("Action impossible");
                errorAlert.setContentText("Une erreur est survenue lors de la suppression.");
                errorAlert.showAndWait();
            }
        }
    }

    private void hideMedia() {
        postImageView.setVisible(false);
        postImageView.setManaged(false);
        mediaPlaceholder.setVisible(false);
        mediaPlaceholder.setManaged(false);

        // Hide the entire media pane to save space
        mediaPane.setVisible(false);
        mediaPane.setManaged(false);
    }
}
