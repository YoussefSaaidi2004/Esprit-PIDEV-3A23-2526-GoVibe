package org.example.utils;

import org.example.entities.Voiture;

public final class VoitureSelection {

    private static Voiture selected;

    private VoitureSelection() {
    }

    public static Voiture get() {
        return selected;
    }

    public static void set(Voiture voiture) {
        selected = voiture;
    }

    public static void clear() {
        selected = null;
    }
}
