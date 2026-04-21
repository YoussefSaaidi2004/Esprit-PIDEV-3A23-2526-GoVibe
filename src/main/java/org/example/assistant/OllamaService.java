package org.example.assistant;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.logging.Logger;

/**
 * Direct Java HTTP client for a local <a href="https://ollama.com/">Ollama</a>
 * LLM server running at {@code http://localhost:11434}.
 *
 * <p>This is used by {@link VoiceAssistantService} as a <em>secondary</em>
 * classification path — it is called when:
 * <ol>
 *   <li>The Python voice agent is not ready or has timed out, <em>and</em></li>
 *   <li>Simple keyword matching in {@link CommandRouter} also failed to
 *       produce a confident result.</li>
 * </ol>
 *
 * <p>If Ollama is not running the service degrades silently — every call
 * returns {@code null} and the caller falls back gracefully.
 *
 * <h3>Setup (one-time)</h3>
 * <pre>
 *   # Download Ollama: https://ollama.com/download
 *   ollama run llama3.2   # pulls ~2 GB model once, then serves it locally
 * </pre>
 *
 * <h3>Protocol</h3>
 * Sends a system prompt describing GoVibe's actions, then the user's speech.
 * Forces JSON output via Ollama's {@code "format":"json"} flag.
 */
public class OllamaService {

    private static final Logger LOG = Logger.getLogger(OllamaService.class.getName());

    // ── Configuration ─────────────────────────────────────────────────────────
    /** Ollama REST base URL — change only if you run Ollama on a different port. */
    public static final String OLLAMA_BASE = "http://localhost:11434";

    /** LLM model to use. Must be pulled with {@code ollama pull <model>} first. */
    public static final String MODEL = "llama3.2";

    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    // ── System prompt ──────────────────────────────────────────────────────────
    private static final String SYSTEM_PROMPT =
            "You are the voice assistant for GoVibe, a travel booking desktop app. " +
            "The user speaks French or English. Their speech has already been transcribed " +
            "by Vosk (offline STT) so it may have minor transcription errors. " +
            "Classify the intent into exactly ONE of these action names:\n" +
            "  BOOK            - open flight booking form\n" +
            "  MES RESERVATIONS - show the user's reservations\n" +
            "  RECHERCHER      - search available flights\n" +
            "  ANNULER         - cancel / go back / close\n" +
            "  PAYER           - confirm payment\n" +
            "  AIDE            - list voice commands\n" +
            "  DECRIRE         - describe current screen\n" +
            "  DECONNEXION     - log out\n" +
            "  LOGIN           - log in\n" +
            "  SIGNUP          - create account\n" +
            "  FOCUS_EMAIL     - focus email field\n" +
            "  FOCUS_PASSWORD  - focus password field\n" +
            "  NONE            - greeting only, no app action\n" +
            "  UNKNOWN         - cannot determine intent\n\n" +
            "Also extract, if mentioned:\n" +
            "  destination: city name the user wants to travel to (or null)\n" +
            "  date: travel date in YYYY-MM-DD format (or null)\n\n" +
            "Reply ONLY with minified JSON — no markdown, no code fences:\n" +
            "{\"action\":\"BOOK\",\"confidence\":0.95,\"destination\":\"Paris\"," +
            "\"date\":null,\"response\":\"Je vais chercher des vols pour Paris.\"}";

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static final OllamaService INSTANCE = new OllamaService();
    public  static OllamaService getInstance() { return INSTANCE; }

    // ── State ─────────────────────────────────────────────────────────────────
    private final HttpClient http;
    private volatile Boolean available = null;   // null = not yet checked

