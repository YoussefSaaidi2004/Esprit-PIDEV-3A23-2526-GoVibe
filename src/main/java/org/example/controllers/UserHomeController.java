package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserHomeController {

    @FXML private Label welcomeLabel;
    @FXML private TilePane activitiesPane;

    // ✅ Données 100% dans le code
    private static class ActivityData {
        String name;
        String shortDesc;
        String longDetails;
        String imageFile; // ex: randonnee-cap-bon.png

        ActivityData(String name, String shortDesc, String longDetails, String imageFile) {
            this.name = name;
            this.shortDesc = shortDesc;
            this.longDetails = longDetails;
            this.imageFile = imageFile;
        }
    }

    private final List<ActivityData> activities = new ArrayList<>();

    @FXML
    public void initialize() {
        welcomeLabel.setText("Bonjour !");

        // ✅ 6 activités (images dans src/main/resources/images/)
        activities.add(new ActivityData(
                "Randonnée Cap Bon",
                "Randonnée nature avec guide local.",
                "📍 Lieu: Cap Bon\n⏱ Durée: 4h\n🎯 Niveau: Moyen\n✅ Inclus: Guide + eau\n🎒 À prendre: chaussures, casquette",
                "randonnee-cap-bon.png"
        ));

        activities.add(new ActivityData(
                "Visite Médina Tunis",
                "Découverte de la médina et du patrimoine.",
                "📍 Lieu: Tunis\n⏱ Durée: 2h30\n✅ Inclus: Guide\n📌 Point de départ: Bab Bhar\n💡 Conseil: venez tôt",
                "visite-medina-tunis.png"
        ));

        activities.add(new ActivityData(
                "Kayak Ghar El Melh",
                "Balade en kayak dans un cadre magnifique.",
                "📍 Lieu: Ghar El Melh\n⏱ Durée: 2h\n🎯 Niveau: Facile\n✅ Inclus: gilet + pagaie\n🛟 Sécurité: briefing avant départ",
                "kayak-ghar-el-melh.png"
        ));

        activities.add(new ActivityData(
                "Atelier Poterie Nabeul",
                "Atelier artisanal (initiation poterie).",
                "📍 Lieu: Nabeul\n⏱ Durée: 1h30\n✅ Inclus: matériel\n🎁 Vous repartez avec votre création",
                "atelier-poterie-nabeul.png"
        ));

        activities.add(new ActivityData(
                "Dégustation Huile d’Olive",
                "Dégustation de produits locaux.",
                "📍 Lieu: Domaine local\n⏱ Durée: 1h\n✅ Inclus: dégustation\n⭐ Bonus: conseils de conservation",
                "degustation-huile-olive.png"
        ));

        activities.add(new ActivityData(
                "Balade Sidi Bou Saïd",
                "Visite panoramique + spots photo.",
                "📍 Lieu: Sidi Bou Saïd\n⏱ Durée: 2h\n✅ Inclus: guide\n🌅 Conseil: coucher de soleil recommandé",
                "balade-sidi-bou-said.png"
        ));

        loadCards();
    }

    private void loadCards() {
        activitiesPane.getChildren().clear();
        for (ActivityData a : activities) {
            activitiesPane.getChildren().add(createCard(a));
        }
    }

    private Node createCard(ActivityData a) {
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

        ImageView img = new ImageView(loadImageFromResources(a.imageFile));
        img.setFitWidth(276);
        img.setFitHeight(150);
        img.setPreserveRatio(false);
        img.setSmooth(true);

        Label title = new Label(a.name);
        title.setWrapText(true);
        title.setStyle("-fx-text-fill: #013220; -fx-font-size: 15px; -fx-font-weight: bold;");

        Label desc = new Label(a.shortDesc);
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #084E36; -fx-font-size: 12px;");

        Region spacer = new Region();
        spacer.setMinHeight(4);

        HBox footer = new HBox();
        Label hint = new Label("Clic → Réserver | Détails");
        hint.setStyle("-fx-text-fill: #50C878; -fx-font-size: 11px; -fx-font-weight: bold;");
        footer.getChildren().add(hint);

        card.getChildren().addAll(img, title, desc, spacer, footer);

        // ✅ 1 clic -> popup avec bouton Réserver
        card.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1) {
                showDetailsWithReservation(a);
            }
        });

        return card;
    }

    // ✅ charge depuis src/main/resources/images/<fileName>
    private Image loadImageFromResources(String fileName) {
        URL url = getClass().getResource("/images/" + fileName);
        if (url != null) return new Image(url.toExternalForm(), true);

        URL placeholder = getClass().getResource("/images/placeholder.png");
        if (placeholder != null) return new Image(placeholder.toExternalForm(), true);

        return null;
    }

    // ✅ Popup Détails + bouton Réserver
    private void showDetailsWithReservation(ActivityData a) {

        ButtonType reserverBtn = new ButtonType("Réserver", ButtonBar.ButtonData.OK_DONE);
        ButtonType fermerBtn = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Activité");
        alert.setHeaderText(a.name);
        alert.setContentText(a.shortDesc);

        // ✅ Zone détails (multi-lignes)
        TextArea area = new TextArea(a.longDetails);
        area.setWrapText(true);
        area.setEditable(false);
        area.setFont(Font.font("System", 12));
        area.setPrefRowCount(10);

        VBox box = new VBox(10, area);
        box.setStyle("-fx-padding: 10;");

        alert.getDialogPane().setExpandableContent(box);
        alert.getDialogPane().setExpanded(true);

        // ✅ Ajout des boutons
        alert.getButtonTypes().setAll(reserverBtn, fermerBtn);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == reserverBtn) {
            reserveActivity(a);
        }
    }

    // ✅ Action de réservation (à connecter plus tard à ta vraie logique)
    private void reserveActivity(ActivityData a) {
        Alert ok = new Alert(Alert.AlertType.INFORMATION);
        ok.setTitle("Réservation");
        ok.setHeaderText("Réservation confirmée ✅");
        ok.setContentText("Vous avez réservé : " + a.name);
        ok.showAndWait();

        // TODO: ici tu peux ouvrir Reservation.fxml ou envoyer vers DB etc.
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Dashboard.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 700);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("GoVibe - Dashboard Activités");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}