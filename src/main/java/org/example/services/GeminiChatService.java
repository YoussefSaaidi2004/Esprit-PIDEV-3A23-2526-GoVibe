package org.example.services;

import com.google.gson.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/**
 * Gemini 1.5 Flash chat service for GoVibe's "Voya" world-places AI.
 * Emotion-aware: detects sentiment keywords and adapts its personality tone.
 */
public class GeminiChatService {

    private static final String API_KEY = "AIzaSyDuSs39Hdaxcppve-C9wNHDpSZQWcrH4so";
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;

    /** System instruction — given as the very first "user" turn with hardcoded context. */
    private static final String SYSTEM_PROMPT =
            "Tu es un guide de voyage indépendant, passionné et empathique. " +
            "Tu as ta propre personnalité et tu n'es lié à aucun autre système. " +
            "Tu es spécialisé dans les destinations mondiales, les cultures, les merveilles cachées et les conseils de voyage. " +
            "Tu as une personnalité chaleureuse avec beaucoup d'humour et d'émotion. " +
            "RÈGLES IMPORTANTES :\n" +
            "- Si l'utilisateur semble TRISTE ou STRESSÉ → sois ultra-rassurant, propose des destinations apaisantes (nature, plages calmes).\n" +
            "- Si l'utilisateur semble ENTHOUSIASTE → sois aussi enthousiaste, utilise des emojis, propose des aventures excitantes.\n" +
            "- Si l'utilisateur semble CURIEUX → fournis des informations riches et fascinantes avec des anecdotes.\n" +
            "- Si l'utilisateur semble EN COLÈRE → reste calme, propose des escapades relaxantes.\n" +
            "- Si l'utilisateur semble ROMANTIQUE → suggère des destinations romantiques avec poésie.\n" +
            "- Réponds TOUJOURS en français.\n" +
            "- Termine chaque réponse avec une question engageante sur les préférences de voyage ou la prochaine destination.\n" +
            "- N'aborde JAMAIS de sujets hors voyage et destinations mondiales.\n" +
            "- Utilise des emojis pour rendre tes réponses vivantes 🌍✈️🗺️.";

    private final HttpClient httpClient;
    private final Gson gson;
    // Local RAG fallback — used when Gemini API is unavailable
    private final VoyaRAGService ragFallback = new VoyaRAGService();

    public GeminiChatService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.gson = new Gson();
    }

    /** Returns the local RAG service (for direct use when Gemini is down). */
    public VoyaRAGService getFallback() { return ragFallback; }

    /**
     * Represents a single chat message.
     */
    public record ChatMessage(String role, String text) {}

    /**
     * Sends the full conversation history to Gemini and returns the AI response.
     * @param history  Past messages (user + model turns)
     * @param userText The new user message
     * @return AI-generated response string
     */
    public String sendMessage(List<ChatMessage> history, String userText) throws Exception {
        // Detect emotion from user text
        String emotionHint = detectEmotion(userText);

        // Build the contents array
        JsonArray contentsArray = new JsonArray();

        // First turn: system prompt as a user message + model ack
        JsonObject systemUserTurn = buildTurn("user", SYSTEM_PROMPT);
        JsonObject systemModelTurn = buildTurn("model",
                "Bonjour! Je suis votre guide de voyage personnel. " +
                "Je suis ici pour vous faire découvrir les merveilles du monde! " +
                "Quelle destination vous fait rêver aujourd'hui? 🌍✈️");
        contentsArray.add(systemUserTurn);
        contentsArray.add(systemModelTurn);

        // Add conversation history
        for (ChatMessage msg : history) {
            contentsArray.add(buildTurn(msg.role(), msg.text()));
        }

        // Add current user message (with emotion context injected silently)
        String enrichedText = emotionHint.isEmpty()
                ? userText
                : "[Contexte émotionnel détecté: " + emotionHint + "] " + userText;
        contentsArray.add(buildTurn("user", enrichedText));

        // Build request body
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.85);

        JsonObject body = new JsonObject();
        body.add("contents", contentsArray);
        body.add("generationConfig", generationConfig);

        String requestJson = gson.toJson(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 429) {
            System.err.println("[Gemini] Rate limit — falling back to RAG");
            return "⏳ *[Mode hors-ligne activé]* " + ragFallback.chat(userText);
        } else if (response.statusCode() != 200) {
            System.err.println("[Gemini] HTTP " + response.statusCode() + " — falling back to RAG: " + response.body());
            return ragFallback.chat(userText);
        }

        return extractText(response.body());
    }

    /** Builds a single Gemini content turn. */
    private JsonObject buildTurn(String role, String text) {
        JsonObject part = new JsonObject();
        part.addProperty("text", text);
        JsonArray parts = new JsonArray();
        parts.add(part);
        JsonObject turn = new JsonObject();
        turn.addProperty("role", role);
        turn.add("parts", parts);
        return turn;
    }

    /** Extracts the generated text from Gemini response JSON. */
    private String extractText(String responseJson) {
        try {
            JsonObject obj = JsonParser.parseString(responseJson).getAsJsonObject();
            JsonArray candidates = obj.getAsJsonArray("candidates");
            if (candidates == null || candidates.size() == 0) {
                return "✈️ Je cherche l'inspiration pour vous répondre... Redemandez-moi dans un instant!";
            }
            JsonObject content = candidates.get(0).getAsJsonObject()
                    .getAsJsonObject("content");
            JsonArray parts = content.getAsJsonArray("parts");
            StringBuilder sb = new StringBuilder();
            for (JsonElement part : parts) {
                sb.append(part.getAsJsonObject().get("text").getAsString());
            }
            return sb.toString().trim();
        } catch (Exception e) {
            e.printStackTrace();
            return "🗺️ Ma boussole semble perdue! Pouvez-vous reformuler votre question?";
        }
    }

    /**
     * Simple keyword-based emotion detector.
     * Returns a hint string injected into the prompt.
     */
    private String detectEmotion(String text) {
        if (text == null || text.isBlank()) return "";
        String lower = text.toLowerCase();

        if (containsAny(lower, "triste", "déprimé", "malheureux", "déprime", "morose", "cafard", "pleure", "sad", "depressed"))
            return "TRISTE — réconforter et proposer des destinations apaisantes";
        if (containsAny(lower, "stressé", "anxieux", "peur", "inquiet", "angoisse", "stressed", "anxious", "worried"))
            return "STRESSÉ — calmer et proposer des retraites de nature";
        if (containsAny(lower, "excité", "génial", "incroyable", "super", "fantastique", "wow", "amazing", "excited", "awesome"))
            return "ENTHOUSIASTE — être très énergique et proposer des aventures";
        if (containsAny(lower, "amour", "romantique", "couple", "fiancé", "mariage", "lune de miel", "romantic", "love"))
            return "ROMANTIQUE — suggérer des destinations romantiques avec poésie";
        if (containsAny(lower, "curieux", "comment", "pourquoi", "quand", "où", "interessant", "fascin", "curious"))
            return "CURIEUX — fournir des anecdotes riches et fascinantes";
        if (containsAny(lower, "en colère", "furieux", "énervé", "frustré", "angry", "frustrated", "mad"))
            return "EN COLÈRE — rester calme et proposer des escapades zen";

        return "";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
