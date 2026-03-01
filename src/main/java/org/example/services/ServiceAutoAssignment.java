package org.example.services;

import org.example.entities.Chambre;
import org.example.entities.Hotel;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 1️⃣ Service Attribution Automatique de Chambre — GoVibe Hotel Module
 * Automatically selects the best available room based on:
 *   - Capacity requested
 *   - Client budget
 *   - Room availability (not already reserved for those dates)
 */
public class ServiceAutoAssignment {

    private Connection connection;

    public ServiceAutoAssignment() {
        connection = MyDataBase.getInstance().getMyConnection();
    }

    /**
     * Find the best available room for a given hotel, capacity, budget, and dates.
     *
     * @param hotelId       The target hotel
     * @param capaciteMin   Minimum capacity needed (e.g. 2 for a couple)
     * @param budgetMax     Maximum price the client accepts (0 = no limit)
     * @param dateDebut     Check-in date
     * @param dateFin       Check-out date
     * @return The best matching Chambre or null if none available
     */
    public Chambre attribuerChambreOptimale(int hotelId, int capaciteMin, double budgetMax,
                                             LocalDate dateDebut, LocalDate dateFin) throws SQLException {

        // Query: rooms in the hotel with enough capacity NOT reserved during the requested dates
        String sql =
            "SELECT c.* FROM chambre c " +
            "WHERE c.hotel_id = ? " +
            "AND c.capacite >= ? " +
            (budgetMax > 0 ? "AND c.prix_standard <= ? " : "") +
            "AND c.id NOT IN ( " +
            "  SELECT r.chambre_id FROM reservation r " +
            "  WHERE r.statut != 'ANNULEE' " +
            "  AND ( " +
            "    (r.date_debut < ? AND r.date_fin > ?) " +  // overlaps
            "  ) " +
            ") " +
            "ORDER BY c.prix_standard ASC " + // prefer cheapest first
            "LIMIT 1";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int idx = 1;
            stmt.setInt(idx++, hotelId);
            stmt.setInt(idx++, capaciteMin);
            if (budgetMax > 0) stmt.setDouble(idx++, budgetMax);
            stmt.setDate(idx++, Date.valueOf(dateFin));
            stmt.setDate(idx, Date.valueOf(dateDebut));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapChambre(rs);
            }
        }
        return null; // No room available → trigger waitlist
    }

    /**
     * Get all available rooms for a hotel between dates.
     */
    public List<Chambre> getChambresDisponibles(int hotelId,
                                                  LocalDate dateDebut,
                                                  LocalDate dateFin) throws SQLException {
        List<Chambre> result = new ArrayList<>();
        String sql =
            "SELECT c.* FROM chambre c " +
            "WHERE c.hotel_id = ? " +
            "AND c.id NOT IN ( " +
            "  SELECT r.chambre_id FROM reservation r " +
            "  WHERE r.statut != 'ANNULEE' " +
            "  AND (r.date_debut < ? AND r.date_fin > ?) " +
            ") " +
            "ORDER BY c.capacite ASC, c.prix_standard ASC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, hotelId);
            stmt.setDate(2, Date.valueOf(dateFin));
            stmt.setDate(3, Date.valueOf(dateDebut));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) result.add(mapChambre(rs));
        }
        return result;
    }

    /**
     * Check if a specific room is available for given dates.
     */
    public boolean isChambreDisponible(int chambreId, LocalDate dateDebut, LocalDate dateFin) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reservation " +
                     "WHERE chambre_id = ? AND statut != 'ANNULEE' " +
                     "AND (date_debut < ? AND date_fin > ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, chambreId);
            stmt.setDate(2, Date.valueOf(dateFin));
            stmt.setDate(3, Date.valueOf(dateDebut));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1) == 0;
        }
        return false;
    }

    /**
     * Check if a hotel is fully booked for given dates.
     */
    public boolean isHotelComplet(int hotelId, LocalDate dateDebut, LocalDate dateFin) throws SQLException {
        List<Chambre> available = getChambresDisponibles(hotelId, dateDebut, dateFin);
        return available.isEmpty();
    }

    private Chambre mapChambre(ResultSet rs) throws SQLException {
        return new Chambre(
            rs.getInt("id"),
            rs.getString("type"),
            rs.getInt("capacite"),
            rs.getString("equipements"),
            rs.getInt("hotel_id"),
            rs.getDouble("prix_standard"),
            rs.getDouble("prix_haute_saison"),
            rs.getDouble("prix_basse_saison")
        );
    }
}
