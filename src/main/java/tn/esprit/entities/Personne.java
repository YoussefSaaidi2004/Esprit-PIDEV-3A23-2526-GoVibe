package tn.esprit.entities;

import java.sql.Timestamp;

public class Personne {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String password;
    private String role;
    private Timestamp created_at;
    private String faceEncoding; // Used for Face ID

    // --- Symfony-compatible fields (all nullable/defaulted) ---
    private String provider = "local";        // OAuth provider: 'local', 'google', etc.
    private String providerId;                // OAuth provider user ID
    private String photoUrl;                  // Profile photo URL
    private boolean isAccountLocked = false;  // Account lockout flag
    private String preferredMfa = "NONE";     // MFA preference: 'NONE', 'EMAIL', 'FACE'
    private Timestamp lockoutUntil;           // Lockout expiry timestamp
    private int absenceCount = 0;             // Number of absences
    private String customerType = "standard"; // 'standard' or 'premium'
    private Timestamp subscriptionExpiresAt;  // Premium subscription expiry
    private int sessionCredits = 0;           // Activity session credits
    private String preferredCategories;       // JSON array of preferred categories
    private String residenceCity;             // User's city of residence

    public Personne() {
    }

    public Personne(int id, String nom, String prenom, String email, String password, String role,
            Timestamp created_at) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.role = role;
        this.created_at = created_at;
    }

    public Personne(int id, String nom, String prenom, String email, String password, String role,
            Timestamp created_at, String faceEncoding) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.role = role;
        this.created_at = created_at;
        this.faceEncoding = faceEncoding;
    }

    public Personne(String nom, String prenom, String email, String password, String role) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    // --- Core getters/setters ---

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

    public String getFaceEncoding() {
        return faceEncoding;
    }

    public void setFaceEncoding(String faceEncoding) {
        this.faceEncoding = faceEncoding;
    }

    // --- Symfony-compatible getters/setters ---

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

    public boolean isAccountLocked() {
        return isAccountLocked;
    }

    public void setAccountLocked(boolean accountLocked) {
        isAccountLocked = accountLocked;
    }

    public String getPreferredMfa() {
        return preferredMfa;
    }

    public void setPreferredMfa(String preferredMfa) {
        this.preferredMfa = preferredMfa;
    }

    public Timestamp getLockoutUntil() {
        return lockoutUntil;
    }

    public void setLockoutUntil(Timestamp lockoutUntil) {
        this.lockoutUntil = lockoutUntil;
    }

    public int getAbsenceCount() {
        return absenceCount;
    }

    public void setAbsenceCount(int absenceCount) {
        this.absenceCount = absenceCount;
    }

    public String getCustomerType() {
        return customerType;
    }

    public void setCustomerType(String customerType) {
        this.customerType = customerType;
    }

    public Timestamp getSubscriptionExpiresAt() {
        return subscriptionExpiresAt;
    }

    public void setSubscriptionExpiresAt(Timestamp subscriptionExpiresAt) {
        this.subscriptionExpiresAt = subscriptionExpiresAt;
    }

    public int getSessionCredits() {
        return sessionCredits;
    }

    public void setSessionCredits(int sessionCredits) {
        this.sessionCredits = sessionCredits;
    }

    public String getPreferredCategories() {
        return preferredCategories;
    }

    public void setPreferredCategories(String preferredCategories) {
        this.preferredCategories = preferredCategories;
    }

    public String getResidenceCity() {
        return residenceCity;
    }

    public void setResidenceCity(String residenceCity) {
        this.residenceCity = residenceCity;
    }

    @Override
    public String toString() {
        return "Personne{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", created_at=" + created_at +
                '}';
    }
}
