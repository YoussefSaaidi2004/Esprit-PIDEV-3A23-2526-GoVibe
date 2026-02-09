package com.example.gestionvol.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL =
            "jdbc:mysql://localhost:3306/gestion_vol?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private DBConnection() {}

    /**
     * Return a new JDBC Connection. DAOs should close the Connection when done.
     */
    public static Connection getConnection() {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connected to MySQL");
            return conn;
        } catch (SQLException e) {
            System.err.println("❌ DB Connection failed");
            e.printStackTrace();
            return null;
        }
    }
}
