package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.example.entities.Reclamation;
import org.example.services.ServiceReclamation;

import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class AdminReclamationController implements Initializable {

    @FXML private AdminSidebarController adminSidebarController;
    @FXML private Label lblTotal;
    @FXML private Label lblEnAttente;
    @FXML private Label lblResolu;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> comboStatus;
    @FXML private ComboBox<String> comboSort;
    @FXML private VBox reclamationsContainer;
    @FXML private VBox emptyState;

    private final ServiceReclamation service = new ServiceReclamation();
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private List<Reclamation> allReclamations;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Set active page in sidebar
        if (adminSidebarController != null) {
            adminSidebarController.setActivePage("reclamations");
        }

        // Setup filters
        comboStatus.setItems(FXCollections.observableArrayList(
            "Tous", "En attente", "En cours", "Résolu", "Rejeté"
        ));
        comboStatus.setValue("Tous");

        comboSort.setItems(FXCollections.observableArrayList(
            "Plus récent", "Plus ancien", "Par statut"
        ));
        comboSort.setValue("Plus récent");

        // Add listeners
        txtSearch.textProperty().addListener((obs, old, nw) -> filterAndDisplay());
        comboStatus.setOnAction(e -> filterAndDisplay());
        comboSort.setOnAction(e -> filterAndDisplay());

        // Load data
        loadReclamations();
    }

    private void loadReclamations() {
        try {
            allReclamations = service.getAll();
            updateStats();
            filterAndDisplay();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les réclamations: " + e.getMessage());
        }
    }

    private void updateStats() {
        int total = allReclamations.size();
        int enAttente = (int) allReclamations.stream().filter(r -> "EN_ATTENTE".equals(r.getStatus())).count();
        int resolu = (int) allReclamations.stream().filter(r -> "RESOLU".equals(r.getStatus())).count();

        lblTotal.setText(String.valueOf(total));
        lblEnAttente.setText(String.valueOf(enAttente));
        lblResolu.setText(String.valueOf(resolu));
    }

    private void filterAndDisplay() {
        if (allReclamations == null) return;

        List<Reclamation> filtered = allReclamations.stream()
            .filter(r -> {
                // Filter by status
                String status = comboStatus.getValue();
                if (status == null || "Tous".equals(status)) return true;
                return status.equalsIgnoreCase(r.getStatus().replace("_", " "));
            })
            .filter(r -> {
                // Filter by search text
                String search = txtSearch.getText().toLowerCase();
                if (search.isEmpty()) return true;
                return r.getSujet().toLowerCase().contains(search) ||
                       r.getUserNomComplet().toLowerCase().contains(search) ||
                       r.getMessage().toLowerCase().contains(search);
            })
            .sorted(getComparator())
            .toList();

        displayReclamations(filtered);
    }

    private Comparator<Reclamation> getComparator() {
        String sort = comboSort.getValue();
        if ("Plus ancien".equals(sort)) {
            return Comparator.comparing(Reclamation::getDateEnvoi);
        } else if ("Par statut".equals(sort)) {
            return Comparator.comparing(Reclamation::getStatus);
        }
        // Default: Plus récent
        return Comparator.comparing(Reclamation::getDateEnvoi).reversed();
    }

    private void displayReclamations(List<Reclamation> list) {
        reclamationsContainer.getChildren().clear();

        if (list.isEmpty()) {
            emptyState.setVisible(true);
            reclamationsContainer.getChildren().add(emptyState);
        } else {
            emptyState.setVisible(false);
            for (Reclamation r : list) {
                reclamationsContainer.getChildren().add(createReclamationCard(r));
            }
        }
    }

    private HBox createReclamationCard(Reclamation r) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(20, 25, 20, 25));
        card.setStyle(getCardStyle(r.getStatus()));

        // Left: Status indicator + User info
        VBox left = new VBox(8);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setPrefWidth(250);

        HBox statusBox = new HBox(8);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        Circle statusCircle = new Circle(6);
        statusCircle.setFill(Color.web(getStatusColor(r.getStatus())));
        Label lblStatus = new Label(formatStatus(r.getStatus()));
        lblStatus.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + getStatusColor(r.getStatus()) + ";");
        statusBox.getChildren().addAll(statusCircle, lblStatus);

        Label lblUser = new Label("👤 " + r.getUserNomComplet());
        lblUser.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #013220;");

        Label lblEmail = new Label(r.getUserEmail());
        lblEmail.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        left.getChildren().addAll(statusBox, lblUser, lblEmail);

        // Center: Sujet + Message
        VBox center = new VBox(8);
        center.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(center, Priority.ALWAYS);

        Label lblSujet = new Label(r.getSujet());
        lblSujet.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #013220;");

        Label lblMessage = new Label(r.getMessage());
        lblMessage.setStyle("-fx-font-size: 13px; -fx-text-fill: #444;");
        lblMessage.setWrapText(true);
        lblMessage.setMaxHeight(40);

        Label lblDate = new Label("📅 " + sdf.format(r.getDateEnvoi()));
        lblDate.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");

        center.getChildren().addAll(lblSujet, lblMessage, lblDate);

        // Right: Actions
        VBox right = new VBox(8);
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setPrefWidth(200);

        if (r.getReponse() != null && !r.getReponse().isEmpty()) {
            Label lblRep = new Label("✓ Répondu");
            lblRep.setStyle("-fx-font-size: 11px; -fx-text-fill: #27ae60; -fx-font-weight: 600;");
            right.getChildren().add(lblRep);
        }

        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        // View button
        Button btnView = createIconButton("Consulter", "#1565c0", "#e3f2fd");
        btnView.setOnAction(e -> showReclamationDetails(r));

        // Reply button
        Button btnEdit = createIconButton("Répondre", "#e65100", "#fff3e0");
        btnEdit.setOnAction(e -> showEditDialog(r));

        // Delete button
        Button btnDelete = createIconButton("Supprimer", "#c62828", "#ffebee");
        btnDelete.setOnAction(e -> deleteReclamation(r));

        buttons.getChildren().addAll(btnView, btnEdit, btnDelete);
        right.getChildren().add(buttons);

        card.getChildren().addAll(left, center, right);
        return card;
    }

    private String getCardStyle(String status) {
        String color = getStatusColor(status);
        return "-fx-background-color: white; -fx-background-radius: 12; " +
               "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2); " +
               "-fx-border-color: " + color + "40; -fx-border-width: 2; -fx-border-radius: 12; " +
               "-fx-cursor: hand;";
    }

    private String getStatusColor(String status) {
        switch (status) {
            case "EN_ATTENTE": return "#f39c12";
            case "EN_COURS": return "#3498db";
            case "RESOLU": return "#27ae60";
            case "REJETE": return "#e74c3c";
            default: return "#95a5a6";
        }
    }

    private String formatStatus(String status) {
        return status.replace("_", " ");
    }

    private void showReclamationDetails(Reclamation r) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détail de la Réclamation");
        dialog.setHeaderText(null);

        VBox content = new VBox(15);
        content.setPadding(new Insets(25));
        content.setStyle("-fx-background-color: #f8f9fa;");

        // User info
        VBox userBox = new VBox(5);
        userBox.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8;");
        Label lblUserHeader = new Label("👤 Utilisateur");
        lblUserHeader.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        Label lblUser = new Label(r.getUserNomComplet() + " (" + r.getUserEmail() + ")");
        lblUser.setStyle("-fx-font-size: 14px; -fx-font-weight: 600;");
        userBox.getChildren().addAll(lblUserHeader, lblUser);

        // Reclamation info
        VBox recBox = new VBox(8);
        recBox.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8;");
        
        Label lblSujet = new Label("📋 " + r.getSujet());
        lblSujet.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #013220;");
        
        Label lblMessage = new Label(r.getMessage());
        lblMessage.setStyle("-fx-font-size: 13px; -fx-text-fill: #444;");
        lblMessage.setWrapText(true);
        lblMessage.setMaxWidth(450);
        
        Label lblDate = new Label("Envoyé le: " + sdf.format(r.getDateEnvoi()));
        lblDate.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
        
        recBox.getChildren().addAll(lblSujet, lblMessage, lblDate);

        content.getChildren().addAll(userBox, recBox);

        // Response section
        if (r.getReponse() != null && !r.getReponse().isEmpty()) {
            VBox respBox = new VBox(8);
            respBox.setStyle("-fx-background-color: #e8f8f5; -fx-padding: 15; -fx-background-radius: 8;");
            
            Label lblRepHeader = new Label("💬 Réponse de l'administration");
            lblRepHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #013220;");
            
            Label lblReponse = new Label(r.getReponse());
            lblReponse.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");
            lblReponse.setWrapText(true);
            lblReponse.setMaxWidth(450);
            
            respBox.getChildren().addAll(lblRepHeader, lblReponse);
            content.getChildren().add(respBox);
        }

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(400);

        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setStyle("-fx-background-color: #f8f9fa;");

        dialog.showAndWait();
    }

    private void showEditDialog(Reclamation r) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Répondre à la Réclamation");
        dialog.setHeaderText(null);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #f8f9fa;");

        // Info
        Label lblInfo = new Label("📋 " + r.getSujet() + " - " + r.getUserNomComplet());
        lblInfo.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #013220;");

        // Status combo
        ComboBox<String> cmbStatus = new ComboBox<>(FXCollections.observableArrayList(
            "EN_ATTENTE", "EN_COURS", "RESOLU", "REJETE"
        ));
        cmbStatus.setValue(r.getStatus());
        cmbStatus.setPromptText("Statut");

        // Response textarea
        TextArea txtResponse = new TextArea(r.getReponse());
        txtResponse.setPromptText("Votre réponse...");
        txtResponse.setWrapText(true);
        txtResponse.setPrefRowCount(6);
        txtResponse.setStyle("-fx-font-size: 13px;");

        content.getChildren().addAll(lblInfo, cmbStatus, new Label("Réponse:"), txtResponse);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                return cmbStatus.getValue() + "|" + txtResponse.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            String[] parts = result.split("\\|", 2);
            try {
                // Update status in database
                service.updateStatus(r.getId(), parts[0]);

                if (parts.length == 2 && !parts[1].isEmpty()) {
                    service.repondre(r.getId(), parts[1]);
                }
                loadReclamations();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Réclamation mise à jour!");
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la mise à jour: " + e.getMessage());
            }
        });
    }

    private void deleteReclamation(Reclamation r) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText("Supprimer cette réclamation ?");
        confirm.setContentText("Cette action est irréversible.");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    service.supprimer(r.getId());
                    loadReclamations();
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Réclamation supprimée!");
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleRefresh() {
        loadReclamations();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private Button createIconButton(String text, String textColor, String bgColor) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + bgColor + "; " +
                     "-fx-text-fill: " + textColor + "; " +
                     "-fx-font-size: 11px; " +
                     "-fx-font-weight: 600; " +
                     "-fx-padding: 8 10; " +
                     "-fx-background-radius: 8; " +
                     "-fx-cursor: hand;");
        btn.setMinWidth(80);
        btn.setTooltip(new Tooltip(text));
        return btn;
    }
}
