package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.example.entites.Session;
import org.example.services.ServiceSession;

import java.sql.SQLException;

public class SessionSuppressionController {

    @FXML private BorderPane root;

    @FXML private TextField tfId, tfDate, tfHeure, tfCapacite, tfRestant, tfActiviteId;
    @FXML private Label lblMsg;

    private final ServiceSession service = new ServiceSession();

    @FXML
    private void charger() {
        try {
            int id = Integer.parseInt(tfId.getText().trim());

            Session s = findSessionById(id);
            if (s == null) {
                setMsgError("ID introuvable : " + id);
                clearInfo();
                return;
            }

            tfDate.setText(s.getDate() != null ? s.getDate().toString() : "");
            tfHeure.setText(s.getHeure() != null ? s.getHeure().toString() : "");
            tfCapacite.setText(String.valueOf(s.getCapacite()));
            tfRestant.setText(String.valueOf(s.getNbr_places_restant()));
            tfActiviteId.setText(String.valueOf(s.getActivite_id()));

            setMsgOk("✅ Session chargée (ID=" + id + ")");

        } catch (Exception e) {
            setMsgError("❌ " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Session findSessionById(int id) throws SQLException {
        for (Session s : service.getAll()) {
            if (s.getId_session() == id) return s;
        }
        return null;
    }

    @FXML
    private void supprimer() {
        try {
            int id = Integer.parseInt(tfId.getText().trim());

            // Vérifier que la session existe (pour éviter supprimer un ID faux)
            Session s = findSessionById(id);
            if (s == null) {
                setMsgError("ID introuvable : " + id);
                clearInfo();
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText("Supprimer la session ID=" + id);
            confirm.setContentText("Tu es sûr ?");

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

            service.supprimer(id);

            setMsgOk("✅ Suppression OK (ID=" + id + ")");
            vider();

        } catch (Exception e) {
            setMsgError("❌ " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void vider() {
        tfId.clear();
        clearInfo();
        lblMsg.setText("");
    }

    private void clearInfo() {
        tfDate.clear();
        tfHeure.clear();
        tfCapacite.clear();
        tfRestant.clear();
        tfActiviteId.clear();
    }

    // ===== Navigation (Sessions فقط) =====
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