package org.example.assistant;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Manages a persistent Python subprocess running {@code voice_agent.py}.
 *
 * <p>The agent uses ML (scikit-learn TF-IDF, or sentence-transformers if
 * installed) to classify user speech into GoVibe intents and generate
 * natural-language responses.
 *
 * <p>Protocol (line-delimited JSON):
 * <pre>
 *   Java  → Python stdin :  {"text":"book a flight"}
 *   Python → Java stdout :  {"intent":"BOOK","response":"Je vais ouvrir...","action":"BOOK","confidence":0.92}
 * </pre>
 *
 * <p>On startup the Python process writes a ready-signal:
 * <pre>  {"status":"ready","engine":"sklearn-tfidf"}</pre>
 *
 * <p>Use {@link #processText(String)} which is synchronised and safe to call
 * from any thread.
 */
public class PythonVoiceAgent {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static PythonVoiceAgent instance;

    public static synchronized PythonVoiceAgent getInstance() {
        if (instance == null) instance = new PythonVoiceAgent();
        return instance;
    }

    // ── AgentResponse ─────────────────────────────────────────────────────────
    /**
     * Structured response from the Python ML agent.
     */
    public static class AgentResponse {
        public final String intent;       // e.g. "BOOK"
        public final String response;     // TTS text
        public final String action;       // CommandRouter keyword e.g. "BOOK"
        public final double confidence;   // 0..1
        /** Ollama-extracted travel destination (city), or null if not mentioned. */
        public final String destination;
        /** Ollama-extracted travel date (YYYY-MM-DD string), or null if not mentioned. */
        public final String date;
        /** Which tier produced the result: "sentence-transformers", "ollama", "sklearn-tfidf". */
        public final String engine;
        /** Number of passengers extracted by LLM function calling (default 1). */
        public final int passengers;

        AgentResponse(String intent, String response, String action, double confidence,
                      String destination, String date, String engine, int passengers) {
            this.intent      = intent;
            this.response    = response;
            this.action      = action;
            this.confidence  = confidence;
            this.destination = destination;
            this.date        = date;
            this.engine      = engine != null ? engine : "unknown";
            this.passengers  = passengers > 0 ? passengers : 1;
        }

        /** A fallback when the agent fails to respond. */
        static AgentResponse unknown(String originalText) {
            return new AgentResponse(
                    "UNKNOWN",
                    "I did not understand. Say Help for the list of commands.",
                    "UNKNOWN",
                    0.0, null, null, "error", 1
            );
        }

        /** True if the LLM extracted a travel destination from speech. */
        public boolean hasDestination() { return destination != null && !destination.isBlank(); }
        /** True if the LLM extracted a travel date from speech. */
        public boolean hasDate()        { return date != null && !date.isBlank(); }
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private Process         pythonProcess;
    private BufferedWriter  stdin;
    private BufferedReader  stdout;
    private volatile boolean ready = false;
    private String          engineName = "none";

    // ── Speak queue — messages to deliver once Vivian is ready ─────────────────
    // vivianSpeak() / sendSpeak() called before the Python process finishes
    // loading are stored here and flushed automatically after ready=true.
    private final ConcurrentLinkedQueue<String> pendingSpeaks = new ConcurrentLinkedQueue<>();

    // ── Qwen3-TTS status callback ─────────────────────────────────────────────
    // Invoked from the reader thread whenever Python sends {"type":"tts_status",...}.
    // VoiceAssistantService wires this to control micDiscardUntilMs.
    private volatile Consumer<Boolean> ttsStatusCallback = speaking -> {};  // no-op default

    // ── Resume-wake-word callback ─────────────────────────────────────────────
    // Python sends {"type":"resume_wake_word"} after the post-logout TTS sequence
    // ends, signalling that the wake-word detector should be re-enabled.
    private volatile Runnable resumeWakeWordCallback = () -> {};  // no-op default

    // ── Weather data callback ─────────────────────────────────────────────────
    // Python sends {"type":"weather_show",...} before a WEATHER intent response.
    // VoiceAssistantService wires this to pass data into CommandRouter.
    /**
     * Structured weather data parsed from Python's weather_show message.
     */
    public static class WeatherData {
        public final String city, temp, condition, humidity, wind, feel;
        public WeatherData(String city, String temp, String condition,
                           String humidity, String wind, String feel) {
            this.city      = city      != null ? city      : "Unknown";
            this.temp      = temp      != null ? temp      : "--";
            this.condition = condition != null ? condition : "--";
            this.humidity  = humidity  != null ? humidity  : "--";
            this.wind      = wind      != null ? wind      : "--";
            this.feel      = feel      != null ? feel      : "--";
        }
    }
    private volatile Consumer<WeatherData> weatherCallback = data -> {};

    /** Lock protecting all stdin writes (multiple threads write: Vosk, VoiceAssistantService). */
    private final Object stdinLock = new Object();

    /** Incremented for every processText() call; Python echoes it back for reliable matching. */
    private final AtomicInteger seqCounter = new AtomicInteger(0);

    /**
     * Outstanding processText() calls waiting for their Python response.
     * Key = seq number; value = future completed by the persistent reader thread.
     * Stale responses from a previous timeout are simply discarded (no matching future).
     */
    private final ConcurrentHashMap<Integer, CompletableFuture<String>> pendingResponses
            = new ConcurrentHashMap<>();

    // Timeout waiting for Python to respond (ms).
    // Stale responses from previous timeouts are handled by seq matching and discarded,
    // so a generous timeout is safe — it only applies to genuinely slow responses.
    // 25 s to accommodate slow Vivian (Qwen3-TTS) generation on CPU.
    private static final int RESPONSE_TIMEOUT_MS = 25_000;

    // ─────────────────────────────────────────────────────────────────────────
    private PythonVoiceAgent() {
        try {
            start();
        } catch (Exception e) {
            System.err.println("[PythonAgent] Failed to start: " + e.getMessage());
        }
    }

    // ── Start subprocess ──────────────────────────────────────────────────────
    private void start() throws Exception {
        File script = extractScript();
        String python = findPython();
        System.out.println("[PythonAgent] Python: " + python);
        System.out.println("[PythonAgent] Script: " + script.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(python, "-u", script.getAbsolutePath());
        pb.redirectErrorStream(false);
        pb.environment().put("PYTHONIOENCODING", "utf-8");
        pb.environment().put("PYTHONUNBUFFERED", "1");
        pythonProcess = pb.start();

        stdin  = new BufferedWriter(new OutputStreamWriter(
                pythonProcess.getOutputStream(), StandardCharsets.UTF_8));
        stdout = new BufferedReader(new InputStreamReader(
                pythonProcess.getInputStream(), StandardCharsets.UTF_8));

        // Drain Python stderr to Java stderr in a daemon thread.
        BufferedReader pyErr = new BufferedReader(new InputStreamReader(
                pythonProcess.getErrorStream(), StandardCharsets.UTF_8));
        Thread errDrain = new Thread(() -> {
            try {
                String l;
                while ((l = pyErr.readLine()) != null)
                    System.err.println("[PythonAgent-py] " + l);
            } catch (IOException ignored) {}
        }, "PythonAgent-stderr");
        errDrain.setDaemon(true);
        errDrain.start();

        // Wait for the ready signal with a timeout.
        // Use a short-lived dedicated executor (the persistent reader thread
        // must not start until after this one-time bootstrap read is done).
        System.out.println("[PythonAgent] Waiting for ML model to load...");
        // Skip any stdout noise (flash-attn warnings, separator lines, etc.) printed
        // before the real {"status":"ready"} line.  Loop until we find it.
        ExecutorService startupReader = Executors.newSingleThreadExecutor();
        Future<String> readyLineFuture = startupReader.submit(() -> {
            String l;
            while ((l = stdout.readLine()) != null) {
                if (l.contains("\"status\"") && l.contains("\"ready\"")) return l;
                System.err.println("[PythonAgent-boot] skipping: " + l.substring(0, Math.min(l.length(), 80)));
            }
            return null;
        });
        String readyLine;
        try {
            readyLine = readyLineFuture.get(120, TimeUnit.SECONDS); // model download can take a while
        } catch (TimeoutException te) {
            readyLineFuture.cancel(true);
            startupReader.shutdownNow();
            throw new IOException("Python agent timed out during startup.");
        }
        startupReader.shutdown();
        if (readyLine == null) throw new IOException("Python agent did not emit ready signal.");
        engineName = parseStringField(readyLine, "engine");
        if (engineName == null) engineName = parseStringField(readyLine.replace(": ", ":"), "engine");

        // Set ready=true and drain queued speaks, then launch the persistent reader.
        synchronized (this) {
            ready = true;
            String queued;
            while ((queued = pendingSpeaks.poll()) != null) {
                try {
                    synchronized (stdinLock) {
                        stdin.write("{\"type\":\"speak\",\"text\":" + jsonString(queued) + "}");
                        stdin.newLine();
                        stdin.flush();
                    }
                    System.out.println("[PythonAgent] Flushed queued speak: " + queued);
                } catch (IOException e) {
                    System.err.println("[PythonAgent] Queue flush error: " + e.getMessage());
                }
            }
        }
        System.out.println("[PythonAgent] Ready! ML engine: " + engineName);

        // Launch the persistent stdout reader thread.
        Thread readerThread = new Thread(this::runReaderLoop, "PythonAgent-reader");
        readerThread.setDaemon(true);
        readerThread.start();

        // Shutdown hook — cleanly kill Python process on JVM exit.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (pythonProcess != null && pythonProcess.isAlive())
                pythonProcess.destroyForcibly();
        }));
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public boolean isReady() { return ready; }
    public String  getEngineName() { return engineName; }

    /**
     * Registers a callback for Qwen3-TTS start/end events emitted by Python.
     * The callback is called with {@code true} when Python's TTS starts playing
     * and {@code false} when it finishes.  VoiceAssistantService uses this to
     * set/clear the microphone discard window (echo prevention).
     */
    public void setTtsStatusListener(Consumer<Boolean> listener) {
        this.ttsStatusCallback = (listener != null) ? listener : speaking -> {};
    }

    /**
     * Registers a callback for weather_show events emitted by Python just before
     * a WEATHER intent response. The callback receives a {@link WeatherData} object
     * with city, temperature, condition, humidity, wind speed, and feels-like temp.
     */
    public void setWeatherListener(Consumer<WeatherData> listener) {
        this.weatherCallback = (listener != null) ? listener : data -> {};
    }

    /**
     * Registers a callback invoked when Python's post-logout personality sequence
     * finishes and the wake-word detector should be re-armed.
     * Python signals this with {@code {"type":"resume_wake_word"}}.
     */
    public void setResumeWakeWordListener(Runnable listener) {
        this.resumeWakeWordCallback = (listener != null) ? listener : () -> {};
    }

    /**
     * Sends user-context information to the Python state machine so it can
     * personalise wake responses (returning-user recognition vs. new-user apology).
     * Fire-and-forget — Python does not send a response for this message.
     *
     * <p>Call this at startup with the current login state and after every
     * login / logout event.
     */
    public void sendUserContext(boolean loggedIn, String name) {
        if (!ready || pythonProcess == null || !pythonProcess.isAlive()) return;
        String safeName = (name != null)
                ? name.replace("\\", "\\\\").replace("\"", "\\\"")
                : "";
        String payload = "{\"type\":\"user_context\",\"logged_in\":"
                + loggedIn + ",\"name\":\"" + safeName + "\"}";
        try {
            synchronized (stdinLock) {
                stdin.write(payload);
                stdin.newLine();
                stdin.flush();
            }
        } catch (IOException e) {
            System.err.println("[PythonAgent] sendUserContext error: " + e.getMessage());
        }
    }

    /**
     * Sends a gesture event to the Python agent so it can respond contextually.
     * For example THUMBS_UP during gesture confirmation → "Hold it!" feedback.
     * Fire-and-forget — Python does not reply with a response message.
     *
     * @param gestureName the gesture enum name, e.g. "THUMBS_UP", "OPEN_PALM"
     */
    public void sendGestureEvent(String gestureName) {
        if (!ready || pythonProcess == null || !pythonProcess.isAlive()) return;
        if (gestureName == null || gestureName.isBlank()) return;
        String safeName = gestureName.replace("\"", "\\\"");
        String payload = "{\"type\":\"gesture_event\",\"gesture\":\"" + safeName + "\"}";
        try {
            synchronized (stdinLock) {
                stdin.write(payload);
                stdin.newLine();
                stdin.flush();
            }
        } catch (IOException e) {
            System.err.println("[PythonAgent] sendGestureEvent error: " + e.getMessage());
        }
    }

    /**
     * Sends the GoVibe database snapshot (activities, cars, hotels) to the Python
     * voice agent so Go can answer data-driven questions and include real inventory
     * details in DeepSeek / Ollama prompts.
     *
     * <p>Fire-and-forget — Python does not send a response for this message.
     * Build the payload with {@link VoiceDataService#buildDbContextJson()}.
     *
     * @param dbContextJson pre-built JSON line from VoiceDataService
     */
    public void sendDbContext(String dbContextJson) {
        if (!ready || pythonProcess == null || !pythonProcess.isAlive()) return;
        if (dbContextJson == null || dbContextJson.isBlank()) return;
        try {
            synchronized (stdinLock) {
                stdin.write(dbContextJson);
                stdin.newLine();
                stdin.flush();
            }
            System.out.println("[PythonAgent] DB context sent (" + dbContextJson.length() + " chars).");
        } catch (IOException e) {
            System.err.println("[PythonAgent] sendDbContext error: " + e.getMessage());
        }
    }

    /**
     * Notifies the Python agent that the wake word was detected, forwarding the
     * full STT text so Python can detect embedded commands (e.g. "hi go logout").
     * Python (Qwen3-TTS) owns all audio output for the wake sequence — Java does
     * not speak anything here.
     *
     * @param fullText the complete STT utterance that contained the wake trigger
     */
    public void signalWakeWord(String fullText) {
        if (!ready || pythonProcess == null || !pythonProcess.isAlive()) return;
        String safeText = (fullText != null)
                ? fullText.replace("\\", "\\\\").replace("\"", "\\\"")
                : "";
        String payload = "{\"type\":\"wake_word\",\"text\":\"" + safeText + "\"}";
        try {
            synchronized (stdinLock) {
                stdin.write(payload);
                stdin.newLine();
                stdin.flush();
            }
            // Python (Qwen3-TTS) handles TTS for the wake sequence.
            // The persistent reader thread consumes the GREET response; since it has
            // no seq field, it is discarded automatically — no Java action needed.
        } catch (IOException e) {
            System.err.println("[PythonAgent] signalWakeWord error: " + e.getMessage());
        }
    }

    /** Convenience overload — signals wake with no embedded text. */
    public void signalWakeWord() { signalWakeWord(""); }

    // ── tts_status / resume_wake_word helpers ────────────────────────────────

    /** Returns true when {@code line} is a Qwen3-TTS status notification from Python.
     *  Python's json.dumps uses ": " (with space) so we match just the value substring. */
    private static boolean isTtsStatusLine(String line) {
        return line != null && line.contains("tts_status");
    }

    /** Returns true when {@code line} is a resume_wake_word signal from Python. */
    private static boolean isResumeWakeWordLine(String line) {
        return line != null && line.contains("resume_wake_word");
    }

    /** Returns true when {@code line} is a real-time weather data message from Python. */
    private static boolean isWeatherShowLine(String line) {
        return line != null && line.contains("weather_show");
    }

    /**
     * Parses city/temp/condition/humidity/wind/feel from a weather_show line
     * and invokes the registered {@link #weatherCallback}.
     */
    private void dispatchWeatherShow(String line) {
        String city      = parseStringField(line, "city");
        String temp      = parseStringField(line, "temp");
        String condition = parseStringField(line, "condition");
        String humidity  = parseStringField(line, "humidity");
        String wind      = parseStringField(line, "wind");
        String feel      = parseStringField(line, "feel");
        Consumer<WeatherData> cb = weatherCallback;
        if (cb != null) cb.accept(new WeatherData(city, temp, condition, humidity, wind, feel));
        System.out.println("[PythonAgent-Weather] Weather received for " + city + ": " + temp + " " + condition);
    }

    /**
     * Parses the {@code speaking} boolean from a tts_status line and invokes
     * the registered {@link #ttsStatusCallback}.
     */
    private void dispatchTtsStatus(String line) {
        // json.dumps may produce "speaking": true (with space) or "speaking":true
        boolean speaking = line.contains("\"speaking\": true") || line.contains("\"speaking\":true");
        Consumer<Boolean> cb = ttsStatusCallback;
        if (cb != null) cb.accept(speaking);
        System.out.println("[PythonAgent-TTS] tts_status speaking=" + speaking);
    }

    /** Invokes the registered {@link #resumeWakeWordCallback} on the current thread. */
    private void dispatchResumeWakeWord() {
        Runnable cb = resumeWakeWordCallback;
        if (cb != null) cb.run();
        System.out.println("[PythonAgent] resume_wake_word received — re-arming wake detector.");
    }

    /**
     * Persistent stdout reader — runs in a daemon thread from startup to JVM exit.
     * Dispatches tts_status and resume_wake_word signals immediately;
     * completes the registered CompletableFuture for each intent response
     * by matching the seq field Python echoes back.
     * Responses without a seq (wake_word flow, user_context events) are discarded
     * since Python-TTS already spoke the audio and Java needs no further action.
     */
    private void runReaderLoop() {
        try {
            String line;
            while ((line = stdout.readLine()) != null) {
                if (isTtsStatusLine(line))    { dispatchTtsStatus(line);    continue; }
                if (isResumeWakeWordLine(line)) { dispatchResumeWakeWord(); continue; }
                if (isWeatherShowLine(line))   { dispatchWeatherShow(line); continue; }
                int seq = parseSeq(line);
                if (seq >= 0) {
                    CompletableFuture<String> f = pendingResponses.remove(seq);
                    if (f != null) f.complete(line);
                    else System.err.println("[PythonAgent] No pending call for seq=" + seq + " — discarding.");
                } else {
                    System.err.println("[PythonAgent] Discarding unseq'd: "
                            + line.substring(0, Math.min(line.length(), 120)));
                }
            }
        } catch (IOException e) {
            System.err.println("[PythonAgent] Reader loop error: " + e.getMessage());
        }
        System.err.println("[PythonAgent] Reader loop ended.");
    }

    /**
     * Fire-and-forget: instructs Vivian (Python TTS worker) to speak {@code text}.
     * Returns immediately — audio plays in Python's background TTS thread.
     * Thread-safe: synchronized on the agent instance. No-op if not ready.
     */
    public void sendSpeak(String text) {
        if (text == null || text.isBlank()) return;
        if (!ready || pythonProcess == null || !pythonProcess.isAlive()) {
            pendingSpeaks.add(text);
            System.out.println("[PythonAgent] Queued speak (Vivian loading): " + text);
            return;
        }
        try {
            synchronized (stdinLock) {
                stdin.write("{\"type\":\"speak\",\"text\":" + jsonString(text) + "}");
                stdin.newLine();
                stdin.flush();
            }
        } catch (IOException e) {
            System.err.println("[PythonAgent] sendSpeak error: " + e.getMessage());
        }
    }

    /**
     * Fire-and-forget: instructs Echo (edge-tts, ~200 ms) to speak {@code text}.
     * Uses the speak_fast Python path — the mic is muted for ~200 ms only,
     * vs 30–60 s for Qwen3-TTS on CPU. Use for all short action confirmations.
     */
    public void sendSpeakFast(String text) {
        if (text == null || text.isBlank()) return;
        if (!ready || pythonProcess == null || !pythonProcess.isAlive()) return;
        try {
            synchronized (stdinLock) {
                stdin.write("{\"type\":\"speak_fast\",\"text\":" + jsonString(text) + "}");
                stdin.newLine();
                stdin.flush();
            }
        } catch (IOException e) {
            System.err.println("[PythonAgent] sendSpeakFast error: " + e.getMessage());
        }
    }

    /**
     * Sends {@code text} to Python, waits for its intent response (up to
     * {@value RESPONSE_TIMEOUT_MS} ms), and returns a structured result.
     * Stale responses from previous timeouts are discarded by seq matching.
     * Thread-safe: synchronized to prevent concurrent calls.
     *
     * @param text raw recognised speech
     * @return {@link AgentResponse} — never null
     */
    public synchronized AgentResponse processText(String text) {
        if (!ready || pythonProcess == null || !pythonProcess.isAlive()) {
            System.err.println("[PythonAgent] Agent not ready — returning UNKNOWN.");
            return AgentResponse.unknown(text);
        }
        int seq = seqCounter.incrementAndGet();
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingResponses.put(seq, future);
        try {
            String jsonIn = "{\"text\":" + jsonString(text) + ",\"seq\":" + seq + "}";
            synchronized (stdinLock) {
                stdin.write(jsonIn);
                stdin.newLine();
                stdin.flush();
            }
            String jsonOut;
            try {
                jsonOut = future.get(RESPONSE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (TimeoutException te) {
                pendingResponses.remove(seq);
                System.err.println("[PythonAgent] Timeout (seq=" + seq + ") for: \"" + text + "\"");
                // Do NOT set ready=false: the reader thread continues; any late
                // response for this seq will simply find no pending future and be discarded.
                return AgentResponse.unknown(text);
            } catch (InterruptedException ie) {
                pendingResponses.remove(seq);
                Thread.currentThread().interrupt();
                return AgentResponse.unknown(text);
            }
            if (jsonOut == null) return AgentResponse.unknown(text);
            return parseResponse(jsonOut);
        } catch (Exception e) {
            pendingResponses.remove(seq);
            System.err.println("[PythonAgent] processText error: " + e.getMessage());
            return AgentResponse.unknown(text);
        }
    }

    // ── Script extraction ─────────────────────────────────────────────────────
    /**
     * Extracts {@code scripts/voice_agent.py} from classpath resources to a
     * temp file so it can be given to the Python interpreter as a file path.
     */
    private static File extractScript() throws IOException {
        InputStream is = PythonVoiceAgent.class
                .getResourceAsStream("/scripts/voice_agent.py");
        if (is == null)
            throw new FileNotFoundException("voice_agent.py not found in classpath.");
        File tmp = File.createTempFile("govibe-voice-agent-", ".py");
        tmp.deleteOnExit();
        Files.copy(is, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
        is.close();

        // Also extract govibe_knowledge.md to the same temp directory so that
        // __file__-relative lookup inside voice_agent.py finds it correctly.
        InputStream kbIs = PythonVoiceAgent.class
                .getResourceAsStream("/scripts/govibe_knowledge.md");
        if (kbIs != null) {
            File kbFile = new File(tmp.getParentFile(), "govibe_knowledge.md");
            kbFile.deleteOnExit();
            Files.copy(kbIs, kbFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            kbIs.close();
        }
        // Extract behavior_tracker.py so the unsupervised learning module is importable.
        InputStream btIs = PythonVoiceAgent.class
                .getResourceAsStream("/scripts/behavior_tracker.py");
        if (btIs != null) {
            File btFile = new File(tmp.getParentFile(), "behavior_tracker.py");
            btFile.deleteOnExit();
            Files.copy(btIs, btFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            btIs.close();
        }
        // Extract seed learned_phrases.json (initial paraphrase database for tracker).
        InputStream lpIs = PythonVoiceAgent.class
                .getResourceAsStream("/scripts/learned_phrases.json");
        if (lpIs != null) {
            File lpFile = new File(tmp.getParentFile(), "learned_phrases.json");
            lpFile.deleteOnExit();
            Files.copy(lpIs, lpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            lpIs.close();
        }
        return tmp;
    }

    // ── Python executable discovery ───────────────────────────────────────────
    private static String findPython() {
        String[] candidates = {"python", "python3", "py"};
        for (String candidate : candidates) {
            try {
                Process test = new ProcessBuilder(candidate, "--version")
                        .redirectErrorStream(true).start();
                String ver = new String(test.getInputStream().readAllBytes()).trim();
                test.waitFor(3, TimeUnit.SECONDS);
                if (ver.startsWith("Python 3")) {
                    System.out.println("[PythonAgent] Found: " + candidate + " → " + ver);
                    return candidate;
                }
            } catch (Exception ignored) {}
        }
        throw new RuntimeException(
                "Python 3 not found on PATH. Install Python 3 to enable ML voice responses.");
    }

    // ── Minimal JSON helpers (avoids depending on Gson here) ──────────────────
    private static AgentResponse parseResponse(String json) {
        String intent     = parseStringField(json, "intent");
        String response   = parseStringField(json, "response");
        String action     = parseStringField(json, "action");
        String destination= parseStringField(json, "destination");
        String date       = parseStringField(json, "date");
        String engine     = parseStringField(json, "engine");
        String confStr    = parseRawField(json, "confidence");
        String paxStr     = parseRawField(json, "passengers");
        double confidence = 0.0;
        int    passengers = 1;
        try { confidence = Double.parseDouble(confStr); } catch (Exception ignored) {}
        try { passengers = Integer.parseInt(paxStr.trim()); } catch (Exception ignored) {}

        if (intent == null)   intent   = "UNKNOWN";
        if (response == null) response = "";
        if (action == null)   action   = "UNKNOWN";
        // "null" literal from JSON → treat as absent
        if ("null".equalsIgnoreCase(destination)) destination = null;
        if ("null".equalsIgnoreCase(date))        date        = null;
        return new AgentResponse(intent, response, action, confidence, destination, date, engine, passengers);
    }

    /** Extracts the string value of a JSON key, e.g.  "key":"value" → value. */
    static String parseStringField(String json, String key) {
        if (json == null) return null;
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        idx += search.length();
        while (idx < json.length() && Character.isWhitespace(json.charAt(idx))) idx++;
        if (idx >= json.length() || json.charAt(idx) != '"') return null;
        idx++; // skip opening quote
        StringBuilder sb = new StringBuilder();
        while (idx < json.length()) {
            char c = json.charAt(idx++);
            if (c == '\\' && idx < json.length()) {
                char esc = json.charAt(idx++);
                switch (esc) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    default  -> sb.append(esc);
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Extracts a raw (unquoted) JSON field value, e.g. "confidence":0.92 → "0.92". */
    private static String parseRawField(String json, String key) {
        if (json == null) return "0";
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return "0";
        idx += search.length();
        while (idx < json.length() && Character.isWhitespace(json.charAt(idx))) idx++;
        int start = idx;
        while (idx < json.length() && ",}".indexOf(json.charAt(idx)) < 0) idx++;
        return json.substring(start, idx).trim();
    }

    /** Extracts the {@code seq} integer field echoed by Python; returns -1 if absent. */
    private static int parseSeq(String line) {
        if (line == null || !line.contains("\"seq\":")) return -1;
        String raw = parseRawField(line, "seq");
        try { return Integer.parseInt(raw.trim()); }
        catch (NumberFormatException e) { return -1; }
    }

    /** Minimal JSON string escaper. */
    private static String jsonString(String value) {
        if (value == null) return "\"\"";
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }
}
