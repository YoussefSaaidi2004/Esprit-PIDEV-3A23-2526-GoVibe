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
        String normalized = normalizeKey(value);
        switch (normalized) {
            case "en_attente":
            case "en attente":
                return EN_ATTENTE;
            case "confirmee":
                return CONFIRMEE;
            case "annulee":
                return ANNULEE;
            default:
                throw new RuntimeException("Statut location invalide. Valeurs possibles: EN_ATTENTE, CONFIRMEE, ANNULEE.");
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
