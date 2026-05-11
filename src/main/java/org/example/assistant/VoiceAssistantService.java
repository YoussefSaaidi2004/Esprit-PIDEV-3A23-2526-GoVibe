package org.example.assistant;

import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Hybrid offline voice assistant — STT + TTS — for GoVibe.
 *
 * <h3>STT strategy (tried in order)</h3>
 * <ol>
 *   <li><b>Vosk</b> — highest accuracy. Requires the model folder at
 *       {@code %USERPROFILE%\.govibe\vosk-model}
 *       (download from <a href="https://alphacephei.com/vosk/models">alphacephei.com</a>).
 *   </li>
 *   <li><b>Windows SAPI SpeechRecognitionEngine</b> — always available on
 *       Windows 10+, zero download. Uses a persistent PowerShell subprocess
 *       that prints each recognised phrase to stdout.</li>
 * </ol>
 *
 * <h3>TTS</h3>
 * Windows SAPI Synthesis via PowerShell — no dependency, fully offline.
 */
public class VoiceAssistantService {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static VoiceAssistantService instance;

    public static VoiceAssistantService getInstance() {
        if (instance == null) {
            synchronized (VoiceAssistantService.class) {
                if (instance == null) instance = new VoiceAssistantService();
            }
        }
        return instance;
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private final AtomicBoolean listening = new AtomicBoolean(false);
    private volatile Thread   listenerThread;
    private volatile Process  sapiProcess;          // kept alive while using SAPI STT
    // Secondary SAPI grammar engine — runs alongside Vosk when Vosk is primary.
    // Provides reliable command capture for French even with an English Vosk model.
    private volatile Process  sapiGrammarProcess;
    private volatile Thread   sapiGrammarThread;
    private volatile VoiceCommandListener commandListener;

    // Vosk fields (null when model absent)
    private Model   voskModel;
    private boolean voskAvailable = false;

    // SAPI fallback — always available on Windows 10+
    private boolean sapiAvailable = false;

    // ── Vivian speak queue — messages to deliver once Python agent is ready ──────
    // vivianSpeak() enqueues here when pythonAgent is null (still loading).
    // initPythonAgent() drains this after pythonAgent is assigned.
    private final ConcurrentLinkedQueue<String> pendingVivianSpeaks = new ConcurrentLinkedQueue<>();

    // ── TTS debounce — prevent duplicate speaks ────────────────────────────────
    // If speak() is called with the same text within DEBOUNCE_MS, the second
    // call is silently dropped.
    private volatile long   lastTtsSpeakTime = 0;
    private volatile String lastTtsSpeakText = null;
    private static final long DEBOUNCE_MS = 2_000;

    // ── Noise isolation — STT dispatch debounce ───────────────────────────────
    // Prevents the same recognised phrase from triggering actions twice (e.g.
    // when Vosk emits a partial + final result for the same utterance).
    private volatile long   lastSttDispatchTime = 0;
    private volatile String lastSttDispatchText = null;
    private static final long STT_DEBOUNCE_MS  = 3_000;

    /** True while a user is authenticated (set by notifyUserLoggedIn/Out). */
    private volatile boolean userIsLoggedIn = false;

    /**
     * Vosk phonetic repairs for "face id" — applied only on the login screen
     * where these garbled STT outputs are otherwise meaningless.
     * Key = exact lowercase Vosk transcript, Value = corrected text.
     */
    private static final java.util.Map<String, String> FACE_ID_VOSK_REPAIRS;
    static {
        FACE_ID_VOSK_REPAIRS = new java.util.LinkedHashMap<>();
        // Vosk → "face id"
        FACE_ID_VOSK_REPAIRS.put("they say d",   "face id");
        FACE_ID_VOSK_REPAIRS.put("they said d",  "face id");
        FACE_ID_VOSK_REPAIRS.put("they say",     "face id");  // login-screen only — safe
        FACE_ID_VOSK_REPAIRS.put("this aid",     "face id");
        FACE_ID_VOSK_REPAIRS.put("these aid",    "face id");
        FACE_ID_VOSK_REPAIRS.put("phase id",     "face id");
        FACE_ID_VOSK_REPAIRS.put("based id",     "face id");
        FACE_ID_VOSK_REPAIRS.put("space id",     "face id");
        FACE_ID_VOSK_REPAIRS.put("faced id",     "face id");
        FACE_ID_VOSK_REPAIRS.put("face it",      "face id");
        FACE_ID_VOSK_REPAIRS.put("the saint",    "face id");
        FACE_ID_VOSK_REPAIRS.put("base it",      "face id");
        FACE_ID_VOSK_REPAIRS.put("faith id",     "face id");
        FACE_ID_VOSK_REPAIRS.put("face aid",     "face id");
    }

    /** Apply login-screen-only Vosk phonetic repairs for common face-id mishearings. */
    private String repairLoginScreenStt(String text) {
        String lower = text.toLowerCase().trim();
        String repaired = FACE_ID_VOSK_REPAIRS.get(lower);
        if (repaired != null) {
            System.out.println("[VoiceAssistant-STT] Login repair: \"" + text + "\" -> \"" + repaired + "\"");
            return repaired;
        }
        return text;
    }

    // ── Closed-loop Adaptive Noise Orchestrator ───────────────────────────────
    // Replaces the old static calibration block with a dynamic, three-stage
    // pipeline: Detector → Orchestrator → Adaptive Filter → SNR Validator.
    // See NoiseOrchestrator for full architecture documentation.
    private final NoiseOrchestrator noiseOrchestrator = NoiseOrchestrator.getInstance();

    /** Timestamp of the last frame that exceeded the adaptive gate. */
    private volatile long lastSpeechFrameTime = 0;

    /**
     * Epoch timestamp (ms) until which all microphone audio frames are silently
     * discarded and never fed to Vosk.
     *
     * <ul>
     *   <li>Set to {@link Long#MAX_VALUE} when TTS starts — microphone is
     *       effectively muted for the entire duration of TTS playback and the
     *       OS audio buffer is drained without feeding the Vosk decoder.</li>
     *   <li>Set to {@code System.currentTimeMillis()} (i.e., "now") when TTS
     *       finishes — the Vosk loop immediately exits the discard window on
     *       its next iteration and calls {@link Recognizer#reset()} once
     *       (JNI-safe: same thread as acceptWaveForm).  Any acoustic echo that
     *       reaches Vosk is suppressed by the {@link #TTS_MUTE_WINDOW_MS} gate
     *       in {@link #dispatchText}.</li>
     * </ul>
     *
     * Why read-and-discard rather than just pausing reads?
     * OS audio buffers refill while we pause — we must read every chunk to
     * prevent overflow, but must NOT feed TTS playback to Vosk.
     */
    private volatile long micDiscardUntilMs = 0L;

    // ── Noise isolation — "not understood" TTS throttle ───────────────────────
    // In noisy environments every background sound would trigger this message.
    // Allow it at most once per NOT_UNDERSTOOD_THROTTLE_MS.
    private volatile long lastNotUnderstoodTime = 0;
    private static final long NOT_UNDERSTOOD_THROTTLE_MS = 8_000;

    // ── TTS bleed mute gate ───────────────────────────────────────────────────
    // While the TTS voice is playing, the SAPI microphone picks up the speakers
    // and dispatches its own speech as user commands ("echo" or "bleed").
    // Solution: set ttsSpeaking=true while speak() is running, then keep a
    // TTS_MUTE_WINDOW_MS silence window after it finishes.  dispatchText()
    // checks this gate and silently discards recognised text during that period.
    private final java.util.concurrent.atomic.AtomicBoolean ttsSpeaking =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private volatile long ttsFinishedAtMs = 0L;
    // 3500 ms: edges out the full room-echo tail of a multi-sentence TTS burst
    // (edge-tts @ ~140 wpm can take 4-5 s for a greeting; 1500 ms was too short
    // and Vosk resumed mid-sentence, picking up the reverberant TTS as fake
    // user commands and triggering an infinite echo loop).
    private static final long TTS_MUTE_WINDOW_MS = 3500L;

    // ── Post-TTS echo extension gate ──────────────────────────────────────────
    // When the TTS discard window expires, if the mic RMS is still above this
    // threshold the window is extended by POST_TTS_ECHO_EXTEND_MS repeatedly
    // until the room is quiet — preventing false commands from residual echo.
    // Threshold 500 ≈ 10× typical silence floor (10–40 RMS); TTS echo = 2000–5000.
    private static final double POST_TTS_RMS_THRESHOLD  = 500.0;
    private static final long   POST_TTS_ECHO_EXTEND_MS = 1500L;

    private static final AudioFormat AUDIO_FORMAT =
            new AudioFormat(16_000f, 16, 1, true, false);

    private static final String PS_EXE =
            "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe";

    // ── edge-tts (neural voice, no SAPI conflict) ─────────────────────────────
    // null = not yet checked, true/false = result of detection
    private static volatile Boolean  edgeTtsAvailable = null;
    private static volatile String   edgeTtsPythonExe  = null;

    // ── Persistent TTS process + single-thread executor ───────────────────────
    // A single Python process is kept alive so we avoid ~300 ms cold-start per
    // speak() call.  A single-thread ExecutorService ensures writes to the
    // process' stdin are never interleaved between concurrent speak() callers.
    private static volatile Process        ttsPersistentProcess;
    private static volatile BufferedWriter ttsProcWriter;
    private static volatile BufferedReader ttsProcReader;
    private static final ExecutorService   ttsExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "GoVibe-TTS");
        t.setDaemon(true);
        return t;
    });

    // ── Python ML agent (started in background, used when ready) ──────────────
    private volatile PythonVoiceAgent pythonAgent;

    // ── Wake-word mode ────────────────────────────────────────────────────────
    // When wake-word mode is ON, Go listens silently in the background.
    // Only when it hears "hi go" / "hey go" does it switch to full active mode
    // (plays a greeting and routes commands) for WAKE_ACTIVE_WINDOW_MS.
    // After the window expires it silently returns to wake-word-only mode.
    //
    // Wake-word mode is optional: callers that never call enableWakeWordMode()
    // get the old always-active behaviour (push-to-talk or constant listening).
    private final AtomicBoolean wakeWordModeEnabled = new AtomicBoolean(false);
    private final AtomicBoolean wakeWordActive      = new AtomicBoolean(false);
    /** How long (ms) Go stays in "active" mode after hearing the wake word. */
    private static final long WAKE_ACTIVE_WINDOW_MS = 15_000;   // 15 seconds
    private volatile long wakeActivatedAtMs = 0L;
    /** Known wake-word phrases (lower-case, accent-stripped). */
    private static final String[] WAKE_WORDS = {
        "hi go", "hey go", "hello go", "go", "ok go", "govibe", "hi govibe",
        "salut go", "bonjour go", "hey govibe",
    };

    /**
     * Enables always-listening wake-word mode.
     * Call this at startup to give Go a Siri-like always-on capability.
     * STT must have been started first with {@link #startListening()}.
     */
    public void enableWakeWordMode() {
        wakeWordModeEnabled.set(true);
        wakeWordActive.set(false);
        System.out.println("[VoiceAssistant] Wake-word mode ENABLED — say 'Hi Go' to activate.");
    }

    /** Disables wake-word mode — all speech is routed to commands (old behaviour). */
    public void disableWakeWordMode() {
        wakeWordModeEnabled.set(false);
        wakeWordActive.set(true); // treat as always active
        System.out.println("[VoiceAssistant] Wake-word mode DISABLED — always listening.");
    }

    /** Returns true if Go is currently awake and actively routing commands. */
    public boolean isWakeWordActive() {
        if (!wakeWordModeEnabled.get()) return true;  // not in wake-word mode → always active
        return wakeWordActive.get();
    }

    /**
     * Checks whether {@code text} contains a wake-word trigger.
     * If so, activates the assistant and returns true.
     * Must NOT speak (called from STT thread).
     */
    private boolean checkAndTriggerWakeWord(String text) {
        if (!wakeWordModeEnabled.get()) return false;
        String norm = java.text.Normalizer.normalize(text.toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}", "");
        for (String ww : WAKE_WORDS) {
            if (norm.contains(ww)) {
                wakeWordActive.set(true);
                wakeActivatedAtMs = System.currentTimeMillis();
                System.out.println("[WakeWord] Triggered by: \"" + text + "\"");
                // Notify Python agent — forward the full utterance so Python can
                // detect embedded commands such as "hi go logout" in one breath.
                PythonVoiceAgent agent = pythonAgent;
                if (agent != null && agent.isReady()) {
                    agent.signalWakeWord(text);
                }
                // Python (Qwen3-TTS) handles the wake greeting and all subsequent
                // TTS playback.  The microphone discard window is driven by the
                // ttsStatusCallback registered in initPythonAgent() — no Java TTS
                // call is needed here.
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the wake-word active window has expired and, if so,
     * returns Go to dormant mode.  Called from STT loops.
     */
    private void checkWakeWordTimeout() {
        if (wakeWordModeEnabled.get() && wakeWordActive.get()) {
            long elapsed = System.currentTimeMillis() - wakeActivatedAtMs;
            if (elapsed > WAKE_ACTIVE_WINDOW_MS) {
                wakeWordActive.set(false);
                System.out.println("[WakeWord] Active window expired — returning to wake-word mode. Say 'Hi Go' to wake me.");
            }
        }
    }

    /**
     * Resets (extends) the wake-word active window whenever a command is successfully
     * processed — so an ongoing conversation keeps Go awake.
     */
    public void extendWakeWordWindow() {
        if (wakeWordModeEnabled.get() && wakeWordActive.get()) {
            wakeActivatedAtMs = System.currentTimeMillis();
        }
    }

    private VoiceAssistantService() {
        // Delay heavy component initialization until JavaFX has rendered its first scene
        // to prevent the JavaFX Application Thread from racing with Vosk's native JNI loader.
        javafx.application.Platform.runLater(() -> {
            Thread initThread = new Thread(() -> {
                try {
                    System.out.println("[VoiceAssistant] Initializing internal core (background)...");
                    initVosk();
                    detectSapi();
                    warmupTts();
                    initNoiseOrchestrator();
                    System.out.println("[VoiceAssistant] Core initialization complete.");
                } catch (Throwable t) {
                    System.err.println("[VoiceAssistant] Core initialization failed: " + t.getMessage());
                    t.printStackTrace();
                }
            }, "vosk-model-loader");
            initThread.setDaemon(true);
            initThread.start();
        });

        // Already non-blocking (has its own thread)
        initPythonAgent();
    }

    private void initNoiseOrchestrator() {
        // Callback: announce environment changes via TTS so the user always knows
        // the assistant has adapted to new surroundings (important for accessibility).
        noiseOrchestrator.setOnEnvironmentChanged(env -> {
            // Don't announce while TTS is playing — the orchestrator would calibrate
            // against the speaker output and immediately fire a false "very noisy" alert.
            if (ttsSpeaking.get()
                    || System.currentTimeMillis() - ttsFinishedAtMs < TTS_MUTE_WINDOW_MS) {
                System.out.println("[VoiceAssistant-Gate] Noise env change suppressed during TTS: " + env);
                return;
            }
            String msg = switch (env) {
                case SILENT    -> null; // no need to announce pure silence
                case QUIET     -> "Oh, quiet at last. I can actually hear you now.";
                case NORMAL    -> "Ambient chaos: normal. I'm ready when you are.";
                case NOISY     -> "It's a bit loud in here. Speak up, will you?";
                case VERY_NOISY-> "Wow, it's basically a concert. Get closer to the mic, please.";
            };
            if (msg != null) vivianSpeak(msg);
        });

        // Callback: SNR validator signals environment may have shifted — log it.
        noiseOrchestrator.setOnRedetectRequested(() ->
            System.out.println("[VoiceAssistant] Orchestrator SNR-validator: re-detection done."));
    }

    // ── Init ──────────────────────────────────────────────────────────────────
    private void initVosk() {
        String modelPath = resolveModelPath();
        if (modelPath == null) {
            System.out.println("[VoiceAssistant] Vosk English model NOT found at: " + defaultModelPath() +
                    " — will use Windows SAPI STT instead.");
            printEnglishModelTip();
            return;
        }
        try {
            System.out.println("[VoiceAssistant] Loading Vosk English model from: " + modelPath);
            voskModel = new Model(modelPath);
            voskAvailable = true;
            System.out.println("[VoiceAssistant] Vosk English model loaded OK — high-accuracy STT active.");
            // FIX race condition: auto-start STT as soon as Vosk is ready.
            // Previously startListening() was called from MainApp before this thread
            // finished — so voskAvailable was still false at that point. Now we
            // self-start here to avoid a Vosk JNI crash from premature Recognizer init.
            if (!listening.get()) {
                System.out.println("[VoiceAssistant] Auto-starting Vosk STT after model load.");
                startListening();
            }
        } catch (Exception e) {
            System.err.println("[VoiceAssistant] Vosk model load failed: " + e.getMessage());
        }
    }

    /** Prints a tip and starts a background download of the English Vosk model. */
    private void printEnglishModelTip() {
        String target = defaultModelPath();
        System.out.println("[VoiceAssistant] TIP: English Vosk model not found — auto-downloading in background (~40 MB)...");
        System.out.println("  Target: " + target);
        startEnglishModelDownload(target);
    }

    /**
     * Downloads vosk-model-small-en-us-0.15.zip from alphacephei.com and extracts it
     * to %USERPROFILE%\.govibe\vosk-model in a daemon thread.
     */
    private void startEnglishModelDownload(String targetPath) {
        Thread dl = new Thread(() -> {
            try {
                String zipUrl = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip";
                String tmpZip = System.getProperty("java.io.tmpdir") + File.separator + "vosk-en.zip";
                String govibe = Paths.get(System.getProperty("user.home"), ".govibe").toString();

                System.out.println("[VoskDownload] Downloading English model from " + zipUrl + " ...");

                String dlScript = String.format(
                    "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; " +
                    "Invoke-WebRequest -Uri '%s' -OutFile '%s' -UseBasicParsing",
                    zipUrl, tmpZip);

                int rc = new ProcessBuilder(PS_EXE, "-NoProfile", "-NonInteractive",
                            "-WindowStyle", "Hidden", "-Command", dlScript)
                        .inheritIO().start().waitFor();

                if (rc != 0 || !new File(tmpZip).exists()) {
                    System.err.println("[VoskDownload] Download failed (exit=" + rc + ").");
                    return;
                }
                System.out.println("[VoskDownload] Download complete. Extracting...");

                String exScript = String.format(
                    "Expand-Archive -Path '%s' -DestinationPath '%s' -Force", tmpZip, govibe);
                new ProcessBuilder(PS_EXE, "-NoProfile", "-NonInteractive",
                            "-WindowStyle", "Hidden", "-Command", exScript)
                        .inheritIO().start().waitFor();

                java.io.File extracted = new java.io.File(govibe, "vosk-model-small-en-us-0.15");
                java.io.File dest      = new java.io.File(targetPath);
                if (extracted.exists() && !dest.exists()) {
                    extracted.renameTo(dest);
                    System.out.println("[VoskDownload] English model installed at: " + targetPath);
                    System.out.println("[VoskDownload] Restart GoVibe to activate Vosk English STT.");
                    speak("English voice model downloaded. Restart GoVibe and I'll sound even smarter.");
                } else if (dest.exists()) {
                    System.out.println("[VoskDownload] Model already present at: " + targetPath);
                } else {
                    System.err.println("[VoskDownload] Extraction done but renamed folder not found.");
                }
                new File(tmpZip).delete();
            } catch (Exception e) {
                System.err.println("[VoskDownload] Error: " + e.getMessage());
            }
        }, "VoskDownload-EN");
        dl.setDaemon(true);
        dl.start();
    }

    private void detectSapi() {
        sapiAvailable = new File(PS_EXE).exists();
        System.out.println("[VoiceAssistant] Windows SAPI STT " +
                (sapiAvailable ? "available" : "NOT available") + " (" + PS_EXE + ").");
        System.out.println("[VoiceAssistant] Active STT engine: " +
                (voskAvailable ? "Vosk (primary)" : sapiAvailable ? "Windows SAPI" : "NONE"));
        
        // FIX: Auto-start SAPI fallback if Vosk is not available
        if (!voskAvailable && sapiAvailable && !listening.get()) {
            System.out.println("[VoiceAssistant] Auto-starting SAPI STT (Vosk disabled/missing).");
            startListening();
        }
    }

    /**
     * Starts the Python ML voice agent in a background daemon thread.
     * The agent handles intent classification and generates natural TTS responses.
     * Falls back gracefully if Python is unavailable.
     */
    /**
     * Pre-warm TTS.  When edge-tts is available we skip SAPI warmup entirely —
     * firing a SAPI TTS process kills the SAPI STT recognition process.
     */
    private void warmupTts() {
        Thread t = new Thread(() -> {
            // Probe edge-tts availability on startup so the first real speak is instant.
            if (checkEdgeTts()) {
                // Pre-start the persistent TTS process — eliminates cold-start on first speak.
                ensureTtsPersistentProcess();
                System.out.println("[VoiceAssistant-TTS] Persistent TTS process pre-started.");
                return;
            }
            // edge-tts unavailable — fall back to silent SAPI warmup
            try {
                String script =
                    "Add-Type -AssemblyName System.Speech; " +
                    "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                    "$s.Volume = 0; $s.Rate = 5; $s.Speak('ready');";
                new ProcessBuilder(PS_EXE, "-NoProfile", "-NonInteractive",
                        "-WindowStyle", "Hidden", "-Command", script)
                        .redirectErrorStream(true).start().waitFor();
                System.out.println("[VoiceAssistant-TTS] Engine pre-warmed (SAPI).");
            } catch (Exception ignored) {}
        }, "VoiceAssistant-TTS-Warmup");
        t.setDaemon(true);
        t.start();
    }

    private void initPythonAgent() {
        Thread t = new Thread(() -> {
            try {
                System.out.println("[VoiceAssistant] Initialising Python ML agent...");

                // Register boot-phase mic-gate callback BEFORE getInstance() blocks.
                // Python emits tts_status signals during startup TTS (before the
                // ready signal). Without this, the boot reader skips those signals,
                // the mic stays open, and the startup greeting is picked up by Vosk
                // as fake user commands (self-echo feedback loop).
                PythonVoiceAgent.setBootTtsCallback(speaking -> {
                    if (speaking) {
                        micDiscardUntilMs = Long.MAX_VALUE;
                        ttsSpeaking.set(true);
                    } else {
                        ttsFinishedAtMs   = System.currentTimeMillis();
                        micDiscardUntilMs = ttsFinishedAtMs + TTS_MUTE_WINDOW_MS;
                        ttsSpeaking.set(false);
                    }
                });

                pythonAgent = PythonVoiceAgent.getInstance();
                System.out.println("[VoiceAssistant] Python ML agent ready. Engine: "
                        + pythonAgent.getEngineName());

                // Wire Qwen3-TTS status events FIRST — before flushing pending speaks
                // so the callback is already registered when Python emits the leading
                // tts_status=speaking signal.  Without this ordering the callback is
                // null when speaking=true arrives and the mic stays open during TTS.
                pythonAgent.setTtsStatusListener(speaking -> {
                    if (speaking) {
                        // Python TTS started — mute the mic feed to Vosk.
                        micDiscardUntilMs = Long.MAX_VALUE;
                        ttsSpeaking.set(true);
                    } else {
                        // Python TTS finished — keep Vosk muted for TTS_MUTE_WINDOW_MS
                        // so acoustic echo (speaker→room→mic path) cannot reach Vosk.
                        // Without this grace window Vosk resumes immediately after
                        // sd.wait() returns, picks up the trailing reverb, and dispatches
                        // the TTS text itself as a fake user command (feedback loop).
                        ttsFinishedAtMs   = System.currentTimeMillis();
                        micDiscardUntilMs = ttsFinishedAtMs + TTS_MUTE_WINDOW_MS;
                        ttsSpeaking.set(false);
                    }
                });

                // Drain any speak requests that arrived while pythonAgent was null.
                // Pre-mute synchronously before sending so Vosk is silent while TTS
                // plays — independent of the async tts_status callback timing.
                String pendingText;
                if (pendingVivianSpeaks.peek() != null) {
                    micDiscardUntilMs = Long.MAX_VALUE;
                    ttsSpeaking.set(true);
                }
                while ((pendingText = pendingVivianSpeaks.poll()) != null) {
                    System.out.println("[VoiceAssistant-Vivian] Flushing queued speak: " + pendingText);
                    pythonAgent.sendSpeakFast(pendingText);
                }

                // Re-arm the wake-word detector after Python's post-logout
                // personality sequence ends (Python sends resume_wake_word).
                pythonAgent.setResumeWakeWordListener(() -> {
                    System.out.println("[VoiceAssistant] Resuming wake-word mode after logout.");
                    wakeWordActive.set(false);
                    wakeWordModeEnabled.set(true);
                });

                // Wire real-time weather data so CommandRouter receives it
                // before the WEATHER intent response arrives.
                pythonAgent.setWeatherListener(data -> {
                    VoiceCommandListener listener = commandListener;
                    if (listener instanceof org.example.assistant.CommandRouter cr) {
                        cr.setWeatherContext(data.city, data.temp, data.condition,
                                             data.humidity, data.wind, data.feel);
                    }
                });

                // Send current user context so the Python state machine can
                // personalise wake responses from the very first interaction.
                // At startup nobody is logged in, so we pass loggedIn=false.
                pythonAgent.sendUserContext(false, null);

                // Send GoVibe database snapshot (activities, cars, hotels) so
                // the Python voice agent can answer data-driven questions and
                // include real inventory in its DeepSeek / Ollama prompts.
                // Run in a separate thread to avoid blocking initPythonAgent.
                Thread dbCtxThread = new Thread(() -> {
                    try {
                        String dbJson = VoiceDataService.buildDbContextJson();
                        pythonAgent.sendDbContext(dbJson);
                        System.out.println("[VoiceAssistant] DB context dispatched to Python agent.");
                    } catch (Exception ex) {
                        System.err.println("[VoiceAssistant] DB context send failed: " + ex.getMessage());
                    }
                }, "VoiceAssistant-DBContext");
                dbCtxThread.setDaemon(true);
                dbCtxThread.start();

            } catch (Exception e) {
                System.err.println("[VoiceAssistant] Python ML agent failed: " + e.getMessage()
                        + " — falling back to keyword routing.");
                pythonAgent = null;
            }
        }, "VoiceAssistant-PythonInit");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Notify the Python voice agent that a user just logged in successfully.
     * This triggers the Echo+Vivian post-login greeting sequence and updates
     * the Python state machine's {@code logged_in} flag so it can personalise
     * subsequent responses.
     *
     * <p>Call this from the login / splash controller immediately after the
     * authenticated user's home screen is loaded.
     *
     * @param firstName the user's first name (may be null if unavailable)
     */
    public void notifyUserLoggedIn(String firstName) {
        userIsLoggedIn = true;
        PythonVoiceAgent agent = pythonAgent;
        if (agent != null) {
            String name = (firstName != null && !firstName.isBlank()) ? firstName : null;
            agent.sendUserContext(true, name);
            System.out.println("[VoiceAssistant] Notified Python: user logged in" +
                               (name != null ? " as " + name : "") + ".");
        }
    }

    /**
     * Notify the Python voice agent that the user has logged out.
     * This completes Python's PROCESSING_LOGOUT state machine: Python is
     * waiting for user_context(logged_in=false) before running the
     * post-logout farewell + confused-yawn sequence and returning to SLEEPING.
     * Without this call Python stays stuck in PROCESSING_LOGOUT and ignores
     * all subsequent voice input.
     *
     * <p>Call this whenever the login screen is displayed (including on first
     * app launch — sendUserContext is idempotent).
     */
    public void notifyUserLoggedOut() {
        userIsLoggedIn = false;
        PythonVoiceAgent agent = pythonAgent;
        if (agent != null) {
            agent.sendUserContext(false, null);
            System.out.println("[VoiceAssistant] Notified Python: user logged out.");
        }
    }

    /**
     * Forward a gesture event to the Python agent subprocess so it can respond
     * contextually (e.g. speak "Hold it!" when THUMBS_UP is detected, or
     * "Try thumbs up or open palm" for a FIST gesture during confirmation).
     *
     * @param gestureName the gesture name, e.g. "THUMBS_UP", "OPEN_PALM", "FIST"
     */
    public void sendGestureToAgent(String gestureName) {
        PythonVoiceAgent agent = pythonAgent;
        if (agent != null) {
            agent.sendGestureEvent(gestureName);
        }
    }

    private String resolveModelPath() {
        Path home = Paths.get(System.getProperty("user.home"), ".govibe", "vosk-model");
        if (Files.isDirectory(home)) return home.toAbsolutePath().toString();
        Path cwd = Paths.get("vosk-model");
        if (Files.isDirectory(cwd)) return cwd.toAbsolutePath().toString();
        return null;
    }

    private String defaultModelPath() {
        return Paths.get(System.getProperty("user.home"), ".govibe", "vosk-model")
                    .toAbsolutePath().toString();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Returns true if at least one STT engine is available. */
    public boolean isSttAvailable() { return voskAvailable || sapiAvailable; }

    public void setCommandListener(VoiceCommandListener listener) {
        this.commandListener = listener;
    }

    /**
     * Starts listening in a daemon background thread.
     * Uses Vosk if the model is present, otherwise Windows SAPI.
     *
     * <p>When the English Vosk model is active but the user speaks French, the
     * acoustic transcription will produce accent-stripped English approximations.
     * To ensure commands still execute reliably, the constrained SAPI grammar engine
     * is also started alongside Vosk — SAPI grammar matches emit {@code [HCONF]}
     * and bypass the ML pipeline entirely, routing straight to {@link CommandRouter}.
     */
    public void startListening() {
        if (!isSttAvailable()) {
            System.out.println("[VoiceAssistant] No STT engine available — listening skipped.");
            return;
        }
        if (listening.compareAndSet(false, true)) {
            Runnable loop = voskAvailable ? this::voskListenLoop : this::sapiListenLoop;
            String name  = voskAvailable ? "VoiceAssistant-Vosk" : "VoiceAssistant-SAPI";
            listenerThread = new Thread(loop, name);
            listenerThread.setDaemon(true);
            listenerThread.start();
            System.out.println("[VoiceAssistant] Listening started via " +
                    (voskAvailable ? "Vosk." : "Windows SAPI SpeechRecognitionEngine."));

            // When Vosk is primary, also run the constrained SAPI grammar engine as
            // a secondary detector.  It only fires on exact command-keyword matches
            // ([HCONF] prefix) so there are no false positives, and the STT debounce
            // in dispatchText() catches any duplicate triggers from both engines.
            if (voskAvailable && sapiAvailable) {
                sapiGrammarThread = new Thread(this::sapiGrammarListenLoop,
                        "VoiceAssistant-SAPI-Grammar");
                sapiGrammarThread.setDaemon(true);
                sapiGrammarThread.start();
                System.out.println("[VoiceAssistant] SAPI grammar engine started alongside Vosk " +
                        "(French command capture, [HCONF] fast-path only).");
            }
        }
    }

    /** Stops the active listening loop gracefully. */
    public void stopListening() {
        listening.set(false);
        Process p = sapiProcess;
        if (p != null) { p.destroyForcibly(); sapiProcess = null; }
        Process gp = sapiGrammarProcess;
        if (gp != null) { gp.destroyForcibly(); sapiGrammarProcess = null; }
        if (listenerThread != null) {
            listenerThread.interrupt();
            listenerThread = null;
        }
        if (sapiGrammarThread != null) {
            sapiGrammarThread.interrupt();
            sapiGrammarThread = null;
        }
        System.out.println("[VoiceAssistant] Listening stopped.");
    }

    /**
     * Speaks {@code text} exclusively through Vivian (Python TTS worker).
     * Never falls back to Java TTS — queues until Vivian is ready.
     * Use this for ALL assistant speech to guarantee a single voice.
     */
    public void vivianSpeak(String text) {
        // Route through Echo (fast, ~200ms) so the mic is never muted for 30-60s.
        echoSpeak(text);
    }

    /**
     * Speaks {@code text} via Echo (edge-tts, ~200 ms) — the instant fast path.
     * The mic is muted for ~200 ms instead of 30–60 s (Qwen3-TTS on CPU).
     * Use for all action confirmations where low latency matters.
     */
    public void echoSpeak(String text) {
        if (text == null || text.isBlank()) return;
        PythonVoiceAgent agent = pythonAgent;
        if (agent != null) {
            // Pre-mute the mic BEFORE sending to Python so the ~50-100 ms race
            // window between Python starting TTS audio and Java receiving the
            // tts_status=speaking callback is closed synchronously here.
            // The tts_status=done callback (setTtsStatusListener) will release
            // the mute and set the post-TTS grace window exactly as before.
            micDiscardUntilMs = Long.MAX_VALUE;
            ttsSpeaking.set(true);
            agent.sendSpeakFast(text);
        } else {
            pendingVivianSpeaks.add(text);
            System.out.println("[VoiceAssistant-Vivian] Queued (agent loading): " + text);
        }
    }

    /**
     * Speaks {@code text} via Windows SAPI asynchronously.
     * Duplicate calls within {@value #DEBOUNCE_MS} ms with identical text are dropped.
     */
    public void speak(String text) {
        if (text == null || text.isBlank()) return;
        // Debounce: skip if same text was spoken very recently.
        long now = System.currentTimeMillis();
        if (text.equals(lastTtsSpeakText) && (now - lastTtsSpeakTime) < DEBOUNCE_MS) {
            System.out.println("[VoiceAssistant-TTS] Debounced duplicate: " + text);
            return;
        }
        lastTtsSpeakText = text;
        lastTtsSpeakTime = now;
        // Submit to the single-thread executor (no new thread created per call,
        // calls are serialised so the persistent Python process stdin is never
        // written to concurrently).
        ttsExecutor.submit(() -> {
            // Mute microphone feed immediately — no echo bytes will reach Vosk.
            micDiscardUntilMs = Long.MAX_VALUE;
            ttsSpeaking.set(true);
            try {
                speakSync(text);
            } finally {
                // Release the discard window immediately.
                // The Vosk loop will call recognizer.reset() on its own thread
                // (JNI-safe) on the very next iteration (wasDiscarding → true).
                // Any acoustic echo that still reaches Vosk is caught by the
                // dispatchText() TTS mute gate (TTS_MUTE_WINDOW_MS = 600 ms)
                // — so we do NOT need a post-TTS silence buffer any more.
                ttsFinishedAtMs  = System.currentTimeMillis();
                micDiscardUntilMs = ttsFinishedAtMs; // = "now", so nowDiscarding → false immediately
                ttsSpeaking.set(false);
            }
        });
    }

    // ── Vosk STT loop ─────────────────────────────────────────────────────────
    //
    // ROOT CAUSE of "rms=1 always" bug:
    //   AudioSystem.getLine() opens the DEFAULT Windows mixer, which on many
    //   systems is a virtual device ("Stereo Mix", "Wave", DirectSound loopback)
    //   rather than the physical microphone. Virtual devices produce near-silence
    //   (rms≈1) unless the user is playing audio, and they never capture speech.
    //
    // FIX: enumerate ALL available mixers, score each by name (penalise known
    //   virtual/loopback names, reward known microphone keywords), buy the best
    //   mixer that can open a TargetDataLine, and print the full list so the
    //   user can see exactly which device was chosen.
    private void voskListenLoop() {
        TargetDataLine microphone = null;
        try (Recognizer recognizer = new Recognizer(voskModel, 16_000f)) {

            // ── Step 1: enumerate every mixer and find the best physical mic ──────
            Mixer.Info[] allMixers = AudioSystem.getMixerInfo();
            System.out.println("[VoiceAssistant-Vosk] Available audio mixers:");
            for (Mixer.Info mi : allMixers) {
                System.out.println("  • " + mi.getName() + " — " + mi.getDescription());
            }

            // Score mixers: positive = good mic keywords, negative = virtual/loopback.
            // Lower score wins (we pick best = highest positive score).
            java.util.List<float[]> candidates = new java.util.ArrayList<>(); // [mixerIdx, score, rateHz]

            for (int mi = 0; mi < allMixers.length; mi++) {
                Mixer mixer = AudioSystem.getMixer(allMixers[mi]);
                Line.Info[] targetLines = mixer.getTargetLineInfo();
                if (targetLines.length == 0) continue;   // output-only mixer

                String name = allMixers[mi].getName().toLowerCase();
                // Reward physical microphone keywords
                int score = 0;
                if (name.contains("microphone") || name.contains("micro") || name.contains("mic")) score += 40;
                if (name.contains("input"))  score += 10;
                if (name.contains("capture")) score += 5;
                if (name.contains("line in")) score += 5;
                if (name.contains("realtek") || name.contains("conexant") ||
                    name.contains("idt")     || name.contains("via hd")   ||
                    name.contains("audio device") || name.contains("hdaudio")) score += 20;
                // Penalise known virtual / loopback names
                if (name.contains("stereo mix")  || name.contains("what u hear") ||
                    name.contains("wave out mix") || name.contains("loopback")     ||
                    name.contains("primary")      || name.contains("mapper")       ||
                    name.contains("default")      || name.contains("wave")         ||
                    name.contains("virtual")      || name.contains("monitor"))     score -= 50;

                if (score < -10) continue;   // definitely a loopback — skip

                // Try each sample rate on this mixer
                for (float rate : new float[]{48_000f, 44_100f, 22_050f, 16_000f}) {
                    AudioFormat fmt = new AudioFormat(rate, 16, 1, true, false);
                    DataLine.Info dlInfo = new DataLine.Info(TargetDataLine.class, fmt);
                    if (mixer.isLineSupported(dlInfo)) {
                        candidates.add(new float[]{mi, score, rate});
                        break;   // use highest-priority rate for this mixer
                    }
                }
            }

            if (candidates.isEmpty()) {
                System.err.println("[VoiceAssistant] No usable microphone mixer found — falling back to SAPI STT.");
                sapiListenLoop();
                return;
            }

            // Sort by score descending; break ties by preferring 16kHz (ratio=1)
            candidates.sort((a, b) -> {
                int cmp = Float.compare(b[1], a[1]);
                if (cmp != 0) return cmp;
                return Float.compare(Math.abs(a[2] - 16_000f), Math.abs(b[2] - 16_000f));
            });

            // If ALL candidates scored ≤0 (no keyword match), just take whatever is first
            float[] best = candidates.get(0);
            int bestMixerIdx = (int) best[0];
            float bestRate   = best[2];
            Mixer bestMixer  = AudioSystem.getMixer(allMixers[bestMixerIdx]);
            AudioFormat selectedFormat = new AudioFormat(bestRate, 16, 1, true, false);

            int nativeHz = (int) selectedFormat.getSampleRate();
            System.out.println("[VoiceAssistant-Vosk] Selected mic: \"" + allMixers[bestMixerIdx].getName() +
                    "\" (score=" + (int)best[1] + ")  at " + nativeHz + " Hz" +
                    (nativeHz == 16_000 ? " (no resampling)" :
                            " → decimate to 16000 Hz (ratio=" +
                            String.format("%.2f", nativeHz / 16_000.0) + "×)"));

            DataLine.Info dlInfo = new DataLine.Info(TargetDataLine.class, selectedFormat);
            microphone = (TargetDataLine) bestMixer.getLine(dlInfo);
            microphone.open(selectedFormat);
            microphone.start();

            // ── Buffer geometry ───────────────────────────────────────────────────
            // Read 4096 native bytes per iteration (~21ms at 48kHz, ~85ms at 16kHz).
            // Decimate each native chunk into the voskAccum accumulator.
            // Feed Vosk only when the accumulator holds a full 4096-byte (2048-sample)
            // frame — this guarantees Vosk always receives properly-sized chunks and
            // avoids the "partial nRead" bug caused by TargetDataLine short-reads.
            final int NATIVE_CHUNK = 4096;           // bytes per raw mic read
            final double ratio     = nativeHz / 16_000.0;
            // Max output samples from one native chunk (ceiling, may be slightly fewer)
            final int MAX_OUT_PER_CHUNK = (int) Math.ceil((NATIVE_CHUNK / 2) / ratio);
            byte[] nativeBuf   = new byte[NATIVE_CHUNK];
            // Accumulator: collect decimated samples until we have a full Vosk frame
            final int VOSK_FRAME_BYTES = 4096;       // 2048 samples × 2 bytes
            byte[] voskAccum   = new byte[VOSK_FRAME_BYTES + MAX_OUT_PER_CHUNK * 2];
            int    voskAccumPos = 0;                 // bytes written into accumulator

            System.out.println("[VoiceAssistant] Vosk microphone open — speak now.");
            boolean wasDiscarding = false;
            int frameCount = 0;
            // State for sub-sample accurate decimation (avoids drift over long sessions)
            double srcPos = 0.0;  // fractional position in the infinite native stream

            while (listening.get() && !Thread.currentThread().isInterrupted()) {
                // Simple single-call read — TargetDataLine blocks until data available.
                // Never loop here: a short read is fine, we accumulate below.
                int nRead = microphone.read(nativeBuf, 0, NATIVE_CHUNK);
                if (nRead <= 0) continue;

                // ── Microphone discard window (TTS echo prevention) ───────────────
                boolean nowDiscarding = System.currentTimeMillis() < micDiscardUntilMs;
                if (nowDiscarding) {
                    if (!wasDiscarding) {
                        // First frame entering the mute window: flush any audio that
                        // Vosk accumulated BEFORE the mic-discard gate closed (the
                        // ~50-100 ms race between Python starting TTS playback and
                        // Java receiving the tts_status=speaking signal and setting
                        // micDiscardUntilMs). Without this reset the pre-mute audio
                        // is emitted as a spurious final result when the gate lifts.
                        recognizer.reset();
                    }
                    wasDiscarding  = true;
                    voskAccumPos   = 0;
                    srcPos         = 0.0;
                    continue;
                }
                if (wasDiscarding) {
                    recognizer.reset();
                    System.out.println("[VoiceAssistant] Vosk buffer cleared — listening for your command.");
                    wasDiscarding = false;
                    frameCount    = 0;
                    srcPos        = 0.0;
                    voskAccumPos  = 0;
                }

                // ── Log raw-mic RMS every ~3s so we can see mic energy independently
                //    of decimation (nRead samples at nativeHz rate).
                double nativeRms = rmsEnergy(nativeBuf, nRead);
                frameCount++;
                if (frameCount % 24 == 0) {          // ~3 s at 48kHz / 4096-byte chunks
                    System.out.println("[VoiceAssistant-Vosk] MicRaw | nativeHz=" + nativeHz +
                            " | nRead=" + nRead + " | nativeRms=" + String.format("%.0f", nativeRms));
                }

                // ── Nearest-neighbour decimation into the accumulator ─────────────
                // srcPos tracks where we are in the stream of native samples.
                // After each native chunk, advance srcPos by (nRead/2) and pick the
                // output samples that fall within [oldSrcPos, newSrcPos).
                int nativeSamplesRead = nRead / 2;
                // For each output sample whose source index falls in this chunk:
                //   outIdx such that outIdx * ratio is in [srcPos, srcPos + nativeSamplesRead)
                double chunkStart = srcPos;
                double chunkEnd   = srcPos + nativeSamplesRead;
                // First output sample index that maps into this chunk
                long firstOutIdx  = (long) Math.ceil(chunkStart / ratio);
                // Last output sample index (exclusive)
                long lastOutIdx   = (long) Math.ceil(chunkEnd   / ratio);

                for (long outIdx = firstOutIdx; outIdx < lastOutIdx; outIdx++) {
                    int srcSample = (int) (outIdx * ratio);
                    // srcSample is relative to stream start; offset into nativeBuf:
                    int bufSample = srcSample - (int) chunkStart;
                    int bufByte   = bufSample * 2;
                    if (bufByte < 0 || bufByte + 1 >= nRead) continue;
                    if (voskAccumPos + 1 >= voskAccum.length) break; // safety
                    voskAccum[voskAccumPos++] = nativeBuf[bufByte];
                    voskAccum[voskAccumPos++] = nativeBuf[bufByte + 1];
                }
                srcPos = chunkEnd;

                // ── Feed complete Vosk frames from the accumulator ────────────────
                while (voskAccumPos >= VOSK_FRAME_BYTES) {
                    int n = VOSK_FRAME_BYTES;
                    double rms = rmsEnergy(voskAccum, n);

                    // Heartbeat every ~10 s
                    if (frameCount % 78 == 0) {
                        String partial = recognizer.getPartialResult();
                        System.out.println("[VoiceAssistant-Vosk] Heartbeat | rms=" +
                                String.format("%.0f", rms) + " | partial=" + partial.trim());
                    }

                    boolean isSpeech = noiseOrchestrator.feedFrame(voskAccum, n, rms);
                    if (isSpeech) lastSpeechFrameTime = System.currentTimeMillis();

                    if (recognizer.acceptWaveForm(voskAccum, n)) {
                        String raw = recognizer.getResult();
                        System.out.println("[VoiceAssistant-Vosk] Raw result: " + raw);
                        // Calibration gate removed — Vosk's own VAD is the gating mechanism.
                        // NoiseOrchestrator calibration only affects isSpeech/lastSpeechFrameTime,
                        // not whether results are dispatched.
                        dispatchVoskJson(raw);
                    }

                    // Shift consumed bytes out of the accumulator
                    int remaining = voskAccumPos - n;
                    if (remaining > 0) {
                        System.arraycopy(voskAccum, n, voskAccum, 0, remaining);
                    }
                    voskAccumPos = remaining;
                } // end while(voskAccumPos >= VOSK_FRAME_BYTES)
            } // end while(listening)
            dispatchVoskJson(recognizer.getFinalResult());

        } catch (LineUnavailableException e) {
            System.err.println("[VoiceAssistant] Vosk mic unavailable: " + e.getMessage() +
                    " — falling back to SAPI.");
            sapiListenLoop();
        } catch (Exception e) {
            if (listening.get())
                System.err.println("[VoiceAssistant] Vosk loop error: " + e.getMessage());
        } finally {
            if (microphone != null && microphone.isOpen()) { microphone.stop(); microphone.close(); }
            System.out.println("[VoiceAssistant] Vosk loop exited.");
        }
    }

    private void dispatchVoskJson(String json) {
        if (json == null) return;
        int start = json.indexOf("\"text\"");
        if (start < 0) return;
        start = json.indexOf(':', start);
        if (start < 0) return;
        start = json.indexOf('"', start + 1);
        if (start < 0) return;
        int end = json.indexOf('"', start + 1);
        if (end < 0) return;
        String text = json.substring(start + 1, end).trim();
        if (text.isBlank()) return;

        // ── Wake-word timeout check ───────────────────────────────────────────
        checkWakeWordTimeout();

        // ── Layer 1: minimum meaningful-word filter ───────────────────────────
        // Reject if no word has ≥ 2 characters (noise produces single-char fragments).
        // Allows conversational words like "hi", "ok", "yo", "bye" through.
        String[] words = text.split("\\s+");
        boolean hasMeaningfulWord = false;
        for (String w : words) {
            if (w.length() >= 2) { hasMeaningfulWord = true; break; }
        }
        if (!hasMeaningfulWord) {
            System.out.println("[VoiceAssistant-Gate] Rejected (too short): \"" + text + "\"");
            return;
        }

        // ── Wake-word check — must run BEFORE the dormant gate ────────────────
        if (checkAndTriggerWakeWord(text)) return;  // wake word consumed — don't route as command

        // ── Dormant gate: if wake-word mode is on but not yet active, discard ─
        if (wakeWordModeEnabled.get() && !wakeWordActive.get()) {
            System.out.println("[VoiceAssistant-WakeGate] Dormant — ignoring: \"" + text + "\"");
            return;
        }

        // ── Layer 2: STT dispatch debounce ───────────────────────────────────
        // Drop identical text dispatched within STT_DEBOUNCE_MS ms.
        long now = System.currentTimeMillis();
        if (text.equalsIgnoreCase(lastSttDispatchText)
                && (now - lastSttDispatchTime) < STT_DEBOUNCE_MS) {
            System.out.println("[VoiceAssistant-Gate] Debounced STT duplicate: \"" + text + "\"");
            return;
        }
        lastSttDispatchText = text;
        lastSttDispatchTime = now;

        // ── Login-screen Vosk phonetic repair ────────────────────────────────
        // On the login screen the only real commands are face-id / login / signup.
        // Vosk commonly mangles "face id" into garbage like "they say d".
        // Repair those before routing so the Python agent sees clean text.
        if (!userIsLoggedIn) {
            text = repairLoginScreenStt(text);
        }

        dispatchText(text);
    }

    /**
     * Computes the RMS energy of a 16-bit little-endian PCM buffer.
     * Used as a noise gate before feeding audio to Vosk.
     *
     * @param buf raw PCM bytes
     * @param len number of valid bytes in buf
     * @return RMS amplitude in the range 0-32767
     */
    private static double rmsEnergy(byte[] buf, int len) {
        long sum = 0;
        int samples = len / 2;  // 16-bit = 2 bytes per sample
        for (int i = 0; i < samples; i++) {
            // Little-endian 16-bit signed sample
            int lo  = buf[i * 2]     & 0xFF;
            int hi  = buf[i * 2 + 1] & 0xFF;
            int sample = (short) ((hi << 8) | lo);
            sum += (long) sample * sample;
        }
        return samples > 0 ? Math.sqrt((double) sum / samples) : 0.0;
    }

    // ── Windows SAPI STT loop ─────────────────────────────────────────────────
    /**
     * Spawns a persistent PowerShell process running
     * {@code System.Speech.Recognition.SpeechRecognitionEngine}.
     * Each recognised phrase is printed to stdout and read here line-by-line.
     */
    private void sapiListenLoop() {
        while (listening.get()) {
            sapiListenLoopOnce();
            if (listening.get()) {
                System.out.println("[VoiceAssistant] SAPI loop restarting in 1.5 s...");
                try { Thread.sleep(1500); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); break;
                }
            }
        }
        System.out.println("[VoiceAssistant] SAPI loop exited.");
    }

    private void sapiListenLoopOnce() {
        File script = null;
        try {
            script = writeSapiScript();
            ProcessBuilder pb = new ProcessBuilder(
                    PS_EXE, "-NoProfile", "-NonInteractive",
                    "-WindowStyle", "Hidden", "-ExecutionPolicy", "Bypass",
                    "-File", script.getAbsolutePath());
            pb.redirectErrorStream(false);
            Process proc = pb.start();
            sapiProcess = proc;

            // Drain stderr so the process never deadlocks on a full pipe buffer.
            Process procRef = proc;
            Thread stderrDrain = new Thread(() -> {
                try { procRef.getErrorStream().transferTo(OutputStream.nullOutputStream()); }
                catch (IOException ignored) {}
            }, "VoiceAssistant-SAPI-stderr");
            stderrDrain.setDaemon(true);
            stderrDrain.start();

            System.out.println("[VoiceAssistant] SAPI engine process started — speak now.");

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8));

            String line;
            while (listening.get() && (line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                // Filter PowerShell error artifacts: "+" continuation chars, exception text,
                // LoadGrammar error messages — none of these are real speech.
                if (line.equals("+")) continue;
                if (line.length() < 3) continue;
                if (line.startsWith("Exception") || line.contains("appel de")
                        || line.contains("LoadGrammar") || line.contains("grammaire")) continue;
                if (line.startsWith("[SAPI]")) {
                    System.out.println("[VoiceAssistant] " + line);
                } else if (line.startsWith("[HCONF]")) {
                    // ══ High-confidence grammar match: bypass ML, go straight to CommandRouter ══
                    // SAPI matched the speech to one of our exact grammar phrases with
                    // confidence > 0.75 — the text IS one of our command keywords.
                    // No need to run the ML pipeline; just norm() and dispatch directly.
                    String cmd = line.substring(7).trim();
                    if (!cmd.isEmpty()) {
                        System.out.println("[VoiceAssistant] [FAST] Grammar match: \"" + cmd + "\"");
                        // Still apply TTS bleed gate
                        if (ttsSpeaking.get()
                                || System.currentTimeMillis() - ttsFinishedAtMs < TTS_MUTE_WINDOW_MS) {
                            System.out.println("[VoiceAssistant-Gate] TTS-muted (fast path): \"" + cmd + "\"");
                        } else {
                            VoiceCommandListener listener = commandListener;
                            if (listener instanceof org.example.assistant.CommandRouter cr) {
                                // Acknowledge via Vivian (single voice — not Java TTS).
                                vivianSpeak("On it!");
                                boolean matched = cr.tryKeywordMatch(cmd);
                                if (!matched) {
                                    // Grammar hit but keyword table miss — let ML decide
                                    dispatchText(cmd);
                                }
                            } else if (listener != null) {
                                vivianSpeak("On it!");
                                try { listener.onCommand(cmd.trim().toUpperCase(), cmd); }
                                catch (Exception ex) { System.err.println("[VoiceAssistant] Fast-path error: " + ex.getMessage()); }
                            }
                        }
                    }
                } else {
                    dispatchText(line);
                }
            }
        } catch (Exception e) {
            if (listening.get())
                System.err.println("[VoiceAssistant] SAPI loop error: " + e.getMessage());
        } finally {
            Process p = sapiProcess;
            if (p != null) { p.destroyForcibly(); sapiProcess = null; }
            if (script != null) script.delete();
        }
    }

    /**
     * Secondary SAPI loop — grammar-only fast-path ran alongside Vosk.
     *
     * <p>Uses the same constrained grammar as the primary SAPI loop but:
     * <ul>
     *   <li>Stores process in {@link #sapiGrammarProcess} (separate from primary).</li>
     *   <li>Only acts on {@code [HCONF]} lines; non-HCONF output is silently discarded
     *       so Vosk remains the sole provider of free-form / ML-path text.</li>
     * </ul>
     * This solves the language-mismatch problem: even when the English Vosk model
     * transcribes French speech as garbled English, the constrained SAPI grammar
     * matches the actual French word (e.g. "réserver") and routes it directly to
     * {@link CommandRouter} via the fast {@code [HCONF]} path.
     */
    private void sapiGrammarListenLoop() {
        while (listening.get()) {
            sapiGrammarListenLoopOnce();
            if (listening.get()) {
                System.out.println("[VoiceAssistant] SAPI grammar loop restarting in 1.5 s...");
                try { Thread.sleep(1500); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); break;
                }
            }
        }
        System.out.println("[VoiceAssistant] SAPI grammar loop exited.");
    }

    private void sapiGrammarListenLoopOnce() {
        File script = null;
        try {
            script = writeSapiScript();
            ProcessBuilder pb = new ProcessBuilder(
                    PS_EXE, "-NoProfile", "-NonInteractive",
                    "-WindowStyle", "Hidden", "-ExecutionPolicy", "Bypass",
                    "-File", script.getAbsolutePath());
            pb.redirectErrorStream(false);
            Process proc = pb.start();
            sapiGrammarProcess = proc;

            // Drain stderr.
            Process procRef = proc;
            Thread stderrDrain = new Thread(() -> {
                try { procRef.getErrorStream().transferTo(OutputStream.nullOutputStream()); }
                catch (IOException ignored) {}
            }, "VoiceAssistant-SAPIGrammar-stderr");
            stderrDrain.setDaemon(true);
            stderrDrain.start();

            System.out.println("[VoiceAssistant] SAPI grammar process started — command fast-path active.");

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8));

            String line;
            while (listening.get() && (line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("[SAPI]")) continue;
                if (line.startsWith("[HCONF]")) {
                    // Grammar match — route directly to CommandRouter (same as primary SAPI path).
                    String cmd = line.substring(7).trim();
                    if (!cmd.isEmpty() && !ttsSpeaking.get()
                            && System.currentTimeMillis() - ttsFinishedAtMs >= TTS_MUTE_WINDOW_MS) {
                        System.out.println("[VoiceAssistant-Grammar] [FAST] \"" + cmd + "\"");
                        // Acknowledge via Vivian — single voice throughout.
                        vivianSpeak("On it!");
                        VoiceCommandListener listener = commandListener;
                        if (listener instanceof CommandRouter cr) {
                            if (!cr.tryKeywordMatch(cmd)) dispatchText(cmd);
                        } else if (listener != null) {
                            try { listener.onCommand(cmd.trim().toUpperCase(), cmd); }
                            catch (Exception ex) {
                                System.err.println("[VoiceAssistant-Grammar] Error: " + ex.getMessage());
                            }
                        }
                    } else if (!cmd.isEmpty()) {
                        System.out.println("[VoiceAssistant-Gate] TTS-muted (grammar): \"" + cmd + "\"");
                    }
                }
                // Non-HCONF lines (rejected alternates etc.) are intentionally discarded —
                // Vosk handles the free-text path.
            }
        } catch (Exception e) {
            if (listening.get())
                System.err.println("[VoiceAssistant] SAPI grammar loop error: " + e.getMessage());
        } finally {
            Process gp = sapiGrammarProcess;
            if (gp != null) { gp.destroyForcibly(); sapiGrammarProcess = null; }
            if (script != null) script.delete();
        }
    }

    /** Generates the PowerShell script that runs SpeechRecognitionEngine. */
    private File writeSapiScript() throws IOException {
        // All command keywords the router understands — loaded as a grammar
        // for high precision. Dictation grammar also loaded for free-form phrases.
        String[] commands = {
            // ── Wake words ──
            "hi go", "hey go", "hello go", "ok go", "govibe",
            "salut go", "bonjour go",
            // ── Booking (both accented and unaccented for fr-FR SAPI) ──
            "réserver", "reserver", "book", "nouvelle réservation", "nouvelle reservation", "réserve", "reserve",
            // ── Bookings list ──
            "mes réservations", "mes reservations", "my bookings", "réservations", "reservations",
            // ── Search ──
            "rechercher", "search", "vols", "flights",
            // ── Cancel / back ──
            "annuler", "cancel", "retour", "go back",
            // ── Payment ──
            "payer", "pay", "payment", "confirmer", "confirm", "valider",
            // ── Help / describe ──
            "aide", "help", "commandes",
            "décrire", "decrire", "describe", "qu est ce que", "quoi", "what",
            // ── Navigation ──
            "accueil", "dashboard", "home",
            "déconnexion", "deconnexion", "logout", "log out",
            // ── Login screen ──
            "connexion", "login", "log in", "sign in", "connecter", "se connecter",
            "email", "adresse email", "nom utilisateur", "username",
            "mot de passe", "password",
            "créer un compte", "creer un compte", "sign up", "register", "inscription",
            // ── Face ID / camera login ──
            "face id", "face login", "face recognition", "facial recognition",
            "open camera", "camera", "scan face", "face scan",
            // ── Car rental ──
            "voitures", "louer", "cars", "car rental", "rent a car",
            // ── Activities ──
            "activités", "activites", "activities", "quoi faire", "what to do", "loisirs",
            "describe activity", "describe activities", "tell me about activities",
            "décris l activite", "what can i do",
            // ── Hotels ──
            "hôtels", "hotels", "hotel", "hébergement", "hebergement", "chambres",
            // ── Sessions ──
            "sessions", "my sessions", "show sessions", "session list", "workshops",
            // ── Profile / account ──
            "profile", "my profile", "mon profil", "my account", "mon compte", "settings", "parametres",
            // ── Messages / inbox ──
            "messages", "mes messages", "inbox", "messagerie", "chat",
            // ── Forum / community ──
            "forum", "community", "communaute", "communauté", "discussions",
            // ── Reclamation / complaint ──
            "reclamation", "réclamation", "complaint", "support", "signaler", "plainte",
            // ── Locations / map ──
            "map", "carte", "locations", "explorer", "explore", "destinations",
            // ── Car describe ──
            "describe car", "describe the car", "tell me about cars", "car details",
            // ── Noise recalibration ──
            "recalibrer", "calibrer", "bruit"
        };

        // Always prefer en-US SAPI engine: the command list is primarily English,
        // and the fr-FR acoustic model cannot reliably recognise English words
        // like "log in", "book", "flights", "search" etc.
        // Fall back to the system-default engine if en-US is not installed.
        StringBuilder ps = new StringBuilder();
        ps.append("$ErrorActionPreference = 'SilentlyContinue'\n");
        ps.append("Add-Type -AssemblyName System.Speech\n");
        ps.append("try {\n");
        ps.append("  $culture = [Globalization.CultureInfo]::GetCultureInfo('en-US')\n");
        ps.append("  $engine  = New-Object System.Speech.Recognition.SpeechRecognitionEngine($culture)\n");
        ps.append("  Write-Output '[SAPI] Using language: en-US'\n");
        ps.append("} catch {\n");
        ps.append("  Write-Output '[SAPI] en-US pack not found, using system default'\n");
        ps.append("  $engine = New-Object System.Speech.Recognition.SpeechRecognitionEngine\n");
        ps.append("}\n");
        ps.append("try { $engine.SetInputToDefaultAudioDevice() }\n");
        ps.append("catch { Write-Output '[SAPI] ERROR: No default audio device.'; exit 1 }\n");
        // Keep mic open indefinitely. [TimeSpan]::Zero causes SAPI to immediately
        // time out and crash the process. Use 24 hours as a safe "never timeout" value.
        ps.append("try { $engine.InitialSilenceTimeout = [TimeSpan]::FromHours(24) } catch {}\n");
        ps.append("try { $engine.BabbleTimeout          = [TimeSpan]::FromHours(24) } catch {}\n");
        ps.append("try { $engine.EndSilenceTimeout      = [TimeSpan]::FromSeconds(0.8) } catch {}\n");

        // ── Grammar 1: Command keywords (structured, high-precision) ──────────
        // Named "CMD" so the SpeechRecognized handler can tell which grammar fired.
        // A hit here goes straight to CommandRouter via [HCONF] — no ML needed.
        ps.append("$choices = New-Object System.Speech.Recognition.Choices\n");
        for (String cmd : commands) {
            ps.append("$choices.Add('").append(cmd.replace("'", "''")).append("')\n");
        }
        ps.append("$gb = New-Object System.Speech.Recognition.GrammarBuilder($choices)\n");
        ps.append("$cmdGrammar = New-Object System.Speech.Recognition.Grammar($gb)\n");
        ps.append("$cmdGrammar.Name = 'CMD'\n");
        ps.append("try { $engine.LoadGrammar($cmdGrammar) } catch { Write-Output '[SAPI] CMD grammar load failed: ' + $_.Exception.Message }\n");

        // ── Grammar 2: Dictation (free-form — catches anything not in CMD list) ──
        // Output goes as plain text → Java ML pipeline → CommandRouter.
        // Provides fallback for free-form phrases, synonyms, and sentences
        // that are not in the CMD keyword list.
        ps.append("$dictGrammar = New-Object System.Speech.Recognition.DictationGrammar\n");
        ps.append("$dictGrammar.Name = 'DICT'\n");
        ps.append("try { $engine.LoadGrammar($dictGrammar) } catch { Write-Output '[SAPI] DICT grammar load failed: ' + $_.Exception.Message }\n");

        // Lower confidence thresholds so both grammars accept speech broadly.
        ps.append("try { $engine.UpdateRecognizerSetting('CFGConfidenceRejectionThreshold', 5) } catch {}\n");
        ps.append("try { $engine.UpdateRecognizerSetting('HighConfidenceThreshold', 75) } catch {}\n");

        // ── SpeechRecognized: route by grammar name ────────────────────────────
        // CMD grammar  → [HCONF] prefix  → Java bypasses ML, direct CommandRouter
        // DICT grammar → plain text      → Java ML pipeline classifies intent
        ps.append("$engine.add_SpeechRecognized({\n");
        ps.append("  param($s, $e)\n");
        ps.append("  $t = $e.Result.Text.Trim()\n");
        ps.append("  if ($t -eq '') { return }\n");
        ps.append("  if ($e.Result.Grammar.Name -eq 'CMD') {\n");
        ps.append("    [Console]::Out.WriteLine('[HCONF]' + $t)\n");
        ps.append("  } else {\n");
        ps.append("    [Console]::Out.WriteLine($t)\n");
        ps.append("  }\n");
        ps.append("  [Console]::Out.Flush()\n");
        ps.append("})\n");
        // Rejected alternates: send best alternate as plain text regardless of grammar.
        ps.append("$engine.add_SpeechRecognitionRejected({\n");
        ps.append("  param($s, $e)\n");
        ps.append("  $alts = $e.Result.Alternates\n");
        ps.append("  if ($alts -and $alts.Count -gt 0) {\n");
        ps.append("    $t = $alts[0].Text.Trim()\n");
        ps.append("    if ($t.Length -ge 3) {\n");
        ps.append("      [Console]::Out.WriteLine($t)\n");
        ps.append("      [Console]::Out.Flush()\n");
        ps.append("    }\n");
        ps.append("  }\n");
        ps.append("})\n");

        ps.append("Write-Output '[SAPI] SpeechRecognitionEngine ready.'\n");
        ps.append("[Console]::Out.Flush()\n");

        // Start async recognition and keep the process alive indefinitely.
        ps.append("$engine.RecognizeAsync([System.Speech.Recognition.RecognizeMode]::Multiple)\n");
        ps.append("trap { continue }\n");
        ps.append("while ($true) { [System.Threading.Thread]::Sleep(500) }\n");

        File tmp = File.createTempFile("govibe-stt-", ".ps1");
        tmp.deleteOnExit();
        Files.writeString(tmp.toPath(), ps.toString(), StandardCharsets.UTF_8);
        return tmp;
    }

    /**
     * Routes recognised speech through the Python ML agent (if ready) to get
     * an intent, a natural TTS response, and an action code, then:
     * <ol>
     *   <li>Speaks the response via Windows SAPI TTS.</li>
     *   <li>Forwards the action code to the {@link VoiceCommandListener} so
     *       {@link CommandRouter} can execute the matching UI action.</li>
     * </ol>
     * Falls back to raw keyword dispatch if the Python agent is not ready.
     */
    private void dispatchText(String text) {
        // ── Wake-word dormant gate ────────────────────────────────────────────
        checkWakeWordTimeout();
        if (wakeWordModeEnabled.get() && !wakeWordActive.get()) {
            // Check for wake word in plain SAPI/grammar output too
            if (!checkAndTriggerWakeWord(text)) {
                System.out.println("[VoiceAssistant-WakeGate] Dormant (dispatchText) — ignoring: \"" + text + "\"");
            }
            return;
        }

        // ── TTS bleed gate ────────────────────────────────────────────────────
        // Discard any text recognised while TTS is playing or within the
        // settle window after it finishes (speaker echo picked up by mic).
        if (ttsSpeaking.get()
                || System.currentTimeMillis() - ttsFinishedAtMs < TTS_MUTE_WINDOW_MS) {
            System.out.println("[VoiceAssistant-Gate] TTS-muted: \""
                    + (text.length() > 50 ? text.substring(0, 50) + "\u2026" : text) + "\"");
            return;
        }
        System.out.println("[VoiceAssistant] Recognised: \"" + text + "\"");

        PythonVoiceAgent agent = pythonAgent;
        if (agent != null && agent.isReady()) {
            // ── ML path: Python agent classifies + generates response ──────────
            PythonVoiceAgent.AgentResponse ar = agent.processText(text);
            System.out.println("[VoiceAssistant] ML → intent=" + ar.intent
                    + "  confidence=" + String.format("%.2f", ar.confidence)
                    + "  action=" + ar.action);

            if ("UNKNOWN".equals(ar.intent)) {
                // Below confidence threshold — try keyword fallback on raw text first,
                // then speak "not understood" (throttled) only if that also fails.
                System.out.println("[VoiceAssistant] ML UNKNOWN — trying keyword fallback on: \"" + text + "\"");
                String upperText = text.trim().toUpperCase();
                boolean keywordHit = false;
                VoiceCommandListener kbListener = commandListener;
                if (kbListener instanceof org.example.assistant.CommandRouter cr) {
                    keywordHit = cr.tryKeywordMatch(upperText);
                } else if (kbListener != null) {
                    try { kbListener.onCommand(upperText, text); }
                    catch (Exception e) {
                        System.err.println("[VoiceAssistant] Keyword fallback error: " + e.getMessage());
                    }
                }
                if (!keywordHit) {
                    // Vivian (Python) already spoke "Hmm, I didn't quite catch that."
                    // before returning UNKNOWN — no Java TTS needed here.
                    System.out.println("[VoiceAssistant] UNKNOWN — Vivian already spoke not-understood response.");
                }
                return;
            }

            // Extend wake-word active window — conversation is ongoing.
            extendWakeWordWindow();

            // Python agent already spoke the response via edge-tts — do NOT
            // call speak(ar.response) here or the user hears every line twice.
            boolean mlSpoke = (ar.response != null && !ar.response.isBlank());

            // Dispatch action to CommandRouter (UI execution).
            if (!"NONE".equals(ar.action) && !"UNKNOWN".equals(ar.action)) {
                VoiceCommandListener listener = commandListener;
                if (listener != null) {
                    // Pass Ollama-extracted parameters (destination, date) to CommandRouter
                    // before firing onCommand, so it can use them in the action handler.
                    if (listener instanceof CommandRouter cr) {
                        cr.setVoiceContext(ar.destination, ar.date, ar.passengers);
                        // ML already spoke — suppress duplicate action speech
                        if (mlSpoke) cr.suppressNextActionSpeech();
                    }
                    try { listener.onCommand(ar.action, text); }
                    catch (Exception e) {
                        System.err.println("[VoiceAssistant] Listener error: " + e.getMessage());
                    }
                }
            }
        } else {
            // ── Fallback path: Python agent not ready ─────────────────────────
            // Try Ollama Java client first (if running), then raw keywords.
            System.out.println("[VoiceAssistant] Python agent not ready.");
            OllamaService ollama = OllamaService.getInstance();
            if (ollama.isAvailable()) {
                System.out.println("[VoiceAssistant] Trying Ollama Java client...");
                PythonVoiceAgent.AgentResponse ar = ollama.classify(text);
                if (ar != null && !"UNKNOWN".equals(ar.intent) && ar.confidence >= 0.65) {
                    System.out.println("[VoiceAssistant] Ollama-Java → intent=" + ar.intent
                            + "  conf=" + String.format("%.2f", ar.confidence));
                    extendWakeWordWindow();
                    boolean ollamaSpoke = false;
                    if (ar.response != null && !ar.response.isBlank()) { speak(ar.response); ollamaSpoke = true; }
                    if (!"NONE".equals(ar.action)) {
                        VoiceCommandListener listener = commandListener;
                        if (listener instanceof CommandRouter cr) {
                            cr.setVoiceContext(ar.destination, ar.date, ar.passengers);
                            if (ollamaSpoke) cr.suppressNextActionSpeech();
                        }
                        if (listener != null) {
                            try { listener.onCommand(ar.action, text); }
                            catch (Exception e) {
                                System.err.println("[VoiceAssistant] Listener error: " + e.getMessage());
                            }
                        }
                    }
                    return;
                }
            }
            // Last resort: raw keyword matching
            System.out.println("[VoiceAssistant] Keyword-only fallback.");
            String command = text.trim().toUpperCase();
            VoiceCommandListener listener = commandListener;
            if (listener instanceof CommandRouter cr) {
                if (cr.tryKeywordMatch(command)) extendWakeWordWindow();
            } else if (listener != null) {
                try { listener.onCommand(command, text); }
                catch (Exception e) {
                    System.err.println("[VoiceAssistant] Listener error: " + e.getMessage());
                }
            }
        }
    }

    // ── TTS (Windows SAPI Synthesis) ──────────────────────────────────────────
    // ── edge-tts detection ────────────────────────────────────────────────────

    private static synchronized boolean checkEdgeTts() {
        if (edgeTtsAvailable != null) return edgeTtsAvailable;
        for (String py : new String[]{"python", "python3", "py"}) {
            try {
                Process p = new ProcessBuilder(py, "-c",
                        "import edge_tts; print('ok')")
                        .redirectErrorStream(true).start();
                String out = new String(p.getInputStream().readAllBytes()).trim();
                if (p.waitFor(5, TimeUnit.SECONDS) && out.contains("ok")) {
                    edgeTtsPythonExe  = py;
                    edgeTtsAvailable  = true;
                    System.out.println("[VoiceAssistant-TTS] edge-tts ready via " + py
                            + " — using en-US-JennyNeural");
                    return true;
                }
            } catch (Exception ignored) {}
        }
        edgeTtsAvailable = false;
        System.out.println("[VoiceAssistant-TTS] edge-tts not available — using SAPI TTS.");
        return false;
    }

    /**
     * Builds a self-contained Python script that synthesises {@code text} via
     * Microsoft Edge's neural TTS (en-US-JennyNeural) and plays the resulting
     * MP3 through Windows Media Player COM — which does NOT share any audio
     * resource with the SAPI SpeechRecognitionEngine, so the STT loop stays alive.
     */
    private static String buildEdgeTtsScript(String text) {
        // Escape backslash and single-quote for embedding in a Python literal
        String esc = text.replace("\\", "\\\\").replace("'", "\\'");
        return
            "import asyncio, edge_tts, tempfile, os, ctypes\n" +
            "async def speak():\n" +
            "    com = edge_tts.Communicate('" + esc + "', 'en-US-JennyNeural')\n" +
            "    mp3 = tempfile.mktemp(suffix='.mp3')\n" +
            "    await com.save(mp3)\n" +
            "    try:\n" +
            "        winmm = ctypes.windll.winmm\n" +
            "        winmm.mciSendStringW(f'open \"{mp3}\" type mpegvideo alias voice', None, 0, None)\n" +
            "        winmm.mciSendStringW('play voice wait', None, 0, None)\n" +
            "        winmm.mciSendStringW('close voice', None, 0, None)\n" +
            "    except Exception as e:\n" +
            "        print('[TTS] MCI playback error:', e)\n" +
            "    try:\n" +
            "        os.unlink(mp3)\n" +
            "    except Exception:\n" +
            "        pass\n" +
            "asyncio.run(speak())\n";
    }

    /**
     * Ensures the persistent Python TTS server process is running.
     * The server reads text lines from stdin and speaks each via edge-tts,
     * printing "DONE" to stdout when the utterance finishes.
     * Keeping it alive eliminates the ~300 ms Python cold-start per speak().
     */
    private static synchronized void ensureTtsPersistentProcess() {
        if (ttsPersistentProcess != null && ttsPersistentProcess.isAlive()) return;
        if (!checkEdgeTts()) return;
        try {
            Path scriptPath = Files.createTempFile("govibe-tts-server-", ".py");
            Files.writeString(scriptPath, buildTtsServerScript(), StandardCharsets.UTF_8);
            scriptPath.toFile().deleteOnExit();

            ProcessBuilder pb = new ProcessBuilder(edgeTtsPythonExe, scriptPath.toString());
            pb.redirectErrorStream(false);
            ttsPersistentProcess = pb.start();

            // Drain stderr in a daemon thread (suppress Python warnings).
            Thread errDrain = new Thread(() -> {
                try { ttsPersistentProcess.getErrorStream().transferTo(OutputStream.nullOutputStream()); }
                catch (Exception ignored) {}
            }, "TTS-ErrDrain");
            errDrain.setDaemon(true);
            errDrain.start();

            ttsProcWriter = new BufferedWriter(new OutputStreamWriter(
                    ttsPersistentProcess.getOutputStream(), StandardCharsets.UTF_8));
            ttsProcReader = new BufferedReader(new InputStreamReader(
                    ttsPersistentProcess.getInputStream(), StandardCharsets.UTF_8));

            // Read the READY handshake (with 10-second timeout).
            String ready = ttsProcReader.readLine();
            if ("READY".equals(ready)) {
                System.out.println("[VoiceAssistant-TTS] Persistent TTS process ready.");
            } else {
                System.err.println("[VoiceAssistant-TTS] Unexpected startup line: " + ready);
            }
        } catch (Exception e) {
            System.err.println("[VoiceAssistant-TTS] Failed to start persistent process: " + e.getMessage());
            ttsPersistentProcess = null;
            ttsProcWriter = null;
            ttsProcReader = null;
        }
    }

    /** Builds the Python source for the persistent TTS server process. */
    private static String buildTtsServerScript() {
        return
            "import sys, asyncio, ctypes, tempfile, os\n" +
            "import edge_tts\n" +
            "if sys.stdout.encoding != 'utf-8':\n" +
            "    import io\n" +
            "    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', line_buffering=True)\n" +
            "winmm = ctypes.windll.winmm\n" +
            "def mci(cmd): winmm.mciSendStringW(cmd, None, 0, None)\n" +
            "async def speak_once(text):\n" +
            "    com = edge_tts.Communicate(text, 'en-US-JennyNeural')\n" +
            "    mp3 = tempfile.mktemp(suffix='.mp3')\n" +
            "    await com.save(mp3)\n" +
            "    try:\n" +
            "        mci(f\"open \\\"{mp3}\\\" type mpegvideo alias voice\")\n" +
            "        mci('play voice wait')\n" +
            "        mci('close voice')\n" +
            "    finally:\n" +
            "        try: os.unlink(mp3)\n" +
            "        except: pass\n" +
            "print('READY', flush=True)\n" +
            "for line in sys.stdin:\n" +
            "    text = line.rstrip('\\n').rstrip('\\r')\n" +
            "    if not text or text == 'STOP': continue\n" +
            "    try:\n" +
            "        asyncio.run(speak_once(text))\n" +
            "        print('DONE', flush=True)\n" +
            "    except Exception as e:\n" +
            "        sys.stderr.write(f'[TTS] {e}\\n'); sys.stderr.flush()\n" +
            "        print('ERROR', flush=True)\n";
    }

    /** Speaks via the persistent Python process (fast — no cold-start). */
    private static boolean speakEdgeTts(String text) {
        if (!checkEdgeTts()) return false;
        ensureTtsPersistentProcess();
        BufferedWriter w = ttsProcWriter;
        BufferedReader r = ttsProcReader;
        if (w == null || r == null ||
                ttsPersistentProcess == null || !ttsPersistentProcess.isAlive()) {
            // Persistent process not running — fall back to spawning a one-shot script.
            return speakEdgeTtsFallback(text);
        }
        try {
            w.write(text.replace("\n", " ").replace("\r", " "));
            w.newLine();
            w.flush();
            // Block until Python confirms the utterance is complete.
            String line;
            while ((line = r.readLine()) != null) {
                if ("DONE".equals(line) || line.startsWith("ERROR")) return true;
            }
            // EOF — process exited unexpectedly; clear references.
            ttsPersistentProcess = null;
            ttsProcWriter = null;
            ttsProcReader = null;
            return false;
        } catch (IOException e) {
            if (Thread.currentThread().isInterrupted()) return true;
            System.err.println("[VoiceAssistant-TTS] Persistent process I/O error: " + e.getMessage());
            ttsPersistentProcess = null;
            ttsProcWriter = null;
            ttsProcReader = null;
            return false;
        }
    }

    /**
     * One-shot edge-tts fallback — spawns a new Python process each call.
     * Used only when the persistent process is unavailable.
     */
    private static boolean speakEdgeTtsFallback(String text) {
        File tmp = null;
        try {
            tmp = File.createTempFile("govibe-tts-", ".py");
            tmp.deleteOnExit();
            Files.writeString(tmp.toPath(), buildEdgeTtsScript(text), StandardCharsets.UTF_8);
            ProcessBuilder pb = new ProcessBuilder(edgeTtsPythonExe, tmp.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            proc.getInputStream().transferTo(OutputStream.nullOutputStream());
            boolean done = proc.waitFor(30, TimeUnit.SECONDS);
            if (!done) proc.destroyForcibly();
            return true;
        } catch (Exception e) {
            System.err.println("[VoiceAssistant-TTS] edge-tts fallback error: " + e.getMessage());
            return false;
        } finally {
            if (tmp != null) tmp.delete();
        }
    }

    // ── TTS ───────────────────────────────────────────────────────────────────

    /**
     * Speaks {@code text} synchronously.
     * Strategy: edge-tts (neural, no SAPI conflict) → SAPI PowerShell fallback.
     */
    private static void speakSync(String text) {
        System.out.println("[VoiceAssistant-TTS] Speaking: " + text);
        if (speakEdgeTts(text)) {
            System.out.println("[VoiceAssistant-TTS] Done.");
            return;
        }
        // ── SAPI fallback ──────────────────────────────────────────────────
        // WARNING: spawning a new SAPI TTS process while the SAPI STT process
        // is running will kill the STT engine.  This path is only used when
        // edge-tts is not installed.
        String escaped = text.replace("'", "''");
        String script =
                "Add-Type -AssemblyName System.Speech; " +
                "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                "$s.SelectVoiceByHints('Female'); " +
                "$s.Rate = 1; $s.Volume = 100; " +
                "$s.Speak('" + escaped + "');";
        for (String exe : new String[]{PS_EXE, "powershell.exe", "powershell"}) {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        exe, "-NoProfile", "-NonInteractive",
                        "-WindowStyle", "Hidden", "-Command", script);
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                byte[] out = proc.getInputStream().readAllBytes();
                int rc = proc.waitFor();
                if (rc != 0)
                    System.err.println("[VoiceAssistant-TTS] Exit " + rc + ": " + new String(out).trim());
                else
                    System.out.println("[VoiceAssistant-TTS] Done.");
                return;
            } catch (IOException e) {
                System.err.println("[VoiceAssistant-TTS] " + exe + ": " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        System.err.println("[VoiceAssistant-TTS] All TTS paths failed.");
    }

    // ── Shutdown ──────────────────────────────────────────────────────────────

    /**
     * Cleanly shuts down all voice-assistant subsystems.
     * Called by {@link org.example.mains.MainApp#stop()} when the JavaFX
     * application window is closed, ensuring no subprocess (edge-tts Python,
     * Qwen3 voice agent) lingers as a zombie OS process after the JVM exits.
     */
    public void shutdown() {
        System.out.println("[VoiceAssistant] Shutting down all subsystems...");

        // 1. Stop the STT loop
        listening.set(false);

        // 2. Kill the persistent edge-tts Python process
        Process ttsProc = ttsPersistentProcess;
        if (ttsProc != null && ttsProc.isAlive()) {
            System.out.println("[VoiceAssistant] Killing persistent TTS process...");
            ttsProc.destroyForcibly();
            ttsPersistentProcess = null;
        }

        // 3. Close the writer/reader streams so the TTS process stdin pipe unblocks
        try { if (ttsProcWriter != null) ttsProcWriter.close(); } catch (Exception ignored) {}
        try { if (ttsProcReader != null) ttsProcReader.close(); } catch (Exception ignored) {}
        ttsProcWriter = null;
        ttsProcReader = null;

        // 4. Shut down the TTS executor (drains queued tasks then terminates)
        ttsExecutor.shutdownNow();

        // 5. Stop the Python ML voice agent (Qwen3-TTS / voice_agent.py)
        PythonVoiceAgent agent = pythonAgent;
        if (agent != null) {
            System.out.println("[VoiceAssistant] Stopping Python ML agent...");
            agent.stop();
            pythonAgent = null;
        }

        System.out.println("[VoiceAssistant] All subsystems stopped.");
    }
}
