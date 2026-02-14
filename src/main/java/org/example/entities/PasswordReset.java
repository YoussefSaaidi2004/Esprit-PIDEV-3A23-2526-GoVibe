package org.example.entities;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class PasswordReset {
    private int id;
    private String email;
    private String token;
    private LocalDateTime expirationDate;

    public PasswordReset() {
    }

    public PasswordReset(String email, String token, LocalDateTime expirationDate) {
        this.email = email;
        this.token = token;
        this.expirationDate = expirationDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    @Override
    public String toString() {
        return "PasswordReset{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", token='" + token + '\'' +
                ", expirationDate=" + expirationDate +
                '}';
    }
}
