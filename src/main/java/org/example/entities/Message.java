package org.example.entities;

import java.sql.Timestamp;

public class Message {
    public static final String TYPE_TEXTE = "TEXTE";
    public static final String TYPE_IMAGE = "IMAGE";
    public static final String TYPE_FICHIER = "FICHIER";
    
    public static final String SENDER_CLIENT = "CLIENT";
    public static final String SENDER_ADMIN = "ADMIN";
    
    public static final String STATUS_ENVOYE = "ENVOYE";
    public static final String STATUS_RECU = "RECU";
    public static final String STATUS_LU = "LU";
    
    private int id;
    private int conversationId;
    private int senderId;
    private String senderType;
    private String content;
    private String type;
    private String status;
    private Timestamp createdAt;
    private Timestamp luAt;
    
    // Infos expéditeur
    private String senderNom;
    private String senderPrenom;
    
    public Message() {
        this.type = TYPE_TEXTE;
        this.status = STATUS_ENVOYE;
    }
    
    public Message(int conversationId, int senderId, String senderType, String content) {
        this();
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderType = senderType;
        this.content = content;
    }
    
    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getConversationId() { return conversationId; }
    public void setConversationId(int conversationId) { this.conversationId = conversationId; }
    
    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }
    
    public String getSenderType() { return senderType; }
    public void setSenderType(String senderType) { this.senderType = senderType; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    
    public Timestamp getLuAt() { return luAt; }
    public void setLuAt(Timestamp luAt) { this.luAt = luAt; }
    
    public String getSenderNom() { return senderNom; }
    public void setSenderNom(String senderNom) { this.senderNom = senderNom; }
    
    public String getSenderPrenom() { return senderPrenom; }
    public void setSenderPrenom(String senderPrenom) { this.senderPrenom = senderPrenom; }
    
    public boolean isFromClient() { return SENDER_CLIENT.equals(senderType); }
    public boolean isFromAdmin() { return SENDER_ADMIN.equals(senderType); }
    public boolean isLu() { return STATUS_LU.equals(status); }
    
    public String getSenderNomComplet() {
        return (senderPrenom != null ? senderPrenom : "") + " " + (senderNom != null ? senderNom : "");
    }
}
