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
            // Try the correct resource paths
            String[] candidates = {
                "/org/example/images/home-hero5.png",
                "/images/home-hero.jpg",
                "/images/home-hero.png"
            };
            for (String path : candidates) {
                var res = getClass().getResource(path);
                if (res != null) {
                    bgImageView.setImage(new Image(res.toExternalForm(), true));
                    break;
                }
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
        // ── CARD SHELL ──────────────────────────────────────────────────────
        VBox card = new VBox(0);
        card.setPrefWidth(300);
        card.setMaxWidth(300);
        final String cardBase =
            "-fx-background-color: rgba(8,20,13,0.82);" +
            "-fx-background-radius: 16;" +
            "-fx-border-radius: 16;" +
            "-fx-border-color: rgba(80,200,120,0.22);" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.50), 18, 0, 0, 6);"
            + "-fx-cursor: hand;";
        final String cardHover =
            "-fx-background-color: rgba(10,28,17,0.92);" +
            "-fx-background-radius: 16;" +
            "-fx-border-radius: 16;" +
            "-fx-border-color: rgba(80,200,120,0.65);" +
            "-fx-border-width: 1.5;" +
            "-fx-effect: dropshadow(gaussian, rgba(80,200,120,0.25), 22, 0, 0, 8);" +
            "-fx-cursor: hand;";
        card.setStyle(cardBase);
        card.setOnMouseEntered(e -> card.setStyle(cardHover));
        card.setOnMouseExited(e -> card.setStyle(cardBase));

        // ── CONTENT PADDING ─────────────────────────────────────────────────
        VBox inner = new VBox(14);
        inner.setPadding(new Insets(18, 18, 16, 18));

        // ── HEADER: type + capacity icon ──────────────────────────────────
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        // Type badge
        Label typePill = new Label("🛏  " + chambre.getType());
        typePill.setStyle(
            "-fx-text-fill: white;" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 4, 0, 0, 1);");
        HBox.setHgrow(typePill, Priority.ALWAYS);

        Label capBadge = new Label("👥 " + chambre.getCapacite());
        capBadge.setStyle(
            "-fx-background-color: rgba(80,200,120,0.16);" +
            "-fx-text-fill: #50C878;" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 3 10;" +
            "-fx-background-radius: 20;");

        header.getChildren().addAll(typePill, capBadge);

        // ── HOTEL BADGE ──────────────────────────────────────────────────
        String hotelName = getHotelName(chambre.getHotelId());
        Label hotelLabel = new Label("🏨  " + hotelName);
        hotelLabel.setStyle(
            "-fx-background-color: rgba(60,150,100,0.12);" +
            "-fx-text-fill: rgba(140,210,170,0.85);" +
            "-fx-padding: 4 12;" +
            "-fx-background-radius: 10;" +
            "-fx-font-size: 11px;");

        // ── THIN SEPARATOR ────────────────────────────────────────────────
        Region sep1 = new Region();
        sep1.setPrefHeight(1);
        sep1.setStyle("-fx-background-color: rgba(80,200,120,0.10);");

        // ── EQUIPEMENTS ──────────────────────────────────────────────────
        Label equipLabel = new Label("✦  " + (chambre.getEquipements() != null ? chambre.getEquipements() : ""));
        equipLabel.setStyle(
            "-fx-text-fill: rgba(180,220,200,0.72);" +
            "-fx-font-size: 11px;" +
            "-fx-line-spacing: 2;");
        equipLabel.setWrapText(true);
        equipLabel.setMaxHeight(42);

        // ── PRICING BLOCK ────────────────────────────────────────────────
        VBox priceBlock = new VBox(6);
        priceBlock.setStyle(
            "-fx-background-color: rgba(255,255,255,0.04);" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 10 14;");

        priceBlock.getChildren().addAll(
            buildPriceRow("Standard",       chambre.getPrixStandard(),      "#50C878"),
            buildPriceRow("Haute saison",   chambre.getPrixHauteSaison(),   "#FFA040"),
            buildPriceRow("Basse saison",   chambre.getPrixBasseSaison(),   "rgba(180,220,200,0.75)")
        );

        // ── AVAILABILITY BADGE ────────────────────────────────────────────
        Label availBadge = new Label();
        try {
            LocalDate today = LocalDate.now();
            boolean dispo = serviceAutoAssign.isChambreDisponible(chambre.getId(), today, today.plusDays(1));
            if (dispo) {
                availBadge.setText("✅  Disponible");
                availBadge.setStyle(
                    "-fx-background-color: rgba(80,200,120,0.14);" +
                    "-fx-text-fill: #50C878;" +
                    "-fx-padding: 4 14;" +
                    "-fx-background-radius: 20;" +
                    "-fx-font-size: 11px;" +
                    "-fx-font-weight: bold;");
            } else {
                availBadge.setText("🔴  Occupée");
                availBadge.setStyle(
                    "-fx-background-color: rgba(220,70,50,0.14);" +
                    "-fx-text-fill: rgba(255,130,110,0.90);" +
                    "-fx-padding: 4 14;" +
                    "-fx-background-radius: 20;" +
                    "-fx-font-size: 11px;" +
                    "-fx-font-weight: bold;");
            }
        } catch (Exception ex) {
            availBadge.setText("⚪  Statut inconnu");
            availBadge.setStyle(
                "-fx-background-color: rgba(120,160,140,0.10);" +
                "-fx-text-fill: rgba(160,210,180,0.70);" +
                "-fx-padding: 4 14;" +
                "-fx-background-radius: 20;" +
                "-fx-font-size: 11px;");
        }

        // ── THIN SEPARATOR ────────────────────────────────────────────────
        Region sep2 = new Region();
        sep2.setPrefHeight(1);
        sep2.setStyle("-fx-background-color: rgba(80,200,120,0.10);");

        // ── FOOTER: action buttons ────────────────────────────────────────
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(2, 0, 0, 0));

        String btnEdit =
            "-fx-background-color: rgba(255,255,255,0.07);" +
            "-fx-text-fill: #6bcf9a;" +
            "-fx-font-size: 15px;" +
            "-fx-padding: 6 12;" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;";
        String btnDel =
            "-fx-background-color: rgba(255,80,60,0.10);" +
            "-fx-text-fill: rgba(255,130,110,0.85);" +
            "-fx-font-size: 15px;" +
            "-fx-padding: 6 12;" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;";

        Button editBtn = new Button("✎");
        editBtn.setStyle(btnEdit);
        editBtn.setOnMouseEntered(e -> editBtn.setStyle(btnEdit.replace("0.07", "0.15")));
        editBtn.setOnMouseExited(e -> editBtn.setStyle(btnEdit));
        editBtn.setOnAction(e -> editChambre(chambre));

        Button deleteBtn = new Button("🗑");
        deleteBtn.setStyle(btnDel);
        deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle(btnDel.replace("0.10", "0.22")));
        deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle(btnDel));
        deleteBtn.setOnAction(e -> deleteChambre(chambre));

        footer.getChildren().addAll(editBtn, deleteBtn);

        inner.getChildren().addAll(header, hotelLabel, sep1, equipLabel, priceBlock, availBadge, sep2, footer);
        card.getChildren().add(inner);
        return card;
    }

    /** Helper — one price row inside the pricing block */
    private HBox buildPriceRow(String label, double price, String valueColor) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label + " :");
        lbl.setStyle("-fx-text-fill: rgba(140,200,170,0.65); -fx-font-size: 11px;");
        HBox.setHgrow(lbl, Priority.ALWAYS);
        Label val = new Label(String.format("%.0f DT", price));
        val.setStyle("-fx-text-fill: " + valueColor + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        row.getChildren().addAll(lbl, val);
        return row;
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
        dialog.setHeaderText(null);

        // Dark glassmorphism dialog styling — inline, no CSS dependency
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle(
            "-fx-background-color: rgba(8,20,13,0.98);" +
            "-fx-border-color: rgba(80,200,120,0.30);" +
            "-fx-border-width: 1.5;");
        java.net.URL cssUrl = getClass().getResource("/styles/unified-styles.css");
        if (cssUrl != null) dialogPane.getStylesheets().add(cssUrl.toExternalForm());

        ButtonType saveButtonType = new ButtonType("💾  Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("✕  Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().addAll(saveButtonType, cancelButtonType);

        Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
        saveButton.setStyle(
            "-fx-background-color: linear-gradient(to right,#1a8f4e,#22b860);" +
            "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 22;" +
            "-fx-background-radius: 12; -fx-cursor: hand;");
        Button cancelButton = (Button) dialogPane.lookupButton(cancelButtonType);
        cancelButton.setStyle(
            "-fx-background-color: rgba(200,60,50,0.15);" +
            "-fx-text-fill: rgba(255,130,110,0.90); -fx-padding: 8 22;" +
            "-fx-background-radius: 12; -fx-cursor: hand;");

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
        dialog.setTitle("Modifier: " + chambre.getType());
        dialog.setHeaderText(null);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle(
            "-fx-background-color: rgba(8,20,13,0.98);" +
            "-fx-border-color: rgba(80,200,120,0.30);" +
            "-fx-border-width: 1.5;");
        java.net.URL cssUrl = getClass().getResource("/styles/unified-styles.css");
        if (cssUrl != null) dialogPane.getStylesheets().add(cssUrl.toExternalForm());

        ButtonType saveButtonType = new ButtonType("💾  Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("✕  Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().addAll(saveButtonType, cancelButtonType);

        Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
        saveButton.setStyle(
            "-fx-background-color: linear-gradient(to right,#1a8f4e,#22b860);" +
            "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 22;" +
            "-fx-background-radius: 12; -fx-cursor: hand;");
        Button cancelButton = (Button) dialogPane.lookupButton(cancelButtonType);
        cancelButton.setStyle(
            "-fx-background-color: rgba(200,60,50,0.15);" +
            "-fx-text-fill: rgba(255,130,110,0.90); -fx-padding: 8 22;" +
            "-fx-background-radius: 12; -fx-cursor: hand;");

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
        container.setStyle(
            "-fx-background-color: rgba(8,20,13,0.96);" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: rgba(80,200,120,0.18);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 16;");

        Label titleLabel = new Label("✦ Détails de la Chambre");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #50C878; -fx-padding: 0 0 6 0;");

        // Type
        VBox typeBox = new VBox(5);
        Label typeLabel = new Label("Type de chambre *");
        typeLabel.setStyle("-fx-text-fill: rgba(180,220,195,0.82); -fx-font-size: 11.5px; -fx-font-weight: bold;");
        TextField typeField = new TextField(chambre != null ? chambre.getType() : "");
        typeField.setPromptText("Ex: Suite, Standard, Deluxe...");
        typeField.setId("typeField");
        typeField.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-text-fill: #e8f5ec; -fx-prompt-text-fill: rgba(255,255,255,0.28); -fx-border-color: rgba(80,200,120,0.25); -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 12; -fx-font-size: 13px;");
        Label typeError = org.example.utils.FormValidator.createErrorLabel();
        typeError.setId("typeError");
        typeBox.getChildren().addAll(typeLabel, typeField, typeError);

        // Capacité
        VBox capaciteBox = new VBox(5);
        Label capaciteLabel = new Label("Capacité (personnes) *");
        capaciteLabel.setStyle("-fx-text-fill: rgba(180,220,195,0.82); -fx-font-size: 11.5px; -fx-font-weight: bold;");
        Spinner<Integer> capaciteSpinner = new Spinner<>(1, 10, chambre != null ? chambre.getCapacite() : 2);
        capaciteSpinner.setId("capaciteSpinner");
        capaciteSpinner.setEditable(true);
        capaciteSpinner.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-border-color: rgba(80,200,120,0.25); -fx-border-radius: 8; -fx-background-radius: 8;");
        capaciteBox.getChildren().addAll(capaciteLabel, capaciteSpinner);

        // Équipements
        VBox equipBox = new VBox(5);
        Label equipLabel = new Label("Équipements *");
        equipLabel.setStyle("-fx-text-fill: rgba(180,220,195,0.82); -fx-font-size: 11.5px; -fx-font-weight: bold;");
        TextField equipField = new TextField(chambre != null ? chambre.getEquipements() : "");
        equipField.setPromptText("Ex: WiFi, TV, Climatisation, Balcon");
        equipField.setId("equipField");
        equipField.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-text-fill: #e8f5ec; -fx-prompt-text-fill: rgba(255,255,255,0.28); -fx-border-color: rgba(80,200,120,0.25); -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 12; -fx-font-size: 13px;");
        Label equipError = org.example.utils.FormValidator.createErrorLabel();
        equipError.setId("equipError");
        equipBox.getChildren().addAll(equipLabel, equipField, equipError);

        // Hôtel
        VBox hotelBox = new VBox(5);
        Label hotelLabel = new Label("Hôtel *");
        hotelLabel.setStyle("-fx-text-fill: rgba(180,220,195,0.82); -fx-font-size: 11.5px; -fx-font-weight: bold;");
        ComboBox<String> hotelCombo = new ComboBox<>();
        hotelCombo.setPromptText("Sélectionnez un hôtel");
        for (Hotel h : hotelList) {
            hotelCombo.getItems().add(h.getId() + " - " + h.getNom());
        }
        if (chambre != null) {
            hotelCombo.setValue(chambre.getHotelId() + " - " + getHotelName(chambre.getHotelId()));
        }
        hotelCombo.setId("hotelCombo");
        hotelCombo.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-text-fill: #e8f5ec; -fx-prompt-text-fill: rgba(255,255,255,0.28); -fx-border-color: rgba(80,200,120,0.25); -fx-border-radius: 8; -fx-background-radius: 8;");
        hotelCombo.setPrefWidth(430);
        Label hotelError = org.example.utils.FormValidator.createErrorLabel();
        hotelError.setId("hotelError");
        hotelBox.getChildren().addAll(hotelLabel, hotelCombo, hotelError);

        // Prix Standard
        VBox prixStdBox = new VBox(5);
        Label prixStdLabel = new Label("Prix Standard (DT) *");
        prixStdLabel.setStyle("-fx-text-fill: rgba(180,220,195,0.82); -fx-font-size: 11.5px; -fx-font-weight: bold;");
        TextField prixStdField = new TextField(chambre != null ? String.valueOf(chambre.getPrixStandard()) : "");
        prixStdField.setPromptText("Ex: 150.00");
        prixStdField.setId("prixStdField");
        prixStdField.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-text-fill: #e8f5ec; -fx-prompt-text-fill: rgba(255,255,255,0.28); -fx-border-color: rgba(80,200,120,0.25); -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 12; -fx-font-size: 13px;");
        Label prixStdError = org.example.utils.FormValidator.createErrorLabel();
        prixStdError.setId("prixStdError");
        prixStdBox.getChildren().addAll(prixStdLabel, prixStdField, prixStdError);

        // Prix Haute Saison
        VBox prixHauteBox = new VBox(5);
        Label prixHauteLabel = new Label("Prix Haute Saison (DT) *");
        prixHauteLabel.setStyle("-fx-text-fill: rgba(180,220,195,0.82); -fx-font-size: 11.5px; -fx-font-weight: bold;");
        TextField prixHauteField = new TextField(chambre != null ? String.valueOf(chambre.getPrixHauteSaison()) : "");
        prixHauteField.setPromptText("Ex: 200.00");
        prixHauteField.setId("prixHauteField");
        prixHauteField.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-text-fill: #e8f5ec; -fx-prompt-text-fill: rgba(255,255,255,0.28); -fx-border-color: rgba(80,200,120,0.25); -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 12; -fx-font-size: 13px;");
        Label prixHauteError = org.example.utils.FormValidator.createErrorLabel();
        prixHauteError.setId("prixHauteError");
        prixHauteBox.getChildren().addAll(prixHauteLabel, prixHauteField, prixHauteError);

        // Prix Basse Saison
        VBox prixBasseBox = new VBox(5);
        Label prixBasseLabel = new Label("Prix Basse Saison (DT) *");
        prixBasseLabel.setStyle("-fx-text-fill: rgba(180,220,195,0.82); -fx-font-size: 11.5px; -fx-font-weight: bold;");
        TextField prixBasseField = new TextField(chambre != null ? String.valueOf(chambre.getPrixBasseSaison()) : "");
        prixBasseField.setPromptText("Ex: 120.00");
        prixBasseField.setId("prixBasseField");
        prixBasseField.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-text-fill: #e8f5ec; -fx-prompt-text-fill: rgba(255,255,255,0.28); -fx-border-color: rgba(80,200,120,0.25); -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 12; -fx-font-size: 13px;");
        Label prixBasseError = org.example.utils.FormValidator.createErrorLabel();
        prixBasseError.setId("prixBasseError");
        prixBasseBox.getChildren().addAll(prixBasseLabel, prixBasseField, prixBasseError);

        // Info text
        Label infoLabel = new Label("* Champs obligatoires");
        infoLabel.setStyle("-fx-text-fill: rgba(150,200,170,0.55); -fx-font-size: 10.5px;");

        GridPane formGrid = new GridPane();
        formGrid.setHgap(16);
        formGrid.setVgap(16);
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
            org.example.utils.SceneNavigator.switchTo(fxmlPath, chambreCardsContainer);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de naviguer: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}

