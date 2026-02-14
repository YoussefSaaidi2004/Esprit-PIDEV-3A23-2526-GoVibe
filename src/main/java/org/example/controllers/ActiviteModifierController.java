package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.example.entites.Activite;
import org.example.services.ServiceActivite;

import java.math.BigDecimal;

public class ActiviteModifierController {

    @FXML
    private BorderPane root;

    @FXML
    private TextField tfId, tfName, tfDescription, tfType, tfLocalisation, tfPrix;
    @FXML
    private Label lblMsg;

    private final ServiceActivite service = new ServiceActivite();

    @FXML
    private void charger() {
        try {
            int id = Integer.parseInt(tfId.getText().trim());
            Activite a = service.getById(id);

            if (a == null) {
                setMsgError("ID introuvable : " + id);
                return;
            }

            tfName.setText(a.getName());
            tfDescription.setText(a.getDescription());
            tfType.setText(a.getType());
            tfLocalisation.setText(a.getLocalisation());
            tfPrix.setText(a.getPrix() != null ? a.getPrix().toString() : "");

            setMsgOk("✅ Activité chargée (ID=" + id + ")");
        } catch (Exception e) {
            setMsgError("❌ " + e.getMessage());
        }
    }

    @FXML
    private void modifier() {
        try {
            int id = Integer.parseInt(tfId.getText().trim());

            if (tfName.getText() == null || tfName.getText().trim().isEmpty())
                throw new IllegalArgumentException("Le nom est obligatoire.");

            BigDecimal prix = new BigDecimal(tfPrix.getText().trim());

            service.modifierParId(
                    id,
                    tfName.getText().trim(),
                    tfDescription.getText(),
                    tfType.getText(),
                    tfLocalisation.getText(),
                    prix);

            setMsgOk("✅ Modification OK (ID=" + id + ")");
        } catch (Exception e) {
            setMsgError("❌ " + e.getMessage());
        }
    }

    @FXML
    private void vider() {
        tfId.clear();
        tfName.clear();
        tfDescription.clear();
        tfType.clear();
        tfLocalisation.clear();
        tfPrix.clear();
        lblMsg.setText("");
    }

    // ===== NAVIGATION (obligatoire car onAction existe dans le FXML) =====
    @FXML
    private void openDashboard() {
        safeSwitchTo("/Dashboard.fxml");
    }

    @FXML
    private void openAjout() {
        safeSwitchTo("/ActiviteAjout.fxml");
    }

    @FXML
    private void openModifier() {
        safeSwitchTo("/ActiviteModifier.fxml");
    }

    @FXML
    private void openSuppression() {
        safeSwitchTo("/ActiviteSuppression.fxml");
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

    private void setMsgOk(String msg) {
        lblMsg.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        lblMsg.setText(msg);
    }

    private void setMsgError(String msg) {
        lblMsg.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        lblMsg.setText(msg);
    }
}