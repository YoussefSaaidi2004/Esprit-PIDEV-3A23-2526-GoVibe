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
        String req = "INSERT INTO `personne`(`nom`, `prenom`, `email`, `password`, `role`) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setString(1, personne.getNom());
            ps.setString(2, personne.getPrenom());
            ps.setString(3, personne.getEmail());

            String hashedPassword = BCrypt.hashpw(personne.getPassword(), BCrypt.gensalt());
            ps.setString(4, hashedPassword);

            ps.setString(5, personne.getRole());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout de la personne", e);
        }
    }

    public void modifier(personne personne) {
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
                personne p = new personne();
                p.setId(rs.getInt("id"));
                p.setNom(rs.getString("nom"));
                p.setPrenom(rs.getString("prenom"));
                p.setEmail(rs.getString("email"));
                p.setPassword(rs.getString("password"));
                p.setRole(rs.getString("role"));
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
                personne p = new personne();
                p.setId(rs.getInt("id"));
                p.setNom(rs.getString("nom"));
                p.setPrenom(rs.getString("prenom"));
                p.setEmail(rs.getString("email"));
                p.setPassword(storedPassword);
                p.setRole(rs.getString("role"));
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
                p = new personne();
                p.setId(rs.getInt("id"));
                p.setNom(rs.getString("nom"));
                p.setPrenom(rs.getString("prenom"));
                p.setEmail(rs.getString("email"));
                p.setPassword(rs.getString("password"));
                p.setRole(rs.getString("role"));
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
                p = new personne();
                p.setId(rs.getInt("id"));
                p.setNom(rs.getString("nom"));
                p.setPrenom(rs.getString("prenom"));
                p.setEmail(rs.getString("email"));
                p.setPassword(rs.getString("password"));
                p.setRole(rs.getString("role"));
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération par email : " + e.getMessage());
        }
        return p;
    }
}
