package org.example.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Service for calling the Python AI Risk Scoring micro-API.
 * Falls back to rule-based scoring if the API is unreachable.
 */
public class RiskScoringService {

    private static final String API_URL = "http://localhost:5001/predict-risk";
    private static final int TIMEOUT_SECONDS = 1;

    private final HttpClient httpClient;

    public RiskScoringService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    /**
     * Result record holding prediction and probability.
     */
    public static class RiskResult {
        private final int prediction;    // 0 = normal, 1 = risky
        private final double probability; // probability of being risky

        public RiskResult(int prediction, double probability) {
            this.prediction = prediction;
            this.probability = probability;
        }

        public int getPrediction() { return prediction; }
        public double getProbability() { return probability; }

        @Override
        public String toString() {
            return "RiskResult{prediction=" + prediction + ", probability=" + String.format("%.4f", probability) + "}";
        }
    }

    /**
     * Evaluates the risk of a login attempt by calling the Python AI API.
     * Falls back to rule-based scoring on failure.
     *
     * @param failedAttempts Number of recent failed attempts
     * @param newDevice      1 if device is unknown, 0 otherwise
     * @param newCountry     1 if country is different from past sessions
     * @param unusualTime    1 if login between 00:00-05:00
     * @param isAdmin        1 if user has ADMIN role
     * @return RiskResult with prediction and probability
     */
    public RiskResult evaluateRisk(int failedAttempts, int newDevice, int newCountry,
                                    int unusualTime, int isAdmin) {
        try {
            return callPythonAPI(failedAttempts, newDevice, newCountry, unusualTime, isAdmin);
        } catch (Exception e) {
            System.err.println("⚠️ [RiskScoring] Python API unreachable, using fallback rules: " + e.getMessage());
            return fallbackRuleBasedScoring(failedAttempts, newDevice, newCountry, unusualTime, isAdmin);
        }
    }

    /**
     * Calls the Python Flask API for ML-based risk prediction.
     */
    private RiskResult callPythonAPI(int failedAttempts, int newDevice, int newCountry,
                                      int unusualTime, int isAdmin) throws Exception {
        // Build JSON payload
        String jsonPayload = String.format(
            "{\"failed_attempts\":%d,\"new_device\":%d,\"new_country\":%d,\"unusual_time\":%d,\"is_admin\":%d}",
            failedAttempts, newDevice, newCountry, unusualTime, isAdmin
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("API returned status " + response.statusCode() + ": " + response.body());
        }

        // Parse response JSON
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        int prediction = json.get("risk_prediction").getAsInt();
        double probability = json.get("risk_probability").getAsDouble();

        System.out.println("🤖 [RiskScoring] AI prediction=" + prediction + ", probability=" + String.format("%.4f", probability));
        return new RiskResult(prediction, probability);
    }

    /**
     * Fallback rule-based scoring when the Python API is unavailable.
     * Uses a point system:
     *   - IP différente/new device: +2
     *   - Nouveau pays: +2
     *   - 3+ tentatives échouées: +3
     *   - Heure suspecte: +1
     *   - Admin: +1
     * Score >= 4 → risky (prediction=1)
     */
    private RiskResult fallbackRuleBasedScoring(int failedAttempts, int newDevice, int newCountry,
                                                 int unusualTime, int isAdmin) {
        int score = 0;
        score += newDevice * 2;
        score += newCountry * 2;
        score += (failedAttempts >= 3) ? 3 : 0;
        score += unusualTime;
        score += isAdmin;

        // Normalize score to a 0-1 probability (max possible score = 9)
        double probability = Math.min(score / 9.0, 1.0);
        int prediction = score >= 4 ? 1 : 0;

        System.out.println("📏 [RiskScoring] Fallback score=" + score + "/9, probability=" + String.format("%.4f", probability));
        return new RiskResult(prediction, probability);
    }
}
