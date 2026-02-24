package org.example.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.assistant.CommandRouter;
import org.example.assistant.VoiceAssistantService;
import org.example.entities.personne;
import org.example.mains.MainApp;
import org.example.services.ServicePersonne;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {

    @FXML private TextField tfEmail;
    @FXML private PasswordField tfPassword;
    @FXML private Label errorLabel;

    private ServicePersonne servicePersonne;

    @FXML
    public void initialize() {
        initVoiceAssistant();
    }

    // ── Voice assistant support ───────────────────────────────────────────────

    private void initVoiceAssistant() {
        VoiceAssistantService vas = MainApp.getVoiceAssistant();
        if (vas == null) return;

        CommandRouter.ControllerProxy loginProxy = new CommandRouter.ControllerProxy() {

            @Override
            public void openBooking() { /* not applicable on login screen */ }

            @Override
            public void cancelBooking() {
                // "annuler" on login → clear both fields
                tfEmail.clear();
                tfPassword.clear();
                tfEmail.requestFocus();
            }

            @Override
            public void showBookingsTab() { /* not applicable */ }

            @Override
            public void showSearchTab() { /* not applicable */ }

            @Override
            public void triggerPayment() { /* not applicable */ }

            @Override
            public String describeScreen() {
                return "Écran de connexion GoVibe. " +
                       "Saisissez votre adresse email, puis votre mot de passe, puis dites Connexion pour vous connecter. " +
                       "Dites Créer un compte pour vous inscrire. " +
                       "Dites Email pour aller au champ email, Mot de passe pour le champ mot de passe.";
            }

            @Override
            public void performLogin() {
                Platform.runLater(() -> handleLogin());
            }

            @Override
            public void focusEmailField() {
                Platform.runLater(() -> tfEmail.requestFocus());
            }

            @Override
            public void focusPasswordField() {
                Platform.runLater(() -> tfPassword.requestFocus());
            }

            @Override
            public void navigateToSignup() {
                Platform.runLater(() -> handleSignup());
            }
        };

        MainApp.setVoiceProxy(loginProxy);

        // Announce the login screen after a short delay (let screen render first)
        Thread announcer = new Thread(() -> {
            try { Thread.sleep(700); } catch (InterruptedException ignored) {}
            vas.speak(
                "Écran de connexion GoVibe. " +
                "Dites Email pour saisir votre adresse, Mot de passe pour le mot de passe, " +
                "puis Connexion pour vous connecter."
            );
        });
        announcer.setDaemon(true);
        announcer.start();
    }

    @FXML
    private void handleLogin() {
        String email = tfEmail.getText().trim();
        String password = tfPassword.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        try {
            if (servicePersonne == null) {
                servicePersonne = new ServicePersonne();
            }
        } catch (RuntimeException e) {
            showError("Base de donnees indisponible. Verifiez que MySQL est demarre.");
            return;
        }

        try {
            personne user = servicePersonne.login(email, password);
            if (user != null) {
                SessionManager.setCurrentUser(user);
                // Show splash loading screen, then route to dashboard
                showSplashThenNavigate(user);
            } else {
                showError("Email ou mot de passe incorrect.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur de connexion a la base de donnees.");
        }
    }

    /**
     * Show animated splash screen with GoVibe logo, then navigate to dashboard.
     */
    private void showSplashThenNavigate(personne user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/SplashView.fxml"));
            Parent splashRoot = loader.load();
            SplashController splashController = loader.getController();

            Stage stage = (Stage) tfEmail.getScene().getWindow();

            // Swap to splash
            if (stage.getScene() != null) {
                stage.getScene().setRoot(splashRoot);
            } else {
                stage.setScene(new Scene(splashRoot));
            }
            stage.setMaximized(true);

            // After splash animation completes, navigate to the real destination
            splashController.playAnimation(() -> {
                if ("admin".equalsIgnoreCase(user.getRole())) {
                    loadAdminDashboard(user);
                } else {
                    loadUserHome(user);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            // Fallback: navigate directly without splash
            if ("admin".equalsIgnoreCase(user.getRole())) {
                loadAdminDashboard(user);
            } else {
                loadUserHome(user);
            }
        }
    }

    @FXML
    private void handleForgotPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/ForgotPasswordView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tfEmail.getScene().getWindow();

            // Swap root — no new Scene — keeps maximized, no flicker
            if (stage.getScene() != null) {
                stage.getScene().setRoot(root);
            } else {
                stage.setScene(new Scene(root));
            }
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Impossible de charger la vue mot de passe oublie.");
        }
    }

    @FXML
    private void handleSignup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/SignupView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tfEmail.getScene().getWindow();

            // Swap root — smooth transition
            if (stage.getScene() != null) {
                stage.getScene().setRoot(root);
            } else {
                stage.setScene(new Scene(root));
            }
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Impossible de charger la vue d'inscription.");
        }
    }

    private void loadAdminDashboard(personne user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/AdminDashboardView.fxml"));
            Parent root = loader.load();
            AdminDashboardController controller = loader.getController();
            controller.initData(user);
            Stage stage = (Stage) getActiveStage();
            stage.setTitle("GoVibe - Administration");

            if (stage.getScene() != null) {
                Scene scene = stage.getScene();
                scene.setRoot(root);
                // Add unified stylesheet for admin views
                String unifiedCss = getClass().getResource("/styles/unified-styles.css").toExternalForm();
                if (!scene.getStylesheets().contains(unifiedCss)) {
                    scene.getStylesheets().add(unifiedCss);
                }
            } else {
                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/styles/unified-styles.css").toExternalForm());
                stage.setScene(scene);
            }
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors du chargement du dashboard.");
        }
    }

    private void loadUserHome(personne user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/UserHomeView.fxml"));
            Parent root = loader.load();
            UserHomeController controller = loader.getController();
            controller.initData(user);
            Stage stage = getActiveStage();
            stage.setTitle("GoVibe - Accueil");

            if (stage.getScene() != null) {
                Scene scene = stage.getScene();
                scene.setRoot(root);
                String unifiedCss = getClass().getResource("/styles/unified-styles.css").toExternalForm();
                if (!scene.getStylesheets().contains(unifiedCss)) {
                    scene.getStylesheets().add(unifiedCss);
                }
            } else {
                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/styles/unified-styles.css").toExternalForm());
                stage.setScene(scene);
            }
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors du chargement de l'accueil.");
        }
    }

    /**
     * Get the active stage – works even after splash screen (when tfEmail may be detached).
     */
    private Stage getActiveStage() {
        if (tfEmail != null && tfEmail.getScene() != null) {
            return (Stage) tfEmail.getScene().getWindow();
        }
        return javafx.stage.Stage.getWindows().stream()
                .filter(w -> w instanceof Stage)
                .map(w -> (Stage) w)
                .filter(s -> s.getScene() != null)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No active stage found"));
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
