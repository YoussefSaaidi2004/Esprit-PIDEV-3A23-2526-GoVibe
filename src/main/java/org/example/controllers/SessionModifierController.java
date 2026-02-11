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
import java.sql.SQLException;
import java.sql.Time;
import java.util.List;

public class SessionModifierController {

    @FXML private BorderPane root;

    @FXML private TextField tfId, tfDate, tfHeure, tfCapacite, tfRestant;
    @FXML private ComboBox<Activite> cbActivite;
    @FXML private Label lblMsg;

    private final ServiceSession serviceSession = new ServiceSession();
    private final ServiceActivite serviceActivite = new ServiceActivite();

    @FXML
    public void initialize() {
        loadActivites();
    }

    private void loadActivites() {
        try {
            List<Activite> list = serviceActivite.getAll(); // throws SQLException
            cbActivite.setItems(FXCollections.observableArrayList(list));

            cbActivite.setConverter(new StringConverter<>() {
                @Override public String toString(Activite a) {
                    return (a == null) ? "" : (a.getName() + " (ID=" + a.getId() + ")");
                }
                @Override public Activite fromString(String s) { return null; }
            });

        } catch (SQLException e) {
            e.printStackTrace();
            setMsgError("❌ Erreur chargement activités: " + e.getMessage());
        }
    }

    @FXML
    private void charger() {
        try {
            int id = Integer.parseInt(tfId.getText().trim());

            // ✅ ServiceSession n’a pas getById → on cherche dans getAll()
            Session s = findSessionById(id);
            if (s == null) {
                setMsgError("ID Session introuvable : " + id);
                return;
            }

            tfDate.setText(s.getDate() != null ? s.getDate().toString() : "");
            tfHeure.setText(s.getHeure() != null ? s.getHeure().toString() : "");
            tfCapacite.setText(String.valueOf(s.getCapacite()));
            tfRestant.setText(String.valueOf(s.getNbr_places_restant()));

            // ✅ sélectionner automatiquement l'activité liée
            preselectActiviteById(s.getActivite_id());

            setMsgOk("✅ Session chargée (ID=" + id + ")");
        } catch (Exception e) {
            e.printStackTrace();
            setMsgError("❌ " + e.getMessage());
        }
    }

    private Session findSessionById(int id) throws SQLException {
        for (Session s : serviceSession.getAll()) {
            if (s.getId_session() == id) return s;
        }
        return null;
    }

    private void preselectActiviteById(int activiteId) {
        if (cbActivite.getItems() == null) return;
        cbActivite.getItems().stream()
                .filter(a -> a.getId() == activiteId)
                .findFirst()
                .ifPresent(a -> cbActivite.getSelectionModel().select(a));
    }

    @FXML
    private void modifier() {
        try {
            int id = Integer.parseInt(tfId.getText().trim());

            Activite selected = cbActivite.getValue();
            if (selected == null) throw new IllegalArgumentException("Choisis une activité.");

            Date date = Date.valueOf(tfDate.getText().trim());  // yyyy-mm-dd
            Time heure = parseTime(tfHeure.getText().trim());   // HH:mm ou HH:mm:ss

            int capacite = Integer.parseInt(tfCapacite.getText().trim());
            int restant  = Integer.parseInt(tfRestant.getText().trim());

            if (capacite <= 0) throw new IllegalArgumentException("Capacité doit être > 0");
            if (restant < 0) throw new IllegalArgumentException("Places restantes doit être >= 0");
            if (restant > capacite) throw new IllegalArgumentException("Places restantes > capacité");

            // ✅ construire la session + modifier via service.modifier(s)
            Session s = new Session(id, date, heure, capacite, restant, selected.getId());
            serviceSession.modifier(s);

            setMsgOk("✅ Modification OK (ID=" + id + ")");
        } catch (Exception e) {
            e.printStackTrace();
            setMsgError("❌ " + e.getMessage());
        }
    }

    private Time parseTime(String input) {
        String t = input.trim();
        if (t.matches("^\\d{2}:\\d{2}$")) t = t + ":00";
        return Time.valueOf(t);
    }

    @FXML
    private void vider() {
        tfId.clear();
        tfDate.clear();
        tfHeure.clear();
        tfCapacite.clear();
        tfRestant.clear();
        cbActivite.getSelectionModel().clearSelection();
        lblMsg.setText("");
    }

    // ===== NAVIGATION (sessions only) =====
    @FXML private void openDashboardSessions() { safeSwitchTo("/DashboardSession.fxml"); }
    @FXML private void openAjoutSession() { safeSwitchTo("/SessionAjout.fxml"); }
    @FXML private void openModifierSession() { safeSwitchTo("/SessionModifier.fxml"); }
    @FXML private void openSuppressionSession() { safeSwitchTo("/SessionSuppression.fxml"); }

    private void safeSwitchTo(String fxml) {
        try {
            var url = getClass().getResource(fxml);
            if (url == null) throw new IllegalArgumentException("FXML introuvable: " + fxml);

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

    private void setMsgOk(String msg) {
        lblMsg.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        lblMsg.setText(msg);
    }

    private void setMsgError(String msg) {
        lblMsg.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        lblMsg.setText(msg);
    }
}