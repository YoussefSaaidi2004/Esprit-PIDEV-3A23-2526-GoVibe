package tn.esprit.services;

import tn.esprit.entities.Personne;
import tn.esprit.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ServicePersonne {

    private Connection cnx = MyDataBase.getInstance().getConnection();

    public Personne getOneById(int id) {
        Personne personne = null;
        String req = "SELECT * FROM personne WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                personne = new Personne();
                personne.setId(rs.getInt("id"));
                personne.setNom(rs.getString("nom"));
                personne.setPrenom(rs.getString("prenom"));
                personne.setEmail(rs.getString("email"));
                personne.setPassword(rs.getString("password"));
                personne.setRole(rs.getString("role"));
                personne.setCreated_at(rs.getTimestamp("created_at"));
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération de la personne : " + e.getMessage());
        }
        return personne;
    }

    public void ajouter(Personne p) throws SQLException {
        String req = "INSERT INTO personne (nom, prenom, email, password, role) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, p.getNom());
        ps.setString(2, p.getPrenom());
        ps.setString(3, p.getEmail());
        ps.setString(4, p.getPassword());
        ps.setString(5, p.getRole());
        ps.executeUpdate();
    }

    public Personne login(String email, String password) {
        Personne personne = null;
        String req = "SELECT * FROM personne WHERE email = ? AND password = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setString(1, email);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                personne = new Personne();
                personne.setId(rs.getInt("id"));
                personne.setNom(rs.getString("nom"));
                personne.setPrenom(rs.getString("prenom"));
                personne.setEmail(rs.getString("email"));
                personne.setPassword(rs.getString("password"));
                personne.setRole(rs.getString("role"));
                personne.setCreated_at(rs.getTimestamp("created_at"));
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
                personne = new Personne();
                personne.setId(rs.getInt("id"));
                personne.setNom(rs.getString("nom"));
                personne.setPrenom(rs.getString("prenom"));
                personne.setEmail(rs.getString("email"));
                personne.setPassword(rs.getString("password"));
                personne.setRole(rs.getString("role"));
                personne.setCreated_at(rs.getTimestamp("created_at"));
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération par email : " + e.getMessage());
        }
        return personne;
    }
}
