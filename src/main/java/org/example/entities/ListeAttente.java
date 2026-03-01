package org.example.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 6️⃣ Liste d'Attente Entity — GoVibe Hotel Module
 * Manages waitlist when a hotel is fully booked.
 */
public class ListeAttente {

    public enum StatutAttente {
        EN_ATTENTE,  // Waiting for a room
        NOTIFIE,     // Client has been notified (room available)
        CONFIRME,    // Client confirmed the room
        EXPIRE       // Notification expired
    }

    private int id;
    private int userId;
    private String userName;
    private String userEmail;
    private int hotelId;
    private String hotelNom;
    private int capaciteSouhaitee;
    private double budgetMax;
    private LocalDate dateSouhaiteeDebut;
    private LocalDate dateSouhaiteeFin;
    private StatutAttente statut;
    private Integer chambreProposeeId;
    private LocalDateTime dateCreation;
    private LocalDateTime dateNotification;

    public ListeAttente() {}

    public ListeAttente(int userId, int hotelId, int capaciteSouhaitee, double budgetMax,
                        LocalDate dateSouhaiteeDebut, LocalDate dateSouhaiteeFin) {
        this.userId = userId;
        this.hotelId = hotelId;
        this.capaciteSouhaitee = capaciteSouhaitee;
        this.budgetMax = budgetMax;
        this.dateSouhaiteeDebut = dateSouhaiteeDebut;
        this.dateSouhaiteeFin = dateSouhaiteeFin;
        this.statut = StatutAttente.EN_ATTENTE;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public int getHotelId() { return hotelId; }
    public void setHotelId(int hotelId) { this.hotelId = hotelId; }
    public String getHotelNom() { return hotelNom; }
    public void setHotelNom(String hotelNom) { this.hotelNom = hotelNom; }
    public int getCapaciteSouhaitee() { return capaciteSouhaitee; }
    public void setCapaciteSouhaitee(int capaciteSouhaitee) { this.capaciteSouhaitee = capaciteSouhaitee; }
    public double getBudgetMax() { return budgetMax; }
    public void setBudgetMax(double budgetMax) { this.budgetMax = budgetMax; }
    public LocalDate getDateSouhaiteeDebut() { return dateSouhaiteeDebut; }
    public void setDateSouhaiteeDebut(LocalDate d) { this.dateSouhaiteeDebut = d; }
    public LocalDate getDateSouhaiteeFin() { return dateSouhaiteeFin; }
    public void setDateSouhaiteeFin(LocalDate d) { this.dateSouhaiteeFin = d; }
    public StatutAttente getStatut() { return statut; }
    public void setStatut(StatutAttente statut) { this.statut = statut; }
    public Integer getChambreProposeeId() { return chambreProposeeId; }
    public void setChambreProposeeId(Integer chambreProposeeId) { this.chambreProposeeId = chambreProposeeId; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    public LocalDateTime getDateNotification() { return dateNotification; }
    public void setDateNotification(LocalDateTime dateNotification) { this.dateNotification = dateNotification; }

    public String getStatutEmoji() {
        switch (statut) {
            case NOTIFIE: return "🔔 Notifié";
            case CONFIRME: return "✅ Confirmé";
            case EXPIRE: return "⏰ Expiré";
            default: return "⏳ En Attente";
        }
    }

    @Override
    public String toString() {
        return "Attente #" + id + " — User " + userId + " | Hôtel " + hotelId + " | " + statut;
    }
}
