package org.example.controllers;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.example.entities.Voiture;
import org.example.services.IService;
import org.example.services.ServiceVoiture;

import java.util.List;

public class VoitureController {

    @FXML
    private TextField matriculeField;
    @FXML
    private TextField marqueField;
    @FXML
    private TextField modeleField;
    @FXML
    private TextField anneeField;
    @FXML
    private TextField typeCarburantField;
    @FXML
    private TextField prixJourField;
    @FXML
    private TextField statutField;
    @FXML
    private TextField adresseAgenceField;
    @FXML
    private TextField latitudeField;
    @FXML
    private TextField longitudeField;
    @FXML
    private TextField imageUrlField;
    @FXML
    private TextArea descriptionArea;

    @FXML
    private TableView<Voiture> voitureTable;
    @FXML
    private TableColumn<Voiture, Number> idColumn;
    @FXML
    private TableColumn<Voiture, String> matriculeColumn;
    @FXML
    private TableColumn<Voiture, String> marqueColumn;
    @FXML
    private TableColumn<Voiture, String> modeleColumn;
    @FXML
    private TableColumn<Voiture, Number> anneeColumn;
    @FXML
    private TableColumn<Voiture, String> typeCarburantColumn;
    @FXML
    private TableColumn<Voiture, Number> prixJourColumn;
    @FXML
    private TableColumn<Voiture, String> statutColumn;

    private final ObservableList<Voiture> voitureItems = FXCollections.observableArrayList();
    private final IService<Voiture> voitureService = new ServiceVoiture();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getIdVoiture()));
        matriculeColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMatricule()));
        marqueColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMarque()));
        modeleColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getModele()));
        anneeColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getAnnee()));
        typeCarburantColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTypeCarburant()));
        prixJourColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getPrixJour()));
        statutColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatut()));

        voitureTable.setItems(voitureItems);
        refreshTable();

        voitureTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                fillForm(newSel);
            }
        });
    }

    @FXML
    private void handleAjouter(ActionEvent event) {
        if (!validateFields()) {
            return;
        }
        Voiture voiture = buildVoitureFromFields();
        voitureService.add(voiture);
        refreshTable();
        clearFields();
    }

    @FXML
    private void handleModifier(ActionEvent event) {
        Voiture selected = voitureTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Sélection requise", "Veuillez sélectionner une voiture à modifier.");
            return;
        }
        if (!validateFields()) {
            return;
        }
        Voiture updated = buildVoitureFromFields();
        updated.setIdVoiture(selected.getIdVoiture());
        voitureService.update(updated);
        refreshTable();
        clearFields();
    }

    @FXML
    private void handleSupprimer(ActionEvent event) {
        Voiture selected = voitureTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Sélection requise", "Veuillez sélectionner une voiture à supprimer.");
            return;
        }
        voitureService.delete(selected.getIdVoiture());
        refreshTable();
        clearFields();
    }

    public void refreshTable() {
        List<Voiture> voitures = voitureService.getAll();
        voitureItems.setAll(voitures);
    }

    private Voiture buildVoitureFromFields() {
        int annee = Integer.parseInt(anneeField.getText().trim());
        double prixJour = Double.parseDouble(prixJourField.getText().trim());
        double latitude = Double.parseDouble(latitudeField.getText().trim());
        double longitude = Double.parseDouble(longitudeField.getText().trim());

        return new Voiture(
                matriculeField.getText().trim(),
                marqueField.getText().trim(),
                modeleField.getText().trim(),
                annee,
                typeCarburantField.getText().trim(),
                prixJour,
                statutField.getText().trim(),
                adresseAgenceField.getText().trim(),
                latitude,
                longitude,
                descriptionArea.getText().trim(),
                imageUrlField.getText().trim()
        );
    }

    private boolean validateFields() {
        if (isBlank(matriculeField) || isBlank(marqueField) || isBlank(modeleField)
                || isBlank(anneeField) || isBlank(typeCarburantField) || isBlank(prixJourField)
                || isBlank(statutField) || isBlank(adresseAgenceField) || isBlank(latitudeField)
                || isBlank(longitudeField) || isBlank(imageUrlField) || isBlank(descriptionArea)) {
            showAlert("Validation", "Veuillez remplir tous les champs.");
            return false;
        }
        try {
            Integer.parseInt(anneeField.getText().trim());
            Double.parseDouble(prixJourField.getText().trim());
            Double.parseDouble(latitudeField.getText().trim());
            Double.parseDouble(longitudeField.getText().trim());
        } catch (NumberFormatException ex) {
            showAlert("Validation", "Les champs numériques sont invalides.");
            return false;
        }
        return true;
    }

    private boolean isBlank(TextField field) {
        return field.getText() == null || field.getText().trim().isEmpty();
    }

    private boolean isBlank(TextArea area) {
        return area.getText() == null || area.getText().trim().isEmpty();
    }

    private void fillForm(Voiture voiture) {
        matriculeField.setText(voiture.getMatricule());
        marqueField.setText(voiture.getMarque());
        modeleField.setText(voiture.getModele());
        anneeField.setText(String.valueOf(voiture.getAnnee()));
        typeCarburantField.setText(voiture.getTypeCarburant());
        prixJourField.setText(String.valueOf(voiture.getPrixJour()));
        statutField.setText(voiture.getStatut());
        adresseAgenceField.setText(voiture.getAdresseAgence());
        latitudeField.setText(String.valueOf(voiture.getLatitude()));
        longitudeField.setText(String.valueOf(voiture.getLongitude()));
        descriptionArea.setText(voiture.getDescription());
        imageUrlField.setText(voiture.getImageUrl());
    }

    private void clearFields() {
        matriculeField.clear();
        marqueField.clear();
        modeleField.clear();
        anneeField.clear();
        typeCarburantField.clear();
        prixJourField.clear();
        statutField.clear();
        adresseAgenceField.clear();
        latitudeField.clear();
        longitudeField.clear();
        imageUrlField.clear();
        descriptionArea.clear();
        voitureTable.getSelectionModel().clearSelection();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
