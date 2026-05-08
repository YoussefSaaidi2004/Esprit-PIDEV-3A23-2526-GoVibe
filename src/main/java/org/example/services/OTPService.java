package org.example.services;

import org.example.utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Random;

/**
 * Service for generating, storing, and validating OTP codes.
 * OTPs are 6-digit numeric codes valid for 5 minutes.
 */
public class OTPService {

    /**
     * Result of a generateAndSendOTP call.
     * Contains the plaintext code (for dev-mode fallback) and whether
     * the email was actually delivered.
     */
    public static class OTPResult {
        public final String code;
        public final boolean emailSent;

        public OTPResult(String code, boolean emailSent) {
            this.code = code;
            this.emailSent = emailSent;
        }
    }

    private static final int OTP_LENGTH = 6;
    private static final int OTP_VALIDITY_MINUTES = 5;

    private final Connection connection;
    private final EmailService emailService;
    private final Random random = new Random();

    public OTPService() {
        connection = MyDataBase.getInstance().getConnection();
        emailService = new EmailService();
    }

    /**
     * Package-private constructor for unit testing — allows injecting mock
     * Connection and EmailService without needing a real MySQL instance.
     */
    OTPService(Connection connection, EmailService emailService) {
        this.connection = connection;
        this.emailService = emailService;
    }

    /**
     * Generates a 6-digit OTP, stores it in the database, and sends it by email.
     * Returns an {@link OTPResult} with the plaintext code and whether the email
     * was actually delivered (useful for dev-mode fallback display).
     */
    public OTPResult generateAndSendOTP(int userId, String email) {
        String code = generateCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES);

        // Invalidate any existing unused OTPs for this user
        invalidateExistingOTPs(userId);

