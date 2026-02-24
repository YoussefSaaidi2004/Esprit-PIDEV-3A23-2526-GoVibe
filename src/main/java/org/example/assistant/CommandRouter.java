package org.example.assistant;

import javafx.application.Platform;
import org.example.mains.MainApp;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Routes voice commands to GoVibe UI actions.
 *
 * <p>Register it once against the {@link VoiceAssistantService} and supply a
 * {@link ControllerProxy} so it has an interface to the currently active
 * controller without creating a circular dependency.
 *
 * <pre>{@code
 * CommandRouter router = new CommandRouter(vas, proxy);
 * vas.setCommandListener(router);
 * }</pre>
 *
 * <h3>Supported commands (French / English mixed)</h3>
 * <ul>
 *   <li>{@code RESERVER / BOOK / NOUVELLE RESERVATION} — open booking modal</li>
 *   <li>{@code MES RESERVATIONS / MY BOOKINGS / RESERVATIONS} — switch to bookings tab</li>
 *   <li>{@code RECHERCHER / SEARCH / VOLS / FLIGHTS} — switch to flights search tab</li>
 *   <li>{@code ANNULER / CANCEL / RETOUR / GO BACK} — close booking modal</li>
 *   <li>{@code PAYER / PAY / PAYMENT} — trigger payment in booking form</li>
 *   <li>{@code AIDE / HELP / COMMANDES} — speak available commands</li>
 *   <li>{@code DECRIRE / DESCRIBE / QU'EST CE / QUOI} — describe current screen</li>
 *   <li>{@code ACCUEIL / DASHBOARD / HOME} — navigate to dashboard</li>
 *   <li>{@code DECONNEXION / LOGOUT / LOG OUT} — return to login screen</li>
 * </ul>
 */
public class CommandRouter implements VoiceCommandListener {

    private static final Logger LOG = Logger.getLogger(CommandRouter.class.getName());

    // ── Controller proxy ─────────────────────────────────────────────────────
    /**
     * Minimal interface that the controller implements so CommandRouter can
     * invoke UI actions without knowing the concrete controller class.
     */
    public interface ControllerProxy {
        /** Open the booking modal/form. */
        void openBooking();
        /** Close / cancel the booking modal. */
        void cancelBooking();
        /** Switch to the "my bookings" tab. */
        void showBookingsTab();
        /** Switch to the "search flights" tab. */
        void showSearchTab();
        /**
         * Trigger the payment step (noop if booking form not active).
         * Implementations should call the pay button action.
         */
        void triggerPayment();
        /** Human-readable description of the currently visible screen section. */
        String describeScreen();

        // ── Login-screen actions (optional — default no-op so other screens ignore them) ──

        /** Trigger the login button / authenticate. */
        default void performLogin() {}
        /** Focus the email / username input field. */
        default void focusEmailField() {}
        /** Focus the password input field. */
        default void focusPasswordField() {}
        /** Navigate to the sign-up / create account screen. */
        default void navigateToSignup() {}

        /**
         * Open the booking form pre-filled with Ollama-extracted parameters.
         * Default implementation falls back to {@link #openBooking()}.
         *
         * @param destination city name extracted from speech, or null
         * @param date        travel date (YYYY-MM-DD), or null
         */
        default void openBookingWith(String destination, String date) { openBooking(); }
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private final VoiceAssistantService vas;
    private volatile ControllerProxy proxy;

    // LLM-extracted parameters set by VoiceAssistantService before onCommand().
    private volatile String  pendingDestination    = null;
    private volatile String  pendingDate           = null;
    private volatile int     pendingPassengers     = 1;
    // When true, speakAndRun() skips TTS (ML already spoke a response).
    private volatile boolean suppressActionSpeech  = false;

    /**
     * Called by {@link VoiceAssistantService} with LLM-extracted parameters
     * before it fires {@link #onCommand}.  CommandRouter uses them in the
     * matching action and then clears them.
     */
    public void setVoiceContext(String destination, String date) {
        setVoiceContext(destination, date, 1);
    }

    /**
     * Overload that also carries the passenger count extracted by DeepSeek
     * function calling.
     */
    public void setVoiceContext(String destination, String date, int passengers) {
        this.pendingDestination = destination;
        this.pendingDate        = date;
        this.pendingPassengers  = (passengers > 0) ? passengers : 1;
        if (destination != null || date != null || passengers != 1)
            LOG.info("[CommandRouter] Voice context — dest=" + destination
                     + "  date=" + date + "  pax=" + this.pendingPassengers);
    }

    // Command keyword table: keyword fragment → action label
    // Order matters — more specific phrases first.
    private final Map<String, Consumer<String>> commandTable = new LinkedHashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    public CommandRouter(VoiceAssistantService vas, ControllerProxy proxy) {
        this.vas   = vas;
        this.proxy = proxy;
        buildCommandTable();
    }

    /** Update the controller proxy when the active screen changes. */
    public void setProxy(ControllerProxy proxy) {
        this.proxy = proxy;
    }

    // ── VoiceCommandListener ─────────────────────────────────────────────────
    /**
     * Strip diacritics (accents) and uppercase so that
     * "réserver" == "RESERVER", "écran" == "ECRAN", etc.
     * Both stored keys and incoming text are normalized before comparison.
     */
    private static String norm(String s) {
        if (s == null) return "";
        String nfd = Normalizer.normalize(s, Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{InCombiningDiacriticalMarks}", "").toUpperCase().trim();
    }

    @Override
    public void onCommand(String command, String rawText) {
        String n = norm(command);
        LOG.info("[CommandRouter] Received: \"" + command + "\" (normalized: \"" + n + "\")");
        for (Map.Entry<String, Consumer<String>> entry : commandTable.entrySet()) {
            if (n.contains(norm(entry.getKey()))) {
                entry.getValue().accept(command);
                return;           // first match wins
            }
        }
        // No match — ask user to repeat
        LOG.fine("[CommandRouter] No match for: \"" + command + "\"");
        speakAndRun("I heard something interesting, but I need you to repeat that clearly.", null);
    }

    /**
     * Attempts to match {@code command} against the keyword table.
     * Returns {@code true} if a handler was invoked, {@code false} if no
     * keyword matched.  Used by {@link VoiceAssistantService} to decide
     * whether to suppress the "not understood" TTS prompt.
     */
    public boolean tryKeywordMatch(String command) {
        String n = norm(command);
        for (Map.Entry<String, Consumer<String>> entry : commandTable.entrySet()) {
            if (n.contains(norm(entry.getKey()))) {
                LOG.info("[CommandRouter] Keyword fallback matched \"" + entry.getKey()
                        + "\" in: \"" + command + "\"");
                entry.getValue().accept(command);
                return true;
            }
        }
        LOG.fine("[CommandRouter] Keyword fallback: no match for \"" + command + "\"");
        return false;
    }

    // ── Command table ─────────────────────────────────────────────────────────
    private void buildCommandTable() {

        // ---- Navigation ----
        add("DECONNEXION", "logout", cmd -> navigate(
                "/org/example/LoginView.fxml", "GoVibe — Login",
                "Poof — off you go. Come back soon!"));

        add("LOGOUT",       "logout", cmd -> navigate(
                "/org/example/LoginView.fxml", "GoVibe — Login",
                "Poof — off you go. Come back soon!"));

        add("LOG OUT",      "logout", cmd -> navigate(
                "/org/example/LoginView.fxml", "GoVibe — Login",
                "Poof — off you go. Come back soon!"));

        // ---- Login screen actions ----
        add("LOGIN",        "login",           cmd -> doLogin());
        add("LOG IN",       "log in",          cmd -> doLogin());
        add("SIGN IN",      "sign in",         cmd -> doLogin());
        add("CONNEXION",    "connexion",        cmd -> doLogin());
        add("SE CONNECTER", "se connecter",     cmd -> doLogin());
        add("FOCUS_EMAIL",  "focus email",      cmd -> doFocusEmail());
        add("EMAIL",        "email",            cmd -> doFocusEmail());
        add("ADRESSE EMAIL","email address",   cmd -> doFocusEmail());
        add("USERNAME",     "username",         cmd -> doFocusEmail());
        add("FOCUS_PASSWORD","focus password",  cmd -> doFocusPassword());
        add("PASSWORD",     "password",         cmd -> doFocusPassword());
        add("MOT DE PASSE", "password",         cmd -> doFocusPassword());
        add("SIGNUP",       "signup",           cmd -> doSignup());
        add("SIGN UP",      "sign up",          cmd -> doSignup());
        add("REGISTER",     "register",         cmd -> doSignup());
        add("CREER",        "creer compte",     cmd -> doSignup());
        add("INSCRIPTION",  "inscription",      cmd -> doSignup());

        // ---- Dashboard / home ----
        add("ACCUEIL",    "dashboard", cmd -> speakAndRun("Back to base.", null));
        add("DASHBOARD",  "dashboard", cmd -> speakAndRun("Back to base.", null));
        add("HOME",       "home",      cmd -> speakAndRun("Home sweet home.", null));

        // ---- Booking ----
        add("NOUVELLE RESERVATION", "new booking", cmd -> doOpenBooking());
        add("RESERVER",   "book",    cmd -> doOpenBooking());
        add("RESERVATION","book",    cmd -> doOpenBooking());
        add("BOOK",       "book",    cmd -> doOpenBooking());

        // ---- My bookings tab ----
        add("MES RESERVATIONS", "my bookings",  cmd -> doShowBookings());
        add("MY BOOKINGS",      "my bookings",  cmd -> doShowBookings());
        add("RESERVATIONS",     "bookings",     cmd -> doShowBookings());

        // ---- Checkouts ----
        add("MY CHECKOUTS",     "checkouts",    cmd -> doShowCheckouts());
        add("CHECKOUTS",        "checkouts",    cmd -> doShowCheckouts());
        add("MES PAIEMENTS",    "checkouts",    cmd -> doShowCheckouts());
        add("MES COMMANDES",    "checkouts",    cmd -> doShowCheckouts());
        add("PAYMENT HISTORY",  "checkouts",    cmd -> doShowCheckouts());
        add("CHECKOUT HISTORY", "checkouts",    cmd -> doShowCheckouts());

        // ---- Search / flights tab ----
        add("RECHERCHER", "search", cmd -> doShowSearch());
        add("SEARCH",     "search", cmd -> doShowSearch());
        add("VOLS",       "flights",cmd -> doShowSearch());
        add("FLIGHTS",    "flights",cmd -> doShowSearch());

        // ---- Cancel / back ----
        add("ANNULER",   "cancel", cmd -> doCancel());
        add("CANCEL",    "cancel", cmd -> doCancel());
        add("RETOUR",    "back",   cmd -> doCancel());
        add("GO BACK",   "back",   cmd -> doCancel());

        // ---- Payment ----
        add("PAYER",     "pay",    cmd -> doPayment());
        add("PAY",       "pay",    cmd -> doPayment());
        add("PAYMENT",   "pay",    cmd -> doPayment());
        add("CONFIRMER", "confirm",cmd -> doPayment());

        // ---- Describe ----
        add("DECRIRE",   "describe", cmd -> doDescribe());
        add("DESCRIBE",  "describe", cmd -> doDescribe());
        add("QU EST CE", "describe", cmd -> doDescribe());
        add("QUOI",      "describe", cmd -> doDescribe());
        add("WHAT",      "describe", cmd -> doDescribe());

        // ---- Help — last so it doesn't swallow other commands ----
        add("AIDE",        "help",         cmd -> doHelp());
        add("HELP",        "help",         cmd -> doHelp());
        add("COMMANDES",   "help",         cmd -> doHelp());
        // ---- Recalibrate noise gate ----
        add("RECALIBRER",  "recalibrate",  cmd -> doRecalibrate());
        add("CALIBRER",    "recalibrate",  cmd -> doRecalibrate());
        add("BRUIT",       "recalibrate",  cmd -> doRecalibrate());
    }

    /** Register one keyword + action. */
    private void add(String keyword, String label, Consumer<String> action) {
        commandTable.put(keyword, action);
    }

    // ── Action helpers ────────────────────────────────────────────────────────

    private void doOpenBooking() {
        // Consume LLM-extracted parameters (clear after reading).
        final String dest = pendingDestination;
        final String date = pendingDate;
        final int    pax  = pendingPassengers;
        pendingDestination = null;
        pendingDate        = null;
        pendingPassengers  = 1;

        // Build a sarcastic spoken announcement that includes all known details.
        final String speech;
        if (dest != null && !dest.isBlank() && date != null && !date.isBlank()) {
            String seatWord = (pax == 1) ? "seat" : pax + " seats";
            speech = "Ooh, " + dest + " on " + date + " for " + seatWord
                     + "? Fancy. Let me see what's flying.";
        } else if (dest != null && !dest.isBlank()) {
            speech = dest + "? Nice choice. Let me find your ride.";
        } else {
            speech = "A blank booking form, just for you. The world is your oyster.";
        }

        speakAndRun(speech, () -> {
            ControllerProxy p = proxy;
            if (p != null) {
                if (dest != null && !dest.isBlank()) {
                    p.openBookingWith(dest, date);  // parametric — controller pre-fills
                } else {
                    p.openBooking();                // standard
                }
            }
        });
    }

    private void doLogin() {
        speakAndRun("Let me check if you're you. One moment.", () -> {
            ControllerProxy p = proxy;
            if (p != null) p.performLogin();
        });
    }

    private void doFocusEmail() {
        speakAndRun("Email field — your keyboard awaits.", () -> {
            ControllerProxy p = proxy;
            if (p != null) p.focusEmailField();
        });
    }

    private void doFocusPassword() {
        speakAndRun("Password field. No peeking.", () -> {
            ControllerProxy p = proxy;
            if (p != null) p.focusPasswordField();
        });
    }

    private void doSignup() {
        speakAndRun("Another traveler joins GoVibe. Welcome aboard!", () -> {
            ControllerProxy p = proxy;
            if (p != null) p.navigateToSignup();
        });
    }

    private void doShowBookings() {
        speakAndRun("Let me pull up all your grand travel plans.", () -> {
            ControllerProxy p = proxy;
            if (p != null) p.showBookingsTab();
        });
    }

    private void doShowCheckouts() {
        speakAndRun("Let me pull up your checkout history. Hope you didn't break the bank.", () -> {
            ControllerProxy p = proxy;
            if (p != null) p.showBookingsTab();
        });
    }

    private void doShowSearch() {
        speakAndRun("Scanning the skies. This won't take long.", () -> {
            ControllerProxy p = proxy;
            if (p != null) p.showSearchTab();
        });
    }

    private void doCancel() {
        speakAndRun("Alright, pretend that never happened.", () -> {
            ControllerProxy p = proxy;
            if (p != null) p.cancelBooking();
        });
    }

    private void doPayment() {
        speakAndRun("Time to make it official. Processing payment.", () -> {
            ControllerProxy p = proxy;
            if (p != null) p.triggerPayment();
        });
    }

    private void doRecalibrate() {
        // Immediately acknowledge — recalibration takes ~9 s, let user know.
        vas.speak("Recalibrating microphone. Shh — pretend you don't exist for a few seconds.");
        NoiseOrchestrator.getInstance().forceRecalibrate();
    }

    private void doDescribe() {
        ControllerProxy p = proxy;
        String description = (p != null)
                ? p.describeScreen()
                : "No description available.";
        vas.speak(description);
    }

    private void doHelp() {
        String help =
                "Available commands. " +
                "On the login screen: Email for the email field. " +
                "Password for the password field. " +
                "Login to sign in. " +
                "Sign up to create an account. " +
                "In the app: Book to open the booking form. " +
                "My Bookings or Checkouts to view your bookings. " +
                "Search to find flights. " +
                "Pay to start payment. " +
                "Cancel to close the form. " +
                "Describe to describe the current screen. " +
                "Logout to end the session. " +
                "Recalibrate to adapt the microphone to your environment.";
        vas.speak(help);
    }

    private void navigate(String fxml, String title, String speech) {
        speakAndRun(speech, () -> MainApp.switchScene(fxml, title));
    }

    /**
     * Speaks {@code message} immediately (non-blocking TTS) then schedules
     * {@code uiAction} on the JavaFX thread.
     * When {@link #suppressActionSpeech} is set (ML path already spoke a response)
     * the TTS is skipped so Jenny does not double-speak.
     */
    private void speakAndRun(String message, Runnable uiAction) {
        if (!suppressActionSpeech) {
            vas.speak(message);
        }
        suppressActionSpeech = false;   // always reset after one use
        if (uiAction != null) {
            Platform.runLater(uiAction);
        }
    }

    /**
     * Called by {@link VoiceAssistantService} from the ML path before
     * {@link #onCommand} so that the next {@link #speakAndRun} call is silent
     * (the ML response has already been spoken aloud).
     */
    public void suppressNextActionSpeech() {
        suppressActionSpeech = true;
    }
}
