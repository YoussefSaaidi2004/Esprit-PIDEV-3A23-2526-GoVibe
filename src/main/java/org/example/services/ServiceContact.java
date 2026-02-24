package org.example.services;

import org.example.entities.Contact;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceContact {

    private final Connection connection;

    public ServiceContact() {
        connection = MyDataBase.getInstance().getConnection();
    }

    // ==================== CRUD CLIENT ====================

    // ✅ Envoyer un message (Client)
    public void envoyerMessage(Contact contact) throws SQLException {
        String sql = "INSERT INTO contact (user_id, sujet, message, status, created_by_user) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, contact.getUserId());
            ps.setString(2, contact.getSujet());
            ps.setString(3, contact.getMessage());
            ps.setString(4, Contact.STATUS_EN_ATTENTE);
            ps.setBoolean(5, true);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    contact.setId(rs.getInt(1));
                }
            }
        }
    }

    // ✅ Modifier son message (Client) - uniquement si EN_ATTENTE
    public void modifierMessage(Contact contact) throws SQLException {
        String sql = "UPDATE contact SET sujet = ?, message = ? WHERE id = ? AND user_id = ? AND status = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, contact.getSujet());
            ps.setString(2, contact.getMessage());
            ps.setInt(3, contact.getId());
            ps.setInt(4, contact.getUserId());
            ps.setString(5, Contact.STATUS_EN_ATTENTE);

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Message introuvable ou déjà traité par l'admin");
            }
        }
    }

    // ✅ Supprimer son message (Client) - uniquement si EN_ATTENTE
    public void supprimerMessage(int contactId, int userId) throws SQLException {
        String sql = "DELETE FROM contact WHERE id = ? AND user_id = ? AND status = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, contactId);
            ps.setInt(2, userId);
            ps.setString(3, Contact.STATUS_EN_ATTENTE);

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Message introuvable ou déjà traité par l'admin");
            }
        }
    }

    // ✅ Récupérer les messages d'un client
    public List<Contact> getMessagesByUser(int userId) throws SQLException {
        List<Contact> list = new ArrayList<>();
        String sql = "SELECT * FROM contact WHERE user_id = ? ORDER BY date_envoi DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    // ==================== ADMIN ====================

    // ✅ Récupérer tous les messages (Admin)
    public List<Contact> getAllMessages() throws SQLException {
        List<Contact> list = new ArrayList<>();
        String sql = "SELECT c.*, p.nom, p.prenom, p.email " +
                     "FROM contact c " +
                     "JOIN personne p ON c.user_id = p.id " +
                     "ORDER BY c.date_envoi DESC";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Contact contact = mapResultSet(rs);
                contact.setUserNom(rs.getString("prenom") + " " + rs.getString("nom"));
                contact.setUserEmail(rs.getString("email"));
                list.add(contact);
            }
        }
        return list;
    }

    // ✅ Récupérer les messages par status (Admin)
    public List<Contact> getMessagesByStatus(String status) throws SQLException {
        List<Contact> list = new ArrayList<>();
        String sql = "SELECT c.*, p.nom, p.prenom, p.email " +
                     "FROM contact c " +
                     "JOIN personne p ON c.user_id = p.id " +
                     "WHERE c.status = ? " +
                     "ORDER BY c.date_envoi DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Contact contact = mapResultSet(rs);
                    contact.setUserNom(rs.getString("prenom") + " " + rs.getString("nom"));
                    contact.setUserEmail(rs.getString("email"));
                    list.add(contact);
                }
            }
        }
        return list;
    }

    // ✅ Répondre à un message (Admin)
    public void repondreMessage(int contactId, String reponse) throws SQLException {
        String sql = "UPDATE contact SET reponse = ?, status = ?, date_reponse = NOW() WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, reponse);
            ps.setString(2, Contact.STATUS_REPONDU);
            ps.setInt(3, contactId);

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Message introuvable");
            }
        }
    }

    // ✅ Marquer comme lu (Admin)
    public void marquerCommeLu(int contactId) throws SQLException {
        String sql = "UPDATE contact SET status = ? WHERE id = ? AND status = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, Contact.STATUS_LU);
            ps.setInt(2, contactId);
            ps.setString(3, Contact.STATUS_EN_ATTENTE);

            ps.executeUpdate();
        }
    }

    // ✅ Modifier réponse (Admin)
    public void modifierReponse(int contactId, String nouvelleReponse) throws SQLException {
        String sql = "UPDATE contact SET reponse = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nouvelleReponse);
            ps.setInt(2, contactId);

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Message introuvable");
            }
        }
    }

    // ✅ Supprimer message (Admin)
    public void supprimerMessageAdmin(int contactId) throws SQLException {
        String sql = "DELETE FROM contact WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, contactId);

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Message introuvable");
            }
        }
    }

    // ✅ Fermer un message (Admin)
    public void fermerMessage(int contactId) throws SQLException {
        String sql = "UPDATE contact SET status = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, Contact.STATUS_FERME);
            ps.setInt(2, contactId);

            ps.executeUpdate();
        }
    }

    // ✅ Compte les messages en attente
    public int countEnAttente() throws SQLException {
        String sql = "SELECT COUNT(*) FROM contact WHERE status = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, Contact.STATUS_EN_ATTENTE);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    // ==================== UTILS ====================

    private Contact mapResultSet(ResultSet rs) throws SQLException {
        Contact c = new Contact();
        c.setId(rs.getInt("id"));
        c.setUserId(rs.getInt("user_id"));
        c.setSujet(rs.getString("sujet"));
        c.setMessage(rs.getString("message"));
        c.setReponse(rs.getString("reponse"));
        c.setStatus(rs.getString("status"));
        c.setDateEnvoi(rs.getTimestamp("date_envoi"));
        c.setDateReponse(rs.getTimestamp("date_reponse"));
        c.setCreatedByUser(rs.getBoolean("created_by_user"));
        return c;
    }
}
