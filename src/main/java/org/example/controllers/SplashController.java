package org.example.controllers;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * Splash screen controller — plays a slick logo animation then invokes a callback.
 */
public class SplashController {

    @FXML private VBox splashContent;
    @FXML private ImageView logoImage;
    @FXML private Text splashTitle;
    @FXML private ProgressIndicator progressIndicator;

    private Runnable onFinished;

    /**
     * Play the splash animation sequence, then call the provided callback.
     *
     * @param onFinished action to execute after the animation completes
     */
    public void playAnimation(Runnable onFinished) {
        this.onFinished = onFinished;

        // ── 1. Fade-in the entire splash content ──
        FadeTransition fadeIn = new FadeTransition(Duration.millis(600), splashContent);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        // ── 2. Logo scale pulse (breathing effect) ──
        ScaleTransition logoPulse = new ScaleTransition(Duration.millis(800), logoImage);
        logoPulse.setFromX(0.6);
        logoPulse.setFromY(0.6);
        logoPulse.setToX(1.05);
        logoPulse.setToY(1.05);
        logoPulse.setCycleCount(2);
        logoPulse.setAutoReverse(true);
        logoPulse.setInterpolator(Interpolator.EASE_BOTH);

        // ── 3. Logo rotation (subtle spin) ──
        RotateTransition logoRotate = new RotateTransition(Duration.millis(1200), logoImage);
        logoRotate.setByAngle(360);
        logoRotate.setInterpolator(Interpolator.EASE_BOTH);

        // ── 4. Title slide up ──
        TranslateTransition titleSlide = new TranslateTransition(Duration.millis(500), splashTitle);
        titleSlide.setFromY(20);
        titleSlide.setToY(0);
        FadeTransition titleFade = new FadeTransition(Duration.millis(500), splashTitle);
        titleFade.setFromValue(0.0);
        titleFade.setToValue(1.0);

        // ── 5. Hold for a moment, then fade out ──
        PauseTransition hold = new PauseTransition(Duration.millis(800));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), splashContent);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        // ── Build sequence ──
        SequentialTransition sequence = new SequentialTransition(
                fadeIn,
                new ParallelTransition(logoPulse, logoRotate),
                new ParallelTransition(titleSlide, titleFade),
                hold,
                fadeOut
        );

        sequence.setOnFinished(event -> {
            if (this.onFinished != null) {
                javafx.application.Platform.runLater(this.onFinished);
            }
        });

        sequence.play();
    }
}
