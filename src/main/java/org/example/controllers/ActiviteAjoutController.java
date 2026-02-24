package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import org.example.entities.Activite;
import org.example.services.ServiceActivite;
import org.example.utils.SceneNavigator;

import java.math.BigDecimal;

public class ActiviteAjoutController {

    @FXML
    private TextField tfName, tfDescription, tfType, tfLocalisation, tfPrix;
    @FXML
    private Label lblMsg;

    private DashboardActiviteController parentController;
    private final ServiceActivite service = new ServiceActivite();

    public void setParentController(DashboardActiviteController parentController) {
        this.parentController = parentController;
    }

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

            lblMsg.setStyle("-fx-text-fill: #50C878; -fx-font-weight: bold;");
            lblMsg.setText("✓ Activité publiée avec succès !");

            // Close modal after success
            if (parentController != null) {
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
                pause.setOnFinished(e -> parentController.closeModal());
                pause.play();
            }

        } catch (Exception e) {
            lblMsg.setStyle("-fx-text-fill: #ef476f; -fx-font-weight: bold;");
            lblMsg.setText("⚠ Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void handleRetour() {
        if (parentController != null) {
            parentController.closeModal();
        }
    }
}
