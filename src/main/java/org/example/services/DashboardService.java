package org.example.services;

import org.example.dao.CheckoutDAO;
import org.example.dao.FlightDAO;
import org.example.entities.Checkout;
import org.example.entities.Flight;
import java.util.List;

public class DashboardService {
    private final FlightDAO flightDAO = new FlightDAO();
    private final CheckoutDAO checkoutDAO = new CheckoutDAO();

    public int getPendingApprovalsCount() {
        return checkoutDAO.findPendingCheckouts().size();
    }

    public double getTotalRevenue() {
        return checkoutDAO.findAll().stream()
                .filter(c -> "CONFIRMED".equalsIgnoreCase(c.getStatusReservation()))
                .mapToDouble(c -> c.getTotalPrix().doubleValue())
                .sum();
    }

    public int getTotalFlights() {
        return flightDAO.findAll().size();
    }

    public double getAverageOccupancyRate() {
        List<Flight> flights = flightDAO.findAll();
        if (flights.isEmpty()) return 0.0;
        
        double totalRate = 0;
        for (Flight f : flights) {
            if (f.getTotalSeats() > 0) {
                totalRate += (double) (f.getTotalSeats() - f.getAvailableSeats()) / f.getTotalSeats();
            }
        }
        return (totalRate / flights.size()) * 100;
    }
}
