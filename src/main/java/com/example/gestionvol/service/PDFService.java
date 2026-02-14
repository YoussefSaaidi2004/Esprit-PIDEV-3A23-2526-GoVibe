package com.example.gestionvol.service;

import com.example.gestionvol.entities.Checkout;
import com.example.gestionvol.entities.Flight;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * PDFService: Generate booking confirmation files
 * 
 * Generates a formatted text-based booking confirmation document.
 * Uses plain Java I/O — no external PDF library needed.
 */
public class PDFService {

    private static final String NOTIFICATIONS_DIR = "./notifications";

    /**
     * Generate a booking confirmation document for a checkout
     * @param userId User ID
     * @param checkout The checkout/booking object
     * @param flight The flight associated with the booking
     * @return File path if successful, null otherwise
     */
    public static String generateBookingConfirmation(int userId, Checkout checkout, Flight flight) {
        try {
            String userDir = NOTIFICATIONS_DIR + File.separator + "user_" + userId;
            File dir = new File(userDir);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    System.err.println("❌ Failed to create directory: " + userDir);
                    return null;
                }
            }

            String fileName = "booking_" + checkout.getCheckoutId() + ".txt";
            String filePath = userDir + File.separator + fileName;

            try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
                String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                
                writer.println("╔══════════════════════════════════════════════════════╗");
                writer.println("║           GOVIBE — BOOKING CONFIRMATION              ║");
                writer.println("╚══════════════════════════════════════════════════════╝");
                writer.println();
                writer.println("  Confirmation #: GV-" + checkout.getCheckoutId());
                writer.println("  Generated:      " + now);
                writer.println("  Status:         " + checkout.getStatusReservation());
                writer.println();
                writer.println("──────────────── FLIGHT DETAILS ────────────────────────");
                writer.println();
                
                if (flight != null) {
                    writer.println("  Flight:         " + flight.getFlightId());
                    writer.println("  Route:          " + flight.getDepartureAirport() + " → " + flight.getDestination());
                    writer.println("  Airline:        " + flight.getAirline());
                    writer.println("  Departure:      " + flight.getDepartureTime());
                    writer.println("  Arrival:        " + flight.getArrivalTime());
                    writer.println("  Class:          " + flight.getClasseChaise());
                } else {
                    writer.println("  Flight ID:      " + checkout.getFlightId());
                }
                
                writer.println();
                writer.println("──────────────── PASSENGER INFO ────────────────────────");
                writer.println();
                writer.println("  Name:           " + safe(checkout.getPassengerName()));
                writer.println("  Email:          " + safe(checkout.getPassengerEmail()));
                writer.println("  Phone:          " + safe(checkout.getPassengerPhone()));
                writer.println("  Passengers:     " + checkout.getPassengerNbr());
                writer.println("  Seat Pref:      " + safe(checkout.getSeatPreference()));
                writer.println();
                writer.println("──────────────── PAYMENT ───────────────────────────────");
                writer.println();
                writer.println("  Method:         " + safe(checkout.getPaymentMethod()));
                writer.println("  Total Price:    " + checkout.getTotalPrix() + " DT");
                writer.println("  Booking Date:   " + (checkout.getReservationDate() != null 
                    ? checkout.getReservationDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) 
                    : "N/A"));
                writer.println();
                writer.println("════════════════════════════════════════════════════════");
                writer.println("  Thank you for choosing GoVibe! Safe travels.");
                writer.println("════════════════════════════════════════════════════════");
            }

            System.out.println("📄 Booking confirmation generated: " + filePath);
            return filePath;

        } catch (Exception e) {
            System.err.println("❌ Error generating booking confirmation: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Legacy method — kept for backward compatibility
     */
    public static String generateBookingPDF(int userId, int bookingId, String bookingDetails) {
        try {
            String userDir = NOTIFICATIONS_DIR + File.separator + "user_" + userId;
            File dir = new File(userDir);
            if (!dir.exists()) dir.mkdirs();

            String filePath = userDir + File.separator + "booking_" + bookingId + ".txt";

            try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
                writer.println("╔══════════════════════════════════════════════════════╗");
                writer.println("║           GOVIBE — BOOKING CONFIRMATION              ║");
                writer.println("╚══════════════════════════════════════════════════════╝");
                writer.println();
                writer.println(bookingDetails);
                writer.println();
                writer.println("════════════════════════════════════════════════════════");
            }

            System.out.println("📄 Booking confirmation generated: " + filePath);
            return filePath;

        } catch (Exception e) {
            System.err.println("❌ Error generating PDF: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Check if a booking confirmation exists
     */
    public static boolean bookingPDFExists(int userId, int bookingId) {
        String path = NOTIFICATIONS_DIR + File.separator + "user_" + userId
                     + File.separator + "booking_" + bookingId + ".txt";
        return new File(path).exists();
    }

    /**
     * Get the path for a booking confirmation
     */
    public static String getBookingPDFPath(int userId, int bookingId) {
        return NOTIFICATIONS_DIR + File.separator + "user_" + userId
               + File.separator + "booking_" + bookingId + ".txt";
    }

    /**
     * Delete a booking confirmation
     */
    public static boolean deleteBookingPDF(int userId, int bookingId) {
        File file = new File(getBookingPDFPath(userId, bookingId));
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                System.out.println("🗑️ Confirmation deleted: user_" + userId + "/booking_" + bookingId);
            }
            return deleted;
        }
        return false;
    }

    private static String safe(String value) {
        return value != null ? value : "N/A";
    }
}
