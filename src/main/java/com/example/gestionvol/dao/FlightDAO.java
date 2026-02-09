package com.example.gestionvol.dao;

import com.example.gestionvol.config.DBConnection;
import com.example.gestionvol.entities.Flight;

import java.sql.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Flight entity
 * Handles all database operations for flights
 */
public class FlightDAO {

    /**
     * Create a new flight in the database
     * @param flight Flight object to insert
     * @return true if successful, false otherwise
     */
    public boolean create(Flight flight) {
        String sql = """
            INSERT INTO vol 
            (flight_id, departure_airport, destination, departure_time, arrival_time, 
             classe_chaise, airline, prix, available_seats, description)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, flight.getFlightId());
            ps.setString(2, flight.getDepartureAirport());
            ps.setString(3, flight.getDestination());
            ps.setTime(4, Time.valueOf(flight.getDepartureTime()));
            ps.setTime(5, Time.valueOf(flight.getArrivalTime()));
            ps.setString(6, flight.getClasseChaise());
            ps.setString(7, flight.getAirline());
            ps.setInt(8, flight.getPrix());
            ps.setInt(9, flight.getAvailableSeats());
            ps.setString(10, flight.getDescription());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error creating flight: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieve all flights from database
     * @return List of all flights
     */
    public List<Flight> findAll() {
        List<Flight> flights = new ArrayList<>();
        String sql = "SELECT * FROM vol";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                flights.add(extractFlightFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving flights: " + e.getMessage());
            e.printStackTrace();
        }

        return flights;
    }

    /**
     * Find a flight by ID
     * @param flightId The flight ID to search for
     * @return Flight object if found, null otherwise
     */
    public Flight findById(String flightId) {
        String sql = "SELECT * FROM vol WHERE flight_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, flightId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return extractFlightFromResultSet(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error finding flight: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Update an existing flight
     * @param flight Flight object with updated data
     * @return true if successful, false otherwise
     */
    public boolean update(Flight flight) {
        String sql = """
            UPDATE vol SET
            departure_airport = ?, destination = ?, departure_time = ?, arrival_time = ?,
            classe_chaise = ?, airline = ?, prix = ?, available_seats = ?, description = ?
            WHERE flight_id = ?
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, flight.getDepartureAirport());
            ps.setString(2, flight.getDestination());
            ps.setTime(3, Time.valueOf(flight.getDepartureTime()));
            ps.setTime(4, Time.valueOf(flight.getArrivalTime()));
            ps.setString(5, flight.getClasseChaise());
            ps.setString(6, flight.getAirline());
            ps.setInt(7, flight.getPrix());
            ps.setInt(8, flight.getAvailableSeats());
            ps.setString(9, flight.getDescription());
            ps.setString(10, flight.getFlightId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating flight: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete a flight by ID
     * @param flightId The flight ID to delete
     * @return true if successful, false otherwise
     */
    public boolean delete(String flightId) {
        String sql = "DELETE FROM vol WHERE flight_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, flightId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting flight: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Helper method to extract Flight object from ResultSet
     * @param rs ResultSet containing flight data
     * @return Flight object
     * @throws SQLException if database error occurs
     */
    private Flight extractFlightFromResultSet(ResultSet rs) throws SQLException {
        Flight flight = new Flight();
        flight.setFlightId(rs.getString("flight_id"));
        flight.setDepartureAirport(rs.getString("departure_airport"));
        flight.setDestination(rs.getString("destination"));
        
        Time depTime = rs.getTime("departure_time");
        Time arrTime = rs.getTime("arrival_time");
        
        flight.setDepartureTime(depTime != null ? depTime.toLocalTime() : LocalTime.now());
        flight.setArrivalTime(arrTime != null ? arrTime.toLocalTime() : LocalTime.now());
        flight.setClasseChaise(rs.getString("classe_chaise"));
        flight.setAirline(rs.getString("airline"));
        flight.setPrix(rs.getInt("prix"));
        flight.setAvailableSeats(rs.getInt("available_seats"));
        flight.setDescription(rs.getString("description"));

        return flight;
    }
}
