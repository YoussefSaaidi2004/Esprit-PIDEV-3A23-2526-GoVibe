package org.example.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Location {

    private int idLocation;
    private String reference;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private int nbJours;
    private double montantTotal;
    private String contratPdf;
    private String qrCode;
    private StatutLocation statut;
    private LocalDateTime dateCreation;
    private int idVoiture;
    private Voiture voiture;
    private int idPersonne;
    private personne personne;

    public Location() {
    }

    public Location(String reference, LocalDate dateDebut, LocalDate dateFin,
                    int nbJours, double montantTotal, String contratPdf, String qrCode,
                    StatutLocation statut, int idVoiture) {
        this.reference = reference;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.nbJours = nbJours;
        this.montantTotal = montantTotal;
        this.contratPdf = contratPdf;
        this.qrCode = qrCode;
        this.statut = statut;
        this.idVoiture = idVoiture;
    }

    public Location(String reference, LocalDate dateDebut, LocalDate dateFin,
                    int nbJours, double montantTotal, String contratPdf, String qrCode,
                    StatutLocation statut, int idVoiture, int idPersonne) {
        this(reference, dateDebut, dateFin, nbJours, montantTotal, contratPdf, qrCode, statut, idVoiture);
        this.idPersonne = idPersonne;
    }

    public Location(int idLocation, String reference, LocalDate dateDebut, LocalDate dateFin,
                    int nbJours, double montantTotal, String contratPdf, String qrCode,
                    StatutLocation statut, LocalDateTime dateCreation, int idVoiture, Voiture voiture) {
        this.idLocation = idLocation;
        this.reference = reference;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.nbJours = nbJours;
        this.montantTotal = montantTotal;
        this.contratPdf = contratPdf;
        this.qrCode = qrCode;
        this.statut = statut;
        this.dateCreation = dateCreation;
        this.idVoiture = idVoiture;
        this.voiture = voiture;
    }

    public Location(int idLocation, String reference, LocalDate dateDebut, LocalDate dateFin,
                    int nbJours, double montantTotal, String contratPdf, String qrCode,
                    StatutLocation statut, LocalDateTime dateCreation, int idVoiture, int idPersonne,
                    Voiture voiture, personne personne) {
        this(idLocation, reference, dateDebut, dateFin, nbJours, montantTotal, contratPdf, qrCode,
                statut, dateCreation, idVoiture, voiture);
        this.idPersonne = idPersonne;
        this.personne = personne;
    }

    public int getIdLocation() {
        return idLocation;
    }

    public void setIdLocation(int idLocation) {
        this.idLocation = idLocation;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public int getNbJours() {
        return nbJours;
    }

    public void setNbJours(int nbJours) {
        this.nbJours = nbJours;
    }

    public double getMontantTotal() {
        return montantTotal;
    }

    public void setMontantTotal(double montantTotal) {
        this.montantTotal = montantTotal;
    }

    public String getContratPdf() {
        return contratPdf;
    }

    public void setContratPdf(String contratPdf) {
        this.contratPdf = contratPdf;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public StatutLocation getStatut() {
        return statut;
    }

    public void setStatut(StatutLocation statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public int getIdVoiture() {
        return idVoiture;
    }

    public void setIdVoiture(int idVoiture) {
        this.idVoiture = idVoiture;
    }

    public Voiture getVoiture() {
        return voiture;
    }

    public void setVoiture(Voiture voiture) {
        this.voiture = voiture;
    }

    public int getIdPersonne() {
        return idPersonne;
    }

    public void setIdPersonne(int idPersonne) {
        this.idPersonne = idPersonne;
    }

    public personne getPersonne() {
        return personne;
    }

    public void setPersonne(personne personne) {
        this.personne = personne;
    }
}
