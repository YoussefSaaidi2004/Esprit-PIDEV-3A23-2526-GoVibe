package org.example.entities;

import java.sql.Timestamp;

public class Reclamation {
    
    public static final String STATUS_EN_ATTENTE = "EN_ATTENTE";
    public static final String STATUS_EN_COURS = "EN_COURS";
    public static final String STATUS_RESOLU = "RESOLU";
    public static final String STATUS_REJETE = "REJETE";
    
    private int id;
    private int userId;
    private String sujet;
    private String message;
    private String reponse;
    private String status;
    private Timestamp dateEnvoi;
    private Timestamp dateReponse;
    private int createdByUser;
    
    // User info (for display)
    private String userNom;
    private String userPrenom;
    private String userEmail;
    
    public Reclamation() {
        this.status = STATUS_EN_ATTENTE;
    }
    
    public Reclamation(int userId, String sujet, String message) {
        this.userId = userId;
        this.sujet = sujet;
        this.message = message;
        this.status = STATUS_EN_ATTENTE;
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
    
    public int getCreatedByUser() {
        return createdByUser;
    }
    
    public void setCreatedByUser(int createdByUser) {
        this.createdByUser = createdByUser;
    }
    
    public String getUserNom() {
        return userNom;
    }
    
    public void setUserNom(String userNom) {
        this.userNom = userNom;
    }
    
    public String getUserPrenom() {
        return userPrenom;
    }
    
    public void setUserPrenom(String userPrenom) {
        this.userPrenom = userPrenom;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public String getUserNomComplet() {
        return userPrenom + " " + userNom;
    }
    
    public boolean isEnAttente() {
        return STATUS_EN_ATTENTE.equals(status);
    }
    
    public boolean isResolu() {
        return STATUS_RESOLU.equals(status);
    }
    
    @Override
    public String toString() {
        return "Reclamation{" +
                "id=" + id +
                ", sujet='" + sujet + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
