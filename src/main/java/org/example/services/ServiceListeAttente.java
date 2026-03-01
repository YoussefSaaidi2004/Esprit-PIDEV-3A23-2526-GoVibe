package org.example.services;

import org.example.entities.Chambre;
import org.example.entities.ListeAttente;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 6️⃣ Service Liste d'Attente — GoVibe Hotel Module
 * Manages hotel waitlist when fully booked.
 * Notifies first client automatically on cancellation.
 */
public class ServiceListeAttente {

    private Connection connection;

    public ServiceListeAttente() {
        connection = MyDataBase.getInstance().getMyConnection();
    }

    /** Add a client to the waitlist */
    public void ajouterAttente(ListeAttente la) throws SQLException {
        String sql = "INSERT INTO liste_attente (user_id, hotel_id, capacite_souhaitee, budget_max, " +
                     "date_souhaitee_debut, date_souhaitee_fin, statut) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, la.getUserId());
            stmt.setInt(2, la.getHotelId());
            stmt.setInt(3, la.getCapaciteSouhaitee());
            stmt.setDouble(4, la.getBudgetMax());
            stmt.setDate(5, Date.valueOf(la.getDateSouhaiteeDebut()));
            stmt.setDate(6, Date.valueOf(la.getDateSouhaiteeFin()));
            stmt.setString(7, "EN_ATTENTE");
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) la.setId(keys.getInt(1));
        }
    }

    /** Get all waitlist entries for a hotel */
    public List<ListeAttente> getAttenteParHotel(int hotelId) throws SQLException {
        List<ListeAttente> list = new ArrayList<>();
        String sql = "SELECT la.*, CONCAT(p.prenom, ' ', p.nom) AS user_name, p.email, h.nom AS hotel_nom " +
                     "FROM liste_attente la " +
                     "LEFT JOIN personne p ON p.id = la.user_id " +
                     "LEFT JOIN hotel h ON h.id = la.hotel_id " +
                     "WHERE la.hotel_id = ? AND la.statut = 'EN_ATTENTE' " +
                     "ORDER BY la.date_creation ASC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, hotelId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /** Get all waitlist entries */
    public List<ListeAttente> getAll() throws SQLException {
        List<ListeAttente> list = new ArrayList<>();
        String sql = "SELECT la.*, CONCAT(p.prenom, ' ', p.nom) AS user_name, p.email, h.nom AS hotel_nom " +
                     "FROM liste_attente la " +
                     "LEFT JOIN personne p ON p.id = la.user_id " +
                     "LEFT JOIN hotel h ON h.id = la.hotel_id " +
                     "ORDER BY la.statut ASC, la.date_creation ASC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /**
     * When a reservation is cancelled, notify the first waiting client.
     * @param hotelId The hotel where a room became available
     * @param chambreDisponible The newly freed room
     * @return The first waiting client's email (null if no waitlist)
     */
    public String notifierPremierClient(int hotelId, Chambre chambreDisponible) throws SQLException {
        // Find first client in waitlist matching hotel + capacity + budget
        String sql = "SELECT la.*, CONCAT(p.prenom, ' ', p.nom) AS user_name, p.email, h.nom AS hotel_nom " +
                     "FROM liste_attente la " +
                     "LEFT JOIN personne p ON p.id = la.user_id " +
                     "LEFT JOIN hotel h ON h.id = la.hotel_id " +
                     "WHERE la.hotel_id = ? AND la.statut = 'EN_ATTENTE' " +
                     "AND la.capacite_souhaitee <= ? " +
                     "AND (la.budget_max = 0 OR la.budget_max >= ?) " +
                     "ORDER BY la.date_creation ASC LIMIT 1";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, hotelId);
            stmt.setInt(2, chambreDisponible.getCapacite());
            stmt.setDouble(3, chambreDisponible.getPrixStandard());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int attenteId = rs.getInt("id");
                String email = rs.getString("email");
                String name = rs.getString("user_name");

                // Mark as notified
                String sql2 = "UPDATE liste_attente SET statut = 'NOTIFIE', " +
                              "chambre_proposee_id = ?, date_notification = ? WHERE id = ?";
                try (PreparedStatement upd = connection.prepareStatement(sql2)) {
                    upd.setInt(1, chambreDisponible.getId());
                    upd.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                    upd.setInt(3, attenteId);
                    upd.executeUpdate();
                }
                return email + "|" + name; // Return info for notification display
            }
        }
        return null;
    }

    /** Mark waitlist entry status */
    public void updateStatut(int id, ListeAttente.StatutAttente statut) throws SQLException {
        String sql = "UPDATE liste_attente SET statut = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, statut.name());
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM liste_attente WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private ListeAttente map(ResultSet rs) throws SQLException {
        ListeAttente la = new ListeAttente();
        la.setId(rs.getInt("id"));
        la.setUserId(rs.getInt("user_id"));
        la.setHotelId(rs.getInt("hotel_id"));
        la.setCapaciteSouhaitee(rs.getInt("capacite_souhaitee"));
        la.setBudgetMax(rs.getDouble("budget_max"));
        la.setDateSouhaiteeDebut(rs.getDate("date_souhaitee_debut").toLocalDate());
        la.setDateSouhaiteeFin(rs.getDate("date_souhaitee_fin").toLocalDate());
        try {
            String s = rs.getString("statut");
            la.setStatut(ListeAttente.StatutAttente.valueOf(s));
        } catch (Exception e) {
            la.setStatut(ListeAttente.StatutAttente.EN_ATTENTE);
        }
        try { la.setUserName(rs.getString("user_name")); } catch (Exception ignored) {}
        try { la.setUserEmail(rs.getString("email")); } catch (Exception ignored) {}
        try { la.setHotelNom(rs.getString("hotel_nom")); } catch (Exception ignored) {}
        int chambreId = rs.getInt("chambre_proposee_id");
        if (!rs.wasNull()) la.setChambreProposeeId(chambreId);
        Timestamp created = rs.getTimestamp("date_creation");
        if (created != null) la.setDateCreation(created.toLocalDateTime());
        return la;
    }
}
