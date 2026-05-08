package tn.esprit.entities;

import java.sql.Timestamp;

public class Membre {
    private int forum_id;
    private int user_id;
    private Timestamp date_adhesion;
    private String status = "PENDING"; // Matches Symfony MembreForum.status: PENDING, APPROVED, REJECTED

    public Membre() {
    }

    public Membre(int forum_id, int user_id, Timestamp date_adhesion) {
        this.forum_id = forum_id;
        this.user_id = user_id;
        this.date_adhesion = date_adhesion;
    }

    public Membre(int forum_id, int user_id, Timestamp date_adhesion, String status) {
        this.forum_id = forum_id;
        this.user_id = user_id;
        this.date_adhesion = date_adhesion;
        this.status = status;
    }

    // Constructor without date (for insertion)
    public Membre(int forum_id, int user_id) {
        this.forum_id = forum_id;
        this.user_id = user_id;
    }

    public int getForum_id() {
        return forum_id;
    }

    public void setForum_id(int forum_id) {
        this.forum_id = forum_id;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public Timestamp getDate_adhesion() {
        return date_adhesion;
    }

    public void setDate_adhesion(Timestamp date_adhesion) {
        this.date_adhesion = date_adhesion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Membre{" +
                "forum_id=" + forum_id +
                ", user_id=" + user_id +
                ", date_adhesion=" + date_adhesion +
                ", status='" + status + '\'' +
                '}';
    }
}
