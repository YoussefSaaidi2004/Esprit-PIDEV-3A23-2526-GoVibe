package org.example.entities;

/**
 * 🎯 Programme Fidélité Entity — GoVibe Hotel Module
 * Tracks loyalty points, status, and rewards for clients.
 */
public class ProgrammeFidelite {

    public enum StatutFidelite {
        BRONZE,    // 0 – 499 points
        SILVER,    // 500 – 1499 points  (5% off)
        GOLD,      // 1500 – 2999 points (10% off)
        PLATINUM   // 3000+ points       (15% off + priority)
    }

    private static final int POINTS_PER_DT = 1;         // 1 pt per DT spent
    private static final int SILVER_THRESHOLD  = 500;
    private static final int GOLD_THRESHOLD    = 1500;
    private static final int PLATINUM_THRESHOLD = 3000;

    private int id;
    private int userId;
    private String userName;
    private int points;
    private StatutFidelite statut;
    private int totalReservations;
    private double totalDepenses;

    public ProgrammeFidelite() {}

    public ProgrammeFidelite(int userId, int points, StatutFidelite statut,
                              int totalReservations, double totalDepenses) {
        this.userId = userId;
        this.points = points;
        this.statut = statut;
        this.totalReservations = totalReservations;
        this.totalDepenses = totalDepenses;
    }

    // ---- Computed ----

    public StatutFidelite calculerStatut() {
        if (points >= PLATINUM_THRESHOLD) return StatutFidelite.PLATINUM;
        if (points >= GOLD_THRESHOLD)     return StatutFidelite.GOLD;
        if (points >= SILVER_THRESHOLD)   return StatutFidelite.SILVER;
        return StatutFidelite.BRONZE;
    }

    /** Points earned for a given reservation price */
    public static int calculerPointsGagnes(double prix) {
        return (int) Math.floor(prix * POINTS_PER_DT);
    }

    /** Discount percentage based on current status */
    public double getReductionPourcentage() {
        switch (calculerStatut()) {
            case PLATINUM: return 15.0;
            case GOLD:     return 10.0;
            case SILVER:   return 5.0;
            default:       return 0.0;
        }
    }

    /** Points needed to reach next level */
    public int getPointsVersProchainNiveau() {
        int pts = points;
        if (pts < SILVER_THRESHOLD)    return SILVER_THRESHOLD  - pts;
        if (pts < GOLD_THRESHOLD)      return GOLD_THRESHOLD    - pts;
        if (pts < PLATINUM_THRESHOLD)  return PLATINUM_THRESHOLD - pts;
        return 0; // Already Platinum
    }

    public String getStatutEmoji() {
        switch (calculerStatut()) {
            case PLATINUM: return "💎 Platinum";
            case GOLD:     return "🥇 Gold";
            case SILVER:   return "🥈 Silver";
            default:       return "🥉 Bronze";
        }
    }

    public String getStatutColor() {
        switch (calculerStatut()) {
            case PLATINUM: return "#B9F2FF";
            case GOLD:     return "#FFD700";
            case SILVER:   return "#C0C0C0";
            default:       return "#CD7F32";
        }
    }

    // ---- Getters & Setters ----
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; this.statut = calculerStatut(); }
    public StatutFidelite getStatut() { return calculerStatut(); }
    public void setStatut(StatutFidelite statut) { this.statut = statut; }
    public int getTotalReservations() { return totalReservations; }
    public void setTotalReservations(int totalReservations) { this.totalReservations = totalReservations; }
    public double getTotalDepenses() { return totalDepenses; }
    public void setTotalDepenses(double totalDepenses) { this.totalDepenses = totalDepenses; }
}
