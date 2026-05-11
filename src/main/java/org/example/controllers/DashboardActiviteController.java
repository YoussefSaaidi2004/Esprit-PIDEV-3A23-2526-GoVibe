package org.example.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
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
    private FlowPane activitesContainer;

    private Activite selectedActivite;

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

        if (colId != null) {
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colName.setCellValueFactory(new PropertyValueFactory<>("name"));
            colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
            colType.setCellValueFactory(new PropertyValueFactory<>("type"));
            colLoc.setCellValueFactory(new PropertyValueFactory<>("localisation"));
            colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
            colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        }
        if (tableActivite != null) {
            tableActivite.setItems(data);
        }
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
                ActiviteModifierController amc = (ActiviteModifierController) childController;
                amc.setParentController(this);
                if (selectedActivite != null) {
                    amc.initData(selectedActivite);
                }
            } else if (childController instanceof ActiviteSuppressionController) {
                ActiviteSuppressionController asc = (ActiviteSuppressionController) childController;
                asc.setParentController(this);
                if (selectedActivite != null) {
                    asc.initData(selectedActivite);
                }
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
        Activite selected = (selectedActivite != null) ? selectedActivite :
                (tableActivite != null ? tableActivite.getSelectionModel().getSelectedItem() : null);
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
        new Thread(() -> {
            try {
                // On affiche TOUTES les activités (Pending + Confirmed) pour l'admin
                java.util.List<Activite> list = service.getAllAll();
                Platform.runLater(() -> {
                    data.setAll(list);
                    if (activitesContainer != null) buildCards(list);
                    lblCount.setText(list.size() + " activité(s)");
                    lblMsg.setText("Liste chargée avec succès.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblCount.setText("Erreur");
                    lblMsg.setText("Erreur: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }, "Activite-Refresh-Thread").start();
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
        Activite selected = (selectedActivite != null) ? selectedActivite :
                (tableActivite != null ? tableActivite.getSelectionModel().getSelectedItem() : null);
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
        Activite selected = (selectedActivite != null) ? selectedActivite :
                (tableActivite != null ? tableActivite.getSelectionModel().getSelectedItem() : null);
        if (selected == null) {
            showAlert("Attention", "Veuillez sélectionner une activité à supprimer.");
            return;
        }
        showModal("/ActiviteSuppression.fxml");
    }

    @FXML
    private void openSessionsDashboard() {
        SceneNavigator.switchTo("DashboardSession.fxml", mainStack);
    }

    private void buildCards(java.util.List<Activite> list) {
        activitesContainer.getChildren().clear();
        for (Activite a : list) {
            VBox card = new VBox(10);
            card.setStyle("-fx-background-color: rgba(255,255,255,0.07); " +
                          "-fx-background-radius: 18; " +
                          "-fx-border-color: rgba(80,200,120,0.28); " +
                          "-fx-border-width: 1.5; -fx-border-radius: 18; " +
                          "-fx-padding: 20 22; " +
                          "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.38), 16, 0, 0, 5);");
            card.setPrefWidth(300);
            card.setMaxWidth(300);

            // Name
            Label nameLabel = new Label(a.getName());
            nameLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: 900; -fx-text-fill: white; -fx-wrap-text: true;");
            nameLabel.setWrapText(true);

            // Type + status row
            HBox badges = new HBox(8);
            badges.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label typeLabel = new Label(a.getType());
            typeLabel.setStyle("-fx-background-color: rgba(80,200,120,0.18); -fx-background-radius: 10; " +
                               "-fx-text-fill: #50C878; -fx-font-weight: 700; -fx-font-size: 11; -fx-padding: 3 10;");
            boolean confirmed = "Confirmed".equalsIgnoreCase(String.valueOf(a.getStatus()));
            Label statusLabel = new Label(confirmed ? "✅ Confirmé" : "⏳ En attente");
            statusLabel.setStyle("-fx-background-color: " + (confirmed ? "rgba(80,200,120,0.22)" : "rgba(255,165,0,0.22)") + "; " +
                                 "-fx-background-radius: 10; " +
                                 "-fx-text-fill: " + (confirmed ? "#50C878" : "#FFA500") + "; " +
                                 "-fx-font-weight: 700; -fx-font-size: 11; -fx-padding: 3 10;");
            badges.getChildren().addAll(typeLabel, statusLabel);

            // Localisation
            Label locLabel = new Label("📍 " + a.getLocalisation());
            locLabel.setStyle("-fx-font-size: 12; -fx-text-fill: rgba(200,240,220,0.70);");

            // Prix
            Label prixLabel = new Label(a.getPrix() + " TND");
            prixLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: white;");

            // Action buttons
            HBox actions = new HBox(8);
            actions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Button btnEdit = new Button("✏️ Modifier");
            btnEdit.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 8; " +
                             "-fx-border-color: rgba(255,255,255,0.2); -fx-border-width: 1; -fx-border-radius: 8; " +
                             "-fx-text-fill: rgba(220,250,235,0.88); -fx-font-weight: 700; -fx-font-size: 11; " +
                             "-fx-padding: 6 12; -fx-cursor: hand;");
            btnEdit.setOnAction(e -> { selectedActivite = a; openModifier(); });

            Button btnDel = new Button("🗑️");
            btnDel.setStyle("-fx-background-color: rgba(255,80,80,0.14); -fx-background-radius: 8; " +
                            "-fx-border-color: rgba(255,80,80,0.35); -fx-border-width: 1; -fx-border-radius: 8; " +
                            "-fx-text-fill: rgba(255,140,140,0.90); -fx-font-weight: 700; -fx-font-size: 11; " +
                            "-fx-padding: 6 12; -fx-cursor: hand;");
            btnDel.setOnAction(e -> {
                selectedActivite = a;
                openSuppression();
            });

            if (!confirmed) {
                Button btnVal = new Button("✅ Valider");
                btnVal.setStyle("-fx-background-color: rgba(80,200,120,0.18); -fx-background-radius: 8; " +
                                "-fx-border-color: rgba(80,200,120,0.40); -fx-border-width: 1; -fx-border-radius: 8; " +
                                "-fx-text-fill: #50C878; -fx-font-weight: 700; -fx-font-size: 11; " +
                                "-fx-padding: 6 12; -fx-cursor: hand;");
                btnVal.setOnAction(e -> { selectedActivite = a; validerActivite(); });
                actions.getChildren().addAll(btnVal, btnEdit, btnDel);
            } else {
                actions.getChildren().addAll(btnEdit, btnDel);
            }

            card.getChildren().addAll(nameLabel, badges, locLabel, prixLabel, actions);
            activitesContainer.getChildren().add(card);
        }
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