package org.example.entities;

import java.sql.Timestamp;

public class Contact {

    public static final String STATUS_EN_ATTENTE = "EN_ATTENTE";
    public static final String STATUS_LU = "LU";
    public static final String STATUS_REPONDU = "REPONDU";
    public static final String STATUS_FERME = "FERME";

    private int id;
    private int userId;
    private String sujet;
    private String message;
    private String reponse;
    private String status;
    private Timestamp dateEnvoi;
    private Timestamp dateReponse;
    private boolean createdByUser;

    // Infos supplémentaires pour affichage
    private String userNom;
    private String userEmail;

    public Contact() {
        this.status = STATUS_EN_ATTENTE;
        this.createdByUser = true;
    }

    public Contact(int userId, String sujet, String message) {
        this();
        this.userId = userId;
        this.sujet = sujet;
        this.message = message;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getSujet() {
        return sujet;
    }

    public void setSujet(String sujet) {
        this.sujet = sujet;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReponse() {
        return reponse;
    }

    public void setReponse(String reponse) {
        this.reponse = reponse;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getDateEnvoi() {
        return dateEnvoi;
    }

    public void setDateEnvoi(Timestamp dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }

    public Timestamp getDateReponse() {
        return dateReponse;
    }

    public void setDateReponse(Timestamp dateReponse) {
        this.dateReponse = dateReponse;
    }

    public boolean isCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(boolean createdByUser) {
        this.createdByUser = createdByUser;
    }

    public String getUserNom() {
        return userNom;
    }

    public void setUserNom(String userNom) {
        this.userNom = userNom;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public boolean hasReponse() {
        return reponse != null && !reponse.isEmpty();
    }

    @Override
    public String toString() {
        return "Contact{" +
                "id=" + id +
                ", userId=" + userId +
                ", sujet='" + sujet + '\'' +
                ", status='" + status + '\'' +
                ", dateEnvoi=" + dateEnvoi +
                '}';
    }
}
