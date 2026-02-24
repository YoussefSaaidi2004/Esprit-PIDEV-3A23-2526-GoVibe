package org.example.controllers;

import org.example.entities.Checkout;

public class CheckoutController {
    private static org.example.entities.Flight selectedFlight;

    public static void setSelectedFlight(org.example.entities.Flight flight) {
        selectedFlight = flight;
    }

    public static org.example.entities.Flight getSelectedFlight() {
        return selectedFlight;
    }
}
