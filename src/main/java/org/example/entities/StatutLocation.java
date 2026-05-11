package org.example.entities;

import java.text.Normalizer;
import java.util.Locale;

public enum StatutLocation {
    EN_ATTENTE("EN_ATTENTE", "En attente"),
    CONFIRMEE("CONFIRMEE", "Confirmee"),
    ANNULEE("ANNULEE", "Annulee");

    private final String dbValue;
    private final String label;

    StatutLocation(String dbValue, String label) {
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

    public static StatutLocation fromDbValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return EN_ATTENTE;
        }
        String normalized = normalizeKey(value);
        switch (normalized) {
            case "en_attente":
            case "en attente":
                return EN_ATTENTE;
            case "confirmee":
            case "confirme":
                return CONFIRMEE;
            case "annulee":
            case "annule":
                return ANNULEE;
            default:
                System.err.println("⚠️ [StatutLocation] Valeur non reconnue depuis la BD: '" + value + "'. Remplacement par EN_ATTENTE par défaut.");
                return EN_ATTENTE;
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
