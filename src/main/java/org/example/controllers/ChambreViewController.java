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
import org.example.services.ServiceChambre;
import org.example.services.ServiceHotel;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ChambreViewController implements Initializable {

    @FXML private FlowPane chambreCardsContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterHotelCombo;

    private ServiceChambre serviceChambre;
    private ServiceHotel serviceHotel;
    private ObservableList<Chambre> chambreList;
    private ObservableList<Hotel> hotelList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        serviceChambre = new ServiceChambre();
        serviceHotel = new ServiceHotel();
        chambreList = FXCollections.observableArrayList();
        hotelList = FXCollections.observableArrayList();

        loadHotels();
        loadChambres();
    }

    private void loadHotels() {
        try {
            hotelList.clear();
            hotelList.addAll(serviceHotel.show());

            if (filterHotelCombo != null) {
                filterHotelCombo.getItems().clear();
                filterHotelCombo.getItems().add("Tous les hôtels");
                for (Hotel h : hotelList) {
                    filterHotelCombo.getItems().add(h.getNom());
                }
                filterHotelCombo.setValue("Tous les hôtels");
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les hôtels: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadChambres() {
        try {
            chambreList.clear();
            chambreList.addAll(serviceChambre.show());
            displayChambreCards();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les chambres: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void displayChambreCards() {
        chambreCardsContainer.getChildren().clear();

        for (Chambre chambre : chambreList) {
            VBox card = createChambreCard(chambre);
            chambreCardsContainer.getChildren().add(card);
        }
    }

    private VBox createChambreCard(Chambre chambre) {
        VBox card = new VBox(12);
        card.setPrefWidth(300);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                     "-fx-effect: dropshadow(gaussian, rgba(1,50,32,0.15), 15, 0, 0, 5); " +
                     "-fx-padding: 20; -fx-cursor: hand;");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label typeLabel = new Label(chambre.getType());
        typeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #013220;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label capacityLabel = new Label("👥 " + chambre.getCapacite() + " pers");
        capacityLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #0B6E4F; -fx-font-weight: bold;");

        header.getChildren().addAll(typeLabel, spacer, capacityLabel);

        // Hotel name
        String hotelName = getHotelName(chambre.getHotelId());
        Label hotelLabel = new Label("🏨 " + hotelName);
        hotelLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        // Equipments
        Label equipLabel = new Label("🛋️ " + chambre.getEquipements());
        equipLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555; -fx-wrap-text: true;");
        equipLabel.setWrapText(true);
        equipLabel.setMaxWidth(260);

        // Prices
        VBox pricesBox = new VBox(6);
        pricesBox.setStyle("-fx-background-color: #D1F2EB; -fx-background-radius: 10; -fx-padding: 12;");

        Label priceTitle = new Label("💰 Tarifs");
        priceTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #013220;");

        HBox standardBox = new HBox(8);
        standardBox.setAlignment(Pos.CENTER_LEFT);
        Label standardLabel = new Label("Standard:");
        standardLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        Label standardPrice = new Label(String.format("%.2f DT", chambre.getPrixStandard()));
        standardPrice.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #0B6E4F;");
        standardBox.getChildren().addAll(standardLabel, standardPrice);

        HBox hauteBox = new HBox(8);
        hauteBox.setAlignment(Pos.CENTER_LEFT);
        Label hauteLabel = new Label("Haute saison:");
        hauteLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        Label hautePrice = new Label(String.format("%.2f DT", chambre.getPrixHauteSaison()));
        hautePrice.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #D84E36;");
        hauteBox.getChildren().addAll(hauteLabel, hautePrice);

        HBox basseBox = new HBox(8);
        basseBox.setAlignment(Pos.CENTER_LEFT);
        Label basseLabel = new Label("Basse saison:");
        basseLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        Label bassePrice = new Label(String.format("%.2f DT", chambre.getPrixBasseSaison()));
        bassePrice.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #50C878;");
        basseBox.getChildren().addAll(basseLabel, bassePrice);

        pricesBox.getChildren().addAll(priceTitle, standardBox, hauteBox, basseBox);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #E0E0E0;");

        // Action buttons
        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER);

        Button editBtn = new Button("✏️ Modifier");
        editBtn.setStyle("-fx-background-color: #50C878; -fx-text-fill: white; -fx-background-radius: 8; " +
                        "-fx-padding: 8 16; -fx-font-size: 12px; -fx-cursor: hand; -fx-font-weight: bold;");
        editBtn.setOnAction(e -> editChambre(chambre));

        Button deleteBtn = new Button("🗑️");
        deleteBtn.setStyle("-fx-background-color: #D84E36; -fx-text-fill: white; -fx-background-radius: 8; " +
                          "-fx-padding: 8 12; -fx-font-size: 12px; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> deleteChambre(chambre));

        actionButtons.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(header, hotelLabel, equipLabel, pricesBox, sep, actionButtons);

        return card;
    }

    private String getHotelName(int hotelId) {
        for (Hotel h : hotelList) {
            if (h.getId() == hotelId) {
                return h.getNom();
            }
        }
        return "Hôtel inconnu";
    }

    @FXML
    public void addChambre() {
        Dialog<Chambre> dialog = new Dialog<>();
        dialog.setTitle("Ajouter une Chambre");
        dialog.setHeaderText("✨ Nouvelle Chambre");

        // Style the dialog
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #F5F3E7;");

        ButtonType saveButtonType = new ButtonType("💾 Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("❌ Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        // Style buttons
        Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
        saveButton.setStyle("-fx-background-color: #50C878; -fx-text-fill: white; -fx-font-weight: bold; " +
                           "-fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");

        ScrollPane form = createChambreForm(null);
        dialog.getDialogPane().setContent(form);

        // Prevent closing on validation error
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            Chambre chambre = getChambreFromForm(form, null);
            if (chambre == null) {
                event.consume();
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return getChambreFromForm(form, null);
            }
            return null;
        });

        Optional<Chambre> result = dialog.showAndWait();
        result.ifPresent(chambre -> {
            try {
                serviceChambre.insert(chambre);
                loadChambres();
                showAlert("Succès", "Chambre ajoutée avec succès!", Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                showAlert("Erreur", "Erreur lors de l'ajout: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void editChambre(Chambre chambre) {
        Dialog<Chambre> dialog = new Dialog<>();
        dialog.setTitle("Modifier la Chambre");
        dialog.setHeaderText("✏️ Modifier: " + chambre.getType());

        // Style the dialog
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #F5F3E7;");

        ButtonType saveButtonType = new ButtonType("💾 Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("❌ Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        // Style buttons
        Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
        saveButton.setStyle("-fx-background-color: #50C878; -fx-text-fill: white; -fx-font-weight: bold; " +
                           "-fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");

        ScrollPane form = createChambreForm(chambre);
        dialog.getDialogPane().setContent(form);

        // Prevent closing on validation error
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            Chambre updatedChambre = getChambreFromForm(form, chambre);
            if (updatedChambre == null) {
                event.consume();
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return getChambreFromForm(form, chambre);
            }
            return null;
        });

        Optional<Chambre> result = dialog.showAndWait();
        result.ifPresent(updatedChambre -> {
            try {
                serviceChambre.update(updatedChambre);
                loadChambres();
                showAlert("Succès", "Chambre modifiée avec succès!", Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                showAlert("Erreur", "Erreur lors de la modification: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void deleteChambre(Chambre chambre) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer la chambre");
        confirmation.setContentText("Êtes-vous sûr de vouloir supprimer cette chambre ?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                serviceChambre.delete(chambre.getId());
                loadChambres();
                showAlert("Succès", "Chambre supprimée avec succès!", Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                showAlert("Erreur", "Erreur lors de la suppression: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private ScrollPane createChambreForm(Chambre chambre) {
        VBox container = new VBox(15);
        container.setPadding(new Insets(25));
        container.setStyle("-fx-background-color: #F5F3E7; -fx-background-radius: 10;");
        container.setPrefWidth(480);

        // Type
        VBox typeBox = new VBox(5);
        Label typeLabel = new Label("Type de chambre *");
        typeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #013220;");
        TextField typeField = new TextField(chambre != null ? chambre.getType() : "");
        typeField.setPromptText("Ex: Suite, Standard, Deluxe...");
        typeField.setId("typeField");
        typeField.setStyle("-fx-padding: 10; -fx-background-radius: 8; -fx-font-size: 13px;");
        Label typeError = org.example.utils.FormValidator.createErrorLabel();
        typeError.setId("typeError");
        typeBox.getChildren().addAll(typeLabel, typeField, typeError);

        // Capacité
        VBox capaciteBox = new VBox(5);
        Label capaciteLabel = new Label("Capacité (personnes) *");
        capaciteLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #013220;");
        Spinner<Integer> capaciteSpinner = new Spinner<>(1, 10, chambre != null ? chambre.getCapacite() : 2);
        capaciteSpinner.setId("capaciteSpinner");
        capaciteSpinner.setEditable(true);
        capaciteSpinner.setStyle("-fx-padding: 5;");
        capaciteBox.getChildren().addAll(capaciteLabel, capaciteSpinner);

        // Équipements
        VBox equipBox = new VBox(5);
        Label equipLabel = new Label("Équipements *");
        equipLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #013220;");
        TextField equipField = new TextField(chambre != null ? chambre.getEquipements() : "");
        equipField.setPromptText("Ex: WiFi, TV, Climatisation, Balcon");
        equipField.setId("equipField");
        equipField.setStyle("-fx-padding: 10; -fx-background-radius: 8; -fx-font-size: 13px;");
        Label equipError = org.example.utils.FormValidator.createErrorLabel();
        equipError.setId("equipError");
        equipBox.getChildren().addAll(equipLabel, equipField, equipError);

        // Hôtel
        VBox hotelBox = new VBox(5);
        Label hotelLabel = new Label("Hôtel *");
        hotelLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #013220;");
        ComboBox<String> hotelCombo = new ComboBox<>();
        hotelCombo.setPromptText("Sélectionnez un hôtel");
        for (Hotel h : hotelList) {
            hotelCombo.getItems().add(h.getId() + " - " + h.getNom());
        }
        if (chambre != null) {
            hotelCombo.setValue(chambre.getHotelId() + " - " + getHotelName(chambre.getHotelId()));
        }
        hotelCombo.setId("hotelCombo");
        hotelCombo.setStyle("-fx-padding: 5;");
        hotelCombo.setPrefWidth(430);
        Label hotelError = org.example.utils.FormValidator.createErrorLabel();
        hotelError.setId("hotelError");
        hotelBox.getChildren().addAll(hotelLabel, hotelCombo, hotelError);

        // Prix Standard
        VBox prixStdBox = new VBox(5);
        Label prixStdLabel = new Label("Prix Standard (DT) *");
        prixStdLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #013220;");
        TextField prixStdField = new TextField(chambre != null ? String.valueOf(chambre.getPrixStandard()) : "");
        prixStdField.setPromptText("Ex: 150.00");
        prixStdField.setId("prixStdField");
        prixStdField.setStyle("-fx-padding: 10; -fx-background-radius: 8; -fx-font-size: 13px;");
        Label prixStdError = org.example.utils.FormValidator.createErrorLabel();
        prixStdError.setId("prixStdError");
        prixStdBox.getChildren().addAll(prixStdLabel, prixStdField, prixStdError);

        // Prix Haute Saison
        VBox prixHauteBox = new VBox(5);
        Label prixHauteLabel = new Label("Prix Haute Saison (DT) *");
        prixHauteLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #013220;");
        TextField prixHauteField = new TextField(chambre != null ? String.valueOf(chambre.getPrixHauteSaison()) : "");
        prixHauteField.setPromptText("Ex: 200.00");
        prixHauteField.setId("prixHauteField");
        prixHauteField.setStyle("-fx-padding: 10; -fx-background-radius: 8; -fx-font-size: 13px;");
        Label prixHauteError = org.example.utils.FormValidator.createErrorLabel();
        prixHauteError.setId("prixHauteError");
        prixHauteBox.getChildren().addAll(prixHauteLabel, prixHauteField, prixHauteError);

        // Prix Basse Saison
        VBox prixBasseBox = new VBox(5);
        Label prixBasseLabel = new Label("Prix Basse Saison (DT) *");
        prixBasseLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #013220;");
        TextField prixBasseField = new TextField(chambre != null ? String.valueOf(chambre.getPrixBasseSaison()) : "");
        prixBasseField.setPromptText("Ex: 120.00");
        prixBasseField.setId("prixBasseField");
        prixBasseField.setStyle("-fx-padding: 10; -fx-background-radius: 8; -fx-font-size: 13px;");
        Label prixBasseError = org.example.utils.FormValidator.createErrorLabel();
        prixBasseError.setId("prixBasseError");
        prixBasseBox.getChildren().addAll(prixBasseLabel, prixBasseField, prixBasseError);

        // Info text
        Label infoLabel = new Label("* Champs obligatoires");
        infoLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px; -fx-font-style: italic;");

        container.getChildren().addAll(typeBox, capaciteBox, equipBox, hotelBox,
                                        prixStdBox, prixHauteBox, prixBasseBox, infoLabel);

        // Wrap in ScrollPane
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #F5F3E7; -fx-background-color: #F5F3E7;");
        scrollPane.setPrefHeight(500);
        scrollPane.setMaxHeight(500);

        return scrollPane;
    }

    private Chambre getChambreFromForm(ScrollPane scrollPane, Chambre existingChambre) {
        VBox container = (VBox) scrollPane.getContent();
        TextField typeField = (TextField) container.lookup("#typeField");
        Spinner<Integer> capaciteSpinner = (Spinner<Integer>) container.lookup("#capaciteSpinner");
        TextField equipField = (TextField) container.lookup("#equipField");
        ComboBox<String> hotelCombo = (ComboBox<String>) container.lookup("#hotelCombo");
        TextField prixStdField = (TextField) container.lookup("#prixStdField");
        TextField prixHauteField = (TextField) container.lookup("#prixHauteField");
        TextField prixBasseField = (TextField) container.lookup("#prixBasseField");

        Label typeError = (Label) container.lookup("#typeError");
        Label equipError = (Label) container.lookup("#equipError");
        Label hotelError = (Label) container.lookup("#hotelError");
        Label prixStdError = (Label) container.lookup("#prixStdError");
        Label prixHauteError = (Label) container.lookup("#prixHauteError");
        Label prixBasseError = (Label) container.lookup("#prixBasseError");

        // Validation
        boolean valid = true;
        valid &= org.example.utils.FormValidator.validateRequired(typeField, typeError, "Le type");
        valid &= org.example.utils.FormValidator.validateRequired(equipField, equipError, "Les équipements");
        valid &= org.example.utils.FormValidator.validateComboBox(hotelCombo, hotelError, "un hôtel");
        valid &= org.example.utils.FormValidator.validateDouble(prixStdField, prixStdError, "Le prix standard");
        valid &= org.example.utils.FormValidator.validateDouble(prixHauteField, prixHauteError, "Le prix haute saison");
        valid &= org.example.utils.FormValidator.validateDouble(prixBasseField, prixBasseError, "Le prix basse saison");

        if (!valid) {
            return null;
        }

        String hotelSelection = hotelCombo.getValue();
        int hotelId = Integer.parseInt(hotelSelection.split(" - ")[0]);

        if (existingChambre != null) {
            existingChambre.setType(typeField.getText().trim());
            existingChambre.setCapacite(capaciteSpinner.getValue());
            existingChambre.setEquipements(equipField.getText().trim());
            existingChambre.setHotelId(hotelId);
            existingChambre.setPrixStandard(Double.parseDouble(prixStdField.getText().trim()));
            existingChambre.setPrixHauteSaison(Double.parseDouble(prixHauteField.getText().trim()));
            existingChambre.setPrixBasseSaison(Double.parseDouble(prixBasseField.getText().trim()));
            return existingChambre;
        } else {
            return new Chambre(
                typeField.getText().trim(),
                capaciteSpinner.getValue(),
                equipField.getText().trim(),
                hotelId,
                Double.parseDouble(prixStdField.getText().trim()),
                Double.parseDouble(prixHauteField.getText().trim()),
                Double.parseDouble(prixBasseField.getText().trim())
            );
        }
    }

    @FXML
    public void searchChambres() {
        String searchText = searchField.getText().toLowerCase();
        if (searchText.isEmpty()) {
            displayChambreCards();
            return;
        }

        chambreCardsContainer.getChildren().clear();
        for (Chambre chambre : chambreList) {
            if (chambre.getType().toLowerCase().contains(searchText) ||
                chambre.getEquipements().toLowerCase().contains(searchText)) {
                chambreCardsContainer.getChildren().add(createChambreCard(chambre));
            }
        }
    }

    @FXML
    public void filterByHotel() {
        String selectedHotel = filterHotelCombo.getValue();
        if (selectedHotel == null || selectedHotel.equals("Tous les hôtels")) {
            displayChambreCards();
            return;
        }

        chambreCardsContainer.getChildren().clear();
        for (Chambre chambre : chambreList) {
            if (getHotelName(chambre.getHotelId()).equals(selectedHotel)) {
                chambreCardsContainer.getChildren().add(createChambreCard(chambre));
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
}

