package org.example.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.example.entities.Poste;
import org.example.services.ServicePoste;
import org.example.utils.ForumPdfExporter;
import org.example.utils.SessionManager;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin-only controller for viewing / managing posts.
 * Set the static filter before navigating to AdminPostView.fxml:
 *   AdminListPostController.setForumFilter(forumId, forumName);
 *   MainApp.switchScene("/org/example/AdminPostView.fxml", "...");
 * Use clearFilter() for "all posts" mode.
 */
public class AdminListPostController {

    // ─── Static Filter (set before navigation) ────────────────────────────────
    private static Integer forumFilter = null;
    private static String  forumName   = null;

    public static void setForumFilter(Integer forumId, String name) {
        forumFilter = forumId;
        forumName   = name;
    }

    public static void clearFilter() {
        forumFilter = null;
        forumName   = null;
    }

    // ─── FXML Fields ──────────────────────────────────────────────────────────
    @FXML private VBox       postsContainer;
    @FXML private StackPane  rootStackPane;
    @FXML private ImageView  bgImageView;
    @FXML private StackPane  formOverlay;
    @FXML private VBox       formContainer;
    @FXML private TextField  searchField;
    @FXML private Label      statTotalLabel;
    @FXML private Label      statLikesLabel;
    @FXML private Label      headerSubtitleLabel;

    private final ServicePoste servicePoste = new ServicePoste();
    private List<Poste> allPosts;

    // ─── Init ─────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        // Update header subtitle to show which forum's posts we're viewing
        if (headerSubtitleLabel != null) {
            if (forumFilter != null && forumName != null) {
                headerSubtitleLabel.setText("Forum : " + forumName);
            } else {
                headerSubtitleLabel.setText("Toutes les publications");
            }
        }

        loadPosts();
        setupHeroBackground();

        // Hide formOverlay initially
        if (formOverlay != null) {
            formOverlay.setVisible(false);
            formOverlay.setManaged(false);
        }
    }

    // ─── Loading ──────────────────────────────────────────────────────────────
    public void loadPosts() {
        if (postsContainer == null) return;
        postsContainer.getChildren().clear();
        try {
            allPosts = (forumFilter != null)
                    ? servicePoste.afficherParForum(forumFilter)
                    : servicePoste.afficherOrphelins();
            updateStatTiles(allPosts);
            renderPosts(allPosts);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void renderPosts(List<Poste> list) {
        postsContainer.getChildren().clear();
        if (list == null) return;
        for (Poste p : list) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/poste-forumviews/PostItem.fxml"));
                Node node = loader.load();
                PostItemController controller = loader.getController();
                controller.setData(p);
                controller.setOnRefresh(this::loadPosts);
                postsContainer.getChildren().add(node);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateStatTiles(List<Poste> list) {
        if (statTotalLabel != null)
            statTotalLabel.setText(String.valueOf(list.size()));
        if (statLikesLabel != null)
            statLikesLabel.setText(String.valueOf(list.stream().mapToInt(Poste::getLikes).sum()));
    }

    // ─── Search ───────────────────────────────────────────────────────────────
    @FXML
    private void handleSearch() {
        if (allPosts == null) return;
        String query = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        if (query.isEmpty()) {
            renderPosts(allPosts);
            return;
        }
        List<Poste> filtered = allPosts.stream()
                .filter(p -> (p.getContenu() != null && p.getContenu().toLowerCase().contains(query))
                          || (p.getType()    != null && p.getType().toLowerCase().contains(query)))
                .collect(Collectors.toList());
        renderPosts(filtered);
    }

    // ─── PDF Export ───────────────────────────────────────────────────────────
    @FXML
    private void handleExportPdf() {
        if (allPosts == null || allPosts.isEmpty()) {
            showAlert("Export", "Aucune publication à exporter.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les publications en PDF");
        chooser.setInitialFileName("govibe-publications-admin.pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        File file = chooser.showSaveDialog(rootStackPane.getScene().getWindow());
        if (file == null) return;
        try {
            ForumPdfExporter.exportPosts(allPosts, file);
            showAlert("Succès ✅", "PDF exporté avec succès :\n" + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de l'export PDF:\n" + e.getMessage());
        }
    }

    // ─── Overlay – Add Post ───────────────────────────────────────────────────
    @FXML
    private void handleAjoutPost(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/poste-forumviews/AjoutPost.fxml"));
            Parent root = loader.load();
            AjoutPostController controller = loader.getController();
            controller.setAdminListPostOverlayController(this);
            showFormOverlay(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ─── Navigation ───────────────────────────────────────────────────────────
    @FXML
    private void handleGoToForums(ActionEvent event) {
        clearFilter();
        org.example.mains.MainApp.switchScene("/org/example/AdminForumView.fxml", "Forums Admin");
    }

    @FXML
    private void handleGoToPosts(ActionEvent event) {
        loadPosts();
    }

    // ─── Overlay Animations ───────────────────────────────────────────────────
    public void showFormOverlay(Parent formRoot) {
        if (formOverlay == null || formContainer == null) return;
        formContainer.getChildren().clear();
        formContainer.getChildren().add(formRoot);
        formOverlay.setVisible(true);
        formOverlay.setManaged(true);

        // Blur every background child (all except the form overlay)
        GaussianBlur blur = new GaussianBlur(8);
        if (rootStackPane != null) {
            for (javafx.scene.Node child : rootStackPane.getChildren()) {
                if (child != formOverlay) child.setEffect(blur);
            }
        }

        // Overlay fade-in
        FadeTransition fadeIn = new FadeTransition(Duration.millis(280), formOverlay);
        fadeIn.setFromValue(0.0); fadeIn.setToValue(1.0);

        // Form card: slide up + scale in
        formContainer.setScaleX(0.92); formContainer.setScaleY(0.92);
        TranslateTransition slideUp = new TranslateTransition(Duration.millis(300), formContainer);
        slideUp.setFromY(60); slideUp.setToY(0);
        javafx.animation.ScaleTransition scaleIn =
                new javafx.animation.ScaleTransition(Duration.millis(300), formContainer);
        scaleIn.setToX(1.0); scaleIn.setToY(1.0);

        new ParallelTransition(fadeIn, slideUp, scaleIn).play();
    }

    public void hideFormOverlay() {
        if (formOverlay == null || formContainer == null) return;
        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), formOverlay);
        fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);
        TranslateTransition slideDown = new TranslateTransition(Duration.millis(250), formContainer);
        slideDown.setFromY(0); slideDown.setToY(50);

        ParallelTransition pt = new ParallelTransition(fadeOut, slideDown);
        pt.setOnFinished(e -> {
            formOverlay.setVisible(false);
            formOverlay.setManaged(false);
            formContainer.getChildren().clear();
            // Remove blur from all background children
            if (rootStackPane != null) {
                for (javafx.scene.Node child : rootStackPane.getChildren()) {
                    if (child != formOverlay) child.setEffect(null);
                }
            }
            loadPosts();
        });
        pt.play();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private void setupHeroBackground() {
        if (bgImageView != null && rootStackPane != null) {
            bgImageView.fitWidthProperty().bind(rootStackPane.widthProperty());
            bgImageView.fitHeightProperty().bind(rootStackPane.heightProperty());
            var url = getClass().getResource("/messages/home-hero5.png");
            if (url != null) bgImageView.setImage(new Image(url.toExternalForm(), true));
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
