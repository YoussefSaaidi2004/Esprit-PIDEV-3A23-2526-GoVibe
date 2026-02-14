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

import org.example.entites.Session;
import org.example.services.ServiceSession;

import java.sql.Date;
import java.sql.Time;
import java.sql.SQLException;

public class DashboardSessionController {

    @FXML
    private BorderPane root;

    @FXML
    private TableView<Session> tableSession;
    @FXML
    private TableColumn<Session, Integer> colId;
    @FXML
    private TableColumn<Session, Date> colDate;
    @FXML
    private TableColumn<Session, Time> colHeure;
    @FXML
    private TableColumn<Session, Integer> colCapacite;
    @FXML
    private TableColumn<Session, Integer> colRestant;
    @FXML
    private TableColumn<Session, Integer> colActiviteId;

    @FXML
    private Label lblCount;
    @FXML
    private Label lblMsg;

    private final ServiceSession service = new ServiceSession();
    private final ObservableList<Session> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // ✅ propriétés = noms des getters de ton entity Session
        colId.setCellValueFactory(new PropertyValueFactory<>("id_session"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colHeure.setCellValueFactory(new PropertyValueFactory<>("heure"));
        colCapacite.setCellValueFactory(new PropertyValueFactory<>("capacite"));
        colRestant.setCellValueFactory(new PropertyValueFactory<>("nbr_places_restant"));
        colActiviteId.setCellValueFactory(new PropertyValueFactory<>("activite_id"));

        tableSession.setItems(data);
        rafraichir();
    }

    @FXML
    private void rafraichir() {
        try {
            data.setAll(service.getAll());
            lblCount.setText("✅ " + data.size() + " session(s)");
            lblMsg.setText("Liste chargée avec succès.");
        } catch (SQLException e) {
            lblCount.setText("❌");
            lblMsg.setText("Erreur SQL: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            lblCount.setText("❌");
            lblMsg.setText("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= NAVIGATION (SESSIONS SEULEMENT) =================

    @FXML
    private void openDashboardSessions() {
        safeSwitchTo("/DashboardSession.fxml");
    }

    // ✅ NOUVEAU : Retour vers Dashboard Activités
    @FXML
    private void openDashboardActivites() {
        safeSwitchTo("/Dashboard.fxml");
    }

    @FXML
    private void openAjoutSession() {
        safeSwitchTo("/SessionAjout.fxml"); // change si ton fichier a un autre nom
    }

    @FXML
    private void openModifierSession() {
        safeSwitchTo("/SessionModifier.fxml");
    }

    @FXML
    private void openSuppressionSession() {
        safeSwitchTo("/SessionSuppression.fxml");
    }

    // ✅ NOUVEAU : Déconnexion
    @FXML
    private void logout() {
        safeSwitchTo("/RoleSelection.fxml");
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