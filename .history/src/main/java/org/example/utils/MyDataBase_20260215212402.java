package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database connection manager for GoVibe.
 * Connects directly to GoVibe_Project database.
 */
@Deprecated
public class MyDataBase {

    private static MyDataBase instance;
    private Connection connection;

    private static final String URL = "jdbc:mysql://localhost:3306/govibe?useSSL=false&serverTimezone=UTC&autoReconnect=true";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private MyDataBase() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ [MyDataBase] Connected to GoVibe_Project");
        } catch (SQLException e) {
            System.err.println("❌ [MyDataBase] Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public Connection getMyConnection() {
        return getConnection();
    }
}
