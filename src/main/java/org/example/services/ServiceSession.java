package org.example.services;

import org.example.entities.Session;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceSession implements IService<Session> {

    private final Connection connection;

    public ServiceSession() {
        connection = MyDataBase.getInstance().getConnection();
    }

    // --- IService Implementation (Required by group pattern) ---
    @Override
    public void add(Session s) {
        try {
            ajouter(s);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(Session s) {
        try {
            modifier(s);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(int id) {
        try {
            supprimer(id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Session> getAll() {
        try {
            return readAll();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // --- Original Methods (Used by existing controllers) ---
    public void ajouter(Session s) throws SQLException {
        String sql = "INSERT INTO sessions (date, heure, capacite, nbr_places_restant, activite_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, s.getDate());
            ps.setTime(2, s.getHeure());
            ps.setInt(3, s.getCapacite());
            ps.setInt(4, s.getNbr_places_restant());
            ps.setInt(5, s.getActivite_id());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    s.setId_session(rs.getInt(1));
            }
        }
    }

    public void modifier(Session s) throws SQLException {
        String sql = "UPDATE sessions SET date=?, heure=?, capacite=?, nbr_places_restant=?, activite_id=? WHERE id_session=?";
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

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM sessions WHERE id_session=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

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
                                rs.getInt("activite_id"));
            }
        }
    }

    // ✅ Renommé readAll pour éviter conflit avec getAll() de l'interface qui a une
    // signature différente (SQLException)
    public List<Session> readAll() throws SQLException {
        List<Session> list = new ArrayList<>();
        String sql = "SELECT * FROM sessions ORDER BY date, heure";
        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                list.add(mapRow(rs));
        }
        return list;
    }

    public Session getById(int idSession) throws SQLException {
        String sql = "SELECT * FROM sessions WHERE id_session = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idSession);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return mapRow(rs);
            }
        }
        return null;
    }

    // ✅ Utilisé par HomeUser (réservation)
    public List<Session> getByActiviteId(int activiteId) throws SQLException {
        List<Session> list = new ArrayList<>();
        String sql = "SELECT * FROM sessions WHERE activite_id = ? ORDER BY date, heure";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, activiteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(mapRow(rs));
            }
        }
        return list;
    }

    private Session mapRow(ResultSet rs) throws SQLException {
        return new Session(
                rs.getInt("id_session"),
                rs.getDate("date"),
                rs.getTime("heure"),
                rs.getInt("capacite"),
                rs.getInt("nbr_places_restant"),
                rs.getInt("activite_id"));
    }
}