package org.example.services;

import org.example.entites.Session;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceSession implements IService<Session> {

    private final Connection connection;

    public ServiceSession() {
        connection = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Session s) throws SQLException {
        String sql = "INSERT INTO sessions (date, heure, capacite, nbr_places_restant, activite_id) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, s.getDate());
            ps.setTime(2, s.getHeure());
            ps.setInt(3, s.getCapacite());
            ps.setInt(4, s.getNbr_places_restant());
            ps.setInt(5, s.getActivite_id());

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                s.setId_session(rs.getInt(1));
            }
        }
    }

    @Override
    public void modifier(Session s) throws SQLException {
        String sql = "UPDATE sessions SET date = ?, heure = ?, capacite = ?, nbr_places_restant = ?, activite_id = ? "
                + "WHERE id_session = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDate(1, s.getDate());
            ps.setTime(2, s.getHeure());
            ps.setInt(3, s.getCapacite());
            ps.setInt(4, s.getNbr_places_restant());
            ps.setInt(5, s.getActivite_id());
            ps.setInt(6, s.getId_session());

            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM sessions WHERE id_session = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public void afficher() throws SQLException {
        String sql = "SELECT * FROM sessions ORDER BY id_session";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                System.out.println(
                        rs.getInt("id_session") + " | " +
                                rs.getDate("date") + " | " +
                                rs.getTime("heure") + " | " +
                                rs.getInt("capacite") + " | " +
                                rs.getInt("nbr_places_restant") + " | " +
                                rs.getInt("activite_id")
                );
            }
        }
    }

    public List<Session> getAll() throws SQLException {
        List<Session> list = new ArrayList<>();

        String sql = "SELECT * FROM sessions";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Session s = new Session(
                        rs.getInt("id_session"),
                        rs.getDate("date"),
                        rs.getTime("heure"),
                        rs.getInt("capacite"),
                        rs.getInt("nbr_places_restant"),
                        rs.getInt("activite_id")
                );
                list.add(s);
            }
        }
        return list;
    }
}