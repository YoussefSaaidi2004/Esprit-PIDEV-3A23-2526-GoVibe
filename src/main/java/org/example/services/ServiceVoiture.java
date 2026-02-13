package org.example.services;

import org.example.entities.Voiture;
import org.example.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.example.entities.Statut;
import org.example.entities.TypeCarburant;

public class ServiceVoiture implements IService<Voiture> {

    private final Connection connection;

    public ServiceVoiture() {
        this.connection = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void add(Voiture voiture) {
        String sql = "INSERT INTO voiture (matricule, marque, modele, annee, type_carburant, prix_jour, statut, adresse_agence, latitude, longitude, description, image_url) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            TypeCarburant typeCarburant = voiture.getTypeCarburant();
            Statut statut = voiture.getStatut();
            if (typeCarburant == null || statut == null) {
                throw new RuntimeException("Champ requis manquant.");
            }
            ps.setString(1, voiture.getMatricule());
            ps.setString(2, voiture.getMarque());
            ps.setString(3, voiture.getModele());
            ps.setInt(4, voiture.getAnnee());
            ps.setString(5, typeCarburant.toDbValue());
            ps.setDouble(6, voiture.getPrixJour());
            ps.setString(7, statut.toDbValue());
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
            TypeCarburant typeCarburant = voiture.getTypeCarburant();
            Statut statut = voiture.getStatut();
            if (typeCarburant == null || statut == null) {
                throw new RuntimeException("Champ requis manquant.");
            }
            ps.setString(1, voiture.getMatricule());
            ps.setString(2, voiture.getMarque());
            ps.setString(3, voiture.getModele());
            ps.setInt(4, voiture.getAnnee());
            ps.setString(5, typeCarburant.toDbValue());
            ps.setDouble(6, voiture.getPrixJour());
            ps.setString(7, statut.toDbValue());
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
                TypeCarburant typeCarburant = TypeCarburant.fromDbValue(rs.getString("type_carburant"));
                double prixJour = rs.getDouble("prix_jour");
                Statut statut = Statut.fromDbValue(rs.getString("statut"));
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

}
