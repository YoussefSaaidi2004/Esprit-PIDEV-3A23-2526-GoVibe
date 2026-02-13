package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {

    private static MyDataBase instance;

    private final String URL="jdbc:mysql://localhost:3306/govibe_project";
    private final String USER="root";
    private final String PASSWORD="";
    private Connection conn;

    private MyDataBase() {
        try {
            conn =  DriverManager.getConnection(URL,USER,PASSWORD);
            System.out.println("Connected to database successfully");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static synchronized MyDataBase getInstance() {
        if (instance == null) {
            instance = new MyDataBase();
        }
        return instance;
    }
    public Connection getConnection(){
        return conn;
    }
}
