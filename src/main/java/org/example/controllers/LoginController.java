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
import org.example.services.*;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.Set;

/**
 * Login controller with integrated AI Risk Scoring and Adaptive MFA.
 *
 * Flow:
 * 1. User enters email + password
 * 2. Password verification
 * 3. Extract behavioral features (failed_attempts, new_device, new_country, unusual_time, is_admin)
 * 4. Call AI model (Python Flask API) for risk scoring
 * 5. MFA decision: LOW → direct login, HIGH/VERY_HIGH → OTP verification
 */
public class LoginController {

    @FXML private TextField tfEmail;
    @FXML private PasswordField tfPassword;
    @FXML private TextField tfPasswordVisible;
    @FXML private Button btnTogglePassword;
    @FXML private Label errorLabel;
    @FXML private Button btnGoogleLogin;

    private boolean isPasswordVisible = false;

    private ServicePersonne servicePersonne;
    private LoginAttemptService loginAttemptService;
    private UserSessionService userSessionService;
    private RiskScoringService riskScoringService;
    private GeoIPService geoIPService;
    private MFAService mfaService;
    private FaceRecognitionService faceService;

    @FXML
    public void initialize() {
        if (tfPasswordVisible != null && tfPassword != null) {
            tfPasswordVisible.textProperty().bindBidirectional(tfPassword.textProperty());
        }

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

    /**
     * Lazily initializes all MFA-related services.
     */
    private void initServices() {
        if (servicePersonne == null) {
            servicePersonne = new ServicePersonne();
        }
        if (loginAttemptService == null) {
            loginAttemptService = new LoginAttemptService();
        }
        if (userSessionService == null) {
            userSessionService = new UserSessionService();
        }
        if (riskScoringService == null) {
            riskScoringService = new RiskScoringService();
        }
        if (geoIPService == null) {
            geoIPService = new GeoIPService();
        }
        if (mfaService == null) {
            mfaService = new MFAService();
        }
        if (faceService == null) {
            faceService = new FaceRecognitionService();
        }
    }

    @FXML
    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        tfPasswordVisible.setVisible(isPasswordVisible);
        tfPassword.setVisible(!isPasswordVisible);
        btnTogglePassword.setText(isPasswordVisible ? "🙈" : "👁");
    }

    // ── Voice assistant support ───────────────────────────────────────────────

    private void initVoiceAssistant() {
        CommandRouter.ControllerProxy loginProxy = new CommandRouter.ControllerProxy() {

            @Override
            public void openBooking() { /* not applicable on login screen */ }

            @Override
            public void cancelBooking() {
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

            @Override
            public void openCamera() {
                Platform.runLater(() -> handleLoginFaceID());
            }
        };

        MainApp.setVoiceProxy(loginProxy);
    }

    // ── Login with AI Risk Scoring ──────────────────────────────────────────

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

        btnTogglePassword.setDisable(true);
        // Show a "Logging in..." message
        showError("Connexion en cours...");

        javafx.concurrent.Task<Void> loginTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    initServices();
                } catch (RuntimeException e) {
                    throw new RuntimeException("Base de donnees indisponible. Verifiez que MySQL est demarre.");
                }

                // ── Step 0: Check if account exists and is locked ────────────────
                personne existingUser = servicePersonne.getOneByEmail(email);
                if (existingUser != null && existingUser.isAccountLocked()) {
                    if (existingUser.getLockoutUntil() != null && existingUser.getLockoutUntil().before(new Timestamp(System.currentTimeMillis()))) {
                        servicePersonne.unlockAccount(existingUser.getId());
                        existingUser.setAccountLocked(false);
                        existingUser.setLockoutUntil(null);
                    } else {
                        throw new RuntimeException("Votre compte est temporairement verrouillé. Veuillez réessayer plus tard.");
                    }
                }

                // ── Step 1: Verify password ──────────────────────────────────
                personne user = servicePersonne.login(email, password);

                if (user == null) {
                    if (existingUser != null) {
                        String ip = geoIPService.getExternalIP();
                        GeoIPService.GeoInfo geo = geoIPService.getGeoInfo(ip);
                        loginAttemptService.recordAttempt(existingUser.getId(), ip,
                                System.getProperty("os.name"), geo.getCountry(), false, 0, "NONE");
                        
                        int recentFailures = loginAttemptService.getRecentFailedCount(existingUser.getId(), 30);
                        if (recentFailures >= 5) {
                            servicePersonne.lockAccount(existingUser.getId(), 60);
                            throw new RuntimeException("Trop de tentatives échouées. Compte verrouillé pour 1 heure.");
                        }
                    }
                    throw new RuntimeException("Email ou mot de passe incorrect.");
                }

