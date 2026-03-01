package org.example.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.effect.BoxBlur;
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

public class ListPostController {

    @FXML private VBox postsContainer;
    @FXML private StackPane rootStackPane;
    @FXML private ImageView bgImageView;
    @FXML private StackPane formOverlay;
    @FXML private VBox formContainer;
    @FXML private TextField searchField;

    // Stats
    @FXML private Label statTotalLabel;
    @FXML private Label statLikesLabel;
    @FXML private VBox statsPanel;
    @FXML private HBox topPostsRow;

    private final ServicePoste servicePoste = new ServicePoste();
    private List<Poste> allPosts;

    @FXML
    public void initialize() {
        loadPosts();
        loadStats();
        setupHeroBackground();
    }

    // ─── Loading ─────────────────────────────────────────────────────────────

    public void loadPosts() {
        postsContainer.getChildren().clear();
        try {
            allPosts = servicePoste.afficherOrphelins();
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
                // Pass reload callback so edits/deletes refresh the list
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

    // ─── Statistics Panel ────────────────────────────────────────────────────

    private void loadStats() {
        if (topPostsRow == null) return;
        topPostsRow.getChildren().clear();
        try {
            List<Poste> top = servicePoste.getTopPostsByLikes(3);
            String[] medals = {"🥇", "🥈", "🥉"};
            for (int i = 0; i < top.size(); i++) {
                Poste p = top.get(i);
                VBox card = buildStatCard(medals[i], p);
                topPostsRow.getChildren().add(card);
            }
            if (top.isEmpty() && statsPanel != null) {
                statsPanel.setVisible(false);
                statsPanel.setManaged(false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox buildStatCard(String medal, Poste p) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPrefWidth(250);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.92); -fx-background-radius: 14; " +
                "-fx-border-color: rgba(80,200,120,0.25); -fx-border-width: 1; -fx-border-radius: 14; " +
                "-fx-padding: 14 16; -fx-effect: dropshadow(gaussian, rgba(80,200,120,0.18), 12, 0, 0, 4);");

        // Medal + likes row
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label medalLbl = new Label(medal);
        medalLbl.setStyle("-fx-font-size: 20;");

        Label likesLbl = new Label("❤️ " + p.getLikes() + " likes");
        likesLbl.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: 700; -fx-font-size: 13; " +
                "-fx-background-color: rgba(192,57,43,0.08); -fx-background-radius: 8; -fx-padding: 2 8;");

        topRow.getChildren().addAll(medalLbl, likesLbl);

        // Content excerpt
        String contenu = p.getContenu() != null ? p.getContenu() : "(Pas de contenu)";
        String excerpt = contenu.length() > 80 ? contenu.substring(0, 80) + "…" : contenu;
        Label contentLbl = new Label(excerpt);
        contentLbl.setWrapText(true);
        contentLbl.setStyle("-fx-text-fill: #4a5568; -fx-font-size: 12;");

        // Type badge
        Label typeLbl = new Label(p.getType() != null ? p.getType().toUpperCase() : "TEXTE");
        typeLbl.setStyle("-fx-text-fill: #2d7a4d; -fx-font-size: 10; -fx-font-weight: 700; " +
                "-fx-background-color: rgba(80,200,120,0.12); -fx-background-radius: 6; -fx-padding: 2 6;");

        card.getChildren().addAll(topRow, contentLbl, typeLbl);
        return card;
    }

    // ─── Search ──────────────────────────────────────────────────────────────

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
                        || (p.getType() != null && p.getType().toLowerCase().contains(query)))
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
        chooser.setInitialFileName("govibe-publications.pdf");
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

    // ─── Overlay Animations ──────────────────────────────────────────────────

    public void showFormOverlay(Parent formRoot) {
        formContainer.getChildren().clear();
        formContainer.getChildren().add(formRoot);
        formOverlay.setVisible(true);
        formOverlay.setManaged(true);

        BoxBlur blur = new BoxBlur(10, 10, 3);
        if (rootStackPane != null && rootStackPane.getChildren().size() > 1)
            rootStackPane.getChildren().get(rootStackPane.getChildren().size() - 2).setEffect(blur);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), formOverlay);
        fadeIn.setFromValue(0.0); fadeIn.setToValue(1.0);
        TranslateTransition slideUp = new TranslateTransition(Duration.millis(300), formContainer);
        slideUp.setFromY(50); slideUp.setToY(0);
        new ParallelTransition(fadeIn, slideUp).play();
    }

    public void hideFormOverlay() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), formOverlay);
        fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);
        TranslateTransition slideDown = new TranslateTransition(Duration.millis(250), formContainer);
        slideDown.setFromY(0); slideDown.setToY(50);

        ParallelTransition pt = new ParallelTransition(fadeOut, slideDown);
        pt.setOnFinished(e -> {
            formOverlay.setVisible(false);
            formOverlay.setManaged(false);
            formContainer.getChildren().clear();
            if (rootStackPane != null && rootStackPane.getChildren().size() > 1)
                rootStackPane.getChildren().get(rootStackPane.getChildren().size() - 2).setEffect(null);
            loadPosts();
            loadStats();
        });
        pt.play();
    }

    // ─── Navigation ──────────────────────────────────────────────────────────

    @FXML
    private void handleAjoutPost(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/poste-forumviews/AjoutPost.fxml"));
            Parent root = loader.load();
            AjoutPostController controller = loader.getController();
            controller.setListPostOverlayController(this);
            showFormOverlay(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleGoToForums(ActionEvent event) {
        org.example.mains.MainApp.switchScene("/poste-forumviews/ListForum.fxml", "Forums");
    }

    @FXML
    private void handleGoToPosts(ActionEvent event) { loadPosts(); }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.clear();
        org.example.mains.MainApp.switchScene("/org/example/LoginView.fxml", "GoVibe - Connexion");
    }

    // ─── Utilities ───────────────────────────────────────────────────────────

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void setupHeroBackground() {
        if (bgImageView != null && rootStackPane != null) {
            bgImageView.fitWidthProperty().bind(rootStackPane.widthProperty());
            bgImageView.fitHeightProperty().bind(rootStackPane.heightProperty());
            var url = getClass().getResource("/messages/home-hero5.png");
            if (url != null) bgImageView.setImage(new Image(url.toExternalForm()));
        }
    }
}
