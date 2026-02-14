package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.entities.Hotel;
import org.example.services.ServiceHotel;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class HotelController implements Initializable {

    @FXML private TableView<Hotel> hotelTable;
    @FXML private TableColumn<Hotel, Integer> idColumn;
    @FXML private TableColumn<Hotel, String> nomColumn;
    @FXML private TableColumn<Hotel, String> adresseColumn;
    @FXML private TableColumn<Hotel, String> villeColumn;
    @FXML private TableColumn<Hotel, Integer> etoilesColumn;
    @FXML private TableColumn<Hotel, Double> budgetColumn;

    private ServiceHotel serviceHotel;
    private ObservableList<Hotel> hotelList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        serviceHotel = new ServiceHotel();
        hotelList = FXCollections.observableArrayList();

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        adresseColumn.setCellValueFactory(new PropertyValueFactory<>("adresse"));
        villeColumn.setCellValueFactory(new PropertyValueFactory<>("ville"));
        etoilesColumn.setCellValueFactory(new PropertyValueFactory<>("nombreEtoiles"));
        budgetColumn.setCellValueFactory(new PropertyValueFactory<>("budget"));

        loadHotels();
    }

    private void loadHotels() {
        try {
            hotelList.clear();
            hotelList.addAll(serviceHotel.show());
            hotelTable.setItems(hotelList);
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    @FXML
    public void ajouterHotel() {
        try {
            Hotel hotel = new Hotel(
                    "Test Hotel",
                    "Adresse Test",
                    "Tunis",
                    4,
                    "Description test",
                    "photo.jpg",
                    200.0
            );

            serviceHotel.insert(hotel);
            loadHotels();
            showAlert("Succès", "Hôtel ajouté !");
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    @FXML
    public void supprimerHotel() {

        Hotel selected = hotelTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Attention", "Sélectionnez un hôtel !");
            return;
        }

        try {
            serviceHotel.delete(selected.getId());
            loadHotels();
            showAlert("Succès", "Hôtel supprimé !");
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    @FXML
    public void deconnexion() {
        System.out.println("Déconnexion...");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}


