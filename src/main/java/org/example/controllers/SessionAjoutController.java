package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import org.example.entites.Activite;
import org.example.entites.Session;
import org.example.services.ServiceActivite;
import org.example.services.ServiceSession;

import java.sql.Date;
import java.sql.Time;
import java.sql.SQLException;
import java.util.List;

public class SessionAjoutController {

    @FXML
    private BorderPane root;

    @FXML
    private ComboBox<Activite> cbActivite;
    @FXML
    private TextField tfDate, tfHeure, tfCapacite, tfRestant;
    @FXML
    private Label lblMsg;

    private final ServiceSession serviceSession = new ServiceSession();
    private final ServiceActivite serviceActivite = new ServiceActivite();

    @FXML
    public void initialize() {
        loadActivites();
    }

    private void loadActivites() {
        try {
            List<Activite> list = serviceActivite.getAll(); // ⚠️ si ta méthode s'appelle afficher/readAll change ici
            cbActivite.setItems(FXCollections.observableArrayList(list));

            // Afficher nom + id dans la combo
            cbActivite.setConverter(new StringConverter<>() {
                @Override
                public String toString(Activite a) {
                    return (a == null) ? "" : (a.getName() + " (ID=" + a.getId() + ")");
                }

                @Override
                public Activite fromString(String s) {
                    return null;
                }
            });

        } catch (SQLException e) {
            e.printStackTrace();
            lblMsg.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            lblMsg.setText("❌ Erreur chargement activités: " + e.getMessage());
        }
    }

    @FXML
    private void ajouter() {
        try {
            Activite selected = cbActivite.getValue();
            if (selected == null)
                throw new IllegalArgumentException("Choisis une activité.");

            if (tfDate.getText() == null || tfDate.getText().trim().isEmpty())
                throw new IllegalArgumentException("Date obligatoire (yyyy-mm-dd)");

            if (tfHeure.getText() == null || tfHeure.getText().trim().isEmpty())
                throw new IllegalArgumentException("Heure obligatoire (HH:mm ou HH:mm:ss)");

            int capacite = Integer.parseInt(tfCapacite.getText().trim());
            int restant = Integer.parseInt(tfRestant.getText().trim());

            if (capacite <= 0)
                throw new IllegalArgumentException("Capacité doit être > 0");
            if (restant < 0)
                throw new IllegalArgumentException("Places restantes doit être >= 0");
            if (restant > capacite)
                throw new IllegalArgumentException("Places restantes > capacité");

            Date date;
            try {
                date = Date.valueOf(tfDate.getText().trim()); // yyyy-mm-dd
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Format de date invalide. Utilisez yyyy-mm-dd");
            }

            Time heure;
            try {
                heure = parseTime(tfHeure.getText().trim()); // HH:mm or HH:mm:ss
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Format d'heure invalide. Utilisez HH:mm");
            }

            // ✅ ID automatique depuis l'activité choisie
            int activiteId = selected.getId();

            Session s = new Session(date, heure, capacite, restant, activiteId);
            serviceSession.ajouter(s);

            lblMsg.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            lblMsg.setText("✅ Session ajoutée (ID=" + s.getId_session() + ")");

            tfDate.clear();
            tfHeure.clear();
            tfCapacite.clear();
            tfRestant.clear();

        } catch (Exception e) {
            e.printStackTrace();
            lblMsg.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            lblMsg.setText("❌ " + e.getMessage());
        }
    }

    private Time parseTime(String input) {
        String t = input.trim();
        if (t.matches("^\\d{2}:\\d{2}$"))
            t = t + ":00";
        return Time.valueOf(t);
    }

    // ===== NAVIGATION (sessions فقط) =====
    @FXML
    private void openDashboardSessions() {
        safeSwitchTo("/DashboardSession.fxml");
    }

    @FXML
    private void openAjoutSession() {
        safeSwitchTo("/SessionAjout.fxml");
    }

    @FXML
    private void openModifierSession() {
        safeSwitchTo("/SessionModifier.fxml");
    }

    @FXML
    private void openSuppressionSession() {
        safeSwitchTo("/SessionSuppression.fxml");
    }

    private void safeSwitchTo(String fxml) {
        try {
            var url = getClass().getResource(fxml);
            if (url == null)
                throw new IllegalArgumentException("FXML introuvable: " + fxml);

            Stage stage = (Stage) root.getScene().getWindow();
            Parent p = FXMLLoader.load(url);
            stage.setScene(new Scene(p, 1100, 700));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Erreur");
            a.setHeaderText("Navigation impossible");
            a.setContentText(e.getMessage());
            a.showAndWait();
        }
    }

    public void selectActivite(Activite a) {
        if (a == null)
            return;
        for (Activite item : cbActivite.getItems()) {
            if (item.getId() == a.getId()) {
                cbActivite.setValue(item);
                break;
            }
        }
    }
}