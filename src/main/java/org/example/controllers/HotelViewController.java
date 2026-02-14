package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import org.example.entities.Hotel;
import org.example.services.ServiceHotel;

import java.net.URL;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.stage.FileChooser;
import java.io.File;

public class HotelViewController implements Initializable {

    @FXML private FlowPane hotelCardsContainer;
    @FXML private TextField searchField;

    private ServiceHotel serviceHotel;
    private ObservableList<Hotel> hotelList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        serviceHotel = new ServiceHotel();
        hotelList = FXCollections.observableArrayList();
        loadHotels();
    }

    private void loadHotels() {
        try {
            hotelList.clear();
            hotelList.addAll(serviceHotel.show());
            displayHotelCards();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les hôtels: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void displayHotelCards() {
        hotelCardsContainer.getChildren().clear();

        for (Hotel hotel : hotelList) {
            VBox card = createHotelCard(hotel);
            hotelCardsContainer.getChildren().add(card);
        }
    }

    private VBox createHotelCard(Hotel hotel) {
        VBox card = new VBox(12);
        card.setPrefWidth(320);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                     "-fx-effect: dropshadow(gaussian, rgba(1,50,32,0.15), 15, 0, 0, 5); " +
                     "-fx-padding: 0; -fx-cursor: hand;");

        // IMAGE HEADER
        StackPane imageWrapper = new StackPane();
        imageWrapper.setPrefHeight(150);
        imageWrapper.setMaxHeight(150);
        imageWrapper.setStyle("-fx-background-radius: 15 15 0 0; -fx-overflow: hidden;");

        ImageView imageView = new ImageView();
        imageView.setFitHeight(150);
        imageView.setFitWidth(320);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.setCache(true);
        imageView.setStyle("-fx-background-radius: 15 15 0 0;");

        // Try to load hotel image, fallback to placeholder
        String url = hotel.getPhotoUrl();
        Image image;
        try {
            if (url != null && !url.isBlank()) {
                if (url.startsWith("http")) {
                    image = new Image(url, 320, 150, false, true, true);
                } else {
                    // treat as local resource or file path
                    if (url.startsWith("file:")) {
                        image = new Image(url, 320, 150, false, true, true);
                    } else {
                        image = new Image("file:" + url, 320, 150, false, true, true);
                    }
                }
            } else {
                image = new Image(getClass().getResource("/images/hotel-placeholder.jpg").toExternalForm(),
                                   320, 150, false, true, true);
            }
        } catch (Exception ex) {
            image = new Image(getClass().getResource("/images/hotel-placeholder.jpg").toExternalForm(),
                               320, 150, false, true, true);
        }
        imageView.setImage(image);

        // Overlay gradient and hotel name
        VBox imageOverlay = new VBox(4);
        imageOverlay.setPadding(new Insets(10));
        imageOverlay.setAlignment(Pos.BOTTOM_LEFT);
        imageOverlay.setStyle("-fx-background-color: linear-gradient(to top, rgba(1,50,32,0.85), transparent);");

        Label nameLabel = new Label(hotel.getNom());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        nameLabel.setMaxWidth(260);
        nameLabel.setWrapText(true);

        Label citySmall = new Label("\uD83D\uDCCD " + hotel.getVille());
        citySmall.setStyle("-fx-font-size: 12px; -fx-text-fill: #D1F2EB;");

        imageOverlay.getChildren().addAll(nameLabel, citySmall);

        imageWrapper.getChildren().addAll(imageView, imageOverlay);

        // CONTENT SECTION
        VBox content = new VBox(8);
        content.setPadding(new Insets(12, 16, 16, 16));

        // Stars row
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label starsLabel = new Label("\u2B50".repeat(Math.max(0, hotel.getNombreEtoiles())));
        starsLabel.setStyle("-fx-font-size: 13px;");
        header.getChildren().addAll(spacer, starsLabel);

        // Address
        Label addressLabel = new Label(hotel.getAdresse());
        addressLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #000000; -fx-wrap-text: true;");
        addressLabel.setWrapText(true);
        addressLabel.setMaxWidth(260);

        // Budget
        HBox budgetBox = new HBox(8);
        budgetBox.setAlignment(Pos.CENTER_LEFT);
        budgetBox.setStyle("-fx-background-color: #D1F2EB; -fx-background-radius: 8; -fx-padding: 6 8;");
        Label budgetLabel = new Label("\uD83D\uDCB0 Budget:");
        budgetLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #013220; -fx-font-weight: bold;");
        Label budgetValue = new Label(String.format("%.2f DT", hotel.getBudget()));
        budgetValue.setStyle("-fx-font-size: 12px; -fx-text-fill: #0B6E4F; -fx-font-weight: bold;");
        budgetBox.getChildren().addAll(budgetLabel, budgetValue);

        // Description
        Label descLabel = new Label(hotel.getDescription());
        descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555; -fx-wrap-text: true;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(260);
        descLabel.setMaxHeight(40);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #E0E0E0;");

        // Action buttons
        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER);
        Button editBtn = new Button("\u270F\uFE0F Modifier");
        editBtn.setStyle("-fx-background-color: #50C878; -fx-text-fill: white; -fx-background-radius: 8; " +
                        "-fx-padding: 6 14; -fx-font-size: 11px; -fx-cursor: hand; -fx-font-weight: bold;");
        editBtn.setOnAction(e -> editHotel(hotel));
        Button deleteBtn = new Button("\uD83D\uDDD1\uFE0F");
        deleteBtn.setStyle("-fx-background-color: #D84E36; -fx-text-fill: white; -fx-background-radius: 8; " +
                          "-fx-padding: 6 10; -fx-font-size: 11px; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> deleteHotel(hotel));
        actionButtons.getChildren().addAll(editBtn, deleteBtn);

        content.getChildren().addAll(header, addressLabel, budgetBox, descLabel, sep, actionButtons);

        card.getChildren().addAll(imageWrapper, content);
        return card;
    }

    @FXML
    public void addHotel() {
        Dialog<Hotel> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un Hôtel");
        dialog.setHeaderText("✨ Nouvel Hôtel");

        // Style the dialog
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #F5F3E7;");
        dialogPane.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        ButtonType saveButtonType = new ButtonType("💾 Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("❌ Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        // Style buttons
        Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
        saveButton.setStyle("-fx-background-color: #50C878; -fx-text-fill: white; -fx-font-weight: bold; " +
                           "-fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");

        ScrollPane form = createHotelForm(null);
        dialog.getDialogPane().setContent(form);

        // Prevent closing on validation error
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            Hotel hotel = getHotelFromForm(form, null);
            if (hotel == null) {
                event.consume(); // Prevent dialog from closing
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return getHotelFromForm(form, null);
            }
            return null;
        });

        Optional<Hotel> result = dialog.showAndWait();
        result.ifPresent(hotel -> {
            try {
                serviceHotel.insert(hotel);
                loadHotels();
                showAlert("Succès", "Hôtel ajouté avec succès!", Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                showAlert("Erreur", "Erreur lors de l'ajout: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void editHotel(Hotel hotel) {
        Dialog<Hotel> dialog = new Dialog<>();
        dialog.setTitle("Modifier l'Hôtel");
        dialog.setHeaderText("✏️ Modifier: " + hotel.getNom());

        // Style the dialog
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #F5F3E7;");
        dialogPane.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        ButtonType saveButtonType = new ButtonType("💾 Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("❌ Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        // Style buttons
        Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
        saveButton.setStyle("-fx-background-color: #50C878; -fx-text-fill: white; -fx-font-weight: bold; " +
                           "-fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");

        ScrollPane form = createHotelForm(hotel);
        dialog.getDialogPane().setContent(form);

        // Prevent closing on validation error
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            Hotel updatedHotel = getHotelFromForm(form, hotel);
            if (updatedHotel == null) {
                event.consume();
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return getHotelFromForm(form, hotel);
            }
            return null;
        });

        Optional<Hotel> result = dialog.showAndWait();
        result.ifPresent(updatedHotel -> {
            try {
                serviceHotel.update(updatedHotel);
                loadHotels();
                showAlert("Succès", "Hôtel modifié avec succès!", Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                showAlert("Erreur", "Erreur lors de la modification: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void deleteHotel(Hotel hotel) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer l'hôtel");
        confirmation.setContentText("Êtes-vous sûr de vouloir supprimer l'hôtel \"" + hotel.getNom() + "\" ?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                serviceHotel.delete(hotel.getId());
                loadHotels();
                showAlert("Succès", "Hôtel supprimé avec succès!", Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                showAlert("Erreur", "Erreur lors de la suppression: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private ScrollPane createHotelForm(Hotel hotel) {
        VBox container = new VBox(15);
        container.setPadding(new Insets(25));
        container.setStyle("-fx-background-color: #F5F3E7; -fx-background-radius: 10;");
        container.setPrefWidth(480);

        // Nom
        VBox nomBox = new VBox(5);
        Label nomLabel = new Label("Nom de l'hôtel *");
        nomLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #013220;");
        TextField nomField = new TextField(hotel != null ? hotel.getNom() : "");
        nomField.setPromptText("Ex: Hôtel Royal Palace");
        nomField.setId("nomField");
        nomField.setStyle("-fx-padding: 10; -fx-background-radius: 8; -fx-font-size: 13px;");
        Label nomError = org.example.utils.FormValidator.createErrorLabel();
        nomError.setId("nomError");
        nomBox.getChildren().addAll(nomLabel, nomField, nomError);

        // Adresse
        VBox adresseBox = new VBox(5);
        Label adresseLabel = new Label("Adresse *");
        adresseLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #013220;");
        TextField adresseField = new TextField(hotel != null ? hotel.getAdresse() : "");
        adresseField.setPromptText("Ex: 123 Avenue Habib Bourguiba");
        adresseField.setId("adresseField");
        adresseField.setStyle("-fx-padding: 10; -fx-background-radius: 8; -fx-font-size: 13px;");
        Label adresseError = org.example.utils.FormValidator.createErrorLabel();
        adresseError.setId("adresseError");
        adresseBox.getChildren().addAll(adresseLabel, adresseField, adresseError);

        // Ville
        VBox villeBox = new VBox(5);
        Label villeLabel = new Label("Ville *");
        villeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #013220;");
        TextField villeField = new TextField(hotel != null ? hotel.getVille() : "");
        villeField.setPromptText("Ex: Tunis");
        villeField.setId("villeField");
        villeField.setStyle("-fx-padding: 10; -fx-background-radius: 8; -fx-font-size: 13px;");
        Label villeError = org.example.utils.FormValidator.createErrorLabel();
        villeError.setId("villeError");
        villeBox.getChildren().addAll(villeLabel, villeField, villeError);

        // Étoiles
        VBox etoilesBox = new VBox(5);
        Label etoilesLabel = new Label("Nombre d'étoiles (1-5) *");
        etoilesLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #013220;");
        Spinner<Integer> etoilesSpinner = new Spinner<>(1, 5, hotel != null ? hotel.getNombreEtoiles() : 3);
        etoilesSpinner.setId("etoilesSpinner");
        etoilesSpinner.setStyle("-fx-padding: 5;");
        etoilesSpinner.setEditable(true);
        etoilesBox.getChildren().addAll(etoilesLabel, etoilesSpinner);

        // Budget
        VBox budgetBox = new VBox(5);
        Label budgetLabel = new Label("Budget (DT) *");
        budgetLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #013220;");
        TextField budgetField = new TextField(hotel != null ? String.valueOf(hotel.getBudget()) : "");
        budgetField.setPromptText("Ex: 200.00");
        budgetField.setId("budgetField");
        budgetField.setStyle("-fx-padding: 10; -fx-background-radius: 8; -fx-font-size: 13px;");
        Label budgetError = org.example.utils.FormValidator.createErrorLabel();
        budgetError.setId("budgetError");
        budgetBox.getChildren().addAll(budgetLabel, budgetField, budgetError);

        // Description
        VBox descBox = new VBox(5);
        Label descLabel = new Label("Description *");
        descLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #013220;");
        TextArea descArea = new TextArea(hotel != null ? hotel.getDescription() : "");
        descArea.setPromptText("Décrivez votre hôtel...");
        descArea.setPrefRowCount(3);
        descArea.setId("descArea");
        descArea.setStyle("-fx-padding: 10; -fx-background-radius: 8; -fx-font-size: 13px;");
        Label descError = org.example.utils.FormValidator.createErrorLabel();
        descError.setId("descError");
        descBox.getChildren().addAll(descLabel, descArea, descError);

        // Photo URL + Browse button
        VBox photoBox = new VBox(5);
        Label photoLabel = new Label("Image de l'hôtel (fichier local ou URL)");
        photoLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #013220;");

        HBox photoRow = new HBox(8);
        photoRow.setAlignment(Pos.CENTER_LEFT);

        TextField photoField = new TextField(hotel != null ? hotel.getPhotoUrl() : "");
        photoField.setPromptText("Choisissez une image ou collez une URL...");
        photoField.setId("photoField");
        photoField.setStyle("-fx-padding: 10; -fx-background-radius: 8; -fx-font-size: 13px;");
        HBox.setHgrow(photoField, Priority.ALWAYS);

        Button browseBtn = new Button("Parcourir...");
        browseBtn.setStyle("-fx-background-color: #50C878; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 6 12; -fx-font-size: 11px; -fx-cursor: hand;");
        browseBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choisir une image d'hôtel");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
            );
            File file = fileChooser.showOpenDialog(photoRow.getScene().getWindow());
            if (file != null) {
                // Enregistrer le chemin absolu, qui sera chargé avec "file:" dans createHotelCard
                photoField.setText(file.getAbsolutePath());
            }
        });

        photoRow.getChildren().addAll(photoField, browseBtn);
        photoBox.getChildren().addAll(photoLabel, photoRow);

        // Info text
        Label infoLabel = new Label("* Champs obligatoires");
        infoLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px; -fx-font-style: italic;");

        container.getChildren().addAll(nomBox, adresseBox, villeBox, etoilesBox, budgetBox, descBox, photoBox, infoLabel);

        // Wrap in ScrollPane
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #F5F3E7; -fx-background-color: #F5F3E7;");
        scrollPane.setPrefHeight(500);
        scrollPane.setMaxHeight(500);

        return scrollPane;
    }

    private Hotel getHotelFromForm(ScrollPane scrollPane, Hotel existingHotel) {
        VBox container = (VBox) scrollPane.getContent();
        TextField nomField = (TextField) container.lookup("#nomField");
        TextField adresseField = (TextField) container.lookup("#adresseField");
        TextField villeField = (TextField) container.lookup("#villeField");
        Spinner<Integer> etoilesSpinner = (Spinner<Integer>) container.lookup("#etoilesSpinner");
        TextField budgetField = (TextField) container.lookup("#budgetField");
        TextArea descArea = (TextArea) container.lookup("#descArea");
        TextField photoField = (TextField) container.lookup("#photoField");

        Label nomError = (Label) container.lookup("#nomError");
        Label adresseError = (Label) container.lookup("#adresseError");
        Label villeError = (Label) container.lookup("#villeError");
        Label budgetError = (Label) container.lookup("#budgetError");
        Label descError = (Label) container.lookup("#descError");

        // Validation
        boolean valid = true;
        valid &= org.example.utils.FormValidator.validateRequired(nomField, nomError, "Le nom");
        valid &= org.example.utils.FormValidator.validateRequired(adresseField, adresseError, "L'adresse");
        valid &= org.example.utils.FormValidator.validateRequired(villeField, villeError, "La ville");
        valid &= org.example.utils.FormValidator.validateDouble(budgetField, budgetError, "Le budget");

        if (descArea.getText() == null || descArea.getText().trim().isEmpty()) {
            org.example.utils.FormValidator.showError(descError, "La description est obligatoire");
            descArea.setStyle("-fx-border-color: #E74C3C; -fx-border-width: 2;");
            valid = false;
        } else {
            org.example.utils.FormValidator.hideError(descError);
            descArea.setStyle("");
        }

        if (!valid) {
            return null;
        }

        if (existingHotel != null) {
            existingHotel.setNom(nomField.getText().trim());
            existingHotel.setAdresse(adresseField.getText().trim());
            existingHotel.setVille(villeField.getText().trim());
            existingHotel.setNombreEtoiles(etoilesSpinner.getValue());
            existingHotel.setBudget(Double.parseDouble(budgetField.getText().trim()));
            existingHotel.setDescription(descArea.getText().trim());
            existingHotel.setPhotoUrl(photoField.getText().trim());
            return existingHotel;
        } else {
            return new Hotel(
                nomField.getText().trim(),
                adresseField.getText().trim(),
                villeField.getText().trim(),
                etoilesSpinner.getValue(),
                descArea.getText().trim(),
                photoField.getText().trim(),
                Double.parseDouble(budgetField.getText().trim())
            );
        }
    }

    @FXML
    public void searchHotels() {
        String searchText = searchField.getText().toLowerCase();
        if (searchText.isEmpty()) {
            displayHotelCards();
            return;
        }

        hotelCardsContainer.getChildren().clear();
        for (Hotel hotel : hotelList) {
            if (hotel.getNom().toLowerCase().contains(searchText) ||
                hotel.getVille().toLowerCase().contains(searchText)) {
                hotelCardsContainer.getChildren().add(createHotelCard(hotel));
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

