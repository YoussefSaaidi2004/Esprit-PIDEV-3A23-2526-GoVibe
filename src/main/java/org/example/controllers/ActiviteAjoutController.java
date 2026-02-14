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

public class ActiviteAjoutController {

    @FXML
    private BorderPane root;

    @FXML
    private TextField tfName, tfDescription, tfType, tfLocalisation, tfPrix;
    @FXML
    private Label lblMsg;

    private final ServiceActivite service = new ServiceActivite();

    @FXML
    private void ajouter() {
        try {
            if (tfName.getText() == null || tfName.getText().trim().isEmpty()) {
                throw new IllegalArgumentException("Nom obligatoire");
            }

            BigDecimal prix = new BigDecimal(tfPrix.getText().trim());

            Activite a = new Activite(
                    tfName.getText().trim(),
                    tfDescription.getText(),
                    tfType.getText(),
                    tfLocalisation.getText(),
                    prix);

            service.ajouter(a);

            lblMsg.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            lblMsg.setText("✅ Ajout OK (ID=" + a.getId() + ")");

            tfName.clear();
            tfDescription.clear();
            tfType.clear();
            tfLocalisation.clear();
            tfPrix.clear();

        } catch (Exception e) {
            lblMsg.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            lblMsg.setText("❌ " + e.getMessage());
        }
    }

    // ===== NAVIGATION =====
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
}
