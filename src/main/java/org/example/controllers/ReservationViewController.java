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
import org.example.entities.personne;
import org.example.services.ServiceChambre;
import org.example.services.ServiceHotel;
import org.example.services.ServicePersonne;
import org.example.services.ServiceReservation;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class ReservationViewController implements Initializable {

    @FXML private FlowPane reservationCardsContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatutCombo;

    private ServiceReservation serviceReservation;
    private ServiceHotel serviceHotel;
    private ServiceChambre serviceChambre;
    private ServicePersonne servicePersonne;
    private ObservableList<Reservation> reservationList;
    private ObservableList<Hotel> hotelList;
    private ObservableList<Chambre> chambreList;
    private ObservableList<personne> userList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        serviceReservation = new ServiceReservation();
        serviceHotel = new ServiceHotel();
        serviceChambre = new ServiceChambre();
        servicePersonne = new ServicePersonne();
        reservationList = FXCollections.observableArrayList();
        hotelList = FXCollections.observableArrayList();
        chambreList = FXCollections.observableArrayList();
        userList = FXCollections.observableArrayList();

        setupFilterStatut();
        loadData();
        loadReservations();
    }

    private void setupFilterStatut() {
        if (filterStatutCombo != null) {
            filterStatutCombo.getItems().addAll("Tous", "EN_ATTENTE", "CONFIRMEE", "ANNULEE");
            filterStatutCombo.setValue("Tous");
        }
    }

    private void loadData() {
        try {
            hotelList.clear();
            hotelList.addAll(serviceHotel.show());
            chambreList.clear();
            chambreList.addAll(serviceChambre.show());
            userList.clear();
            userList.addAll(servicePersonne.getAll());
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du chargement des données: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadReservations() {
        try {
            reservationList.clear();
            reservationList.addAll(serviceReservation.show());
            displayReservationCards();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les réservations: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void displayReservationCards() {
        reservationCardsContainer.getChildren().clear();

        for (Reservation reservation : reservationList) {
            VBox card = createReservationCard(reservation);
            reservationCardsContainer.getChildren().add(card);
        }
    }

    private VBox createReservationCard(Reservation reservation) {
        VBox card = new VBox(12);
        card.setPrefWidth(340);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                     "-fx-effect: dropshadow(gaussian, rgba(1,50,32,0.15), 15, 0, 0, 5); " +
                     "-fx-padding: 20; -fx-cursor: hand;");

        // Status badge
        HBox statusBox = new HBox();
        statusBox.setAlignment(Pos.TOP_RIGHT);
        Label statusLabel = new Label(reservation.getStatut());
        String statusColor = getStatusColor(reservation.getStatut());
        statusLabel.setStyle("-fx-background-color: " + statusColor + "; -fx-text-fill: white; " +
                            "-fx-padding: 5 12; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
        statusBox.getChildren().add(statusLabel);

        // Client info
        Label clientLabel = new Label("👤 " + reservation.getUserDisplayName());
        clientLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #013220;");

        String emailValue = reservation.getUserEmail() != null ? reservation.getUserEmail() : "Email inconnu";
        Label emailLabel = new Label("📧 " + emailValue);
        emailLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background-color: #E0E0E0;");

        // Hotel & Chambre info
        String hotelName = getHotelName(reservation.getHotelId());
        String chambreType = getChambreType(reservation.getChambreId());

        Label hotelLabel = new Label("🏨 " + hotelName);
        hotelLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #0B6E4F; -fx-font-weight: bold;");

        Label chambreLabel = new Label("🛏️ Chambre: " + chambreType);
        chambreLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

        // Dates
        VBox datesBox = new VBox(6);
        datesBox.setStyle("-fx-background-color: #D1F2EB; -fx-background-radius: 10; -fx-padding: 12;");

        Label datesTitle = new Label("📅 Période de séjour");
        datesTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #013220;");

        HBox dateRangeBox = new HBox(8);
        dateRangeBox.setAlignment(Pos.CENTER_LEFT);
        Label dateDebut = new Label(reservation.getDateDebut().toString());
        dateDebut.setStyle("-fx-font-size: 12px; -fx-text-fill: #0B6E4F; -fx-font-weight: bold;");
        Label arrow = new Label("→");
        arrow.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        Label dateFin = new Label(reservation.getDateFin().toString());
        dateFin.setStyle("-fx-font-size: 12px; -fx-text-fill: #0B6E4F; -fx-font-weight: bold;");
        dateRangeBox.getChildren().addAll(dateDebut, arrow, dateFin);

        long nights = ChronoUnit.DAYS.between(reservation.getDateDebut(), reservation.getDateFin());
        Label nightsLabel = new Label(nights + " nuit" + (nights > 1 ? "s" : ""));
        nightsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        datesBox.getChildren().addAll(datesTitle, dateRangeBox, nightsLabel);

        // Price
        HBox priceBox = new HBox(8);
        priceBox.setAlignment(Pos.CENTER_LEFT);
        priceBox.setStyle("-fx-background-color: #F5F3E7; -fx-background-radius: 8; -fx-padding: 10;");

        Label priceLabel = new Label("💰 Prix Total:");
        priceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #013220; -fx-font-weight: bold;");

        Label priceValue = new Label(String.format("%.2f DT", reservation.getPrixTotal()));
        priceValue.setStyle("-fx-font-size: 16px; -fx-text-fill: #0B6E4F; -fx-font-weight: bold;");

        priceBox.getChildren().addAll(priceLabel, priceValue);

        Separator sep2 = new Separator();
        sep2.setStyle("-fx-background-color: #E0E0E0;");

        // Action buttons
        HBox actionButtons = new HBox(8);
        actionButtons.setAlignment(Pos.CENTER);

        Button editBtn = new Button("✏️ Modifier");
        editBtn.setStyle("-fx-background-color: #50C878; -fx-text-fill: white; -fx-background-radius: 8; " +
                        "-fx-padding: 8 14; -fx-font-size: 11px; -fx-cursor: hand; -fx-font-weight: bold;");
        editBtn.setOnAction(e -> editReservation(reservation));

        Button cancelBtn = new Button("❌ Annuler");
        cancelBtn.setStyle("-fx-background-color: #D84E36; -fx-text-fill: white; -fx-background-radius: 8; " +
                          "-fx-padding: 8 14; -fx-font-size: 11px; -fx-cursor: hand; -fx-font-weight: bold;");
        cancelBtn.setOnAction(e -> cancelReservation(reservation));

        Button deleteBtn = new Button("🗑️");
        deleteBtn.setStyle("-fx-background-color: #666; -fx-text-fill: white; -fx-background-radius: 8; " +
                          "-fx-padding: 8 12; -fx-font-size: 11px; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> deleteReservation(reservation));

        actionButtons.getChildren().addAll(editBtn, cancelBtn, deleteBtn);

        card.getChildren().addAll(statusBox, clientLabel, emailLabel, sep1,
                                   hotelLabel, chambreLabel, datesBox, priceBox, sep2, actionButtons);

        return card;
    }

    private String getStatusColor(String statut) {
        switch (statut) {
            case "CONFIRMEE":
                return "#50C878";
            case "ANNULEE":
                return "#D84E36";
            default:
                return "#FFA500";
        }
    }

    private String getHotelName(int hotelId) {
        for (Hotel h : hotelList) {
            if (h.getId() == hotelId) {
                return h.getNom();
            }
        }
        return "Hôtel inconnu";
    }

    private String getChambreType(int chambreId) {
        for (Chambre c : chambreList) {
            if (c.getId() == chambreId) {
                return c.getType();
            }
        }
        return "Chambre inconnue";
    }

    @FXML
    public void addReservation() {
        Dialog<Reservation> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle Réservation");
        dialog.setHeaderText("✨ Créer une réservation client");

        // Style the dialog
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles/unified-styles.css").toExternalForm());
        dialogPane.getStyleClass().add("form-dialog");

        ButtonType saveButtonType = new ButtonType("💾 Réserver", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("❌ Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        // Style buttons
        Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
        saveButton.getStyleClass().add("form-button-primary");
        Button cancelButton = (Button) dialogPane.lookupButton(cancelButtonType);
        cancelButton.getStyleClass().add("form-button-secondary");

        ScrollPane form = createReservationForm(null);
        dialog.getDialogPane().setContent(form);

        // Prevent closing on validation error
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            Reservation reservation = getReservationFromForm(form, null);
            if (reservation == null) {
                event.consume();
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return getReservationFromForm(form, null);
            }
            return null;
        });

        Optional<Reservation> result = dialog.showAndWait();
        result.ifPresent(reservation -> {
            try {
                serviceReservation.insert(reservation);
                loadReservations();
                showAlert("Succès", "Réservation créée avec succès!", Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                showAlert("Erreur", "Erreur lors de la création: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void editReservation(Reservation reservation) {
        Dialog<Reservation> dialog = new Dialog<>();
        dialog.setTitle("Modifier la Réservation");
        dialog.setHeaderText("✏️ Modifier: Réservation #" + reservation.getId());

        // Style the dialog
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles/unified-styles.css").toExternalForm());
        dialogPane.getStyleClass().add("form-dialog");

        ButtonType saveButtonType = new ButtonType("💾 Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("❌ Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        // Style buttons
        Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
        saveButton.getStyleClass().add("form-button-primary");
        Button cancelButton = (Button) dialogPane.lookupButton(cancelButtonType);
        cancelButton.getStyleClass().add("form-button-secondary");

        ScrollPane form = createReservationForm(reservation);
        dialog.getDialogPane().setContent(form);

        // Prevent closing on validation error
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            Reservation updatedReservation = getReservationFromForm(form, reservation);
            if (updatedReservation == null) {
                event.consume();
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return getReservationFromForm(form, reservation);
            }
            return null;
        });

        Optional<Reservation> result = dialog.showAndWait();
        result.ifPresent(updatedReservation -> {
            try {
                serviceReservation.update(updatedReservation);
                loadReservations();
                showAlert("Succès", "Réservation modifiée avec succès!", Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                showAlert("Erreur", "Erreur lors de la modification: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void cancelReservation(Reservation reservation) {
        reservation.setStatut("ANNULEE");
        try {
            serviceReservation.update(reservation);
            loadReservations();
            showAlert("Info", "Réservation annulée", Alert.AlertType.INFORMATION);
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void deleteReservation(Reservation reservation) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer la réservation");
        confirmation.setContentText("Êtes-vous sûr de vouloir supprimer cette réservation ?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                serviceReservation.delete(reservation.getId());
                loadReservations();
                showAlert("Succès", "Réservation supprimée!", Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                showAlert("Erreur", "Erreur: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private ScrollPane createReservationForm(Reservation reservation) {
        VBox container = new VBox(15);
        container.setPadding(new Insets(25));
        container.setPrefWidth(500);
        container.getStyleClass().add("form-card");

        Label titleLabel = new Label("Détails de la réservation");
        titleLabel.getStyleClass().add("form-title");

        // User
        VBox userBox = new VBox(5);
        Label userLabel = new Label("Utilisateur *");
        userLabel.getStyleClass().add("form-label");
        ComboBox<String> userCombo = new ComboBox<>();
        userCombo.setPromptText("Sélectionnez un utilisateur");
        for (personne user : userList) {
            userCombo.getItems().add(formatUserOption(user));
        }
        if (reservation != null) {
            userCombo.setValue(findUserOption(reservation.getUserId()));
        }
        userCombo.setId("userCombo");
        userCombo.getStyleClass().add("form-field");
        userCombo.setPrefWidth(500);
        Label userError = org.example.utils.FormValidator.createErrorLabel();
        userError.setId("userError");
        userBox.getChildren().addAll(userLabel, userCombo, userError);

        // Hotel
        VBox hotelBox = new VBox(5);
        Label hotelLabel = new Label("Hôtel *");
        hotelLabel.getStyleClass().add("form-label");
        ComboBox<String> hotelCombo = new ComboBox<>();
        hotelCombo.setPromptText("Sélectionnez un hôtel");
        for (Hotel h : hotelList) {
            hotelCombo.getItems().add(h.getId() + " - " + h.getNom());
        }
        if (reservation != null) {
            hotelCombo.setValue(reservation.getHotelId() + " - " + getHotelName(reservation.getHotelId()));
        }
        hotelCombo.setId("hotelCombo");
        hotelCombo.getStyleClass().add("form-field");
        hotelCombo.setPrefWidth(500);
        Label hotelError = org.example.utils.FormValidator.createErrorLabel();
        hotelError.setId("hotelError");
        hotelBox.getChildren().addAll(hotelLabel, hotelCombo, hotelError);

        // Chambre
        VBox chambreBox = new VBox(5);
        Label chambreLabel = new Label("Chambre *");
        chambreLabel.getStyleClass().add("form-label");
        ComboBox<String> chambreCombo = new ComboBox<>();
        chambreCombo.setPromptText("Sélectionnez d'abord un hôtel");

        // Load chambres when hotel is selected
        hotelCombo.setOnAction(e -> {
            if (hotelCombo.getValue() != null) {
                int hotelId = Integer.parseInt(hotelCombo.getValue().split(" - ")[0]);
                chambreCombo.getItems().clear();
                for (Chambre c : chambreList) {
                    if (c.getHotelId() == hotelId) {
                        chambreCombo.getItems().add(c.getId() + " - " + c.getType() + " (" + c.getPrixStandard() + " DT)");
                    }
                }
                chambreCombo.setPromptText("Sélectionnez une chambre");
            }
        });

        if (reservation != null) {
            int hotelId = reservation.getHotelId();
            for (Chambre c : chambreList) {
                if (c.getHotelId() == hotelId) {
                    chambreCombo.getItems().add(c.getId() + " - " + c.getType() + " (" + c.getPrixStandard() + " DT)");
                }
            }
            chambreCombo.setValue(reservation.getChambreId() + " - " + getChambreType(reservation.getChambreId()));
        }
        chambreCombo.setId("chambreCombo");
        chambreCombo.getStyleClass().add("form-field");
        chambreCombo.setPrefWidth(500);
        Label chambreError = org.example.utils.FormValidator.createErrorLabel();
        chambreError.setId("chambreError");
        chambreBox.getChildren().addAll(chambreLabel, chambreCombo, chambreError);

        // Date Début
        VBox dateDebutBox = new VBox(5);
        Label dateDebutLabel = new Label("Date de début *");
        dateDebutLabel.getStyleClass().add("form-label");
        DatePicker dateDebutPicker = new DatePicker(reservation != null ? reservation.getDateDebut() : LocalDate.now());
        dateDebutPicker.setId("dateDebutPicker");
        dateDebutPicker.getStyleClass().add("form-field");
        dateDebutPicker.setPrefWidth(500);
        Label dateDebutError = org.example.utils.FormValidator.createErrorLabel();
        dateDebutError.setId("dateDebutError");
        dateDebutBox.getChildren().addAll(dateDebutLabel, dateDebutPicker, dateDebutError);

        // Date Fin
        VBox dateFinBox = new VBox(5);
        Label dateFinLabel = new Label("Date de fin *");
        dateFinLabel.getStyleClass().add("form-label");
        DatePicker dateFinPicker = new DatePicker(reservation != null ? reservation.getDateFin() : LocalDate.now().plusDays(1));
        dateFinPicker.setId("dateFinPicker");
        dateFinPicker.getStyleClass().add("form-field");
        dateFinPicker.setPrefWidth(500);
        Label dateFinError = org.example.utils.FormValidator.createErrorLabel();
        dateFinError.setId("dateFinError");
        dateFinBox.getChildren().addAll(dateFinLabel, dateFinPicker, dateFinError);

        // Prix Total
        VBox prixBox = new VBox(5);
        Label prixLabel = new Label("Prix total (DT) *");
        prixLabel.getStyleClass().add("form-label");
        TextField prixField = new TextField(reservation != null ? String.valueOf(reservation.getPrixTotal()) : "");
        prixField.setPromptText("Ex: 450.00");
        prixField.setId("prixField");
        prixField.getStyleClass().add("form-field");
        Label prixError = org.example.utils.FormValidator.createErrorLabel();
        prixError.setId("prixError");
        prixBox.getChildren().addAll(prixLabel, prixField, prixError);

        // Statut
        VBox statutBox = new VBox(5);
        Label statutLabel = new Label("Statut *");
        statutLabel.getStyleClass().add("form-label");
        ComboBox<String> statutCombo = new ComboBox<>();
        statutCombo.getItems().addAll("EN_ATTENTE", "CONFIRMEE", "ANNULEE");
        statutCombo.setValue(reservation != null ? reservation.getStatut() : "EN_ATTENTE");
        statutCombo.setId("statutCombo");
        statutCombo.getStyleClass().add("form-field");
        Label statutError = org.example.utils.FormValidator.createErrorLabel();
        statutError.setId("statutError");
        statutBox.getChildren().addAll(statutLabel, statutCombo, statutError);

        // Info text
        Label infoLabel = new Label("* Champs obligatoires");
        infoLabel.getStyleClass().add("form-help");

        GridPane formGrid = new GridPane();
        formGrid.getStyleClass().add("form-grid");
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        col1.setHgrow(Priority.ALWAYS);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        col2.setHgrow(Priority.ALWAYS);
        formGrid.getColumnConstraints().addAll(col1, col2);

        formGrid.add(userBox, 0, 0);
        formGrid.add(hotelBox, 1, 0);

        formGrid.add(chambreBox, 0, 1);
        formGrid.add(statutBox, 1, 1);

        formGrid.add(dateDebutBox, 0, 2);
        formGrid.add(dateFinBox, 1, 2);

        formGrid.add(prixBox, 0, 3);
        GridPane.setColumnSpan(prixBox, 2);

        container.getChildren().addAll(titleLabel, formGrid, infoLabel);

        // Wrap in ScrollPane
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #F5F3E7; -fx-background-color: #F5F3E7;");
        scrollPane.setPrefHeight(550);
        scrollPane.setMaxHeight(550);

        return scrollPane;
    }

    private Reservation getReservationFromForm(ScrollPane scrollPane, Reservation existingReservation) {
        VBox container = (VBox) scrollPane.getContent();
        ComboBox<String> userCombo = (ComboBox<String>) container.lookup("#userCombo");
        ComboBox<String> hotelCombo = (ComboBox<String>) container.lookup("#hotelCombo");
        ComboBox<String> chambreCombo = (ComboBox<String>) container.lookup("#chambreCombo");
        DatePicker dateDebutPicker = (DatePicker) container.lookup("#dateDebutPicker");
        DatePicker dateFinPicker = (DatePicker) container.lookup("#dateFinPicker");
        TextField prixField = (TextField) container.lookup("#prixField");
        ComboBox<String> statutCombo = (ComboBox<String>) container.lookup("#statutCombo");

        Label userError = (Label) container.lookup("#userError");
        Label hotelError = (Label) container.lookup("#hotelError");
        Label chambreError = (Label) container.lookup("#chambreError");
        Label dateDebutError = (Label) container.lookup("#dateDebutError");
        Label dateFinError = (Label) container.lookup("#dateFinError");
        Label prixError = (Label) container.lookup("#prixError");
        Label statutError = (Label) container.lookup("#statutError");

        // Validation
        boolean valid = true;
        valid &= org.example.utils.FormValidator.validateComboBox(userCombo, userError, "un utilisateur");
        valid &= org.example.utils.FormValidator.validateComboBox(hotelCombo, hotelError, "un hôtel");
        valid &= org.example.utils.FormValidator.validateComboBox(chambreCombo, chambreError, "une chambre");
        valid &= org.example.utils.FormValidator.validateDateRange(dateDebutPicker, dateFinPicker, dateDebutError, dateFinError);
        valid &= org.example.utils.FormValidator.validateDouble(prixField, prixError, "Le prix total");
        valid &= org.example.utils.FormValidator.validateComboBox(statutCombo, statutError, "un statut");

        if (!valid) {
            return null;
        }

        int userId = Integer.parseInt(userCombo.getValue().split(" - ")[0]);
        int hotelId = Integer.parseInt(hotelCombo.getValue().split(" - ")[0]);
        int chambreId = Integer.parseInt(chambreCombo.getValue().split(" - ")[0]);

        if (existingReservation != null) {
            existingReservation.setUserId(userId);
            existingReservation.setHotelId(hotelId);
            existingReservation.setChambreId(chambreId);
            existingReservation.setDateDebut(dateDebutPicker.getValue());
            existingReservation.setDateFin(dateFinPicker.getValue());
            existingReservation.setPrixTotal(Double.parseDouble(prixField.getText().trim()));
            existingReservation.setStatut(statutCombo.getValue());
            return existingReservation;
        } else {
            return new Reservation(
                userId,
                chambreId,
                hotelId,
                dateDebutPicker.getValue(),
                dateFinPicker.getValue(),
                Double.parseDouble(prixField.getText().trim()),
                statutCombo.getValue()
            );
        }
    }

    @FXML
    public void searchReservations() {
        String searchText = searchField.getText().toLowerCase();
        if (searchText.isEmpty()) {
            displayReservationCards();
            return;
        }

        reservationCardsContainer.getChildren().clear();
        for (Reservation reservation : reservationList) {
            String name = reservation.getUserDisplayName().toLowerCase();
            String email = reservation.getUserEmail() != null ? reservation.getUserEmail().toLowerCase() : "";
            if (name.contains(searchText) || email.contains(searchText)) {
                reservationCardsContainer.getChildren().add(createReservationCard(reservation));
            }
        }
    }

    private String formatUserOption(personne user) {
        return user.getId() + " - " + user.getPrenom() + " " + user.getNom() + " (" + user.getEmail() + ")";
    }

    private String findUserOption(int userId) {
        for (personne user : userList) {
            if (user.getId() == userId) {
                return formatUserOption(user);
            }
        }
        return null;
    }

    @FXML
    public void filterByStatut() {
        String selectedStatut = filterStatutCombo.getValue();
        if (selectedStatut == null || selectedStatut.equals("Tous")) {
            displayReservationCards();
            return;
        }

        reservationCardsContainer.getChildren().clear();
        for (Reservation reservation : reservationList) {
            if (reservation.getStatut().equals(selectedStatut)) {
                reservationCardsContainer.getChildren().add(createReservationCard(reservation));
            }
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ===== Navigation Methods =====

    @FXML
    public void goToHotels() {
        navigateTo("/hotel-view.fxml");
    }

    @FXML
    public void goToChambres() {
        navigateTo("/chambre-view.fxml");
    }

    @FXML
    public void goToReservations() {
        // Already on reservations page - refresh
        loadReservations();
    }

    @FXML
    public void backToDashboard() {
        navigateTo("/org/example/AdminDashboardView.fxml");
    }

    private void navigateTo(String fxmlPath) {
        try {
            Stage stage = (Stage) reservationCardsContainer.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de naviguer: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}