    private OllamaService() {
        http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if Ollama is reachable at {@link #OLLAMA_BASE}.
     * Result is cached after the first call.
     */
    public boolean isAvailable() {
        if (available != null) return available;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_BASE + "/"))
                    .timeout(Duration.ofSeconds(2))
                    .GET().build();
            int status = http.send(req, HttpResponse.BodyHandlers.discarding()).statusCode();
            available = (status == 200);
            LOG.info("[Ollama] Server reachable=" + available + "  model=" + MODEL);
        } catch (Exception e) {
            available = false;
            LOG.info("[Ollama] Server not reachable: " + e.getMessage());
        }
        return available;
    }

    /**
     * Resets the availability cache — call this if Ollama was started after the app.
     */
    public void resetAvailabilityCache() { available = null; }

    /**
     * Sends {@code text} to the local Ollama LLM and returns a structured
     * {@link PythonVoiceAgent.AgentResponse}, or {@code null} if Ollama is
     * unavailable or the call fails.
     *
     * @param text recognised speech text (may be noisy / mis-transcribed)
     */
    public PythonVoiceAgent.AgentResponse classify(String text) {
        if (!isAvailable()) return null;
        try {
            String body = buildChatRequest(text);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_BASE + "/api/chat"))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            String raw = http.send(req, HttpResponse.BodyHandlers.ofString()).body();
            return parseOllamaResponse(raw, text);

        } catch (Exception e) {
            LOG.warning("[Ollama] classify failed: " + e.getMessage());
            return null;
        }
    }

    // ── Request builder ───────────────────────────────────────────────────────
    private String buildChatRequest(String userText) {
        // Escape the user text for JSON embedding.
        String escaped = userText
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        String sysEscaped = SYSTEM_PROMPT
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        return "{\"model\":\"" + MODEL + "\",\"stream\":false,\"format\":\"json\"," +
               "\"messages\":[" +
               "{\"role\":\"system\",\"content\":\"" + sysEscaped + "\"}," +
               "{\"role\":\"user\",\"content\":\"" + escaped + "\"}" +
               "]}";
    }

    // ── Response parser ───────────────────────────────────────────────────────
    /**
     * Parses the Ollama /api/chat response and extracts the embedded JSON
     * produced by the LLM.
     */
    private PythonVoiceAgent.AgentResponse parseOllamaResponse(String raw, String originalText) {
        try {
            // Ollama /api/chat → {"message":{"role":"assistant","content":"{...}"},...}
            String contentJson = extractField(raw, "content");
            if (contentJson == null || contentJson.isBlank())
                return null;

            String action      = extractField(contentJson, "action");
            String dest        = extractField(contentJson, "destination");
            String date        = extractField(contentJson, "date");
            String response    = extractField(contentJson, "response");
            String confStr     = extractRaw(contentJson, "confidence");
            double confidence  = 0.0;
            try { confidence = Double.parseDouble(confStr); } catch (Exception ignored) {}

            if (action == null || "null".equalsIgnoreCase(action)) action = "UNKNOWN";
            if ("null".equalsIgnoreCase(dest))  dest  = null;
            if ("null".equalsIgnoreCase(date))  date  = null;
            if (response == null) response = "";

            // Derive intent from action for logging / downstream use.
            String intent = actionToIntent(action);

            LOG.info("[Ollama] intent=" + intent + "  action=" + action
                    + "  conf=" + String.format("%.2f", confidence)
                    + "  dest=" + dest + "  date=" + date);

            return new PythonVoiceAgent.AgentResponse(
                    intent, response, action, confidence, dest, date, "ollama-java", 1);

        } catch (Exception e) {
            LOG.warning("[Ollama] Parse error: " + e.getMessage() + "  raw=" + raw);
            return null;
        }
    }

    private static String actionToIntent(String action) {
        if (action == null) return "UNKNOWN";
        return switch (action.toUpperCase().trim()) {
            case "BOOK"             -> "BOOK";
            case "MES RESERVATIONS" -> "VIEW_BOOKINGS";
            case "RECHERCHER"       -> "SEARCH";
            case "ANNULER"          -> "CANCEL";
            case "PAYER"            -> "PAY";
            case "AIDE"             -> "HELP";
            case "DECRIRE"          -> "DESCRIBE";
            case "DECONNEXION"      -> "LOGOUT";
            case "LOGIN"            -> "LOGIN";
            case "SIGNUP"           -> "SIGNUP";
            case "FOCUS_EMAIL"      -> "FOCUS_EMAIL";
            case "FOCUS_PASSWORD"   -> "FOCUS_PASSWORD";
            case "NONE"             -> "GREET";
            default                 -> "UNKNOWN";
        };
    }

    // ── Minimal JSON extraction ───────────────────────────────────────────────
    private static String extractField(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        idx += search.length();
        while (idx < json.length() && Character.isWhitespace(json.charAt(idx))) idx++;
        if (idx >= json.length()) return null;

        char first = json.charAt(idx);
        // String value
        if (first == '"') {
            idx++;
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
        // Nested object / array — return as substring
        if (first == '{' || first == '[') {
            int depth = 0;
            int start = idx;
            while (idx < json.length()) {
                char c = json.charAt(idx);
                if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') { depth--; if (depth == 0) { idx++; break; } }
                idx++;
            }
            return json.substring(start, idx);
        }
        return null;
    }

    private static String extractRaw(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return "0";
        idx += search.length();
        while (idx < json.length() && Character.isWhitespace(json.charAt(idx))) idx++;
        int start = idx;
        while (idx < json.length() && ",}".indexOf(json.charAt(idx)) < 0) idx++;
        return json.substring(start, idx).trim();
    }
}
