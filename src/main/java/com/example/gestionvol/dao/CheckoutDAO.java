package com.example.gestionvol.dao;

import com.example.gestionvol.config.DBConnection;
import com.example.gestionvol.entities.Checkout;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Checkout entity
 * Handles all database operations for checkouts
 */
public class CheckoutDAO {

    /**
     * Create a new checkout in the database
     * @param checkout Checkout object to insert
     * @return true if successful, false otherwise
     */
    public boolean create(Checkout checkout) {
        String sql = """
            INSERT INTO checkout 
            (flight_id, id_user, reservation_date, passenger_nbr, status_reservation, total_prix)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, checkout.getFlightId());
            ps.setInt(2, checkout.getIdUser());
            ps.setTimestamp(3, Timestamp.valueOf(checkout.getReservationDate()));
            ps.setInt(4, checkout.getPassengerNbr());
            ps.setString(5, checkout.getStatusReservation());
            ps.setInt(6, checkout.getTotalPrix());

            int affectedRows = ps.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        checkout.setCheckoutId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error creating checkout: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Retrieve all checkouts from database
     * @return List of all checkouts
     */
    public List<Checkout> findAll() {
        List<Checkout> checkouts = new ArrayList<>();
        String sql = "SELECT * FROM checkout";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                checkouts.add(extractCheckoutFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving checkouts: " + e.getMessage());
            e.printStackTrace();
        }

        return checkouts;
    }

    /**
     * Find a checkout by ID
     * @param checkoutId The checkout ID to search for
     * @return Checkout object if found, null otherwise
     */
    public Checkout findById(int checkoutId) {
        String sql = "SELECT * FROM checkout WHERE checkout_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, checkoutId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return extractCheckoutFromResultSet(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error finding checkout: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Update an existing checkout
     * @param checkout Checkout object with updated data
     * @return true if successful, false otherwise
     */
    public boolean update(Checkout checkout) {
        String sql = """
            UPDATE checkout SET
            flight_id = ?, id_user = ?, reservation_date = ?, 
            passenger_nbr = ?, status_reservation = ?, total_prix = ?
            WHERE checkout_id = ?
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, checkout.getFlightId());
            ps.setInt(2, checkout.getIdUser());
            ps.setTimestamp(3, Timestamp.valueOf(checkout.getReservationDate()));
            ps.setInt(4, checkout.getPassengerNbr());
            ps.setString(5, checkout.getStatusReservation());
            ps.setInt(6, checkout.getTotalPrix());
            ps.setInt(7, checkout.getCheckoutId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating checkout: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete a checkout by ID
     * @param checkoutId The checkout ID to delete
     * @return true if successful, false otherwise
     */
    public boolean delete(int checkoutId) {
        String sql = "DELETE FROM checkout WHERE checkout_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, checkoutId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting checkout: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Helper method to extract Checkout object from ResultSet
     * @param rs ResultSet containing checkout data
     * @return Checkout object
     * @throws SQLException if database error occurs
     */
    private Checkout extractCheckoutFromResultSet(ResultSet rs) throws SQLException {
        Checkout checkout = new Checkout();
        checkout.setCheckoutId(rs.getInt("checkout_id"));
        checkout.setFlightId(rs.getString("flight_id"));
        checkout.setIdUser(rs.getInt("id_user"));
        
        Timestamp timestamp = rs.getTimestamp("reservation_date");
        checkout.setReservationDate(timestamp != null ? timestamp.toLocalDateTime() : LocalDateTime.now());
        
        checkout.setPassengerNbr(rs.getInt("passenger_nbr"));
        checkout.setStatusReservation(rs.getString("status_reservation"));
        checkout.setTotalPrix(rs.getInt("total_prix"));

        return checkout;
    }
}
