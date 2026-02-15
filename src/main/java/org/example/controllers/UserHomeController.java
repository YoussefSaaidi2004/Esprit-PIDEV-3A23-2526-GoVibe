package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import org.example.entites.Session;
import org.example.services.ServiceReservationSession;
import org.example.services.ServiceSession;

import java.net.URL;
import java.util.*;

public class UserHomeController {

    @FXML
    private Label welcomeLabel;
    @FXML
    private TilePane activitiesPane;

    private final ServiceSession serviceSession = new ServiceSession();
    private final ServiceReservationSession serviceRes = new ServiceReservationSession();

    private final org.example.services.ServiceActivite serviceActivite = new org.example.services.ServiceActivite();
    private final List<org.example.entites.Activite> activities = new ArrayList<>();

    @FXML
    public void initialize() {
        welcomeLabel.setText("Bonjour !");
        loadActivitiesFromDB();
        loadCards();
    }

    private void loadActivitiesFromDB() {
        try {
            activities.clear();
            activities.addAll(serviceActivite.getAll());
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors du chargement des activités : " + e.getMessage())
                    .showAndWait();
        }
    }

    private void loadCards() {
        activitiesPane.getChildren().clear();
        for (org.example.entites.Activite a : activities) {
            activitiesPane.getChildren().add(createCard(a));
        }
    }

    private Node createCard(org.example.entites.Activite a) {
        VBox card = new VBox(8);
        card.setPrefWidth(300);
        card.setStyle("""
                    -fx-background-color: white;
                    -fx-background-radius: 12;
                    -fx-padding: 12;
                    -fx-border-color: #A0E0C9;
                    -fx-border-radius: 12;
                    -fx-cursor: hand;
                    -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.12), 10, 0, 0, 3);
                """);

        // ✅ Image dynamique selon la localisation
        String imagePath = getImagePathForLocation(a.getLocalisation());
        ImageView img = new ImageView(loadImageFromResources(imagePath));
        img.setFitWidth(276);
        img.setFitHeight(150);
        img.setPreserveRatio(false);
        img.setSmooth(true);

        Label title = new Label(a.getName());
        title.setWrapText(true);
        title.setStyle("-fx-text-fill: #013220; -fx-font-size: 15px; -fx-font-weight: bold;");

        Label desc = new Label(a.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #084E36; -fx-font-size: 12px;");

        Region spacer = new Region();
        spacer.setMinHeight(4);

        HBox footer = new HBox();
        Label hint = new Label("Clic → Réserver | " + a.getPrix() + " DT");
        hint.setStyle("-fx-text-fill: #50C878; -fx-font-size: 11px; -fx-font-weight: bold;");
        footer.getChildren().add(hint);

        card.getChildren().addAll(img, title, desc, spacer, footer);

        card.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1) {
                showDetailsWithReservation(a);
            }
        });

        return card;
    }

    private Image loadImageFromResources(String fileName) {
        URL url = getClass().getResource("/images/" + fileName);
        if (url != null)
            return new Image(url.toExternalForm(), true);

        URL placeholder = getClass().getResource("/images/placeholder.png");
        if (placeholder != null)
            return new Image(placeholder.toExternalForm(), true);

        return null;
    }

    private String getImagePathForLocation(String location) {
        if (location == null)
            return "placeholder.png";

        return switch (location) {
            case "Tunis (Medina)" -> "visite-medina-tunis.png";
            case "Nabeul" -> "atelier-poterie-nabeul.png";
            case "Sidi Bou Said" -> "balade-sidi-bou-said.png";
            case "Ghar El Melh" -> "kayak-ghar-el-melh.png";
            case "Cap Bon" -> "randonnee-cap-bon.png";
            case "Degustation Huile d'Olive" -> "degustation-huile-olive.png";
            default -> "placeholder.png";
        };
    }

    // ✅ Popup détail + bouton Réserver
    private void showDetailsWithReservation(org.example.entites.Activite a) {
        ButtonType reserverBtn = new ButtonType("Réserver", ButtonData.OK_DONE);
        ButtonType fermerBtn = new ButtonType("Fermer", ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Activité");
        alert.setHeaderText(a.getName());
        alert.setContentText(a.getDescription());

        TextArea area = new TextArea(
                a.getDescription() + "\n\nPrix: " + a.getPrix() + " DT\nLieu: " + a.getLocalisation());
        area.setWrapText(true);
        area.setEditable(false);
        area.setFont(Font.font("System", 12));
        area.setPrefRowCount(10);

        VBox box = new VBox(10, area);
        box.setStyle("-fx-padding: 10;");
        alert.getDialogPane().setExpandableContent(box);
        alert.getDialogPane().setExpanded(true);

        alert.getButtonTypes().setAll(reserverBtn, fermerBtn);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == reserverBtn) {
            reserveActivity(a); // ✅ réservation DB
        }
    }

    /**
     * ✅ Réservation DB SANS setConverter():
     * On affiche une liste de String et on map vers Session.
     */
    private void reserveActivity(org.example.entites.Activite a) {
        try {
            List<Session> sessions = serviceSession.getByActiviteId(a.getId());

            List<Session> dispo = new ArrayList<>();
            for (Session s : sessions) {
                if (s.getNbr_places_restant() > 0)
                    dispo.add(s);
            }

            if (dispo.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Aucune session disponible pour cette activité.").showAndWait();
                return;
            }

            // Build String choices + mapping
            Map<String, Session> map = new HashMap<>();
            List<String> choices = new ArrayList<>();

            for (Session s : dispo) {
                String label = "ID=" + s.getId_session()
                        + " | " + s.getDate() + " " + s.getHeure()
                        + " | Restant=" + s.getNbr_places_restant();
                choices.add(label);
                map.put(label, s);
            }

            ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
            dialog.setTitle("Réserver");
            dialog.setHeaderText("Choisir une session pour : " + a.getName());
            dialog.setContentText("Session :");

            Optional<String> chosenStr = dialog.showAndWait();
            if (chosenStr.isEmpty())
                return;

            Session chosen = map.get(chosenStr.get());
            if (chosen == null)
                return;

            // user_ref temporaire (ton groupe remplacera plus tard)
            serviceRes.reserver(chosen.getId_session(), 1, "guest");

            new Alert(Alert.AlertType.INFORMATION,
                    "✅ Réservation confirmée !\n" +
                            "Activité: " + a.getName() + "\n" +
                            "Session: " + chosen.getDate() + " " + chosen.getHeure())
                    .showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "❌ Erreur réservation: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/RoleSelection.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 700);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("GoVibe - Accueil");
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openProposeActivity(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ProposeActivity.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 700);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("GoVibe - Proposer une activité");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}