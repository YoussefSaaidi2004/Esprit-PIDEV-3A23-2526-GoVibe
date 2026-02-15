package org.example.entities;

import java.sql.Timestamp;

public class Forum {
    private int forum_id;
    private String name;
    private String image;
    private int created_by;
    private int post_count;
    private int nbr_members;
    private String description;
    private Timestamp date_creation;
    private boolean is_private;

    public Forum() {
    }

    public Forum(int forum_id, String name, String image, int created_by, int post_count, int nbr_members,
            String description, Timestamp date_creation, boolean is_private) {
        this.forum_id = forum_id;
        this.name = name;
        this.image = image;
        this.created_by = created_by;
        this.post_count = post_count;
        this.nbr_members = nbr_members;
        this.description = description;
        this.date_creation = date_creation;
        this.is_private = is_private;
    }

    public Forum(String name, String image, int created_by, int post_count, int nbr_members, String description,
            Timestamp date_creation, boolean is_private) {
        this.name = name;
        this.image = image;
        this.created_by = created_by;
        this.post_count = post_count;
        this.nbr_members = nbr_members;
        this.description = description;
        this.date_creation = date_creation;
        this.is_private = is_private;
    }

    public int getForum_id() {
        return forum_id;
    }

    public void setForum_id(int forum_id) {
        this.forum_id = forum_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getCreated_by() {
        return created_by;
    }

    public void setCreated_by(int created_by) {
        this.created_by = created_by;
    }

    public int getPost_count() {
        return post_count;
    }

    public void setPost_count(int post_count) {
        this.post_count = post_count;
    }

    public int getNbr_members() {
        return nbr_members;
    }

    public void setNbr_members(int nbr_members) {
        this.nbr_members = nbr_members;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getDate_creation() {
        return date_creation;
    }

    public void setDate_creation(Timestamp date_creation) {
        this.date_creation = date_creation;
    }

    public boolean isIs_private() {
        return is_private;
    }

    public void setIs_private(boolean is_private) {
        this.is_private = is_private;
    }

    @Override
    public String toString() {
        return "Forum{" +
                "forum_id=" + forum_id +
                ", name='" + name + '\'' +
                ", image='" + image + '\'' +
                ", created_by=" + created_by +
                ", post_count=" + post_count +
                ", nbr_members=" + nbr_members +
                ", description='" + description + '\'' +
                ", date_creation=" + date_creation +
                ", is_private=" + is_private +
                '}';
    }
}
