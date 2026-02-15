package org.example.entities;

import java.time.LocalDateTime;

public class Voiture {

    private int idVoiture;
    private String matricule;
    private String marque;
    private String modele;
    private int annee;
    private TypeCarburant typeCarburant;
    private double prixJour;
    private Statut statut;

    private String adresseAgence;
    private double latitude;
    private double longitude;

    private String description;
    private String imageUrl;
    private LocalDateTime dateCreation;

    // Constructeur vide (obligatoire pour JDBC)
    public Voiture() {
    }

    // Constructeur pour INSERT
    public Voiture(String matricule, String marque, String modele, int annee,
                   TypeCarburant typeCarburant, double prixJour, Statut statut,
                   String adresseAgence, double latitude, double longitude,
                   String description, String imageUrl) {

        this.matricule = matricule;
        this.marque = marque;
        this.modele = modele;
        this.annee = annee;
        this.typeCarburant = typeCarburant;
        this.prixJour = prixJour;
        this.statut = statut;
        this.adresseAgence = adresseAgence;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    // Constructeur pour SELECT
    public Voiture(int idVoiture, String matricule, String marque, String modele,
                   int annee, TypeCarburant typeCarburant, double prixJour, Statut statut,
                   String adresseAgence, double latitude, double longitude,
                   String description, String imageUrl, LocalDateTime dateCreation) {

        this.idVoiture = idVoiture;
        this.matricule = matricule;
        this.marque = marque;
        this.modele = modele;
        this.annee = annee;
        this.typeCarburant = typeCarburant;
        this.prixJour = prixJour;
        this.statut = statut;
        this.adresseAgence = adresseAgence;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.imageUrl = imageUrl;
        this.dateCreation = dateCreation;
    }

    // Getters & Setters

    public int getIdVoiture() {
        return idVoiture;
    }

    public void setIdVoiture(int idVoiture) {
        this.idVoiture = idVoiture;
    }

    public String getMatricule() {
        return matricule;
    }

    public void setMatricule(String matricule) {
        this.matricule = matricule;
    }

    public String getMarque() {
        return marque;
    }

    public void setMarque(String marque) {
        this.marque = marque;
    }

    public String getModele() {
        return modele;
    }

    public void setModele(String modele) {
        this.modele = modele;
    }

    public int getAnnee() {
        return annee;
    }

    public void setAnnee(int annee) {
        this.annee = annee;
    }

    public TypeCarburant getTypeCarburant() {
        return typeCarburant;
    }

    public void setTypeCarburant(TypeCarburant typeCarburant) {
        this.typeCarburant = typeCarburant;
    }

    public double getPrixJour() {
        return prixJour;
    }

    public void setPrixJour(double prixJour) {
        this.prixJour = prixJour;
    }

    public Statut getStatut() {
        return statut;
    }

    public void setStatut(Statut statut) {
        this.statut = statut;
    }

    public String getAdresseAgence() {
        return adresseAgence;
    }

    public void setAdresseAgence(String adresseAgence) {
        this.adresseAgence = adresseAgence;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    @Override
    public String toString() {
        return "Voiture{" +
                "idVoiture=" + idVoiture +
                ", matricule='" + matricule + '\'' +
                ", marque='" + marque + '\'' +
                ", modele='" + modele + '\'' +
                ", annee=" + annee +
                ", prixJour=" + prixJour +
                ", statut='" + statut + '\'' +
                '}';
    }
}
