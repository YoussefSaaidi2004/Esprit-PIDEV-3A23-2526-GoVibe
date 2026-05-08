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

        // ── App section navigation (optional — default no-op) ─────────────────

        /** Navigate to the car rental section. */
        default void showCarsSection() {}
        /** Navigate to the activities section. */
        default void showActivitiesSection() {}
        /** Navigate to the hotels section. */
        default void showHotelsSection() {}
        /** Navigate to the sessions section. */
        default void showSessionsSection() {}
        /** Navigate to the messages / inbox section. */
        default void showMessagesSection() {}
        /** Navigate to the community forum section. */
        default void showForumSection() {}
        /** Navigate to the reclamation / customer-support section. */
        default void showReclamationSection() {}
        /** Navigate to the user profile / account-settings section. */
        default void showProfileSection() {}
        /** Navigate to the destinations map / locations section. */
        default void showLocationsSection() {}

        /** Open the destination details modal for the first visible flight (Mes Vols page). */
        default void showFlightDetails() {}

        /**
         * Show real-time weather data in the weather overlay panel.
         * Called by {@link CommandRouter#doShowWeather()} after Python fetches weather.
         * Default is a no-op; implement in whichever controller hosts the flight page.
         *
         * @param city      city name
         * @param temp      temperature string e.g. "24\u00b0C"
         * @param condition weather description e.g. "Sunny"
         * @param humidity  humidity string e.g. "65%"
         * @param wind      wind speed string e.g. "15 km/h"
         * @param feel      feels-like temperature e.g. "26\u00b0C"
         */
        default void showWeather(String city, String temp, String condition,
                                 String humidity, String wind, String feel) {}
        // ── Login-screen actions (optional — default no-op so other screens ignore them) ──

        /** Trigger the login button / authenticate. */
        default void performLogin() {}
        /** Open the camera / Face ID login flow on the login screen. */
        default void openCamera() {}
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

    // Real-time weather data received from Python via weather_show message.
    // Set by setWeatherContext() before SHOW_WEATHER command arrives.
    private volatile String pendingWeatherCity      = null;
    private volatile String pendingWeatherTemp      = null;
    private volatile String pendingWeatherCondition = null;
    private volatile String pendingWeatherHumidity  = null;
    private volatile String pendingWeatherWind      = null;
    private volatile String pendingWeatherFeel      = null;

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

    /**
     * Stores real-time weather data received from Python's weather_show message.
     * Called by {@link VoiceAssistantService}'s weather listener before the
     * SHOW_WEATHER command arrives so {@link #doShowWeather()} has data to display.
     */
    public void setWeatherContext(String city, String temp, String condition,
                                  String humidity, String wind, String feel) {
        this.pendingWeatherCity      = city;
        this.pendingWeatherTemp      = temp;
        this.pendingWeatherCondition = condition;
        this.pendingWeatherHumidity  = humidity;
        this.pendingWeatherWind      = wind;
        this.pendingWeatherFeel      = feel;
        LOG.info("[CommandRouter] Weather context — city=" + city + "  temp=" + temp
                 + "  condition=" + condition);
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
        // Notify Python BEFORE the scene switch so it exits PROCESSING_LOGOUT
        // immediately (plays farewell) instead of waiting for LoginController.
        // LoginController.initialize() also calls notifyUserLoggedOut() as a
        // redundant safety net, which then auto-wakes Python to HELPING so
        // login-screen voice commands (e.g. "log in") work without a wake word.
        add("DECONNEXION", "logout", cmd -> {
            if (vas != null) vas.notifyUserLoggedOut();
            navigate("/org/example/LoginView.fxml", "GoVibe — Login",
                    "Poof — off you go. Come back soon!");
        });

        add("LOGOUT",       "logout", cmd -> {
            if (vas != null) vas.notifyUserLoggedOut();
            navigate("/org/example/LoginView.fxml", "GoVibe — Login",
                    "Poof — off you go. Come back soon!");
        });

        add("LOG OUT",      "logout", cmd -> {
            if (vas != null) vas.notifyUserLoggedOut();
            navigate("/org/example/LoginView.fxml", "GoVibe — Login",
                    "Poof — off you go. Come back soon!");
        });

        // ---- Login screen actions ----
        add("OPEN_CAMERA",  "camera",          cmd -> doOpenCamera());
        add("OPEN CAMERA",  "camera",          cmd -> doOpenCamera());
        add("CAMERA",       "camera",          cmd -> doOpenCamera());
        add("FACE ID",      "face id",         cmd -> doOpenCamera());
        add("FACE LOGIN",   "face id",         cmd -> doOpenCamera());
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
        add("ACCUEIL",    "dashboard", cmd -> navigate("/org/example/UserHomeView.fxml", "GoVibe — Home", "Back to base!"));
        add("DASHBOARD",  "dashboard", cmd -> navigate("/org/example/UserHomeView.fxml", "GoVibe — Home", "Back to base!"));
        add("HOME",       "home",      cmd -> navigate("/org/example/UserHomeView.fxml", "GoVibe — Home", "Home sweet home!"));

        // ---- My bookings tab — MUST come before RESERVATION/BOOK keys ----
        // (CommandRouter uses substring matching: "MES RESERVATIONS" contains
        //  "RESERVATION", so the more specific keys must be registered first.)
        add("MES RESERVATIONS", "my bookings",  cmd -> doShowBookings());
        add("MY BOOKINGS",      "my bookings",  cmd -> doShowBookings());
        add("RESERVATIONS",     "bookings",     cmd -> doShowBookings());

        // ---- Booking ----
        add("NOUVELLE RESERVATION", "new booking", cmd -> doOpenBooking());
        add("RESERVER",   "book",    cmd -> doOpenBooking());
        add("RESERVATION","book",    cmd -> doOpenBooking());
        add("BOOK",       "book",    cmd -> doOpenBooking());

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

        // ---- Car rental ----
        add("SHOW_CARS",       "cars",       cmd -> doShowCars());
        add("VOITURES",        "cars",       cmd -> doShowCars());
        add("LOUER",           "rent car",   cmd -> doShowCars());
        add("CAR RENTAL",      "cars",       cmd -> doShowCars());
        add("RENT A CAR",      "cars",       cmd -> doShowCars());
        add("CARS",            "cars",       cmd -> doShowCars());

        // ---- Activities ----
        add("SHOW_ACTIVITIES", "activities", cmd -> doShowActivities());
        add("ACTIVITES",       "activities", cmd -> doShowActivities());
        add("ACTIVITIES",      "activities", cmd -> doShowActivities());
        add("QUOI FAIRE",      "activities", cmd -> doShowActivities());
        add("WHAT TO DO",      "activities", cmd -> doShowActivities());
        add("LOISIRS",         "activities", cmd -> doShowActivities());

        // ---- Hotels ----
        add("SHOW_HOTELS",        "hotels",     cmd -> doShowHotels());
        add("HOTELS",             "hotels",     cmd -> doShowHotels());
        add("HOTEL",              "hotels",     cmd -> doShowHotels());
        add("HEBERGEMENT",        "hotels",     cmd -> doShowHotels());
        add("CHAMBRES",           "hotels",     cmd -> doShowHotels());

        // ---- Sessions ----
        add("SHOW_SESSIONS",      "sessions",  cmd -> doShowSessions());
        add("SESSIONS",           "sessions",  cmd -> doShowSessions());
        add("SESSION LIST",       "sessions",  cmd -> doShowSessions());

        // ---- Profile / Account ----
        add("PROFILE",            "profile",   cmd -> doShowProfile());
        add("MON PROFIL",         "profile",   cmd -> doShowProfile());
        add("MY PROFILE",         "profile",   cmd -> doShowProfile());
        add("PARAMETRES",         "profile",   cmd -> doShowProfile());
        add("SETTINGS",           "profile",   cmd -> doShowProfile());
        add("MY ACCOUNT",         "profile",   cmd -> doShowProfile());
        add("MON COMPTE",         "profile",   cmd -> doShowProfile());

        // ---- Messages / Inbox ----
        add("MESSAGES",           "messages",  cmd -> doShowMessages());
        add("MES MESSAGES",       "messages",  cmd -> doShowMessages());
        add("INBOX",              "messages",  cmd -> doShowMessages());
        add("MESSAGE",            "messages",  cmd -> doShowMessages());
        add("MESSAGERIE",         "messages",  cmd -> doShowMessages());
        add("CHAT",               "messages",  cmd -> doShowMessages());

        // ---- Forum / Community ----
        add("FORUM",              "forum",     cmd -> doShowForum());
        add("COMMUNAUTE",         "forum",     cmd -> doShowForum());
        add("COMMUNITY",          "forum",     cmd -> doShowForum());
        add("DISCUSSIONS",        "forum",     cmd -> doShowForum());
        add("REVIEWS",            "forum",     cmd -> doShowForum());

        // ---- Reclamation / Customer Support ----
        add("RECLAMATION",        "support",   cmd -> doShowReclamation());
        add("COMPLAINT",          "support",   cmd -> doShowReclamation());
        add("SUPPORT",            "support",   cmd -> doShowReclamation());
        add("SIGNALER",           "support",   cmd -> doShowReclamation());
        add("REPORT",             "support",   cmd -> doShowReclamation());
        add("PLAINTE",            "support",   cmd -> doShowReclamation());

        // ---- Locations / Map ----
        add("SHOW_LOCATIONS",     "map",       cmd -> doShowLocations());
        add("MAP",                "map",       cmd -> doShowLocations());
        add("LOCATIONS",          "locations", cmd -> doShowLocations());
        add("CARTE",              "locations", cmd -> doShowLocations());
        add("EXPLORER",           "locations", cmd -> doShowLocations());
        add("EXPLORE",            "locations", cmd -> doShowLocations());

        // ---- DB-powered describe intents (Python already spoke; just navigate) ----
        add("DESCRIBE_ACTIVITY",  "describe activity", cmd -> doShowActivities());
        add("DESCRIBE_CAR",       "describe car",      cmd -> doShowCars());

        // ---- Flight details — opens city/destination info modal ----
        add("SHOW_FLIGHT_DETAILS", "details",  cmd -> doShowFlightDetails());
        add("DETAILS",             "details",  cmd -> doShowFlightDetails());
        add("CITY DETAILS",        "details",  cmd -> doShowFlightDetails());
        add("DESTINATION INFO",    "details",  cmd -> doShowFlightDetails());

        // ---- Weather ----
        add("SHOW_WEATHER",   "weather", cmd -> doShowWeather());
        add("WEATHER",        "weather", cmd -> doShowWeather());
        add("METEO",          "weather", cmd -> doShowWeather());
        add("QUEL TEMPS",     "weather", cmd -> doShowWeather());
        add("CHECK WEATHER",  "weather", cmd -> doShowWeather());
        add("FORECAST",       "weather", cmd -> doShowWeather());
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

    private void doOpenCamera() {
        speakAndRun("Opening camera — look at the lens!", () -> {
            ControllerProxy p = proxy;
            if (p != null) p.openCamera();
        });
    }

    private void doLogin() {
        // Python already spoke the login ack ("On it — logging you in!").
        // Always suppress the Java TTS here to avoid double-speech.
        suppressActionSpeech = false;  // clear any stale flag
        ControllerProxy p = proxy;
        if (p != null) Platform.runLater(() -> p.performLogin());
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
        vas.vivianSpeak("Recalibrating microphone! Shh — pretend you don't exist for a few seconds.");
        NoiseOrchestrator.getInstance().forceRecalibrate();
    }

    private void doShowCars() {
        final String dest = pendingDestination;
        pendingDestination = null;
        final String speech = (dest != null && !dest.isBlank())
            ? "Looking for cars in " + dest + ". Let me pull up the fleet!"
            : "Let me pull up our car rental fleet. Pick your wheels!";
        speakAndRun(speech, () -> {
            ControllerProxy p = proxy;
            if (p != null) p.showCarsSection();
        });
    }

    private void doShowActivities() {
        final String dest = pendingDestination;
        pendingDestination = null;
        final String speech = (dest != null && !dest.isBlank())
            ? "Here's what you can do in " + dest + "!"
            : "Let me find you something fun to do!";
        speakAndRun(speech, () -> {
            ControllerProxy p = proxy;
            if (p != null) p.showActivitiesSection();
        });
    }

    private void doShowHotels() {
        final String dest = pendingDestination;
        pendingDestination = null;
        final String speech = (dest != null && !dest.isBlank())
            ? "Finding hotels in " + dest + ". Let me see what's available!"
            : "Let me find you a place to stay. Browsing hotels now!";
        speakAndRun(speech, () -> {
            ControllerProxy p = proxy;
            if (p != null) p.showHotelsSection();
        });
    }

    private void doDescribe() {
        // Python already spoke the describe response (e.g. "Let me narrate…").
        // Honour suppressActionSpeech so we don't double-speak, and skip the
        // Java-side narration entirely when the ML path already handled it.
        if (suppressActionSpeech) {
            suppressActionSpeech = false;
            return;
        }
        ControllerProxy p = proxy;
        String description = (p != null) ? p.describeScreen() : null;
        if (description != null && !description.isBlank()) {
            vas.vivianSpeak(description);
        }
        // If describeScreen() returned nothing, Python's response is already
        // playing — no need to say "No description available."
    }

    private void doShowSessions() {
        speakAndRun("Here are the available sessions.", () -> {
            ControllerProxy p = proxy;
            if (p != null) p.showSessionsSection();
        });
    }

    private void doShowProfile() {
        speakAndRun("Opening your profile. Looking sharp as always.", () -> {
            ControllerProxy p = proxy;
            if (p != null) p.showProfileSection();
        });
    }

    private void doShowMessages() {
        speakAndRun("Let me check your messages. Someone might be trying to reach you.", () -> {
            ControllerProxy p = proxy;
            if (p != null) p.showMessagesSection();
        });
    }

    private void doShowForum() {
        speakAndRun("Opening the GoVibe community forum. Let's see what people are saying!", () -> {
            ControllerProxy p = proxy;
            if (p != null) p.showForumSection();
        });
    }

    private void doShowReclamation() {
        speakAndRun("Oh no, that doesn't sound fun! Let me open the support form so we can sort this out.", () -> {
            ControllerProxy p = proxy;
            if (p != null) p.showReclamationSection();
        });
    }

    private void doShowLocations() {
        speakAndRun("Here's the world — let me show you where you can go!", () -> {
            ControllerProxy p = proxy;
            if (p != null) p.showLocationsSection();
        });
    }

    private void doShowFlightDetails() {
        speakAndRun("Opening destination details — here's everything about where you're headed!", () -> {
            ControllerProxy p = proxy;
            if (p != null) p.showFlightDetails();
        });
    }

    private void doShowWeather() {
        // Python already spoke the weather description via Echo — suppress duplicate TTS.
        final String city      = pendingWeatherCity      != null ? pendingWeatherCity      : "";
        final String temp      = pendingWeatherTemp      != null ? pendingWeatherTemp      : "--";
        final String condition = pendingWeatherCondition != null ? pendingWeatherCondition : "--";
        final String humidity  = pendingWeatherHumidity  != null ? pendingWeatherHumidity  : "--";
        final String wind      = pendingWeatherWind      != null ? pendingWeatherWind      : "--";
        final String feel      = pendingWeatherFeel      != null ? pendingWeatherFeel      : "--";
        // Clear consumed context
        pendingWeatherCity = pendingWeatherTemp = pendingWeatherCondition = null;
        pendingWeatherHumidity = pendingWeatherWind = pendingWeatherFeel = null;
        // Python already spoke the weather — don't double-speak
        suppressActionSpeech = true;
        Platform.runLater(() -> {
            ControllerProxy p = proxy;
            if (p != null) p.showWeather(city, temp, condition, humidity, wind, feel);
        });
    }

    private void doHelp() {
        String help =
                "Here are all the voice commands I understand. " +
                "On the login screen: Email for the email field. " +
                "Password for the password field. " +
                "Login to sign in. Sign up to create an account. " +
                "In the app: Book to open the booking form. " +
                "My Bookings to view your reservations. " +
                "Search to find flights. " +
                "Cars to browse car rentals. " +
                "Activities to discover things to do. Just ask me to describe activities and I'll tell you! " +
                "Hotels to find accommodation. " +
                "Sessions to view available sessions. " +
                "Messages to open your inbox. " +
                "Forum to visit the community. " +
                "Profile to manage your account. " +
                "Complaint to report an issue. " +
                "Map to explore destinations. " +
                "Pay to start payment. " +
                "Cancel to close the form. " +
                "Describe to narrate the current screen. " +
                "Logout to end the session. " +
                "Recalibrate to adapt the microphone. " +
                "And if wake-word mode is on, just say Hi Go to wake me up!";
        vas.vivianSpeak(help);
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
            vas.vivianSpeak(message);
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
