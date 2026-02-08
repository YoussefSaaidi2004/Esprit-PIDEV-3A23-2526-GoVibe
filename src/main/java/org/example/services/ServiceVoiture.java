package org.example.services;

import org.example.entities.Voiture;
import org.example.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ServiceVoiture implements IService<Voiture> {

    private final Connection connection;

    public ServiceVoiture() {
        this.connection = new MyDataBase().getConnection();
    }

    @Override
    public void add(Voiture voiture) {
        String sql = "INSERT INTO voiture (matricule, marque, modele, annee, type_carburant, prix_jour, statut, adresse_agence, latitude, longitude, description, image_url) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            String typeCarburant = normalizeTypeCarburant(voiture.getTypeCarburant());
            String statut = normalizeStatut(voiture.getStatut());
            ps.setString(1, voiture.getMatricule());
            ps.setString(2, voiture.getMarque());
            ps.setString(3, voiture.getModele());
            ps.setInt(4, voiture.getAnnee());
            ps.setString(5, typeCarburant);
            ps.setDouble(6, voiture.getPrixJour());
            ps.setString(7, statut);
            ps.setString(8, voiture.getAdresseAgence());
            ps.setDouble(9, voiture.getLatitude());
            ps.setDouble(10, voiture.getLongitude());
            ps.setString(11, voiture.getDescription());
            ps.setString(12, voiture.getImageUrl());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout de la voiture", e);
        }
    }

    @Override
    public void update(Voiture voiture) {
        String sql = "UPDATE voiture SET matricule=?, marque=?, modele=?, annee=?, type_carburant=?, prix_jour=?, statut=?, adresse_agence=?, latitude=?, longitude=?, description=?, image_url=? " +
                "WHERE id_voiture=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            String typeCarburant = normalizeTypeCarburant(voiture.getTypeCarburant());
            String statut = normalizeStatut(voiture.getStatut());
            ps.setString(1, voiture.getMatricule());
            ps.setString(2, voiture.getMarque());
            ps.setString(3, voiture.getModele());
            ps.setInt(4, voiture.getAnnee());
            ps.setString(5, typeCarburant);
            ps.setDouble(6, voiture.getPrixJour());
            ps.setString(7, statut);
            ps.setString(8, voiture.getAdresseAgence());
            ps.setDouble(9, voiture.getLatitude());
            ps.setDouble(10, voiture.getLongitude());
            ps.setString(11, voiture.getDescription());
            ps.setString(12, voiture.getImageUrl());
            ps.setInt(13, voiture.getIdVoiture());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la modification de la voiture", e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM voiture WHERE id_voiture=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression de la voiture", e);
        }
    }

    @Override
    public List<Voiture> getAll() {
        List<Voiture> voitures = new ArrayList<>();
        String sql = "SELECT id_voiture, matricule, marque, modele, annee, type_carburant, prix_jour, statut, adresse_agence, latitude, longitude, description, image_url, date_creation FROM voiture ORDER BY id_voiture DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id_voiture");
                String matricule = rs.getString("matricule");
                String marque = rs.getString("marque");
                String modele = rs.getString("modele");
                int annee = rs.getInt("annee");
                String typeCarburant = rs.getString("type_carburant");
                double prixJour = rs.getDouble("prix_jour");
                String statut = rs.getString("statut");
                String adresseAgence = rs.getString("adresse_agence");
                double latitude = rs.getDouble("latitude");
                double longitude = rs.getDouble("longitude");
                String description = rs.getString("description");
                String imageUrl = rs.getString("image_url");
                Timestamp ts = rs.getTimestamp("date_creation");
                LocalDateTime dateCreation = ts != null ? ts.toLocalDateTime() : null;

                voitures.add(new Voiture(
                        id, matricule, marque, modele, annee, typeCarburant, prixJour, statut,
                        adresseAgence, latitude, longitude, description, imageUrl, dateCreation
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des voitures", e);
        }
        return voitures;
    }

    private static String normalizeTypeCarburant(String input) {
        String normalized = normalizeKey(input);
        switch (normalized) {
            case "essence":
                return "Essence";
            case "diesel":
                return "Diesel";
            case "hybride":
                return "Hybride";
            case "electrique":
                return "Electrique";
            default:
                throw new RuntimeException("Type carburant invalide. Valeurs possibles: Essence, Diesel, Hybride, Electrique.");
        }
    }

    private static String normalizeStatut(String input) {
        String normalized = normalizeKey(input);
        switch (normalized) {
            case "disponible":
                return "DISPONIBLE";
            case "loue":
            case "louee":
                return "LOUEE";
            case "maintenance":
                return "MAINTENANCE";
            default:
                throw new RuntimeException("Statut invalide. Valeurs possibles: DISPONIBLE, LOUEE, MAINTENANCE.");
        }
    }

    private static String normalizeKey(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new RuntimeException("Champ requis manquant.");
        }
        String trimmed = input.trim();
        String noAccents = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return noAccents.toLowerCase(Locale.ROOT);
    }
}
