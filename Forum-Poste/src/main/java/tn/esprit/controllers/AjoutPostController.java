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
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import tn.esprit.entities.Poste;
import tn.esprit.services.ServicePoste;
import java.io.IOException;
import java.sql.SQLException;

public class AjoutPostController {

    private final ServicePoste servicePoste = new ServicePoste();
    private tn.esprit.entities.Forum currentForum; // Contexte du forum (peut être null)

    public void setForum(tn.esprit.entities.Forum forum) {
        this.currentForum = forum;
        // Optionnel : changer le titre ou interface pour indiquer qu'on poste dans un
        // forum
    }

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

    // Assuming these ToggleButtons exist in the FXML
    @FXML
    private ToggleButton mediaToggle;
    @FXML
    private ToggleButton statusToggle;

    @FXML
    public void initialize() {
        // Logique pour afficher/masquer les champs média
        mediaUrlContainer.setVisible(false); // Initially hidden
        mediaUrlContainer.setManaged(false); // Initially not managed
        previewContainer.setVisible(false); // Initially hidden
        previewContainer.setManaged(false); // Initially not managed

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
            }
        });
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
                // Placeholder pour vidéo
                mediaPreview.setImage(null);
            }
        }
    }

    @FXML
    private void handlePublish(javafx.event.ActionEvent event) {
        String contenu = contenuArea.getText();
        String url = urlField.getText();
        String type = mediaToggle.isSelected() ? "MEDIA" : "STATUS";

        if (contenu.trim().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Champ vide");
            alert.setHeaderText(null);
            alert.setContentText("Le contenu de la publication ne peut pas être vide.");
            alert.showAndWait();
            return;
        }

        // Création de l'objet Poste
        int userId = 1; // Default to admin if no user logged in (fallback)
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

            // Retourner à la liste
            handleCancel(event);

        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Erreur lors de l'ajout");
            alert.setContentText(
                    "Une erreur est survenue lors de l'enregistrement dans la base de données : " + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel(javafx.event.ActionEvent event) {
        try {
            if (currentForum != null) {
                // Retour au détail du forum
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                        getClass().getResource("/poste-forumviews/DetailsForum.fxml"));
                javafx.scene.Parent root = loader.load();
                DetailsForumController controller = loader.getController();
                controller.initData(currentForum);

                javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene()
                        .getWindow();
                stage.setScene(new javafx.scene.Scene(root));
                stage.show();
            } else {
                // Retour à la liste globale
                javafx.scene.Parent root = javafx.fxml.FXMLLoader
                        .load(getClass().getResource("/poste-forumviews/ListPost.fxml"));
                javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene()
                        .getWindow();
                javafx.scene.Scene scene = new javafx.scene.Scene(root);
                stage.setScene(scene);
                stage.show();
            }
        } catch (java.io.IOException e) {
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
