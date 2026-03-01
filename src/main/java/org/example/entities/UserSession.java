package org.example.entities;

import java.sql.Timestamp;

/**
 * Entity representing an active user session.
 * Tracks device, location, and activity for session management.
 */
public class UserSession {
    private String id;
    private int userId;
    private String ipAddress;
    private String deviceName;
    private String country;
    private String city;
    private Timestamp loginDate;
    private Timestamp lastActivity;
    private boolean isActive;

    public UserSession() {}

    public UserSession(String id, int userId, String ipAddress, String deviceName,
                       String country, String city) {
        this.id = id;
        this.userId = userId;
        this.ipAddress = ipAddress;
        this.deviceName = deviceName;
        this.country = country;
        this.city = city;
        this.isActive = true;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public Timestamp getLoginDate() { return loginDate; }
    public void setLoginDate(Timestamp loginDate) { this.loginDate = loginDate; }

    public Timestamp getLastActivity() { return lastActivity; }
    public void setLastActivity(Timestamp lastActivity) { this.lastActivity = lastActivity; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    @Override
    public String toString() {
        return "UserSession{" +
                "id='" + id + '\'' +
                ", userId=" + userId +
                ", device='" + deviceName + '\'' +
                ", country='" + country + '\'' +
                ", city='" + city + '\'' +
                ", active=" + isActive +
                '}';
    }
}
