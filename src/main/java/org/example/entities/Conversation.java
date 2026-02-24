package org.example.entities;

import java.sql.Timestamp;
import java.util.List;

public class Conversation {
    public static final String STATUS_ACTIF = "ACTIF";
    public static final String STATUS_ARCHIVE = "ARCHIVE";
    public static final String STATUS_FERME = "FERME";
    
    private int id;
    private int clientId;
    private String sujet;
    private String status;
    private Timestamp lastMessageAt;
    private Timestamp createdAt;
    
    // Infos client
    private String clientNom;
    private String clientPrenom;
    private String clientEmail;
    
    // Messages de la conversation
    private List<Message> messages;
    private int nonLusCount;
    
    public Conversation() {
        this.status = STATUS_ACTIF;
    }
    
    public Conversation(int clientId, String sujet) {
        this();
        this.clientId = clientId;
        this.sujet = sujet;
    }
    
    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }
    
    public String getSujet() { return sujet; }
    public void setSujet(String sujet) { this.sujet = sujet; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Timestamp getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Timestamp lastMessageAt) { this.lastMessageAt = lastMessageAt; }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    
    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }
    
    public String getClientPrenom() { return clientPrenom; }
    public void setClientPrenom(String clientPrenom) { this.clientPrenom = clientPrenom; }
    
    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }
    
    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
    
    public int getNonLusCount() { return nonLusCount; }
    public void setNonLusCount(int nonLusCount) { this.nonLusCount = nonLusCount; }
    
    public String getClientNomComplet() {
        return (clientPrenom != null ? clientPrenom : "") + " " + (clientNom != null ? clientNom : "");
    }
    
    public boolean isActif() { return STATUS_ACTIF.equals(status); }
    public boolean isFerme() { return STATUS_FERME.equals(status); }
}
