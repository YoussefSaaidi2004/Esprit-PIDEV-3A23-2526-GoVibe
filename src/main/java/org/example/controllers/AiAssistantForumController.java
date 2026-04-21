package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import org.example.entities.Voiture;
import org.example.services.AiRecommendationService;
import java.util.List;
import javafx.application.Platform;

public class AiAssistantForumController {

    @FXML private TextField budgetField;
    @FXML private TextField destinationField;
    @FXML private VBox carsList;
    @FXML private Label statusLabel;
    @FXML private VBox resultsContainer;

    private final AiRecommendationService aiService = new AiRecommendationService();
    private LocationListController parentController;

    public void setParentController(LocationListController parentController) {
        this.parentController = parentController;
    }

    @FXML
    private void handleGenerate() {
        String budgetStr = budgetField.getText().trim();
        String destination = destinationField.getText().trim();

        if (budgetStr.isEmpty()) {
            statusLabel.setText("⚠️ Veuillez entrer un budget.");
            statusLabel.setStyle("-fx-text-fill: #FF6B6B;");
            return;
        }

        try {
            double budget = Double.parseDouble(budgetStr.replaceAll("[^0-9.]", ""));
            
            statusLabel.setText("🤖 Le robot réfléchit... (Interrogation DeepSeek)");
            statusLabel.setStyle("-fx-text-fill: #50C878;");
            carsList.getChildren().clear();

            // Run in background to keep UI responsive
            new Thread(() -> {
                List<Voiture> recos = aiService.recommendCars(budget, destination);
                
                Platform.runLater(() -> {
                    if (recos.isEmpty()) {
                        statusLabel.setText("❌ Aucune voiture trouvée pour ce budget.");
                    } else {
                        statusLabel.setText("✨ Voici ce que j'ai trouvé pour vous :");
                        for (Voiture v : recos) {
                            addCarCard(v);
                        }
                    }
                });
            }).start();

        } catch (NumberFormatException e) {
            statusLabel.setText("⚠️ Budget invalide.");
            statusLabel.setStyle("-fx-text-fill: #FF6B6B;");
        }
    }

    private void addCarCard(Voiture v) {
        VBox card = new VBox(12);
        card.getStyleClass().add("glass-panel-light");
        card.setStyle("-fx-padding: 20; -fx-background-radius: 18; -fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 1; -fx-background-color: rgba(255,255,255,0.03);");

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label icon = new Label("🏎️");
        icon.setStyle("-fx-font-size: 18;");
        
        Label title = new Label(v.getMarque() + " " + v.getModele());
        title.setStyle("-fx-text-fill: white; -fx-font-weight: 800; -fx-font-size: 15;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Label price = new Label(v.getPrixJour() + " DT/j");
        price.setStyle("-fx-text-fill: #50C878; -fx-font-weight: 900; -fx-background-color: rgba(80,200,120,0.1); -fx-padding: 4 10; -fx-background-radius: 8;");
        
        header.getChildren().addAll(icon, title, spacer, price);

        Label desc = new Label(v.getDescription() != null ? v.getDescription() : "Véhicule de luxe recommandé par l'IA.");
        desc.setWrapText(true);
        desc.setMaxWidth(400);
        desc.setStyle("-fx-text-fill: rgba(255,255,255,0.65); -fx-font-size: 11.5; -fx-line-spacing: 2;");

        HBox actions = new HBox(10);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        
        Button mapBtn = new Button("📍 Localiser & Détails");
        mapBtn.getStyleClass().add("btn-emerald-mini");
        mapBtn.setStyle("-fx-font-size: 11; -fx-padding: 8 15; -fx-background-radius: 10;");
        mapBtn.setOnAction(e -> {
            if (parentController != null) {
                parentController.highlightCarOnMap(v);
            }
        });

        actions.getChildren().add(mapBtn);
        card.getChildren().addAll(header, desc, actions);
        
        // Add subtle entrance animation
        card.setOpacity(0);
        card.setTranslateY(10);
        carsList.getChildren().add(card);
        
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), card);
        ft.setToValue(1.0);
        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(300), card);
        tt.setToY(0);
        new javafx.animation.ParallelTransition(ft, tt).play();
    }

    @FXML
    private void handleClose() {
        if (parentController != null) {
            parentController.closeModal();
        }
    }
}
