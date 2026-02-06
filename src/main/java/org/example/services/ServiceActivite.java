package org.example.services;

import org.example.entites.Activite;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.math.BigDecimal;

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

    // ✅ AFFICHER
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

    // ✅ MODIFIER PAR ID (tu choisis l'id)
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

            int rows = ps.executeUpdate();
            if (rows > 0) System.out.println("✅ Modification OK (id = " + id + ")");
            else System.out.println("⚠️ ID introuvable : " + id);
        }
    }

    // 🗑️ SUPPRIMER PAR ID (tu choisis l'id)
    public void supprimerParId(int id) throws SQLException {
        String sql = "DELETE FROM activite WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);

            int rows = ps.executeUpdate();
            if (rows > 0) System.out.println("✅ Suppression OK (id = " + id + ")");
            else System.out.println("⚠️ ID introuvable : " + id);
        }
    }

    // ⭐ Récupérer l'ID du dernier enregistrement (optionnel)
    public Integer getDernierId() throws SQLException {
        String sql = "SELECT id FROM activite ORDER BY id DESC LIMIT 1";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt("id");
        }
        return null;
    }
}