package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import org.example.entities.Activite;
import org.example.services.ServiceActivite;
import org.example.utils.SceneNavigator;

import java.math.BigDecimal;

public class ActiviteModifierController {

    @FXML
    private TextField tfId, tfName, tfDescription, tfType, tfLocalisation, tfPrix;
    @FXML
    private Label lblMsg;

    private DashboardActiviteController parentController;
    private final ServiceActivite service = new ServiceActivite();

    public void setParentController(DashboardActiviteController parentController) {
        this.parentController = parentController;
    }

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

            setMsgOk("Activité chargée avec succès");
        } catch (Exception e) {
            setMsgError("Erreur: " + e.getMessage());
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

            setMsgOk("✓ Modifications enregistrées !");

            // Close modal after success
            if (parentController != null) {
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
                pause.setOnFinished(e -> parentController.closeModal());
                pause.play();
            }
        } catch (Exception e) {
            setMsgError("Erreur: " + e.getMessage());
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

    @FXML
    private void handleRetour() {
        if (parentController != null) {
            parentController.closeModal();
        }
    }

    private void setMsgOk(String msg) {
        lblMsg.setStyle("-fx-text-fill: #50C878; -fx-font-weight: bold;");
        lblMsg.setText(msg);
    }

    private void setMsgError(String msg) {
        lblMsg.setStyle("-fx-text-fill: #ef476f; -fx-font-weight: bold;");
        lblMsg.setText(msg);
    }
}