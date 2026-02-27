package org.example.controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Controller for the real-time weather overlay card.
 *
 * <p>Use the static factory {@link #show(String, String, String, String, String, String)}
 * to display weather data received from the voice agent.
 * Use {@link #fetchAndShow(String)} to trigger a full fetch+display cycle from
 * the manual weather button on the flight page.
 */
public class WeatherOverlayController {

    // ── FXML fields ──────────────────────────────────────────────────────────
    @FXML private Label cityLabel;
    @FXML private Label tempLabel;
    @FXML private Label conditionLabel;
    @FXML private Label humidityLabel;
    @FXML private Label windLabel;
    @FXML private Label feelLabel;
    @FXML private Label conditionIconLabel;
    @FXML private VBox  weatherCard;

    // ── Public static API ─────────────────────────────────────────────────────

    /**
     * Show weather data in a floating overlay window.
     * Safe to call from any thread — internally runs on the FX thread.
     *
     * @param city      city name
     * @param temp      temperature string e.g. "24°C"
     * @param condition weather description e.g. "Sunny"
     * @param humidity  humidity string e.g. "65%"
     * @param wind      wind speed string e.g. "15 km/h"
     * @param feel      feels-like temperature e.g. "26°C"
     */
    public static void show(String city, String temp, String condition,
                            String humidity, String wind, String feel) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                    WeatherOverlayController.class.getResource("/views/weather-overlay.fxml"));
                Parent root = loader.load();
                WeatherOverlayController ctrl = loader.getController();
                ctrl.populate(city, temp, condition, humidity, wind, feel);

                Stage stage = new Stage(StageStyle.TRANSPARENT);
                stage.initModality(Modality.NONE);
                Scene scene = new Scene(root);
                scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
                stage.setScene(scene);
                stage.setTitle("Weather — " + city);
                stage.setAlwaysOnTop(true);

                // Centre the overlay on screen
                javafx.geometry.Rectangle2D screen =
                    javafx.stage.Screen.getPrimary().getVisualBounds();
                stage.setX(screen.getMinX() + (screen.getWidth()  - 420) / 2);
                stage.setY(screen.getMinY() + 60);

                stage.show();

                // Slide-in + fade animation
                root.setTranslateY(-60);
                root.setOpacity(0.0);
                Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO,
                        new KeyValue(root.translateYProperty(), -60),
                        new KeyValue(root.opacityProperty(), 0.0)
                    ),
                    new KeyFrame(Duration.millis(350),
                        new KeyValue(root.translateYProperty(), 0,
                            Interpolator.EASE_OUT),
                        new KeyValue(root.opacityProperty(), 1.0,
                            Interpolator.EASE_OUT)
                    )
                );
                tl.play();

                // Store stage reference for close button
                ctrl.currentStage = stage;
            } catch (Exception e) {
                System.err.println("[WeatherOverlay] Failed to open: " + e.getMessage());
            }
        });
    }

    /**
     * Fetch real-time weather for {@code city} from wttr.in (Java side),
     * then display the overlay. Used by the manual weather button on the flight page.
     * Runs the HTTP call on a background thread to avoid blocking the FX thread.
     *
     * @param city city to query; falls back to "Tunis" if blank
     */
    public static void fetchAndShow(String city) {
        if (city == null || city.isBlank()) city = "Tunis";
        final String finalCity = city.trim();
        new Thread(() -> {
            WeatherResult result = fetchWeather(finalCity);
            if (result != null) {
                show(result.city, result.temp, result.condition,
                     result.humidity, result.wind, result.feel);
            } else {
                Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.WARNING);
                    alert.setTitle("Weather Unavailable");
                    alert.setHeaderText("Could not get weather for \"" + finalCity + "\"");
                    alert.setContentText("Please check your internet connection and try again.");
                    alert.show();
                });
            }
        }, "GoVibe-WeatherFetch").start();
    }

    // ── Java-side weather fetch (fallback / manual button) ───────────────────

    public static class WeatherResult {
        public String city, temp, condition, humidity, wind, feel;
    }

    public static WeatherResult fetchWeather(String city) {
        try {
            String encoded = URLEncoder.encode(city, StandardCharsets.UTF_8);
            String apiKey = "zCouNYcNJzY0Luj5ZeSA9pEQ8jqiFULU";
            // Tomorrow.io API endpoint for real-time weather
            String urlStr = "https://api.tomorrow.io/v4/weather/realtime?location=" + encoded + "&apikey=" + apiKey;
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "GoVibe/1.0");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);

            if (conn.getResponseCode() != 200) return null;

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
            String json = sb.toString();

            WeatherResult r = new WeatherResult();
            r.city = toTitleCase(city);
            // Parse Tomorrow.io JSON response
            r.temp = parseJsonString(json, "temperature") + "\u00b0C";
            r.feel = parseJsonString(json, "apparent_temperature") + "\u00b0C";
            r.humidity = parseJsonString(json, "humidity") + "%";
            r.wind = parseJsonString(json, "wind_speed") + " km/h";
            r.condition = parseJsonString(json, "weather_code"); // Use weather_code for description
            return r;
        } catch (Exception e) {
            System.err.println("[WeatherOverlay] Fetch error: " + e.getMessage());
            return null;
        }
    }

    // ── Instance methods ──────────────────────────────────────────────────────

    private Stage currentStage;

    private void populate(String city, String temp, String condition,
                          String humidity, String wind, String feel) {
        cityLabel.setText(city != null ? city : "—");
        tempLabel.setText(temp != null ? temp : "--");
        conditionLabel.setText(condition != null ? condition : "--");
        humidityLabel.setText(humidity != null ? humidity : "--");
        windLabel.setText(wind != null ? wind : "--");
        feelLabel.setText(feel != null ? feel : "--");
        conditionIconLabel.setText(conditionToIcon(condition));
    }

    @FXML
    private void handleClose() {
        if (currentStage != null) {
            FadeTransition ft = new FadeTransition(Duration.millis(200), weatherCard);
            ft.setToValue(0.0);
            ft.setOnFinished(e -> currentStage.close());
            ft.play();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns a weather emoji matching the condition description. */
    private static String conditionToIcon(String condition) {
        if (condition == null) return "�";
        String lc = condition.toLowerCase();
        // Tomorrow.io weather_code mapping
        if (lc.contains("clear")) return "☀";
        if (lc.contains("cloud")) return "☁";
        if (lc.contains("rain")) return "🌧";
        if (lc.contains("snow")) return "❄";
        if (lc.contains("fog")) return "🌫";
        if (lc.contains("wind")) return "💨";
        if (lc.contains("thunder")) return "⛈";
        return "🌧";
    }

    /** Extract a string value for the given key from a simple JSON blob. */
    private static String parseJsonString(String json, String key) {
        // Match "key":"value" or "key": "value"
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return "--";
        idx = json.indexOf(':', idx + pattern.length());
        if (idx < 0) return "--";
        idx++;
        while (idx < json.length() && (json.charAt(idx) == ' ' || json.charAt(idx) == '"')) idx++;
        int end = idx;
        while (end < json.length() && json.charAt(end) != '"'
               && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
        return json.substring(idx, end).trim();
    }

    private static String toTitleCase(String s) {
        if (s == null || s.isBlank()) return s;
        String[] parts = s.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0)));
                if (p.length() > 1) sb.append(p.substring(1).toLowerCase());
            }
            sb.append(' ');
        }
        return sb.toString().trim();
    }
}
