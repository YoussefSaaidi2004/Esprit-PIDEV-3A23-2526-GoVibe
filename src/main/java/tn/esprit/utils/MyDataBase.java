package tn.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.function.Consumer;
import javafx.application.Platform;

public class MyDataBase {

    private final String URL = "jdbc:mysql://127.0.0.1:3306/govibe_project" +
        "?useSSL=false&serverTimezone=UTC&autoReconnect=true" +
        "&connectTimeout=10000&socketTimeout=10000";
    private final String USERNAME = "root";
    private final String PASSWORD = "";
    private Connection connection;
    private static MyDataBase instance;

    private MyDataBase() {}

    public static MyDataBase getInstance() {
        if (instance == null) {
            instance = new MyDataBase();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                System.out.println("✅ [MyDataBase] Connected (synchronous fallback)");
            }
        } catch (SQLException e) {
            System.err.println("❌ [MyDataBase] Connection error: " + e.getMessage());
        }
        return connection;
    }

    public void initAsync(Runnable onReady, Consumer<String> onError) {
        new Thread(() -> {
            try {
                if (connection == null || connection.isClosed()) {
                    connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                    System.out.println("✅ [MyDataBase] Connection established asynchronously.");
                }
                Platform.runLater(() -> { if (onReady != null) onReady.run(); });
            } catch (SQLException e) {
                System.err.println("❌ [MyDataBase] Async error: " + e.getMessage());
                Platform.runLater(() -> { if (onError != null) onError.accept(e.getMessage()); });
            }
        }, "db-init-thread").start();
    }
}
