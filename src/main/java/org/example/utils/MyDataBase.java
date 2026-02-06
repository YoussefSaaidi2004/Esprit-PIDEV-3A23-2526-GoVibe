package org.example.utils;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {
    private final String url = "jdbc:mysql://127.0.0.1:3306/projet?useSSL=false&serverTimezone=UTC";
    private final String user = "root";
    private final String password = "";
    private Connection myConnection;
    private static MyDataBase instance;
    public MyDataBase() {
        try {
            myConnection = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to database successfully");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    public Connection getConnection() {
        return myConnection;
    }
    public static MyDataBase getInstance() {
        if (instance == null) {
            instance = new MyDataBase();

        }
        return instance;
    }
}