                // ── Step 3: Extract behavioral features ──────────────────────
                String ip = geoIPService.getExternalIP();
                GeoIPService.GeoInfo geo = geoIPService.getGeoInfo(ip);

                int failedAttempts = loginAttemptService.getRecentFailedCount(user.getId(), 30);
                Set<String> knownDevices = userSessionService.getUserDevices(user.getId());
                Set<String> knownCountries = userSessionService.getUserCountries(user.getId());

                String currentDevice = System.getProperty("os.name");
                String currentCountry = geo.getCountry();

                int newDevice = (knownDevices == null || knownDevices.isEmpty()) ? 0 :
                        (knownDevices.contains(currentDevice.toLowerCase()) ? 0 : 1);
                int newCountry = (knownCountries == null || knownCountries.isEmpty()) ? 0 :
                        (knownCountries.contains(currentCountry.toLowerCase()) ? 0 : 1);

                LocalTime now = LocalTime.now();
                int unusualTime = (now.getHour() >= 0 && now.getHour() < 5) ? 1 : 0;
                int isAdmin = "admin".equalsIgnoreCase(user.getRole()) ? 1 : 0;

                // ── Step 4: Call AI Risk Scoring ─────────────────────────────
                RiskScoringService.RiskResult riskResult = riskScoringService.evaluateRisk(
                        failedAttempts, newDevice, newCountry, unusualTime, isAdmin);

                // ── Step 5: MFA Decision ─────────────────────────────────────
                MFAService.AuthLevel authLevel = mfaService.determineAuthLevel(user, riskResult.getProbability());

