package com.example.gestionvol;

/**
 * Simple test to verify JavaFX basics work
 * Run this to test if JavaFX is configured correctly
 */
public class SimpleTest {
    public static void main(String[] args) {
        System.out.println("=== GoVibe Diagnostic Test ===");
        System.out.println("✅ Java is working!");
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("JavaFX modules available: " + System.getProperty("javafx.version"));
        
        // Test database connection
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✅ MySQL driver found!");
        } catch (ClassNotFoundException e) {
            System.out.println("❌ MySQL driver NOT found!");
        }
        
        // Test resource loading
        try {
            var resource = SimpleTest.class.getResource("/main-view.fxml");
            if (resource != null) {
                System.out.println("✅ FXML resource found: " + resource);
            } else {
                System.out.println("❌ FXML resource NOT found!");
            }
        } catch (Exception e) {
            System.out.println("❌ Error loading resource: " + e.getMessage());
        }
        
        System.out.println("=== Test Complete ===");
    }
}
