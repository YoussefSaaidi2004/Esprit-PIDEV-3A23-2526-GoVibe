package org.example.entities;


import java.time.LocalDateTime;
import java.math.BigDecimal;

public class Checkout {

    private int checkoutId;
    private String flightId;
    private int idUser;
    private LocalDateTime reservationDate;
    private int passengerNbr;
    private String statusReservation;
    private BigDecimal totalPrix;
    
    // New fields for passenger details and preferences
    private String passengerName;
    private String passengerEmail;
    private String passengerPhone;
    private String travelClass;
    private String seatPreference;
    private String paymentMethod;

    public Checkout() {}

    public Checkout(String flightId, int idUser,
                    LocalDateTime reservationDate,
                    int passengerNbr, String statusReservation,
                    BigDecimal totalPrix) {

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

    public BigDecimal getTotalPrix() {
        return totalPrix;
    }

    public void setTotalPrix(BigDecimal totalPrix) {
        this.totalPrix = totalPrix;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public String getPassengerEmail() {
        return passengerEmail;
    }

    public void setPassengerEmail(String passengerEmail) {
        this.passengerEmail = passengerEmail;
    }

    public String getPassengerPhone() {
        return passengerPhone;
    }

    public void setPassengerPhone(String passengerPhone) {
        this.passengerPhone = passengerPhone;
    }

    public String getTravelClass() {
        return travelClass;
    }

    public void setTravelClass(String travelClass) {
        this.travelClass = travelClass;
    }

    public String getSeatPreference() {
        return seatPreference;
    }

    public void setSeatPreference(String seatPreference) {
        this.seatPreference = seatPreference;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(this.statusReservation);
    }

    public boolean isConfirmed() {
        return "CONFIRMED".equalsIgnoreCase(this.statusReservation);
    }
}
