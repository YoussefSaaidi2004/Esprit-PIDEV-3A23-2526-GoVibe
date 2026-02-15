package org.example.entities;

import java.sql.Date;
import java.sql.Time;

public class Session {

    private int id_session;
    private Date date;
    private Time heure;
    private int capacite;
    private int nbr_places_restant;

    // Clé étrangère vers Activite (ID seul + optionnel objet Activite)
    private int activite_id;
    private Activite activite; // facultatif (utile pour jointures)

    public Session() {
    }

    // Constructeur complet
    public Session(int id_session, Date date, Time heure, int capacite, int nbr_places_restant, int activite_id) {
        this.id_session = id_session;
        this.date = date;
        this.heure = heure;
        this.capacite = capacite;
        this.nbr_places_restant = nbr_places_restant;
        this.activite_id = activite_id;
    }

    // Constructeur sans ID
    public Session(Date date, Time heure, int capacite, int nbr_places_restant, int activite_id) {
        this.date = date;
        this.heure = heure;
        this.capacite = capacite;
        this.nbr_places_restant = nbr_places_restant;
        this.activite_id = activite_id;
    }

    public int getId_session() {
        return id_session;
    }

    public void setId_session(int id_session) {
        this.id_session = id_session;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Time getHeure() {
        return heure;
    }

    public void setHeure(Time heure) {
        this.heure = heure;
    }

    public int getCapacite() {
        return capacite;
    }

    public void setCapacite(int capacite) {
        this.capacite = capacite;
    }

    public int getNbr_places_restant() {
        return nbr_places_restant;
    }

    public void setNbr_places_restant(int nbr_places_restant) {
        this.nbr_places_restant = nbr_places_restant;
    }

    public int getActivite_id() {
        return activite_id;
    }

    public void setActivite_id(int activite_id) {
        this.activite_id = activite_id;
    }

    public Activite getActivite() {
        return activite;
    }

    public void setActivite(Activite activite) {
        this.activite = activite;
    }

    @Override
    public String toString() {
        return "Session{" +
                "id_session=" + id_session +
                ", date=" + date +
                ", heure=" + heure +
                ", capacite=" + capacite +
                ", nbr_places_restant=" + nbr_places_restant +
                ", activite_id=" + activite_id +
                '}';
    }
}