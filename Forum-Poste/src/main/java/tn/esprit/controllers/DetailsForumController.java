package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.Forum;
import tn.esprit.entities.Poste;
import tn.esprit.services.ServicePoste;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class DetailsForumController {

    @FXML
    private ImageView coverImage;

    @FXML
    private Label forumNameLabel;

    @FXML
    private Label forumDescriptionLabel;
    @FXML
    private Label creatorLabel;

    @FXML
    private VBox postsContainer;

    private Forum currentForum;
    private final ServicePoste servicePoste = new ServicePoste();

    private final tn.esprit.services.ServiceMembre serviceMembre = new tn.esprit.services.ServiceMembre();
    private final tn.esprit.services.ServicePersonne servicePersonne = new tn.esprit.services.ServicePersonne();

    @FXML
    private javafx.scene.layout.VBox creatorControls;
    @FXML
    private javafx.scene.control.TextField memberEmailField;

    public void initData(Forum forum) {
        this.currentForum = forum;
        forumNameLabel.setText(forum.getName());
        forumDescriptionLabel.setText(forum.getDescription());

        // Fetch Creator Name
        tn.esprit.entities.Personne creator = servicePersonne.getOneById(forum.getCreated_by());
        if (creator != null) {
            creatorLabel.setText("Créé par " + creator.getPrenom() + " " + creator.getNom());
        } else {
            creatorLabel.setText("Créé par Inconnu");
        }

        if (forum.getImage() != null && !forum.getImage().isEmpty()) {
            File file = new File(forum.getImage());
            if (file.exists()) {
                coverImage.setImage(new Image(file.toURI().toString()));
            }
        }

        // Auto-join logic
        if (tn.esprit.mains.MainApp.loggedInUser != null) {
            int userId = tn.esprit.mains.MainApp.loggedInUser.getId();
            try {
                if (!serviceMembre.estMembre(forum.getForum_id(), userId)) {
                    tn.esprit.entities.Membre m = new tn.esprit.entities.Membre(forum.getForum_id(), userId);
                    serviceMembre.ajouter(m);
                    System.out.println("Utilisateur ajouté automatiquement au forum (via Membre entity).");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // Show creator controls if owner
            if (forum.getCreated_by() == userId) {
                creatorControls.setVisible(true);
                creatorControls.setManaged(true);
            } else {
                creatorControls.setVisible(false);
                creatorControls.setManaged(false);
            }
        } else {
            creatorControls.setVisible(false);
            creatorControls.setManaged(false);
        }

        loadForumPosts();
    }

    @FXML
    private VBox membersContainer;
    @FXML
    private Label membersCountLabel;

    private void loadForumPosts() {
        postsContainer.getChildren().clear();
        try {
            List<Poste> list = servicePoste.afficherParForum(currentForum.getForum_id());
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

        loadMembers();
    }

    private void loadMembers() {
        membersContainer.getChildren().clear();
        try {
            List<tn.esprit.entities.Membre> membres = serviceMembre.afficherParForum(currentForum.getForum_id());
            membersCountLabel.setText(String.valueOf(membres.size()));

            for (tn.esprit.entities.Membre m : membres) {
                tn.esprit.entities.Personne p = servicePersonne.getOneById(m.getUser_id());
                if (p != null) {
                    javafx.scene.layout.HBox card = new javafx.scene.layout.HBox();
                    card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    card.setSpacing(10);
                    card.setStyle(
                            "-fx-background-color: #F8F9FA; -fx-padding: 8; -fx-background-radius: 8; -fx-border-color: #E0E0E0; -fx-border-radius: 8;");

                    // Avatar placeholder
                    javafx.scene.control.Label avatar = new javafx.scene.control.Label(
                            p.getPrenom().substring(0, 1).toUpperCase());
                    avatar.setStyle(
                            "-fx-background-color: #E0F2F1; -fx-text-fill: #00695C; -fx-font-weight: bold; -fx-min-width: 30; -fx-min-height: 30; -fx-max-width: 30; -fx-max-height: 30; -fx-alignment: center; -fx-background-radius: 15;");

                    javafx.scene.layout.VBox info = new javafx.scene.layout.VBox();
                    info.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    info.setSpacing(2);

                    javafx.scene.control.Label name = new javafx.scene.control.Label(p.getPrenom() + " " + p.getNom());
                    name.setStyle("-fx-font-weight: bold; -fx-text-fill: #333; -fx-font-size: 13px;");

                    javafx.scene.control.Label role = new javafx.scene.control.Label(p.getEmail());
                    role.setStyle("-fx-text-fill: #757575; -fx-font-size: 11px;");

                    info.getChildren().addAll(name, role);
                    card.getChildren().addAll(avatar, info);

                    membersContainer.getChildren().add(card);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddMember(ActionEvent event) {
        String email = memberEmailField.getText();
        if (email.isEmpty()) {
            showAlert(javafx.scene.control.Alert.AlertType.WARNING, "Attention", "Veuillez saisir un email.");
            return;
        }

        try {
            tn.esprit.entities.Personne p = servicePersonne.getOneByEmail(email);
            if (p != null) {
                tn.esprit.entities.Membre m = new tn.esprit.entities.Membre(currentForum.getForum_id(), p.getId());
                serviceMembre.ajouter(m);
                showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Succès", "Membre ajouté avec succès !");
                memberEmailField.clear();
            } else {
                showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur",
                        "Utilisateur introuvable avec cet email.");
            }
        } catch (SQLException e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void showAlert(javafx.scene.control.Alert.AlertType type, String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleAjoutPost(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/poste-forumviews/AjoutPost.fxml"));
            Parent root = loader.load();
            AjoutPostController controller = loader.getController();
            controller.setForum(currentForum); // Pass the forum context

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
}
