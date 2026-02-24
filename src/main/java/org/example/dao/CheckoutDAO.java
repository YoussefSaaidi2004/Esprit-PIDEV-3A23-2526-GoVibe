package org.example.dao;

import org.example.config.UnifiedDatabaseManager;
import org.example.entities.Checkout;

import java.math.BigDecimal;
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
            (flight_id, user_id, reservation_date, passenger_nbr, status_reservation, total_prix,
             passenger_name, passenger_email, passenger_phone, travel_class, payment_method, seat_preference)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = UnifiedDatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, checkout.getFlightId());
            ps.setInt(2, checkout.getIdUser());
            ps.setTimestamp(3, Timestamp.valueOf(checkout.getReservationDate()));
            ps.setInt(4, checkout.getPassengerNbr());
            ps.setString(5, checkout.getStatusReservation());
            ps.setInt(6, checkout.getTotalPrix() != null ? checkout.getTotalPrix().intValue() : 0);
            ps.setString(7, checkout.getPassengerName());
            ps.setString(8, checkout.getPassengerEmail());
            ps.setString(9, checkout.getPassengerPhone());
            ps.setString(10, checkout.getTravelClass());
            ps.setString(11, checkout.getPaymentMethod());
            ps.setString(12, checkout.getSeatPreference());

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

        try (Connection conn = UnifiedDatabaseManager.getConnection();
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

        try (Connection conn = UnifiedDatabaseManager.getConnection();
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
        // Use user_id — the actual live DB column name (id_user is the schema alias)
        String sql = """
            UPDATE checkout SET
            flight_id = ?, user_id = ?, reservation_date = ?, 
            passenger_nbr = ?, status_reservation = ?, total_prix = ?,
            passenger_name = ?, passenger_email = ?, passenger_phone = ?,
            travel_class = ?, payment_method = ?, seat_preference = ?
            WHERE checkout_id = ?
        """;

        try (Connection conn = UnifiedDatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, checkout.getFlightId());
            ps.setInt(2, checkout.getIdUser());
            ps.setTimestamp(3, Timestamp.valueOf(checkout.getReservationDate()));
            ps.setInt(4, checkout.getPassengerNbr());
            ps.setString(5, checkout.getStatusReservation());
            ps.setInt(6, checkout.getTotalPrix() != null ? checkout.getTotalPrix().intValue() : 0);
            ps.setString(7, checkout.getPassengerName());
            ps.setString(8, checkout.getPassengerEmail());
            ps.setString(9, checkout.getPassengerPhone());
            ps.setString(10, checkout.getTravelClass());
            ps.setString(11, checkout.getPaymentMethod());
            ps.setString(12, checkout.getSeatPreference());
            ps.setInt(13, checkout.getCheckoutId());

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

        try (Connection conn = UnifiedDatabaseManager.getConnection();
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
        
        // Handle user_id vs id_user
        if (hasColumn(rs, "user_id")) {
            checkout.setIdUser(rs.getInt("user_id"));
        } else if (hasColumn(rs, "id_user")) {
            checkout.setIdUser(rs.getInt("id_user"));
        }
        
        Timestamp timestamp = rs.getTimestamp("reservation_date");
        checkout.setReservationDate(timestamp != null ? timestamp.toLocalDateTime() : LocalDateTime.now());
        
        checkout.setPassengerNbr(rs.getInt("passenger_nbr"));
        checkout.setStatusReservation(rs.getString("status_reservation"));
        if (hasColumn(rs, "passenger_name")) {
            checkout.setPassengerName(rs.getString("passenger_name"));
        }
        if (hasColumn(rs, "passenger_email")) {
            checkout.setPassengerEmail(rs.getString("passenger_email"));
        }
        if (hasColumn(rs, "passenger_phone")) {
            checkout.setPassengerPhone(rs.getString("passenger_phone"));
        }
        if (hasColumn(rs, "payment_method")) {
            checkout.setPaymentMethod(rs.getString("payment_method"));
        }
        if (hasColumn(rs, "seat_preference")) {
            checkout.setSeatPreference(rs.getString("seat_preference"));
        }
        if (hasColumn(rs, "travel_class")) {
            checkout.setTravelClass(rs.getString("travel_class"));
        }
        
        // Handle total_prix vs total_price vs price
        BigDecimal price = BigDecimal.ZERO;
        if (hasColumn(rs, "total_prix")) {
            price = rs.getBigDecimal("total_prix");
        } else if (hasColumn(rs, "total_price")) {
            price = rs.getBigDecimal("total_price");
        } else if (hasColumn(rs, "price")) {
            price = rs.getBigDecimal("price");
        }
        checkout.setTotalPrix(price);

        return checkout;
    }

    private boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int count = metaData.getColumnCount();
        for (int i = 1; i <= count; i++) {
            if (columnName.equalsIgnoreCase(metaData.getColumnName(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find all pending checkouts
     * @return List of pending checkouts
     */
    public List<Checkout> findPendingCheckouts() {
        List<Checkout> checkouts = new ArrayList<>();
        String sql = "SELECT * FROM checkout WHERE UPPER(status_reservation) = 'PENDING'";

        try (Connection conn = UnifiedDatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                checkouts.add(extractCheckoutFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving pending checkouts: " + e.getMessage());
            e.printStackTrace();
        }

        return checkouts;
    }

    /**
     * Update only the status of a checkout
     * @param checkoutId The checkout ID
     * @param status The new status
     * @return true if successful
     */
    public boolean updateBookingStatus(int checkoutId, String status) {
        return updateStatus(checkoutId, status);
    }

    /**
     * Update only the status of a checkout (Aliased method for plan consistency)
     * @param checkoutId The checkout ID
     * @param status The new status
     * @return true if successful
     */
    public boolean updateStatus(int checkoutId, String status) {
        String sql = "UPDATE checkout SET status_reservation = ? WHERE checkout_id = ?";
        try (Connection conn = UnifiedDatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, checkoutId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating booking status: " + e.getMessage());
            return false;
        }
    }
}
