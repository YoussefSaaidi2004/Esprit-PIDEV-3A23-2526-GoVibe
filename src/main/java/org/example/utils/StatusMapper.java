package org.example.utils;

/**
 * Unified status mapping between Symfony (French) and JavaFX (English).
 * The database uses Symfony's French status values as the canonical format.
 * JavaFX displays English labels but reads/writes French values to the DB.
 */
public class StatusMapper {

    // ========================
    // CHECKOUT / RESERVATION
    // ========================

    /**
     * Convert a DB status value to display-friendly English label.
     * Used when READING from the database for JavaFX display.
     */
    public static String toDisplayLabel(String dbStatus) {
        if (dbStatus == null) return "UNKNOWN";
        return switch (dbStatus.toUpperCase().trim()) {
            // Symfony French values → English display
            case "EN_ATTENTE" -> "PENDING";
            case "CONFIRMEE", "CONFIRME", "CONFIRMED" -> "CONFIRMED";
            case "REFUSEE", "REFUSE", "REJECTED" -> "REJECTED";
            case "ANNULEE", "ANNULE", "CANCELLED" -> "CANCELLED";
            case "TRAITEE", "TRAITE" -> "PROCESSED";
            // Already English (backwards compat)
            case "PENDING" -> "PENDING";
            default -> dbStatus.toUpperCase();
        };
    }

    /**
     * Convert a JavaFX English status to Symfony's French DB value.
     * Used when WRITING to the database from JavaFX.
     */
    public static String toDbValue(String displayStatus) {
        if (displayStatus == null) return "EN_ATTENTE";
        return switch (displayStatus.toUpperCase().trim()) {
            case "PENDING", "EN_ATTENTE" -> "EN_ATTENTE";
            case "CONFIRMED", "CONFIRMEE", "CONFIRME" -> "CONFIRMEE";
            case "REJECTED", "REFUSEE", "REFUSE" -> "REFUSEE";
            case "CANCELLED", "ANNULEE", "ANNULE" -> "ANNULEE";
            case "PROCESSED", "TRAITEE", "TRAITE" -> "TRAITEE";
            default -> displayStatus.toUpperCase();
        };
    }

    /**
     * Check if a status (in any language) means "pending".
     */
    public static boolean isPending(String status) {
        if (status == null) return false;
        String s = status.toUpperCase().trim();
        return s.equals("PENDING") || s.equals("EN_ATTENTE");
    }

    /**
     * Check if a status (in any language) means "confirmed".
     */
    public static boolean isConfirmed(String status) {
        if (status == null) return false;
        String s = status.toUpperCase().trim();
        return s.equals("CONFIRMED") || s.equals("CONFIRMEE") || s.equals("CONFIRME");
    }

    /**
     * Check if a status (in any language) means "rejected".
     */
    public static boolean isRejected(String status) {
        if (status == null) return false;
        String s = status.toUpperCase().trim();
        return s.equals("REJECTED") || s.equals("REFUSEE") || s.equals("REFUSE");
    }

    /**
     * Check if a status (in any language) means "cancelled".
     */
    public static boolean isCancelled(String status) {
        if (status == null) return false;
        String s = status.toUpperCase().trim();
        return s.equals("CANCELLED") || s.equals("ANNULEE") || s.equals("ANNULE");
    }
}
