package org.example.services;

import org.example.dao.FlightDAO;
import org.example.entities.Flight;

import java.time.LocalTime;
import java.util.List;

/**
 * Service class for Flight business logic
 * Acts as intermediary between Controller and DAO
 */
public class FlightService {

    private final FlightDAO flightDAO;

    public FlightService() {
        this.flightDAO = new FlightDAO();
    }

    /**
     * Add a new flight
     * @param flight Flight to add
     * @return true if successful
     * @throws IllegalArgumentException if validation fails
     */
    public boolean addFlight(Flight flight) {
        validateFlight(flight);
        if (flightDAO.findById(flight.getFlightId()) != null) {
            throw new IllegalArgumentException("Flight ID '" + flight.getFlightId() + "' already exists.");
        }
        return flightDAO.create(flight);
    }

    /**
     * Get all flights
     * @return List of all flights
     */
    public List<Flight> getAllFlights() {
        return flightDAO.findAll();
    }

    /**
     * Find flight by ID
     * @param flightId The flight ID
     * @return Flight if found, null otherwise
     */
    public Flight getFlightById(String flightId) {
        return flightDAO.findById(flightId);
    }

    /**
     * Update an existing flight
     * @param flight Flight with updated data
     * @return true if successful
     * @throws IllegalArgumentException if validation fails
     */
    public boolean updateFlight(Flight flight) {
        validateFlight(flight);
        return flightDAO.update(flight);
    }

    /**
     * Delete a flight
     * @param flightId The flight ID to delete
     * @return true if successful
     */
    public boolean deleteFlight(String flightId) {
        if (flightId == null || flightId.trim().isEmpty()) {
            throw new IllegalArgumentException("Flight ID cannot be empty");
        }
        return flightDAO.delete(flightId);
    }

    /**
     * Validate flight data
     * @param flight Flight to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateFlight(Flight flight) {
        if (flight == null) {
            throw new IllegalArgumentException("Flight cannot be null");
        }
        
        if (flight.getFlightId() == null || flight.getFlightId().trim().isEmpty()) {
            throw new IllegalArgumentException("Flight ID is required");
        }
        
        if (flight.getDepartureAirport() == null || flight.getDepartureAirport().trim().isEmpty()) {
            throw new IllegalArgumentException("Departure airport is required");
        }
        
        if (flight.getDestination() == null || flight.getDestination().trim().isEmpty()) {
            throw new IllegalArgumentException("Destination is required");
        }
        
        if (flight.getAirline() == null || flight.getAirline().trim().isEmpty()) {
            throw new IllegalArgumentException("Airline is required");
        }
        
        if (flight.getPrix() <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        
        if (flight.getAvailableSeats() < 0) {
            throw new IllegalArgumentException("Available seats cannot be negative");
        }
        
        if (flight.getDepartureTime() != null && flight.getArrivalTime() != null) {
            if (flight.getArrivalTime().isBefore(flight.getDepartureTime())) {
                throw new IllegalArgumentException("Arrival time must be after departure time");
            }
        }
    }

    /**
     * Get all available flights (flights with available seats > 0)
     * @return List of available flights
     */
    public List<Flight> getAvailableFlights() {
        return flightDAO.findAll().stream()
                .filter(flight -> flight.getAvailableSeats() > 0)
                .toList();
    }
}
