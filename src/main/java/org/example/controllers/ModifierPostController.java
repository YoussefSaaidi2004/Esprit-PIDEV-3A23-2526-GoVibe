package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.entities.Poste;
import org.example.services.ServicePoste;
import org.example.utils.SessionManager;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;

public class ModifierPostController {

    private final ServicePoste servicePoste = new ServicePoste();
    private Poste currentPost;
    
    // Popup integration
    private DetailsForumController parentController;
    public void setOverlayController(DetailsForumController parent) {
        this.parentController = parent;
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
    private Label charCountLabel;

    @FXML
    public void initialize() {
        syncSidebarRole();
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

        contenuArea.textProperty().addListener((obs, oldVal, newVal) -> {
            charCountLabel.setText(newVal.length() + " / 500");
        });
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

    public void initData(Poste p) {
        this.currentPost = p;
        contenuArea.setText(p.getContenu());
        urlField.setText(p.getUrl());

        if ("MEDIA".equals(p.getType())) {
            mediaToggle.setSelected(true);
            if (p.getUrl() != null && !p.getUrl().isEmpty()) {
                File file = new File(p.getUrl());
                if (file.exists()) {
                    mediaPreview.setImage(new Image(file.toURI().toString()));
                }
            }
        } else {
            statusToggle.setSelected(true);
            mediaUrlContainer.setVisible(false);
            mediaUrlContainer.setManaged(false);
            previewContainer.setVisible(false);
            previewContainer.setManaged(false);
        }
    }

    @FXML
    private void handleUploadMedia() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un média");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png"));
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            urlField.setText(selectedFile.getAbsolutePath());
            mediaPreview.setImage(new Image(selectedFile.toURI().toString()));
        }
    }

    @FXML
    private void handleUpdate(ActionEvent event) {
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
            showAlert(Alert.AlertType.WARNING, "Validation échouée", errorMessage.toString());
            return;
        }

        currentPost.setContenu(contenu);
        currentPost.setUrl(url);
        currentPost.setType(type);
        currentPost.setDate_modification(new Timestamp(System.currentTimeMillis()));

        try {
            servicePoste.modifier(currentPost);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Publication mise à jour avec succès !");
            navigateBack(event);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Une erreur est survenue : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        navigateBack(event);
    }

    private void navigateBack(ActionEvent event) {
        if (parentController != null) {
            parentController.hideFormOverlay();
            return;
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/poste-forumviews/ListPost.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
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
    private void handlePersonnes() {
    }

    @FXML
    private void handleGoToPosts(ActionEvent event) {
        navigateBack(event);
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
        SessionManager.clear();
        org.example.mains.MainApp.switchScene("/org/example/LoginView.fxml", "GoVibe - Connexion");
    }
}