                Platform.runLater(() -> {
                    if (authLevel == MFAService.AuthLevel.LOW) {
                        new Thread(() -> {
                            try {
                                loginAttemptService.recordAttempt(user.getId(), ip, currentDevice,
                                        currentCountry, true, riskResult.getProbability(), "LOW");
                                String sessionId = userSessionService.createSession(user.getId(), ip,
                                        currentDevice, currentCountry, geo.getCity());

                                Platform.runLater(() -> {
                                    SessionManager.setCurrentUser(user);
                                    SessionManager.setSessionId(sessionId);
                                    showSplashThenNavigate(user);
                                });
                            } catch (Exception ex) { ex.printStackTrace(); }
                        }).start();
                    } else {
                        SessionManager.setPendingMfaUser(user);
                        SessionManager.setPendingRiskScore(riskResult.getProbability());
                        SessionManager.setPendingAuthLevel(authLevel.name());
                        navigateToMFAVerification();
                    }
                });
                return null;
            }
        };

        loginTask.setOnFailed(e -> {
            btnTogglePassword.setDisable(false);
            Throwable ex = loginTask.getException();
            String msg = ex.getMessage();
            showError(msg);
            
            VoiceAssistantService vas = MainApp.getVoiceAssistant();
            if (vas != null) {
                if (msg.contains("MySQL")) vas.vivianSpeak("Database is down. Check MySQL.");
                else if (msg.contains("verrouillé")) vas.vivianSpeak("Account is locked.");
                else if (msg.contains("Email")) vas.vivianSpeak("Invalid credentials.");
            }
        });

        new Thread(loginTask).start();
    }

    @FXML
    private void handleLoginFaceID() {
        String email = tfEmail.getText().trim();
        if (email.isEmpty()) {
            showError("Veuillez entrer votre email pour utiliser Face ID.");
            return;
        }

        showError("Vérification Face ID en cours...");

        javafx.concurrent.Task<Boolean> faceTask = new javafx.concurrent.Task<>() {
            @Override
            protected Boolean call() throws Exception {
                initServices();
                personne user = servicePersonne.getOneByEmail(email);
                if (user == null || user.getFaceEncoding() == null || user.getFaceEncoding().isEmpty()) {
                    throw new RuntimeException("Face ID n'est pas configuré pour cet utilisateur.");
                }

                if (user.isAccountLocked() && user.getLockoutUntil() != null && user.getLockoutUntil().after(new Timestamp(System.currentTimeMillis()))) {
                    throw new RuntimeException("Votre compte est temporairement verrouillé.");
                }

                Platform.runLater(() -> showError("La caméra va s'ouvrir. Regardez l'objectif."));
                return faceService.verifyFace(user.getFaceEncoding());
            }
        };

        faceTask.setOnSucceeded(e -> {
            if (faceTask.getValue()) {
                // Verified — now perform the login logic in the background
                new Thread(() -> {
                    try {
                        personne user = servicePersonne.getOneByEmail(email);
                        String ip = geoIPService.getExternalIP();
                        GeoIPService.GeoInfo geo = geoIPService.getGeoInfo(ip);
                        String currentDevice = System.getProperty("os.name");

                        loginAttemptService.recordAttempt(user.getId(), ip, currentDevice, geo.getCountry(), true, 0.0, "LOW");
                        String sessionId = userSessionService.createSession(user.getId(), ip, currentDevice, geo.getCountry(), geo.getCity());

                        Platform.runLater(() -> {
                            SessionManager.setCurrentUser(user);
                            SessionManager.setSessionId(sessionId);
                            System.out.println("✅ [Login] Direct Face ID login for " + user.getEmail());
                            showSplashThenNavigate(user);
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Platform.runLater(() -> showError("Erreur lors de la finalisation de la session."));
                    }
                }).start();
            } else {
                showError("Visage non reconnu ou délai dépassé.");
            }
        });

        faceTask.setOnFailed(e -> {
            Throwable ex = faceTask.getException();
            showError(ex.getMessage());
        });

        new Thread(faceTask).start();
    }

    /**
     * Navigate to the MFA verification screen.
     */
    private void navigateToMFAVerification() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/MFAVerificationView.fxml"));
            Parent root = loader.load();
            MFAVerificationController controller = loader.getController();
            controller.initData();

            Stage stage = getActiveStage();
            stage.setTitle("GoVibe — Vérification de sécurité");
            if (stage.getScene() != null) {
                stage.getScene().setRoot(root);
            } else {
                stage.setScene(new Scene(root));
            }
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors du chargement de la vérification MFA.");
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

            Stage stage = getActiveStage();

            if (stage.getScene() != null) {
                stage.getScene().setRoot(splashRoot);
            } else {
                stage.setScene(new Scene(splashRoot));
            }
            stage.setMaximized(true);

            splashController.playAnimation(() -> {
                if ("admin".equalsIgnoreCase(user.getRole())) {
                    loadAdminDashboard(user);
                } else {
                    loadUserHome(user);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            if ("admin".equalsIgnoreCase(user.getRole())) {
                loadAdminDashboard(user);
            } else {
                loadUserHome(user);
            }
        }
    }

    // ── Connexion Google OAuth2 ─────────────────────────────────────────────

    @FXML
    private void handleGoogleLogin() {
        if (btnGoogleLogin != null) {
            btnGoogleLogin.setDisable(true);
            btnGoogleLogin.setText("⏳  Connexion en cours...");
        }
        errorLabel.setVisible(false);

        GoogleOAuth2Service oAuth2Service = new GoogleOAuth2Service();
        oAuth2Service.authenticate().thenAccept(user -> {
            Platform.runLater(() -> {
                if (user != null) {
                    SessionManager.setCurrentUser(user);
                    System.out.println("✅ [LoginController] Utilisateur connecté via Google : " + user.getEmail());
                    showSplashThenNavigate(user);
                } else {
                    showError("La connexion Google a échoué ou a été annulée.");
                    resetGoogleButton();
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                ex.printStackTrace();
                showError("Erreur lors de la connexion Google : " + ex.getMessage());
                resetGoogleButton();
            });
            return null;
        });
    }

    private void resetGoogleButton() {
        if (btnGoogleLogin != null) {
            btnGoogleLogin.setDisable(false);
            btnGoogleLogin.setText("\uD83D\uDD35  Se connecter avec Google");
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    private void handleForgotPassword() {
        MainApp.switchScene("/org/example/ForgotPasswordView.fxml", "GoVibe \u2014 Mot de passe oublié");
    }

    @FXML
    private void handleSignup() {
        MainApp.switchScene("/org/example/SignupView.fxml", "GoVibe \u2014 Inscription");
    }

    private void loadAdminDashboard(personne user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/AdminDashboardView.fxml"));
            Parent root = loader.load();
            AdminDashboardController controller = loader.getController();
            controller.initData(user);
            VoiceAssistantService vas = MainApp.getVoiceAssistant();
            if (vas != null) vas.notifyUserLoggedIn(user.getPrenom());
            Stage stage = (Stage) getActiveStage();
            stage.setTitle("GoVibe - Administration");

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
            showError("Erreur lors du chargement du dashboard.");
        }
    }

    private void loadUserHome(personne user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/UserHomeView.fxml"));
            Parent root = loader.load();
            UserHomeController controller = loader.getController();
            controller.initData(user);
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
