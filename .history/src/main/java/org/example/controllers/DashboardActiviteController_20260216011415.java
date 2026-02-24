package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.example.entities.Activite;
import org.example.services.ServiceActivite;

import java.math.BigDecimal;

public class DashboardActiviteController {

    @FXML
    private BorderPane root;

    @FXML
    private AdminSidebarController adminSidebarController;

    @FXML
    private TableView<Activite> tableActivite;
    @FXML
    private TableColumn<Activite, Integer> colId;
    @FXML
    private TableColumn<Activite, String> colName;
    @FXML
    private TableColumn<Activite, String> colDesc;
    @FXML
    private TableColumn<Activite, String> colType;
    @FXML
    private TableColumn<Activite, String> colLoc;
    @FXML
    private TableColumn<Activite, BigDecimal> colPrix;
    @FXML
    private TableColumn<Activite, String> colStatus;

    @FXML
    private Label lblCount;
    @FXML
    private Label lblMsg;

    private final ServiceActivite service = new ServiceActivite();
    private final ObservableList<Activite> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Set active page in sidebar
        if (adminSidebarController != null) {
            adminSidebarController.setActivePage("activites");
        }

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colLoc.setCellValueFactory(new PropertyValueFactory<>("localisation"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tableActivite.setItems(data);
        rafraichir();
    }

    @FXML
    private void validerActivite() {
        Activite selected = tableActivite.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle("Attention");
            a.setHeaderText("Aucune activité sélectionnée");
            a.setContentText("Veuillez sélectionner une activité à valider.");
            a.showAndWait();
            return;
        }

        try {
            // 1. Valider en base (passe en 'Confirmed')
            service.valider(selected.getId());

            // 2. Rafraîchir la table pour voir le changement de statut
            rafraichir();

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Succès");
            info.setHeaderText("Activité validée !");
            info.setContentText(
                    "L'activité '" + selected.getName() + "' est maintenant visible pour les utilisateurs.\n" +
                            "Vous allez être redirigé pour ajouter une session.");
            info.showAndWait();

            // 3. Redirection vers Ajout Session avec présélection
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SessionAjout.fxml"));
            Parent p = loader.load();

            SessionAjoutController controller = loader.getController();
            controller.selectActivite(selected);

            Stage stage = (Stage) root.getScene().getWindow();
            stage.setScene(new Scene(p, 1100, 700));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Erreur");
            a.setHeaderText("Erreur lors de la validation");
            a.setContentText(e.getMessage());
            a.showAndWait();
        }
    }

    @FXML
    private void rafraichir() {
        try {
            // On affiche TOUTES les activités (Pending + Confirmed) pour l'admin
            data.setAll(service.getAllAll());
            lblCount.setText("✅ " + data.size() + " activité(s)");
            lblMsg.setText("Liste chargée avec succès.");
        } catch (Exception e) {
            lblCount.setText("❌");
            lblMsg.setText("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= NAVIGATION =================

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

    // ✅ NOUVEAU : aller vers dashboard sessions
    @FXML
    private void openSessionsDashboard() {
        safeSwitchTo("/DashboardSession.fxml");
    }

    // ✅ Retour vers Dashboard Admin
    @FXML
    private void goBackToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/AdminDashboardView.fxml"));
            Parent p = loader.load();
            Stage stage = (Stage) root.getScene().getWindow();
            stage.setScene(new Scene(p));
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ✅ NOUVEAU : Déconnexion
    @FXML
    private void logout() {
        safeSwitchTo("/org/example/LoginView.fxml");
    }

    private void safeSwitchTo(String fxml) {
        try {
            var url = getClass().getResource(fxml);
            if (url == null) {
                throw new IllegalArgumentException("FXML introuvable: " + fxml +
                        "\n➡ Vérifie qu'il est dans src/main/resources");
            }

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