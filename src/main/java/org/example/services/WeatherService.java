package org.example.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 🌤 Service Météo — OpenWeather API
 * API Key: zCouNYcNJzY0Luj5ZeSA9pEQ8jqiFULU
 * Displays real-time weather for the hotel's city.
 */
public class WeatherService {

    private static final String API_KEY = "zCouNYcNJzY0Luj5ZeSA9pEQ8jqiFULU";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";

    private final HttpClient httpClient;

    public WeatherService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static class WeatherData {
        public String city;
        public String country;
        public double temperature;
        public double feelsLike;
        public double humidity;
        public String description;
        public String iconCode;
        public double windSpeed;

        public String getEmoji() {
            if (description == null) return "🌡";
            String d = description.toLowerCase();
            if (d.contains("soleil") || d.contains("clear") || d.contains("dégagé")) return "☀️";
            if (d.contains("nuage") || d.contains("cloud") || d.contains("couvert")) return "⛅";
            if (d.contains("pluie") || d.contains("rain") || d.contains("drizzle")) return "🌧";
            if (d.contains("orage") || d.contains("thunder") || d.contains("storm")) return "⛈";
            if (d.contains("neige") || d.contains("snow")) return "❄️";
            if (d.contains("brouillard") || d.contains("fog") || d.contains("mist")) return "🌫";
            return "🌡";
        }

        public String getSummary() {
            return String.format("%s %s | %.0f°C | 💧%d%% | 💨%.1f m/s",
                    getEmoji(), description, temperature, (int) humidity, windSpeed);
        }

        public String getFullDisplay() {
            return String.format(
                "%s %s\n%.1f°C  (ressenti %.1f°C)\n💧 Humidité: %d%%\n💨 Vent: %.1f m/s",
                getEmoji(), description, temperature, feelsLike, (int) humidity, windSpeed
            );
        }
    }

    /**
     * Fetch weather data for a given city.
     * @param ville City name (e.g. "Tunis", "Paris")
     * @return WeatherData or null on failure
     */
    public WeatherData getWeather(String ville) {
        if (ville == null || ville.isBlank()) return null;

        try {
            String encodedCity = ville.trim().replace(" ", "+");
            String url = BASE_URL + "?q=" + encodedCity + "&appid=" + API_KEY +
                         "&units=metric&lang=fr";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseWeatherResponse(response.body());
            } else {
                System.err.println("⚠️ WeatherService: HTTP " + response.statusCode() + " for city: " + ville);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("⚠️ WeatherService: " + e.getMessage());
        }
        return null;
    }

    private WeatherData parseWeatherResponse(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            WeatherData data = new WeatherData();

            data.city    = root.get("name").getAsString();
            data.country = root.getAsJsonObject("sys").get("country").getAsString();

            JsonObject main = root.getAsJsonObject("main");
            data.temperature = main.get("temp").getAsDouble();
            data.feelsLike   = main.get("feels_like").getAsDouble();
            data.humidity    = main.get("humidity").getAsDouble();

            JsonObject wind = root.getAsJsonObject("wind");
            data.windSpeed = wind.has("speed") ? wind.get("speed").getAsDouble() : 0;

            if (root.has("weather") && root.getAsJsonArray("weather").size() > 0) {
                JsonObject w = root.getAsJsonArray("weather").get(0).getAsJsonObject();
                data.description = w.get("description").getAsString();
                data.iconCode = w.get("icon").getAsString();
            } else {
                data.description = "N/A";
            }
            return data;
        } catch (Exception e) {
            System.err.println("⚠️ WeatherService parse error: " + e.getMessage());
            return null;
        }
    }
}
