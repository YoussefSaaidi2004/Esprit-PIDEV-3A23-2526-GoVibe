package org.example.utils;

import org.example.config.UnifiedDatabaseManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class SchemaFixer {
    public static void main(String[] args) {
        System.out.println("🚀 [SchemaFixer] Starting database schema fix...");
        
        String[] alterStatements = {
            "ALTER TABLE checkout ADD COLUMN IF NOT EXISTS passenger_name VARCHAR(255)",
            "ALTER TABLE checkout ADD COLUMN IF NOT EXISTS passenger_email VARCHAR(255)",
            "ALTER TABLE checkout ADD COLUMN IF NOT EXISTS passenger_phone VARCHAR(50)",
            "ALTER TABLE checkout ADD COLUMN IF NOT EXISTS payment_method VARCHAR(50) DEFAULT 'CREDIT_CARD'",
            "ALTER TABLE checkout ADD COLUMN IF NOT EXISTS seat_preference VARCHAR(20) DEFAULT 'WINDOW'",
            "ALTER TABLE checkout ADD COLUMN IF NOT EXISTS travel_class VARCHAR(20) DEFAULT 'Economy'"
        };

        try (Connection conn = UnifiedDatabaseManager.getConnection()) {
            if (conn == null) {
                System.err.println("❌ [SchemaFixer] Could not get database connection!");
                return;
            }

            try (Statement stmt = conn.createStatement()) {
                for (String sql : alterStatements) {
                    try {
                        System.out.println("Executing: " + sql);
                        stmt.execute(sql);
                        System.out.println("✅ Success.");
                    } catch (SQLException e) {
                        // Some versions of MySQL might not support IF NOT EXISTS in ALTER TABLE
                        if (e.getErrorCode() == 1060) { // Duplicate column name
                            System.out.println("ℹ️ Column already exists, skipping.");
                        } else {
                            System.err.println("❌ Error: " + e.getMessage());
                        }
                    }
                }
            }
            System.out.println("✨ [SchemaFixer] Database schema fix completed.");
        } catch (SQLException e) {
            System.err.println("❌ [SchemaFixer] SQL Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
