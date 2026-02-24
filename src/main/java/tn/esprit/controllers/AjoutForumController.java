package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.entities.Forum;
import tn.esprit.services.ServiceForum;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;

public class AjoutForumController {

    @FXML
    private Label roleLabel;

    @FXML
    private TextField nameField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private ToggleButton privateToggle;
    @FXML
    private TextField imagePathField;

    private final ServiceForum serviceForum = new ServiceForum();

    @FXML
    public void initialize() {
        syncSidebarRole();
    }

    private void syncSidebarRole() {
        if (roleLabel != null && tn.esprit.mains.MainApp.loggedInUser != null) {
            String role = tn.esprit.mains.MainApp.loggedInUser.getRole();
            if ("admin".equalsIgnoreCase(role)) {
                roleLabel.setText("Espace admin");
            } else {
                roleLabel.setText("Espace client");
            }
        }
    }

    @FXML
    private void handleChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir l'image du forum");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png"));
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            imagePathField.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    private void handleSave(ActionEvent event) {
        String name = nameField.getText();
        String description = descriptionArea.getText();
        String image = imagePathField.getText();
        boolean isPrivate = privateToggle.isSelected();

        boolean isValid = true;
        StringBuilder errorMessage = new StringBuilder();

        if (name == null || name.trim().isEmpty()) {
            nameField.getStyleClass().add("error-border");
            errorMessage.append("Le nom du forum ne peut pas être vide.\n");
            isValid = false;
        } else if (name.length() > 50) {
            nameField.getStyleClass().add("error-border");
            errorMessage.append("Le nom du forum ne peut pas dépasser 50 caractères.\n");
            isValid = false;
        } else {
            nameField.getStyleClass().remove("error-border");
        }

        if (description == null || description.trim().isEmpty()) {
            descriptionArea.getStyleClass().add("error-border");
            errorMessage.append("La description ne peut pas être vide.\n");
            isValid = false;
        } else if (description.length() > 500) {
            descriptionArea.getStyleClass().add("error-border");
            errorMessage.append("La description ne peut pas dépasser 500 caractères.\n");
            isValid = false;
        } else {
            descriptionArea.getStyleClass().remove("error-border");
        }

        if (image == null || image.trim().isEmpty()) {
            imagePathField.getStyleClass().add("error-border");
            errorMessage.append("L'image de couverture est obligatoire.\n");
            isValid = false;
        } else {
            imagePathField.getStyleClass().remove("error-border");
        }

        if (!isValid) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation échouée");
            alert.setHeaderText(null);
            alert.setContentText(errorMessage.toString());
            alert.showAndWait();
            return;
        }

        // Check if user is logged in
        if (tn.esprit.mains.MainApp.loggedInUser == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setContentText("Vous devez être connecté pour créer un forum.");
            alert.showAndWait();

            // Optional: Redirect to login
            handleLogout(event);
            return;
        }

        Forum f = new Forum(name, image, tn.esprit.mains.MainApp.loggedInUser.getId(), 0, 0, description,
                new Timestamp(System.currentTimeMillis()), isPrivate);

        try {
            serviceForum.ajouter(f);
            handleCancel(event);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
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
        handleCancel(event);
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
