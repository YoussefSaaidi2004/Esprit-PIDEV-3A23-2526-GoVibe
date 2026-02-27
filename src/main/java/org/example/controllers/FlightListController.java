package org.example.controllers;

import org.example.entities.Flight;
import org.example.services.FlightService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TextField;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class FlightListController {

    @FXML private Text        flightListText;
    @FXML private TextField   searchField;
    @FXML private Button      btnWeather;
    @FXML private StackPane   weatherRainOverlay;   // full-centre blur overlay
    @FXML private StackPane   rainDropCenter;       // the big animated drop inside overlay
    @FXML private Text        rainOverlaySubtitle;  // "Fetching weather…" label inside overlay

    private final FlightService flightService = new FlightService();

    @FXML
    public void initialize() {
        loadFlights();
        startRainDropPulse();
    }

    // ── Idle pulse on the rain-drop button ───────────────────────────────────
    private void startRainDropPulse() {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(1100), btnWeather);
        pulse.setFromX(1.00); pulse.setFromY(1.00);
        pulse.setToX(1.08);   pulse.setToY(1.08);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setInterpolator(Interpolator.EASE_BOTH);
        pulse.play();
    }

    // ── Shared animated rain-drop reveal ─────────────────────────────────────
    /**
     * Shows the centre rain-drop animation, runs {@code onDone} on the FX thread
     * after it finishes. Safe to call from any thread.
     *
     * @param subtitle text shown below the drop while animating
     * @param onDone   action to execute when the overlay fades out
     */
    private void playRainDropRevealThen(String subtitle, Runnable onDone) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> playRainDropRevealThen(subtitle, onDone));
            return;
        }

        // Update subtitle text
        if (rainOverlaySubtitle != null) rainOverlaySubtitle.setText(subtitle);

        // Fade the dark overlay in
        weatherRainOverlay.setVisible(true);
        weatherRainOverlay.setOpacity(0.0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), weatherRainOverlay);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        // Animate the big rain-drop: scale from tiny + blur dissolve
        rainDropCenter.setScaleX(0.10);
        rainDropCenter.setScaleY(0.10);
        GaussianBlur blur = new GaussianBlur(28);
        rainDropCenter.setEffect(blur);

        ScaleTransition grow = new ScaleTransition(Duration.millis(520), rainDropCenter);
        grow.setToX(1.0);
        grow.setToY(1.0);
        grow.setInterpolator(Interpolator.EASE_OUT);
        grow.play();

        // Animate blur clearance via a DoubleProperty so JavaFX can interpolate
        SimpleDoubleProperty blurProp = new SimpleDoubleProperty(28);
        blurProp.addListener((obs, o, n) -> blur.setRadius(n.doubleValue()));
        new Timeline(
            new KeyFrame(Duration.ZERO,        new KeyValue(blurProp, 28, Interpolator.EASE_IN)),
            new KeyFrame(Duration.millis(520),  new KeyValue(blurProp,  0, Interpolator.EASE_IN))
        ).play();

        // Hold briefly, then fade out and execute callback
        PauseTransition hold = new PauseTransition(Duration.millis(740));
        hold.setOnFinished(ev -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), weatherRainOverlay);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e2 -> {
                weatherRainOverlay.setVisible(false);
                rainDropCenter.setEffect(null);
                if (onDone != null) onDone.run();
            });
            fadeOut.play();
        });
        hold.play();
    }

    // ── Button flash for voice-triggered events ───────────────────────────────
    /** Scale-flash the rain-drop button to show the AI triggered something. */
    private void flashButton() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::flashButton);
            return;
        }
        ScaleTransition flash = new ScaleTransition(Duration.millis(160), btnWeather);
        flash.setFromX(1.0); flash.setFromY(1.0);
        flash.setToX(1.38);  flash.setToY(1.38);
        flash.setAutoReverse(true);
        flash.setCycleCount(2);
        flash.setInterpolator(Interpolator.EASE_BOTH);
        flash.play();
    }

    // ── Weather button handler (manual click) ─────────────────────────────────
    @FXML
    private void handleWeatherButton() {
        // Animate then show dialog
        playRainDropRevealThen("Checking the skies …", this::showCityDialog);
    }

    /** City-name dialog → fetch → show card (manual / button path). */
    private void showCityDialog() {
        TextInputDialog dialog = new TextInputDialog("Tunis");
        dialog.setTitle("GoVibe Weather");
        dialog.setHeaderText("Real-Time Weather");
        dialog.setContentText("Enter a city name:");
        try {
            dialog.getDialogPane().getStylesheets()
                  .add(getClass().getResource("/styles.css").toExternalForm());
        } catch (Exception ignored) {}
        dialog.showAndWait().ifPresent(city -> {
            if (!city.isBlank()) {
                WeatherOverlayController.fetchAndShow(city.trim());
            }
        });
    }

    // ── Voice-triggered weather display (AI path) ─────────────────────────────
    /**
     * Called by the AI voice system (CommandRouter proxy) when the voice agent
     * has already fetched weather data. Plays the animated rain-drop reveal
     * (same as the button click) then opens the weather card directly — no
     * second HTTP call, no dialog, instant display.
     *
     * @param city      city name extracted by the voice agent
     * @param temp      temperature string e.g. "24°C"
     * @param condition weather description e.g. "Sunny"
     * @param humidity  humidity string e.g. "65%"
     * @param wind      wind speed string e.g. "15 km/h"
     * @param feel      feels-like temperature e.g. "26°C"
     */
    public void showWeather(String city, String temp, String condition,
                            String humidity, String wind, String feel) {
        // Flash the button so the user sees the AI triggered weather
        flashButton();
        // Play the animated reveal, then show the pre-fetched weather card
        String subtitle = (city != null && !city.isBlank())
                ? "Weather for " + city + " …"
                : "Fetching weather …";
        playRainDropRevealThen(subtitle, () ->
            WeatherOverlayController.show(city, temp, condition, humidity, wind, feel));
    }

    // ── Data loading ─────────────────────────────────────────────────────────
    private void loadFlights() {
        var flights = flightService.getAllFlights();
        if (flights.isEmpty()) {
            flightListText.setText("No flights available");
        } else {
            StringBuilder sb = new StringBuilder();
            for (Flight f : flights) {
                sb.append(f.getFlightId()).append(": ").append(f.getDestination()).append("\n");
            }
            flightListText.setText(sb.toString());
        }
    }
}
