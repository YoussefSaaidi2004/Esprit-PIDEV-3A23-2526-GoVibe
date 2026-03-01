package org.example.entities;

import java.time.LocalDate;

/**
 * 💡 Code Promo Entity — GoVibe Hotel Module
 * Supports both percentage and fixed-amount discounts.
 */
public class CodePromo {

    public enum TypeRemise {
        POURCENTAGE,   // e.g. 10% off
        MONTANT_FIXE   // e.g. 50 DT off
    }

    private int id;
    private String code;
    private String description;
    private TypeRemise type;
    private double valeur;
    private LocalDate dateExpiration;
    private boolean actif;
    private int utilisationsMax;
    private int utilisationsActuelles;

    public CodePromo() {}

    public CodePromo(int id, String code, String description, TypeRemise type,
                     double valeur, LocalDate dateExpiration, boolean actif,
                     int utilisationsMax, int utilisationsActuelles) {
        this.id = id;
        this.code = code;
        this.description = description;
        this.type = type;
        this.valeur = valeur;
        this.dateExpiration = dateExpiration;
        this.actif = actif;
        this.utilisationsMax = utilisationsMax;
        this.utilisationsActuelles = utilisationsActuelles;
    }

    // ---- Computed properties ----

    /**
     * Checks if this code is still valid today.
     */
    public boolean isValide() {
        if (!actif) return false;
        if (utilisationsActuelles >= utilisationsMax) return false;
        if (dateExpiration != null && LocalDate.now().isAfter(dateExpiration)) return false;
        return true;
    }

    /**
     * Calculate the discount to apply on a given price.
     */
    public double calculerRemise(double prixOriginal) {
        if (!isValide()) return 0;
        if (type == TypeRemise.POURCENTAGE) {
            return prixOriginal * (valeur / 100.0);
        } else {
            return Math.min(valeur, prixOriginal); // can't discount more than price
        }
    }

    /**
     * Calculate final price after applying this promo code.
     */
    public double appliquerSur(double prixOriginal) {
        return Math.max(0, prixOriginal - calculerRemise(prixOriginal));
    }

    public String getDisplayLabel() {
        if (type == TypeRemise.POURCENTAGE) {
            return code + " — " + (int) valeur + "% de réduction";
        } else {
            return code + " — " + (int) valeur + " DT de réduction";
        }
    }

    // ---- Getters & Setters ----
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TypeRemise getType() { return type; }
    public void setType(TypeRemise type) { this.type = type; }
    public double getValeur() { return valeur; }
    public void setValeur(double valeur) { this.valeur = valeur; }
    public LocalDate getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDate dateExpiration) { this.dateExpiration = dateExpiration; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
    public int getUtilisationsMax() { return utilisationsMax; }
    public void setUtilisationsMax(int utilisationsMax) { this.utilisationsMax = utilisationsMax; }
    public int getUtilisationsActuelles() { return utilisationsActuelles; }
    public void setUtilisationsActuelles(int utilisationsActuelles) { this.utilisationsActuelles = utilisationsActuelles; }

    @Override
    public String toString() {
        return getDisplayLabel();
    }
}
