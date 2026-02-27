package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import tn.esprit.entities.Forum;
import tn.esprit.entities.Poste;
import tn.esprit.services.ServicePoste;
import java.io.IOException;
import java.sql.SQLException;

public class AjoutPostController {

    private final ServicePoste servicePoste = new ServicePoste();
    private Forum currentForum;

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
        if (mediaUrlContainer != null) {
            mediaUrlContainer.setVisible(false);
            mediaUrlContainer.setManaged(false);
        }
        if (previewContainer != null) {
            previewContainer.setVisible(false);
            previewContainer.setManaged(false);
        }

        if (mediaToggle != null) {
            mediaToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (mediaUrlContainer != null) {
                    mediaUrlContainer.setVisible(newVal);
                    mediaUrlContainer.setManaged(newVal);
                }
                if (previewContainer != null) {
                    previewContainer.setVisible(newVal);
                    previewContainer.setManaged(newVal);
                }

                if (newVal && statusToggle != null) {
                    statusToggle.setSelected(false);
                    statusToggle.setStyle("-fx-background-color: white; -fx-border-color: #A0E0C9; -fx-text-fill: #084E36;");
                    mediaToggle.setStyle("-fx-background-color: white; -fx-border-color: #50C878; -fx-text-fill: #50C878;");
                }
            });
        }

        if (statusToggle != null) {
            statusToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal && mediaToggle != null) {
                    mediaToggle.setSelected(false);
                    mediaToggle.setStyle("-fx-background-color: white; -fx-border-color: #A0E0C9; -fx-text-fill: #084E36;");
                    statusToggle.setStyle("-fx-background-color: white; -fx-border-color: #50C878; -fx-text-fill: #50C878;");
                    if (urlField != null) urlField.setText("");
                    if (mediaPreview != null) mediaPreview.setImage(null);
                }
            });
        }
    }

    private void syncSidebarRole() {
        try {
            if (roleLabel != null && tn.esprit.mains.MainApp.loggedInUser != null) {
                String role = tn.esprit.mains.MainApp.loggedInUser.getRole();
                if ("admin".equalsIgnoreCase(role)) {
                    roleLabel.setText("Espace admin");
                } else {
                    roleLabel.setText("Espace client");
                }
            }
        } catch (Exception ignored) {
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
            if (urlField != null) urlField.setText(selectedFile.getAbsolutePath());
            if (selectedFile.getName().toLowerCase().endsWith(".png") || selectedFile.getName().toLowerCase().endsWith(".jpg")) {
                if (mediaPreview != null) mediaPreview.setImage(new javafx.scene.image.Image(selectedFile.toURI().toString()));
            } else {
                if (mediaPreview != null) mediaPreview.setImage(null);
            }
        }
    }

    @FXML
    private void handlePublish(ActionEvent event) {
        String contenu = contenuArea != null ? contenuArea.getText() : "";
        String url = (mediaToggle != null && mediaToggle.isSelected()) ? (urlField != null ? urlField.getText() : null) : null;
        String type = (mediaToggle != null && mediaToggle.isSelected()) ? "MEDIA" : "STATUS";

        boolean isValid = true;
        StringBuilder errorMessage = new StringBuilder();

        if (contenu == null || contenu.trim().isEmpty()) {
            if (contenuArea != null) contenuArea.getStyleClass().add("error-border");
            errorMessage.append("Le contenu ne peut pas être vide.\n");
            isValid = false;
        } else if (contenu.length() > 500) {
            if (contenuArea != null) contenuArea.getStyleClass().add("error-border");
            errorMessage.append("Le contenu ne peut pas dépasser 500 caractères.\n");
            isValid = false;
        } else {
            if (contenuArea != null) contenuArea.getStyleClass().remove("error-border");
        }

        if (mediaToggle != null && mediaToggle.isSelected() && (url == null || url.trim().isEmpty())) {
            if (urlField != null) urlField.getStyleClass().add("error-border");
            errorMessage.append("Vous devez sélectionner un média (photo/vidéo) pour ce type de publication.\n");
            isValid = false;
        } else {
            if (urlField != null) urlField.getStyleClass().remove("error-border");
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
        if (tn.esprit.mains.MainApp.loggedInUser != null) {
            userId = tn.esprit.mains.MainApp.loggedInUser.getId();
        }

        Poste p;
        if (currentForum != null) {
            p = new Poste(userId, currentForum.getForum_id(), contenu, type, url);
        } else {
            p = new Poste(userId, contenu, type, url);
        }

        try {
            servicePoste.ajouter(p);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setHeaderText(null);
            alert.setContentText("Publication ajoutée avec succès !");
            alert.showAndWait();

            handleCancel(event);

        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Erreur lors de l'ajout");
            alert.setContentText("Une erreur est survenue lors de l'enregistrement dans la base de données : " + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        try {
            if (currentForum != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/poste-forumviews/DetailsForum.fxml"));
                Parent root = loader.load();
                DetailsForumController controller = loader.getController();
                controller.initData(currentForum);

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } else {
                Parent root = FXMLLoader.load(getClass().getResource("/poste-forumviews/ListPost.fxml"));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoToPosts(ActionEvent event) {
        handleCancel(event);
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
