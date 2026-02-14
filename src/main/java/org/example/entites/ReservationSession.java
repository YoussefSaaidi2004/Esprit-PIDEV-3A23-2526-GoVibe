package org.example.entites;

import java.sql.Timestamp;

public class ReservationSession {

    private int id_reservation;
    private int session_id;
    private int nb_places;
    private String user_ref;
    private Timestamp reserved_at;

    public ReservationSession() {}

    public ReservationSession(int session_id, int nb_places, String user_ref) {
        this.session_id = session_id;
        this.nb_places = nb_places;
        this.user_ref = user_ref;
    }

    public ReservationSession(int id_reservation, int session_id, int nb_places, String user_ref, Timestamp reserved_at) {
        this.id_reservation = id_reservation;
        this.session_id = session_id;
        this.nb_places = nb_places;
        this.user_ref = user_ref;
        this.reserved_at = reserved_at;
    }

    public int getId_reservation() { return id_reservation; }
    public void setId_reservation(int id_reservation) { this.id_reservation = id_reservation; }

    public int getSession_id() { return session_id; }
    public void setSession_id(int session_id) { this.session_id = session_id; }

    public int getNb_places() { return nb_places; }
    public void setNb_places(int nb_places) { this.nb_places = nb_places; }

    public String getUser_ref() { return user_ref; }
    public void setUser_ref(String user_ref) { this.user_ref = user_ref; }

    public Timestamp getReserved_at() { return reserved_at; }
    public void setReserved_at(Timestamp reserved_at) { this.reserved_at = reserved_at; }

    @Override
    public String toString() {
        return "ReservationSession{" +
                "id_reservation=" + id_reservation +
                ", session_id=" + session_id +
                ", nb_places=" + nb_places +
                ", user_ref='" + user_ref + '\'' +
                ", reserved_at=" + reserved_at +
                '}';
    }
}