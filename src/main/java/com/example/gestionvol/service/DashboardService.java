package com.example.gestionvol.service;

import com.example.gestionvol.dao.CheckoutDAO;
import com.example.gestionvol.dao.FlightDAO;
import com.example.gestionvol.entities.Checkout;
import com.example.gestionvol.entities.Flight;
import java.util.List;

public class DashboardService {
    private final FlightDAO flightDAO = new FlightDAO();
    private final CheckoutDAO checkoutDAO = new CheckoutDAO();

    public int getPendingApprovalsCount() {
        return checkoutDAO.findPendingCheckouts().size();
    }

    public double getTotalRevenue() {
        return checkoutDAO.findAll().stream()
                .filter(c -> "Confirmed".equalsIgnoreCase(c.getStatusReservation()))
                .mapToDouble(Checkout::getTotalPrix)
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
