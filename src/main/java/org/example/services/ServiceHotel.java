package org.example.services;

import org.example.entities.Hotel;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceHotel {

    private Connection connection;

    public ServiceHotel() {
        // Initialisation de la connexion
        connection = MyDataBase.getInstance().getMyConnection();
    }

    // 🔹 Insert
    public void insert(Hotel hotel) throws SQLException {
        if (connection == null) {
            throw new IllegalStateException("Connexion BD = NULL. Verify MyDataBase (URL/user/password).");
        }

        String sql = "INSERT INTO hotel(nom, adresse, ville, nombre_etoiles, description, photo_url, budget) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, hotel.getNom());
        stmt.setString(2, hotel.getAdresse());
        stmt.setString(3, hotel.getVille());
        stmt.setInt(4, hotel.getNombreEtoiles());
        stmt.setString(5, hotel.getDescription());
        stmt.setString(6, hotel.getPhotoUrl());
        stmt.setDouble(7, hotel.getBudget()); // 🔹 Budget ajouté
        stmt.executeUpdate();
    }


    // 🔹 Update
    public void update(Hotel hotel) throws SQLException {
        String sql = "UPDATE hotel SET nom=?, adresse=?, ville=?, nombre_etoiles=?, description=?, photo_url=?, budget=? WHERE id=?";
        PreparedStatement stmt = connection.prepareStatement(sql);

        stmt.setString(1, hotel.getNom());
        stmt.setString(2, hotel.getAdresse());
        stmt.setString(3, hotel.getVille());
        stmt.setInt(4, hotel.getNombreEtoiles());
        stmt.setString(5, hotel.getDescription());
        stmt.setString(6, hotel.getPhotoUrl());
        stmt.setDouble(7, hotel.getBudget()); // 🔹 Budget ajouté
        stmt.setInt(8, hotel.getId());

        stmt.executeUpdate();
    }

    // 🔹 Delete
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM hotel WHERE id=?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, id);
        stmt.executeUpdate();
    }

    // 🔹 Show
    public List<Hotel> show() throws SQLException {
        List<Hotel> hotels = new ArrayList<>();
        String sql = "SELECT * FROM hotel";

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        while (rs.next()) {
            Hotel hotel = new Hotel(
                    rs.getInt("id"),
                    rs.getString("nom"),
                    rs.getString("adresse"),
                    rs.getString("ville"),
                    rs.getInt("nombre_etoiles"),
                    rs.getString("description"),
                    rs.getString("photo_url"),
                    rs.getDouble("budget") // 🔹 Budget ajouté
            );

            hotels.add(hotel);
        }

        return hotels;
    }

    // 🔹 Find By ID
    public Hotel findById(int id) throws SQLException {
        String sql = "SELECT * FROM hotel WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Hotel(
                            rs.getInt("id"),
                            rs.getString("nom"),
                            rs.getString("adresse"),
                            rs.getString("ville"),
                            rs.getInt("nombre_etoiles"),
                            rs.getString("description"),
                            rs.getString("photo_url"),
                            rs.getDouble("budget")
                    );
                }
            }
        }
        return null;
    }
}
