package org.example.services;

import org.example.entities.personne;

/**
 * MFA Decision Engine.
 * Determines the authentication level based on risk probability and user role.
 *
 * Levels:
 *   LOW       → Direct login (no additional verification)
 *   HIGH      → OTP verification by email required
 *   VERY_HIGH → OTP verification by email required (highest risk)
 *
 * Rules:
 *   - risk_probability < 0.30   → LOW  (but HIGH if admin)
 *   - 0.30 <= risk_prob < 0.70  → HIGH
 *   - risk_probability >= 0.70  → VERY_HIGH
 *   - Admin users always get at least HIGH
 */
public class MFAService {

    /**
     * Authentication security levels.
     */
    public enum AuthLevel {
        LOW,        // Direct login
        HIGH,       // OTP required
        VERY_HIGH   // OTP required (max risk)
    }

    /**
     * Determines the authentication level based on AI risk score and user role.
     */
    public AuthLevel determineAuthLevel(personne user, double riskProbability) {
        boolean isAdmin = "admin".equalsIgnoreCase(user.getRole());

        AuthLevel level;

        if (riskProbability >= 0.70) {
            level = AuthLevel.VERY_HIGH;
        } else if (riskProbability >= 0.30) {
            level = AuthLevel.HIGH;
        } else {
            level = AuthLevel.LOW;
        }

        // Admin override: minimum HIGH
        if (isAdmin && level == AuthLevel.LOW) {
            level = AuthLevel.HIGH;
            System.out.println("🔒 [MFA] Admin override: LOW → HIGH");
        }

        System.out.println("🛡️ [MFA] User=" + user.getEmail() +
                           " | Role=" + user.getRole() +
                           " | RiskProb=" + String.format("%.4f", riskProbability) +
                           " | AuthLevel=" + level);

        return level;
    }

    /**
     * Returns a human-readable description of the auth level for display.
     */
    public String getAuthLevelDescription(AuthLevel level) {
        switch (level) {
            case LOW:
                return "Connexion directe — Risque faible";
            case HIGH:
                return "Vérification OTP requise — Risque moyen";
            case VERY_HIGH:
                return "Vérification OTP requise — Risque élevé";
            default:
                return "Inconnu";
        }
    }
}
