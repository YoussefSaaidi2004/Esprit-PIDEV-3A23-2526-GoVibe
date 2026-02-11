package com.example.gestionvol.service;

import com.example.gestionvol.dao.CheckoutDAO;
import com.example.gestionvol.entities.Checkout;

import java.util.List;

/**
 * Service class for Checkout business logic
 * Acts as intermediary between Controller and DAO
 */
public class CheckoutService {

    private final CheckoutDAO checkoutDAO;

    public CheckoutService() {
        this.checkoutDAO = new CheckoutDAO();
    }

    /**
     * Add a new checkout
     * @param checkout Checkout to add
     * @return true if successful
     * @throws IllegalArgumentException if validation fails
     */
    public boolean addCheckout(Checkout checkout) {
        validateCheckout(checkout);
        return checkoutDAO.create(checkout);
    }

    /**
     * Get all checkouts
     * @return List of all checkouts
     */
    public List<Checkout> getAllCheckouts() {
        return checkoutDAO.findAll();
    }

    /**
     * Find checkout by ID
     * @param checkoutId The checkout ID
     * @return Checkout if found, null otherwise
     */
    public Checkout getCheckoutById(int checkoutId) {
        return checkoutDAO.findById(checkoutId);
    }

    /**
     * Update an existing checkout
     * @param checkout Checkout with updated data
     * @return true if successful
     * @throws IllegalArgumentException if validation fails
     */
    public boolean updateCheckout(Checkout checkout) {
        validateCheckout(checkout);
        return checkoutDAO.update(checkout);
    }

    /**
     * Delete a checkout
     * @param checkoutId The checkout ID to delete
     * @return true if successful
     */
    public boolean deleteCheckout(int checkoutId) {
        if (checkoutId <= 0) {
            throw new IllegalArgumentException("Invalid checkout ID");
        }
        return checkoutDAO.delete(checkoutId);
    }

    /**
     * Validate checkout data
     * @param checkout Checkout to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCheckout(Checkout checkout) {
        if (checkout == null) {
            throw new IllegalArgumentException("Checkout cannot be null");
        }
        
        if (checkout.getFlightId() == null || checkout.getFlightId().trim().isEmpty()) {
            throw new IllegalArgumentException("Flight ID is required");
        }
        
        if (checkout.getIdUser() <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        
        if (checkout.getPassengerNbr() <= 0) {
            throw new IllegalArgumentException("Number of passengers must be positive");
        }
        
        if (checkout.getStatusReservation() == null || checkout.getStatusReservation().trim().isEmpty()) {
            throw new IllegalArgumentException("Reservation status is required");
        }
        
        if (checkout.getTotalPrix() <= 0) {
            throw new IllegalArgumentException("Total price must be positive");
        }
    }

    /**
     * Get all pending checkouts
     * @return List of pending checkouts
     */
    public List<Checkout> getPendingCheckouts() {
        return checkoutDAO.findPendingCheckouts();
    }

    /**
     * Approve a checkout
     * @param checkoutId The checkout ID
     * @param userId The user ID making the approval
     * @return true if successful
     */
    public boolean approveCheckout(int checkoutId, int userId) {
        Checkout checkout = checkoutDAO.findById(checkoutId);
        if (checkout == null) {
            throw new IllegalArgumentException("Checkout not found");
        }
        checkout.setStatusReservation("Confirmed");
        return checkoutDAO.update(checkout);
    }

    /**
     * Reject a checkout
     * @param checkoutId The checkout ID
     * @param userId The user ID making the rejection
     * @param reason The rejection reason
     * @return true if successful
     */
    public boolean rejectCheckout(int checkoutId, int userId, String reason) {
        Checkout checkout = checkoutDAO.findById(checkoutId);
        if (checkout == null) {
            throw new IllegalArgumentException("Checkout not found");
        }
        checkout.setStatusReservation("Rejected");
        return checkoutDAO.update(checkout);
    }

    /**
     * Cancel a checkout
     * @param checkoutId The checkout ID
     * @param userId The user ID making the cancellation
     * @return true if successful
     */
    public boolean cancelCheckout(int checkoutId, int userId) {
        Checkout checkout = checkoutDAO.findById(checkoutId);
        if (checkout == null) {
            throw new IllegalArgumentException("Checkout not found");
        }
        checkout.setStatusReservation("Cancelled");
        return checkoutDAO.update(checkout);
    }
}
