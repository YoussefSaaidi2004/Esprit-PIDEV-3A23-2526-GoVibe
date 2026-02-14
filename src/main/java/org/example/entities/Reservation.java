package org.example.entities;

import java.time.LocalDate;

public class Reservation {

    private int id;
    private int userId;
    private String userNom;
    private String userPrenom;
    private String userEmail;
    private int chambreId;
    private int hotelId;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private double prixTotal;
    private String statut; // "EN_ATTENTE", "CONFIRMEE", "ANNULEE"

    // Constructeurs
    public Reservation() {}

    public Reservation(int userId, int chambreId, int hotelId,
                      LocalDate dateDebut, LocalDate dateFin,
                      double prixTotal, String statut) {
        this.userId = userId;
        this.chambreId = chambreId;
        this.hotelId = hotelId;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.prixTotal = prixTotal;
        this.statut = statut;
    }

    public Reservation(int id, int userId, int chambreId, int hotelId,
                      LocalDate dateDebut, LocalDate dateFin,
                      double prixTotal, String statut) {
        this(userId, chambreId, hotelId, dateDebut, dateFin, prixTotal, statut);
        this.id = id;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserNom() { return userNom; }
    public void setUserNom(String userNom) { this.userNom = userNom; }

    public String getUserPrenom() { return userPrenom; }
    public void setUserPrenom(String userPrenom) { this.userPrenom = userPrenom; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserDisplayName() {
        String prenom = userPrenom != null ? userPrenom.trim() : "";
        String nom = userNom != null ? userNom.trim() : "";
        String fullName = (prenom + " " + nom).trim();
        return fullName.isEmpty() ? "Utilisateur #" + userId : fullName;
    }

    public int getChambreId() { return chambreId; }
    public void setChambreId(int chambreId) { this.chambreId = chambreId; }

    public int getHotelId() { return hotelId; }
    public void setHotelId(int hotelId) { this.hotelId = hotelId; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public double getPrixTotal() { return prixTotal; }
    public void setPrixTotal(double prixTotal) { this.prixTotal = prixTotal; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    @Override
    public String toString() {
        return "Réservation #" + id + " - " + getUserDisplayName() + " | " + dateDebut + " → " + dateFin;
    }
}

