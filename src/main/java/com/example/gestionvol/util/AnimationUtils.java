package com.example.gestionvol.util;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

public class AnimationUtils {

    public static void fadeIn(Node node, double ms) {
        FadeTransition ft = new FadeTransition(Duration.millis(ms), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    public static void slideIn(Node node, double ms) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(ms), node);
        tt.setFromX(-50);
        tt.setToX(0);
        tt.play();
    }

    public static void applyHoverScale(Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(200), node);
        node.setOnMouseEntered(e -> {
            st.setToX(1.03);
            st.setToY(1.03);
            st.playFromStart();
        });
        node.setOnMouseExited(e -> {
            st.setToX(1.0);
            st.setToY(1.0);
            st.playFromStart();
        });
    }
}
