package org.example.utils;

import java.sql.Connection;
import org.example.config.UnifiedDatabaseManager;

/**
 * LEGACY CLASS - Use UnifiedDatabaseManager directly for new code
 * This class is maintained for backward compatibility only.
 * It delegates to UnifiedDatabaseManager to ensure consistent database access.
 */
@Deprecated
public class MyDataBase {

    private static MyDataBase instance;

    private MyDataBase() {
        System.out.println("⚠️  MyDataBase is DEPRECATED. Use UnifiedDatabaseManager instead.");
    }

    public static synchronized MyDataBase getInstance() {
        if (instance == null) {
            instance = new MyDataBase();
        }
        return instance;
    }

    /**
     * Get connection from unified manager
     * @return Connection from UnifiedDatabaseManager
     */
    public Connection getConnection() {
        return UnifiedDatabaseManager.getInstance().getConnection();
    }

    public Connection getMyConnection() {
        return getConnection();
    }
}
