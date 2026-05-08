package org.example.controllers;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.example.entities.*;
import org.example.services.*;

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

    @FXML private StackPane rootStack;
    @FXML private ImageView bgImageView;
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

    // ── New feature services ──────────────────────────────────
    private final ServiceCodePromo serviceCodePromo = new ServiceCodePromo();
    private final ServiceFidelite serviceFidelite    = new ServiceFidelite();
    private final ServiceListeAttente serviceListeAttente = new ServiceListeAttente();
    private final ServiceAutoAssignment serviceAutoAssign = new ServiceAutoAssignment();
    private final HolidayService holidayService          = new HolidayService();

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

        setupBackground();
        setupFilterStatut();
        loadData();
        loadReservations();
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
        VBox card = new VBox(15);
        card.setPrefWidth(350);
        card.getStyleClass().add("glass-card");
        card.setPadding(new Insets(20));

        // Status badge
        HBox statusRow = new HBox();
        statusRow.setAlignment(Pos.CENTER_RIGHT);
        Label statusLabel = new Label(reservation.getStatut());
        String statusColor = getStatutColor(reservation.getStatut());
        statusLabel.setStyle("-fx-background-color: " + statusColor + "33; -fx-text-fill: " + statusColor + "; " +
                            "-fx-padding: 5 15; -fx-background-radius: 15; -fx-font-size: 11px; -fx-font-weight: bold; " +
                            "-fx-border-color: " + statusColor + "; -fx-border-width: 0.5; -fx-border-radius: 15;");
        statusRow.getChildren().add(statusLabel);

        // Client info
        VBox clientInfo = new VBox(4);
        Label clientLabel = new Label(reservation.getUserDisplayName());
        clientLabel.getStyleClass().add("card-title");
        clientLabel.setStyle("-fx-font-size: 18px;");

        String emailValue = reservation.getUserEmail() != null ? reservation.getUserEmail() : "Email inconnu";
        Label emailLabel = new Label("📧 " + emailValue);
        emailLabel.setStyle("-fx-text-fill: #A0E0C9; -fx-font-size: 12px;");
        clientInfo.getChildren().addAll(clientLabel, emailLabel);

        Separator sep1 = new Separator();
        sep1.setOpacity(0.1);

        // Hotel & Chambre info
        String hotelName = getHotelName(reservation.getHotelId());
        String chambreType = getChambreType(reservation.getChambreId());

        VBox stayInfo = new VBox(6);
        Label hotelLabel = new Label("🏨 " + hotelName);
        hotelLabel.setStyle("-fx-text-fill: #50C878; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label chambreLabel = new Label("🛏️ Type: " + chambreType);
        chambreLabel.getStyleClass().add("card-description");
        stayInfo.getChildren().addAll(hotelLabel, chambreLabel);

        // Dates
        HBox datesBox = new HBox(10);
        datesBox.setAlignment(Pos.CENTER_LEFT);
        datesBox.setStyle("-fx-background-color: rgba(160,224,201,0.05); -fx-padding: 10; -fx-background-radius: 10;");

        VBox dateFrom = createDateBox("Arrivée", reservation.getDateDebut().toString());
        Label arrow = new Label("➜");
        arrow.setStyle("-fx-text-fill: #A0E0C9;");
        VBox dateTo = createDateBox("Départ", reservation.getDateFin().toString());

        long nights = ChronoUnit.DAYS.between(reservation.getDateDebut(), reservation.getDateFin());
        Label nightBadge = new Label(nights + "N");
        nightBadge.setStyle("-fx-background-color: rgba(80,200,120,0.2); -fx-text-fill: #50C878; " +
                           "-fx-padding: 2 6; -fx-background-radius: 5; -fx-font-size: 10px;");

        datesBox.getChildren().addAll(dateFrom, arrow, dateTo, nightBadge);

        // Price
        HBox priceRow = new HBox(10);
        priceRow.setAlignment(Pos.CENTER_LEFT);
        Label priceLabel = new Label("Total séjour :");
        priceLabel.setStyle("-fx-text-fill: #A0E0C9; -fx-font-size: 12px;");
        Label priceValue = new Label(String.format("%.2f DT", reservation.getPrixTotal()));
        priceValue.setStyle("-fx-text-fill: #50C878; -fx-font-size: 18px; -fx-font-weight: bold;");
        priceRow.getChildren().addAll(priceLabel, priceValue);

        // ── Loyalty points badge ──────────────────────────────
        HBox loyaltyRow = new HBox(8);
        loyaltyRow.setAlignment(Pos.CENTER_LEFT);
        try {
            ProgrammeFidelite pf = serviceFidelite.getOrCreate(reservation.getUserId());
            Label loyaltyLabel = new Label(pf.getStatutEmoji() + " | " + pf.getPoints() + " pts");
            loyaltyLabel.setStyle("-fx-background-color: rgba(80,200,120,0.1); " +
                                  "-fx-text-fill: " + pf.getStatutColor() + "; " +
                                  "-fx-padding: 3 10; -fx-background-radius: 12; -fx-font-size: 11px; " +
                                  "-fx-font-weight: bold;");
            int next = pf.getPointsVersProchainNiveau();
            if (next > 0) {
                Label nextLabel = new Label("+" + next + " pts → niveau suivant");
                nextLabel.setStyle("-fx-text-fill: #A0E0C9; -fx-font-size: 10px;");
                loyaltyRow.getChildren().addAll(loyaltyLabel, nextLabel);
            } else {
                loyaltyRow.getChildren().add(loyaltyLabel);
            }
        } catch (Exception ex) {
            Label loyaltyLabel = new Label("🥉 Bronze");
            loyaltyLabel.setStyle("-fx-text-fill: #CD7F32; -fx-font-size: 11px;");
            loyaltyRow.getChildren().add(loyaltyLabel);
        }

        // Action buttons
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = new Button("✎");
        editBtn.getStyleClass().add("card-action-btn");
        editBtn.setOnAction(e -> editReservation(reservation));

        Button cancelBtn = new Button("✕");
        cancelBtn.getStyleClass().add("card-action-btn-danger");
        cancelBtn.setTooltip(new Tooltip("Annuler la réservation"));
        cancelBtn.setOnAction(e -> cancelReservation(reservation));

        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().add("card-action-btn-danger");
        deleteBtn.setOnAction(e -> deleteReservation(reservation));

        // 🎟 QR Code Button
        Button qrBtn = new Button("🎟 QR");
        qrBtn.setStyle("-fx-background-color: rgba(80,200,120,0.2); -fx-text-fill: #50C878; " +
                       "-fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 10; -fx-cursor: hand;");
        qrBtn.setTooltip(new Tooltip("Afficher le QR Code de check-in"));
        qrBtn.setOnAction(e -> showQRCode(reservation));

        footer.getChildren().addAll(qrBtn, editBtn, cancelBtn, deleteBtn);

        card.getChildren().addAll(statusRow, clientInfo, sep1, stayInfo, datesBox, priceRow, loyaltyRow, footer);

        // Hover Effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-border-color: #50C878; -fx-border-width: 1; -fx-border-radius: 20;"));
        card.setOnMouseExited(e -> card.setStyle(""));

        return card;
    }

    private VBox createDateBox(String label, String date) {
        VBox box = new VBox(2);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #A0E0C9; -fx-font-size: 9px; -fx-text-transform: uppercase;");
        Label val = new Label(date);
        val.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
        box.getChildren().addAll(lbl, val);
        return box;
    }

    // ══════════════════════════════════════════════════════════════
    // 🎟 QR CODE — Check-in Scanner
    // ══════════════════════════════════════════════════════════════

    private void showQRCode(Reservation reservation) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("🎟 QR Code — Réservation #" + reservation.getId());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/styles/unified-styles.css").toExternalForm());

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: rgba(1,30,20,0.97);");

        // Title
        Label title = new Label("🎟 QR Code de Check-in");
        title.setStyle("-fx-text-fill: #50C878; -fx-font-size: 18px; -fx-font-weight: bold;");

        // Reservation info
        String hotelName = getHotelName(reservation.getHotelId());
        String chambreType = getChambreType(reservation.getChambreId());
        Label info = new Label("🏨 " + hotelName + "  |  🛏 " + chambreType + "\n" +
                               "📅 " + reservation.getDateDebut() + " → " + reservation.getDateFin());
        info.setStyle("-fx-text-fill: #A0E0C9; -fx-font-size: 12px; -fx-text-alignment: center;");
        info.setWrapText(true);
        info.setAlignment(Pos.CENTER);

        // QR Code image
        VBox qrContainer = new VBox(10);
        qrContainer.setAlignment(Pos.CENTER);
        qrContainer.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 15;");

        try {
            String qrContent = QRCodeService.buildReservationQRContent(
                reservation.getId(), reservation.getUserId(),
                hotelName, chambreType,
                reservation.getDateDebut().toString(), reservation.getDateFin().toString(),
                reservation.getPrixTotal()
            );
            WritableImage qrImage = QRCodeService.generateQRCode(qrContent, 220, 220);
            ImageView qrView = new ImageView(qrImage);
            qrView.setFitWidth(220);
            qrView.setFitHeight(220);
            qrView.setPreserveRatio(true);
            qrContainer.getChildren().add(qrView);
        } catch (Exception ex) {
            Label errorLabel = new Label("❌ Erreur génération QR: " + ex.getMessage());
            errorLabel.setStyle("-fx-text-fill: red;");
            qrContainer.getChildren().add(errorLabel);
        }

        // QR ID label
        Label qrId = new Label("GOVIBE-" + String.format("%06d", reservation.getId()));
        qrId.setStyle("-fx-text-fill: #50C878; -fx-font-size: 14px; -fx-font-family: monospace; " +
                      "-fx-font-weight: bold;");

        // Instruction
        Label instruction = new Label("📱 Présentez ce QR code à la réception lors du check-in");
        instruction.setStyle("-fx-text-fill: #A0E0C9; -fx-font-size: 11px;");
        instruction.setWrapText(true);
        instruction.setAlignment(Pos.CENTER);

        root.getChildren().addAll(title, info, qrContainer, qrId, instruction);
        dialog.getDialogPane().setContent(root);
        dialog.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════
    // 6️⃣ LISTE D'ATTENTE
    // ══════════════════════════════════════════════════════════════

    @FXML
    public void openWaitlistDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("⏳ Liste d'Attente");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/styles/unified-styles.css").toExternalForm());

        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: rgba(1,30,20,0.97);");
        root.setPrefWidth(560);

        Label title = new Label("⏳ Liste d'Attente Hôtel");
        title.setStyle("-fx-text-fill: #50C878; -fx-font-size: 18px; -fx-font-weight: bold;");

        // Form to add waitlist entry
        VBox formBox = new VBox(12);
        formBox.setStyle("-fx-background-color: rgba(80,200,120,0.05); -fx-padding: 15; -fx-background-radius: 10;");
        Label formTitle = new Label("Ajouter en liste d'attente:");
        formTitle.setStyle("-fx-text-fill: #A0E0C9; -fx-font-weight: bold;");

        ComboBox<String> userCombo = new ComboBox<>();
        userCombo.setPromptText("Sélectionner le client");
        userCombo.setPrefWidth(500);
        userCombo.getStyleClass().add("form-field");
        for (personne user : userList) {
            userCombo.getItems().add(user.getId() + " - " + user.getPrenom() + " " + user.getNom());
        }

        ComboBox<String> hotelWCombo = new ComboBox<>();
        hotelWCombo.setPromptText("Sélectionner l'hôtel");
        hotelWCombo.setPrefWidth(500);
        hotelWCombo.getStyleClass().add("form-field");
        for (Hotel h : hotelList) {
            hotelWCombo.getItems().add(h.getId() + " - " + h.getNom());
        }

        HBox datesRow = new HBox(15);
        DatePicker debutPicker = new DatePicker(LocalDate.now());
        DatePicker finPicker = new DatePicker(LocalDate.now().plusDays(3));
        debutPicker.setPromptText("Date début souhaitée");
        finPicker.setPromptText("Date fin souhaitée");
        debutPicker.setPrefWidth(240); finPicker.setPrefWidth(240);
        datesRow.getChildren().addAll(debutPicker, finPicker);

        HBox detailsRow = new HBox(15);
        Spinner<Integer> capaciteSpinner = new Spinner<>(1, 10, 2);
        capaciteSpinner.setEditable(true);
        capaciteSpinner.setPrefWidth(150);
        TextField budgetField = new TextField();
        budgetField.setPromptText("Budget max (DT, 0 = illimité)");
        budgetField.setPrefWidth(200);
        detailsRow.getChildren().addAll(
            new Label("👥"), capaciteSpinner, new Label("💰"), budgetField);
        detailsRow.setAlignment(Pos.CENTER_LEFT);
        detailsRow.getChildren().forEach(n -> {
            if (n instanceof Label) ((Label) n).setStyle("-fx-text-fill: #A0E0C9;");
        });

        Button addWaitBtn = new Button("✅ Ajouter en liste d'attente");
        addWaitBtn.getStyleClass().add("premium-button");
        addWaitBtn.setOnAction(e -> {
            if (userCombo.getValue() == null || hotelWCombo.getValue() == null) {
                showAlert("Erreur", "Veuillez sélectionner un client et un hôtel.", Alert.AlertType.WARNING);
                return;
            }
            int userId = Integer.parseInt(userCombo.getValue().split(" - ")[0]);
            int hotelId = Integer.parseInt(hotelWCombo.getValue().split(" - ")[0]);
            double budget = budgetField.getText().isBlank() ? 0 :
                Double.parseDouble(budgetField.getText().trim());
            ListeAttente la = new ListeAttente(userId, hotelId,
                capaciteSpinner.getValue(), budget,
                debutPicker.getValue(), finPicker.getValue());
            try {
                serviceListeAttente.ajouterAttente(la);
                showAlert("Succès", "✅ Client ajouté en liste d'attente !\nIl sera notifié dès qu'une chambre se libère.",
                    Alert.AlertType.INFORMATION);
            } catch (SQLException ex) {
                showAlert("Erreur", ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        formBox.getChildren().addAll(formTitle, userCombo, hotelWCombo, datesRow, detailsRow, addWaitBtn);

        // Current waitlist
        Label listTitle = new Label("👥 Clients en attente:");
        listTitle.setStyle("-fx-text-fill: #50C878; -fx-font-weight: bold;");

        VBox waitlistDisplay = new VBox(8);
        try {
            List<ListeAttente> attentes = serviceListeAttente.getAll();
            if (attentes.isEmpty()) {
                Label empty = new Label("Aucun client en attente");
                empty.setStyle("-fx-text-fill: #A0E0C9; -fx-font-size: 12px;");
                waitlistDisplay.getChildren().add(empty);
            } else {
                for (ListeAttente la : attentes) {
                    HBox row = new HBox(12);
                    row.setStyle("-fx-background-color: rgba(80,200,120,0.05); -fx-padding: 8; -fx-background-radius: 8;");
                    Label nameL = new Label(la.getUserName() != null ? la.getUserName() : "User #" + la.getUserId());
                    nameL.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                    Label hotelL = new Label("🏨 " + (la.getHotelNom() != null ? la.getHotelNom() : "#" + la.getHotelId()));
                    hotelL.setStyle("-fx-text-fill: #A0E0C9;");
                    Label statL = new Label(la.getStatutEmoji());
                    statL.setStyle("-fx-font-size: 11px;");
                    Label dateL = new Label("📅 " + la.getDateSouhaiteeDebut() + " → " + la.getDateSouhaiteeFin());
                    dateL.setStyle("-fx-text-fill: #A0E0C9; -fx-font-size: 10px;");
                    row.getChildren().addAll(nameL, hotelL, statL, dateL);
                    waitlistDisplay.getChildren().add(row);
                }
            }
        } catch (SQLException ex) {
            waitlistDisplay.getChildren().add(new Label("Erreur: " + ex.getMessage()));
        }

        ScrollPane waitScroll = new ScrollPane(waitlistDisplay);
        waitScroll.setFitToWidth(true);
        waitScroll.setPrefHeight(200);
        waitScroll.setStyle("-fx-background: transparent;");

        root.getChildren().addAll(title, formBox, listTitle, waitScroll);
        dialog.getDialogPane().setContent(root);
        dialog.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════
    // 7️⃣ PROGRAMME FIDÉLITÉ — Dialog
    // ══════════════════════════════════════════════════════════════

    @FXML
    public void openLoyaltyDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("🎯 Programme Fidélité GoVibe");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/styles/unified-styles.css").toExternalForm());

        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: rgba(1,30,20,0.97);");
        root.setPrefWidth(550);

        Label title = new Label("🎯 Programme Fidélité GoVibe");
        title.setStyle("-fx-text-fill: #50C878; -fx-font-size: 18px; -fx-font-weight: bold;");

        // Status legend
        VBox legend = new VBox(8);
        legend.setStyle("-fx-background-color: rgba(80,200,120,0.05); -fx-padding: 15; -fx-background-radius: 10;");
        Label legendTitle = new Label("Niveaux de fidélité:");
        legendTitle.setStyle("-fx-text-fill: #A0E0C9; -fx-font-weight: bold;");
        String[][] levels = {
            {"🥉", "Bronze",   "0 – 499 pts",    "Aucune réduction",       "#CD7F32"},
            {"🥈", "Silver",   "500 – 1499 pts",  "-5% sur réservations",   "#C0C0C0"},
            {"🥇", "Gold",     "1500 – 2999 pts", "-10% sur réservations",  "#FFD700"},
            {"💎", "Platinum", "3000+ pts",       "-15% + priorité",        "#B9F2FF"},
        };
        legend.getChildren().add(legendTitle);
        for (String[] lvl : levels) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            Label emoji = new Label(lvl[0] + " " + lvl[1]);
            emoji.setStyle("-fx-text-fill: " + lvl[4] + "; -fx-font-weight: bold; -fx-font-size: 13px;");
            Label pts = new Label(lvl[2]);
            pts.setStyle("-fx-text-fill: #A0E0C9; -fx-font-size: 11px;");
            Label reduction = new Label("→ " + lvl[3]);
            reduction.setStyle("-fx-text-fill: #50C878; -fx-font-size: 11px;");
            row.getChildren().addAll(emoji, pts, reduction);
            legend.getChildren().add(row);
        }

        // Client loyalty list
        Label listTitle = new Label("📊 Fidélité des clients:");
        listTitle.setStyle("-fx-text-fill: #50C878; -fx-font-weight: bold;");

        VBox clientList = new VBox(8);
        try {
            List<ProgrammeFidelite> allFidelite = serviceFidelite.getAll();
            if (allFidelite.isEmpty()) {
                clientList.getChildren().add(new Label("Aucun programme de fidélité actif"));
            } else {
                for (ProgrammeFidelite pf : allFidelite) {
                    HBox row = new HBox(15);
                    row.setStyle("-fx-background-color: rgba(80,200,120,0.05); -fx-padding: 10; " +
                                 "-fx-background-radius: 8;");
                    row.setAlignment(Pos.CENTER_LEFT);
                    Label nameL = new Label(pf.getUserName() != null ? pf.getUserName() : "User #" + pf.getUserId());
                    nameL.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 130;");
                    Label statL = new Label(pf.getStatutEmoji());
                    statL.setStyle("-fx-text-fill: " + pf.getStatutColor() + "; -fx-font-weight: bold;");
                    Label ptsL = new Label(pf.getPoints() + " pts");
                    ptsL.setStyle("-fx-text-fill: #50C878;");
                    Label resL = new Label("📋 " + pf.getTotalReservations() + " rés.");
                    resL.setStyle("-fx-text-fill: #A0E0C9; -fx-font-size: 11px;");
                    Label depL = new Label("💰 " + String.format("%.0f DT", pf.getTotalDepenses()));
                    depL.setStyle("-fx-text-fill: #A0E0C9; -fx-font-size: 11px;");
                    row.getChildren().addAll(nameL, statL, ptsL, resL, depL);
                    clientList.getChildren().add(row);
                }
            }
        } catch (Exception ex) {
            clientList.getChildren().add(new Label("Erreur: " + ex.getMessage()));
        }

        ScrollPane scroll = new ScrollPane(clientList);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(250);
        scroll.setStyle("-fx-background: transparent;");

        root.getChildren().addAll(title, legend, listTitle, scroll);
        dialog.getDialogPane().setContent(root);
        dialog.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════
    // 💡 CODE PROMO — Dialog
    // ══════════════════════════════════════════════════════════════

    @FXML
    public void openPromoCodesDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("💡 Codes Promotionnels");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/styles/unified-styles.css").toExternalForm());

        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: rgba(1,30,20,0.97);");
        root.setPrefWidth(500);

        Label title = new Label("💡 Codes Promotionnels");
        title.setStyle("-fx-text-fill: #50C878; -fx-font-size: 18px; -fx-font-weight: bold;");

        VBox codesList = new VBox(10);
        try {
            List<CodePromo> codes = serviceCodePromo.show();
            for (CodePromo cp : codes) {
                HBox row = new HBox(15);
                row.setAlignment(Pos.CENTER_LEFT);
                String bgColor = cp.isValide() ? "rgba(80,200,120,0.1)" : "rgba(200,80,80,0.1)";
                row.setStyle("-fx-background-color: " + bgColor + "; -fx-padding: 12; -fx-background-radius: 10;");

                Label codeLabel = new Label(cp.getCode());
                codeLabel.setStyle("-fx-text-fill: #50C878; -fx-font-weight: bold; -fx-font-size: 14px; " +
                                   "-fx-font-family: monospace; -fx-min-width: 120;");
                Label descLabel = new Label(cp.getDescription());
                descLabel.setStyle("-fx-text-fill: #A0E0C9; -fx-font-size: 11px;");
                Label valLabel = new Label(
                    cp.getType() == CodePromo.TypeRemise.POURCENTAGE
                        ? "-" + (int)cp.getValeur() + "%"
                        : "-" + (int)cp.getValeur() + " DT");
                valLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold; -fx-font-size: 13px;");
                Label statusLabel = new Label(cp.isValide() ? "✅ Actif" : "❌ Expiré");
                statusLabel.setStyle("-fx-text-fill: " + (cp.isValide() ? "#50C878" : "#D84E36") + "; -fx-font-size: 11px;");
                String expTxt = cp.getDateExpiration() != null ? "Exp: " + cp.getDateExpiration() : "";
                Label expLabel = new Label(expTxt);
                expLabel.setStyle("-fx-text-fill: #A0E0C9; -fx-font-size: 10px;");

                row.getChildren().addAll(codeLabel, descLabel, valLabel, statusLabel, expLabel);
                codesList.getChildren().add(row);
            }
        } catch (Exception ex) {
            codesList.getChildren().add(new Label("Erreur: " + ex.getMessage()));
        }

        ScrollPane scroll = new ScrollPane(codesList);
        scroll.setFitToWidth(true); scroll.setPrefHeight(300);
        scroll.setStyle("-fx-background: transparent;");

        root.getChildren().addAll(title, scroll);
        dialog.getDialogPane().setContent(root);
        dialog.showAndWait();
    }

    private String getStatutColor(String statut) {
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
        dialogPane.setStyle(
            "-fx-background-color: rgba(8,20,13,0.98);" +
            "-fx-border-color: rgba(80,200,120,0.28);" +
            "-fx-border-width: 1.5;");
        java.net.URL addCssUrl = getClass().getResource("/styles/unified-styles.css");
        if (addCssUrl != null) dialogPane.getStylesheets().add(addCssUrl.toExternalForm());

        ButtonType saveButtonType = new ButtonType("💾 Réserver", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("❌ Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        // Style buttons
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
                // 🎯 Award loyalty points
                try {
                    serviceFidelite.ajouterPointsReservation(reservation.getUserId(), reservation.getPrixTotal());
                } catch (Exception fEx) {
                    System.err.println("⚠️ Loyalty points error: " + fEx.getMessage());
                }
                // 💡 Increment promo code usage if applied
                ScrollPane sp = form;
                VBox cont = (VBox) sp.getContent();
                TextField pf = (TextField) cont.lookup("#promoField");
                if (pf != null && !pf.getText().isBlank()) {
                    try {
                        serviceCodePromo.incrementerUtilisation(pf.getText().trim().toUpperCase());
                    } catch (Exception pEx) {
                        System.err.println("⚠️ Promo increment error: " + pEx.getMessage());
                    }
                }
                loadReservations();
                showAlert("Succès", "✅ Réservation créée!\n🎯 Points de fidélité attribués automatiquement.", Alert.AlertType.INFORMATION);
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
        dialogPane.setStyle(
            "-fx-background-color: rgba(8,20,13,0.98);" +
            "-fx-border-color: rgba(80,200,120,0.28);" +
            "-fx-border-width: 1.5;");
        java.net.URL editCssUrl = getClass().getResource("/styles/unified-styles.css");
        if (editCssUrl != null) dialogPane.getStylesheets().add(editCssUrl.toExternalForm());

        ButtonType saveButtonType = new ButtonType("💾 Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("❌ Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        // Style buttons
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
            // 6️⃣ Check waitlist — notify first waiting client
            try {
                Chambre freedRoom = null;
                for (Chambre c : chambreList) {
                    if (c.getId() == reservation.getChambreId()) { freedRoom = c; break; }
                }
                if (freedRoom != null) {
                    String notifResult = serviceListeAttente.notifierPremierClient(
                        reservation.getHotelId(), freedRoom);
                    if (notifResult != null) {
                        String[] parts = notifResult.split("\\|");
                        String email = parts[0];
                        String name = parts.length > 1 ? parts[1] : "client";
                        showAlert("🔔 Liste d'attente",
                            "Un client en attente a été notifié :\n" +
                            "👤 " + name + "\n📧 " + email +
                            "\n\nIl peut maintenant confirmer sa réservation.",
                            Alert.AlertType.INFORMATION);
                    }
                }
            } catch (Exception wEx) {
                System.err.println("⚠️ Waitlist notification error: " + wEx.getMessage());
            }
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
        VBox container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setPrefWidth(550);
        container.setStyle(
            "-fx-background-color: rgba(8,20,13,0.96);" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: rgba(80,200,120,0.18);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 16;");

        Label titleLabel = new Label("✦ Détails de la Réservation");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #50C878; -fx-padding: 0 0 6 0;");

        // User
        VBox userBox = new VBox(5);
        Label userLabel = new Label("Utilisateur *");
        userLabel.setStyle("-fx-text-fill: rgba(180,220,195,0.82); -fx-font-size: 11.5px; -fx-font-weight: bold;");
        ComboBox<String> userCombo = new ComboBox<>();
        userCombo.setPromptText("Sélectionnez un utilisateur");
        for (personne user : userList) {
            userCombo.getItems().add(formatUserOption(user));
        }
        if (reservation != null) {
            userCombo.setValue(findUserOption(reservation.getUserId()));
        }
        userCombo.setId("userCombo");
        userCombo.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-text-fill: #e8f5ec; -fx-prompt-text-fill: rgba(255,255,255,0.28); -fx-border-color: rgba(80,200,120,0.25); -fx-border-radius: 8; -fx-background-radius: 8;");
        userCombo.setPrefWidth(500);
        Label userError = org.example.utils.FormValidator.createErrorLabel();
        userError.setId("userError");
        userBox.getChildren().addAll(userLabel, userCombo, userError);

        // Hotel
        VBox hotelBox = new VBox(5);
        Label hotelLabel = new Label("Hôtel *");
        hotelLabel.setStyle("-fx-text-fill: rgba(180,220,195,0.82); -fx-font-size: 11.5px; -fx-font-weight: bold;");
        ComboBox<String> hotelCombo = new ComboBox<>();
        hotelCombo.setPromptText("Sélectionnez un hôtel");
        for (Hotel h : hotelList) {
            hotelCombo.getItems().add(h.getId() + " - " + h.getNom());
        }
        if (reservation != null) {
            hotelCombo.setValue(reservation.getHotelId() + " - " + getHotelName(reservation.getHotelId()));
        }
        hotelCombo.setId("hotelCombo");
        hotelCombo.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-text-fill: #e8f5ec; -fx-prompt-text-fill: rgba(255,255,255,0.28); -fx-border-color: rgba(80,200,120,0.25); -fx-border-radius: 8; -fx-background-radius: 8;");
        hotelCombo.setPrefWidth(500);
        Label hotelError = org.example.utils.FormValidator.createErrorLabel();
        hotelError.setId("hotelError");
        hotelBox.getChildren().addAll(hotelLabel, hotelCombo, hotelError);

        // Chambre
        VBox chambreBox = new VBox(5);
        Label chambreLabel = new Label("Chambre *");
        chambreLabel.setStyle("-fx-text-fill: rgba(180,220,195,0.82); -fx-font-size: 11.5px; -fx-font-weight: bold;");
        ComboBox<String> chambreCombo = new ComboBox<>();
        chambreCombo.setPromptText("Sélectionnez d'abord un hôtel");
        chambreCombo.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-text-fill: #e8f5ec; -fx-prompt-text-fill: rgba(255,255,255,0.28); -fx-border-color: rgba(80,200,120,0.25); -fx-border-radius: 8; -fx-background-radius: 8;");

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
        chambreCombo.getStyleClass(); // id set above
        chambreCombo.setPrefWidth(500);
        Label chambreError = org.example.utils.FormValidator.createErrorLabel();
        chambreError.setId("chambreError");
        chambreBox.getChildren().addAll(chambreLabel, chambreCombo, chambreError);

        // Date Début
        VBox dateDebutBox = new VBox(5);
        Label dateDebutLabel = new Label("Date de début *");
        dateDebutLabel.setStyle("-fx-text-fill: rgba(180,220,195,0.82); -fx-font-size: 11.5px; -fx-font-weight: bold;");
        DatePicker dateDebutPicker = new DatePicker(reservation != null ? reservation.getDateDebut() : LocalDate.now());
        dateDebutPicker.setId("dateDebutPicker");
        dateDebutPicker.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-border-color: rgba(80,200,120,0.25); -fx-border-radius: 8; -fx-background-radius: 8;");
        dateDebutPicker.setPrefWidth(500);
        Label dateDebutError = org.example.utils.FormValidator.createErrorLabel();
        dateDebutError.setId("dateDebutError");
        dateDebutBox.getChildren().addAll(dateDebutLabel, dateDebutPicker, dateDebutError);

        // Date Fin
        VBox dateFinBox = new VBox(5);
        Label dateFinLabel = new Label("Date de fin *");
        dateFinLabel.setStyle("-fx-text-fill: rgba(180,220,195,0.82); -fx-font-size: 11.5px; -fx-font-weight: bold;");
        DatePicker dateFinPicker = new DatePicker(reservation != null ? reservation.getDateFin() : LocalDate.now().plusDays(1));
        dateFinPicker.setId("dateFinPicker");
        dateFinPicker.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-border-color: rgba(80,200,120,0.25); -fx-border-radius: 8; -fx-background-radius: 8;");
        dateFinPicker.setPrefWidth(500);
        Label dateFinError = org.example.utils.FormValidator.createErrorLabel();
        dateFinError.setId("dateFinError");
        dateFinBox.getChildren().addAll(dateFinLabel, dateFinPicker, dateFinError);

        // Prix Total
        VBox prixBox = new VBox(5);
        Label prixLabel = new Label("Prix total (DT) *");
        prixLabel.setStyle("-fx-text-fill: rgba(180,220,195,0.82); -fx-font-size: 11.5px; -fx-font-weight: bold;");
        TextField prixField = new TextField(reservation != null ? String.valueOf(reservation.getPrixTotal()) : "");
        prixField.setPromptText("Ex: 450.00");
        prixField.setId("prixField");
        prixField.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-text-fill: #e8f5ec; -fx-prompt-text-fill: rgba(255,255,255,0.28); -fx-border-color: rgba(80,200,120,0.25); -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 12; -fx-font-size: 13px;");
        Label prixError = org.example.utils.FormValidator.createErrorLabel();
        prixError.setId("prixError");
        prixBox.getChildren().addAll(prixLabel, prixField, prixError);

        // 💡 Code Promo
        VBox promoBox = new VBox(5);
        Label promoLabel = new Label("💡 Code Promo (optionnel)");
        promoLabel.setStyle("-fx-text-fill: rgba(180,220,195,0.82); -fx-font-size: 11.5px; -fx-font-weight: bold;");
        HBox promoRow = new HBox(10);
        promoRow.setAlignment(Pos.CENTER_LEFT);
        TextField promoField = new TextField();
        promoField.setPromptText("Ex: HOTEL10, SUMMER2026...");
        promoField.setId("promoField");
        promoField.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-text-fill: #e8f5ec; -fx-prompt-text-fill: rgba(255,255,255,0.28); -fx-border-color: rgba(80,200,120,0.25); -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 12;");
        promoField.setPrefWidth(300);
        HBox.setHgrow(promoField, Priority.ALWAYS);
        Button applyPromoBtn = new Button("Appliquer");
        applyPromoBtn.setStyle("-fx-background-color: rgba(80,200,120,0.2); -fx-text-fill: #50C878; " +
                               "-fx-padding: 6 12; -fx-background-radius: 8; -fx-cursor: hand;");
        Label promoResultLabel = new Label();
        promoResultLabel.setId("promoResultLabel");
        promoResultLabel.setStyle("-fx-font-size: 11px;");
        promoRow.getChildren().addAll(promoField, applyPromoBtn);
        promoBox.getChildren().addAll(promoLabel, promoRow, promoResultLabel);
        promoBox.setId("promoBox");

        // Apply promo action
        applyPromoBtn.setOnAction(e -> {
            String code = promoField.getText().trim().toUpperCase();
            if (code.isBlank()) return;
            try {
                CodePromo cp = serviceCodePromo.validerCode(code);
                if (cp != null) {
                    double currentPrice = prixField.getText().isBlank() ? 0 :
                        Double.parseDouble(prixField.getText().trim());
                    double newPrice = cp.appliquerSur(currentPrice);
                    double remise = cp.calculerRemise(currentPrice);
                    prixField.setText(String.format("%.2f", newPrice));
                    promoResultLabel.setText("✅ Code valide! Réduction: -" + String.format("%.2f", remise) + " DT");
                    promoResultLabel.setStyle("-fx-text-fill: #50C878; -fx-font-size: 11px;");
                    promoField.setStyle("-fx-border-color: #50C878;");
                } else {
                    promoResultLabel.setText("❌ Code invalide ou expiré");
                    promoResultLabel.setStyle("-fx-text-fill: #D84E36; -fx-font-size: 11px;");
                    promoField.setStyle("-fx-border-color: #D84E36;");
                }
            } catch (Exception ex) {
                promoResultLabel.setText("⚠️ Erreur: " + ex.getMessage());
            }
        });

        // 🗓 Holiday Surcharge indicator
        VBox holidayBox = new VBox(5);
        holidayBox.setId("holidayBox");
        Label holidayInfo = new Label();
        holidayInfo.setId("holidayInfo");
        holidayInfo.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 11px; -fx-wrap-text: true;");
        holidayInfo.setWrapText(true);
        holidayBox.getChildren().add(holidayInfo);

        // 1️⃣ Auto-Assign button
        Button autoAssignBtn = new Button("🎯 Attribution automatique de chambre");
        autoAssignBtn.setStyle("-fx-background-color: rgba(80,200,120,0.15); -fx-text-fill: #50C878; " +
                               "-fx-padding: 8 15; -fx-background-radius: 10; -fx-cursor: hand; " +
                               "-fx-font-size: 11px;");
        autoAssignBtn.setId("autoAssignBtn");

        autoAssignBtn.setOnAction(e -> {
            if (hotelCombo.getValue() == null) {
                showAlert("Info", "Veuillez d'abord sélectionner un hôtel.", Alert.AlertType.INFORMATION);
                return;
            }
            if (dateDebutPicker.getValue() == null || dateFinPicker.getValue() == null) {
                showAlert("Info", "Veuillez sélectionner les dates.", Alert.AlertType.INFORMATION);
                return;
            }
            try {
                int hotelId = Integer.parseInt(hotelCombo.getValue().split(" - ")[0]);
                double budget = prixField.getText().isBlank() ? 0 : Double.parseDouble(prixField.getText().trim());
                int capacite = 1;

                Chambre best = serviceAutoAssign.attribuerChambreOptimale(
                    hotelId, capacite, budget,
                    dateDebutPicker.getValue(), dateFinPicker.getValue()
                );

                if (best != null) {
                    // Select in ComboBox
                    String chambreOption = best.getId() + " - " + best.getType() + " (" + best.getPrixStandard() + " DT)";
                    if (!chambreCombo.getItems().contains(chambreOption)) {
                        chambreCombo.getItems().clear();
                        try {
                            for (Chambre c : serviceChambre.show()) {
                                if (c.getHotelId() == hotelId) {
                                    chambreCombo.getItems().add(c.getId() + " - " + c.getType() + " (" + c.getPrixStandard() + " DT)");
                                }
                            }
                        } catch (Exception ex2) { /* ignore */ }
                    }
                    chambreCombo.setValue(chambreOption);

                    // Calculate total price
                    long nights = ChronoUnit.DAYS.between(dateDebutPicker.getValue(), dateFinPicker.getValue());
                    double basePrice = best.getPrixStandard() * nights;

                    // Holiday surcharge
                    double surcharge = holidayService.calculerSurcharge(
                        best.getPrixStandard(), dateDebutPicker.getValue(), dateFinPicker.getValue());
                    String holidaySummary = holidayService.buildHolidaySummary(
                        dateDebutPicker.getValue(), dateFinPicker.getValue());

                    if (holidaySummary != null) {
                        holidayInfo.setText("🗓 " + holidaySummary + "\n💰 Supplément jours fériés: +" +
                                           String.format("%.2f", surcharge) + " DT");
                    }

                    prixField.setText(String.format("%.2f", basePrice + surcharge));
                    showAlert("✅ Chambre attribuée",
                        "Chambre auto-sélectionnée:\n" + best.getType() + " (" + best.getCapacite() + " pers.)\n" +
                        "Prix: " + String.format("%.2f DT", basePrice + surcharge) +
                        (surcharge > 0 ? "\n\n" + holidaySummary : ""),
                        Alert.AlertType.INFORMATION);
                } else {
                    Alert noRoom = new Alert(Alert.AlertType.WARNING);
                    noRoom.setTitle("Hôtel complet");
                    noRoom.setContentText("⚠️ Aucune chambre disponible pour ces critères.\n\nVoulez-vous ajouter ce client à la liste d'attente ?");
                    noRoom.getButtonTypes().setAll(
                        new ButtonType("Oui, liste d'attente", ButtonBar.ButtonData.YES),
                        ButtonType.NO
                    );
                    Optional<ButtonType> answer = noRoom.showAndWait();
                    if (answer.isPresent() && answer.get().getButtonData() == ButtonBar.ButtonData.YES) {
                        openWaitlistDialog();
                    }
                }
            } catch (Exception ex) {
                showAlert("Erreur", "Attribution: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        // Also trigger holiday check when dates change
        Runnable checkHolidays = () -> {
            if (dateDebutPicker.getValue() != null && dateFinPicker.getValue() != null) {
                String summary = holidayService.buildHolidaySummary(
                    dateDebutPicker.getValue(), dateFinPicker.getValue());
                if (summary != null) {
                    holidayInfo.setText("🗓 " + summary);
                } else {
                    holidayInfo.setText("");
                }
            }
        };
        dateDebutPicker.setOnAction(e -> checkHolidays.run());
        dateFinPicker.setOnAction(e -> checkHolidays.run());

        // Statut
        VBox statutBox = new VBox(5);
        Label statutLabel = new Label("Statut *");
        statutLabel.setStyle("-fx-text-fill: rgba(180,220,195,0.82); -fx-font-size: 11.5px; -fx-font-weight: bold;");
        ComboBox<String> statutCombo = new ComboBox<>();
        statutCombo.getItems().addAll("EN_ATTENTE", "CONFIRMEE", "ANNULEE");
        statutCombo.setValue(reservation != null ? reservation.getStatut() : "EN_ATTENTE");
        statutCombo.setId("statutCombo");
        statutCombo.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-text-fill: #e8f5ec; -fx-border-color: rgba(80,200,120,0.25); -fx-border-radius: 8; -fx-background-radius: 8;");
        Label statutError = org.example.utils.FormValidator.createErrorLabel();
        statutError.setId("statutError");
        statutBox.getChildren().addAll(statutLabel, statutCombo, statutError);

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

        formGrid.add(userBox, 0, 0);
        formGrid.add(hotelBox, 1, 0);

        formGrid.add(chambreBox, 0, 1);
        formGrid.add(statutBox, 1, 1);

        formGrid.add(dateDebutBox, 0, 2);
        formGrid.add(dateFinBox, 1, 2);

        formGrid.add(prixBox, 0, 3);
        GridPane.setColumnSpan(prixBox, 2);

        // Add holiday info, promo row, auto-assign
        formGrid.add(holidayBox, 0, 4);
        GridPane.setColumnSpan(holidayBox, 2);

        formGrid.add(promoBox, 0, 5);
        GridPane.setColumnSpan(promoBox, 2);

        formGrid.add(autoAssignBtn, 0, 6);
        GridPane.setColumnSpan(autoAssignBtn, 2);

        container.getChildren().addAll(titleLabel, formGrid, infoLabel);

        // Wrap in ScrollPane
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setPrefHeight(780);
        scrollPane.setMaxHeight(780);

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

