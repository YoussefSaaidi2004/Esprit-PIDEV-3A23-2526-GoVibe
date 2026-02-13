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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.entities.Forum;
import tn.esprit.services.ServiceForum;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class ModifierForumController {

    @FXML
    private TextField nameField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private ToggleButton privateToggle;
    @FXML
    private TextField imagePathField;

    private Forum currentForum;
    private final ServiceForum serviceForum = new ServiceForum();

    public void initData(Forum f) {
        this.currentForum = f;
        nameField.setText(f.getName());
        descriptionArea.setText(f.getDescription());
        privateToggle.setSelected(f.isIs_private());
        imagePathField.setText(f.getImage());

        if (f.isIs_private()) {
            privateToggle.setText("🔒 Privé");
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
    private void handleUpdate(ActionEvent event) {
        String name = nameField.getText();
        String description = descriptionArea.getText();
        String image = imagePathField.getText();
        boolean isPrivate = privateToggle.isSelected();

        if (name.trim().isEmpty() || description.trim().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Information manquante");
            alert.setContentText("Veuillez remplir le nom et la description.");
            alert.showAndWait();
            return;
        }

        currentForum.setName(name);
        currentForum.setDescription(description);
        currentForum.setImage(image);
        currentForum.setIs_private(isPrivate);

        try {
            serviceForum.modifier(currentForum);
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
    private void handleLogout(ActionEvent event) {
        // Logique de déconnexion
        System.out.println("Déconnexion demandée depuis ModifierForum");
    }
}
