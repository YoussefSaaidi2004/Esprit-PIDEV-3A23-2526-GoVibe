package org.example.controllers;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.entities.personne;
import org.example.services.ServicePersonne;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class PersonneController implements Initializable {

    @FXML private StackPane rootStack;
    @FXML private ImageView bgImageView;

    @FXML private TextField tfNom;
    @FXML private TextField tfPrenom;
    @FXML private TextField tfEmail;
    @FXML private PasswordField tfPassword;
    @FXML private ComboBox<String> cbRole;

    @FXML private FlowPane personnesContainer;
    @FXML private Label cardCountLabel;

    @FXML private Label notificationLabel;
    @FXML private Label countLabel;
    @FXML private Label statusLabel;

    private final ServicePersonne servicePersonne = new ServicePersonne();
    private final java.util.List<personne> personneList = new java.util.ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!SessionManager.isAuthenticated()) {
            SceneNavigator.switchTo("/org/example/LoginView.fxml", tfNom);
            return;
        }
        if (!SessionManager.isAdmin()) {
            SceneNavigator.switchTo("/LocationListView.fxml", tfNom);
            return;
        }
        // Hero background
        try {
            String resourcePath = "/messages/home-hero5.png";
            var heroUrl = getClass().getResource(resourcePath);
            if (heroUrl != null && bgImageView != null && rootStack != null) {
                Image heroImage = new Image(heroUrl.toExternalForm());
                bgImageView.setImage(heroImage);
                bgImageView.setPreserveRatio(false);
                bgImageView.setEffect(new GaussianBlur(30));
                bgImageView.fitWidthProperty().bind(rootStack.widthProperty());
                bgImageView.fitHeightProperty().bind(rootStack.heightProperty());
            }
        } catch (Exception e) {
            System.err.println("[PersonneController] Hero image load error: " + e.getMessage());
        }

        // Setup role ComboBox
        cbRole.getItems().addAll("user", "admin");
        cbRole.setValue("user");

        // Load data as cards
        loadPersonnes();

        statusLabel.setText("Connecte");
    }

    @FXML
    private void handleAjouter() {
        String nom = tfNom.getText().trim();
        String prenom = tfPrenom.getText().trim();
        String email = tfEmail.getText().trim();
        String password = tfPassword.getText().trim();
        String role = cbRole.getValue();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showNotification("Veuillez remplir tous les champs", true);
            return;
        }

        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            showNotification("Veuillez entrer un email valide", true);
            return;
        }

        if (password.length() < 6) {
            showNotification("Le mot de passe doit contenir au moins 6 caracteres", true);
            return;
        }

        new Thread(() -> {
            try {
                servicePersonne.ajouter(new personne(nom, prenom, email, password, role));
                Platform.runLater(() -> {
                    showNotification("Personne ajoutee avec succes - " + prenom + " " + nom, false);
                    clearFields();
                    loadPersonnes();
                });
            } catch (Exception e) {
                String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                Platform.runLater(() -> {
                    if (message != null && message.contains("Duplicate")) {
                        showNotification("Cet email est deja utilise", true);
                    } else {
                        showNotification("Erreur: " + message, true);
                    }
                });
            }
        }, "Personne-Add-Thread").start();
    }

    @FXML
    private void handleReset() {
        clearFields();
    }

    @FXML
    private void handleRefresh() {
        loadPersonnes();
        showNotification("Liste rafraichie", false);
    }

    private void loadPersonnes() {
        new Thread(() -> {
            try {
                java.util.List<personne> list = servicePersonne.afficher();
                Platform.runLater(() -> {
                    personneList.clear(); personneList.addAll(list);
                    int n = list.size();
                    String countText = n + " personne" + (n > 1 ? "s" : "") + " enregistree" + (n > 1 ? "s" : "");
                    countLabel.setText(countText);
                    if (cardCountLabel != null) cardCountLabel.setText(n + " utilisateur" + (n > 1 ? "s" : ""));
                    buildCards(list);
                });
            } catch (Exception e) {
                String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                Platform.runLater(() -> showNotification("Erreur de chargement: " + message, true));
            }
        }, "Personne-Load-Thread").start();
    }

    private void buildCards(java.util.List<personne> list) {
        if (personnesContainer == null) return;
        personnesContainer.getChildren().clear();
        for (personne p : list) {
            personnesContainer.getChildren().add(createPersonneCard(p));
        }
    }

    private VBox createPersonneCard(personne p) {
        // Avatar circle with initials
        boolean isAdmin = "admin".equalsIgnoreCase(p.getRole());
        String initials = ((p.getPrenom() != null && !p.getPrenom().isEmpty() ? p.getPrenom().substring(0, 1) : "?")
                + (p.getNom() != null && !p.getNom().isEmpty() ? p.getNom().substring(0, 1) : "?")).toUpperCase();

        StackPane avatarPane = new StackPane();
        avatarPane.setMinSize(44, 44);
        avatarPane.setMaxSize(44, 44);
        avatarPane.setStyle("-fx-background-color: " + (isAdmin ? "rgba(80,200,120,0.28)" : "rgba(255,255,255,0.12)") +
                "; -fx-background-radius: 22;");
        Label initialsLabel = new Label(initials);
        initialsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: " + (isAdmin ? "#50C878" : "rgba(255,255,255,0.85)") + ";");;
        avatarPane.getChildren().add(initialsLabel);

        // Role badge
        Label roleBadge = new Label(isAdmin ? "ADMIN" : "USER");
        roleBadge.setStyle("-fx-background-color: " + (isAdmin ? "rgba(80,200,120,0.22)" : "rgba(255,255,255,0.08)") +
                "; -fx-border-color: " + (isAdmin ? "rgba(80,200,120,0.5)" : "rgba(255,255,255,0.18)") +
                "; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;"
                + "-fx-padding: 3 10; -fx-font-size: 10px; -fx-font-weight: 800;"
                + "-fx-text-fill: " + (isAdmin ? "#50C878" : "rgba(200,230,220,0.85)") + ";");

        HBox badgeRow = new HBox();
        badgeRow.getChildren().add(roleBadge);

        // Name
        Label nameLabel = new Label(p.getPrenom() + " " + p.getNom());
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: white; -fx-wrap-text: true;");
        nameLabel.setMaxWidth(170);

        // Email
        Label emailLabel = new Label(p.getEmail());
        emailLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(180,220,200,0.65); -fx-wrap-text: true;");
        emailLabel.setMaxWidth(170);

        // Action buttons
        Button btnEdit = new Button("Modifier");
        btnEdit.setStyle("-fx-background-color: rgba(80,200,120,0.18); -fx-text-fill: #50C878; -fx-font-size: 11px;"
                + "-fx-font-weight: 700; -fx-background-radius: 8; -fx-border-color: rgba(80,200,120,0.40);"
                + "-fx-border-width: 1; -fx-border-radius: 8; -fx-padding: 5 12; -fx-cursor: hand;");
        btnEdit.setOnAction(e -> {
            tfNom.setText(p.getNom());
            tfPrenom.setText(p.getPrenom());
            tfEmail.setText(p.getEmail());
            tfPassword.setText(p.getPassword());
            cbRole.setValue(p.getRole());
            showNotification("Edition de " + p.getPrenom() + " " + p.getNom() + " – modifiez puis cliquez Enregistrer", false);
        });

        Button btnDelete = new Button("Supprimer");
        btnDelete.setStyle("-fx-background-color: rgba(220,60,60,0.18); -fx-text-fill: #ff6b6b; -fx-font-size: 11px;"
                + "-fx-font-weight: 700; -fx-background-radius: 8; -fx-border-color: rgba(220,60,60,0.40);"
                + "-fx-border-width: 1; -fx-border-radius: 8; -fx-padding: 5 12; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation");
            alert.setHeaderText("Supprimer " + p.getPrenom() + " " + p.getNom() + " ?");
            alert.setContentText("Cette action est irreversible.");
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    new Thread(() -> {
                        try {
                            servicePersonne.supprimer(p.getId());
                            Platform.runLater(() -> {
                                showNotification(p.getPrenom() + " " + p.getNom() + " supprime", false);
                                loadPersonnes();
                            });
                        } catch (Exception ex) {
                            String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                            Platform.runLater(() -> showNotification("Erreur: " + msg, true));
                        }
                    }, "Personne-Delete-Thread").start();
                }
            });
        });

        HBox actionsRow = new HBox(8, btnEdit, btnDelete);
        actionsRow.setAlignment(Pos.CENTER);

        // Separator line
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: rgba(255,255,255,0.10);");

        VBox card = new VBox(10);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(190);
        card.setMaxWidth(190);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.07);"
                + "-fx-border-color: " + (isAdmin ? "rgba(80,200,120,0.30)" : "rgba(255,255,255,0.12)") + ";"
                + "-fx-border-width: 1.5; -fx-border-radius: 16; -fx-background-radius: 16;"
                + "-fx-padding: 16; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.38),18,0,0,6);");
        card.getChildren().addAll(avatarPane, nameLabel, emailLabel, badgeRow, sep, actionsRow);
        return card;
    }

    @FXML
    private void handleBackToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/AdminDashboardView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tfNom.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoVoitures() {
        SceneNavigator.switchTo("/VoitureListView.fxml", tfNom);
    }

    @FXML
    private void handleGoLocations() {
        SceneNavigator.switchTo("/AdminLocationListView.fxml", tfNom);
    }

    @FXML
    private void handleGoFlights() {
        SceneNavigator.switchTo("/views/flight-management.fxml", tfNom);
    }

    @FXML
    private void handleGoCheckouts() {
        SceneNavigator.switchTo("/views/checkout-management.fxml", tfNom);
    }

    @FXML
    private void handleGoDashboard() {
        SceneNavigator.switchTo("/org/example/AdminDashboardView.fxml", tfNom);
    }

    @FXML
    private void handleGoPersonnes() {
        // Already on this page, refresh data
        loadPersonnes();
    }

    @FXML
    private void handleGoHotels() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main-layout.fxml"));
            Parent root = loader.load();
            MainLayoutController controller = loader.getController();
            if (controller != null) {
                controller.loadHotels();
            }
            Stage stage = (Stage) tfNom.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoChambres() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main-layout.fxml"));
            Parent root = loader.load();
            MainLayoutController controller = loader.getController();
            if (controller != null) {
                controller.loadChambres();
            }
            Stage stage = (Stage) tfNom.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoReservations() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main-layout.fxml"));
            Parent root = loader.load();
            MainLayoutController controller = loader.getController();
            if (controller != null) {
                controller.loadReservations();
            }
            Stage stage = (Stage) tfNom.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoActivites() {
        SceneNavigator.switchTo("/Dashboard.fxml", tfNom);
    }

    @FXML
    private void handleGoForums() {
        SceneNavigator.switchTo("/poste-forumviews/ListForum.fxml", tfNom);
    }

    @FXML
    private void handleLogout() {
        SessionManager.clear();
        SceneNavigator.switchTo("/org/example/LoginView.fxml", tfNom);
    }

    private void clearFields() {
        tfNom.clear();
        tfPrenom.clear();
        tfEmail.clear();
        tfPassword.clear();
        cbRole.setValue("user");
        tfNom.requestFocus();
    }

    private void showNotification(String message, boolean isError) {
        notificationLabel.setText(message);
        notificationLabel.getStyleClass().removeAll("notification-success", "notification-error");
        notificationLabel.getStyleClass().add(isError ? "notification-error" : "notification-success");
        notificationLabel.setVisible(true);
        notificationLabel.setManaged(true);

        // Auto-hide after 4 seconds
        PauseTransition pause = new PauseTransition(Duration.seconds(4));
        pause.setOnFinished(e -> {
            notificationLabel.setVisible(false);
            notificationLabel.setManaged(false);
        });
        pause.play();
    }
}
