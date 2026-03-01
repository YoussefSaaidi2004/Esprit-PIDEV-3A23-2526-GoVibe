package com.myapp;

import java.sql.Connection;
import java.sql.DriverManager;

public class TestConnection {
    public static void main(String[] args) {
        String url = "jdbc:mysql://127.0.0.1:3306/govibe_project?useSSL=false&serverTimezone=UTC";
        String user = "root";
        String password = ""; // vide si WAMP par défaut

        try (Connection cnx = DriverManager.getConnection(url, user, password)) {
            System.out.println("✅ Connexion OK à MySQL !");
        } catch (Exception e) {
            System.out.println("❌ Erreur connexion !");
            e.printStackTrace();
        }
    }
}