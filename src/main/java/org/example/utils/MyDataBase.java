package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database connection manager for GoVibe.
 * Connects directly to govibe database.
 */
@Deprecated
public class MyDataBase {

    private static MyDataBase instance;
    private Connection connection;

    private static final String URL = "jdbc:mysql://localhost:3306/govibe_project?useSSL=false&serverTimezone=UTC&autoReconnect=true";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private MyDataBase() {
        // Connection is now acquired lazily in getConnection()
        System.out.println("✅ [MyDataBase] Instance initialized (Lazy Connection)");
    }

    public static synchronized MyDataBase getInstance() {
        if (instance == null) {
            instance = new MyDataBase();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ [MyDataBase] Connected to govibe_project");
            }
        } catch (SQLException e) {
            System.err.println("❌ [MyDataBase] Connection failed for URL: " + URL);
            System.err.println("❌ Error: " + e.getMessage());
            // We throw here to prevent NullPointerException in services
            throw new RuntimeException("Database connection failed. Please ensure MySQL is running and database 'govibe_project' exists.", e);
        }
        return connection;
    }

    public Connection getMyConnection() {
        return getConnection();
    }
}
