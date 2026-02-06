package org.example.entites;
import java.math.BigDecimal;

public class Activite {

    private int id;
    private String name;
    private String description;
    private String type;
    private String localisation;
    private BigDecimal prix;

    public Activite() {
    }

    public Activite(int id, String name, String description, String type, String localisation, BigDecimal prix) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.localisation = localisation;
        this.prix = prix;
    }

    public Activite( String name, String description, String type, String localisation, BigDecimal prix) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.localisation = localisation;
        this.prix = prix;
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
    @Override
    public String toString() {
        return "Activite {" + "id=" +id + ", name=" + name + ", description=" + description + ", type=" + type + ", localisation=" + localisation + ", prix=" + prix + '}';
    }
}
