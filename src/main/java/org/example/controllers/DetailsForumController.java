package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.scene.effect.BoxBlur;
import org.example.entities.Forum;
import org.example.entities.Membre;
import org.example.entities.Poste;
import org.example.entities.personne;
import org.example.services.ServiceMembre;
import org.example.services.ServicePersonne;
import org.example.services.ServicePoste;
import org.example.utils.SessionManager;

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
    private Label roleLabel;

    @FXML
    private VBox postsContainer;

    private Forum currentForum;
    private final ServicePoste servicePoste = new ServicePoste();

    private final ServiceMembre serviceMembre = new ServiceMembre();
    private final ServicePersonne servicePersonne = new ServicePersonne();

    @FXML
    private VBox creatorControls;
    @FXML
    private TextField memberEmailField;
    @FXML
    private Button leaveButton;

    @FXML
    private ImageView bgImageView;

    // Overlay Modal properties
    @FXML
    private StackPane formOverlay;
    @FXML
    private VBox formContainer;
    
    // To identify the root StackPane we might have to wrap DetailsForum or rely on parent
    @FXML
    private StackPane rootStackPane;

    public void initData(Forum forum) {
        syncSidebarRole();
        setupHeroBackground();
        this.currentForum = forum;
        forumNameLabel.setText(forum.getName());
        forumDescriptionLabel.setText(forum.getDescription());

        // Fetch Creator Name
        personne creator = servicePersonne.getOneById(forum.getCreated_by());
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
        if (SessionManager.getCurrentUser() != null) {
            int userId = SessionManager.getCurrentUser().getId();
            try {
                if (!serviceMembre.estMembre(forum.getForum_id(), userId)) {
                    Membre m = new Membre(forum.getForum_id(), userId);
                    serviceMembre.ajouter(m);
                    System.out.println("Utilisateur ajouté automatiquement au forum (via Membre entity).");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // Show creator controls if owner
            boolean isOwner = forum.getCreated_by() == userId;
            boolean isMember = serviceMembre.estMembre(forum.getForum_id(), userId);

            if (isOwner) {
                creatorControls.setVisible(true);
                creatorControls.setManaged(true);
            } else {
                creatorControls.setVisible(false);
                creatorControls.setManaged(false);
            }

            // Show Leave button if member and NOT creator
            boolean showLeave = isMember && !isOwner;
            leaveButton.setVisible(showLeave);
            leaveButton.setManaged(showLeave);

        } else {
            creatorControls.setVisible(false);
            creatorControls.setManaged(false);
            leaveButton.setVisible(false);
            leaveButton.setManaged(false);
        }

        loadForumPosts();
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

    private void setupHeroBackground() {
        if (bgImageView != null && rootStackPane != null) {
            bgImageView.fitWidthProperty().bind(rootStackPane.widthProperty());
            bgImageView.fitHeightProperty().bind(rootStackPane.heightProperty());
            var url = getClass().getResource("/messages/home-hero5.png");
            if (url != null) bgImageView.setImage(new Image(url.toExternalForm(), true));
        }
    }

    @FXML
    private VBox membersContainer;
    @FXML
    private Label membersCountLabel;

    // ==================== ANIMATION POPUP METHODS ====================

    public void showFormOverlay(Parent formRoot) {
        formContainer.getChildren().clear();
        formContainer.getChildren().add(formRoot);
        
        formOverlay.setVisible(true);
        formOverlay.setManaged(true);
        
        // Blur background (assumes rootStackPane exists and main content is at index 0 or similar)
        BoxBlur blur = new BoxBlur(10, 10, 3);
        if (rootStackPane != null && rootStackPane.getChildren().size() > 1) {
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
            if (rootStackPane != null && rootStackPane.getChildren().size() > 1) {
                rootStackPane.getChildren().get(rootStackPane.getChildren().size() - 2).setEffect(null);
            }
            loadForumPosts(); // reload in case we added/modified something
        });
        pt.play();
    }

    public void loadForumPosts() {
        postsContainer.getChildren().clear();
        try {
            List<Poste> list = servicePoste.afficherParForum(currentForum.getForum_id());
            for (Poste p : list) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/poste-forumviews/PostItem.fxml"));
                    Node node = loader.load();
                    PostItemController controller = loader.getController();
                    controller.setParentDetailsController(this);
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
            List<Membre> membres = serviceMembre.afficherParForum(currentForum.getForum_id());
            membersCountLabel.setText(String.valueOf(membres.size()));

            for (Membre m : membres) {
                personne p = servicePersonne.getOneById(m.getUser_id());
                if (p != null) {
                    HBox card = new HBox();
                    card.setAlignment(Pos.CENTER_LEFT);
                    card.setSpacing(10);
                    card.setStyle(
                            "-fx-background-color: rgba(255,255,255,0.07); -fx-padding: 10 12; -fx-background-radius: 12; -fx-border-color: rgba(80,200,120,0.2); -fx-border-radius: 12; -fx-border-width: 1;");

                    // Avatar placeholder
                    Label avatar = new Label(
                            p.getPrenom().substring(0, 1).toUpperCase());
                    avatar.setStyle(
                            "-fx-background-color: rgba(80,200,120,0.25); -fx-text-fill: #50C878; -fx-font-weight: 900; -fx-min-width: 34; -fx-min-height: 34; -fx-max-width: 34; -fx-max-height: 34; -fx-alignment: center; -fx-background-radius: 17; -fx-font-size: 14px;");

                    VBox info = new VBox();
                    info.setAlignment(Pos.CENTER_LEFT);
                    info.setSpacing(2);

                    Label name = new Label(p.getPrenom() + " " + p.getNom());
                    name.setStyle("-fx-font-weight: 800; -fx-text-fill: white; -fx-font-size: 13px;");

                    Label role = new Label(p.getEmail());
                    role.setStyle("-fx-text-fill: rgba(255,255,255,0.45); -fx-font-size: 11px;");

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
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez saisir un email.");
            return;
        }

        try {
            personne p = servicePersonne.getOneByEmail(email);
            if (p != null) {
                Membre m = new Membre(currentForum.getForum_id(), p.getId());
                serviceMembre.ajouter(m);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Membre ajouté avec succès !");
                memberEmailField.clear();
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Utilisateur introuvable avec cet email.");
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
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
            controller.setOverlayController(this); // Tell the form how to close itself
            controller.setForum(currentForum); // Pass the forum context
            
            showFormOverlay(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoToPosts(ActionEvent event) {
        org.example.mains.MainApp.switchScene("/poste-forumviews/ListPost.fxml", "GoVibe - Publications");
    }

    @FXML
    private void handleGoToForums(ActionEvent event) {
        org.example.mains.MainApp.switchScene("/poste-forumviews/ListForum.fxml", "GoVibe - Forums");
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
                handleGoToForums(event);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.clear();
        org.example.mains.MainApp.switchScene("/org/example/LoginView.fxml", "GoVibe - Connexion");
    }
}
