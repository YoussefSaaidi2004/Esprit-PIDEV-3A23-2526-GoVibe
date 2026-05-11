package org.example.entities;

import java.text.Normalizer;
import java.util.Locale;

public enum TypeCarburant {
    ESSENCE("Essence"),
    DIESEL("Diesel"),
    HYBRIDE("Hybride"),
    ELECTRIQUE("Electrique");

    private final String dbValue;

    TypeCarburant(String dbValue) {
        this.dbValue = dbValue;
    }

    public String toDbValue() {
        return dbValue;
    }

    @Override
    public String toString() {
        return dbValue;
    }

    public static TypeCarburant fromDbValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ESSENCE;
        }
        String normalized = normalizeKey(value);
        switch (normalized) {
            case "essence":
                return ESSENCE;
            case "diesel":
                return DIESEL;
            case "hybride":
                return HYBRIDE;
            case "electrique":
                return ELECTRIQUE;
            default:
                System.err.println("⚠️ [TypeCarburant] Valeur non reconnue depuis la BD: '" + value + "'. Remplacement par ESSENCE par défaut.");
                return ESSENCE;
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
