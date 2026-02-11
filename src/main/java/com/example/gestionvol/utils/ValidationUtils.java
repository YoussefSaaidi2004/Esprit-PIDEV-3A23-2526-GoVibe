package com.example.gestionvol.utils;

import java.util.regex.Pattern;

/**
 * Utility class for input validation
 */
public class ValidationUtils {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,9}$"
    );
    
    /**
     * Validates if a string is not null or empty
     */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
    
    /**
     * Validates email format
     */
    public static boolean isValidEmail(String email) {
        return isNotEmpty(email) && EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * Validates phone number format
     */
    public static boolean isValidPhone(String phone) {
        return isNotEmpty(phone) && PHONE_PATTERN.matcher(phone).matches();
    }
    
    /**
     * Validates passenger count against available seats
     */
    public static boolean isValidPassengerCount(int passengers, int availableSeats) {
        return passengers > 0 && passengers <= availableSeats;
    }
    
    /**
     * Gets a user-friendly validation error message
     */
    public static String getValidationMessage(String fieldName, String validationType) {
        switch (validationType) {
            case "required":
                return fieldName + " is required";
            case "email":
                return "Please enter a valid email address";
            case "phone":
                return "Please enter a valid phone number (e.g., +216-12-345-678)";
            case "seats":
                return "Please select a valid number of passengers";
            default:
                return "Invalid " + fieldName;
        }
    }
}