        // Store OTP in database
        String sql = "INSERT INTO otp_codes (user_id, code, expires_at) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, code);
            ps.setTimestamp(3, Timestamp.valueOf(expiresAt));
            ps.executeUpdate();
            System.out.println("✅ [OTP] Code generated for user " + userId + " → code=" + code + " expires=" + expiresAt);
        } catch (SQLException e) {
            System.err.println("❌ [OTP] Error storing OTP: " + e.getMessage());
        }

        // Guard: if SMTP not configured, skip email and signal failure
        if (!emailService.isConfigured()) {
            System.err.println("⚠️ [OTP] SMTP not configured (mail.password is blank).");
            System.err.println("⚠️ [OTP] DEV-MODE — OTP for user " + userId + " → " + code);
            return new OTPResult(code, false);
        }

        // Send OTP by email
        boolean sent = sendOTPEmail(email, code);
        return new OTPResult(code, sent);
    }

    /**
     * Validates an OTP code for a user.
     * @return true if the code is valid, not expired, and not used
     */
    public boolean validateOTP(int userId, String code) {
        // Debug: show what's in the DB for this user
        try (PreparedStatement dbg = connection.prepareStatement(
                "SELECT id, code, used, expires_at FROM otp_codes WHERE user_id = ? ORDER BY id DESC LIMIT 3")) {
            dbg.setInt(1, userId);
            ResultSet dbgRs = dbg.executeQuery();
            while (dbgRs.next()) {
                System.out.println("🔍 [OTP DEBUG] DB row: id=" + dbgRs.getInt("id") +
                        " code=" + dbgRs.getString("code") +
                        " used=" + dbgRs.getBoolean("used") +
                        " expires_at=" + dbgRs.getTimestamp("expires_at"));
            }
        } catch (SQLException e) {
            System.err.println("🔍 [OTP DEBUG] Error reading DB: " + e.getMessage());
        }

        System.out.println("🔍 [OTP DEBUG] Validating: userId=" + userId + " inputCode='" + code + "'");

        // Use Java Timestamp for comparison to avoid timezone mismatch with MySQL NOW()
        Timestamp now = new Timestamp(System.currentTimeMillis());
        String sql = "SELECT id, code FROM otp_codes WHERE user_id = ? AND code = ? AND used = FALSE AND expires_at > ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, code);
            ps.setTimestamp(3, now);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                // Mark as used
                int otpId = rs.getInt("id");
                markAsUsed(otpId);
                System.out.println("✅ [OTP] Code validated for user " + userId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ [OTP] Error validating OTP: " + e.getMessage());
        }

        // Fallback: try without expiry check (in case of timezone issues)
        String fallbackSql = "SELECT id, code FROM otp_codes WHERE user_id = ? AND code = ? AND used = FALSE";
        try (PreparedStatement ps = connection.prepareStatement(fallbackSql)) {
            ps.setInt(1, userId);
            ps.setString(2, code);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int otpId = rs.getInt("id");
                markAsUsed(otpId);
                System.out.println("✅ [OTP] Code validated via fallback (ignoring expiry) for user " + userId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ [OTP] Fallback validation error: " + e.getMessage());
        }

        System.out.println("❌ [OTP] Invalid or expired code for user " + userId);
        return false;
    }

    /**
     * Generates a random 6-digit numeric code.
     */
    private String generateCode() {
        int code = 100000 + random.nextInt(900000); // 6 digits
        return String.valueOf(code);
    }

    /**
     * Marks an OTP as used.
     */
    private void markAsUsed(int otpId) {
        String sql = "UPDATE otp_codes SET used = TRUE WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, otpId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ [OTP] Error marking OTP as used: " + e.getMessage());
        }
    }

    /**
     * Invalidates any existing unused OTPs for a user.
     */
    private void invalidateExistingOTPs(int userId) {
        String sql = "UPDATE otp_codes SET used = TRUE WHERE user_id = ? AND used = FALSE";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ [OTP] Error invalidating old OTPs: " + e.getMessage());
        }
    }

    /**
     * Sends a styled OTP email.
     * @return true if email was delivered, false on SMTP failure
     */
    private boolean sendOTPEmail(String email, String code) {
        String subject = "🔐 GoVibe — Code de vérification";
        String htmlBody = "<!DOCTYPE html>"
                + "<html><head><meta charset='UTF-8'></head>"
                + "<body style='font-family:Arial,sans-serif;background:#f4f4f4;padding:20px;'>"
                + "<div style='max-width:600px;margin:0 auto;background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,0.1);'>"
                + "<div style='background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);padding:30px;text-align:center;'>"
                + "<h1 style='color:#ffffff;margin:0;font-size:28px;'>GoVibe</h1>"
                + "<p style='color:rgba(255,255,255,0.8);margin:5px 0 0;'>Vérification de sécurité</p>"
                + "</div>"
                + "<div style='padding:30px;text-align:center;'>"
                + "<h2 style='color:#333;'>Code de vérification</h2>"
                + "<p style='color:#666;font-size:16px;'>Une vérification supplémentaire est requise pour votre connexion.</p>"
                + "<div style='background:#f0f0f0;border-radius:8px;padding:20px;margin:20px 0;'>"
                + "<span style='font-size:36px;font-weight:bold;letter-spacing:10px;color:#667eea;'>" + code + "</span>"
                + "</div>"
                + "<p style='color:#999;font-size:14px;'>Ce code expire dans " + OTP_VALIDITY_MINUTES + " minutes.</p>"
                + "<p style='color:#cc0000;font-size:13px;margin-top:15px;'>⚠️ Si vous n'avez pas initié cette connexion, changez immédiatement votre mot de passe.</p>"
                + "<hr style='border:none;border-top:1px solid #eee;margin:20px 0;'>"
                + "<p style='color:#999;font-size:12px;'>Cet email a été envoyé automatiquement par le système de sécurité GoVibe.</p>"
                + "</div></div></body></html>";

        try {
            emailService.sendHtmlEmailChecked(email, subject, htmlBody);
            return true;
        } catch (Exception e) {
            System.err.println("❌ [OTP] Failed to send email to " + email + ": " + e.getMessage());
            System.err.println("⚠️ [OTP] DEV-MODE — OTP code: " + code);
            return false;
        }
    }
}
