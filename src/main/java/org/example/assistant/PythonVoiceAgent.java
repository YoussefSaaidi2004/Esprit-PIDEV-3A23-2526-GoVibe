package org.example.assistant;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;

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
    private boolean         ready = false;
    private String          engineName = "none";

    // Persistent single-thread executor reused across all processText() calls.
    // Creating a new ExecutorService per call wastes one OS thread each time.
    private final ExecutorService responseReader = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "PythonAgent-reader");
        t.setDaemon(true);
        return t;
    });

    // Timeout waiting for Python to respond (ms).
    private static final int RESPONSE_TIMEOUT_MS = 8_000;

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
        System.out.println("[PythonAgent] Waiting for ML model to load...");
        Future<String> readyLineFuture = responseReader.submit(() -> stdout.readLine());
        String readyLine;
        try {
            readyLine = readyLineFuture.get(60, TimeUnit.SECONDS); // model download can take a while
        } catch (TimeoutException te) {
            readyLineFuture.cancel(true);
            throw new IOException("Python agent timed out during startup.");
        }
        if (readyLine == null) throw new IOException("Python agent did not emit ready signal.");
        engineName = parseStringField(readyLine, "engine");
        ready = true;
        System.out.println("[PythonAgent] Ready! ML engine: " + engineName);

        // Shutdown hook — cleanly kill Python process on JVM exit.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            responseReader.shutdownNow();
            if (pythonProcess != null && pythonProcess.isAlive())
                pythonProcess.destroyForcibly();
        }));
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public boolean isReady() { return ready; }
    public String  getEngineName() { return engineName; }

    /**
     * Sends {@code text} to the Python ML agent and returns a structured intent
     * response. Blocks until the Python process replies (up to 8 s).
     * Thread-safe.
     *
     * @param text raw recognised speech, e.g. "je veux réserver"
     * @return {@link AgentResponse} — never null (falls back on error)
     */
    public synchronized AgentResponse processText(String text) {
        if (!ready || pythonProcess == null || !pythonProcess.isAlive()) {
            System.err.println("[PythonAgent] Agent not ready — returning UNKNOWN.");
            return AgentResponse.unknown(text);
        }
        try {
            // Send JSON line to Python stdin.
            String jsonIn = "{\"text\":" + jsonString(text) + "}";
            stdin.write(jsonIn);
            stdin.newLine();
            stdin.flush();

            // Read response using the persistent executor (no thread creation per call).
            Future<String> future = responseReader.submit(() -> stdout.readLine());
            String jsonOut;
            try {
                jsonOut = future.get(RESPONSE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (TimeoutException te) {
                future.cancel(true);
                System.err.println("[PythonAgent] Response timeout for: \"" + text + "\"");
                // Mark as not-ready so the next call returns immediately.
                ready = false;
                return AgentResponse.unknown(text);
            }
            if (jsonOut == null) {
                System.err.println("[PythonAgent] No response from Python (process died?).");
                ready = false;
                return AgentResponse.unknown(text);
            }
            return parseResponse(jsonOut);

        } catch (Exception e) {
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
