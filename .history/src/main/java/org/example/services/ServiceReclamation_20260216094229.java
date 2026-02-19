package org.example.services;

import org.example.entities.Reclamation;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceReclamation {

    private final Connection connection;

    public ServiceReclamation() {
        connection = MyDataBase.getInstance().getConnection();
    }

    public void ajouter(Reclamation r) throws SQLException {
        String sql = "INSERT INTO reclamation (user_id, sujet, message, status, date_envoi, created_by_user) VALUES (?, ?, ?, ?, NOW(), ?)";

        try (PreparedStatement pst = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pst.setInt(1, r.getUserId());
            pst.setString(2, r.getSujet());
            pst.setString(3, r.getMessage());
            pst.setString(4, Reclamation.STATUS_EN_ATTENTE);
            pst.setInt(5, r.getCreatedByUser());

            pst.executeUpdate();

            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    r.setId(rs.getInt(1));
                }
            }
        }
    }

    public void modifier(Reclamation r) throws SQLException {
        String sql = "UPDATE reclamation SET sujet=?, message=? WHERE id=? AND status=?";

        try (PreparedStatement pst = connection.prepareStatement(sql)) {

            pst.setString(1, r.getSujet());
            pst.setString(2, r.getMessage());
            pst.setInt(3, r.getId());
            pst.setString(4, Reclamation.STATUS_EN_ATTENTE);

            pst.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM reclamation WHERE id=?";

        try (PreparedStatement pst = connection.prepareStatement(sql)) {

            pst.setInt(1, id);

            pst.executeUpdate();
        }
    }

    public void updateStatus(int id, String status) throws SQLException {
        String sql = "UPDATE reclamation SET status=? WHERE id=?";

        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, status);
            pst.setInt(2, id);
            pst.executeUpdate();
        }
    }

    public void repondre(int id, String reponse) throws SQLException {
        String sql = "UPDATE reclamation SET reponse=?, status=?, date_reponse=NOW() WHERE id=?";

        try (PreparedStatement pst = connection.prepareStatement(sql)) {

            pst.setString(1, reponse);
            pst.setString(2, Reclamation.STATUS_RESOLU);
            pst.setInt(3, id);

            pst.executeUpdate();
        }
    }

    public List<Reclamation> getByUser(int userId) throws SQLException {
        List<Reclamation> list = new ArrayList<>();
        String sql = "SELECT r.*, p.nom, p.prenom, p.email FROM reclamation r " +
                     "JOIN personne p ON r.user_id = p.id WHERE r.user_id=? ORDER BY r.date_envoi DESC";

        try (PreparedStatement pst = connection.prepareStatement(sql)) {

            pst.setInt(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public List<Reclamation> getAll() throws SQLException {
        List<Reclamation> list = new ArrayList<>();
        String sql = "SELECT r.*, p.nom, p.prenom, p.email FROM reclamation r " +
                     "JOIN personne p ON r.user_id = p.id ORDER BY r.date_envoi DESC";

        try (PreparedStatement pst = connection.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    public List<Reclamation> getByStatus(String status) throws SQLException {
        List<Reclamation> list = new ArrayList<>();
        String sql = "SELECT r.*, p.nom, p.prenom, p.email FROM reclamation r " +
                     "JOIN personne p ON r.user_id = p.id WHERE r.status=? ORDER BY r.date_envoi DESC";

        try (PreparedStatement pst = connection.prepareStatement(sql)) {

            pst.setString(1, status);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public Reclamation getById(int id) throws SQLException {
        String sql = "SELECT r.*, p.nom, p.prenom, p.email FROM reclamation r " +
                     "JOIN personne p ON r.user_id = p.id WHERE r.id=?";

        try (PreparedStatement pst = connection.prepareStatement(sql)) {

            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    private Reclamation mapResultSet(ResultSet rs) throws SQLException {
        Reclamation r = new Reclamation();
        r.setId(rs.getInt("id"));
        r.setUserId(rs.getInt("user_id"));
        r.setSujet(rs.getString("sujet"));
        r.setMessage(rs.getString("message"));
        r.setReponse(rs.getString("reponse"));
        r.setStatus(rs.getString("status"));
        r.setDateEnvoi(rs.getTimestamp("date_envoi"));
        r.setDateReponse(rs.getTimestamp("date_reponse"));
        r.setCreatedByUser(rs.getInt("created_by_user"));
        r.setUserNom(rs.getString("nom"));
        r.setUserPrenom(rs.getString("prenom"));
        r.setUserEmail(rs.getString("email"));
        return r;
    }
}
