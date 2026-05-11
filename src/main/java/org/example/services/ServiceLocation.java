package org.example.services;

import org.example.entities.Location;
import org.example.entities.Statut;
import org.example.entities.StatutLocation;
import org.example.entities.Voiture;
import org.example.entities.personne;
import org.example.utils.MyDataBase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceLocation implements IService<Location> {

    private final Connection connection;

    public ServiceLocation() {
        this.connection = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void add(Location location) {
        String sql = "INSERT INTO location (reference, date_debut, date_fin, nb_jours, montant_total, contrat_pdf, qr_code, statut, id_voiture, user_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, location.getReference());
            ps.setDate(2, Date.valueOf(location.getDateDebut()));
            ps.setDate(3, Date.valueOf(location.getDateFin()));
            ps.setInt(4, location.getNbJours());
            ps.setDouble(5, location.getMontantTotal());
            ps.setString(6, location.getContratPdf());
            ps.setString(7, location.getQrCode());
            StatutLocation statut = location.getStatut();
            ps.setString(8, statut != null ? statut.toDbValue() : StatutLocation.EN_ATTENTE.toDbValue());
            ps.setInt(9, location.getIdVoiture());
            if (location.getIdPersonne() > 0) {
                ps.setInt(10, location.getIdPersonne());
            } else {
                ps.setNull(10, Types.INTEGER);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout de la location: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Location location) {
        String sql = "UPDATE location SET reference=?, date_debut=?, date_fin=?, nb_jours=?, montant_total=?, contrat_pdf=?, qr_code=?, statut=?, id_voiture=?, user_id=? " +
                "WHERE id_location=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, location.getReference());
            ps.setDate(2, Date.valueOf(location.getDateDebut()));
            ps.setDate(3, Date.valueOf(location.getDateFin()));
            ps.setInt(4, location.getNbJours());
            ps.setDouble(5, location.getMontantTotal());
            ps.setString(6, location.getContratPdf());
            ps.setString(7, location.getQrCode());
            StatutLocation statut = location.getStatut();
            ps.setString(8, statut != null ? statut.toDbValue() : StatutLocation.EN_ATTENTE.toDbValue());
            ps.setInt(9, location.getIdVoiture());
            if (location.getIdPersonne() > 0) {
                ps.setInt(10, location.getIdPersonne());
            } else {
                ps.setNull(10, Types.INTEGER);
            }
            ps.setInt(11, location.getIdLocation());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la modification de la location: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM location WHERE id_location=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression de la location", e);
        }
    }

    @Override
    public List<Location> getAll() {
        return loadLocations(null);
    }

    public List<Location> getAllByPersonneId(int personneId) {
        return loadLocations(personneId);
    }

    private List<Location> loadLocations(Integer personneId) {
        List<Location> locations = new ArrayList<>();
        String sql = "SELECT l.id_location, l.reference, l.date_debut, l.date_fin, l.nb_jours, l.montant_total, l.contrat_pdf, " +
            "l.qr_code, l.statut, l.date_creation, l.id_voiture as l_voiture_id, l.user_id, " +
            "v.id_voiture AS v_id, v.matricule, v.marque, v.modele, v.annee, v.type_carburant, v.prix_jour, " +
            "v.statut AS v_statut, v.adresse_agence, v.latitude, v.longitude, v.description, v.image_url, v.date_creation AS v_date_creation, " +
            "p.id AS p_id, p.nom AS p_nom, p.prenom AS p_prenom, p.email AS p_email, p.password AS p_password, p.role AS p_role " +
            "FROM location l " +
            "JOIN voiture v ON v.id_voiture = l.id_voiture " +
            "LEFT JOIN personne p ON p.id = l.user_id " +
            (personneId != null ? "WHERE l.user_id = ? " : "") +
            "ORDER BY l.id_location DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (personneId != null) {
                ps.setInt(1, personneId);
            }
            try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id_location");
                String reference = rs.getString("reference");
                LocalDate dateDebut = rs.getDate("date_debut").toLocalDate();
                LocalDate dateFin = rs.getDate("date_fin").toLocalDate();
                int nbJours = rs.getInt("nb_jours");
                double montantTotal = rs.getDouble("montant_total");
                String contratPdf = rs.getString("contrat_pdf");
                String qrCode = rs.getString("qr_code");
                String statutValue = rs.getString("statut");
                StatutLocation statut = statutValue == null ? StatutLocation.EN_ATTENTE : StatutLocation.fromDbValue(statutValue);
                Timestamp ts = rs.getTimestamp("date_creation");
                LocalDateTime dateCreation = ts != null ? ts.toLocalDateTime() : null;
                int idVoiture = rs.getInt("l_voiture_id");
                int idPersonne = rs.getInt("user_id");
                if (rs.wasNull()) {
                    idPersonne = 0;
                }

                Voiture voiture = new Voiture(
                        rs.getInt("v_id"),
                        rs.getString("matricule"),
                        rs.getString("marque"),
                        rs.getString("modele"),
                        rs.getInt("annee"),
                        org.example.entities.TypeCarburant.fromDbValue(rs.getString("type_carburant")),
                        rs.getDouble("prix_jour"),
                        org.example.entities.Statut.fromDbValue(rs.getString("v_statut")),
                        rs.getString("adresse_agence"),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude"),
                        rs.getString("description"),
                        rs.getString("image_url"),
                        rs.getTimestamp("v_date_creation") != null ? rs.getTimestamp("v_date_creation").toLocalDateTime() : null
                );

                personne client = null;
                int clientId = rs.getInt("p_id");
                if (!rs.wasNull()) {
                    client = new personne(
                        clientId,
                        rs.getString("p_nom"),
                        rs.getString("p_prenom"),
                        rs.getString("p_email"),
                        rs.getString("p_password"),
                        rs.getString("p_role")
                    );
                }

                locations.add(new Location(
                    id, reference, dateDebut, dateFin, nbJours, montantTotal,
                    contratPdf, qrCode, statut, dateCreation, idVoiture, idPersonne, voiture, client
                ));
            }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation des locations", e);
        }
        return locations;
    }

    public void updateStatusAndHandleOverlap(int locationId, StatutLocation newStatus) {
        String selectSql = "SELECT id_voiture, date_debut, date_fin FROM location WHERE id_location=?";
        String updateSql = "UPDATE location SET statut=? WHERE id_location=?";
        String cancelSql = "UPDATE location SET statut='ANNULEE' WHERE id_voiture=? AND id_location<>? " +
                "AND statut<>'ANNULEE' AND date_debut <= ? AND date_fin >= ?";
            String updateCarSql = "UPDATE voiture SET statut=? WHERE id_voiture=?";
            String countConfirmedSql = "SELECT COUNT(*) FROM location WHERE id_voiture=? AND statut='CONFIRMEE'";

        boolean originalAutoCommit = true;
        try {
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            int idVoiture = 0;
            LocalDate dateDebut = null;
            LocalDate dateFin = null;
            try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
                ps.setInt(1, locationId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        idVoiture = rs.getInt("id_voiture");
                        dateDebut = rs.getDate("date_debut").toLocalDate();
                        dateFin = rs.getDate("date_fin").toLocalDate();
                    } else {
                        throw new RuntimeException("Location introuvable.");
                    }
                }
            }

            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                ps.setString(1, newStatus.toDbValue());
                ps.setInt(2, locationId);
                ps.executeUpdate();
            }

            if (newStatus == StatutLocation.CONFIRMEE) {
                try (PreparedStatement ps = connection.prepareStatement(cancelSql)) {
                    ps.setInt(1, idVoiture);
                    ps.setInt(2, locationId);
                    ps.setDate(3, Date.valueOf(dateFin));
                    ps.setDate(4, Date.valueOf(dateDebut));
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = connection.prepareStatement(updateCarSql)) {
                    ps.setString(1, Statut.LOUEE.toDbValue());
                    ps.setInt(2, idVoiture);
                    ps.executeUpdate();
                }
            } else if (newStatus == StatutLocation.ANNULEE) {
                int confirmedCount = 0;
                try (PreparedStatement ps = connection.prepareStatement(countConfirmedSql)) {
                    ps.setInt(1, idVoiture);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            confirmedCount = rs.getInt(1);
                        }
                    }
                }
                if (confirmedCount == 0) {
                    try (PreparedStatement ps = connection.prepareStatement(updateCarSql)) {
                        ps.setString(1, Statut.DISPONIBLE.toDbValue());
                        ps.setInt(2, idVoiture);
                        ps.executeUpdate();
                    }
                }
            }

            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                // ignore rollback failure
            }
            throw new RuntimeException("Erreur lors de la mise a jour du statut", e);
        } finally {
            try {
                connection.setAutoCommit(originalAutoCommit);
            } catch (SQLException ex) {
                // ignore restore failure
            }
        }
    }
}
