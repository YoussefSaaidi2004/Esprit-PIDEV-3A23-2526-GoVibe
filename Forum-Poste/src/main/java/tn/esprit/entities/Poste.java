package tn.esprit.entities;

import java.sql.Timestamp;

public class Poste {
    private int post_id;
    private int likes;
    private Timestamp date_creation;
    private Timestamp date_modification;
    private String url;
    private String type;
    private String status;

    public Poste() {
    }

    public Poste(int post_id, int likes, Timestamp date_creation, Timestamp date_modification, String url,
            String type, String status) {
        this.post_id = post_id;
        this.likes = likes;
        this.date_creation = date_creation;
        this.date_modification = date_modification;
        this.url = url;
        this.type = type;
        this.status = status;
    }

    public Poste(int likes, Timestamp date_creation, Timestamp date_modification, String url, String type,
            String status) {
        this.likes = likes;
        this.date_creation = date_creation;
        this.date_modification = date_modification;
        this.url = url;
        this.type = type;
        this.status = status;
    }

    public int getPost_id() {
        return post_id;
    }

    public void setPost_id(int post_id) {
        this.post_id = post_id;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public Timestamp getDate_creation() {
        return date_creation;
    }

    public void setDate_creation(Timestamp date_creation) {
        this.date_creation = date_creation;
    }

    public Timestamp getDate_modification() {
        return date_modification;
    }

    public void setDate_modification(Timestamp date_modification) {
        this.date_modification = date_modification;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Poste{" +
                "post_id=" + post_id +
                ", likes=" + likes +
                ", date_creation=" + date_creation +
                ", date_modification=" + date_modification +
                ", chemin_fichier='" + url + '\'' +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
