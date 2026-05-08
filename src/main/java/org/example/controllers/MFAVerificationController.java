package org.example.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.entities.personne;
import org.example.mains.MainApp;
import org.example.services.*;
import org.example.utils.SessionManager;

import java.io.IOException;

/**
 * Controller for the MFA verification screen.
 * Handles OTP input and validation after a risky login is detected.
 */
public class MFAVerificationController {

    @FXML private TextField tfOtp;
    @FXML private Label lblMessage;
    @FXML private Label lblRiskLevel;
    @FXML private Label lblRiskDescription;
    @FXML private Button btnVerify;
    @FXML private Hyperlink linkResend;
    @FXML private ProgressIndicator progressIndicator;

    private personne pendingUser;
    private double riskScore;
    private String authLevel;

    private final OTPService otpService = new OTPService();
    private final LoginAttemptService loginAttemptService = new LoginAttemptService();
    private final UserSessionService userSessionService = new UserSessionService();
    private final GeoIPService geoIPService = new GeoIPService();

    @FXML
    public void initialize() {
        if (progressIndicator != null) {
            progressIndicator.setVisible(false);
        }
    }

    /**
     * Initialize with the pending MFA user data from SessionManager.
     */
    public void initData() {
        this.pendingUser = SessionManager.getPendingMfaUser();
        this.riskScore = SessionManager.getPendingRiskScore();
        this.authLevel = SessionManager.getPendingAuthLevel();

        if (pendingUser == null) {
            showMessage("Erreur: aucun utilisateur en attente de vérification.", true);
            return;
        }

        // Update risk level display
        if (lblRiskLevel != null) {
            lblRiskLevel.setText(authLevel);
            if ("VERY_HIGH".equals(authLevel)) {
                lblRiskLevel.setStyle("-fx-text-fill: #ff4444; -fx-font-weight: bold;");
            } else {
                lblRiskLevel.setStyle("-fx-text-fill: #ffaa00; -fx-font-weight: bold;");
            }
        }

        if (lblRiskDescription != null) {
            lblRiskDescription.setText(String.format("Score de risque: %.1f%% — Vérification OTP requise", riskScore * 100));
        }

        // Generate and send OTP off the FX thread (network call)
        new Thread(() -> {
            OTPService.OTPResult result = otpService.generateAndSendOTP(pendingUser.getId(), pendingUser.getEmail());
            Platform.runLater(() -> {
                if (result.emailSent) {
                    showMessage("Un code OTP a été envoyé à " + maskEmail(pendingUser.getEmail()), false);
                } else {
                    // Email not configured / SMTP failure — show code directly (dev-mode fallback)
                    showMessage("⚠️ Email non configuré — code visible ci-dessous (mode développement)", true);
                    showDevCodeDialog(result.code);
                }
            });
        }, "MFA-SendOTP").start();
        showMessage("Envoi du code en cours...", false);
    }

    @FXML
    private void handleVerifyOTP() {
        String code = tfOtp.getText().trim();
        if (code.isEmpty()) {
            showMessage("Veuillez entrer le code OTP.", true);
            return;
        }

        if (progressIndicator != null) progressIndicator.setVisible(true);
        btnVerify.setDisable(true);

        // Run all blocking work (DB + network) off the FX thread
        new Thread(() -> {
            boolean valid = otpService.validateOTP(pendingUser.getId(), code);

            if (valid) {
                String ip = geoIPService.getExternalIP();
                GeoIPService.GeoInfo geo = geoIPService.getGeoInfo(ip);
                loginAttemptService.recordAttempt(pendingUser.getId(), ip,
                        System.getProperty("os.name"), geo.getCountry(), true, riskScore, authLevel);
                String sessionId = userSessionService.createSession(pendingUser.getId(), ip,
                        System.getProperty("os.name"), geo.getCountry(), geo.getCity());

                SessionManager.setCurrentUser(pendingUser);
                SessionManager.setSessionId(sessionId);
                SessionManager.clearPendingMfa();

                System.out.println("✅ [MFA] User " + pendingUser.getEmail() + " authenticated via OTP (risk=" +
                        String.format("%.2f", riskScore) + ", level=" + authLevel + ")");

                Platform.runLater(() -> {
                    showMessage("✅ Vérification réussie!", false);
                    if ("admin".equalsIgnoreCase(pendingUser.getRole())) {
                        loadAdminDashboard(pendingUser);
                    } else {
                        loadUserHome(pendingUser);
                    }
                });
            } else {
                Platform.runLater(() -> {
                    showMessage("❌ Code OTP invalide ou expiré. Réessayez.", true);
                    btnVerify.setDisable(false);
                    if (progressIndicator != null) progressIndicator.setVisible(false);
                });
            }
        }, "MFA-Verify").start();
    }

    @FXML
    private void handleResendOTP() {
        if (pendingUser != null) {
            showMessage("Envoi du code en cours...", false);
            new Thread(() -> {
                OTPService.OTPResult result = otpService.generateAndSendOTP(pendingUser.getId(), pendingUser.getEmail());
                Platform.runLater(() -> {
                    if (result.emailSent) {
                        showMessage("Nouveau code OTP envoyé à " + maskEmail(pendingUser.getEmail()), false);
                    } else {
                        showMessage("⚠️ Email non configuré — code visible dans la fenêtre (mode développement)", true);
                        showDevCodeDialog(result.code);
                    }
                });
            }, "MFA-Resend").start();
        }
    }

    @FXML
    private void handleBackToLogin() {
        SessionManager.clearPendingMfa();
        MainApp.switchScene("/org/example/LoginView.fxml", "GoVibe — Connexion");
    }

    private void showMessage(String message, boolean isError) {
        if (lblMessage != null) {
            lblMessage.setText(message);
            lblMessage.setStyle(isError ? "-fx-text-fill: #ff6b6b;" : "-fx-text-fill: #50C878;");
            lblMessage.setVisible(true);
        }
    }

    /**
     * Shows a dialog with the raw OTP code when email delivery is unavailable.
     * This is intentionally only shown in dev/testing mode (when SMTP is unconfigured).
     */
    private void showDevCodeDialog(String code) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("🛠️ Mode Développement — Code OTP");
        alert.setHeaderText("Email SMTP non configuré");
        alert.setContentText(
            "Votre code OTP est :\n\n" + code + "\n\n" +
            "Pour activer l'envoi réel, renseignez mail.password\n" +
            "dans src/main/resources/config/oauth2.properties\n" +
            "(utilisez un mot de passe d'application Gmail).");
        // Pre-fill the OTP field for convenience
        if (tfOtp != null) {
            tfOtp.setText(code);
        }
        alert.showAndWait();
    }

    /**
     * Masks email for display (e.g., "m****@gmail.com")
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int at = email.indexOf('@');
        if (at <= 1) return email;
        return email.charAt(0) + "****" + email.substring(at);
    }

    // ── Dashboard navigation (same pattern as LoginController) ──────────

    private void loadAdminDashboard(personne user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/AdminDashboardView.fxml"));
            Parent root = loader.load();
            AdminDashboardController controller = loader.getController();
            controller.initData(user);

            Stage stage = getActiveStage();
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
        }
    }

    private Stage getActiveStage() {
        if (tfOtp != null && tfOtp.getScene() != null) {
            return (Stage) tfOtp.getScene().getWindow();
        }
        return javafx.stage.Stage.getWindows().stream()
                .filter(w -> w instanceof Stage)
                .map(w -> (Stage) w)
                .filter(s -> s.getScene() != null)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No active stage found"));
    }
}
