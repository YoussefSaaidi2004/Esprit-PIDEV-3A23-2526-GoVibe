package org.example.entities;

import java.sql.Timestamp;

public class personne {
    private int id;
    private String nom, prenom, email, password, role;
    private Timestamp created_at;

    // --- Champs OAuth2 ---
    private String provider;    // "local" ou "google"
    private String providerId;  // L'ID unique retourné par Google (sub)
    private String photoUrl;    // Photo de profil Google

    // --- Champs MFA ---
    private boolean isAccountLocked;
    private String preferredMfa;  // "NONE" ou "EMAIL"
    private java.sql.Timestamp lockoutUntil;

    // --- Face ID ---
    private String faceEncoding;

    public personne() {
        this.provider = "local"; // Par défaut, inscription classique
    }

    public personne(int id, String nom, String prenom, String email, String password, String role) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.role = role;
        this.provider = "local";
    }

    public personne(String nom, String prenom, String email, String password, String role) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.role = role;
        this.provider = "local";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Timestamp getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Timestamp created_at) {
        this.created_at = created_at;
    }

    // --- Getters/Setters OAuth2 ---

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    // --- Getters/Setters MFA ---

    public boolean isAccountLocked() {
        return isAccountLocked;
    }

    public void setAccountLocked(boolean accountLocked) {
        this.isAccountLocked = accountLocked;
    }

    public java.sql.Timestamp getLockoutUntil() {
        return lockoutUntil;
    }

    public void setLockoutUntil(java.sql.Timestamp lockoutUntil) {
        this.lockoutUntil = lockoutUntil;
    }

    public String getPreferredMfa() {
        return preferredMfa;
    }

    public void setPreferredMfa(String preferredMfa) {
        this.preferredMfa = preferredMfa;
    }

    public String getFaceEncoding() {
        return faceEncoding;
    }

    public void setFaceEncoding(String faceEncoding) {
        this.faceEncoding = faceEncoding;
    }

    /**
     * Vérifie si cet utilisateur est connecté via OAuth2 (Google).
     */
    public boolean isOAuth2User() {
        return provider != null && !"local".equalsIgnoreCase(provider);
    }

    @Override
    public String toString() {
        return "personne{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", provider='" + provider + '\'' +
                ", created_at=" + created_at +
                '}';
    }
}
