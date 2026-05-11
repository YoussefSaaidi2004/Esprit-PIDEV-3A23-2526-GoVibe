package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import org.example.entities.Activite;
import org.example.services.ServiceActivite;
import org.example.utils.SceneNavigator;

public class ActiviteSuppressionController {

    @FXML
    private TextField tfId, tfName, tfDescription, tfType, tfLocalisation, tfPrix;
    @FXML
    private Label lblMsg;

    private DashboardActiviteController parentController;
    private final ServiceActivite service = new ServiceActivite();

    public void setParentController(DashboardActiviteController parentController) {
        this.parentController = parentController;
    }

    public void initData(Activite a) {
        if (a == null) return;
        tfId.setText(String.valueOf(a.getId()));
        tfName.setText(a.getName());
        tfDescription.setText(a.getDescription());
        tfType.setText(a.getType());
        tfLocalisation.setText(a.getLocalisation());
        tfPrix.setText(a.getPrix() != null ? a.getPrix().toString() : "");
    }

    @FXML
    private void charger() {
        try {
            int id = Integer.parseInt(tfId.getText().trim());
            Activite a = service.getById(id);

            if (a == null) {
                setMsgError("ID introuvable : " + id);
                clearInfo();
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
    private void supprimer() {
        try {
            int id = Integer.parseInt(tfId.getText().trim());

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation de suppression");
            confirm.setHeaderText("Supprimer l'activité ID=" + id);
            confirm.setContentText("Cette action est irréversible. Voulez-vous continuer ?");

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
                return;

            service.supprimerParId(id);

            setMsgOk("✓ Suppression réussie !");
            
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
        clearInfo();
        lblMsg.setText("");
    }

    @FXML
    private void handleRetour() {
        if (parentController != null) {
            parentController.closeModal();
        }
    }

    private void clearInfo() {
        tfName.clear();
        tfDescription.clear();
        tfType.clear();
        tfLocalisation.clear();
        tfPrix.clear();
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