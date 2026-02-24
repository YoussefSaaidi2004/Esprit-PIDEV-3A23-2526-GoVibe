package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.entities.Activite;
import org.example.entities.Session;
import org.example.services.ServiceActivite;
import org.example.services.ServiceSession;
import org.example.services.ServiceReservationSession;
import org.example.entities.personne;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class UserHomeController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label profileInitials;

    @FXML
    private javafx.scene.layout.TilePane activitiesPane;

    @FXML
    private MenuButton profileMenu;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> destinationCombo;

    @FXML
    private DatePicker datePicker;

    private personne currentUser;
    private final ServiceActivite serviceActivite = new ServiceActivite();
    private final ServiceSession serviceSession = new ServiceSession();
    private final ServiceReservationSession serviceReservation = new ServiceReservationSession();

    public void initData(personne user) {
        this.currentUser = user;
        welcomeLabel.setText("Bonjour, " + user.getPrenom() + " !");
        
        // Set profile initials
        String initials = "";
        if (user.getPrenom() != null && !user.getPrenom().isEmpty()) {
            initials += user.getPrenom().charAt(0);
        }
        if (user.getNom() != null && !user.getNom().isEmpty()) {
            initials += user.getNom().charAt(0);
        }
        if (initials.isEmpty()) {
            initials = "U";
        }
        profileInitials.setText(initials.toUpperCase());
        
        // Initialize destinations
        destinationCombo.getItems().addAll("Tunis", "Sousse", "Monastir", "Sfax", "Hammamet", "Tabarka");
    }

    @FXML
    public void initialize() {
        loadActivities();
    }

    private void loadActivities() {
        if (activitiesPane == null)
            return; // Not present in all FXMLs using this controller
        try {
            List<Activite> list = serviceActivite.getAll();
            activitiesPane.getChildren().clear();

            for (Activite a : list) {
                activitiesPane.getChildren().add(createActivityCard(a));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private javafx.scene.Node createActivityCard(Activite a) {
        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-cursor: hand;");
        card.setPrefWidth(280);

        // Image Mapping
        String imgPath = "/images/placeholder.png";
        String loc = a.getLocalisation();
        if (loc.contains("Medina"))
            imgPath = "/images/visite-medina-tunis.png";
        else if (loc.contains("Nabeul"))
            imgPath = "/images/atelier-poterie-nabeul.png";
        else if (loc.contains("Sidi Bou Said"))
            imgPath = "/images/balade-sidi-bou-said.png";
        else if (loc.contains("Ghar El Melh"))
            imgPath = "/images/kayak-ghar-el-melh.png";
        else if (loc.contains("Cap Bon"))
            imgPath = "/images/randonnee-cap-bon.png";
        else if (loc.contains("Degustation"))
            imgPath = "/images/degustation-huile-olive.png";

        try {
            var url = getClass().getResource(imgPath);
            if (url != null) {
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(
                        new javafx.scene.image.Image(url.toExternalForm()));
                iv.setFitWidth(250);
                iv.setPreserveRatio(true);
                card.getChildren().add(iv);
            }
        } catch (Exception e) {
            System.err.println("Could not load image: " + imgPath);
        }

        Label nameLbl = new Label(a.getName());
        nameLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #013220;");

        Label locLbl = new Label("📍 " + a.getLocalisation());
        locLbl.setStyle("-fx-text-fill: #555; -fx-font-size: 13px;");

        Label prixLbl = new Label(a.getPrix() + " DT / personne");
        prixLbl.setStyle("-fx-text-fill: #084E36; -fx-font-weight: bold; -fx-font-size: 14px;");

        card.getChildren().addAll(nameLbl, locLbl, prixLbl);

        // Click → ouvrir les sessions disponibles
        card.setOnMouseClicked(e -> openSessionsDialog(a));

        return card;
    }

    private void openSessionsDialog(Activite activite) {
        try {
            List<Session> sessions = serviceSession.getByActiviteId(activite.getId());

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Sessions — " + activite.getName());

            VBox root = new VBox(15);
            root.setPadding(new Insets(25));
            root.setStyle("-fx-background-color: #D1F2EB;");

            Label title = new Label("📅 Sessions disponibles pour : " + activite.getName());
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #013220;");
            root.getChildren().add(title);

            if (sessions.isEmpty()) {
                Label empty = new Label("Aucune session disponible pour le moment.");
                empty.setStyle("-fx-text-fill: #555; -fx-font-size: 14px;");
                root.getChildren().add(empty);
            } else {
                for (Session s : sessions) {
                    HBox row = new HBox(15);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(12));
                    row.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 6, 0, 0, 2);");

                    Label dateLbl = new Label("📅 " + s.getDate().toString());
                    dateLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #013220;");

                    Label heureLbl = new Label("🕐 " + s.getHeure().toString());
                    heureLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #084E36;");

                    Label placesLbl = new Label("🪑 " + s.getNbr_places_restant() + " places");
                    placesLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " +
                            (s.getNbr_places_restant() > 0 ? "#27ae60" : "#e74c3c") + ";");

                    Spinner<Integer> nbSpinner = new Spinner<>(1, Math.max(1, s.getNbr_places_restant()), 1);
                    nbSpinner.setPrefWidth(80);

                    Button reserveBtn = new Button("Réserver");
                    reserveBtn.setStyle("-fx-background-color: #084E36; -fx-text-fill: white; -fx-font-weight: bold; " +
                            "-fx-padding: 8 16; -fx-background-radius: 8; -fx-cursor: hand;");
                    reserveBtn.setDisable(s.getNbr_places_restant() <= 0);

                    reserveBtn.setOnAction(ev -> {
                        try {
                            int nb = nbSpinner.getValue();
                            String userRef = (currentUser != null) ? currentUser.getEmail() : "anonymous";
                            serviceReservation.reserver(s.getId_session(), nb, userRef);
                            Alert ok = new Alert(Alert.AlertType.INFORMATION,
                                    "✅ Réservation confirmée ! " + nb + " place(s) réservée(s).",
                                    ButtonType.OK);
                            ok.setHeaderText("Succès");
                            ok.showAndWait();
                            dialog.close();
                            loadActivities(); // refresh
                        } catch (SQLException ex) {
                            Alert err = new Alert(Alert.AlertType.ERROR,
                                    "❌ " + ex.getMessage(), ButtonType.OK);
                            err.setHeaderText("Erreur de réservation");
                            err.showAndWait();
                        }
                    });

                    row.getChildren().addAll(dateLbl, heureLbl, placesLbl, nbSpinner, reserveBtn);
                    root.getChildren().add(row);
                }
            }

            ScrollPane scroll = new ScrollPane(root);
            scroll.setFitToWidth(true);
            scroll.setStyle("-fx-background-color: #D1F2EB;");

            dialog.setScene(new Scene(scroll, 700, 450));
            dialog.showAndWait();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openProposeActivity() {
        SceneNavigator.switchTo("/ProposeActivity.fxml", welcomeLabel);
    }

    @FXML
    private void handleLogout() {
        try {
            SessionManager.clear();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setTitle("GoVibe - Connexion");
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLocations() {
        org.example.mains.MainApp.switchScene("/LocationListView.fxml", "Voitures");
    }

    @FXML
    private void handleFlights() {
        System.out.println("[Nav] UserHome -> /views/user-dashboard.fxml");
        org.example.mains.MainApp.switchScene("/views/user-dashboard.fxml", "User Dashboard");
    }

    @FXML
    private void handleActivities() {
        SceneNavigator.switchTo("/UserHome.fxml", welcomeLabel);
    }

    @FXML
    private void handleChambres() {
        org.example.mains.MainApp.switchScene("/views/room-booking.fxml", "Chambres Disponibles");
    }

    @FXML
    private void handleHome() {
        System.out.println("[Nav] Home clicked - navigating to UserHomeView.fxml");
        org.example.mains.MainApp.switchScene("/org/example/UserHomeView.fxml", "Accueil");
    }

    @FXML
    private void handleProfile() {
        System.out.println("[Nav] Profile clicked");
        showInfoAlert("Profil", "Fonctionnalité à venir : Gestion du profil utilisateur");
    }

    @FXML
    private void handleMyReservations() {
        System.out.println("[Nav] My Reservations clicked");
        SceneNavigator.switchTo("/views/room-booking.fxml", welcomeLabel);
    }

    // ✅ NOUVEAU : Navigation vers les réservations d'activités
    @FXML
    private void handleMyActivityReservations() {
        System.out.println("[Nav] My Activity Reservations clicked");
        org.example.mains.MainApp.switchScene("/views/user-activity-reservations.fxml", "Mes Réservations d'Activités");
    }

    @FXML
    private void handleMyLocations() {
        System.out.println("[Nav] My Locations clicked");
        SceneNavigator.switchTo("/LocationListView.fxml", welcomeLabel);
    }

    @FXML
    private void handleMyFlights() {
        System.out.println("[Nav] My Flights clicked");
        org.example.mains.MainApp.switchScene("/views/user-dashboard.fxml", "Mes Vols");
    }

    @FXML
    private void handleSettings() {
        System.out.println("[Nav] Settings clicked");
        showInfoAlert("Paramètres", "Fonctionnalité à venir : Paramètres utilisateur");
    }

    @FXML
    private void handleHelp() {
        System.out.println("[Nav] Help clicked");
        showInfoAlert("Aide", "Besoin d'aide ? Contactez-nous à support@govibe.tn");
    }

    @FXML
    private void handleResetFilters() {
        System.out.println("[Nav] Reset Filters clicked");
        if (searchField != null) {
            searchField.clear();
        }
        refreshActivities();
    }

    @FXML
    private void handleSearch() {
        System.out.println("[Nav] Search clicked - Query: " + (searchField != null ? searchField.getText() : ""));
        refreshActivities();
    }

    @FXML
    private void handleViewAllCategories() {
        System.out.println("[Nav] View All Categories clicked");
        openProposeActivity();
    }

    @FXML
    private void handlePrevActivities() {
        System.out.println("[Nav] Previous Activities clicked");
    }

    @FXML
    private void handleNextActivities() {
        System.out.println("[Nav] Next Activities clicked");
    }

    private void refreshActivities() {
        loadActivities();
    }

    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleForums() {
        SceneNavigator.switchTo("/poste-forumviews/ListForum.fxml", welcomeLabel);
    }
}
