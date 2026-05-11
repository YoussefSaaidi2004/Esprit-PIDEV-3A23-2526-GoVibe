package org.example.entities;

import java.text.Normalizer;
import java.util.Locale;

public enum Statut {
    DISPONIBLE("DISPONIBLE", "Disponible"),
    LOUEE("LOUEE", "Louee"),
    MAINTENANCE("MAINTENANCE", "Maintenance"),
    ACCIDENTE("ACCIDENTE", "Accidenté");

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
        if (value == null || value.trim().isEmpty()) {
            return MAINTENANCE;
        }
        String normalized = normalizeKey(value);
        switch (normalized) {
            case "disponible":
                return DISPONIBLE;
            case "loue":
            case "louee":
                return LOUEE;
            case "maintenance":
            case "en_maintenance":
            case "en maintenance":
                return MAINTENANCE;
            case "accidente":
            case "accidenté":
                return ACCIDENTE;
            default:
                System.err.println("⚠️ [Statut] Valeur non reconnue depuis la BD: '" + value + "'. Remplacement par MAINTENANCE par défaut.");
                return MAINTENANCE;
        }
    }

    private static String normalizeKey(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        String trimmed = input.trim();
        String noAccents = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return noAccents.toLowerCase(Locale.ROOT);
    }
}
