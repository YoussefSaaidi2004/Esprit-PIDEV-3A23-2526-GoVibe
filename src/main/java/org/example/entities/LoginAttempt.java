package org.example.entities;

import java.sql.Timestamp;

/**
 * Entity representing a login attempt record.
 * Tracks every authentication attempt for security analysis.
 */
public class LoginAttempt {
    private int id;
    private int userId;
    private String ipAddress;
    private String device;
    private String country;
    private Timestamp loginTime;
    private boolean success;
    private double riskScore;
    private String authLevel;

    public LoginAttempt() {}

    public LoginAttempt(int userId, String ipAddress, String device, String country, boolean success) {
        this.userId = userId;
        this.ipAddress = ipAddress;
        this.device = device;
        this.country = country;
        this.success = success;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Timestamp getLoginTime() { return loginTime; }
    public void setLoginTime(Timestamp loginTime) { this.loginTime = loginTime; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public double getRiskScore() { return riskScore; }
    public void setRiskScore(double riskScore) { this.riskScore = riskScore; }

    public String getAuthLevel() { return authLevel; }
    public void setAuthLevel(String authLevel) { this.authLevel = authLevel; }

    @Override
    public String toString() {
        return "LoginAttempt{" +
                "id=" + id +
                ", userId=" + userId +
                ", ip='" + ipAddress + '\'' +
                ", device='" + device + '\'' +
                ", country='" + country + '\'' +
                ", success=" + success +
                ", riskScore=" + riskScore +
                ", authLevel='" + authLevel + '\'' +
                '}';
    }
}
