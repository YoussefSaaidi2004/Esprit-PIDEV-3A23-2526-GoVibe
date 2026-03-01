package org.example.controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.entities.personne;
import org.example.services.ServicePersonne;
import org.example.services.FaceRecognitionService;

import java.io.IOException;
import java.sql.SQLException;

public class SignupController {

    @FXML private TextField tfNom;
    @FXML private TextField tfPrenom;
    @FXML private TextField tfEmail;
    @FXML private PasswordField tfPassword;
    @FXML private Label errorLabel;
    @FXML private Button btnSignup;
    @FXML private Button btnFaceID;
    
    @FXML private RadioButton rbUser;
    @FXML private RadioButton rbAdmin;
    @FXML private ToggleGroup roleGroup;

    private final ServicePersonne servicePersonne = new ServicePersonne();
    private final FaceRecognitionService faceService = new FaceRecognitionService();
    private String configuredFaceEncoding = null;

    @FXML
    private void handleSignup() {
        String nom = tfNom.getText().trim();
        String prenom = tfPrenom.getText().trim();
        String email = tfEmail.getText().trim();
        String password = tfPassword.getText().trim();
        
        // Validation: Empty fields
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.", true);
            return;
        }

        // Validation: Role selected
        if (roleGroup.getSelectedToggle() == null) {
            showError("Veuillez selectionner un role.", true);
            return;
        }

        // Validation: Password length
        if (password.length() < 6) {
            showError("Le mot de passe doit contenir au moins 6 caracteres.", true);
            return;
        }
        
        // Validation: Email format
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            showError("Format d'email invalide.", true);
            return;
        }

        btnSignup.setDisable(true);
        showError("Traitement en cours...", false);

        Task<Void> signupTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Validation: Email uniqueness
                if (servicePersonne.emailExists(email)) {
                    throw new RuntimeException("Cet email est deja utilise.");
                }

                // Get selected role
                String role = rbAdmin.isSelected() ? "admin" : "user";
                
                // Create new user
                personne newUser = new personne(nom, prenom, email, password, role);
                if (configuredFaceEncoding != null) {
                    newUser.setFaceEncoding(configuredFaceEncoding);
                }
                servicePersonne.ajouter(newUser);
                return null;
            }
        };

        signupTask.setOnSucceeded(e -> {
            btnSignup.setDisable(false);
            showError("Compte cree avec succes ! Redirection...", false);
            
            // Wait a bit so user can see the message
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    Platform.runLater(() -> handleLoginLink());
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

        signupTask.setOnFailed(e -> {
            btnSignup.setDisable(false);
            Throwable ex = signupTask.getException();
            String msg = (ex instanceof RuntimeException) ? ex.getMessage() : "Erreur lors de la creation du compte.";
            showError(msg, true);
            ex.printStackTrace();
        });

        new Thread(signupTask).start();
    }

    @FXML
    private void handleConfigureFaceID() {
        showError("La caméra va s'ouvrir. Regardez l'objectif.", false);
        btnFaceID.setDisable(true);

        Task<String> faceTask = new Task<>() {
            @Override
            protected String call() {
                return faceService.registerFaceEncoding();
            }
        };

        faceTask.setOnSucceeded(e -> {
            btnFaceID.setDisable(false);
            String encoding = faceTask.getValue();
            if (encoding != null && !encoding.isEmpty()) {
                configuredFaceEncoding = encoding;
                showError("Visage enregistré avec succès !", false);
            } else {
                showError("L'enregistrement du visage a échoué.", true);
            }
        });

        faceTask.setOnFailed(e -> {
            btnFaceID.setDisable(false);
            showError("Erreur lors de l'accès à la caméra.", true);
        });

        new Thread(faceTask).start();
    }

    @FXML
    private void handleLoginLink() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tfEmail.getScene().getWindow();

            if (stage.getScene() != null) {
                stage.getScene().setRoot(root);
            } else {
                stage.setScene(new Scene(root));
            }
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String message, boolean isError) {
        Platform.runLater(() -> {
            errorLabel.setText(message);
            errorLabel.setStyle(isError ? "-fx-text-fill: #ff6b6b;" : "-fx-text-fill: #50C878;");
            errorLabel.setVisible(true);
        });
    }
}

