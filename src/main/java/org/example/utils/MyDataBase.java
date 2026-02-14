package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {

    private final String url = "jdbc:mysql://127.0.0.1:3306/projet?useSSL=false&serverTimezone=UTC";
    private final String user = "root";
    private final String password = "";

    private Connection connection;
    private static MyDataBase instance;

    private MyDataBase() {
        try {
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("✅ Connected to database successfully");
            checkSchema(); // Auto-migration
        } catch (SQLException e) {
            System.out.println("❌ DB Connection error: " + e.getMessage());
        }
    }

    private void checkSchema() {
        try {
            java.sql.DatabaseMetaData meta = connection.getMetaData();
            java.sql.ResultSet rs = meta.getColumns(null, null, "activite", "status");
            if (!rs.next()) {
                System.out.println("⚠️ Column 'status' missing. Adding it now...");
                try (java.sql.Statement st = connection.createStatement()) {
                    st.executeUpdate("ALTER TABLE activite ADD COLUMN status VARCHAR(20) DEFAULT 'Confirmed'");
                    System.out.println("✅ Column 'status' added successfully.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static MyDataBase getInstance() {
        if (instance == null) {
            instance = new MyDataBase();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}