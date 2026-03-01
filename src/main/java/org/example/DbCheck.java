package org.example;

import org.example.dao.FlightDAO;
import org.example.entities.Flight;
import java.util.List;

public class DbCheck {
    public static void main(String[] args) {
        try {
            FlightDAO dao = new FlightDAO();
            List<Flight> flights = dao.findAll();
            System.out.println("--- FLIGHTS IN DB (" + flights.size() + ") ---");
            for (Flight f : flights) {
                System.out.println("ID: " + f.getFlightId() + 
                                   " | Dest: " + f.getDestination() + 
                                   " | Price: " + f.getPrix() + 
                                   " | Seats: " + f.getAvailableSeats() + "/" + f.getTotalSeats() +
                                   " | Class: " + f.getClasseChaise());
            }
            System.out.println("--------------------------------");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
