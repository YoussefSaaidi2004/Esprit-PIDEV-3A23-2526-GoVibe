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
public class AiRecommendationService {

    private final ServiceVoiture voitureService = new ServiceVoiture();
    private final WebCarHarvester harvester = new WebCarHarvester();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
    
    // TODO: Laisser l'utilisateur configurer sa cle API
    private static final String DEEPSEEK_API_KEY = "YOUR_DEEPSEEK_API_KEY";
    private static final String DEEPSEEK_URL = "https://api.deepseek.com/chat/completions";

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
        // Si pas de cle API, on fait un tri simple (fallback)
        if ("YOUR_DEEPSEEK_API_KEY".equals(DEEPSEEK_API_KEY)) {
            System.out.println("[AI] API Key not set. Using fallback ranking.");
            return cars.stream().sorted((v1, v2) -> Double.compare(v1.getPrixJour(), v2.getPrixJour())).collect(Collectors.toList());
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
                System.err.println("[DeepSeek] API Error: " + response.statusCode());
                return cars;
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
            e.printStackTrace();
            return cars;
        }
    }
}
