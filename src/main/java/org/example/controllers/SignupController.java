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

    private ServicePersonne servicePersonne;
    private FaceRecognitionService faceService;
    private String configuredFaceEncoding = null;

    @FXML
    public void initialize() {
        // Lazy-init services to avoid blocking the FX thread on DB connections
        // during FXML load — the connection is only created when first needed.
        /*
        try {
            servicePersonne = new ServicePersonne();
        } catch (Exception e) {
            System.err.println("[Signup] ServicePersonne init deferred: " + e.getMessage());
        }
        */
        try {
            faceService = new FaceRecognitionService();
        } catch (Exception e) {
            System.err.println("[Signup] FaceRecognitionService init failed (Face ID disabled): " + e.getMessage());
        }
    }

    @FXML
    private void handleSignup() {
        // ── Capture ALL UI state on the FX thread FIRST ──────────────────────
        // Reading JavaFX controls from a background thread causes native heap
        // corruption (exit code 0xC0000005) because the JavaFX render pipeline
        // owns those memory regions.
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

        // ── Capture role on FX thread (CRITICAL — was crashing before) ───────
        final String role = (rbAdmin != null && rbAdmin.isSelected()) ? "admin" : "user";
        final String faceEncoding = configuredFaceEncoding;

        btnSignup.setDisable(true);
        showError("Traitement en cours...", false);

        Task<Void> signupTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Ensure ServicePersonne is initialized on the background thread
                // to prevent native COM STA crashes caused by java.sql.DriverManager
                if (servicePersonne == null) {
                    try {
                        servicePersonne = new ServicePersonne();
                    } catch (Exception e) {
                        throw new RuntimeException("Base de données indisponible: " + e.getMessage());
                    }
                }
                
                final ServicePersonne svc = servicePersonne;

                // All values used here are final locals captured on the FX thread.
                // No JavaFX UI controls are touched from this background thread.

                // Validation: Email uniqueness
                if (svc.emailExists(email)) {
                    throw new RuntimeException("Cet email est deja utilise.");
                }

                // Create new user (role was captured on FX thread)
                personne newUser = new personne(nom, prenom, email, password, role);
                if (faceEncoding != null) {
                    newUser.setFaceEncoding(faceEncoding);
                }
                svc.ajouter(newUser);
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
            if (ex != null) ex.printStackTrace();
        });

        Thread t = new Thread(signupTask, "Signup-Task");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleConfigureFaceID() {
        if (faceService == null) {
            showError("Face ID non disponible sur cette machine.", true);
            return;
        }
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

        Thread t = new Thread(faceTask, "FaceID-Task");
        t.setDaemon(true);
        t.start();
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


