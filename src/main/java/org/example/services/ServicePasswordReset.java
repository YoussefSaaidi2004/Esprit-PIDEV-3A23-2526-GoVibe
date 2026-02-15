package org.example.services;

import org.example.entities.PasswordReset;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;

public class ServicePasswordReset {
    private Connection connection;

    public ServicePasswordReset() {
        connection = MyDataBase.getInstance().getConnection();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String req = "CREATE TABLE IF NOT EXISTS password_resets (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "email VARCHAR(150) NOT NULL, " +
                "token VARCHAR(255) NOT NULL, " +
                "expiration_date DATETIME NOT NULL" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(req);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createToken(PasswordReset passwordReset) throws SQLException {
        String req = "INSERT INTO password_resets (email, token, expiration_date) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setString(1, passwordReset.getEmail());
            ps.setString(2, passwordReset.getToken());
            ps.setTimestamp(3, Timestamp.valueOf(passwordReset.getExpirationDate()));
            ps.executeUpdate();
        }
    }

    public PasswordReset getByToken(String token) throws SQLException {
        String req = "SELECT * FROM password_resets WHERE token = ?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PasswordReset pr = new PasswordReset();
                    pr.setId(rs.getInt("id"));
                    pr.setEmail(rs.getString("email"));
                    pr.setToken(rs.getString("token"));
                    pr.setExpirationDate(rs.getTimestamp("expiration_date").toLocalDateTime());
                    return pr;
                }
            }
        }
        return null;
    }

    public void deleteToken(String token) throws SQLException {
        String req = "DELETE FROM password_resets WHERE token = ?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setString(1, token);
            ps.executeUpdate();
        }
    }
    
    // Check if email exists in personne table
    public boolean emailExists(String email) throws SQLException {
        String req = "SELECT COUNT(*) FROM personne WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    // Update password in personne table by email
    public void updatePassword(String email, String hashedPassword) throws SQLException {
        String req = "UPDATE personne SET password = ? WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setString(1, hashedPassword);
            ps.setString(2, email);
            ps.executeUpdate();
        }
    }
}
