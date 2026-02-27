package org.example.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.scene.shape.Rectangle;
import org.example.assistant.CommandRouter;
import org.example.assistant.VoiceAssistantService;
import org.example.entities.Activite;
import org.example.entities.Session;
import org.example.mains.MainApp;
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

    @FXML
    private StackPane heroBanner;

    @FXML
    private ImageView heroImage;

    @FXML
    private VBox heroTitleBox;
    @FXML
    private StackPane rootStackPane;
    @FXML
    private ImageView bgImageView;

    @FXML
    private Label heroBadge;

    @FXML
    private Label heroSubtitle;

    @FXML
    private HBox heroSearchBox;

    @FXML
    private VBox emptyState;

    private personne currentUser;
    private final ServiceActivite serviceActivite = new ServiceActivite();
    private final ServiceSession serviceSession = new ServiceSession();
    private final ServiceReservationSession serviceReservation = new ServiceReservationSession();

    public void initData(personne user) {
        this.currentUser = user;
        if (currentUser != null) {
             if (welcomeLabel != null) {
                welcomeLabel.setText("Bienvenue, " + currentUser.getPrenom());
                welcomeLabel.setVisible(true);
                welcomeLabel.setManaged(true);
             }
        }
        
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
        if (profileInitials != null) {
            profileInitials.setText(initials.toUpperCase());
        }
        
        // Initialize destinations
        if (destinationCombo != null) {
            destinationCombo.getItems().addAll("Tunis", "Sousse", "Monastir", "Sfax", "Hammamet", "Tabarka");
        }

        // Register voice proxy for this screen
        initVoiceAssistant(user);
    }

    // ── Voice assistant support ───────────────────────────────────────────────

    private void initVoiceAssistant(personne user) {
        VoiceAssistantService vas = MainApp.getVoiceAssistant();
        if (vas == null) return;

        CommandRouter.ControllerProxy homeProxy = new CommandRouter.ControllerProxy() {

            @Override
            public void openBooking() {
                // Navigate to user dashboard (flights booking)
                Platform.runLater(() ->
                    org.example.mains.MainApp.switchScene("/views/user-dashboard.fxml", "Mes Vols"));
            }

            @Override
            public void cancelBooking() {
                // Go back to home
                Platform.runLater(() ->
                    org.example.mains.MainApp.switchScene("/org/example/UserHomeView.fxml", "Accueil"));
            }

            @Override
            public void showBookingsTab() {
                Platform.runLater(() ->
                    org.example.mains.MainApp.switchScene("/views/user-dashboard.fxml", "Mes Vols"));
            }

            @Override
            public void showSearchTab() {
                Platform.runLater(() ->
                    org.example.mains.MainApp.switchScene("/views/user-dashboard.fxml", "Mes Vols"));
            }

            @Override
            public void triggerPayment() { /* not applicable on home */ }

            @Override
            public String describeScreen() {
                String name = (user != null && user.getPrenom() != null)
                        ? user.getPrenom() : "utilisateur";
                return "GoVibe home screen. Welcome " + name + ". " +
                       "You can say Book to access flights, " +
                       "Cars for car rentals, Activities to explore things to do, " +
                       "Hotels for accommodation, Messages for your inbox, " +
                       "Forum for the community, Profile for your account, " +
                       "My Bookings to view your bookings, or Logout to sign out.";
            }

            // ── Section navigation — called by CommandRouter voice commands ──

            @Override
            public void showCarsSection() {
                Platform.runLater(() ->
                    org.example.mains.MainApp.switchScene("/VoitureView.fxml", "Location de voitures"));
            }

            @Override
            public void showActivitiesSection() {
                // Activities are displayed on the home page itself.
                Platform.runLater(() ->
                    org.example.mains.MainApp.switchScene("/UserHome.fxml", "Activit\u00e9s"));
            }

            @Override
            public void showHotelsSection() {
                Platform.runLater(() ->
                    org.example.mains.MainApp.switchScene("/views/room-booking.fxml", "H\u00f4tels & Chambres"));
            }

            @Override
            public void showSessionsSection() {
                Platform.runLater(() ->
                    org.example.mains.MainApp.switchScene("/DashboardSession.fxml", "Sessions"));
            }

            @Override
            public void showMessagesSection() {
                Platform.runLater(() ->
                    org.example.mains.MainApp.switchScene("/MesMessages.fxml", "Messages"));
            }

            @Override
            public void showForumSection() {
                Platform.runLater(() ->
                    org.example.mains.MainApp.switchScene("/poste-forumviews/ListForum.fxml", "Forum"));
            }

            @Override
            public void showReclamationSection() {
                Platform.runLater(() ->
                    org.example.mains.MainApp.switchScene("/ReclamationView.fxml", "R\u00e9clamations"));
            }

            @Override
            public void showProfileSection() {
                Platform.runLater(() ->
                    org.example.mains.MainApp.switchScene("/org/example/UserProfileView.fxml", "Mon Profil"));
            }

            @Override
            public void showLocationsSection() {
                Platform.runLater(() ->
                    org.example.mains.MainApp.switchScene("/LocationListView.fxml", "Mes Locations"));
            }
        };

        MainApp.setVoiceProxy(homeProxy);

        // NOTE: The Python login greeting already delivers guidance to the user,
        // so no extra TTS announcement is needed here. The 4-second delay was
        // unreliable anyway — Vivian's Qwen3 synthesis may still be running.
    }

    @FXML
    public void initialize() {
        loadActivities();
        setupHeroBackground();
        startHeroAnimations();
    }

    private void setupHeroBackground() {
        // --- 1. Dashboard Style Background (rootStackPane + bgImageView) ---
        if (bgImageView != null && rootStackPane != null) {
            bgImageView.fitWidthProperty().bind(rootStackPane.widthProperty());
            bgImageView.fitHeightProperty().bind(rootStackPane.heightProperty());
            
            var resourcePath = "/messages/home-hero5.png";
            var url = getClass().getResource(resourcePath);
            if (url != null) {
                Image img = new Image(url.toExternalForm(), true);
                bgImageView.setImage(img);
                System.out.println("[Background] Dashboard-style hero image loaded in UserHomeController");
            }
        }

        // --- 2. Original Hero Style Background (heroBanner + heroImage) ---
        if (heroBanner != null && heroImage != null) {
            try {
                var resourcePath = "/messages/home-hero5.png";
                var url = getClass().getResource(resourcePath);
                
                if (url != null) {
                    System.out.println("[Background] Attempting to load hero-style: " + url.toExternalForm());
                    Image img = new Image(url.toExternalForm(), true); // Background loading
                    
                    img.errorProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal) {
                            System.err.println("[Background] Hero image loading ERROR: " + img.getException());
                        }
                    });

                    img.progressProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal.doubleValue() == 1.0 && !img.isError()) {
                            System.out.println("[Background] Hero-style image loaded!");
                            heroImage.setImage(img);
                            
                            // Setup Hero Animation (Ken Burns)
                            heroImage.setScaleX(1.1);
                            heroImage.setScaleY(1.1);
                            
                            ScaleTransition st = new ScaleTransition(Duration.seconds(25), heroImage);
                            st.setFromX(1.05);
                            st.setFromY(1.05);
                            st.setToX(1.2);
                            st.setToY(1.2);
                            st.setAutoReverse(true);
                            st.setCycleCount(Animation.INDEFINITE);
                            st.setInterpolator(Interpolator.EASE_BOTH);
                            st.play();
                        }
                    });
                } else {
                    System.err.println("[Background] ERROR: Resource " + resourcePath + " not found!");
                }
            } catch (Exception e) {
                System.err.println("[Background] EXCEPTION during hero setup.");
                e.printStackTrace();
            }
        }
    }

    private void startHeroAnimations() {
        // --- 1. Floating Animation for Content ---
        applyFloatingAnimation(heroBadge, 2.5, 6);
        applyFloatingAnimation(heroTitleBox, 3.5, 8);
        applyFloatingAnimation(heroSubtitle, 4.5, 5);
        applyFloatingAnimation(heroSearchBox, 5.5, 10);
        
        // --- 2. Subtle Opacity Pulse for Title highlight ---
        // (Optional additions for "alive" feel)
    }

    private void applyFloatingAnimation(javafx.scene.Node node, double durationSeconds, double distance) {
        if (node == null) return;
        
        TranslateTransition tt = new TranslateTransition(Duration.seconds(durationSeconds), node);
        tt.setFromY(-distance);
        tt.setToY(distance);
        tt.setAutoReverse(true);
        tt.setCycleCount(Animation.INDEFINITE);
        tt.setInterpolator(Interpolator.EASE_BOTH);
        tt.play();
    }

    private void loadActivities() {
        if (activitiesPane == null)
            return;
            
        new Thread(() -> {
            try {
                // Background fetch
                List<Activite> list = serviceActivite.getAll();
                
                // UI update
                javafx.application.Platform.runLater(() -> {
                    activitiesPane.getChildren().clear();
                    for (Activite a : list) {
                        activitiesPane.getChildren().add(createActivityCard(a));
                    }
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private javafx.scene.Node createActivityCard(Activite a) {
        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(15);
        card.getStyleClass().add("activity-card-premium");
        card.setPrefWidth(300);
        card.setPadding(new Insets(0, 0, 20, 0)); // No top padding for flush image

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
                // Load image asynchronously (backgroundLoading = true)
                javafx.scene.image.Image img = new javafx.scene.image.Image(url.toExternalForm(), true);
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                
                iv.setFitWidth(300);
                iv.setPreserveRatio(true);
                
                // Rounded corners for image
                Rectangle clip = new Rectangle(300, 200);
                clip.setArcWidth(28);
                clip.setArcHeight(28);
                iv.setClip(clip);
                
                card.getChildren().add(iv);
            }
        } catch (Exception e) {
            System.err.println("Could not load image: " + imgPath);
        }

        VBox contentBox = new VBox(8);
        contentBox.setPadding(new Insets(0, 20, 0, 20));

        Label nameLbl = new Label(a.getName());
        nameLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #012E20;");

        Label locLbl = new Label("📍 " + a.getLocalisation());
        locLbl.setStyle("-fx-text-fill: #777; -fx-font-size: 14px;");

        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        
        Label prixLbl = new Label(a.getPrix() + " DT");
        prixLbl.getStyleClass().add("price-badge");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bottomRow.getChildren().addAll(prixLbl, spacer);
        contentBox.getChildren().addAll(nameLbl, locLbl, bottomRow);
        
        card.getChildren().add(contentBox);

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

            VBox root = new VBox(25);
            root.setPadding(new Insets(35));
            root.setStyle("-fx-background-color: #FDFDFB;");

            Label title = new Label("Explorez les sessions");
            title.getStyleClass().add("section-title");
            
            Label subtitle = new Label("Pour l'activité : " + activite.getName());
            subtitle.setStyle("-fx-text-fill: #666; -fx-font-size: 15;");
            
            root.getChildren().addAll(title, subtitle);

            if (sessions.isEmpty()) {
                Label empty = new Label("Aucune session disponible pour le moment.");
                empty.setStyle("-fx-text-fill: #555; -fx-font-size: 14px;");
                root.getChildren().add(empty);
            } else {
                for (Session s : sessions) {
                    HBox row = new HBox(25);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(20, 25, 20, 25));
                    row.getStyleClass().add("glass-card");
                    row.setStyle("-fx-background-radius: 20; -fx-background-color: white;");

                    Label dateLbl = new Label("📅  " + s.getDate().toString());
                    dateLbl.setStyle("-fx-font-size: 15px; -fx-text-fill: #1A1A1A; -fx-font-weight: 600;");

                    Label heureLbl = new Label("🕐  " + s.getHeure().toString());
                    heureLbl.setStyle("-fx-font-size: 15px; -fx-text-fill: #50C878; -fx-font-weight: 700;");

                    Label placesLbl = new Label(s.getNbr_places_restant() + " places");
                    placesLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-padding: 5 12; -fx-background-radius: 12; -fx-background-color: " +
                            (s.getNbr_places_restant() > 0 ? "rgba(80, 200, 120, 0.12)" : "rgba(231, 76, 60, 0.12)") + "; -fx-text-fill: " +
                            (s.getNbr_places_restant() > 0 ? "#27ae60" : "#e74c3c") + ";");

                    Spinner<Integer> nbSpinner = new Spinner<>(1, Math.max(1, s.getNbr_places_restant()), 1);
                    nbSpinner.setPrefWidth(90);
                    nbSpinner.setStyle("-fx-background-radius: 15; -fx-background-color: transparent;");

                    Button reserveBtn = new Button("RÉSERVER");
                    reserveBtn.getStyleClass().add("btn-search");
                    reserveBtn.setStyle("-fx-padding: 10 25; -fx-font-size: 13;");
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
            Stage stage;
            if (welcomeLabel != null && welcomeLabel.getScene() != null) {
                stage = (Stage) welcomeLabel.getScene().getWindow();
            } else if (profileMenu != null && profileMenu.getScene() != null) {
                stage = (Stage) profileMenu.getScene().getWindow();
            } else {
                stage = new Stage();
            }
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
        System.out.println("[Nav] Profile clicked - navigating to UserProfileView.fxml");
        org.example.mains.MainApp.switchScene("/org/example/UserProfileView.fxml", "Mon Profil");
    }

    @FXML
    private void handleMyReservations() {
        System.out.println("[Nav] My Reservations clicked");
        SceneNavigator.switchTo("/views/room-booking.fxml", welcomeLabel);
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

    @FXML
    private void handleContact() {
        System.out.println("[Nav] Contact clicked");
        SceneNavigator.switchTo("/MesMessagesChat.fxml", welcomeLabel);
    }

    @FXML
    private void handleReclamation() {
        System.out.println("[Nav] Réclamation clicked");
        SceneNavigator.switchTo("/ReclamationView.fxml", welcomeLabel, controller -> {
            if (controller instanceof ReclamationController) {
                ((ReclamationController) controller).initData(currentUser);
            }
        });
    }
}
