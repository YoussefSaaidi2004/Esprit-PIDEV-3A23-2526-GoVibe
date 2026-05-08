package org.example.services;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.*;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OTPService, MFAService and SessionManager.
 *
 * Uses the package-private OTPService(Connection, EmailService) constructor
 * so no real MySQL connection or SMTP server is needed.
 *
 * Coverage:
 *  - OTP code generation format & randomness
 *  - generateAndSendOTP: DB inserts, old-code invalidation
 *  - generateAndSendOTP: emailSent flag when SMTP configured / unconfigured
 *  - generateAndSendOTP: emailSent flag on SMTP exception
 *  - validateOTP: valid code, wrong code, code marked as used
 *  - MFAService auth-level decisions (LOW / HIGH / VERY_HIGH + admin override)
 *  - SessionManager pending-MFA state machine
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OTPServiceTest {

    // â”€â”€ Fixture helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Builds an OTPService with fully-mocked Connection + EmailService. */
    private OTPService svc(Connection conn, EmailService email) {
        return new OTPService(conn, email);   // package-private test constructor
    }

    /** PreparedStatement whose executeQuery() returns an empty ResultSet. */
    private PreparedStatement emptyPs() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(false);
        when(ps.executeQuery()).thenReturn(rs);
        return ps;
    }

    /** PreparedStatement whose executeQuery() returns a single row with the given id. */
    private PreparedStatement oneRowPs(int id) throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true, false);
        when(rs.getInt("id")).thenReturn(id);
        when(rs.getString("code")).thenReturn("123456");
        when(rs.getBoolean("used")).thenReturn(false);
        when(rs.getTimestamp("expires_at")).thenReturn(
                Timestamp.valueOf(java.time.LocalDateTime.now().plusMinutes(5)));
        when(ps.executeQuery()).thenReturn(rs);
        return ps;
    }

    /** A Connection that returns the given ps for every prepareStatement call. */
    private Connection connAlways(PreparedStatement ps) throws SQLException {
        Connection c = mock(Connection.class);
        when(c.prepareStatement(anyString())).thenReturn(ps);
        return c;
    }

    // â”€â”€ 1. Code generation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test @Order(1)
    @DisplayName("OTP is exactly 6 decimal digits in [100000, 999999]")
    void otpCodeFormatAndRange() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);
        EmailService email = mock(EmailService.class);
        when(email.isConfigured()).thenReturn(false);

        OTPService service = svc(connAlways(ps), email);

        for (int i = 0; i < 100; i++) {
            String code = service.generateAndSendOTP(i + 1, "a@b.com").code;
            assertNotNull(code);
            assertEquals(6, code.length(), "Expected 6 chars: " + code);
            assertTrue(code.matches("\\d{6}"), "Expected all digits: " + code);
            int n = Integer.parseInt(code);
            assertTrue(n >= 100_000 && n <= 999_999, "Out of range: " + n);
        }
    }

    @Test @Order(2)
    @DisplayName("Successive calls produce different codes (randomness check)")
    void otpCodesAreRandom() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);
        EmailService email = mock(EmailService.class);
        when(email.isConfigured()).thenReturn(false);

        OTPService service = svc(connAlways(ps), email);

        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < 30; i++) {
            seen.add(service.generateAndSendOTP(i + 1, "u@test.com").code);
        }
        assertTrue(seen.size() > 1, "Expected random codes, got constant output");
    }

    // â”€â”€ 2. generateAndSendOTP â€” DB interactions â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test @Order(3)
    @DisplayName("generateAndSendOTP: invalidates old OTPs then inserts new row")
    void generatesAndStoresOtp() throws SQLException {
        PreparedStatement invalidatePs = mock(PreparedStatement.class);
        PreparedStatement insertPs     = mock(PreparedStatement.class);

        Connection conn = mock(Connection.class);
        when(conn.prepareStatement(contains("UPDATE otp_codes"))).thenReturn(invalidatePs);
        when(conn.prepareStatement(contains("INSERT INTO otp_codes"))).thenReturn(insertPs);

        EmailService email = mock(EmailService.class);
        when(email.isConfigured()).thenReturn(false);

        OTPService.OTPResult result = svc(conn, email).generateAndSendOTP(42, "t@test.com");

        assertNotNull(result.code);

        verify(invalidatePs).setInt(1, 42);
        verify(invalidatePs).executeUpdate();
        verify(insertPs).setInt(1, 42);
        verify(insertPs).setString(2, result.code);
        verify(insertPs).executeUpdate();
    }

    // â”€â”€ 3. generateAndSendOTP â€” email flags â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test @Order(4)
    @DisplayName("emailSent=false when SMTP not configured (mail.password blank)")
    void emailSentFalseWhenUnconfigured() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        EmailService email = mock(EmailService.class);
        when(email.isConfigured()).thenReturn(false);

        OTPService.OTPResult result = svc(connAlways(ps), email).generateAndSendOTP(5, "u@u.com");

        assertFalse(result.emailSent);
        verify(email, never()).sendHtmlEmailChecked(anyString(), anyString(), anyString());
    }

    @Test @Order(5)
    @DisplayName("emailSent=true when SMTP configured and send succeeds")
    void emailSentTrueOnSuccess() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        EmailService email = mock(EmailService.class);
        when(email.isConfigured()).thenReturn(true);
        // sendHtmlEmailChecked does nothing â†’ success

        OTPService.OTPResult result = svc(connAlways(ps), email).generateAndSendOTP(6, "r@g.com");

        assertTrue(result.emailSent);
    }

    @Test @Order(6)
    @DisplayName("emailSent=false when SMTP configured but throws MessagingException")
    void emailSentFalseOnSmtpError() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        EmailService email = mock(EmailService.class);
        when(email.isConfigured()).thenReturn(true);
        doThrow(new javax.mail.MessagingException("535 Bad auth"))
                .when(email).sendHtmlEmailChecked(anyString(), anyString(), anyString());

        OTPService.OTPResult result = svc(connAlways(ps), email).generateAndSendOTP(7, "u@u.com");

        assertFalse(result.emailSent);
    }

    // â”€â”€ 4. validateOTP â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test @Order(7)
    @DisplayName("validateOTP: true for valid, unexpired, unused code")
    void validateOtpValid() throws SQLException {
        PreparedStatement debugPs    = emptyPs();
        PreparedStatement validatePs = oneRowPs(99);
        PreparedStatement markPs     = mock(PreparedStatement.class);

        Connection conn = mock(Connection.class);
        when(conn.prepareStatement(contains("LIMIT 3"))).thenReturn(debugPs);
        when(conn.prepareStatement(contains("expires_at >"))).thenReturn(validatePs);
        when(conn.prepareStatement(contains("SET used = TRUE WHERE id"))).thenReturn(markPs);

        assertTrue(svc(conn, mock(EmailService.class)).validateOTP(10, "123456"));

        verify(markPs).setInt(1, 99);
        verify(markPs).executeUpdate();
    }

    @Test @Order(8)
    @DisplayName("validateOTP: false for wrong code (primary + fallback miss)")
    void validateOtpWrongCode() throws SQLException {
        PreparedStatement debugPs    = emptyPs();
        PreparedStatement validatePs = emptyPs();
        PreparedStatement fallbackPs = emptyPs();

        Connection conn = mock(Connection.class);
        when(conn.prepareStatement(contains("LIMIT 3"))).thenReturn(debugPs);
        when(conn.prepareStatement(contains("expires_at >"))).thenReturn(validatePs);
        when(conn.prepareStatement(argThat(s ->
                s != null && s.contains("AND used = FALSE") && !s.contains("expires_at"))))
                .thenReturn(fallbackPs);

        assertFalse(svc(conn, mock(EmailService.class)).validateOTP(10, "000000"));
    }

    @Test @Order(9)
    @DisplayName("validateOTP: code marked used â†’ second call with same code returns false")
    void validateOtpCodeMarkedUsed() throws SQLException {
        PreparedStatement debugPs     = emptyPs();
        PreparedStatement validatePs1 = oneRowPs(55);
        PreparedStatement validatePs2 = emptyPs();
        PreparedStatement fallbackPs2 = emptyPs();
        PreparedStatement markPs      = mock(PreparedStatement.class);

        Connection conn = mock(Connection.class);
        when(conn.prepareStatement(contains("LIMIT 3"))).thenReturn(debugPs);
        when(conn.prepareStatement(contains("expires_at >")))
                .thenReturn(validatePs1).thenReturn(validatePs2);
        when(conn.prepareStatement(contains("SET used = TRUE WHERE id"))).thenReturn(markPs);
        when(conn.prepareStatement(argThat(s ->
                s != null && s.contains("AND used = FALSE") && !s.contains("expires_at"))))
                .thenReturn(fallbackPs2);

        OTPService svc = svc(conn, mock(EmailService.class));
        assertTrue(svc.validateOTP(20, "123456"),  "First call must succeed");
        assertFalse(svc.validateOTP(20, "123456"), "Second call (already used) must fail");
    }

    // â”€â”€ 5. OTPResult â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test @Order(10)
    @DisplayName("OTPResult stores code and emailSent correctly")
    void otpResultFields() {
        OTPService.OTPResult r1 = new OTPService.OTPResult("567890", true);
        assertEquals("567890", r1.code);
        assertTrue(r1.emailSent);

        OTPService.OTPResult r2 = new OTPService.OTPResult("112233", false);
        assertEquals("112233", r2.code);
        assertFalse(r2.emailSent);
    }

    // â”€â”€ 6. MFAService auth-level decisions â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Nested
    @DisplayName("MFAService â€” auth-level decisions")
    class MFAServiceTests {

        private final MFAService mfa = new MFAService();

        private org.example.entities.personne user(String role) {
            org.example.entities.personne p = new org.example.entities.personne();
            p.setRole(role);
            return p;
        }

        @Test @DisplayName("risk < 0.30 + regular user â†’ LOW")
        void lowRiskRegular() {
            assertEquals(MFAService.AuthLevel.LOW, mfa.determineAuthLevel(user("user"), 0.00));
            assertEquals(MFAService.AuthLevel.LOW, mfa.determineAuthLevel(user("user"), 0.29));
        }

        @Test @DisplayName("0.30 â‰¤ risk < 0.70 â†’ HIGH for any role")
        void midRiskHigh() {
            assertEquals(MFAService.AuthLevel.HIGH, mfa.determineAuthLevel(user("user"),  0.30));
            assertEquals(MFAService.AuthLevel.HIGH, mfa.determineAuthLevel(user("user"),  0.55));
            assertEquals(MFAService.AuthLevel.HIGH, mfa.determineAuthLevel(user("admin"), 0.50));
        }

        @Test @DisplayName("risk â‰¥ 0.70 â†’ VERY_HIGH")
        void highRiskVeryHigh() {
            assertEquals(MFAService.AuthLevel.VERY_HIGH, mfa.determineAuthLevel(user("user"),  0.70));
            assertEquals(MFAService.AuthLevel.VERY_HIGH, mfa.determineAuthLevel(user("admin"), 1.00));
        }

        @Test @DisplayName("Admin with low risk overridden to HIGH (never LOW)")
        void adminAlwaysAtLeastHigh() {
            assertEquals(MFAService.AuthLevel.HIGH, mfa.determineAuthLevel(user("admin"), 0.05));
            assertEquals(MFAService.AuthLevel.HIGH, mfa.determineAuthLevel(user("admin"), 0.29));
        }

        @ParameterizedTest
        @ValueSource(doubles = {0.0, 0.05, 0.15, 0.29})
        @DisplayName("All low-risk values â†’ LOW for regular user")
        void allLowRiskValues(double risk) {
            assertEquals(MFAService.AuthLevel.LOW, mfa.determineAuthLevel(user("user"), risk));
        }

        @ParameterizedTest
        @ValueSource(doubles = {0.70, 0.80, 0.95, 1.0})
        @DisplayName("All very-high risk values â†’ VERY_HIGH")
        void allVeryHighValues(double risk) {
            assertEquals(MFAService.AuthLevel.VERY_HIGH, mfa.determineAuthLevel(user("user"), risk));
        }
    }

    // â”€â”€ 7. SessionManager pending-MFA state machine â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Nested
    @DisplayName("SessionManager â€” pending MFA state machine")
    class SessionManagerTests {

        @BeforeEach void reset() { org.example.utils.SessionManager.clear(); }

        @Test @DisplayName("clear() wipes all state including pending MFA")
        void clearRemovesAll() {
            org.example.entities.personne u = new org.example.entities.personne();
            org.example.utils.SessionManager.setPendingMfaUser(u);
            org.example.utils.SessionManager.setPendingRiskScore(0.88);
            org.example.utils.SessionManager.setPendingAuthLevel("VERY_HIGH");
            org.example.utils.SessionManager.clear();

            assertNull(org.example.utils.SessionManager.getPendingMfaUser());
            assertEquals(0.0, org.example.utils.SessionManager.getPendingRiskScore(), 1e-9);
            assertNull(org.example.utils.SessionManager.getPendingAuthLevel());
        }

        @Test @DisplayName("clearPendingMfa() keeps currentUser intact")
        void clearPendingMfaPreservesCurrentUser() {
            org.example.entities.personne live = new org.example.entities.personne();
            live.setEmail("live@s.com");
            org.example.utils.SessionManager.setCurrentUser(live);
            org.example.utils.SessionManager.setPendingMfaUser(new org.example.entities.personne());
            org.example.utils.SessionManager.clearPendingMfa();

            assertNull(org.example.utils.SessionManager.getPendingMfaUser());
            assertNotNull(org.example.utils.SessionManager.getCurrentUser());
        }

        @Test @DisplayName("pending user identity preserved across set/get")
        void pendingUserRoundTrip() {
            org.example.entities.personne u = new org.example.entities.personne();
            u.setEmail("p@mfa.com");
            org.example.utils.SessionManager.setPendingMfaUser(u);
            assertSame(u, org.example.utils.SessionManager.getPendingMfaUser());
        }

        @Test @DisplayName("risk score and auth level round-trip")
        void riskAndLevelRoundTrip() {
            org.example.utils.SessionManager.setPendingRiskScore(0.73);
            org.example.utils.SessionManager.setPendingAuthLevel("VERY_HIGH");
            assertEquals(0.73, org.example.utils.SessionManager.getPendingRiskScore(), 1e-9);
            assertEquals("VERY_HIGH", org.example.utils.SessionManager.getPendingAuthLevel());
        }

        @Test @DisplayName("isAuthenticated() false by default")
        void notAuthByDefault() {
            assertFalse(org.example.utils.SessionManager.isAuthenticated());
        }

        @Test @DisplayName("isAdmin() false for role='user'")
        void notAdminForUser() {
            org.example.entities.personne u = new org.example.entities.personne();
            u.setRole("user");
            org.example.utils.SessionManager.setCurrentUser(u);
            assertFalse(org.example.utils.SessionManager.isAdmin());
        }

        @Test @DisplayName("isAdmin() true for role='admin'")
        void isAdminForAdmin() {
            org.example.entities.personne u = new org.example.entities.personne();
            u.setRole("admin");
            org.example.utils.SessionManager.setCurrentUser(u);
            assertTrue(org.example.utils.SessionManager.isAdmin());
        }

        @Test @DisplayName("isAdmin() case-insensitive (role='Admin')")
        void isAdminCaseInsensitive() {
            org.example.entities.personne u = new org.example.entities.personne();
            u.setRole("Admin");
            org.example.utils.SessionManager.setCurrentUser(u);
            assertTrue(org.example.utils.SessionManager.isAdmin());
        }
    }
}

