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
        connection = MyDataBase.getInstance().getMyConnection();
    }

    @Override
    public void ajouter(personne personne) throws SQLException {
        String req = "INSERT INTO `personne`(`nom`, `prenom`, `email`, `password`, `role`) VALUES (?,?,?,?,?)";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, personne.getNom());
        ps.setString(2, personne.getPrenom());
        ps.setString(3, personne.getEmail());
        
        // Hacher le mot de passe avant de l'ajouter
        String hashedPassword = BCrypt.hashpw(personne.getPassword(), BCrypt.gensalt());
        ps.setString(4, hashedPassword);
        
        ps.setString(5, personne.getRole());
        ps.executeUpdate();
    }

    @Override
    public void modifier(personne personne) throws SQLException {
        // Here we check if password needs rehashing. Ideally we shouldn't rehash if it's already hashed,
        // but since we can't easily tell, we assume the controller sends a plaintext password if edited,
        // or we handle logic in controller. 
        // For now, consistent with previous implementation: ALWAYS hash what is sent.
        
        String req = "UPDATE `personne` SET `nom`=?,`prenom`=?,`email`=?,`password`=?,`role`=? WHERE `id`=?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, personne.getNom());
        ps.setString(2, personne.getPrenom());
        ps.setString(3, personne.getEmail());
        
        String hashedPassword = BCrypt.hashpw(personne.getPassword(), BCrypt.gensalt());
        ps.setString(4, hashedPassword);
        
        ps.setString(5, personne.getRole());
        ps.setInt(6, personne.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM `personne` WHERE `id`=?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<personne> afficher() throws SQLException {
        List<personne> personnes = new ArrayList<>();
        String req = "SELECT * FROM `personne`";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(req);
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
        return personnes;
    }
    
    public personne login(String email, String password) throws SQLException {
        String req = "SELECT * FROM `personne` WHERE `email`=?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            String storedPassword = rs.getString("password");
            boolean passwordMatch = false;

            // Check if stored password is a BCrypt hash (starts with $2a$)
            if (storedPassword != null && storedPassword.startsWith("$2a$")) {
                passwordMatch = BCrypt.checkpw(password, storedPassword);
            } else {
                // Plain text password (legacy) — compare directly
                passwordMatch = password.equals(storedPassword);
                // Auto-upgrade to BCrypt hash
                if (passwordMatch) {
                    String newHash = BCrypt.hashpw(password, BCrypt.gensalt());
                    PreparedStatement updatePs = connection.prepareStatement("UPDATE `personne` SET `password`=? WHERE `email`=?");
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
}
