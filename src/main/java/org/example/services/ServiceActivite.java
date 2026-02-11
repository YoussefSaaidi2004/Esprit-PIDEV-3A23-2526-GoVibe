package org.example.services;

import org.example.entites.Activite;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ServiceActivite {

    private final Connection connection;

    public ServiceActivite() {
        connection = MyDataBase.getInstance().getConnection();
    }

    // ✅ AJOUT
    public void ajouter(Activite a) throws SQLException {
        String sql = "INSERT INTO activite (name, description, type, localisation, prix) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, a.getName());
            ps.setString(2, a.getDescription());
            ps.setString(3, a.getType());
            ps.setString(4, a.getLocalisation());
            ps.setBigDecimal(5, a.getPrix());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) a.setId(rs.getInt(1));
            }
        }
    }

    // ✅ AFFICHER CONSOLE (optionnel)
    public void afficher() throws SQLException {
        String sql = "SELECT * FROM activite ORDER BY id";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                System.out.println(
                        rs.getInt("id") + " | " +
                                rs.getString("name") + " | " +
                                rs.getString("description") + " | " +
                                rs.getString("type") + " | " +
                                rs.getString("localisation") + " | " +
                                rs.getBigDecimal("prix")
                );
            }
        }
    }

    // ✅ LISTE POUR JAVAFX
    public List<Activite> getAll() throws SQLException {
        List<Activite> list = new ArrayList<>();
        String sql = "SELECT * FROM activite ORDER BY id";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Activite a = new Activite(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("type"),
                        rs.getString("localisation"),
                        rs.getBigDecimal("prix")
                );
                list.add(a);
            }
        }
        return list;
    }

    // ✅ GET BY ID (NOUVEAU) — pour charger avant modification
    public Activite getById(int id) throws SQLException {
        String sql = "SELECT * FROM activite WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Activite(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getString("type"),
                            rs.getString("localisation"),
                            rs.getBigDecimal("prix")
                    );
                }
            }
        }
        return null;
    }

    // ✅ MODIFIER PAR ID
    public void modifierParId(int id,
                              String name,
                              String description,
                              String type,
                              String localisation,
                              BigDecimal prix) throws SQLException {

        String sql = "UPDATE activite SET name = ?, description = ?, type = ?, localisation = ?, prix = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setString(3, type);
            ps.setString(4, localisation);
            ps.setBigDecimal(5, prix);
            ps.setInt(6, id);

            ps.executeUpdate();
        }
    }

    // ✅ SUPPRIMER PAR ID
    public void supprimerParId(int id) throws SQLException {
        String sql = "DELETE FROM activite WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}