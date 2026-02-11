package com.example.gestionvol.utils;

import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class IconUtils {
    
    public static Text createIcon(String emoji, String size, String color) {
        Text icon = new Text(emoji);
        icon.setStyle("-fx-font-size: " + size + "; -fx-fill: " + color + ";");
        return icon;
    }

    public static Button createIconButton(String emoji, String tooltip, String color) {
        Button btn = new Button();
        btn.setGraphic(createIcon(emoji, "18px", color));
        btn.getStyleClass().add("icon-btn");
        if (tooltip != null) btn.setTooltip(new Tooltip(tooltip));
        return btn;
    }
}
