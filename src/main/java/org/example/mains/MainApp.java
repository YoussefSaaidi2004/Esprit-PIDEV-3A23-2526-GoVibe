package org.example.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.assistant.CommandRouter;
import org.example.assistant.VoiceAssistantService;

public class MainApp extends Application {

    // ── Global voice assistant (survives scene switches) ─────────────────────
    private static VoiceAssistantService voiceAssistant;
    private static CommandRouter globalRouter;

    /**
     * Holds the most-recently requested proxy while the router is still
     * initialising on the background thread.  Applied as soon as the router
     * is ready so that even the login screen's proxy is never lost.
     */
    private static volatile CommandRouter.ControllerProxy pendingProxy = null;

    /** Called by each controller to wire its UI actions into the voice router. */
    public static void setVoiceProxy(CommandRouter.ControllerProxy proxy) {
        if (globalRouter != null) {
            globalRouter.setProxy(proxy);
        } else {
            // Router not ready yet — store for application once it is.
            pendingProxy = proxy;
            System.out.println("[VoiceAssistant] Stored pending proxy (router not yet ready).");
        }
    }

    public static VoiceAssistantService getVoiceAssistant() { return voiceAssistant; }

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/LoginView.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles/unified-styles.css").toExternalForm());

        primaryStage.setTitle("GoVibe - Connexion");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.setResizable(true);
        primaryStage.show();

        // Initialise the offline voice assistant once — it stays alive for the
        // entire application lifetime, across all scene switches.
        initGlobalVoiceAssistant();
    }

    private static void initGlobalVoiceAssistant() {
        // Safe-mode check to bypass initialization if it causes native crashes
        if ("true".equalsIgnoreCase(System.getProperty("govibe.voice.disabled"))) {
            System.err.println("[VoiceAssistant] Voice Assistant DISABLED via system property.");
            return;
        }

        Thread t = new Thread(() -> {
            System.out.println("[VoiceAssistant] Initialising global assistant...");
            try {
                voiceAssistant = VoiceAssistantService.getInstance();
                // Default no-op proxy until a controller registers itself.
                globalRouter = new CommandRouter(voiceAssistant, null);
                // Apply any proxy that was registered before the router was ready
                // (e.g. LoginController.initialize() fires before this thread finishes).
                CommandRouter.ControllerProxy pending = pendingProxy;
                if (pending != null) {
                    globalRouter.setProxy(pending);
                    pendingProxy = null;
                    System.out.println("[VoiceAssistant] Applied pending proxy to router.");
                }
                voiceAssistant.setCommandListener(globalRouter);
                // NOTE: startListening() is now called INSIDE VoiceAssistantService.initVosk()
                // after the Vosk model has fully loaded, to avoid a race condition where
                // the Recognizer JNI was constructed before the model was ready (causing a
                // Windows native heap corruption crash, exit code -805306369).
                // For SAPI-only mode (no Vosk model), startListening() is called from
                // VoiceAssistantService.detectSapi() once SAPI availability is confirmed.
                System.out.println("[VoiceAssistant] Global assistant ready. STT=deferred-until-model-load");
                // NOTE: startup greeting removed from Java — Python plays it independently
                // from its own user_context handler when logged_in=false is detected.
                // Queueing it here caused the same message to play TWICE (Java flush +
                // Python startup TTS overlap), generating extra TTS echo that Vosk would
                // then misprocess as false user commands (echo feedback loop).
            } catch (Throwable e) {
                System.err.println("[VoiceAssistant] FATAL failure during initialization: " + e.getMessage());
                e.printStackTrace();
            }
        }, "VoiceAssistant-Init");
        t.setDaemon(true);
        t.start();
    }


    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Called by the JavaFX runtime when the application window is closed.
     * Kills all voice-assistant subprocesses (edge-tts, SAPI) so they do not
     * linger as zombie OS processes after the JVM exits.
     */
    @Override
    public void stop() {
        if (voiceAssistant != null) {
            voiceAssistant.shutdown();
        }
    }

    private static Stage primaryStage;
    private static boolean isDarkTheme = false;

    public static boolean isDarkTheme() {
        return isDarkTheme;
    }

    public static void switchScene(String fxmlPath, String title) {
        try {
            System.out.println("[Nav] Switching scene to " + fxmlPath);
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(fxmlPath));
            javafx.scene.Parent root = loader.load();
            
            if (isDarkTheme) {
                root.getStyleClass().add("dark-mode");
            }
            
            if (primaryStage != null) {
                if (primaryStage.getScene() != null) {
                    // Swap root — no new Scene — stays maximized, no flicker
                    Scene scene = primaryStage.getScene();
                    scene.setRoot(root);
                    String unifiedCss = MainApp.class.getResource("/styles/unified-styles.css").toExternalForm();
                    if (!scene.getStylesheets().contains(unifiedCss)) {
                        scene.getStylesheets().add(unifiedCss);
                    }
                } else {
                    Scene scene = new Scene(root);
                    scene.getStylesheets().add(MainApp.class.getResource("/styles/unified-styles.css").toExternalForm());
                    primaryStage.setScene(scene);
                }
                primaryStage.setTitle(title);
                primaryStage.setMaximized(true);
                System.out.println("[Nav] Scene loaded: " + title);
            }
        } catch (Exception e) {
            System.err.println("Error switching scene: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        if (primaryStage != null && primaryStage.getScene() != null) {
            if (isDarkTheme) {
                if (!primaryStage.getScene().getRoot().getStyleClass().contains("dark-mode")) {
                    primaryStage.getScene().getRoot().getStyleClass().add("dark-mode");
                }
            } else {
                primaryStage.getScene().getRoot().getStyleClass().remove("dark-mode");
            }
        }
    }
}
