package org.example.entities;


import java.math.BigDecimal;
import java.time.LocalTime;

public class Flight {

    private String flightId;
    private String departureAirport;
    private String destination;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private String classeChaise;
    private String airline;
    private BigDecimal prix;
    private int availableSeats;
    private int totalSeats;
    private String description;

    public Flight() {}

    public Flight(String flightId, String departureAirport, String destination,
                  LocalTime departureTime, LocalTime arrivalTime,
                  String classeChaise, String airline,
                  BigDecimal prix, int availableSeats, int totalSeats, String description) {

        this.flightId = flightId;
        this.departureAirport = departureAirport;
        this.destination = destination;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.classeChaise = classeChaise;
        this.airline = airline;
        this.prix = prix;
        this.availableSeats = availableSeats;
        this.totalSeats = totalSeats;
        this.description = description;
    }

    public String getFlightId() {
        return flightId;
    }

    public void setFlightId(String flightId) {
        this.flightId = flightId;
    }

    public String getDepartureAirport() {
        return departureAirport;
    }

    public void setDepartureAirport(String departureAirport) {
        this.departureAirport = departureAirport;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalTime departureTime) {
        this.departureTime = departureTime;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public String getClasseChaise() {
        return classeChaise;
    }

    public void setClasseChaise(String classeChaise) {
        this.classeChaise = classeChaise;
    }

    public String getAirline() {
        return airline;
    }

    public void setAirline(String airline) {
        this.airline = airline;
    }

    public BigDecimal getPrix() {
        return prix;
    }

    public void setPrix(BigDecimal prix) {
        this.prix = prix;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
