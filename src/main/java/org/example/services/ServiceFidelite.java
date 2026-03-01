package org.example.services;

import org.example.entities.ProgrammeFidelite;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 🎯 Service Programme Fidélité — GoVibe Hotel Module
 * Tracks loyalty points and status for each client.
 * Points: 1 point per DT spent.
 * Statuts: Bronze / Silver / Gold / Platinum
 */
public class ServiceFidelite {

    private Connection connection;

    public ServiceFidelite() {
        connection = MyDataBase.getInstance().getMyConnection();
    }

    /**
     * Get or create a loyalty record for a user.
     */
    public ProgrammeFidelite getOrCreate(int userId) throws SQLException {
        String sql = "SELECT pf.*, CONCAT(p.prenom, ' ', p.nom) AS user_name " +
                     "FROM programme_fidelite pf " +
                     "LEFT JOIN personne p ON p.id = pf.user_id " +
                     "WHERE pf.user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return map(rs);
        }
        // Not found — create new record
        String insert = "INSERT INTO programme_fidelite (user_id, points, statut, total_reservations, total_depenses) " +
                        "VALUES (?, 0, 'BRONZE', 0, 0.0)";
        try (PreparedStatement stmt = connection.prepareStatement(insert)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
        return getOrCreate(userId);
    }

    /**
     * Add points for a completed reservation and update statistics.
     */
    public void ajouterPointsReservation(int userId, double prixReservation) throws SQLException {
        int pointsGagnes = ProgrammeFidelite.calculerPointsGagnes(prixReservation);
        ProgrammeFidelite pf = getOrCreate(userId);

        int newPoints = pf.getPoints() + pointsGagnes;
        String newStatut = calculerStatutStr(newPoints);

        String sql = "UPDATE programme_fidelite SET " +
                     "points = ?, statut = ?, " +
                     "total_reservations = total_reservations + 1, " +
                     "total_depenses = total_depenses + ? " +
                     "WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, newPoints);
            stmt.setString(2, newStatut);
            stmt.setDouble(3, prixReservation);
            stmt.setInt(4, userId);
            stmt.executeUpdate();
        }
    }

    /**
     * Get all loyalty accounts (admin view).
     */
    public List<ProgrammeFidelite> getAll() throws SQLException {
        List<ProgrammeFidelite> list = new ArrayList<>();
        String sql = "SELECT pf.*, CONCAT(p.prenom, ' ', p.nom) AS user_name " +
                     "FROM programme_fidelite pf " +
                     "LEFT JOIN personne p ON p.id = pf.user_id " +
                     "ORDER BY pf.points DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /**
     * Reset points for a user (e.g. after redemption).
     */
    public void resetPoints(int userId) throws SQLException {
        String sql = "UPDATE programme_fidelite SET points = 0, statut = 'BRONZE' WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }

    private String calculerStatutStr(int points) {
        if (points >= 3000) return "PLATINUM";
        if (points >= 1500) return "GOLD";
        if (points >= 500)  return "SILVER";
        return "BRONZE";
    }

    private ProgrammeFidelite map(ResultSet rs) throws SQLException {
        ProgrammeFidelite pf = new ProgrammeFidelite();
        pf.setId(rs.getInt("id"));
        pf.setUserId(rs.getInt("user_id"));
        pf.setPoints(rs.getInt("points"));
        try { pf.setUserName(rs.getString("user_name")); } catch (Exception ignored) {}
        pf.setTotalReservations(rs.getInt("total_reservations"));
        pf.setTotalDepenses(rs.getDouble("total_depenses"));
        String statutStr = rs.getString("statut");
        try { pf.setStatut(ProgrammeFidelite.StatutFidelite.valueOf(statutStr)); }
        catch (Exception e) { pf.setStatut(ProgrammeFidelite.StatutFidelite.BRONZE); }
        return pf;
    }
}
