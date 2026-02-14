package org.example.utils;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.regex.Pattern;

public class FormValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+?[0-9\\s-]{8,20}$"
    );

    public static Label createErrorLabel() {
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 11px; -fx-padding: 2 0 0 0;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        return errorLabel;
    }

    public static void showError(Label errorLabel, String message) {
        errorLabel.setText("⚠ " + message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    public static void hideError(Label errorLabel) {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    public static boolean validateRequired(TextField field, Label errorLabel, String fieldName) {
        if (field.getText() == null || field.getText().trim().isEmpty()) {
            showError(errorLabel, fieldName + " est obligatoire");
            field.setStyle("-fx-border-color: #E74C3C; -fx-border-width: 2;");
            return false;
        }
        hideError(errorLabel);
        field.setStyle("");
        return true;
    }

    public static boolean validateEmail(TextField field, Label errorLabel) {
        String email = field.getText();
        if (email == null || email.trim().isEmpty()) {
            showError(errorLabel, "Email est obligatoire");
            field.setStyle("-fx-border-color: #E74C3C; -fx-border-width: 2;");
            return false;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError(errorLabel, "Format email invalide");
            field.setStyle("-fx-border-color: #E74C3C; -fx-border-width: 2;");
            return false;
        }
        hideError(errorLabel);
        field.setStyle("");
        return true;
    }

    public static boolean validatePhone(TextField field, Label errorLabel) {
        String phone = field.getText();
        if (phone == null || phone.trim().isEmpty()) {
            showError(errorLabel, "Téléphone est obligatoire");
            field.setStyle("-fx-border-color: #E74C3C; -fx-border-width: 2;");
            return false;
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            showError(errorLabel, "Format téléphone invalide (ex: +216 12 345 678)");
            field.setStyle("-fx-border-color: #E74C3C; -fx-border-width: 2;");
            return false;
        }
        hideError(errorLabel);
        field.setStyle("");
        return true;
    }

    public static boolean validateDouble(TextField field, Label errorLabel, String fieldName) {
        String value = field.getText();
        if (value == null || value.trim().isEmpty()) {
            showError(errorLabel, fieldName + " est obligatoire");
            field.setStyle("-fx-border-color: #E74C3C; -fx-border-width: 2;");
            return false;
        }
        try {
            double num = Double.parseDouble(value);
            if (num < 0) {
                showError(errorLabel, fieldName + " doit être positif");
                field.setStyle("-fx-border-color: #E74C3C; -fx-border-width: 2;");
                return false;
            }
            hideError(errorLabel);
            field.setStyle("");
            return true;
        } catch (NumberFormatException e) {
            showError(errorLabel, "Veuillez entrer un nombre valide");
            field.setStyle("-fx-border-color: #E74C3C; -fx-border-width: 2;");
            return false;
        }
    }

    public static boolean validateInteger(TextField field, Label errorLabel, String fieldName, int min, int max) {
        String value = field.getText();
        if (value == null || value.trim().isEmpty()) {
            showError(errorLabel, fieldName + " est obligatoire");
            field.setStyle("-fx-border-color: #E74C3C; -fx-border-width: 2;");
            return false;
        }
        try {
            int num = Integer.parseInt(value);
            if (num < min || num > max) {
                showError(errorLabel, fieldName + " doit être entre " + min + " et " + max);
                field.setStyle("-fx-border-color: #E74C3C; -fx-border-width: 2;");
                return false;
            }
            hideError(errorLabel);
            field.setStyle("");
            return true;
        } catch (NumberFormatException e) {
            showError(errorLabel, "Veuillez entrer un nombre entier valide");
            field.setStyle("-fx-border-color: #E74C3C; -fx-border-width: 2;");
            return false;
        }
    }

    public static boolean validateDate(DatePicker datePicker, Label errorLabel, String fieldName, boolean allowPast) {
        LocalDate date = datePicker.getValue();
        if (date == null) {
            showError(errorLabel, fieldName + " est obligatoire");
            datePicker.setStyle("-fx-border-color: #E74C3C; -fx-border-width: 2;");
            return false;
        }
        if (!allowPast && date.isBefore(LocalDate.now())) {
            showError(errorLabel, "La date ne peut pas être dans le passé");
            datePicker.setStyle("-fx-border-color: #E74C3C; -fx-border-width: 2;");
            return false;
        }
        hideError(errorLabel);
        datePicker.setStyle("");
        return true;
    }

    public static boolean validateDateRange(DatePicker startPicker, DatePicker endPicker,
                                            Label startError, Label endError) {
        LocalDate start = startPicker.getValue();
        LocalDate end = endPicker.getValue();

        if (start == null) {
            showError(startError, "Date de début obligatoire");
            startPicker.setStyle("-fx-border-color: #E74C3C; -fx-border-width: 2;");
            return false;
        }
        if (end == null) {
            showError(endError, "Date de fin obligatoire");
            endPicker.setStyle("-fx-border-color: #E74C3C; -fx-border-width: 2;");
            return false;
        }
        if (start.isBefore(LocalDate.now())) {
            showError(startError, "La date ne peut pas être dans le passé");
            startPicker.setStyle("-fx-border-color: #E74C3C; -fx-border-width: 2;");
            return false;
        }
        if (end.isBefore(start) || end.isEqual(start)) {
            showError(endError, "La date de fin doit être après la date de début");
            endPicker.setStyle("-fx-border-color: #E74C3C; -fx-border-width: 2;");
            return false;
        }

        hideError(startError);
        hideError(endError);
        startPicker.setStyle("");
        endPicker.setStyle("");
        return true;
    }

    public static boolean validateComboBox(ComboBox<?> comboBox, Label errorLabel, String fieldName) {
        if (comboBox.getValue() == null) {
            showError(errorLabel, "Veuillez sélectionner " + fieldName);
            comboBox.setStyle("-fx-border-color: #E74C3C; -fx-border-width: 2;");
            return false;
        }
        hideError(errorLabel);
        comboBox.setStyle("");
        return true;
    }

    public static void addRealTimeValidation(TextField field, Label errorLabel, ValidationCallback callback) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            callback.validate(field, errorLabel);
        });
    }

    @FunctionalInterface
    public interface ValidationCallback {
        boolean validate(TextField field, Label errorLabel);
    }
}

