package org.example.entities;


public class Chambre {

    private int id;
    private String type;
    private int capacite;
    private String equipements;
    private int hotelId;

    // Gestion prix par période
    private double prixStandard;
    private double prixHauteSaison;
    private double prixBasseSaison;

    // Constructeurs
    public Chambre() {}

    public Chambre(String type, int capacite, String equipements, int hotelId,
                   double prixStandard, double prixHauteSaison, double prixBasseSaison) {
        this.type = type;
        this.capacite = capacite;
        this.equipements = equipements;
        this.hotelId = hotelId;
        this.prixStandard = prixStandard;
        this.prixHauteSaison = prixHauteSaison;
        this.prixBasseSaison = prixBasseSaison;
    }

    public Chambre(int id, String type, int capacite, String equipements, int hotelId,
                   double prixStandard, double prixHauteSaison, double prixBasseSaison) {
        this(type, capacite, equipements, hotelId, prixStandard, prixHauteSaison, prixBasseSaison);
        this.id = id;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) { this.capacite = capacite; }
    public String getEquipements() { return equipements; }
    public void setEquipements(String equipements) { this.equipements = equipements; }
    public int getHotelId() { return hotelId; }
    public void setHotelId(int hotelId) { this.hotelId = hotelId; }
    public double getPrixStandard() { return prixStandard; }
    public void setPrixStandard(double prixStandard) { this.prixStandard = prixStandard; }
    public double getPrixHauteSaison() { return prixHauteSaison; }
    public void setPrixHauteSaison(double prixHauteSaison) { this.prixHauteSaison = prixHauteSaison; }
    public double getPrixBasseSaison() { return prixBasseSaison; }
    public void setPrixBasseSaison(double prixBasseSaison) { this.prixBasseSaison = prixBasseSaison; }

    @Override
    public String toString() {
        return "Chambre " + type + " | " + capacite + " pers | Prix standard: " + prixStandard + " DT";
    }
}

