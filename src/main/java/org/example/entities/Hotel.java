package org.example.entities;

public class Hotel {

    private int id;
    private String nom;
    private String adresse;
    private String ville;
    private int nombreEtoiles;
    private String description;
    private String photoUrl;
    private double budget; // 🔹

    // ======================
    // Constructeurs
    // ======================

    // Constructeur complet avec ID
    public Hotel(int id, String nom, String adresse, String ville,
                 int nombreEtoiles, String description,
                 String photoUrl, double budget) {
        this.id = id;
        this.nom = nom;
        this.adresse = adresse;
        this.ville = ville;
        this.nombreEtoiles = nombreEtoiles;
        this.description = description;
        this.photoUrl = photoUrl;
        this.budget = budget; //
    }

    // Constructeur sans ID
    public Hotel(String nom, String adresse, String ville,
                 int nombreEtoiles, String description,
                 String photoUrl, double budget) {
        this.nom = nom;
        this.adresse = adresse;
        this.ville = ville;
        this.nombreEtoiles = nombreEtoiles;
        this.description = description;
        this.photoUrl = photoUrl;
        this.budget = budget; // 🔹 Initialisation budget
    }

    // ======================
    // Getters & Setters
    // ======================

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

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public int getNombreEtoiles() {
        return nombreEtoiles;
    }

    public void setNombreEtoiles(int nombreEtoiles) {
        this.nombreEtoiles = nombreEtoiles;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public double getBudget() { // 🔹 Getter budget
        return budget;
    }

    public void setBudget(double budget) { // 🔹 Setter budget
        this.budget = budget;
    }

    // ======================
    // toString
    // ======================

    @Override
    public String toString() {
        return nom + " - " + ville + " (" + nombreEtoiles + "★) | Budget: " + budget + " DT";
    }
}
