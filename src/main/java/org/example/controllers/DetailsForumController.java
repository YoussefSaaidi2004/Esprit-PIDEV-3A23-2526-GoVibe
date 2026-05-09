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
import javafx.scene.layout.Region;
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

    // Join Request UI
    @FXML
    private Button joinButton;
    @FXML
    private Label pendingLabel;
    @FXML
    private VBox requestsBox;
    @FXML
    private VBox requestsContainer;

    // Overlay Modal properties
    @FXML
    private StackPane formOverlay;
    @FXML
    private VBox formContainer;

    // To identify the root StackPane we might have to wrap DetailsForum or rely on
    // parent
    @FXML
    private StackPane rootStackPane;

    @FXML
    private StackPane userHeaderStack;
    @FXML
    private StackPane adminSidebarStack;

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

        // Membership logic
        if (SessionManager.getCurrentUser() != null) {
            int userId = SessionManager.getCurrentUser().getId();
            boolean isOwner = forum.getCreated_by() == userId;

            // Reset buttons visibility
            joinButton.setVisible(false);
            joinButton.setManaged(false);
            pendingLabel.setVisible(false);
            pendingLabel.setManaged(false);
            leaveButton.setVisible(false);
            leaveButton.setManaged(false);

            Membre membership = serviceMembre.getMembre(forum.getForum_id(), userId);
            boolean isMember = membership != null && "ACCEPTED".equalsIgnoreCase(membership.getStatus());
            boolean isPending = membership != null && "PENDING".equalsIgnoreCase(membership.getStatus());

            boolean isAdmin = "admin".equalsIgnoreCase(SessionManager.getCurrentUser().getRole());
            if (!isOwner && !isMember && !isAdmin) {
                if (isPending) {
                    pendingLabel.setVisible(true);
                    pendingLabel.setManaged(true);
                } else {
                    joinButton.setVisible(true);
                    joinButton.setManaged(true);
                }
            }

            // Show Leave button if member and NOT creator
            if (isMember && !isOwner) {
                leaveButton.setVisible(true);
                leaveButton.setManaged(true);
            }

            // Creator controls
            if (isOwner) {
                creatorControls.setVisible(true);
                creatorControls.setManaged(true);
                loadRequests(); // Load pending requests for the creator
            } else {
                creatorControls.setVisible(false);
                creatorControls.setManaged(false);
                requestsBox.setVisible(false);
                requestsBox.setManaged(false);
            }

        } else {
            creatorControls.setVisible(false);
            creatorControls.setManaged(false);
            leaveButton.setVisible(false);
            leaveButton.setManaged(false);
        }

        loadForumPosts();
    }

    private void syncSidebarRole() {
        if (SessionManager.getCurrentUser() != null) {
            String role = SessionManager.getCurrentUser().getRole();
            boolean isAdmin = "admin".equalsIgnoreCase(role);

            // Toggle Header/Sidebar based on role
            if (userHeaderStack != null) {
                userHeaderStack.setVisible(!isAdmin);
                userHeaderStack.setManaged(!isAdmin);
            }
            if (adminSidebarStack != null) {
                adminSidebarStack.setVisible(isAdmin);
                adminSidebarStack.setManaged(isAdmin);
            }

            if (roleLabel != null) {
                if (isAdmin) {
                    roleLabel.setText("Espace admin");
                } else {
                    roleLabel.setText("Espace client");
                }
            }
        }
    }

    private void setupHeroBackground() {
        if (bgImageView != null && rootStackPane != null) {
            bgImageView.fitWidthProperty().bind(rootStackPane.widthProperty());
            bgImageView.fitHeightProperty().bind(rootStackPane.heightProperty());
            var url = getClass().getResource("/messages/home-hero5.png");
            if (url != null)
                bgImageView.setImage(new Image(url.toExternalForm(), true));
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

        // Blur background (assumes rootStackPane exists and main content is at index 0
        // or similar)
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

        // Security check for private forums
        if (currentForum.isIs_private() && SessionManager.getCurrentUser() != null) {
            int userId = SessionManager.getCurrentUser().getId();
            boolean isAdmin = "admin".equalsIgnoreCase(SessionManager.getCurrentUser().getRole());
            if (!isAdmin && currentForum.getCreated_by() != userId
                    && !serviceMembre.estMembre(currentForum.getForum_id(), userId)) {
                VBox lockedBox = new VBox(15);
                lockedBox.setAlignment(Pos.CENTER);
                lockedBox
                        .setStyle("-fx-padding: 60; -fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 20;");

                Label icon = new Label("🔒");
                icon.setStyle("-fx-font-size: 40;");
                Label msg = new Label("Ce forum est privé");
                msg.setStyle("-fx-text-fill: white; -fx-font-size: 20; -fx-font-weight: bold;");
                Label sub = new Label("Rejoignez la communauté pour voir les publications.");
                sub.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 14;");

                lockedBox.getChildren().addAll(icon, msg, sub);
                postsContainer.getChildren().add(lockedBox);
                return;
            }
        }

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

    @FXML
    private void handleJoinRequest(ActionEvent event) {
        if (currentForum == null || SessionManager.getCurrentUser() == null)
            return;
        try {
            String status = currentForum.isIs_private() ? "PENDING" : "ACCEPTED";
            Membre m = new Membre(currentForum.getForum_id(), SessionManager.getCurrentUser().getId(), status);
            serviceMembre.ajouter(m);

            if ("PENDING".equals(status)) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Votre demande d'adhésion a été envoyée !");
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Vous avez rejoint le forum !");
            }
            initData(currentForum);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de rejoindre le forum.");
        }
    }

    private void loadRequests() {
        requestsContainer.getChildren().clear();
        try {
            List<Membre> demandes = serviceMembre.afficherDemandesParForum(currentForum.getForum_id());
            requestsBox.setVisible(!demandes.isEmpty());
            requestsBox.setManaged(!demandes.isEmpty());

            for (Membre d : demandes) {
                personne p = servicePersonne.getOneById(d.getUser_id());
                if (p != null) {
                    HBox card = new HBox(10);
                    card.setAlignment(Pos.CENTER_LEFT);
                    card.setStyle(
                            "-fx-background-color: rgba(251,192,45,0.08); -fx-padding: 8; -fx-background-radius: 10; -fx-border-color: rgba(251,192,45,0.2); -fx-border-width: 1;");

                    VBox info = new VBox(2);
                    Label name = new Label(p.getPrenom() + " " + p.getNom());
                    name.setStyle("-fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold;");
                    info.getChildren().add(name);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                    Button btnAcc = new Button("✓");
                    btnAcc.setStyle(
                            "-fx-background-color: #50C878; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
                    btnAcc.setOnAction(e -> {
                        try {
                            serviceMembre.accepterMembre(d.getForum_id(), d.getUser_id());
                            initData(currentForum);
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    });

                    Button btnRef = new Button("✕");
                    btnRef.setStyle(
                            "-fx-background-color: #ff4d4d; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
                    btnRef.setOnAction(e -> {
                        try {
                            serviceMembre.supprimer(d.getForum_id(), d.getUser_id());
                            initData(currentForum);
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    });

                    card.getChildren().addAll(info, spacer, btnAcc, btnRef);
                    requestsContainer.getChildren().add(card);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
