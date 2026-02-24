package org.example.utils;

import org.example.entities.personne;

public final class SessionManager {

    private static personne currentUser;

    private SessionManager() {
    }

    public static personne getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(personne user) {
        currentUser = user;
    }

    public static void clear() {
        currentUser = null;
    }

    public static boolean isAuthenticated() {
        return currentUser != null;
    }

    public static boolean isAdmin() {
        return currentUser != null && "admin".equalsIgnoreCase(currentUser.getRole());
    }

    // ✅ NOUVEAU : Get user reference for activity reservations
    public static String getCurrentUserRef() {
        if (currentUser != null) {
            return "USER" + currentUser.getId();
        }
        return null;
    }
}
