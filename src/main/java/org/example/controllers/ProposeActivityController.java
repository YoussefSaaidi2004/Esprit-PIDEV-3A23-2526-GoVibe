package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.entites.Activite;
import org.example.services.ServiceActivite;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;

public class ProposeActivityController {

    @FXML
    private TextField txtName;
    @FXML
    private TextField txtType;
    @FXML
    private javafx.scene.control.ComboBox<String> cmbLoc; // ✅ Changed to ComboBox
    @FXML
    private TextField txtPrix;
    @FXML
    private TextArea txtDesc;

    private final ServiceActivite serviceActivite = new ServiceActivite();

    @FXML
    public void initialize() {
        // ✅ Populate locations matching resources
        cmbLoc.getItems().addAll(
                "Tunis (Medina)",
                "Nabeul",
                "Sidi Bou Said",
                "Ghar El Melh",
                "Cap Bon",
                "Degustation Huile d'Olive");
    }

    @FXML
    private void submitProposal(ActionEvent event) {
        String name = txtName.getText();
        String type = txtType.getText();
        String loc = cmbLoc.getValue(); // ✅ Get value from ComboBox
        String prixStr = txtPrix.getText();
        String desc = txtDesc.getText();

        if (name == null || name.isEmpty() || type.isEmpty() || loc == null || loc.isEmpty() || prixStr.isEmpty()
                || desc.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champs manquants", "Veuillez remplir tous les champs.");
            return;
        }

        try {
            BigDecimal prix = new BigDecimal(prixStr);

            Activite proposal = new Activite(name, desc, type, loc, prix, Activite.STATUS_PENDING);
            serviceActivite.ajouter(proposal);

            showAlert(Alert.AlertType.INFORMATION, "Succès",
                    "Votre activité a été soumise avec succès !\nElle doit être validée par un administrateur.");
            returnToUserHome(event);

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur format", "Le prix doit être un nombre valide.");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur Base de Données", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    private void cancel(ActionEvent event) {
        returnToUserHome(event);
    }

    private void returnToUserHome(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UserHome.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 700));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
