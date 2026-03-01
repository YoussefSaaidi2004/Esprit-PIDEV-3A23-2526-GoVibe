package org.example.services;

import org.example.entities.UserSession;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service for managing user sessions.
 * Tracks active devices, detects new devices/countries.
 */
public class UserSessionService {

    private final Connection connection;

    public UserSessionService() {
        connection = MyDataBase.getInstance().getConnection();
    }

    /**
     * Creates a new active session for the user.
     * @return the generated session ID
     */
    public String createSession(int userId, String ip, String device, String country, String city) {

        String sessionId = UUID.randomUUID().toString();
        String sql = "INSERT INTO user_sessions (id, user_id, ip_address, device_name, country, city) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ps.setInt(2, userId);
            ps.setString(3, ip);
            ps.setString(4, device);
            ps.setString(5, country);
            ps.setString(6, city);
            ps.executeUpdate();
            System.out.println("✅ [UserSessionService] Session created: " + sessionId);
        } catch (SQLException e) {
            System.err.println("❌ [UserSessionService] Error creating session: " + e.getMessage());
        }
        return sessionId;
    }

    /**
     * Gets all active sessions for a user.
     */
    public List<UserSession> getActiveSessions(int userId) {
        List<UserSession> sessions = new ArrayList<>();
        String sql = "SELECT * FROM user_sessions WHERE user_id = ? AND is_active = TRUE ORDER BY login_date DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                sessions.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ [UserSessionService] Error fetching sessions: " + e.getMessage());
        }
        return sessions;
    }

    /**
     * Gets all distinct countries from past sessions for a user.
     * Used to detect new_country feature.
     */
    public Set<String> getUserCountries(int userId) {
        Set<String> countries = new HashSet<>();
        String sql = "SELECT DISTINCT country FROM user_sessions WHERE user_id = ? AND country IS NOT NULL";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                countries.add(rs.getString("country").toLowerCase());
            }
        } catch (SQLException e) {
            System.err.println("❌ [UserSessionService] Error fetching countries: " + e.getMessage());
        }
        return countries;
    }

    /**
     * Gets all distinct devices from past sessions for a user.
     * Used to detect new_device feature.
     */
    public Set<String> getUserDevices(int userId) {
        Set<String> devices = new HashSet<>();
        String sql = "SELECT DISTINCT device_name FROM user_sessions WHERE user_id = ? AND device_name IS NOT NULL";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                devices.add(rs.getString("device_name").toLowerCase());
            }
        } catch (SQLException e) {
            System.err.println("❌ [UserSessionService] Error fetching devices: " + e.getMessage());
        }
        return devices;
    }

    /**
     * Deactivates a specific session (remote logout).
     */
    public void deactivateSession(String sessionId) {
        String sql = "UPDATE user_sessions SET is_active = FALSE WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ps.executeUpdate();
            System.out.println("✅ [UserSessionService] Session deactivated: " + sessionId);
        } catch (SQLException e) {
            System.err.println("❌ [UserSessionService] Error deactivating session: " + e.getMessage());
        }
    }

    /**
     * Deactivates all sessions for a user (logout everywhere).
     */
    public void deactivateAllSessions(int userId) {
        String sql = "UPDATE user_sessions SET is_active = FALSE WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            int count = ps.executeUpdate();
            System.out.println("✅ [UserSessionService] Deactivated " + count + " sessions for user " + userId);
        } catch (SQLException e) {
            System.err.println("❌ [UserSessionService] Error deactivating sessions: " + e.getMessage());
        }
    }

    private UserSession mapResultSet(ResultSet rs) throws SQLException {
        UserSession s = new UserSession();
        s.setId(rs.getString("id"));
        s.setUserId(rs.getInt("user_id"));
        s.setIpAddress(rs.getString("ip_address"));
        s.setDeviceName(rs.getString("device_name"));
        s.setCountry(rs.getString("country"));
        s.setCity(rs.getString("city"));
        s.setLoginDate(rs.getTimestamp("login_date"));
        s.setLastActivity(rs.getTimestamp("last_activity"));
        s.setActive(rs.getBoolean("is_active"));
        return s;
    }
}
