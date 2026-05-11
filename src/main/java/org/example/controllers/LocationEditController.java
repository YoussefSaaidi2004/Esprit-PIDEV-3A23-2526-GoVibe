package org.example.controllers;

import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import org.example.entities.Location;
import org.example.entities.Statut;
import org.example.entities.StatutLocation;
import org.example.entities.Voiture;
import org.example.services.IService;
import org.example.services.ServiceLocation;
import org.example.services.ServiceVoiture;
import org.example.utils.LocationSelection;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;

import java.io.File;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class LocationEditController {

    @FXML
    private TextField referenceField;
    @FXML
    private ComboBox<Voiture> voitureBox;
    @FXML
    private DatePicker dateDebutPicker;
    @FXML
    private DatePicker dateFinPicker;
    @FXML
    private TextField nbJoursField;
    @FXML
    private TextField montantTotalField;
    @FXML
    private ComboBox<StatutLocation> statutBox;

    @FXML
    private Button updateButton;
    @FXML
    private Button backButton;

    private LocationListController parentController;

    public void setParentController(LocationListController parentController) {
        this.parentController = parentController;
    }

    private final IService<Location> locationService = new ServiceLocation();
    private final IService<Voiture> voitureService = new ServiceVoiture();
    private Location selected;

    @FXML
    public void initialize() {
        if (!SessionManager.isAuthenticated()) {
            SceneNavigator.switchTo("/org/example/LoginView.fxml", backButton);
            return;
        }
        if (SessionManager.isAdmin()) {
            SceneNavigator.switchTo("/AdminLocationListView.fxml", backButton);
            return;
        }
        statutBox.setItems(FXCollections.observableArrayList(StatutLocation.values()));
        selected = LocationSelection.get();
        if (selected == null) {
            showAlert("Selection requise", "Veuillez choisir une location a modifier.");
            handleRetour();
            return;
        }
        int currentUserId = SessionManager.getCurrentUser().getId();
        if (selected.getIdPersonne() != 0 && selected.getIdPersonne() != currentUserId) {
            showAlert("Acces refuse", "Vous ne pouvez modifier que vos propres locations.");
            handleRetour();
            return;
        }
        statutBox.setDisable(true);
        loadAvailableVoitures(selected.getIdVoiture());
        fillForm(selected);
        setupAutoCompute();
    }

    @FXML
    private void handleModifier() {
        if (selected == null) {
            showAlert("Sélection requise", "Veuillez choisir une location à modifier.");
            return;
        }
        if (!validateFields()) {
            return;
        }
        Location updated = buildLocationFromFields();
        updated.setIdLocation(selected.getIdLocation());
        updateButton.setDisable(true);

        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<Void>() {
            @Override
            protected Void call() throws Exception {
                locationService.update(updated);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            LocationSelection.clear();
            if (parentController != null) {
                parentController.closeModal();
            } else {
                SceneNavigator.switchTo("/LocationListView.fxml", updateButton);
            }
        });

        task.setOnFailed(e -> {
            updateButton.setDisable(false);
            showAlert("Erreur", task.getException().getMessage());
        });

        new Thread(task).start();
    }

    @FXML
    private void handleRetour() {
        LocationSelection.clear();
        if (parentController != null) {
            parentController.closeModal();
        } else {
            SceneNavigator.switchTo("/LocationListView.fxml", updateButton);
        }
    }

    @FXML
    private void handleGoHome() {
        SceneNavigator.switchTo("/org/example/UserHomeView.fxml", backButton);
    }

    @FXML
    private void handleGoLocations() {
        SceneNavigator.switchTo("/LocationListView.fxml", backButton);
    }

    @FXML
    private void handleLogout() {
        SessionManager.clear();
        SceneNavigator.switchTo("/org/example/LoginView.fxml", backButton);
    }

    private void loadAvailableVoitures(int selectedId) {
        List<Voiture> voitures = voitureService.getAll();
        List<Voiture> disponibles = voitures.stream()
                .filter(voiture -> voiture.getStatut() == Statut.DISPONIBLE || voiture.getIdVoiture() == selectedId)
                .collect(Collectors.toList());
        voitureBox.setItems(FXCollections.observableArrayList(disponibles));
        voitureBox.setConverter(new javafx.util.StringConverter<Voiture>() {
            @Override
            public String toString(Voiture object) {
                if (object == null) {
                    return "";
                }
                return String.format(Locale.ROOT, "%s %s Â· %s", safeTrim(object.getMarque()), safeTrim(object.getModele()), safeTrim(object.getMatricule()));
            }

            @Override
            public Voiture fromString(String string) {
                return null;
            }
        });
    }

    private void setupAutoCompute() {
        dateDebutPicker.valueProperty().addListener((obs, oldValue, newValue) -> recomputeFields());
        dateFinPicker.valueProperty().addListener((obs, oldValue, newValue) -> recomputeFields());
        voitureBox.valueProperty().addListener((obs, oldValue, newValue) -> recomputeFields());
    }

    private void recomputeFields() {
        LocalDate debut = dateDebutPicker.getValue();
        LocalDate fin = dateFinPicker.getValue();
        Voiture voiture = voitureBox.getValue();
        if (debut == null || fin == null || voiture == null) {
            return;
        }
        if (fin.isBefore(debut)) {
            return;
        }
        int nbJours = (int) ChronoUnit.DAYS.between(debut, fin) + 1;
        double montantTotal = nbJours * voiture.getPrixJour();
        nbJoursField.setText(String.valueOf(nbJours));
        montantTotalField.setText(String.format(Locale.ROOT, "%.2f", montantTotal));
    }

    private Location buildLocationFromFields() {
        Voiture voiture = voitureBox.getValue();
        int personneId = selected != null ? selected.getIdPersonne() : 0;
        return new Location(
                referenceField.getText().trim(),
                dateDebutPicker.getValue(),
                dateFinPicker.getValue(),
                Integer.parseInt(nbJoursField.getText().trim()),
                Double.parseDouble(montantTotalField.getText().trim()),
                selected != null ? safeTrim(selected.getContratPdf()) : "",
                selected != null ? safeTrim(selected.getQrCode()) : "",
                selected != null ? selected.getStatut() : StatutLocation.EN_ATTENTE,
            voiture != null ? voiture.getIdVoiture() : 0,
            personneId
        );
    }

    private void fillForm(Location location) {
        referenceField.setText(location.getReference());
        dateDebutPicker.setValue(location.getDateDebut());
        dateFinPicker.setValue(location.getDateFin());
        nbJoursField.setText(String.valueOf(location.getNbJours()));
        montantTotalField.setText(String.format(Locale.ROOT, "%.2f", location.getMontantTotal()));
        statutBox.setValue(location.getStatut());
        if (location.getVoiture() != null) {
            voitureBox.setValue(location.getVoiture());
        }
    }

    private boolean validateFields() {
        clearValidationStyles();
        boolean valid = true;

        if (isBlank(referenceField)) {
            markInvalid(referenceField);
            valid = false;
        }
        if (voitureBox.getValue() == null) {
            markInvalid(voitureBox);
            valid = false;
        }
        if (dateDebutPicker.getValue() == null) {
            markInvalid(dateDebutPicker);
            valid = false;
        }
        if (dateFinPicker.getValue() == null) {
            markInvalid(dateFinPicker);
            valid = false;
        }
        if (isBlank(nbJoursField)) {
            markInvalid(nbJoursField);
            valid = false;
        }
        if (isBlank(montantTotalField)) {
            markInvalid(montantTotalField);
            valid = false;
        }
        if (statutBox.getValue() == null) {
            markInvalid(statutBox);
            valid = false;
        }

        if (dateDebutPicker.getValue() != null && dateFinPicker.getValue() != null) {
            if (dateFinPicker.getValue().isBefore(dateDebutPicker.getValue())) {
                markInvalid(dateFinPicker);
                valid = false;
            }
        }

        if (!isBlank(nbJoursField)) {
            try {
                Integer.parseInt(nbJoursField.getText().trim());
            } catch (NumberFormatException ex) {
                markInvalid(nbJoursField);
                valid = false;
            }
        }
        if (!isBlank(montantTotalField)) {
            try {
                Double.parseDouble(montantTotalField.getText().trim());
            } catch (NumberFormatException ex) {
                markInvalid(montantTotalField);
                valid = false;
            }
        }
        return valid;
    }

    private boolean isBlank(TextField field) {
        return field.getText() == null || field.getText().trim().isEmpty();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearValidationStyles() {
        removeErrorStyle(referenceField);
        removeErrorStyle(voitureBox);
        removeErrorStyle(dateDebutPicker);
        removeErrorStyle(dateFinPicker);
        removeErrorStyle(nbJoursField);
        removeErrorStyle(montantTotalField);
        removeErrorStyle(statutBox);
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

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
