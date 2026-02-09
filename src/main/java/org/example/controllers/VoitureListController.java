package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.entities.Voiture;
import org.example.services.IService;
import org.example.services.ServiceVoiture;
import org.example.utils.SceneNavigator;
import org.example.utils.VoitureSelection;

import java.util.List;

public class VoitureListController {

    @FXML
    private ListView<Voiture> voitureList;
    @FXML
    private Button addPageButton;

    private final ObservableList<Voiture> voitureItems = FXCollections.observableArrayList();
    private final IService<Voiture> voitureService = new ServiceVoiture();

    @FXML
    public void initialize() {
        voitureList.setItems(voitureItems);
        voitureList.setCellFactory(list -> new VoitureCell());
        refreshList();
    }

    @FXML
    private void handleOpenAdd() {
        SceneNavigator.switchTo("/VoitureAddView.fxml", addPageButton);
    }

    private void refreshList() {
        List<Voiture> voitures = voitureService.getAll();
        voitureItems.setAll(voitures);
    }

    private void openEdit(Voiture voiture, Button source) {
        VoitureSelection.set(voiture);
        SceneNavigator.switchTo("/VoitureEditView.fxml", source);
    }

    private void deleteFromRow(Voiture item) {
        try {
            voitureService.delete(item.getIdVoiture());
            refreshList();
        } catch (RuntimeException ex) {
            showAlert("Erreur", ex.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private class VoitureCell extends ListCell<Voiture> {
        private final HBox root = new HBox();
        private final ImageView thumbnail = new ImageView();
        private final VBox left = new VBox();
        private final VBox right = new VBox();
        private final HBox center = new HBox();
        private final Label title = new Label();
        private final Label subtitle = new Label();
        private final Label meta = new Label();
        private final Label price = new Label();
        private final Label status = new Label();
        private final Button editButton = new Button("Modifier");
        private final Button deleteButton = new Button("Supprimer");
        private final Region leftSpacer = new Region();
        private final Region rightSpacer = new Region();

        VoitureCell() {
            root.getStyleClass().add("voiture-cell");
            thumbnail.getStyleClass().add("voiture-thumb");
            thumbnail.setPreserveRatio(true);
            thumbnail.setFitWidth(64);
            thumbnail.setFitHeight(64);
            left.getStyleClass().add("voiture-cell-left");
            right.getStyleClass().add("voiture-cell-right");
            center.getStyleClass().add("voiture-cell-center");
            title.getStyleClass().add("voiture-title");
            subtitle.getStyleClass().add("voiture-subtitle");
            meta.getStyleClass().add("voiture-meta");
            price.getStyleClass().add("voiture-price");
            status.getStyleClass().add("voiture-status");
            editButton.getStyleClass().addAll("row-btn", "row-btn-edit");
            deleteButton.getStyleClass().addAll("row-btn", "row-btn-delete");

            left.getChildren().addAll(title, subtitle, meta);
            center.getChildren().addAll(editButton, deleteButton);
            right.getChildren().addAll(price, status);

            HBox.setHgrow(leftSpacer, Priority.ALWAYS);
            HBox.setHgrow(rightSpacer, Priority.ALWAYS);
            root.getChildren().addAll(thumbnail, left, leftSpacer, center, rightSpacer, right);
            setText(null);
        }

        @Override
        protected void updateItem(Voiture item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            title.setText(item.getMarque() + " " + item.getModele() + " · " + item.getMatricule());
            subtitle.setText("Annee " + item.getAnnee() + " · " + item.getTypeCarburant());
            meta.setText(item.getAdresseAgence());
            price.setText(String.format("%.2f TND / jour", item.getPrixJour()));
            status.setText(item.getStatut().toString());

            String imageUrl = item.getImageUrl();
            if (imageUrl == null) {
                thumbnail.setImage(null);
            } else {
                String trimmedUrl = imageUrl.trim();
                if (trimmedUrl.isEmpty() || !isHttpUrl(trimmedUrl)) {
                    thumbnail.setImage(null);
                } else {
                    try {
                        thumbnail.setImage(new Image(trimmedUrl, 64, 64, true, true, true));
                    } catch (IllegalArgumentException ex) {
                        thumbnail.setImage(null);
                    }
                }
            }

            editButton.setOnAction(event -> openEdit(item, editButton));
            deleteButton.setOnAction(event -> deleteFromRow(item));

            setGraphic(root);
        }
    }

    private static boolean isHttpUrl(String value) {
        String lower = value.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }
}
