package com.example.gestionvol.util;

import com.example.gestionvol.config.DBConnection;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class DBMigration {
    public static void main(String[] args) {
        migrate();
    }

    public static void migrate() {
        String[] sqls = {
            // Flight table migrations
            "ALTER TABLE vol ADD COLUMN IF NOT EXISTS total_seats INT DEFAULT 150",
            "ALTER TABLE vol ADD COLUMN IF NOT EXISTS description TEXT",
            
            // Checkout table migrations
            "ALTER TABLE checkout ADD COLUMN IF NOT EXISTS passenger_name VARCHAR(100)",
            "ALTER TABLE checkout ADD COLUMN IF NOT EXISTS passenger_email VARCHAR(100)",
            "ALTER TABLE checkout ADD COLUMN IF NOT EXISTS passenger_phone VARCHAR(20)",
            "ALTER TABLE checkout ADD COLUMN IF NOT EXISTS payment_method VARCHAR(50) DEFAULT 'Credit Card'",
            "ALTER TABLE checkout ADD COLUMN IF NOT EXISTS seat_preference VARCHAR(20) DEFAULT 'WINDOW'",
            "ALTER TABLE checkout ADD COLUMN IF NOT EXISTS travel_class VARCHAR(20) DEFAULT 'Economy'"
        };

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : sqls) {
                try {
                    stmt.execute(sql);
                    System.out.println("✅ Migrated: " + sql);
                } catch (SQLException e) {
                    // Ignore if column already exists (Error 1060)
                    if (e.getErrorCode() == 1060) {
                        System.out.println("ℹ️ Column already exists, skipping.");
                    } else {
                        System.err.println("❌ Migration failed: " + sql);
                        e.printStackTrace();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
