package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.entities.Personne;
import tn.esprit.services.ServicePersonne;
import org.example.services.FaceRecognitionService;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    private final ServicePersonne servicePersonne = new ServicePersonne();
    private final FaceRecognitionService faceService = new FaceRecognitionService();

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champs vides", "Veuillez remplir tous les champs.");
            return;
        }

        Personne user = servicePersonne.login(email, password);
        if (user != null) {
            // Login successful
            tn.esprit.mains.MainApp.loggedInUser = user;

            try {
                // Navigate to ListPost
                Parent root = FXMLLoader.load(getClass().getResource("/poste-forumviews/ListPost.fxml"));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Échec de connexion", "Email ou mot de passe incorrect.");
        }
    }

    @FXML
    private void handleLoginFaceID(ActionEvent event) {
        String email = emailField.getText();
        if (email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Email requis", "Veuillez entrer votre email d'abord pour utiliser Face ID.");
            return;
        }

        Personne user = servicePersonne.getOneByEmail(email);
        if (user == null || user.getFaceEncoding() == null || user.getFaceEncoding().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Non configuré", "Face ID n'est pas configuré pour cet utilisateur ou l'email est invalide.");
            return;
        }

        showAlert(Alert.AlertType.INFORMATION, "Vérification Face ID", "La caméra va s'ouvrir. Regardez l'objectif.");
        boolean isVerified = faceService.verifyFace(user.getFaceEncoding());

        if (isVerified) {
            tn.esprit.mains.MainApp.loggedInUser = user;
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/poste-forumviews/ListPost.fxml"));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Échec de connexion", "Visage non reconnu ou délai dépassé.");
        }
    }

    @FXML
    private void handleGoToSignUp(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/poste-forumviews/SignUp.fxml"));
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
}
