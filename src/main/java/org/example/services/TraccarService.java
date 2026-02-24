package org.example.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

/**
 * Client for the self-hosted Traccar GPS tracking server.
 * Fetches real-time GPS positions via REST API.
 */
public class TraccarService {
    private final String traccarUrl;
    private final String authHeader;
    private final HttpClient client;

    /**
     * Structured GPS position data from Traccar.
     */
    public static class TraccarPosition {
        public final double latitude;
        public final double longitude;
        public final double speed;       // km/h
        public final double course;      // heading in degrees
        public final String deviceTime;  // ISO timestamp from device
        public final String rawJson;

        public TraccarPosition(double latitude, double longitude, double speed,
                               double course, String deviceTime, String rawJson) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.speed = speed;
            this.course = course;
            this.deviceTime = deviceTime;
            this.rawJson = rawJson;
        }

        @Override
        public String toString() {
            return String.format("Position[%.6f, %.6f] speed=%.1f km/h", latitude, longitude, speed);
        }
    }

    public TraccarService() {
        this("http://localhost:8082/api/positions", "admin", "admin");
    }

    public TraccarService(String baseUrl, String user, String password) {
        this.traccarUrl = baseUrl;
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes());
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Fetch latest raw JSON position for a device.
     */
    public String getLatestPosition(String deviceId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(traccarUrl + "?deviceId=" + deviceId))
                    .header("Authorization", authHeader)
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonArray positions = JsonParser.parseString(response.body()).getAsJsonArray();
                if (positions.size() > 0) {
                    return positions.get(0).toString();
                }
            } else if (response.statusCode() != 404) {
                // Only log unexpected errors, not "not found"
                System.err.println("[TraccarService] API returned status: " + response.statusCode());
            }
        } catch (Exception e) {
            // Silently fail for common network timeouts unless debugging
            // System.err.println("[TraccarService] Error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Fetch latest position as a structured TraccarPosition object.
     * Returns null if the server is unreachable or no data is available.
     */
    public TraccarPosition getLatestPositionParsed(String deviceId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(traccarUrl + "?deviceId=" + deviceId))
                    .header("Authorization", authHeader)
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonArray positions = JsonParser.parseString(response.body()).getAsJsonArray();
                if (positions.size() > 0) {
                    JsonObject pos = positions.get(0).getAsJsonObject();
                    double lat = pos.has("latitude") ? pos.get("latitude").getAsDouble() : 0;
                    double lon = pos.has("longitude") ? pos.get("longitude").getAsDouble() : 0;
                    double speed = pos.has("speed") ? pos.get("speed").getAsDouble() * 1.852 : 0; // knots → km/h
                    double course = pos.has("course") ? pos.get("course").getAsDouble() : 0;
                    String deviceTime = pos.has("deviceTime") ? pos.get("deviceTime").getAsString() : "";
                    return new TraccarPosition(lat, lon, speed, course, deviceTime, pos.toString());
                }
            } else if (response.statusCode() != 404) {
                System.err.println("[TraccarService] API returned status: " + response.statusCode());
            }
        } catch (Exception e) {
            // Suppress common errors for cleaner logs
            // System.err.println("[TraccarService] Error parsing position: " + e.getMessage());
        }
        return null;
    }

    /**
     * Check if the Traccar server is reachable.
     */
    public boolean isServerAvailable() {
        try {
            // Use the /api/server endpoint which doesn't require device filtering
            String serverUrl = traccarUrl.replace("/positions", "/server");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl))
                    .header("Authorization", authHeader)
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
