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
        // Validation basique du status
        if (a.getStatus() == null || a.getStatus().isEmpty()) {
            a.setStatus(Activite.STATUS_CONFIRMED); // Par défaut Confirmed si pas précisé (legacy)
        }

        String sql = "INSERT INTO activite (name, description, type, localisation, prix, status) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, a.getName());
            ps.setString(2, a.getDescription());
            ps.setString(3, a.getType());
            ps.setString(4, a.getLocalisation());
            ps.setBigDecimal(5, a.getPrix());
            ps.setString(6, a.getStatus());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    a.setId(rs.getInt(1));
            }
        }
    }

    // ✅ AFFICHER CONSOLE
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
                                rs.getBigDecimal("prix") + " | " +
                                rs.getString("status"));
            }
        }
    }

    // ✅ LISTE POUR CLIENTS (CONFIRMED UNIQUEMENT)
    public List<Activite> getAll() throws SQLException {
        List<Activite> list = new ArrayList<>();
        // ⚠️ Filtre sur le status
        String sql = "SELECT * FROM activite WHERE status = 'Confirmed' ORDER BY id";

        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ✅ LISTE VALIDATION (PENDING UNIQUEMENT)
    public List<Activite> getAllPending() throws SQLException {
        List<Activite> list = new ArrayList<>();
        String sql = "SELECT * FROM activite WHERE status = 'Pending' ORDER BY id";

        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ✅ LISTE TOUT (POUR ADMIN DASHBOARD GLOBAL)
    public List<Activite> getAllAll() throws SQLException {
        List<Activite> list = new ArrayList<>();
        String sql = "SELECT * FROM activite ORDER BY id";

        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ✅ VALIDER UNE ACTIVITÉ
    public void valider(int id) throws SQLException {
        String sql = "UPDATE activite SET status = 'Confirmed' WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ✅ GET BY ID
    public Activite getById(int id) throws SQLException {
        String sql = "SELECT * FROM activite WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
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

    // Helper pour mapper le ResultSet
    private Activite mapRow(ResultSet rs) throws SQLException {
        return new Activite(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("type"),
                rs.getString("localisation"),
                rs.getBigDecimal("prix"),
                rs.getString("status") // Si status n'existe pas en DB, ça plantera (SQL column not found)
        );
    }
}