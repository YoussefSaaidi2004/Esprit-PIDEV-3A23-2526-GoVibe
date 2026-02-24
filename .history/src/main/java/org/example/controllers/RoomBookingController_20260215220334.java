package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.entities.Chambre;
import org.example.entities.Hotel;
import org.example.entities.Reservation;
import org.example.services.ServiceChambre;
import org.example.services.ServiceHotel;
import org.example.services.ServiceReservation;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class RoomBookingController implements Initializable {

    @FXML private FlowPane chambreCardsContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterHotelCombo;

    private final ServiceChambre serviceChambre = new ServiceChambre();
    private final ServiceHotel serviceHotel = new ServiceHotel();
    private final ServiceReservation serviceReservation = new ServiceReservation();

    private ObservableList<Chambre> chambreList = FXCollections.observableArrayList();
    private ObservableList<Hotel> hotelList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadHotels();
        loadChambres();
    }

    private void loadHotels() {
        try {
            hotelList.clear();
            hotelList.addAll(serviceHotel.show());
            if (filterHotelCombo != null) {
                filterHotelCombo.getItems().clear();
                filterHotelCombo.getItems().add("Tous les hotels");
                for (Hotel hotel : hotelList) {
                    filterHotelCombo.getItems().add(hotel.getNom());
                }
                filterHotelCombo.setValue("Tous les hotels");
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les hotels: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadChambres() {
        try {
            chambreList.clear();
            chambreList.addAll(serviceChambre.show());
            displayChambres(chambreList);
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les chambres: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void displayChambres(List<Chambre> chambres) {
        chambreCardsContainer.getChildren().clear();
        for (Chambre chambre : chambres) {
            chambreCardsContainer.getChildren().add(createChambreCard(chambre));
        }
    }

    private VBox createChambreCard(Chambre chambre) {
        VBox card = new VBox(12);
        card.setPrefWidth(300);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                "-fx-effect: dropshadow(gaussian, rgba(1,50,32,0.15), 15, 0, 0, 5); " +
                "-fx-padding: 20; -fx-cursor: hand;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label typeLabel = new Label(chambre.getType());
        typeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #013220;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label capacityLabel = new Label(chambre.getCapacite() + " pers");
        capacityLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #0B6E4F; -fx-font-weight: bold;");

        header.getChildren().addAll(typeLabel, spacer, capacityLabel);

        Label hotelLabel = new Label("Hotel: " + getHotelName(chambre.getHotelId()));
        hotelLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        String equipements = chambre.getEquipements() != null ? chambre.getEquipements() : "";
        Label equipLabel = new Label(equipements);
        equipLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555; -fx-wrap-text: true;");
        equipLabel.setWrapText(true);
        equipLabel.setMaxWidth(260);

        VBox pricesBox = new VBox(6);
        pricesBox.setStyle("-fx-background-color: #D1F2EB; -fx-background-radius: 10; -fx-padding: 12;");

        Label priceTitle = new Label("Tarif standard");
        priceTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #013220;");

        Label standardPrice = new Label(String.format("%.2f DT / nuit", chambre.getPrixStandard()));
        standardPrice.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #0B6E4F;");

        pricesBox.getChildren().addAll(priceTitle, standardPrice);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #E0E0E0;");

        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER);

        Button reserveBtn = new Button("Reserver");
        reserveBtn.setStyle("-fx-background-color: #50C878; -fx-text-fill: white; -fx-background-radius: 8; " +
                "-fx-padding: 8 16; -fx-font-size: 12px; -fx-cursor: hand; -fx-font-weight: bold;");
        reserveBtn.setOnAction(e -> reserveChambre(chambre));

        actionButtons.getChildren().add(reserveBtn);

        card.getChildren().addAll(header, hotelLabel, equipLabel, pricesBox, sep, actionButtons);
        return card;
    }

    private void reserveChambre(Chambre chambre) {
        if (SessionManager.getCurrentUser() == null) {
            showAlert("Connexion requise", "Veuillez vous connecter pour reserver une chambre.", Alert.AlertType.WARNING);
            return;
        }

        Dialog<Reservation> dialog = new Dialog<>();
        dialog.setTitle("Reservation Chambre");
        dialog.setHeaderText("Nouvelle reservation");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #F5F3E7;");

        ButtonType saveButtonType = new ButtonType("Reserver", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10, 20, 10, 20));

        DatePicker startDate = new DatePicker(LocalDate.now().plusDays(1));
        DatePicker endDate = new DatePicker(LocalDate.now().plusDays(2));
        form.add(new Label("Date debut"), 0, 0);
        form.add(startDate, 1, 0);
        form.add(new Label("Date fin"), 0, 1);
        form.add(endDate, 1, 1);

        dialog.getDialogPane().setContent(form);

        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            Reservation reservation = buildReservation(startDate, endDate, chambre);
            if (reservation == null) {
                event.consume();
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return buildReservation(startDate, endDate, chambre);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(reservation -> {
            try {
                serviceReservation.insert(reservation);
                showAlert("Succes", "Reservation creee avec succes.", Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                showAlert("Erreur", "Erreur lors de la reservation: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private Reservation buildReservation(DatePicker startDate, DatePicker endDate, Chambre chambre) {
        LocalDate start = startDate.getValue();
        LocalDate end = endDate.getValue();

        if (start == null || end == null) {
            showAlert("Validation", "Veuillez remplir tous les champs obligatoires.", Alert.AlertType.WARNING);
            return null;
        }
        long nights = ChronoUnit.DAYS.between(start, end);
        if (nights <= 0) {
            showAlert("Validation", "La date de fin doit etre apres la date de debut.", Alert.AlertType.WARNING);
            return null;
        }

        double totalPrice = nights * chambre.getPrixStandard();
        int userId = SessionManager.getCurrentUser().getId();
        return new Reservation(
            userId,
                chambre.getId(),
                chambre.getHotelId(),
                start,
                end,
                totalPrice,
                "EN_ATTENTE"
        );
    }

    private String getHotelName(int hotelId) {
        for (Hotel h : hotelList) {
            if (h.getId() == hotelId) {
                return h.getNom();
            }
        }
        return "Hotel inconnu";
    }

    @FXML
    public void filterByHotel() {
        applyFilters();
    }

    @FXML
    public void searchChambres() {
        applyFilters();
    }

    private void applyFilters() {
        String filterText = searchField != null && searchField.getText() != null
                ? searchField.getText().trim().toLowerCase() : "";
        String hotelFilter = filterHotelCombo != null ? filterHotelCombo.getValue() : "Tous les hotels";

        List<Chambre> filtered = chambreList.stream()
            .filter(chambre -> filterText.isEmpty() ||
                safeLower(chambre.getType()).contains(filterText) ||
                safeLower(chambre.getEquipements()).contains(filterText) ||
                safeLower(getHotelName(chambre.getHotelId())).contains(filterText))
                .filter(chambre -> "Tous les hotels".equals(hotelFilter) ||
                        getHotelName(chambre.getHotelId()).equals(hotelFilter))
                .collect(Collectors.toList());

        displayChambres(filtered);
    }

    @FXML
    private void handleLogout() {
        SessionManager.clear();
        org.example.mains.MainApp.switchScene("/org/example/LoginView.fxml", "GoVibe Connexion");
    }

    // ==================== NAVIGATION METHODS ====================

    @FXML
    private void handleGoHome() {
        SceneNavigator.switchTo("/UserHome.fxml", chambreCardsContainer);
    }

    @FXML
    private void handleGoActivities() {
        SceneNavigator.switchTo("/UserHome.fxml", chambreCardsContainer);
    }

    @FXML
    private void handleGoLocations() {
        SceneNavigator.switchTo("/LocationListView.fxml", chambreCardsContainer);
    }

    @FXML
    private void handleGoFlights() {
        org.example.mains.MainApp.switchScene("/views/user-dashboard.fxml", "Vols");
    }

    @FXML
    private void handleGoHotels() {
        loadChambres();
    }

    @FXML
    private void handleGoForums() {
        SceneNavigator.switchTo("/poste-forumviews/ListForum.fxml", chambreCardsContainer);
    }

    @FXML
    private void handleHome() {
        SceneNavigator.switchTo("/UserHome.fxml", chambreCardsContainer);
    }

    @FXML
    private void handleFlights() {
        org.example.mains.MainApp.switchScene("/views/user-dashboard.fxml", "Vols");
    }

    @FXML
    private void handleLocations() {
        SceneNavigator.switchTo("/LocationListView.fxml", chambreCardsContainer);
    }

    @FXML
    private void handleChambres() {
        loadChambres();
    }

    @FXML
    private void handleActivities() {
        SceneNavigator.switchTo("/UserHome.fxml", chambreCardsContainer);
    }

    @FXML
    private void handleForums() {
        SceneNavigator.switchTo("/poste-forumviews/ListForum.fxml", chambreCardsContainer);
    }

    @FXML
    private void handleProfile() {
        showAlert("Profil", "Fonctionnalité à venir : Gestion du profil utilisateur", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleMyReservations() {
        org.example.mains.MainApp.switchScene("/views/room-booking.fxml", "Mes Réservations");
    }

    @FXML
    private void handleMyLocations() {
        SceneNavigator.switchTo("/LocationListView.fxml", chambreCardsContainer);
    }

    @FXML
    private void handleMyFlights() {
        org.example.mains.MainApp.switchScene("/views/user-dashboard.fxml", "Mes Vols");
    }

    @FXML
    private void handleSettings() {
        showAlert("Paramètres", "Fonctionnalité à venir : Paramètres utilisateur", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleHelp() {
        showAlert("Aide", "Besoin d'aide ? Contactez-nous à support@govibe.tn", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleResetFilters() {
        if (searchField != null) {
            searchField.clear();
        }
        if (filterHotelCombo != null) {
            filterHotelCombo.setValue("Tous les hotels");
        }
        loadChambres();
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
