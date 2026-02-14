package org.example.services;

import org.example.entities.Chambre;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceChambre {

    private Connection connection;

    public ServiceChambre() {
        connection = MyDataBase.getInstance().getMyConnection();
    }

    // ================= INSERT =================
    public void insert(Chambre c) throws SQLException {

        String sql = "INSERT INTO chambre(type, capacite, equipements, hotel_id, prix_standard, prix_haute_saison, prix_basse_saison) VALUES (?,?,?,?,?,?,?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, c.getType());
            stmt.setInt(2, c.getCapacite());
            stmt.setString(3, c.getEquipements());
            stmt.setInt(4, c.getHotelId());
            stmt.setDouble(5, c.getPrixStandard());
            stmt.setDouble(6, c.getPrixHauteSaison());
            stmt.setDouble(7, c.getPrixBasseSaison());

            stmt.executeUpdate();
        }
    }


    // ================= UPDATE =================
    public void update(Chambre c) throws SQLException {

        String sql = "UPDATE chambre SET type=?, capacite=?, equipements=?, hotel_id=?, prix_standard=?, prix_haute_saison=?, prix_basse_saison=? WHERE id=?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, c.getType());
            stmt.setInt(2, c.getCapacite());
            stmt.setString(3, c.getEquipements());
            stmt.setInt(4, c.getHotelId());
            stmt.setDouble(5, c.getPrixStandard());
            stmt.setDouble(6, c.getPrixHauteSaison());
            stmt.setDouble(7, c.getPrixBasseSaison());
            stmt.setInt(8, c.getId());

            stmt.executeUpdate();
        }
    }

    // ================= DELETE =================
    public void delete(int id) throws SQLException {

        String sql = "DELETE FROM chambre WHERE id=?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // ================= FIND BY ID =================
    public Chambre findById(int id) throws SQLException {

        String sql = "SELECT * FROM chambre WHERE id=?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToChambre(rs);
            }
        }
        return null;
    }

    // ================= FIND ALL =================
    public List<Chambre> show() throws SQLException {

        List<Chambre> list = new ArrayList<>();
        String sql = "SELECT * FROM chambre";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSetToChambre(rs));
            }
        }

        return list;
    }


    // ================= MAPPING =================
    private Chambre mapResultSetToChambre(ResultSet rs) throws SQLException {

        return new Chambre(
                rs.getInt("id"),
                rs.getString("type"),
                rs.getInt("capacite"),
                rs.getString("equipements"),
                rs.getInt("hotel_id"),
                rs.getDouble("prix_standard"),
                rs.getDouble("prix_haute_saison"),
                rs.getDouble("prix_basse_saison")
        );
    }

    // ================= PRIX PAR SAISON =================
    public double getPrixParSaison(Chambre c, String saison) {

        switch (saison.toLowerCase()) {
            case "haute":
                return c.getPrixHauteSaison();
            case "basse":
                return c.getPrixBasseSaison();
            default:
                return c.getPrixStandard();
        }
    }
}

