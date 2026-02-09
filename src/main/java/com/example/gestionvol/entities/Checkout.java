package com.example.gestionvol.entities;


import java.time.LocalDateTime;

public class Checkout {

    private int checkoutId;
    private String flightId;
    private int idUser;
    private LocalDateTime reservationDate;
    private int passengerNbr;
    private String statusReservation;
    private int totalPrix;

    public Checkout() {}

    public Checkout(String flightId, int idUser,
                    LocalDateTime reservationDate,
                    int passengerNbr, String statusReservation,
                    int totalPrix) {

        this.flightId = flightId;
        this.idUser = idUser;
        this.reservationDate = reservationDate;
        this.passengerNbr = passengerNbr;
        this.statusReservation = statusReservation;
        this.totalPrix = totalPrix;
    }

    public int getCheckoutId() {
        return checkoutId;
    }

    public void setCheckoutId(int checkoutId) {
        this.checkoutId = checkoutId;
    }

    public String getFlightId() {
        return flightId;
    }

    public void setFlightId(String flightId) {
        this.flightId = flightId;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public LocalDateTime getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(LocalDateTime reservationDate) {
        this.reservationDate = reservationDate;
    }

    public int getPassengerNbr() {
        return passengerNbr;
    }

    public void setPassengerNbr(int passengerNbr) {
        this.passengerNbr = passengerNbr;
    }

    public String getStatusReservation() {
        return statusReservation;
    }

    public void setStatusReservation(String statusReservation) {
        this.statusReservation = statusReservation;
    }

    public int getTotalPrix() {
        return totalPrix;
    }

    public void setTotalPrix(int totalPrix) {
        this.totalPrix = totalPrix;
    }
}
