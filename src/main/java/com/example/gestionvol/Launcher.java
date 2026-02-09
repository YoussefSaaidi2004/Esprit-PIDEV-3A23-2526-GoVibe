package com.example.gestionvol;

import javafx.application.Application;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Scanner;

public class Launcher {

    static final String URL = "jdbc:mysql://localhost:3306/gestion_vol?useSSL=false&serverTimezone=UTC";
    static final String USER = "root";
    static final String PASSWORD = "";

    static Connection cnx;
    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {

        connectDB();

        while (true) {
            System.out.println("""
            ===== GESTION VOL =====
            1. Flight CRUD
            2. Checkout CRUD
            0. Exit
            """);

            int choice = sc.nextInt();

            switch (choice) {
                case 1 -> flightMenu();
                case 2 -> checkoutMenu();
                case 0 -> System.exit(0);
            }
        }
    }

    // ================= DB =================
    static void connectDB() {
        try {
            cnx = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connected to DB");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================= FLIGHT =================
    static void flightMenu() {
        System.out.println("""
        --- Flight CRUD ---
        1. Add Flight
        2. List Flights
        3. Update Flight (ALL)
        4. Delete Flight
        """);

        int c = sc.nextInt();

        try {
            switch (c) {
                case 1 -> addFlight();
                case 2 -> listFlights();
                case 3 -> updateFlight();
                case 4 -> deleteFlight();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void addFlight() throws SQLException {
        String sql = """
        INSERT INTO vol VALUES (?,?,?,?,?,?,?,?,?,?)
        """;

        PreparedStatement ps = cnx.prepareStatement(sql);

        System.out.print("Flight ID: ");
        ps.setString(1, sc.next());

        System.out.print("Departure airport: ");
        ps.setString(2, sc.next());

        System.out.print("Destination: ");
        ps.setString(3, sc.next());

        ps.setTime(4, Time.valueOf("10:00:00"));
        ps.setTime(5, Time.valueOf("13:00:00"));

        System.out.print("Class: ");
        ps.setString(6, sc.next());

        System.out.print("Airline: ");
        ps.setString(7, sc.next());

        System.out.print("Price: ");
        ps.setInt(8, sc.nextInt());

        System.out.print("Available seats: ");
        ps.setInt(9, sc.nextInt());

        ps.setString(10, "No description");

        ps.executeUpdate();
        System.out.println("✈️ Flight added");
    }

    static void listFlights() throws SQLException {
        ResultSet rs = cnx.createStatement()
                .executeQuery("SELECT * FROM vol");

        while (rs.next()) {
            System.out.println(
                    rs.getString("flight_id") + " | " +
                            rs.getString("destination") + " | " +
                            rs.getInt("prix") + " DT | Seats: " +
                            rs.getInt("available_seats")
            );
        }
    }

    static void updateFlight() throws SQLException {
        String sql = """
        UPDATE vol SET
        departure_airport=?, destination=?, departure_time=?, arrival_time=?,
        classe_chaise=?, airline=?, prix=?, available_seats=?, description=?
        WHERE flight_id=?
        """;

        PreparedStatement ps = cnx.prepareStatement(sql);

        System.out.print("Flight ID to update: ");
        String id = sc.next();

        System.out.print("New departure airport: ");
        ps.setString(1, sc.next());

        System.out.print("New destination: ");
        ps.setString(2, sc.next());

        ps.setTime(3, Time.valueOf("11:00:00"));
        ps.setTime(4, Time.valueOf("14:00:00"));

        System.out.print("New class: ");
        ps.setString(5, sc.next());

        System.out.print("New airline: ");
        ps.setString(6, sc.next());

        System.out.print("New price: ");
        ps.setInt(7, sc.nextInt());

        System.out.print("New available seats: ");
        ps.setInt(8, sc.nextInt());

        ps.setString(9, "Updated flight");
        ps.setString(10, id);

        ps.executeUpdate();
        System.out.println("✏️ Flight updated");
    }

    static void deleteFlight() throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "DELETE FROM vol WHERE flight_id=?"
        );

        System.out.print("Flight ID: ");
        ps.setString(1, sc.next());

        ps.executeUpdate();
        System.out.println("🗑 Flight deleted");
    }

    // ================= CHECKOUT =================
    static void checkoutMenu() {
        System.out.println("""
        --- Checkout CRUD ---
        1. Add Checkout
        2. List Checkouts
        3. Update Checkout
        4. Delete Checkout
        """);

        int c = sc.nextInt();

        try {
            switch (c) {
                case 1 -> addCheckout();
                case 2 -> listCheckouts();
                case 3 -> updateCheckout();
                case 4 -> deleteCheckout();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void addCheckout() throws SQLException {
        String sql = """
        INSERT INTO checkout
        (flight_id,id_user,reservation_date,passenger_nbr,status_reservation,total_prix)
        VALUES (?,?,?,?,?,?)
        """;

        PreparedStatement ps = cnx.prepareStatement(sql);

        System.out.print("Flight ID: ");
        ps.setString(1, sc.next());

        System.out.print("User ID: ");
        ps.setInt(2, sc.nextInt());

        ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));

        System.out.print("Passengers: ");
        ps.setInt(4, sc.nextInt());

        System.out.print("Status: ");
        ps.setString(5, sc.next());

        System.out.print("Total price: ");
        ps.setInt(6, sc.nextInt());

        ps.executeUpdate();
        System.out.println("🧾 Checkout added");
    }

    static void listCheckouts() throws SQLException {
        ResultSet rs = cnx.createStatement()
                .executeQuery("SELECT * FROM checkout");

        while (rs.next()) {
            System.out.println(
                    rs.getInt("checkout_id") + " | " +
                            rs.getString("flight_id") + " | " +
                            rs.getString("status_reservation") + " | " +
                            rs.getInt("total_prix") + " DT"
            );
        }
    }

    static void updateCheckout() throws SQLException {
        String sql = """
        UPDATE checkout SET
        passenger_nbr=?, status_reservation=?, total_prix=?
        WHERE checkout_id=?
        """;

        PreparedStatement ps = cnx.prepareStatement(sql);

        System.out.print("Checkout ID: ");
        int id = sc.nextInt();

        System.out.print("New passengers: ");
        ps.setInt(1, sc.nextInt());

        System.out.print("New status: ");
        ps.setString(2, sc.next());

        System.out.print("New total price: ");
        ps.setInt(3, sc.nextInt());

        ps.setInt(4, id);

        ps.executeUpdate();
        System.out.println("✏️ Checkout updated");
    }

    static void deleteCheckout() throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "DELETE FROM checkout WHERE checkout_id=?"
        );

        System.out.print("Checkout ID: ");
        ps.setInt(1, sc.nextInt());

        ps.executeUpdate();
        System.out.println("🗑 Checkout deleted");
    }
}
