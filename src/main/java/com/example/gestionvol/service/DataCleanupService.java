package com.example.gestionvol.service;

import com.example.gestionvol.dao.FlightDAO;
import com.example.gestionvol.entities.Flight;
import java.sql.*;
import java.util.List;

public class DataCleanupService {
    private final FlightDAO flightDAO = new FlightDAO();

    public void cleanAllFlights() {
        List<Flight> flights = flightDAO.findAll();
        for (Flight f : flights) {
            // Logic to clean messy seat data if any (as per user request)
            // This is primarily for one-time migration
        }
    }

    public static int extractSeatNumber(String text) {
        if (text == null) return 0;
        String digits = text.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }
}
