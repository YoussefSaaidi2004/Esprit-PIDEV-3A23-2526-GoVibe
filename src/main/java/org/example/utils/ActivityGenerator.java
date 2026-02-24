package org.example.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.example.entities.Activity;

import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility to pre-generate activities.json using Gemini AI.
 * This implements the "Data Seeding" strategy to avoid runtime API costs and errors.
 */
public class ActivityGenerator {

    private static final String GEMINI_KEY = "AIzaSyA-w2e0Y5UWk41ZhsAgMJpE_GGRAvJLFF8";
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent?key=" + GEMINI_KEY;
    private static final String COUNTRIES_URL = "https://restcountries.com/v3.1/all?fields=name,cca2";
    
    private static final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        try {
            System.out.println("--- Starting GoVibe Data Seeding ---");
            
            // 1. Fetch all countries
            Map<String, String> countryMap = fetchCountryCodes();
            System.out.println("Fetched " + countryMap.size() + " countries.");

            Map<String, List<Activity>> database = new HashMap<>();

            // 2. Generate for a selection of major countries
            // Expanding to 60+ countries for a professional global database
            String[] targetCodes = {
                "FR", "IT", "ES", "US", "JP", "MA", "TH", "GR", "TR", "PT",
                "DE", "GB", "CH", "AT", "EG", "AE", "CA", "MX", "BR", "ID",
                "DZ", "TN", "SA", "VN", "KR", "AU", "NZ", "ZA", "KE", "SN",
                "BE", "NL", "SE", "NO", "DK", "FI", "IE", "PL", "CZ", "HU",
                "RO", "HR", "IS", "RU", "CN", "IN", "SG", "PH", "MY", "AR",
                "CL", "PE", "CO", "CR", "CU", "DO", "JM", "PR", "IL", "JO",
                "QA", "KW", "OM", "LB", "GL"
            };
            
            for (String code : targetCodes) {
                String name = countryMap.get(code);
                System.out.println("Generating activities for: " + name + " (" + code + ")...");
                try {
                    List<Activity> activities = fetchFromGemini(name);
                    database.put(code, activities);
                    System.out.println("Successfully generated " + activities.size() + " activities.");
                    Thread.sleep(2000); // Respect rate limits
                } catch (Exception e) {
                    System.err.println("Error for " + name + ": " + e.getMessage());
                }
            }

            // 3. Save to file
            saveToFile(database);
            System.out.println("--- Seeding Complete ---");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<String, String> fetchCountryCodes() throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(COUNTRIES_URL)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonArray arr = JsonParser.parseString(response.body()).getAsJsonArray();
        
        Map<String, String> map = new HashMap<>();
        for (var el : arr) {
            JsonObject obj = el.getAsJsonObject();
            String code = obj.get("cca2").getAsString();
            String name = obj.getAsJsonObject("name").get("common").getAsString();
            map.put(code, name);
        }
        return map;
    }

    private static List<Activity> fetchFromGemini(String countryName) throws Exception {
        String prompt = "List the top 8 tourist activities or landmarks in " + countryName + ". " +
                "For each, provide a name (translated to French), a short description in French (1 sentence), and a category (e.g., Monument, Musée, Nature, Gastronomie, Divertissement). " +
                "Return ONLY a JSON array in this format: " +
                "[{\"name\": \"...\", \"description\": \"...\", \"category\": \"...\"}] " +
                "Do not include any other text, markdown blocks, or explanations.";

        JsonObject root = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject contentObj = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject partObj = new JsonObject();
        partObj.addProperty("text", prompt);
        parts.add(partObj);
        contentObj.add("parts", parts);
        contents.add(contentObj);
        root.add("contents", contents);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(root)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonObject resJson = JsonParser.parseString(response.body()).getAsJsonObject();
        String content = resJson.getAsJsonArray("candidates").get(0).getAsJsonObject()
                     .getAsJsonObject("content").getAsJsonArray("parts").get(0).getAsJsonObject()
                     .get("text").getAsString();

        // Robust JSON extraction
        int start = content.indexOf("[");
        int end = content.lastIndexOf("]");
        if (start != -1 && end != -1 && end > start) {
            content = content.substring(start, end + 1);
        }

        JsonArray arr = JsonParser.parseString(content.trim()).getAsJsonArray();
        List<Activity> result = new ArrayList<>();
        for (var el : arr) {
            JsonObject o = el.getAsJsonObject();
            result.add(new Activity(
                o.get("name").getAsString(),
                o.get("description").getAsString(),
                o.get("category").getAsString()
            ));
        }
        return result;
    }

    private static void saveToFile(Map<String, List<Activity>> data) {
        String path = "src/main/resources/data/activities.json";
        try (FileWriter writer = new FileWriter(path)) {
            gson.toJson(data, writer);
            System.out.println("File saved to: " + path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
