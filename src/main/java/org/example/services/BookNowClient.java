package org.example.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.example.entities.Voiture;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * REST client for the self-hosted Book-Now (Spring Boot) car rental API.
 * Communicates via HTTP/JSON — no proprietary SDKs required.
 */
public class BookNowClient {
    private final String baseUrl;
    private final HttpClient client;
    private final Gson gson;

    public BookNowClient() {
        this("http://localhost:8080/api");
    }

    public BookNowClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.gson = new Gson();
    }

    /**
     * Fetch all available cars from the Book-Now fleet.
     */
    public List<Voiture> getAvailableCars() {
        return executeWithRetry(() -> {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/cars/available"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                List<Voiture> cars = gson.fromJson(response.body(), new TypeToken<List<Voiture>>(){}.getType());
                return cars != null ? cars : Collections.emptyList();
            }
            throw new RuntimeException("API error: " + response.statusCode());
        }, Collections.emptyList(), "fetching cars");
    }

    private <T> T executeWithRetry(RequestCallable<T> callable, T defaultValue, String actionName) {
        int maxRetries = 3;
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                return callable.call();
            } catch (Exception e) {
                attempt++;
                if (attempt >= maxRetries) {
                    System.err.println("[BookNowClient] Failed " + actionName + " after " + maxRetries + " attempts: " + e.getMessage());
                } else {
                    try { Thread.sleep(1000L * attempt); } catch (InterruptedException ignored) {}
                }
            }
        }
        return defaultValue;
    }

    @FunctionalInterface
    private interface RequestCallable<T> {
        T call() throws Exception;
    }

    /**
     * Fetch a specific car by ID from the Book-Now API.
     */
    public Voiture getCarById(int id) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/cars/" + id))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), Voiture.class);
            }
        } catch (Exception e) {
            System.err.println("[BookNowClient] Error fetching car #" + id + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Fetch fleet status summary (total, available, rented counts).
     * Returns a Map with keys: "total", "available", "rented".
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getFleetStatus() {
        return executeWithRetry(() -> {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/fleet/status"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Map<String, Object> status = gson.fromJson(response.body(), Map.class);
                return status != null ? status : Collections.emptyMap();
            }
            throw new RuntimeException("API error: " + response.statusCode());
        }, Collections.emptyMap(), "fetching fleet status");
    }

    /**
     * Quick health-check: returns true if Book-Now API is reachable.
     */
    public boolean testConnection() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/health"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
