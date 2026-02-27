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
        // Clear session whenever the login screen loads (covers both first launch
        // and returning from logout so SessionManager state is always reset).
        SessionManager.clear();

        // Tell the Python voice agent the user is logged out — this completes
        // Python's PROCESSING_LOGOUT state machine so it runs the farewell
        // sequence and returns to SLEEPING instead of hanging indefinitely.
        VoiceAssistantService vas = MainApp.getVoiceAssistant();
        if (vas != null) vas.notifyUserLoggedOut();

        initVoiceAssistant();
    }

    // ── Voice assistant support ───────────────────────────────────────────────

    private void initVoiceAssistant() {
        // Always register the proxy — MainApp.setVoiceProxy() stores it as
        // pendingProxy if the router isn't ready yet and applies it once it is.
        // Do NOT guard with getVoiceAssistant() here: VA initialises on a
        // background thread AFTER the login scene loads, so it is always null
        // at this point, causing the proxy to never be registered.
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

        // NOTE: Echo's startup speech covers the session open; Login screen
        // guidance is available via the DESCRIBE ("what's here") voice command.
        // Firing vivianSpeak() at 700 ms races with Echo's startup and the
        // Python agent may not be ready yet, causing stale speech.
    }

    @FXML
    private void handleLogin() {
        String email = tfEmail.getText().trim();
        String password = tfPassword.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            VoiceAssistantService vas = MainApp.getVoiceAssistant();
            if (vas != null) vas.vivianSpeak("Oops — you forgot to fill in all the fields. Email and password, please!");
            return;
        }

        try {
            if (servicePersonne == null) {
                servicePersonne = new ServicePersonne();
            }
        } catch (RuntimeException e) {
            showError("Base de donnees indisponible. Verifiez que MySQL est demarre.");
            VoiceAssistantService vas = MainApp.getVoiceAssistant();
            if (vas != null) vas.vivianSpeak("Uh oh — the database isn't running. Make sure MySQL is started and try again.");
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
                VoiceAssistantService vas = MainApp.getVoiceAssistant();
                if (vas != null) vas.vivianSpeak("Hmm, that email or password doesn't match anything I know. Want to try again?");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur de connexion a la base de donnees.");
            VoiceAssistantService vas = MainApp.getVoiceAssistant();
            if (vas != null) vas.vivianSpeak("Database error — something went wrong on my end. Please try again.");
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

            // Use getActiveStage() — avoids tfEmail.getScene().getWindow() NPE when
            // called via the voice proxy path (scene may not be attached to the node).
            Stage stage = getActiveStage();

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
        // Use MainApp.switchScene() — avoids tfEmail.getScene().getWindow() NPE
        // when this is called via the voice assistant proxy (scene can be null
        // if the node is not yet attached at the time of the Platform.runLater).
        MainApp.switchScene("/org/example/ForgotPasswordView.fxml", "GoVibe \u2014 Mot de passe oubli\u00e9");
    }

    @FXML
    private void handleSignup() {
        // Use MainApp.switchScene() — same reason as handleForgotPassword above.
        // The voice path calls navigateToSignup() → Platform.runLater(handleSignup)
        // and at that point tfEmail.getScene() can be null, causing a NPE.
        MainApp.switchScene("/org/example/SignupView.fxml", "GoVibe \u2014 Inscription");
    }

    private void loadAdminDashboard(personne user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/AdminDashboardView.fxml"));
            Parent root = loader.load();
            AdminDashboardController controller = loader.getController();
            controller.initData(user);
            // Notify voice agent that an admin just logged in.
            VoiceAssistantService vas = MainApp.getVoiceAssistant();
            if (vas != null) vas.notifyUserLoggedIn(user.getPrenom());
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
            // Notify voice agent that login succeeded so Echo+Vivian greet the user.
            VoiceAssistantService vas = MainApp.getVoiceAssistant();
            if (vas != null) vas.notifyUserLoggedIn(user.getPrenom());
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
