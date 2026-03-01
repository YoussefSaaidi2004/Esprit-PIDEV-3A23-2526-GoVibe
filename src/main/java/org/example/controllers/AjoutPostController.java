package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import org.example.entities.Forum;
import org.example.entities.Poste;
import org.example.services.ServicePoste;
import org.example.utils.SessionManager;
import java.sql.SQLException;

public class AjoutPostController {

    private final ServicePoste servicePoste = new ServicePoste();
    private Forum currentForum;
    
    // Popup integration
    private DetailsForumController parentController;
    public void setOverlayController(DetailsForumController parent) {
        this.parentController = parent;
    }

    private ListPostController listPostController;
    public void setListPostOverlayController(ListPostController parent) {
        this.listPostController = parent;
    }

    public void setForum(Forum forum) {
        this.currentForum = forum;
    }

    @FXML
    private Label roleLabel;

    @FXML
    private TextArea contenuArea;

    @FXML
    private VBox mediaUrlContainer;

    @FXML
    private TextField urlField;

    @FXML
    private VBox previewContainer;

    @FXML
    private ImageView mediaPreview;

    @FXML
    private ToggleButton mediaToggle;
    @FXML
    private ToggleButton statusToggle;

    @FXML
    public void initialize() {
        syncSidebarRole();
        mediaUrlContainer.setVisible(false);
        mediaUrlContainer.setManaged(false);
        previewContainer.setVisible(false);
        previewContainer.setManaged(false);

        mediaToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            mediaUrlContainer.setVisible(newVal);
            mediaUrlContainer.setManaged(newVal);
            previewContainer.setVisible(newVal);
            previewContainer.setManaged(newVal);

            if (newVal) {
                statusToggle.setSelected(false);
                statusToggle
                        .setStyle("-fx-background-color: white; -fx-border-color: #A0E0C9; -fx-text-fill: #084E36;");
                mediaToggle.setStyle("-fx-background-color: white; -fx-border-color: #50C878; -fx-text-fill: #50C878;");
            }
        });

        statusToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                mediaToggle.setSelected(false);
                mediaToggle.setStyle("-fx-background-color: white; -fx-border-color: #A0E0C9; -fx-text-fill: #084E36;");
                statusToggle
                        .setStyle("-fx-background-color: white; -fx-border-color: #50C878; -fx-text-fill: #50C878;");
                urlField.setText("");
                mediaPreview.setImage(null);
            }
        });

        if (contenuArea != null) {
            contenuArea.textProperty().addListener((observable, oldValue, newValue) -> {
            });
        }
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

    @FXML
    private void handleUploadMedia() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Sélectionner un média");
        fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Images/Vidéos", "*.jpg", "*.png", "*.mp4", "*.mkv"));
        java.io.File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            urlField.setText(selectedFile.getAbsolutePath());
            if (selectedFile.getName().toLowerCase().endsWith(".png")
                    || selectedFile.getName().toLowerCase().endsWith(".jpg")) {
                mediaPreview.setImage(new javafx.scene.image.Image(selectedFile.toURI().toString()));
            } else {
                mediaPreview.setImage(null);
            }
        }
    }

    @FXML
    private void handlePublish(ActionEvent event) {
        String contenu = contenuArea.getText();
        String url = mediaToggle.isSelected() ? urlField.getText() : null;
        String type = mediaToggle.isSelected() ? "MEDIA" : "STATUS";

        boolean isValid = true;
        StringBuilder errorMessage = new StringBuilder();

        if (contenu == null || contenu.trim().isEmpty()) {
            contenuArea.getStyleClass().add("error-border");
            errorMessage.append("Le contenu ne peut pas être vide.\n");
            isValid = false;
        } else if (contenu.length() > 500) {
            contenuArea.getStyleClass().add("error-border");
            errorMessage.append("Le contenu ne peut pas dépasser 500 caractères.\n");
            isValid = false;
        } else {
            contenuArea.getStyleClass().remove("error-border");
        }

        if (mediaToggle.isSelected() && (url == null || url.trim().isEmpty())) {
            urlField.getStyleClass().add("error-border");
            errorMessage.append("Vous devez sélectionner un média (photo/vidéo) pour ce type de publication.\n");
            isValid = false;
        } else {
            urlField.getStyleClass().remove("error-border");
        }

        if (!isValid) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation échouée");
            alert.setHeaderText(null);
            alert.setContentText(errorMessage.toString());
            alert.showAndWait();
            return;
        }

        int userId = 1;
        if (SessionManager.getCurrentUser() != null) {
            userId = SessionManager.getCurrentUser().getId();
        }

        Poste p;
        if (currentForum != null) {
            p = new Poste(userId, currentForum.getForum_id(), contenu, type, url);
        } else {
            p = new Poste(userId, contenu, type, url);
        }

        try {
            servicePoste.ajouter(p);
            // Smooth transition to forum details
            handleSmoothNavigationToForum(event);
        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Erreur lors de l'ajout");
            alert.setContentText("Une erreur est survenue lors de l'enregistrement dans la base de données : " + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }

    // Smooth navigation separated from handlePublish to avoid nested method declarations
    private void handleSmoothNavigationToForum(ActionEvent event) {
        if (parentController != null) {
            parentController.hideFormOverlay();
            return;
        }
        if (listPostController != null) {
            listPostController.hideFormOverlay();
            return;
        }
        if (currentForum != null) {
            org.example.mains.MainApp.switchScene("/poste-forumviews/DetailsForum.fxml", "GoVibe - Forum");
        } else {
            org.example.mains.MainApp.switchScene("/poste-forumviews/ListPost.fxml", "GoVibe - Publications");
        }
    }
    @FXML
    private void handleCancel(ActionEvent event) {
        if (parentController != null) {
            parentController.hideFormOverlay();
            return;
        }
        if (listPostController != null) {
            listPostController.hideFormOverlay();
            return;
        }
        if (currentForum != null) {
            org.example.mains.MainApp.switchScene("/poste-forumviews/DetailsForum.fxml", "GoVibe - Forum");
        } else {
            org.example.mains.MainApp.switchScene("/poste-forumviews/ListPost.fxml", "GoVibe - Publications");
        }
    }

    @FXML
    private void handleGoToPosts(ActionEvent event) {
        handleCancel(event);
    }

    @FXML
    private void handleGoToForums(ActionEvent event) {
        org.example.mains.MainApp.switchScene("/poste-forumviews/ListForum.fxml", "GoVibe - Forums");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.clear();
        org.example.mains.MainApp.switchScene("/org/example/LoginView.fxml", "GoVibe - Connexion");
    }
}
