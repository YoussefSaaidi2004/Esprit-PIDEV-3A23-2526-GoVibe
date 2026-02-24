package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.entities.Chambre;
import org.example.services.ServiceChambre;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class ChambreController implements Initializable {

    @FXML private TableView<Chambre> tableChambre;
    @FXML private TableColumn<Chambre, Integer> colId;
    @FXML private TableColumn<Chambre, String> colType;
    @FXML private TableColumn<Chambre, Integer> colCapacite;
    @FXML private TableColumn<Chambre, String> colEquipements;
    @FXML private TableColumn<Chambre, Integer> colHotelId;
    @FXML private TableColumn<Chambre, Double> colPrixStandard;
    @FXML private TableColumn<Chambre, Double> colPrixHaute;
    @FXML private TableColumn<Chambre, Double> colPrixBasse;

    @FXML private Button btnAjouter;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;

    private ServiceChambre serviceChambre = new ServiceChambre();
    private ObservableList<Chambre> listChambres = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colCapacite.setCellValueFactory(new PropertyValueFactory<>("capacite"));
        colEquipements.setCellValueFactory(new PropertyValueFactory<>("equipements"));
        colHotelId.setCellValueFactory(new PropertyValueFactory<>("hotelId"));
        colPrixStandard.setCellValueFactory(new PropertyValueFactory<>("prixStandard"));
        colPrixHaute.setCellValueFactory(new PropertyValueFactory<>("prixHauteSaison"));
        colPrixBasse.setCellValueFactory(new PropertyValueFactory<>("prixBasseSaison"));

        loadChambres();

        btnSupprimer.setOnAction(e -> supprimerChambre());
    }

    private void loadChambres() {
        try {
            listChambres.clear();
            listChambres.addAll(serviceChambre.show());
            tableChambre.setItems(listChambres);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void supprimerChambre() {

        Chambre selected = tableChambre.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Erreur", "Veuillez sélectionner une chambre.");
            return;
        }

        try {
            serviceChambre.delete(selected.getId());
            loadChambres();
            showAlert("Succès", "Chambre supprimée avec succès.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
