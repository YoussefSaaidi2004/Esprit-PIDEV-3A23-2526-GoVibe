package org.example.entities;

import java.time.LocalDate;

public class Reservation {

    private int id;
    private String clientNom;
    private String clientEmail;
    private String clientTelephone;
    private int chambreId;
    private int hotelId;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private double prixTotal;
    private String statut; // "EN_ATTENTE", "CONFIRMEE", "ANNULEE"

    // Constructeurs
    public Reservation() {}

    public Reservation(String clientNom, String clientEmail, String clientTelephone,
                      int chambreId, int hotelId, LocalDate dateDebut, LocalDate dateFin,
                      double prixTotal, String statut) {
        this.clientNom = clientNom;
        this.clientEmail = clientEmail;
        this.clientTelephone = clientTelephone;
        this.chambreId = chambreId;
        this.hotelId = hotelId;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.prixTotal = prixTotal;
        this.statut = statut;
    }

    public Reservation(int id, String clientNom, String clientEmail, String clientTelephone,
                      int chambreId, int hotelId, LocalDate dateDebut, LocalDate dateFin,
                      double prixTotal, String statut) {
        this(clientNom, clientEmail, clientTelephone, chambreId, hotelId, dateDebut, dateFin, prixTotal, statut);
        this.id = id;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }

    public String getClientTelephone() { return clientTelephone; }
    public void setClientTelephone(String clientTelephone) { this.clientTelephone = clientTelephone; }

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
        return "Réservation #" + id + " - " + clientNom + " | " + dateDebut + " → " + dateFin;
    }
}

