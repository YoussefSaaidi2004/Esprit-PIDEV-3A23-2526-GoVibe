package org.example.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.util.Duration;
import org.example.entities.Activite;
import org.example.services.ServiceActivite;
import org.example.utils.SceneNavigator;

import java.io.IOException;
import java.math.BigDecimal;

public class DashboardActiviteController {

    @FXML
    private StackPane mainStack;
    @FXML
    private VBox contentRoot;
    @FXML
    private StackPane modalOverlay;
    @FXML
    private VBox modalContent;
    @FXML
    private TextField searchField;
    @FXML
    private ImageView bgImageView;

    @FXML
    private AdminSidebarController adminSidebarController;

    @FXML
    private TableView<Activite> tableActivite;
    @FXML
    private TableColumn<Activite, Integer> colId;
    @FXML
    private TableColumn<Activite, String> colName;
    @FXML
    private TableColumn<Activite, String> colDesc;
    @FXML
    private TableColumn<Activite, String> colType;
    @FXML
    private TableColumn<Activite, String> colLoc;
    @FXML
    private TableColumn<Activite, BigDecimal> colPrix;
    @FXML
    private TableColumn<Activite, String> colStatus;

    @FXML
    private Label lblCount;
    @FXML
    private Label lblMsg;

    private final ServiceActivite service = new ServiceActivite();
    private final ObservableList<Activite> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Set active page in sidebar (if we still use it, but new UI uses UserHeader)
        if (adminSidebarController != null) {
            adminSidebarController.setActivePage("activites");
        }

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colLoc.setCellValueFactory(new PropertyValueFactory<>("localisation"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tableActivite.setItems(data);
        setupHeroBackground();
        rafraichir();

        // Initial state for modal
        modalOverlay.setVisible(false);
        modalOverlay.setOpacity(0);
    }

    private void setupHeroBackground() {
        if (bgImageView != null && mainStack != null) {
            bgImageView.fitWidthProperty().bind(mainStack.widthProperty());
            bgImageView.fitHeightProperty().bind(mainStack.heightProperty());
            
            var resourcePath = "/messages/home-hero5.png";
            var url = getClass().getResource(resourcePath);
            if (url != null) {
                Image img = new Image(url.toExternalForm(), true);
                bgImageView.setImage(img);
                System.out.println("[Background] Hero image loaded successfully in Dashboard");
            } else {
                System.err.println("[Background] ERROR: Resource " + resourcePath + " not found!");
            }
        }
    }

    // ==================== MODAL SYSTEM ====================

    private void showModal(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent modalNode = loader.load();
            
            // Pass parent controller to child
            Object childController = loader.getController();
            if (childController instanceof ActiviteAjoutController) {
                ((ActiviteAjoutController) childController).setParentController(this);
            } else if (childController instanceof ActiviteModifierController) {
                ((ActiviteModifierController) childController).setParentController(this);
            }

            modalContent.getChildren().setAll(modalNode);
            
            // Apply Blur Effect to background
            BoxBlur blur = new BoxBlur(0, 0, 3);
            contentRoot.setEffect(blur);
            
            modalOverlay.setVisible(true);

            // Animations
            FadeTransition fade = new FadeTransition(Duration.millis(300), modalOverlay);
            fade.setFromValue(0);
            fade.setToValue(1);

            ScaleTransition scale = new ScaleTransition(Duration.millis(300), modalContent);
            scale.setFromX(0.85);
            scale.setFromY(0.85);
            scale.setToX(1.0);
            scale.setToY(1.0);

            ParallelTransition pt = new ParallelTransition(fade, scale);
            
            // Animate Blur
            Timeline blurTimeline = new Timeline(
                new KeyFrame(Duration.millis(300),
                    new KeyValue(blur.widthProperty(), 15),
                    new KeyValue(blur.heightProperty(), 15)
                )
            );
            
            pt.play();
            blurTimeline.play();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger le formulaire : " + e.getMessage());
        }
    }

    public void closeModal() {
        FadeTransition fade = new FadeTransition(Duration.millis(250), modalOverlay);
        fade.setFromValue(1);
        fade.setToValue(0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(250), modalContent);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(0.85);
        scale.setToY(0.85);

        BoxBlur blur = (BoxBlur) contentRoot.getEffect();
        Timeline blurTimeline = new Timeline(
            new KeyFrame(Duration.millis(250),
                new KeyValue(blur.widthProperty(), 0),
                new KeyValue(blur.heightProperty(), 0)
            )
        );

        ParallelTransition pt = new ParallelTransition(fade, scale);
        pt.setOnFinished(e -> {
            modalOverlay.setVisible(false);
            contentRoot.setEffect(null);
            rafraichir(); // Auto refresh when modal closes
        });
        
        pt.play();
        blurTimeline.play();
    }

    private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }

    @FXML
    private void validerActivite() {
        Activite selected = tableActivite.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle("Attention");
            a.setHeaderText("Aucune activité sélectionnée");
            a.setContentText("Veuillez sélectionner une activité à valider.");
            a.showAndWait();
            return;
        }

        try {
            // 1. Valider en base (passe en 'Confirmed')
            service.valider(selected.getId());

            // 2. Rafraîchir la table pour voir le changement de statut
            rafraichir();

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Succès");
            info.setHeaderText("Activité validée !");
            info.setContentText(
                    "L'activité '" + selected.getName() + "' est maintenant visible pour les utilisateurs.\n" +
                            "Vous allez être redirigé pour ajouter une session.");
            info.showAndWait();

            // 3. Redirection vers Ajout Session avec présélection
            SceneNavigator.switchTo("SessionAjout.fxml", mainStack, controller -> {
                if (controller instanceof SessionAjoutController) {
                    ((SessionAjoutController) controller).selectActivite(selected);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Erreur");
            a.setHeaderText("Erreur lors de la validation");
            a.setContentText(e.getMessage());
            a.showAndWait();
        }
    }

    @FXML
    private void rafraichir() {
        try {
            // On affiche TOUTES les activités (Pending + Confirmed) pour l'admin
            data.setAll(service.getAllAll());
            lblCount.setText(data.size() + " activité(s)");
            lblMsg.setText("Liste chargée avec succès.");
        } catch (Exception e) {
            lblCount.setText("Erreur");
            lblMsg.setText("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= NAVIGATION (using SceneNavigator for smooth transitions) =================

    @FXML
    private void openDashboard() {
        SceneNavigator.switchTo("Dashboard.fxml", mainStack);
    }

    @FXML
    private void openAjout() {
        showModal("/ActiviteAjout.fxml");
    }

    @FXML
    private void openModifier() {
        Activite selected = tableActivite.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "Veuillez sélectionner une activité à modifier.");
            return;
        }
        // Assuming we set the selected activity in some context or pass it to loader
        // For now, let's just open the modal. ActiviteModifierController should handle loading selected.
        showModal("/ActiviteModifier.fxml");
    }

    @FXML
    private void openSuppression() {
        SceneNavigator.switchTo("ActiviteSuppression.fxml", mainStack);
    }

    @FXML
    private void openSessionsDashboard() {
        SceneNavigator.switchTo("DashboardSession.fxml", mainStack);
    }

    @FXML
    private void goBackToDashboard() {
        SceneNavigator.switchTo("org/example/AdminDashboardView.fxml", mainStack);
    }

    @FXML
    private void logout() {
        SceneNavigator.switchTo("org/example/LoginView.fxml", mainStack);
    }
}