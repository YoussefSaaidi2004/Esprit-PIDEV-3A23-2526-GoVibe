package org.example.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.example.entities.Voiture;
import org.example.entities.Statut;
import org.example.services.ServiceVoiture;
import org.example.services.WebCarHarvester;
import org.example.utils.LocalDateTimeAdapter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de recommandation IA Pro.
 * Utilise DeepSeek pour analyser les donnees locales et web.
 */
/**
 * Service de recommandation IA Pro.
 * Utilise DeepSeek pour analyser les donnees locales et web.
 */
public class AiRecommendationService {
    private final BookNowClient bookNowClient = new BookNowClient();
    private final GeminiChatService geminiService = new GeminiChatService();
    private final ServiceVoiture voitureService = new ServiceVoiture();
    private final WebCarHarvester harvester = new WebCarHarvester();
    private final Gson gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter()).create();
    private static final String DEEPSEEK_API_KEY = "sk-868178ef790c4205acc04d02d4792ee2";
    private static final String DEEPSEEK_URL = "https://api.deepseek.com/v1/chat/completions";
    // Gemini fallback (OpenAI-compatible endpoint)
    private static final String GEMINI_API_KEY = "AIzaSyCNgI3fpLERtasCdFw2R1qBn1ENIpTKQNc";
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
    private static final String GEMINI_MODEL = "gemini-2.0-flash";
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public List<Voiture> recommendCars(double maxBudget, String destination) {
        // 1. Charger les donnees locales et web
        List<Voiture> localCars = voitureService.getAll().stream()
                .filter(v -> v.getStatut() == Statut.DISPONIBLE)
                .collect(Collectors.toList());
        
        List<Voiture> webCars = harvester.loadWebCars();
        
        List<Voiture> allOptions = new ArrayList<>(localCars);
        allOptions.addAll(webCars);

        // 2. Filtrage de base (Budget) avant envoi a l'IA pour economiser des tokens
        List<Voiture> candidates = allOptions.stream()
                .filter(v -> v.getPrixJour() <= maxBudget)
                .limit(10) // Limite pour l'exemple
                .collect(Collectors.toList());

        if (candidates.isEmpty()) return new ArrayList<>();

        // 3. Appel à DeepSeek pour le classement intelligent
        return rankWithDeepSeek(candidates, maxBudget, destination);
    }

    private List<Voiture> rankWithDeepSeek(List<Voiture> cars, double budget, String destination) {
        // Fallback: destination-aware ranking when API fails or key is placeholder
        if ("YOUR_DEEPSEEK_API_KEY".equals(DEEPSEEK_API_KEY)) {
            System.out.println("[AI] API Key not set. Using destination-aware fallback.");
            return fallbackRank(cars, destination);
        }

        try {
            String prompt = String.format("Analyze these cars for a user with budget %.2f and destination '%s'. Rank them by relevance. Return ONLY a comma-separated list of IDs.", budget, destination);
            
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", "deepseek-chat");
            JsonArray messages = new JsonArray();
            JsonObject msg = new JsonObject();
            msg.addProperty("role", "user");
            msg.addProperty("content", prompt + "\nCars: " + gson.toJson(cars));
            messages.add(msg);
            requestBody.add("messages", messages);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DEEPSEEK_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + DEEPSEEK_API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                System.err.println("[DeepSeek] API Error: " + response.statusCode() + " — trying Gemini fallback...");
                return rankWithGemini(cars, budget, destination);
            }

            JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
            String content = jsonResponse.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
            
            System.out.println("[DeepSeek Response] Content: " + content);

            // Parsing des IDs (format attendu: "1001,4,1002...")
            List<String> rankedIds = List.of(content.split(",")).stream()
                    .map(String::trim)
                    .collect(Collectors.toList());

            // Tri de la liste originale selon l'ordre des IDs retournés
            return cars.stream()
                .sorted((v1, v2) -> {
                    int idx1 = rankedIds.indexOf(String.valueOf(v1.getIdVoiture()));
                    int idx2 = rankedIds.indexOf(String.valueOf(v2.getIdVoiture()));
                    
                    // Si un ID n'est pas trouvé, il va à la fin
                    if (idx1 == -1) idx1 = 999;
                    if (idx2 == -1) idx2 = 999;
                    
                    return Integer.compare(idx1, idx2);
                })
                .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("[DeepSeek] Integration Error: " + e.getMessage());
            return rankWithGemini(cars, budget, destination);
        }
    }

    /**
     * Gemini fallback using the OpenAI-compatible endpoint.
     * Called automatically when DeepSeek returns a non-200 or throws.
     */
    private List<Voiture> rankWithGemini(List<Voiture> cars, double budget, String destination) {
        try {
            System.out.println("[Gemini] Attempting car ranking fallback...");
            String prompt = String.format(
                "Analyze these rental cars for a user with a daily budget of %.2f TND heading to '%s'. "
                + "Rank them by value and suitability. Return ONLY a comma-separated list of car IDs in ranked order.",
                budget, destination);

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", GEMINI_MODEL);
            JsonArray messages = new JsonArray();
            JsonObject msg = new JsonObject();
            msg.addProperty("role", "user");
            msg.addProperty("content", prompt + "\nCars: " + gson.toJson(cars));
            messages.add(msg);
            requestBody.add("messages", messages);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(java.net.URI.create(GEMINI_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + GEMINI_API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("[Gemini] API Error: " + response.statusCode() + " — using destination fallback.");
                return fallbackRank(cars, destination);
            }

            JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
            String content = jsonResponse.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

            System.out.println("[Gemini Response] " + content);

            List<String> rankedIds = java.util.Arrays.stream(content.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());

            return cars.stream()
                .sorted((v1, v2) -> {
                    int idx1 = rankedIds.indexOf(String.valueOf(v1.getIdVoiture()));
                    int idx2 = rankedIds.indexOf(String.valueOf(v2.getIdVoiture()));
                    if (idx1 == -1) idx1 = 999;
                    if (idx2 == -1) idx2 = 999;
                    return Integer.compare(idx1, idx2);
                })
                .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("[Gemini] Integration Error: " + e.getMessage());
            return fallbackRank(cars, destination);
        }
    }

    /**
     * Destination-aware fallback when DeepSeek is unavailable.
     * Boosts cars whose agency address matches the destination, then sorts by price.
     */
    private List<Voiture> fallbackRank(List<Voiture> cars, String destination) {
        String dest = destination == null ? "" : destination.trim().toLowerCase(java.util.Locale.ROOT);
        return cars.stream()
            .sorted((a, b) -> {
                boolean aMatch = !dest.isEmpty() && a.getAdresseAgence() != null
                        && a.getAdresseAgence().toLowerCase(java.util.Locale.ROOT).contains(dest);
                boolean bMatch = !dest.isEmpty() && b.getAdresseAgence() != null
                        && b.getAdresseAgence().toLowerCase(java.util.Locale.ROOT).contains(dest);
                if (aMatch && !bMatch) return -1;
                if (!aMatch && bMatch) return 1;
                return Double.compare(a.getPrixJour(), b.getPrixJour());
            })
            .collect(Collectors.toList());
    }
}
