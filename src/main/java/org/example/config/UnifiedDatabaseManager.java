package org.example.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Unified Database Manager for GoVibe Project
 * Provides a single centralized database connection for both org.example and org.example.gestionvol packages
 * This ensures data consistency and eliminates double-connection issues
 */
public class UnifiedDatabaseManager {

    private static UnifiedDatabaseManager instance;
    
    // Database configuration - unified for all packages
    private static final String URL = "jdbc:mysql://localhost:3306/GoVibe_Project?useSSL=false&serverTimezone=UTC&autoReconnect=true";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    
    private static final int CONNECTION_TIMEOUT = 30; // seconds
    private static final int MAX_RETRIES = 3;

    private UnifiedDatabaseManager() {}

    /**
     * Get singleton instance of UnifiedDatabaseManager
     * @return Singleton instance
     */
    public static synchronized UnifiedDatabaseManager getInstance() {
        if (instance == null) {
            instance = new UnifiedDatabaseManager();
        }
        return instance;
    }

    /**
     * Static method to get a database connection
     * Can be called directly without needing an instance
     * @return Connection object
     */
    public static Connection getConnection() {
        return getInstance().getConnectionInternal();
    }

    /**
     * Get a new database connection (internal method)
     * Implements retry logic for resilience
     * @return Connection object, null if connection fails after retries
     */
    private Connection getConnectionInternal() {
        int attempts = 0;
        SQLException lastException = null;

        while (attempts < MAX_RETRIES) {
            try {
                Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                if (conn != null) {
                    System.out.println("✅ [UnifiedDB] Connected to govibe database (Attempt " + (attempts + 1) + ")");
                    // Verify connection is valid
                    conn.isValid(CONNECTION_TIMEOUT);
                    return conn;
                }
            } catch (SQLException e) {
                attempts++;
                lastException = e;
                System.err.println("⚠️  [UnifiedDB] Connection attempt " + attempts + " failed: " + e.getMessage());
                
                if (attempts < MAX_RETRIES) {
                    try {
                        // Wait before retry with exponential backoff
                        Thread.sleep(1000 * attempts);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // All retries exhausted
        System.err.println("❌ [UnifiedDB] Failed to connect after " + MAX_RETRIES + " attempts");
        if (lastException != null) {
            lastException.printStackTrace();
        }
        return null;
    }

    /**
     * Test the database connection
     * @return true if connection successful, false otherwise
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            if (conn != null) {
                System.out.println("✅ [UnifiedDB] Database connection test PASSED");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ [UnifiedDB] Database connection test FAILED: " + e.getMessage());
        }
        return false;
    }

    /**
     * Get database configuration info (for debugging)
     * @return Configuration string
     */
    public String getConnectionInfo() {
        return "GoVibe Database Manager\n" +
               "URL: " + URL + "\n" +
               "User: " + USER + "\n" +
               "Max Retries: " + MAX_RETRIES + "\n" +
               "Connection Timeout: " + CONNECTION_TIMEOUT + "s";
    }
}
