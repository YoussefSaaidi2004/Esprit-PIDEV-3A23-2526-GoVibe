package org.example.services;

import org.example.entities.Reservation;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ServiceReservation {

    private Connection connection;

    public ServiceReservation() {
        connection = MyDataBase.getInstance().getMyConnection();
    }

    public void insert(Reservation r) throws SQLException {
        String sql = "INSERT INTO reservation(user_id, chambre_id, hotel_id, date_debut, date_fin, prix_total, statut) " +
                     "VALUES (?,?,?,?,?,?,?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, r.getUserId());
            stmt.setInt(2, r.getChambreId());
            stmt.setInt(3, r.getHotelId());
            stmt.setDate(4, Date.valueOf(r.getDateDebut()));
            stmt.setDate(5, Date.valueOf(r.getDateFin()));
            stmt.setDouble(6, r.getPrixTotal());
            stmt.setString(7, r.getStatut());
            stmt.executeUpdate();
        }
    }


    public void update(Reservation r) throws SQLException {
        String sql = "UPDATE reservation SET user_id=?, chambre_id=?, hotel_id=?, " +
                     "date_debut=?, date_fin=?, prix_total=?, statut=? WHERE id=?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, r.getUserId());
            stmt.setInt(2, r.getChambreId());
            stmt.setInt(3, r.getHotelId());
            stmt.setDate(4, Date.valueOf(r.getDateDebut()));
            stmt.setDate(5, Date.valueOf(r.getDateFin()));
            stmt.setDouble(6, r.getPrixTotal());
            stmt.setString(7, r.getStatut());
            stmt.setInt(8, r.getId());
            stmt.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM reservation WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public List<Reservation> show() throws SQLException {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT r.*, p.nom, p.prenom, p.email " +
                     "FROM reservation r " +
                     "LEFT JOIN personne p ON p.id = r.user_id";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSetToReservation(rs));
            }
        }
        return list;
    }


    public Reservation findById(int id) throws SQLException {
        String sql = "SELECT r.*, p.nom, p.prenom, p.email " +
                     "FROM reservation r " +
                     "LEFT JOIN personne p ON p.id = r.user_id " +
                     "WHERE r.id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToReservation(rs);
            }
        }
        return null;
    }

    private Reservation mapResultSetToReservation(ResultSet rs) throws SQLException {
        Reservation reservation = new Reservation(
            rs.getInt("id"),
            rs.getInt("user_id"),
            rs.getInt("chambre_id"),
            rs.getInt("hotel_id"),
            rs.getDate("date_debut").toLocalDate(),
            rs.getDate("date_fin").toLocalDate(),
            rs.getDouble("prix_total"),
            rs.getString("statut")
        );

        reservation.setUserNom(getOptionalString(rs, "nom"));
        reservation.setUserPrenom(getOptionalString(rs, "prenom"));
        reservation.setUserEmail(getOptionalString(rs, "email"));
        return reservation;
    }

    private String getOptionalString(ResultSet rs, String columnName) {
        try {
            return rs.getString(columnName);
        } catch (SQLException e) {
            return null;
        }
    }
}

