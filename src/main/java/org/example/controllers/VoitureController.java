package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.entities.AgenceLocation;
import org.example.entities.Statut;
import org.example.entities.TypeCarburant;
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
    private ComboBox<TypeCarburant> typeCarburantField;
    @FXML
    private TextField prixJourField;
    @FXML
    private ComboBox<Statut> statutField;
    @FXML
    private ComboBox<AgenceLocation> agenceBox;
    @FXML
    private TextField imageUrlField;
    @FXML
    private TextArea descriptionArea;

    @FXML
    private Button addButton;
    @FXML
    private Button updateButton;

    @FXML
    private ListView<Voiture> voitureList;

    private final ObservableList<Voiture> voitureItems = FXCollections.observableArrayList();
    private final IService<Voiture> voitureService = new ServiceVoiture();

    @FXML
    public void initialize() {
        voitureList.setItems(voitureItems);
        voitureList.setCellFactory(list -> new VoitureCell());
        typeCarburantField.setItems(FXCollections.observableArrayList(TypeCarburant.values()));
        statutField.setItems(FXCollections.observableArrayList(Statut.values()));
        agenceBox.setItems(FXCollections.observableArrayList(AgenceLocation.values()));
        refreshTable();
        setAddMode();

        voitureList.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
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
        try {
            voitureService.add(voiture);
            refreshTable();
            clearFields();
            setAddMode();
        } catch (RuntimeException ex) {
            showAlert("Erreur", ex.getMessage());
        }
    }

    @FXML
    private void handleModifier(ActionEvent event) {
        Voiture selected = voitureList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Sélection requise", "Veuillez sélectionner une voiture à modifier.");
            return;
        }
        if (!validateFields()) {
            return;
        }
        Voiture updated = buildVoitureFromFields();
        updated.setIdVoiture(selected.getIdVoiture());
        try {
            voitureService.update(updated);
            refreshTable();
            clearFields();
            setAddMode();
        } catch (RuntimeException ex) {
            showAlert("Erreur", ex.getMessage());
        }
    }

    @FXML
    private void handleSupprimer(ActionEvent event) {
        Voiture selected = voitureList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Sélection requise", "Veuillez sélectionner une voiture à supprimer.");
            return;
        }
        voitureService.delete(selected.getIdVoiture());
        refreshTable();
        clearFields();
        setAddMode();
    }

    public void refreshTable() {
        List<Voiture> voitures = voitureService.getAll();
        voitureItems.setAll(voitures);
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
                typeCarburantField.getValue(),
                prixJour,
                statutField.getValue(),
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
        if (typeCarburantField.getValue() == null || statutField.getValue() == null || agenceBox.getValue() == null) {
            showAlert("Validation", "Veuillez remplir tous les champs.");
            return false;
        }
        try {
            Integer.parseInt(anneeField.getText().trim());
            Double.parseDouble(prixJourField.getText().trim());
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
        typeCarburantField.setValue(voiture.getTypeCarburant());
        prixJourField.setText(String.valueOf(voiture.getPrixJour()));
        statutField.setValue(voiture.getStatut());
        agenceBox.setValue(findAgence(voiture));
        descriptionArea.setText(voiture.getDescription());
        imageUrlField.setText(voiture.getImageUrl());
    }

    private void clearFields() {
        matriculeField.clear();
        marqueField.clear();
        modeleField.clear();
        anneeField.clear();
        typeCarburantField.getSelectionModel().clearSelection();
        prixJourField.clear();
        statutField.getSelectionModel().clearSelection();
        agenceBox.getSelectionModel().clearSelection();
        imageUrlField.clear();
        descriptionArea.clear();
        voitureList.getSelectionModel().clearSelection();
    }

    private void setAddMode() {
        addButton.setDisable(false);
        updateButton.setDisable(true);
    }

    private void setEditMode() {
        addButton.setDisable(true);
        updateButton.setDisable(false);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private class VoitureCell extends ListCell<Voiture> {
        private final HBox root = new HBox();
        private final VBox left = new VBox();
        private final VBox right = new VBox();
        private final Label title = new Label();
        private final Label subtitle = new Label();
        private final Label meta = new Label();
        private final Label price = new Label();
        private final Label status = new Label();
        private final HBox rowActions = new HBox();
        private final Button editButton = new Button("Modifier");
        private final Button deleteButton = new Button("Supprimer");
        private final Region spacer = new Region();

        VoitureCell() {
            root.getStyleClass().add("voiture-cell");
            left.getStyleClass().add("voiture-cell-left");
            right.getStyleClass().add("voiture-cell-right");
            title.getStyleClass().add("voiture-title");
            subtitle.getStyleClass().add("voiture-subtitle");
            meta.getStyleClass().add("voiture-meta");
            price.getStyleClass().add("voiture-price");
            status.getStyleClass().add("voiture-status");
            rowActions.getStyleClass().add("row-actions");
            editButton.getStyleClass().addAll("row-btn", "row-btn-edit");
            deleteButton.getStyleClass().addAll("row-btn", "row-btn-delete");

            left.getChildren().addAll(title, subtitle, meta);
            rowActions.getChildren().addAll(editButton, deleteButton);
            right.getChildren().addAll(price, status, rowActions);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            root.getChildren().addAll(left, spacer, right);
            setText(null);
        }

        @Override
        protected void updateItem(Voiture item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            title.setText(item.getMarque() + " " + item.getModele() + " · " + item.getMatricule());
            subtitle.setText("Annee " + item.getAnnee() + " · " + item.getTypeCarburant());
            meta.setText(item.getAdresseAgence());
            price.setText(String.format("%.2f TND / jour", item.getPrixJour()));
            status.setText(item.getStatut().toString());

            editButton.setOnAction(event -> {
                if (getListView() != null) {
                    getListView().getSelectionModel().select(item);
                }
                fillForm(item);
                setEditMode();
            });

            deleteButton.setOnAction(event -> deleteFromRow(item));

            setGraphic(root);
        }
    }

    private void deleteFromRow(Voiture item) {
        try {
            voitureService.delete(item.getIdVoiture());
            refreshTable();
            clearFields();
            setAddMode();
        } catch (RuntimeException ex) {
            showAlert("Erreur", ex.getMessage());
        }
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
}
