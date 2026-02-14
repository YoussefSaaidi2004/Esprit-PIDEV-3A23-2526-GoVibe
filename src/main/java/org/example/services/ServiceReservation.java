package org.example.services;

import org.example.entities.Reservation;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ServiceReservation implements IService<Reservation> {

    private Connection connection;

    public ServiceReservation() {
        connection = MyDataBase.getInstance().getMyConnection();
    }

    @Override
    public void insert(Reservation r) throws SQLException {
        String sql = "INSERT INTO reservation(client_nom, client_email, client_telephone, chambre_id, hotel_id, date_debut, date_fin, prix_total, statut) " +
                     "VALUES (?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, r.getClientNom());
            stmt.setString(2, r.getClientEmail());
            stmt.setString(3, r.getClientTelephone());
            stmt.setInt(4, r.getChambreId());
            stmt.setInt(5, r.getHotelId());
            stmt.setDate(6, Date.valueOf(r.getDateDebut()));
            stmt.setDate(7, Date.valueOf(r.getDateFin()));
            stmt.setDouble(8, r.getPrixTotal());
            stmt.setString(9, r.getStatut());
            stmt.executeUpdate();
        }
    }

    @Override
    public void update(Reservation r) throws SQLException {
        String sql = "UPDATE reservation SET client_nom=?, client_email=?, client_telephone=?, " +
                     "chambre_id=?, hotel_id=?, date_debut=?, date_fin=?, prix_total=?, statut=? WHERE id=?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, r.getClientNom());
            stmt.setString(2, r.getClientEmail());
            stmt.setString(3, r.getClientTelephone());
            stmt.setInt(4, r.getChambreId());
            stmt.setInt(5, r.getHotelId());
            stmt.setDate(6, Date.valueOf(r.getDateDebut()));
            stmt.setDate(7, Date.valueOf(r.getDateFin()));
            stmt.setDouble(8, r.getPrixTotal());
            stmt.setString(9, r.getStatut());
            stmt.setInt(10, r.getId());
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM reservation WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    @Override
    public List<Reservation> show() throws SQLException {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT * FROM reservation";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSetToReservation(rs));
            }
        }
        return list;
    }

    public Reservation findById(int id) throws SQLException {
        String sql = "SELECT * FROM reservation WHERE id=?";
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
        return new Reservation(
            rs.getInt("id"),
            rs.getString("client_nom"),
            rs.getString("client_email"),
            rs.getString("client_telephone"),
            rs.getInt("chambre_id"),
            rs.getInt("hotel_id"),
            rs.getDate("date_debut").toLocalDate(),
            rs.getDate("date_fin").toLocalDate(),
            rs.getDouble("prix_total"),
            rs.getString("statut")
        );
    }
}

