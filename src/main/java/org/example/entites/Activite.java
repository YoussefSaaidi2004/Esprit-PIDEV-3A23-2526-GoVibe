package org.example.entites;

import java.math.BigDecimal;

public class Activite {

    private int id;
    private String name;
    private String description;
    private String type;
    private String localisation;
    private BigDecimal prix;
    private String status; // 'Confirmed' or 'Pending'

    public static final String STATUS_CONFIRMED = "Confirmed";
    public static final String STATUS_PENDING = "Pending";

    public Activite() {
    }

    public Activite(int id, String name, String description, String type, String localisation, BigDecimal prix,
            String status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.localisation = localisation;
        this.prix = prix;
        this.status = status;
    }

    // Constructor without ID (for insertion)
    public Activite(String name, String description, String type, String localisation, BigDecimal prix, String status) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.localisation = localisation;
        this.prix = prix;
        this.status = status;
    }

    // Constructor for backward compatibility (defaults to Confirmed if not
    // specified)
    public Activite(int id, String name, String description, String type, String localisation, BigDecimal prix) {
        this(id, name, description, type, localisation, prix, STATUS_CONFIRMED);
    }

    public Activite(String name, String description, String type, String localisation, BigDecimal prix) {
        this(name, description, type, localisation, prix, STATUS_CONFIRMED);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLocalisation() {
        return localisation;
    }

    public void setLocalisation(String localisation) {
        this.localisation = localisation;
    }

    public BigDecimal getPrix() {
        return prix;
    }

    public void setPrix(BigDecimal prix) {
        this.prix = prix;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Activite{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", type='" + type + '\'' +
                ", localisation='" + localisation + '\'' +
                ", prix=" + prix +
                ", status='" + status + '\'' +
                '}';
    }
}