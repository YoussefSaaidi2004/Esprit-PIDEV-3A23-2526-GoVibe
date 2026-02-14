package org.example.entities;

import java.text.Normalizer;
import java.util.Locale;

public enum Statut {
    DISPONIBLE("DISPONIBLE", "Disponible"),
    LOUEE("LOUEE", "Louee"),
    MAINTENANCE("MAINTENANCE", "Maintenance");

    private final String dbValue;
    private final String label;

    Statut(String dbValue, String label) {
        this.dbValue = dbValue;
        this.label = label;
    }

    public String toDbValue() {
        return dbValue;
    }

    @Override
    public String toString() {
        return label;
    }

    public static Statut fromDbValue(String value) {
        String normalized = normalizeKey(value);
        switch (normalized) {
            case "disponible":
                return DISPONIBLE;
            case "loue":
            case "louee":
                return LOUEE;
            case "maintenance":
                return MAINTENANCE;
            default:
                throw new RuntimeException("Statut invalide. Valeurs possibles: DISPONIBLE, LOUEE, MAINTENANCE.");
        }
    }

    private static String normalizeKey(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new RuntimeException("Champ requis manquant.");
        }
        String trimmed = input.trim();
        String noAccents = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return noAccents.toLowerCase(Locale.ROOT);
    }
}
