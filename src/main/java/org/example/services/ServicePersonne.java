package org.example.services;

import org.example.entities.personne;
import org.example.utils.MyDataBase;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicePersonne implements IService<personne> {
    private Connection connection;

    public ServicePersonne() {
        connection = MyDataBase.getInstance().getConnection();
    }

    public void ajouter(personne personne) {
        String req = "INSERT INTO `personne`(`nom`, `prenom`, `email`, `password`, `role`, `face_encoding`) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setString(1, personne.getNom());
            ps.setString(2, personne.getPrenom());
            ps.setString(3, personne.getEmail());

            String hashedPassword = BCrypt.hashpw(personne.getPassword(), BCrypt.gensalt());
            ps.setString(4, hashedPassword);

            ps.setString(5, personne.getRole());
            ps.setString(6, personne.getFaceEncoding());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout de la personne", e);
        }
    }

    public void modifier(   personne personne) {
        // Here we check if password needs rehashing. Ideally we shouldn't rehash if
        // it's already hashed,
        // but since we can't easily tell, we assume the controller sends a plaintext
        // password if edited,
        // or we handle logic in controller.
        // For now, consistent with previous implementation: ALWAYS hash what is sent.

        String req = "UPDATE `personne` SET `nom`=?,`prenom`=?,`email`=?,`password`=?,`role`=? WHERE `id`=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setString(1, personne.getNom());
            ps.setString(2, personne.getPrenom());
            ps.setString(3, personne.getEmail());

            String hashedPassword = BCrypt.hashpw(personne.getPassword(), BCrypt.gensalt());
            ps.setString(4, hashedPassword);

            ps.setString(5, personne.getRole());
            ps.setInt(6, personne.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la modification de la personne", e);
        }
    }

    public void supprimer(int id) {
        String req = "DELETE FROM `personne` WHERE `id`=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression de la personne", e);
        }
    }

    public List<personne> afficher() {
        List<personne> personnes = new ArrayList<>();
        String req = "SELECT * FROM `personne`";
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(req)) {
            while (rs.next()) {
                personne p = mapResultSetToPersonne(rs);
                personnes.add(p);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation des personnes", e);
        }
        return personnes;
    }

    @Override
    public void add(personne personne) {
        ajouter(personne);
    }

    @Override
    public void update(personne personne) {
        modifier(personne);
    }

    @Override
    public void delete(int id) {
        supprimer(id);
    }

    @Override
    public List<personne> getAll() {
        return afficher();
    }

    public personne login(String email, String password) throws SQLException {
        String req = "SELECT * FROM `personne` WHERE `email`=?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            String storedPassword = rs.getString("password");
            boolean passwordMatch = false;

            // Check if stored password is a BCrypt hash (supports $2a/$2b/$2y)
            if (storedPassword != null && storedPassword.startsWith("$2")) {
                try {
                    passwordMatch = BCrypt.checkpw(password, storedPassword);
                } catch (IllegalArgumentException e) {
                    passwordMatch = false;
                }
            } else {
                // Plain text password (legacy) - compare directly
                passwordMatch = password.equals(storedPassword);
                // Auto-upgrade to BCrypt hash
                if (passwordMatch) {
                    String newHash = BCrypt.hashpw(password, BCrypt.gensalt());
                    PreparedStatement updatePs = connection
                            .prepareStatement("UPDATE `personne` SET `password`=? WHERE `email`=?");
                    updatePs.setString(1, newHash);
                    updatePs.setString(2, email);
                    updatePs.executeUpdate();
                    storedPassword = newHash;
                }
            }

            if (passwordMatch) {
                personne p = mapResultSetToPersonne(rs);
                return p;
            }
        }
        return null;
    }

    public boolean emailExists(String email) throws SQLException {
        String req = "SELECT COUNT(*) FROM `personne` WHERE `email`=?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
        return false;
    }

    public void updatePassword(String email, String newPassword) throws SQLException {
        String req = "UPDATE `personne` SET `password`=? WHERE `email`=?";
        PreparedStatement ps = connection.prepareStatement(req);
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        ps.setString(1, hashedPassword);
        ps.setString(2, email);
        ps.executeUpdate();
    }

    public personne getOneById(int id) {
        personne p = null;
        String req = "SELECT * FROM personne WHERE id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                p = mapResultSetToPersonne(rs);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération de la personne : " + e.getMessage());
        }
        return p;
    }

    public personne getOneByEmail(String email) {
        personne p = null;
        String req = "SELECT * FROM personne WHERE email = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                p = mapResultSetToPersonne(rs);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération par email : " + e.getMessage());
        }
        return p;
    }

    /**
     * Ajoute un utilisateur OAuth2 (sans mot de passe).
     */
    public void ajouterOAuth2(personne p) {
        String req = "INSERT INTO `personne`(`nom`, `prenom`, `email`, `password`, `role`, `provider`, `provider_id`, `photo_url`) VALUES (?,?,?,NULL,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setString(1, p.getNom());
            ps.setString(2, p.getPrenom());
            ps.setString(3, p.getEmail());
            ps.setString(4, p.getRole() != null ? p.getRole() : "USER");
            ps.setString(5, p.getProvider());
            ps.setString(6, p.getProviderId());
            ps.setString(7, p.getPhotoUrl());
            ps.executeUpdate();
            System.out.println("✅ [ServicePersonne] Utilisateur OAuth2 ajouté : " + p.getEmail());
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout de l'utilisateur OAuth2", e);
        }
    }

    /**
     * Mappe un ResultSet vers un objet personne (avec champs OAuth2 si présents).
     */
    private personne mapResultSetToPersonne(ResultSet rs) throws SQLException {
        personne p = new personne();
        p.setId(rs.getInt("id"));
        p.setNom(rs.getString("nom"));
        p.setPrenom(rs.getString("prenom"));
        p.setEmail(rs.getString("email"));
        p.setPassword(rs.getString("password"));
        p.setRole(rs.getString("role"));

        // Champs OAuth2 (avec gestion d'absence pour compatibilité)
        try {
            p.setProvider(rs.getString("provider"));
            p.setProviderId(rs.getString("provider_id"));
            p.setPhotoUrl(rs.getString("photo_url"));
        } catch (SQLException ignored) {
            // Les colonnes OAuth2 n'existent peut-être pas encore (avant migration)
            p.setProvider("local");
        }

        // Champs MFA (avec gestion d'absence pour compatibilité)
        try {
            p.setAccountLocked(rs.getBoolean("is_account_locked"));
            p.setPreferredMfa(rs.getString("preferred_mfa"));
            p.setLockoutUntil(rs.getTimestamp("lockout_until"));
        } catch (SQLException ignored) {
            p.setAccountLocked(false);
            p.setPreferredMfa("NONE");
            p.setLockoutUntil(null);
        }

        try {
            p.setFaceEncoding(rs.getString("face_encoding"));
        } catch (SQLException ignored) {
            p.setFaceEncoding(null);
        }

        return p;
    }

    /**
     * Verrouille le compte d'un utilisateur pour un certain nombre de minutes.
     */
    public void lockAccount(int userId, int minutes) {
        String sql = "UPDATE personne SET is_account_locked = TRUE, lockout_until = DATE_ADD(NOW(), INTERVAL ? MINUTE) WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, minutes);
            ps.setInt(2, userId);
            ps.executeUpdate();
            System.out.println("🔒 [ServicePersonne] Compte " + userId + " verrouillé pour " + minutes + " minutes.");
        } catch (SQLException e) {
            System.err.println("❌ [ServicePersonne] Erreur lors du verrouillage: " + e.getMessage());
        }
    }

    /**
     * Déverrouille le compte d'un utilisateur.
     */
    public void unlockAccount(int userId) {
        String sql = "UPDATE personne SET is_account_locked = FALSE, lockout_until = NULL WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            System.out.println("🔓 [ServicePersonne] Compte " + userId + " déverrouillé.");
        } catch (SQLException e) {
            System.err.println("❌ [ServicePersonne] Erreur lors du déverrouillage: " + e.getMessage());
        }
    }
}
