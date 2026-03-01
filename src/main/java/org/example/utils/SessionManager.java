package org.example.utils;

import org.example.entities.personne;

public final class SessionManager {

    private static personne currentUser;
    private static String sessionId;

    // Holds user during MFA verification (between password check and OTP)
    private static personne pendingMfaUser;
    private static double pendingRiskScore;
    private static String pendingAuthLevel;

    private SessionManager() {
    }

    public static personne getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(personne user) {
        currentUser = user;
    }

    public static String getSessionId() {
        return sessionId;
    }

    public static void setSessionId(String id) {
        sessionId = id;
    }

    public static void clear() {
        currentUser = null;
        sessionId = null;
        pendingMfaUser = null;
        pendingRiskScore = 0;
        pendingAuthLevel = null;
    }

    // --- Pending MFA state ---

    public static personne getPendingMfaUser() {
        return pendingMfaUser;
    }

    public static void setPendingMfaUser(personne user) {
        pendingMfaUser = user;
    }

    public static double getPendingRiskScore() {
        return pendingRiskScore;
    }

    public static void setPendingRiskScore(double score) {
        pendingRiskScore = score;
    }

    public static String getPendingAuthLevel() {
        return pendingAuthLevel;
    }

    public static void setPendingAuthLevel(String level) {
        pendingAuthLevel = level;
    }

    public static void clearPendingMfa() {
        pendingMfaUser = null;
        pendingRiskScore = 0;
        pendingAuthLevel = null;
    }

    public static boolean isAuthenticated() {
        return currentUser != null;
    }

    public static boolean isAdmin() {
        return currentUser != null && "admin".equalsIgnoreCase(currentUser.getRole());
    }
}
