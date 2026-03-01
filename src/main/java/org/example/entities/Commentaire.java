package org.example.entities;

import java.sql.Timestamp;

public class Commentaire {
    private int commentaire_id;
    private int post_id;
    private int user_id;
    private String contenu;
    private Timestamp date_creation;
    private int parent_id; // 0 = top-level
    private int likes;
    private int dislikes;

    public Commentaire() {}

    public Commentaire(int post_id, int user_id, String contenu) {
        this.post_id = post_id;
        this.user_id = user_id;
        this.contenu = contenu;
        this.date_creation = new Timestamp(System.currentTimeMillis());
    }

    public Commentaire(int post_id, int user_id, String contenu, int parent_id) {
        this(post_id, user_id, contenu);
        this.parent_id = parent_id;
    }

    public int getCommentaire_id() { return commentaire_id; }
    public void setCommentaire_id(int commentaire_id) { this.commentaire_id = commentaire_id; }

    public int getPost_id() { return post_id; }
    public void setPost_id(int post_id) { this.post_id = post_id; }

    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public Timestamp getDate_creation() { return date_creation; }
    public void setDate_creation(Timestamp date_creation) { this.date_creation = date_creation; }

    public int getParent_id() { return parent_id; }
    public void setParent_id(int parent_id) { this.parent_id = parent_id; }

    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public int getDislikes() { return dislikes; }
    public void setDislikes(int dislikes) { this.dislikes = dislikes; }
}
