package tn.esprit.entities;

import java.sql.Timestamp;

public class Poste {
    private int post_id;
    private int user_id;
    private int likes;
    private Timestamp date_creation;
    private Timestamp date_modification;
    private String url;
    private String type;
    private String contenu;
    private Integer forum_id;

    public Poste() {
    }

    public Poste(int post_id, int user_id, int likes, Timestamp date_creation, Timestamp date_modification, String url,
            String type, String contenu, Integer forum_id) {
        this.post_id = post_id;
        this.user_id = user_id;
        this.likes = likes;
        this.date_creation = date_creation;
        this.date_modification = date_modification;
        this.url = url;
        this.type = type;
        this.contenu = contenu;
        this.forum_id = forum_id;
    }

    public Poste(int user_id, int likes, Timestamp date_creation, Timestamp date_modification, String url, String type,
            String contenu, Integer forum_id) {
        this.user_id = user_id;
        this.likes = likes;
        this.date_creation = date_creation;
        this.date_modification = date_modification;
        this.url = url;
        this.type = type;
        this.contenu = contenu;
        this.forum_id = forum_id;
    }

    // New Constructor for an ORPHAN post (no forum)
    public Poste(int user_id, String contenu, String type, String url) {
        this.user_id = user_id;
        this.contenu = contenu;
        this.type = type;
        this.url = url;
        this.likes = 0;
        this.date_creation = new Timestamp(System.currentTimeMillis());
        this.date_modification = this.date_creation;
        this.forum_id = null;
    }

    // New Constructor for a post IN A FORUM
    public Poste(int user_id, int forum_id, String contenu, String type, String url) {
        this(user_id, contenu, type, url);
        this.forum_id = forum_id;
    }

    public int getPost_id() {
        return post_id;
    }

    public void setPost_id(int post_id) {
        this.post_id = post_id;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
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

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public Integer getForum_id() {
        return forum_id;
    }

    public void setForum_id(Integer forum_id) {
        this.forum_id = forum_id;
    }

    @Override
    public String toString() {
        return "Poste{" +
                "post_id=" + post_id +
                ", user_id=" + user_id +
                ", likes=" + likes +
                ", date_creation=" + date_creation +
                ", date_modification=" + date_modification +
                ", url='" + url + '\'' +
                ", type='" + type + '\'' +
                ", contenu='" + contenu + '\'' +
                ", forum_id=" + forum_id +
                '}';
    }
}
