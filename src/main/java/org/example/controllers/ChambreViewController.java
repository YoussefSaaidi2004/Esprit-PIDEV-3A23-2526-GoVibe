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
import org.example.services.ServiceAutoAssignment;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class ChambreViewController implements Initializable {

    @FXML private StackPane rootStack;
    @FXML private ImageView bgImageView;
    @FXML private FlowPane chambreCardsContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterHotelCombo;

    private ServiceChambre serviceChambre;
    private ServiceHotel serviceHotel;
    private ServiceAutoAssignment serviceAutoAssign;
    private ObservableList<Chambre> chambreList;
    private ObservableList<Hotel> hotelList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        serviceChambre = new ServiceChambre();
        serviceHotel = new ServiceHotel();
        serviceAutoAssign = new ServiceAutoAssignment();
        chambreList = FXCollections.observableArrayList();
        hotelList = FXCollections.observableArrayList();

        setupBackground();
        loadHotels();
        loadChambres();
    }

    private void setupBackground() {
        if (bgImageView != null && rootStack != null) {
            bgImageView.fitWidthProperty().bind(rootStack.widthProperty());
            bgImageView.fitHeightProperty().bind(rootStack.heightProperty());
            var url = getClass().getResource("/messages/home-hero5.png");
            if (url != null) {
                bgImageView.setImage(new Image(url.toExternalForm(), true));
            }
        }
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
        VBox card = new VBox(15);
        card.setPrefWidth(320);
        card.getStyleClass().add("glass-card");
        card.setPadding(new Insets(20));

        // Header: Type + Capacity
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label typeLabel = new Label(chambre.getType());
        typeLabel.getStyleClass().add("card-title");
        typeLabel.setStyle("-fx-font-size: 18px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label capacityLabel = new Label("👥 " + chambre.getCapacite());
        capacityLabel.setStyle("-fx-text-fill: #50C878; -fx-font-weight: bold; -fx-font-size: 13px;");

        header.getChildren().addAll(typeLabel, spacer, capacityLabel);

        // Hotel Badge
        String hotelName = getHotelName(chambre.getHotelId());
        Label hotelLabel = new Label("🏨 " + hotelName);
        hotelLabel.setStyle("-fx-background-color: rgba(160,224,201,0.1); -fx-text-fill: #A0E0C9; " +
                           "-fx-padding: 4 10; -fx-background-radius: 10; -fx-font-size: 11px;");

        // Equipments
        Label equipLabel = new Label("✨ " + chambre.getEquipements());
        equipLabel.getStyleClass().add("card-description");
        equipLabel.setWrapText(true);
        equipLabel.setMaxHeight(50);

        // Pricing Grid
        GridPane priceGrid = new GridPane();
        priceGrid.setHgap(15);
        priceGrid.setVgap(8);
        priceGrid.setPadding(new Insets(5, 0, 5, 0));

        priceGrid.add(createPriceLabel("Standard", "#A0E0C9"), 0, 0);
        priceGrid.add(createPriceValue(chambre.getPrixStandard(), "#50C878"), 1, 0);

        priceGrid.add(createPriceLabel("Haute Saison", "#A0E0C9"), 0, 1);
        priceGrid.add(createPriceValue(chambre.getPrixHauteSaison(), "#FF7F50"), 1, 1);

        priceGrid.add(createPriceLabel("Basse Saison", "#A0E0C9"), 0, 2);
        priceGrid.add(createPriceValue(chambre.getPrixBasseSaison(), "#D1F2EB"), 1, 2);

        // 🔴 Availability Badge (checked for today → tomorrow)
        HBox availabilityBox = new HBox(8);
        availabilityBox.setAlignment(Pos.CENTER_LEFT);
        Label availBadge = new Label();
        availBadge.setStyle("-fx-padding: 3 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
        try {
            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);
            boolean dispo = serviceAutoAssign.isChambreDisponible(chambre.getId(), today, tomorrow);
            if (dispo) {
                availBadge.setText("✅ Disponible");
                availBadge.setStyle("-fx-background-color: rgba(80,200,120,0.15); -fx-text-fill: #50C878; " +
                    "-fx-padding: 3 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else {
                availBadge.setText("🔴 Occupée");
                availBadge.setStyle("-fx-background-color: rgba(216,78,54,0.15); -fx-text-fill: #D84E36; " +
                    "-fx-padding: 3 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
            }
        } catch (Exception ex) {
            availBadge.setText("⚪ Statut inconnu");
            availBadge.setStyle("-fx-background-color: rgba(160,224,201,0.1); -fx-text-fill: #A0E0C9; " +
                "-fx-padding: 3 10; -fx-background-radius: 12; -fx-font-size: 11px;");
        }
        availabilityBox.getChildren().add(availBadge);

        Separator sep = new Separator();
        sep.setOpacity(0.1);

        // Action buttons
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = new Button("✎");
        editBtn.getStyleClass().add("card-action-btn");
        editBtn.setOnAction(e -> editChambre(chambre));

        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().add("card-action-btn-danger");
        deleteBtn.setOnAction(e -> deleteChambre(chambre));

        footer.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(header, hotelLabel, equipLabel, priceGrid, availabilityBox, sep, footer);

        // Hover Effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-border-color: #50C878; -fx-border-width: 1; -fx-border-radius: 20;"));
        card.setOnMouseExited(e -> card.setStyle(""));

        return card;
    }

    private Label createPriceLabel(String text, String color) {
        Label l = new Label(text + " :");
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px;");
        return l;
    }

    private Label createPriceValue(double price, String color) {
        Label l = new Label(String.format("%.2f DT", price));
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        return l;
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
        dialogPane.getStylesheets().add(getClass().getResource("/styles/unified-styles.css").toExternalForm());
        dialogPane.getStyleClass().add("form-dialog");

        ButtonType saveButtonType = new ButtonType("💾 Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("❌ Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        // Style buttons
        Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
        saveButton.getStyleClass().add("premium-button");
        Button cancelButton = (Button) dialogPane.lookupButton(cancelButtonType);
        cancelButton.getStyleClass().add("card-action-btn-danger");

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
        dialogPane.getStylesheets().add(getClass().getResource("/styles/unified-styles.css").toExternalForm());
        dialogPane.getStyleClass().add("form-dialog");

        ButtonType saveButtonType = new ButtonType("💾 Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("❌ Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        // Style buttons
        Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
        saveButton.getStyleClass().add("premium-button");
        Button cancelButton = (Button) dialogPane.lookupButton(cancelButtonType);
        cancelButton.getStyleClass().add("card-action-btn-danger");

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
        VBox container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setPrefWidth(500);
        container.getStyleClass().add("form-card-glass");

        Label titleLabel = new Label("✨ Détails de la Chambre");
        titleLabel.getStyleClass().add("hero-title");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #0B6E4F;");

        // Type
        VBox typeBox = new VBox(5);
        Label typeLabel = new Label("Type de chambre *");
        typeLabel.getStyleClass().add("form-label");
        TextField typeField = new TextField(chambre != null ? chambre.getType() : "");
        typeField.setPromptText("Ex: Suite, Standard, Deluxe...");
        typeField.setId("typeField");
        typeField.getStyleClass().add("form-field");
        Label typeError = org.example.utils.FormValidator.createErrorLabel();
        typeError.setId("typeError");
        typeBox.getChildren().addAll(typeLabel, typeField, typeError);

        // Capacité
        VBox capaciteBox = new VBox(5);
        Label capaciteLabel = new Label("Capacité (personnes) *");
        capaciteLabel.getStyleClass().add("form-label");
        Spinner<Integer> capaciteSpinner = new Spinner<>(1, 10, chambre != null ? chambre.getCapacite() : 2);
        capaciteSpinner.setId("capaciteSpinner");
        capaciteSpinner.setEditable(true);
        capaciteSpinner.getStyleClass().add("form-field");
        capaciteBox.getChildren().addAll(capaciteLabel, capaciteSpinner);

        // Équipements
        VBox equipBox = new VBox(5);
        Label equipLabel = new Label("Équipements *");
        equipLabel.getStyleClass().add("form-label");
        TextField equipField = new TextField(chambre != null ? chambre.getEquipements() : "");
        equipField.setPromptText("Ex: WiFi, TV, Climatisation, Balcon");
        equipField.setId("equipField");
        equipField.getStyleClass().add("form-field");
        Label equipError = org.example.utils.FormValidator.createErrorLabel();
        equipError.setId("equipError");
        equipBox.getChildren().addAll(equipLabel, equipField, equipError);

        // Hôtel
        VBox hotelBox = new VBox(5);
        Label hotelLabel = new Label("Hôtel *");
        hotelLabel.getStyleClass().add("form-label");
        ComboBox<String> hotelCombo = new ComboBox<>();
        hotelCombo.setPromptText("Sélectionnez un hôtel");
        for (Hotel h : hotelList) {
            hotelCombo.getItems().add(h.getId() + " - " + h.getNom());
        }
        if (chambre != null) {
            hotelCombo.setValue(chambre.getHotelId() + " - " + getHotelName(chambre.getHotelId()));
        }
        hotelCombo.setId("hotelCombo");
        hotelCombo.getStyleClass().add("form-field");
        hotelCombo.setPrefWidth(430);
        Label hotelError = org.example.utils.FormValidator.createErrorLabel();
        hotelError.setId("hotelError");
        hotelBox.getChildren().addAll(hotelLabel, hotelCombo, hotelError);

        // Prix Standard
        VBox prixStdBox = new VBox(5);
        Label prixStdLabel = new Label("Prix Standard (DT) *");
        prixStdLabel.getStyleClass().add("form-label");
        TextField prixStdField = new TextField(chambre != null ? String.valueOf(chambre.getPrixStandard()) : "");
        prixStdField.setPromptText("Ex: 150.00");
        prixStdField.setId("prixStdField");
        prixStdField.getStyleClass().add("form-field");
        Label prixStdError = org.example.utils.FormValidator.createErrorLabel();
        prixStdError.setId("prixStdError");
        prixStdBox.getChildren().addAll(prixStdLabel, prixStdField, prixStdError);

        // Prix Haute Saison
        VBox prixHauteBox = new VBox(5);
        Label prixHauteLabel = new Label("Prix Haute Saison (DT) *");
        prixHauteLabel.getStyleClass().add("form-label");
        TextField prixHauteField = new TextField(chambre != null ? String.valueOf(chambre.getPrixHauteSaison()) : "");
        prixHauteField.setPromptText("Ex: 200.00");
        prixHauteField.setId("prixHauteField");
        prixHauteField.getStyleClass().add("form-field");
        Label prixHauteError = org.example.utils.FormValidator.createErrorLabel();
        prixHauteError.setId("prixHauteError");
        prixHauteBox.getChildren().addAll(prixHauteLabel, prixHauteField, prixHauteError);

        // Prix Basse Saison
        VBox prixBasseBox = new VBox(5);
        Label prixBasseLabel = new Label("Prix Basse Saison (DT) *");
        prixBasseLabel.getStyleClass().add("form-label");
        TextField prixBasseField = new TextField(chambre != null ? String.valueOf(chambre.getPrixBasseSaison()) : "");
        prixBasseField.setPromptText("Ex: 120.00");
        prixBasseField.setId("prixBasseField");
        prixBasseField.getStyleClass().add("form-field");
        Label prixBasseError = org.example.utils.FormValidator.createErrorLabel();
        prixBasseError.setId("prixBasseError");
        prixBasseBox.getChildren().addAll(prixBasseLabel, prixBasseField, prixBasseError);

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

        formGrid.add(typeBox, 0, 0);
        formGrid.add(capaciteBox, 1, 0);

        formGrid.add(hotelBox, 0, 1);
        formGrid.add(equipBox, 1, 1);

        formGrid.add(prixStdBox, 0, 2);
        formGrid.add(prixHauteBox, 1, 2);

        formGrid.add(prixBasseBox, 0, 3);
        GridPane.setColumnSpan(prixBasseBox, 2);

        container.getChildren().addAll(titleLabel, formGrid, infoLabel);

        // Wrap in ScrollPane
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setPrefHeight(550);
        scrollPane.setMaxHeight(550);

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

    // ===== Navigation Methods =====

    @FXML
    public void goToHotels() {
        navigateTo("/hotel-view.fxml");
    }

    @FXML
    public void goToChambres() {
        // Already on chambres page - refresh
        loadChambres();
    }

    @FXML
    public void goToReservations() {
        navigateTo("/reservation-view.fxml");
    }

    @FXML
    public void backToDashboard() {
        navigateTo("/org/example/AdminDashboardView.fxml");
    }

    private void navigateTo(String fxmlPath) {
        try {
            Stage stage = (Stage) chambreCardsContainer.getScene().getWindow();
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

