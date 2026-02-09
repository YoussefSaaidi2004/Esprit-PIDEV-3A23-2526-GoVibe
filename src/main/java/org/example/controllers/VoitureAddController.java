package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.example.entities.AgenceLocation;
import org.example.entities.Statut;
import org.example.entities.TypeCarburant;
import org.example.entities.Voiture;
import org.example.services.IService;
import org.example.services.ServiceVoiture;
import org.example.utils.SceneNavigator;

public class VoitureAddController {

    @FXML
    private TextField matriculeField;
    @FXML
    private TextField marqueField;
    @FXML
    private TextField modeleField;
    @FXML
    private TextField anneeField;
    @FXML
    private ComboBox<TypeCarburant> typeCarburantBox;
    @FXML
    private TextField prixJourField;
    @FXML
    private ComboBox<Statut> statutBox;
    @FXML
    private ComboBox<AgenceLocation> agenceBox;
    @FXML
    private TextField imageUrlField;
    @FXML
    private TextArea descriptionArea;

    @FXML
    private Button addButton;
    @FXML
    private Button backButton;

    private final IService<Voiture> voitureService = new ServiceVoiture();

    @FXML
    public void initialize() {
        typeCarburantBox.setItems(FXCollections.observableArrayList(TypeCarburant.values()));
        statutBox.setItems(FXCollections.observableArrayList(Statut.values()));
        agenceBox.setItems(FXCollections.observableArrayList(AgenceLocation.values()));
    }

    @FXML
    private void handleAjouter() {
        if (!validateFields()) {
            return;
        }
        Voiture voiture = buildVoitureFromFields();
        try {
            voitureService.add(voiture);
            SceneNavigator.switchTo("/VoitureListView.fxml", addButton);
        } catch (RuntimeException ex) {
            showAlert("Erreur", ex.getMessage());
        }
    }

    @FXML
    private void handleRetour() {
        SceneNavigator.switchTo("/VoitureListView.fxml", backButton);
    }

    private Voiture buildVoitureFromFields() {
        int annee = Integer.parseInt(anneeField.getText().trim());
        double prixJour = Double.parseDouble(prixJourField.getText().trim());
        AgenceLocation agence = agenceBox.getValue();

        return new Voiture(
                matriculeField.getText().trim(),
                marqueField.getText().trim(),
                modeleField.getText().trim(),
                annee,
                typeCarburantBox.getValue(),
                prixJour,
                statutBox.getValue(),
            agence.toDisplay(),
            agence.getLatitude(),
            agence.getLongitude(),
                descriptionArea.getText().trim(),
                imageUrlField.getText().trim()
        );
    }

    private boolean validateFields() {
        if (isBlank(matriculeField) || isBlank(marqueField) || isBlank(modeleField)
                || isBlank(anneeField) || isBlank(prixJourField)
                || isBlank(imageUrlField) || isBlank(descriptionArea)) {
            showAlert("Validation", "Veuillez remplir tous les champs.");
            return false;
        }
        if (typeCarburantBox.getValue() == null || statutBox.getValue() == null || agenceBox.getValue() == null) {
            showAlert("Validation", "Veuillez choisir le carburant, le statut et l'agence.");
            return false;
        }
        try {
            Integer.parseInt(anneeField.getText().trim());
            Double.parseDouble(prixJourField.getText().trim());
        } catch (NumberFormatException ex) {
            showAlert("Validation", "Les champs numeriques sont invalides.");
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
