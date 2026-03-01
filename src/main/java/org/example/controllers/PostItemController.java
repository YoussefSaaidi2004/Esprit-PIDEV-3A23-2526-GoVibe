package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import org.example.entities.Commentaire;
import org.example.entities.Poste;
import org.example.entities.personne;
import org.example.services.ServiceCommentaire;
import org.example.services.ServicePoste;
import org.example.services.ServicePersonne;
import org.example.utils.SessionManager;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

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
    private Label commentsCountLabel;

    @FXML
    private StackPane mediaPane;

    // Comments UI
    @FXML private VBox commentsSection;
    @FXML private VBox commentsContainer;
    @FXML private TextField commentField;
    @FXML private Label currentUserInitialsLabel;
    @FXML private Button commentToggleButton;

    private Poste currentPost;
    private boolean likedByCurrentUser = false;
    private boolean commentsVisible = false;

    private final ServiceCommentaire serviceCommentaire = new ServiceCommentaire();

    // Per-comment like / dislike state (session only)
    private final java.util.Set<Integer> likedCommentIds    = new java.util.HashSet<>();
    private final java.util.Set<Integer> dislikedCommentIds = new java.util.HashSet<>();

    @FXML
    private javafx.scene.control.Button likeButton;

    // Reference to parent to trigger overlays
    private DetailsForumController parentDetailsController;
    public void setParentDetailsController(DetailsForumController parent) {
        this.parentDetailsController = parent;
    }

    // Optional refresh callback from ListPostController
    private Runnable onRefresh;
    public void setOnRefresh(Runnable onRefresh) {
        this.onRefresh = onRefresh;
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

        // Allow pressing Enter to submit a comment
        if (commentField != null) {
            commentField.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    handleSendComment();
                }
            });
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

        // Reset like state when data is set
        likedByCurrentUser = false;
        updateLikeButton();

        // Set current user initials in comment input
        if (currentUserInitialsLabel != null && SessionManager.getCurrentUser() != null) {
            personne me = SessionManager.getCurrentUser();
            String initials = (me.getPrenom().substring(0, 1) + me.getNom().substring(0, 1)).toUpperCase();
            currentUserInitialsLabel.setText(initials);
        }

        // Load comment count
        refreshCommentCount();

        // Reset comments panel
        commentsVisible = false;
        if (commentsSection != null) {
            commentsSection.setVisible(false);
            commentsSection.setManaged(false);
        }
    }

    @FXML
    private void handleLikePost() {
        if (currentPost == null) return;
        try {
            if (likedByCurrentUser) {
                servicePoste.unlikePost(currentPost.getPost_id());
                currentPost.setLikes(Math.max(0, currentPost.getLikes() - 1));
                likedByCurrentUser = false;
            } else {
                servicePoste.likePost(currentPost.getPost_id());
                currentPost.setLikes(currentPost.getLikes() + 1);
                likedByCurrentUser = true;
            }
            likesLabel.setText(currentPost.getLikes() + " Likes");
            updateLikeButton();
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateLikeButton() {
        if (likeButton == null) return;
        if (likedByCurrentUser) {
            likeButton.setText("👍  J'aime · " + (currentPost != null ? currentPost.getLikes() : 0));
            likeButton.setStyle("-fx-background-color: rgba(80,200,120,0.18); -fx-text-fill: #50C878;"
                    + "-fx-font-size: 13; -fx-font-weight: 800; -fx-padding: 8 0;"
                    + "-fx-background-radius: 10; -fx-cursor: hand;");
        } else {
            likeButton.setText("👍  J'aime");
            likeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.65);"
                    + "-fx-font-size: 13; -fx-font-weight: 700; -fx-padding: 8 0;"
                    + "-fx-background-radius: 10; -fx-cursor: hand;");
        }
    }
    @FXML
    private void handleCommentToggle() {
        if (commentsSection == null) return;
        commentsVisible = !commentsVisible;
        commentsSection.setVisible(commentsVisible);
        commentsSection.setManaged(commentsVisible);
        if (commentsVisible) {
            loadComments();
            if (commentToggleButton != null) {
                commentToggleButton.setStyle("-fx-background-color: rgba(80,200,120,0.14); -fx-text-fill: #50C878;"
                        + "-fx-font-size: 13; -fx-font-weight: 800; -fx-padding: 8 0;"
                        + "-fx-background-radius: 10; -fx-cursor: hand;");
            }
        } else {
            if (commentToggleButton != null) {
                commentToggleButton.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.65);"
                        + "-fx-font-size: 13; -fx-font-weight: 700; -fx-padding: 8 0;"
                        + "-fx-background-radius: 10; -fx-cursor: hand;");
            }
        }
    }

    @FXML
    private void handleSendComment() {
        if (commentField == null || currentPost == null) return;
        String text = commentField.getText().trim();
        if (text.isEmpty()) return;
        if (SessionManager.getCurrentUser() == null) return;

        Commentaire c = new Commentaire(currentPost.getPost_id(), SessionManager.getCurrentUser().getId(), text);
        try {
            serviceCommentaire.ajouter(c);
            commentField.clear();
            loadComments();
            refreshCommentCount();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadComments() {
        if (commentsContainer == null || currentPost == null) return;
        commentsContainer.getChildren().clear();
        try {
            List<Commentaire> comments = serviceCommentaire.getByPost(currentPost.getPost_id());
            for (Commentaire c : comments) {
                commentsContainer.getChildren().add(buildCommentCard(c, false));
            }
            if (comments.isEmpty()) {
                Label empty = new Label("Aucun commentaire. Soyez le premier !");
                empty.setStyle("-fx-text-fill: rgba(255,255,255,0.3); -fx-font-size: 12; -fx-padding: 10 14;");
                commentsContainer.getChildren().add(empty);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Build a full comment card including like, dislike, reply. isReply=true for nested replies. */
    private VBox buildCommentCard(Commentaire c, boolean isReply) {
        personne author = servicePersonne.getOneById(c.getUser_id());
        String initials = "?", name = "Utilisateur";
        if (author != null) {
            initials = (author.getPrenom().substring(0,1) + author.getNom().substring(0,1)).toUpperCase();
            name = author.getPrenom() + " " + author.getNom();
        }
        int cid = c.getCommentaire_id();

        // ── Avatar
        javafx.scene.layout.StackPane avatar = new javafx.scene.layout.StackPane();
        Circle circle = new Circle(isReply ? 12 : 16);
        circle.setStyle("-fx-fill: rgba(80,200,120,0.2);");
        Label avatarLbl = new Label(initials);
        avatarLbl.setStyle("-fx-text-fill: #50C878; -fx-font-weight: 900; -fx-font-size: " + (isReply ? 9 : 11) + ";");
        avatar.getChildren().addAll(circle, avatarLbl);
        avatar.setMinWidth(isReply ? 24 : 32); avatar.setMinHeight(isReply ? 24 : 32);

        // ── Name + content
        Label nameLbl = new Label(name);
        nameLbl.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 11; -fx-font-weight: 800;");
        Label contentLbl = new Label(c.getContenu());
        contentLbl.setWrapText(true);
        contentLbl.setStyle("-fx-text-fill: rgba(255,255,255,0.88); -fx-font-size: " + (isReply ? 12 : 13) + ";");

        // ── Like / Dislike / Delete action bar
        Button likeBtn = new Button("👍 " + c.getLikes());
        Button dislikeBtn = new Button("👎 " + c.getDislikes());
        String btnBase = "-fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 11; -fx-padding: 3 8;";
        String likeStyle    = btnBase + "-fx-background-color: rgba(80,200,120,0.10); -fx-text-fill: rgba(255,255,255,0.55);";
        String likeActiveStyle = btnBase + "-fx-background-color: rgba(80,200,120,0.22); -fx-text-fill: #50C878; -fx-font-weight: 800;";
        String dislikeStyle    = btnBase + "-fx-background-color: rgba(200,80,80,0.10); -fx-text-fill: rgba(255,255,255,0.55);";
        String dislikeActiveStyle = btnBase + "-fx-background-color: rgba(200,80,80,0.22); -fx-text-fill: #E05C5C; -fx-font-weight: 800;";

        // Restore session state
        likeBtn.setStyle(likedCommentIds.contains(cid) ? likeActiveStyle : likeStyle);
        dislikeBtn.setStyle(dislikedCommentIds.contains(cid) ? dislikeActiveStyle : dislikeStyle);

        // Mutable like/dislike counts
        final int[] likes    = { c.getLikes() };
        final int[] dislikes = { c.getDislikes() };

        likeBtn.setOnAction(e -> {
            try {
                if (likedCommentIds.contains(cid)) {
                    serviceCommentaire.unlikeComment(cid);
                    likedCommentIds.remove(cid);
                    likes[0] = Math.max(0, likes[0] - 1);
                    likeBtn.setStyle(likeStyle);
                } else {
                    serviceCommentaire.likeComment(cid);
                    likedCommentIds.add(cid);
                    likes[0]++;
                    likeBtn.setStyle(likeActiveStyle);
                    // Remove dislike if active
                    if (dislikedCommentIds.contains(cid)) {
                        serviceCommentaire.undislikeComment(cid);
                        dislikedCommentIds.remove(cid);
                        dislikes[0] = Math.max(0, dislikes[0] - 1);
                        dislikeBtn.setText("👎 " + dislikes[0]);
                        dislikeBtn.setStyle(dislikeStyle);
                    }
                }
                likeBtn.setText("👍 " + likes[0]);
            } catch (SQLException ex) { ex.printStackTrace(); }
        });

        dislikeBtn.setOnAction(e -> {
            try {
                if (dislikedCommentIds.contains(cid)) {
                    serviceCommentaire.undislikeComment(cid);
                    dislikedCommentIds.remove(cid);
                    dislikes[0] = Math.max(0, dislikes[0] - 1);
                    dislikeBtn.setStyle(dislikeStyle);
                } else {
                    serviceCommentaire.dislikeComment(cid);
                    dislikedCommentIds.add(cid);
                    dislikes[0]++;
                    dislikeBtn.setStyle(dislikeActiveStyle);
                    // Remove like if active
                    if (likedCommentIds.contains(cid)) {
                        serviceCommentaire.unlikeComment(cid);
                        likedCommentIds.remove(cid);
                        likes[0] = Math.max(0, likes[0] - 1);
                        likeBtn.setText("👍 " + likes[0]);
                        likeBtn.setStyle(likeStyle);
                    }
                }
                dislikeBtn.setText("👎 " + dislikes[0]);
            } catch (SQLException ex) { ex.printStackTrace(); }
        });

        // ── Action bar
        HBox actionBar = new HBox(6, likeBtn, dislikeBtn);
        actionBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        actionBar.setStyle("-fx-padding: 4 0 2 0;");

        // ── Reply section (only for top-level comments)
        VBox repliesArea = new VBox(4);
        repliesArea.setStyle("-fx-padding: 6 0 0 24;");
        repliesArea.setVisible(false); repliesArea.setManaged(false);

        Button replyToggleBtn = null;
        if (!isReply) {
            // Load replies count label
            final int[] replyCount = {0};
            try { replyCount[0] = serviceCommentaire.getReplies(cid).size(); } catch (SQLException ignored) {}
            replyToggleBtn = new Button("💬 Répondre" + (replyCount[0] > 0 ? " (" + replyCount[0] + ")" : ""));
            replyToggleBtn.setStyle(btnBase + "-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.45); -fx-font-size: 11;");
            final Button rBtn = replyToggleBtn;
            final boolean[] repliesOpen = {false};

            rBtn.setOnAction(e -> {
                repliesOpen[0] = !repliesOpen[0];
                repliesArea.setVisible(repliesOpen[0]);
                repliesArea.setManaged(repliesOpen[0]);
                if (repliesOpen[0]) {
                    repliesArea.getChildren().clear();
                    // Load existing replies
                    try {
                        List<Commentaire> replies = serviceCommentaire.getReplies(cid);
                        for (Commentaire r : replies) {
                            repliesArea.getChildren().add(buildCommentCard(r, true));
                        }
                    } catch (SQLException ex) { ex.printStackTrace(); }

                    // Reply input row
                    if (SessionManager.getCurrentUser() != null) {
                        TextField replyField = new TextField();
                        replyField.setPromptText("Écrire une réponse...");
                        replyField.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: white;"
                                + "-fx-prompt-text-fill: rgba(255,255,255,0.3); -fx-background-radius: 10;"
                                + "-fx-border-color: rgba(80,200,120,0.3); -fx-border-radius: 10; -fx-padding: 6 10;");
                        Button sendReplyBtn = new Button("↩ Envoyer");
                        sendReplyBtn.setStyle("-fx-background-color: linear-gradient(to right,#50C878,#3DAF62);"
                                + "-fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 10;"
                                + "-fx-cursor: hand; -fx-padding: 6 14;");
                        HBox.setHgrow(replyField, javafx.scene.layout.Priority.ALWAYS);
                        HBox replyInputRow = new HBox(6, replyField, sendReplyBtn);
                        replyInputRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                        // Enter key support
                        replyField.setOnKeyPressed(ev -> {
                            if (ev.getCode() == javafx.scene.input.KeyCode.ENTER) sendReplyBtn.fire();
                        });

                        sendReplyBtn.setOnAction(ev -> {
                            String txt = replyField.getText().trim();
                            if (txt.isEmpty()) return;
                            try {
                                Commentaire reply = new Commentaire(
                                    currentPost.getPost_id(),
                                    SessionManager.getCurrentUser().getId(),
                                    txt, cid);
                                serviceCommentaire.ajouter(reply);
                                replyField.clear();
                                // Refresh replies in place
                                repliesArea.getChildren().clear();
                                List<Commentaire> updatedReplies = serviceCommentaire.getReplies(cid);
                                for (Commentaire r : updatedReplies) {
                                    repliesArea.getChildren().add(buildCommentCard(r, true));
                                }
                                repliesArea.getChildren().add(replyInputRow);
                                rBtn.setText("💬 Répondre (" + updatedReplies.size() + ")");
                                refreshCommentCount();
                            } catch (SQLException ex) { ex.printStackTrace(); }
                        });
                        repliesArea.getChildren().add(replyInputRow);
                    }
                    rBtn.setStyle(btnBase + "-fx-background-color: rgba(80,200,120,0.08); -fx-text-fill: #50C878; -fx-font-size: 11;");
                } else {
                    repliesArea.getChildren().clear();
                    rBtn.setStyle(btnBase + "-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.45); -fx-font-size: 11;");
                }
            });
            actionBar.getChildren().add(rBtn);
        }

        // ── Delete button
        boolean canDelete = SessionManager.getCurrentUser() != null &&
            (c.getUser_id() == SessionManager.getCurrentUser().getId() ||
             "admin".equalsIgnoreCase(SessionManager.getCurrentUser().getRole()));
        if (canDelete) {
            Button delBtn = new Button("✕");
            delBtn.setStyle("-fx-background-color: rgba(200,60,60,0.12); -fx-text-fill: #E05C5C;"
                    + "-fx-font-size: 10; -fx-padding: 3 7; -fx-background-radius: 8; -fx-cursor: hand;");
            delBtn.setOnAction(e -> {
                try {
                    serviceCommentaire.supprimer(cid);
                    loadComments();
                    refreshCommentCount();
                } catch (SQLException ex) { ex.printStackTrace(); }
            });
            actionBar.getChildren().add(delBtn);
        }

        // ── Bubble: name + content + action bar
        VBox bubble = new VBox(2, nameLbl, contentLbl, actionBar);
        bubble.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 12;"
                + "-fx-padding: 8 12; -fx-border-color: rgba(80,200,120,0.12);"
                + "-fx-border-radius: 12; -fx-border-width: 1;");
        HBox.setHgrow(bubble, javafx.scene.layout.Priority.ALWAYS);

        // ── Main row
        HBox row = new HBox(8, avatar, bubble);
        row.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        row.setStyle("-fx-padding: " + (isReply ? "2 4 2 4;" : "6 14 4 14;"));

        // ── Wrap row + replies in a VBox
        VBox card = new VBox(0, row, repliesArea);
        return card;
    }

    /** Kept for back-compat: wraps buildCommentCard */
    @SuppressWarnings("unused")
    private javafx.scene.layout.HBox buildCommentRow(Commentaire c) {
        // Return just the HBox row part inside the card
        VBox card = buildCommentCard(c, false);
        return (javafx.scene.layout.HBox) card.getChildren().get(0);
    }

    private void refreshCommentCount() {
        if (commentsCountLabel == null || currentPost == null) return;
        try {
            int count = serviceCommentaire.countByPost(currentPost.getPost_id());
            commentsCountLabel.setText(count + " Commentaire" + (count > 1 ? "s" : ""));
        } catch (SQLException e) {
            e.printStackTrace();
        }
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

                if (onRefresh != null) {
                    // Refresh the parent list in-place
                    onRefresh.run();
                } else {
                    // Fallback: reload entire scene
                    Parent root = FXMLLoader.load(getClass().getResource("/poste-forumviews/ListPost.fxml"));
                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.show();
                }

            } catch (SQLException e) {
                e.printStackTrace();
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Erreur");
                errorAlert.setHeaderText("Action impossible");
                errorAlert.setContentText("Une erreur est survenue lors de la suppression.");
                errorAlert.showAndWait();
            } catch (IOException e) {
                e.printStackTrace();
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
