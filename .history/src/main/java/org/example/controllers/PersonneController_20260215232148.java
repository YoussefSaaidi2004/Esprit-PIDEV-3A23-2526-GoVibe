package org.example.controllers;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
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

    @FXML private TextField tfNom;
    @FXML private TextField tfPrenom;
    @FXML private TextField tfEmail;
    @FXML private PasswordField tfPassword;
    @FXML private ComboBox<String> cbRole;

    @FXML private TableView<personne> tablePersonnes;
    @FXML private TableColumn<personne, Integer> colId;
    @FXML private TableColumn<personne, String> colNom;
    @FXML private TableColumn<personne, String> colPrenom;
    @FXML private TableColumn<personne, String> colEmail;
    @FXML private TableColumn<personne, String> colRole;
    @FXML private TableColumn<personne, Void> colActions;

    @FXML private Label notificationLabel;
    @FXML private Label countLabel;
    @FXML private Label statusLabel;

    private final ServicePersonne servicePersonne = new ServicePersonne();
    private final ObservableList<personne> personneList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!SessionManager.isAuthenticated()) {
            SceneNavigator.switchTo("/org/example/LoginView.fxml", tablePersonnes);
            return;
        }
        if (!SessionManager.isAdmin()) {
            SceneNavigator.switchTo("/LocationListView.fxml", tablePersonnes);
            return;
        }
        // Setup columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        // Setup role ComboBox
        cbRole.getItems().addAll("user", "admin");
        cbRole.setValue("user");

        // Setup action column with edit/delete buttons
        setupActionColumn();

        tablePersonnes.setItems(personneList);

        // Load data
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

        // Validation
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showNotification("Veuillez remplir tous les champs", true);
            return;
        }

        // Basic email validation
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            showNotification("Veuillez entrer un email valide", true);
            return;
        }

        if (password.length() < 6) {
            showNotification("Le mot de passe doit contenir au moins 6 caracteres", true);
            return;
        }

        // Add to database
        try {
            servicePersonne.ajouter(new personne(nom, prenom, email, password, role));
            showNotification("Personne ajoutee avec succes - " + prenom + " " + nom, false);
            clearFields();
            loadPersonnes();
        } catch (RuntimeException e) {
            String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            if (message != null && message.contains("Duplicate")) {
                showNotification("Cet email est deja utilise", true);
            } else {
                showNotification("Erreur: " + message, true);
            }
        }
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
        try {
            personneList.clear();
            personneList.addAll(servicePersonne.afficher());
            countLabel.setText(personneList.size() + " personne" + (personneList.size() > 1 ? "s" : "") + " enregistree" + (personneList.size() > 1 ? "s" : ""));
        } catch (RuntimeException e) {
            String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            showNotification("Erreur de chargement: " + message, true);
        }
    }

    private void setupActionColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final HBox container = new HBox(8, btnEdit, btnDelete);

            {
                btnEdit.getStyleClass().add("btn-edit");
                btnDelete.getStyleClass().add("btn-danger");
                container.setAlignment(Pos.CENTER);

                btnEdit.setOnAction(event -> {
                    personne p = getTableView().getItems().get(getIndex());
                    tfNom.setText(p.getNom());
                    tfPrenom.setText(p.getPrenom());
                    tfEmail.setText(p.getEmail());
                    tfPassword.setText(p.getPassword());
                    cbRole.setValue(p.getRole());
                    showNotification("Modification de " + p.getPrenom() + " " + p.getNom() + " - editez les champs puis cliquez Ajouter", false);
                });

                btnDelete.setOnAction(event -> {
                    personne p = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Confirmation");
                    alert.setHeaderText("Supprimer cette personne ?");
                    alert.setContentText(p.getPrenom() + " " + p.getNom() + " sera supprime definitivement.");
                    alert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            try {
                                servicePersonne.supprimer(p.getId());
                                showNotification(p.getPrenom() + " " + p.getNom() + " supprime", false);
                                loadPersonnes();
                            } catch (RuntimeException e) {
                                String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                                showNotification("Erreur: " + message, true);
                            }
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
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
