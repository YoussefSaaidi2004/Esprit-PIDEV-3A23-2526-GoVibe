package org.example.services;

import org.example.entities.CodePromo;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 💡 Service Code Promo — GoVibe Hotel Module
 * Manages promo codes (HOTEL10, SUMMER2026, etc.)
 */
public class ServiceCodePromo {

    private Connection connection;

    public ServiceCodePromo() {
        connection = MyDataBase.getInstance().getMyConnection();
    }

    // ---- CRUD ----

    public void insert(CodePromo cp) throws SQLException {
        String sql = "INSERT INTO code_promo (code, description, type, valeur, date_expiration, actif, utilisations_max) " +
                     "VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, cp.getCode());
            stmt.setString(2, cp.getDescription());
            stmt.setString(3, cp.getType().name());
            stmt.setDouble(4, cp.getValeur());
            stmt.setDate(5, cp.getDateExpiration() != null ? Date.valueOf(cp.getDateExpiration()) : null);
            stmt.setBoolean(6, cp.isActif());
            stmt.setInt(7, cp.getUtilisationsMax());
            stmt.executeUpdate();
        }
    }

    public void update(CodePromo cp) throws SQLException {
        String sql = "UPDATE code_promo SET code=?, description=?, type=?, valeur=?, " +
                     "date_expiration=?, actif=?, utilisations_max=? WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, cp.getCode());
            stmt.setString(2, cp.getDescription());
            stmt.setString(3, cp.getType().name());
            stmt.setDouble(4, cp.getValeur());
            stmt.setDate(5, cp.getDateExpiration() != null ? Date.valueOf(cp.getDateExpiration()) : null);
            stmt.setBoolean(6, cp.isActif());
            stmt.setInt(7, cp.getUtilisationsMax());
            stmt.setInt(8, cp.getId());
            stmt.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM code_promo WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public List<CodePromo> show() throws SQLException {
        List<CodePromo> list = new ArrayList<>();
        String sql = "SELECT * FROM code_promo ORDER BY code";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<CodePromo> getCodesActifs() throws SQLException {
        List<CodePromo> list = new ArrayList<>();
        String sql = "SELECT * FROM code_promo WHERE actif = TRUE " +
                     "AND (date_expiration IS NULL OR date_expiration >= CURDATE()) " +
                     "AND utilisations_actuelles < utilisations_max ORDER BY code";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /**
     * Validate and fetch a promo code by its string code.
     * Returns null if not found or invalid.
     */
    public CodePromo validerCode(String codeStr) throws SQLException {
        String sql = "SELECT * FROM code_promo WHERE code = ? AND actif = TRUE";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, codeStr.trim().toUpperCase());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                CodePromo cp = map(rs);
                return cp.isValide() ? cp : null;
            }
        }
        return null;
    }

    /**
     * Increment usage counter after a code is applied.
     */
    public void incrementerUtilisation(String codeStr) throws SQLException {
        String sql = "UPDATE code_promo SET utilisations_actuelles = utilisations_actuelles + 1 WHERE code = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, codeStr.trim().toUpperCase());
            stmt.executeUpdate();
        }
    }

    private CodePromo map(ResultSet rs) throws SQLException {
        Date expDate = rs.getDate("date_expiration");
        return new CodePromo(
            rs.getInt("id"),
            rs.getString("code"),
            rs.getString("description"),
            CodePromo.TypeRemise.valueOf(rs.getString("type")),
            rs.getDouble("valeur"),
            expDate != null ? expDate.toLocalDate() : null,
            rs.getBoolean("actif"),
            rs.getInt("utilisations_max"),
            rs.getInt("utilisations_actuelles")
        );
    }
}
