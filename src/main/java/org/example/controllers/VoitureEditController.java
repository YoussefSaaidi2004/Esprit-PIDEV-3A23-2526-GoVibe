package org.example.controllers;

import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import org.example.entities.AgenceLocation;
import org.example.entities.Statut;
import org.example.entities.TypeCarburant;
import org.example.entities.Voiture;
import org.example.services.IService;
import org.example.services.ServiceVoiture;
import org.example.utils.SceneNavigator;
import org.example.utils.VoitureSelection;

public class VoitureEditController {

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
    private Button updateButton;
    @FXML
    private Button backButton;

    private final IService<Voiture> voitureService = new ServiceVoiture();
    private Voiture selected;

    @FXML
    public void initialize() {
        typeCarburantBox.setItems(FXCollections.observableArrayList(TypeCarburant.values()));
        statutBox.setItems(FXCollections.observableArrayList(Statut.values()));
        agenceBox.setItems(FXCollections.observableArrayList(AgenceLocation.values()));

        selected = VoitureSelection.get();
        if (selected == null) {
            showAlert("Selection requise", "Veuillez choisir une voiture a modifier.");
            SceneNavigator.switchTo("/VoitureListView.fxml", backButton);
            return;
        }
        fillForm(selected);
    }

    @FXML
    private void handleModifier() {
        if (selected == null) {
            showAlert("Selection requise", "Veuillez choisir une voiture a modifier.");
            return;
        }
        if (!validateFields()) {
            return;
        }
        Voiture updated = buildVoitureFromFields();
        updated.setIdVoiture(selected.getIdVoiture());
        try {
            voitureService.update(updated);
            VoitureSelection.clear();
            SceneNavigator.switchTo("/VoitureListView.fxml", updateButton);
        } catch (RuntimeException ex) {
            showAlert("Erreur", ex.getMessage());
        }
    }

    @FXML
    private void handleRetour() {
        VoitureSelection.clear();
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

    private void fillForm(Voiture voiture) {
        matriculeField.setText(voiture.getMatricule());
        marqueField.setText(voiture.getMarque());
        modeleField.setText(voiture.getModele());
        anneeField.setText(String.valueOf(voiture.getAnnee()));
        typeCarburantBox.setValue(voiture.getTypeCarburant());
        prixJourField.setText(String.valueOf(voiture.getPrixJour()));
        statutBox.setValue(voiture.getStatut());
        agenceBox.setValue(findAgence(voiture));
        descriptionArea.setText(voiture.getDescription());
        imageUrlField.setText(voiture.getImageUrl());
    }

    private boolean validateFields() {
        clearValidationStyles();
        boolean valid = true;

        if (isBlank(matriculeField)) {
            markInvalid(matriculeField);
            valid = false;
        }
        if (isBlank(marqueField)) {
            markInvalid(marqueField);
            valid = false;
        }
        if (isBlank(modeleField)) {
            markInvalid(modeleField);
            valid = false;
        }
        if (isBlank(anneeField)) {
            markInvalid(anneeField);
            valid = false;
        }
        if (isBlank(prixJourField)) {
            markInvalid(prixJourField);
            valid = false;
        }
        if (isBlank(imageUrlField)) {
            markInvalid(imageUrlField);
            valid = false;
        }
        if (isBlank(descriptionArea)) {
            markInvalid(descriptionArea);
            valid = false;
        }
        if (typeCarburantBox.getValue() == null) {
            markInvalid(typeCarburantBox);
            valid = false;
        }
        if (statutBox.getValue() == null) {
            markInvalid(statutBox);
            valid = false;
        }
        if (agenceBox.getValue() == null) {
            markInvalid(agenceBox);
            valid = false;
        }

        if (!isBlank(anneeField)) {
            try {
                Integer.parseInt(anneeField.getText().trim());
            } catch (NumberFormatException ex) {
                markInvalid(anneeField);
                valid = false;
            }
        }
        if (!isBlank(prixJourField)) {
            try {
                Double.parseDouble(prixJourField.getText().trim());
            } catch (NumberFormatException ex) {
                markInvalid(prixJourField);
                valid = false;
            }
        }
        return valid;
    }

    private AgenceLocation findAgence(Voiture voiture) {
        if (voiture == null) {
            return null;
        }
        for (AgenceLocation agence : AgenceLocation.values()) {
            if (agence.toDisplay().equalsIgnoreCase(voiture.getAdresseAgence())) {
                return agence;
            }
        }
        return null;
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

    private void clearValidationStyles() {
        removeErrorStyle(matriculeField);
        removeErrorStyle(marqueField);
        removeErrorStyle(modeleField);
        removeErrorStyle(anneeField);
        removeErrorStyle(prixJourField);
        removeErrorStyle(imageUrlField);
        removeErrorStyle(descriptionArea);
        removeErrorStyle(typeCarburantBox);
        removeErrorStyle(statutBox);
        removeErrorStyle(agenceBox);
    }

    private void markInvalid(Node node) {
        if (!node.getStyleClass().contains("field-error")) {
            node.getStyleClass().add("field-error");
        }
        playShake(node);
    }

    private void removeErrorStyle(Node node) {
        node.getStyleClass().remove("field-error");
    }

    private void playShake(Node node) {
        TranslateTransition transition = new TranslateTransition(Duration.millis(220), node);
        transition.setFromX(0);
        transition.setByX(8);
        transition.setCycleCount(4);
        transition.setAutoReverse(true);
        transition.playFromStart();
    }
}
