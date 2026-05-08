package tn.esprit.services;

import tn.esprit.entities.Personne;
import tn.esprit.utils.MyDataBase;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ServicePersonne {

    private Connection cnx = MyDataBase.getInstance().getConnection();

    /**
     * Helper: populate all Personne fields from a ResultSet row.
     * Reads every column that Symfony's personne table has.
     */
    private Personne mapResultSet(ResultSet rs) throws SQLException {
        Personne p = new Personne();
        p.setId(rs.getInt("id"));
        p.setNom(rs.getString("nom"));
        p.setPrenom(rs.getString("prenom"));
        p.setEmail(rs.getString("email"));
        p.setPassword(rs.getString("password"));
        p.setRole(rs.getString("role"));
        p.setCreated_at(rs.getTimestamp("created_at"));
        p.setFaceEncoding(rs.getString("face_encoding"));
        // Symfony-compatible columns
        p.setProvider(rs.getString("provider"));
        p.setProviderId(rs.getString("provider_id"));
        p.setPhotoUrl(rs.getString("photo_url"));
        p.setAccountLocked(rs.getBoolean("is_account_locked"));
        p.setPreferredMfa(rs.getString("preferred_mfa"));
        p.setLockoutUntil(rs.getTimestamp("lockout_until"));
        p.setAbsenceCount(rs.getInt("absence_count"));
        p.setCustomerType(rs.getString("customer_type"));
        p.setSubscriptionExpiresAt(rs.getTimestamp("subscription_expires_at"));
        p.setSessionCredits(rs.getInt("session_credits"));
        p.setPreferredCategories(rs.getString("preferred_categories"));
        p.setResidenceCity(rs.getString("residence_city"));
        return p;
    }

    public Personne getOneById(int id) {
        Personne personne = null;
        String req = "SELECT * FROM personne WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                personne = mapResultSet(rs);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération de la personne : " + e.getMessage());
        }
        return personne;
    }

    public void ajouter(Personne p) throws SQLException {
        // Hash the password with BCrypt before storing (compatible with Symfony's password_verify)
        String hashedPassword = BCrypt.hashpw(p.getPassword(), BCrypt.gensalt());

        String req = "INSERT INTO personne (nom, prenom, email, password, role, face_encoding, " +
                "provider, provider_id, photo_url, is_account_locked, preferred_mfa, " +
                "absence_count, customer_type, session_credits, residence_city) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, p.getNom());
        ps.setString(2, p.getPrenom());
        ps.setString(3, p.getEmail());
        ps.setString(4, hashedPassword);
        ps.setString(5, p.getRole());
        ps.setString(6, p.getFaceEncoding());
        ps.setString(7, p.getProvider() != null ? p.getProvider() : "local");
        ps.setString(8, p.getProviderId());
        ps.setString(9, p.getPhotoUrl());
        ps.setBoolean(10, p.isAccountLocked());
        ps.setString(11, p.getPreferredMfa() != null ? p.getPreferredMfa() : "NONE");
        ps.setInt(12, p.getAbsenceCount());
        ps.setString(13, p.getCustomerType() != null ? p.getCustomerType() : "standard");
        ps.setInt(14, p.getSessionCredits());
        ps.setString(15, p.getResidenceCity());
        ps.executeUpdate();
    }

    public void modifier(Personne p) throws SQLException {
        String req = "UPDATE personne SET nom=?, prenom=?, email=?, role=?, face_encoding=?, " +
                "provider=?, provider_id=?, photo_url=?, is_account_locked=?, preferred_mfa=?, " +
                "absence_count=?, customer_type=?, session_credits=?, residence_city=? " +
                "WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, p.getNom());
        ps.setString(2, p.getPrenom());
        ps.setString(3, p.getEmail());
        ps.setString(4, p.getRole());
        ps.setString(5, p.getFaceEncoding());
        ps.setString(6, p.getProvider());
        ps.setString(7, p.getProviderId());
        ps.setString(8, p.getPhotoUrl());
        ps.setBoolean(9, p.isAccountLocked());
        ps.setString(10, p.getPreferredMfa());
        ps.setInt(11, p.getAbsenceCount());
        ps.setString(12, p.getCustomerType());
        ps.setInt(13, p.getSessionCredits());
        ps.setString(14, p.getResidenceCity());
        ps.setInt(15, p.getId());
        ps.executeUpdate();
    }

    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM personne WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public List<Personne> afficher() throws SQLException {
        List<Personne> personnes = new ArrayList<>();
        String req = "SELECT * FROM personne";
        PreparedStatement ps = cnx.prepareStatement(req);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            personnes.add(mapResultSet(rs));
        }
        return personnes;
    }

    public Personne login(String email, String password) {
        Personne personne = null;
        // Fetch user by email only - then verify password with BCrypt
        String req = "SELECT * FROM personne WHERE email = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                boolean passwordMatch = false;

                // Check if stored password is a BCrypt hash (supports $2a/$2b/$2y)
                if (storedPassword != null && storedPassword.startsWith("$2")) {
                    try {
                        // jBCrypt only supports $2a$ prefix, but PHP/Symfony uses $2y$ (or $2b$).
                        // They are algorithmically identical, so we convert the prefix for verification.
                        String hashForVerification = storedPassword;
                        if (storedPassword.startsWith("$2y$") || storedPassword.startsWith("$2b$")) {
                            hashForVerification = "$2a$" + storedPassword.substring(4);
                        }
                        passwordMatch = BCrypt.checkpw(password, hashForVerification);
                    } catch (IllegalArgumentException e) {
                        passwordMatch = false;
                    }
                } else {
                    // Plain text password (legacy) - compare directly
                    passwordMatch = password.equals(storedPassword);
                    // Auto-upgrade to BCrypt hash on successful login
                    if (passwordMatch) {
                        String newHash = BCrypt.hashpw(password, BCrypt.gensalt());
                        PreparedStatement updatePs = cnx.prepareStatement(
                                "UPDATE personne SET password = ? WHERE email = ?");
                        updatePs.setString(1, newHash);
                        updatePs.setString(2, email);
                        updatePs.executeUpdate();
                    }
                }

                if (passwordMatch) {
                    personne = mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors du login : " + e.getMessage());
        }
        return personne;
    }

    public boolean checkEmailExists(String email) {
        String req = "SELECT * FROM personne WHERE email = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.out.println("Erreur vérification email : " + e.getMessage());
        }
        return false;
    }

    public Personne getOneByEmail(String email) {
        Personne personne = null;
        String req = "SELECT * FROM personne WHERE email = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                personne = mapResultSet(rs);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération par email : " + e.getMessage());
        }
        return personne;
    }

    public void updateFaceEncoding(String email, String faceEncoding) throws SQLException {
        String req = "UPDATE personne SET face_encoding = ? WHERE email = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, faceEncoding);
        ps.setString(2, email);
        ps.executeUpdate();
    }

    /**
     * Update the user's password (re-hashes with BCrypt).
     * Compatible with Symfony's password_verify().
     */
    public void updatePassword(int userId, String newPlainPassword) throws SQLException {
        String hashedPassword = BCrypt.hashpw(newPlainPassword, BCrypt.gensalt());
        String req = "UPDATE personne SET password = ? WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, hashedPassword);
        ps.setInt(2, userId);
        ps.executeUpdate();
    }
}
