package com.example.gestionvol.utils;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.util.Duration;

public class AnimationUtils {
    public static void applyFadeIn(Node node) {
        FadeTransition ft = new FadeTransition(Duration.millis(500), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    public static void applyHoverEffect(Node node) {
        node.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), node);
            st.setToX(1.02);
            st.setToY(1.02);
            st.play();
        });
        node.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), node);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
    }
}
