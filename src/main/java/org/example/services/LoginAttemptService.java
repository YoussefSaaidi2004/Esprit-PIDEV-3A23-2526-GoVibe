package org.example.services;

import org.example.entities.LoginAttempt;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for recording and querying login attempts.
 * Used by the risk scoring engine to calculate failed_attempts feature.
 */
public class LoginAttemptService {

    private final Connection connection;

    public LoginAttemptService() {
        connection = MyDataBase.getInstance().getConnection();
    }

    /**
     * Records a login attempt in the database.
     */
    public void recordAttempt(int userId, String ip, String device, String country,
                              boolean success, double riskScore, String authLevel) {
        String sql = "INSERT INTO login_attempts (user_id, ip_address, device, country, success, risk_score, auth_level) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, ip);
            ps.setString(3, device);
            ps.setString(4, country);
            ps.setBoolean(5, success);
            ps.setDouble(6, riskScore);
            ps.setString(7, authLevel);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ [LoginAttemptService] Error recording attempt: " + e.getMessage());
        }
    }

    /**
     * Counts failed login attempts for a user in the last N minutes.
     */
    public int getRecentFailedCount(int userId, int minutes) {
        String sql = "SELECT COUNT(*) FROM login_attempts WHERE user_id = ? AND success = FALSE " +
                     "AND login_time >= DATE_SUB(NOW(), INTERVAL ? MINUTE)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, minutes);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ [LoginAttemptService] Error counting failed attempts: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Gets recent login attempts for a user (for audit/display).
     */
    public List<LoginAttempt> getRecentAttempts(int userId, int limit) {
        List<LoginAttempt> attempts = new ArrayList<>();
        String sql = "SELECT * FROM login_attempts WHERE user_id = ? ORDER BY login_time DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                LoginAttempt a = new LoginAttempt();
                a.setId(rs.getInt("id"));
                a.setUserId(rs.getInt("user_id"));
                a.setIpAddress(rs.getString("ip_address"));
                a.setDevice(rs.getString("device"));
                a.setCountry(rs.getString("country"));
                a.setLoginTime(rs.getTimestamp("login_time"));
                a.setSuccess(rs.getBoolean("success"));
                a.setRiskScore(rs.getDouble("risk_score"));
                a.setAuthLevel(rs.getString("auth_level"));
                attempts.add(a);
            }
        } catch (SQLException e) {
            System.err.println("❌ [LoginAttemptService] Error fetching attempts: " + e.getMessage());
        }
        return attempts;
    }
}
