package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.entities.Contact;
import org.example.services.ServiceContact;

import java.sql.SQLException;
import java.text.SimpleDateFormat;

public class AdminMessagesController {

    @FXML private AdminSidebarController adminSidebarController;
    @FXML private Label lblCount;
    @FXML private ComboBox<String> comboFiltre;
    @FXML private Button btnRafraichir;
    @FXML private TableView<Contact> tableMessages;
    @FXML private TableColumn<Contact, String> colClient;
    @FXML private TableColumn<Contact, String> colEmail;
    @FXML private TableColumn<Contact, String> colSujet;
    @FXML private TableColumn<Contact, String> colMessage;
    @FXML private TableColumn<Contact, String> colStatus;
    @FXML private TableColumn<Contact, String> colDate;
    @FXML private Button btnVoir;
    @FXML private Button btnRepondre;
    @FXML private Button btnModifierRep;
    @FXML private Button btnSupprimer;
    @FXML private Button btnFermer;

    private final ServiceContact serviceContact = new ServiceContact();
    private final ObservableList<Contact> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Set active page in sidebar
        if (adminSidebarController != null) {
            adminSidebarController.setActivePage("messages");
        }

        // Configurer filtre
        comboFiltre.setItems(FXCollections.observableArrayList(
            "TOUS", "EN_ATTENTE", "LU", "REPONDU", "FERME"
        ));
        comboFiltre.setValue("TOUS");
        comboFiltre.setOnAction(e -> rafraichir());

        // Configurer colonnes
        colClient.setCellValueFactory(new PropertyValueFactory<>("userNom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("userEmail"));
        colSujet.setCellValueFactory(new PropertyValueFactory<>("sujet"));
        colMessage.setCellValueFactory(new PropertyValueFactory<>("message"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDate.setCellValueFactory(cell -> {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            return new javafx.beans.property.SimpleStringProperty(
                cell.getValue().getDateEnvoi() != null ?
                sdf.format(cell.getValue().getDateEnvoi()) : ""
            );
        });

        // Style status
        colStatus.setCellFactory(column -> new TableCell<Contact, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "EN_ATTENTE" -> setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold; -fx-background-color: #fef3e2; -fx-padding: 5 10; -fx-background-radius: 10;");
                        case "LU" -> setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold; -fx-background-color: #ebf5fb; -fx-padding: 5 10; -fx-background-radius: 10;");
                        case "REPONDU" -> setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-background-color: #e8f8f5; -fx-padding: 5 10; -fx-background-radius: 10;");
                        case "FERME" -> setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold; -fx-background-color: #f2f4f4; -fx-padding: 5 10; -fx-background-radius: 10;");
                        default -> setStyle("");
                    }
                }
            }
        });

        tableMessages.setItems(data);
        rafraichir();

        // Gérer sélection
        tableMessages.getSelectionModel().selectedItemProperty().addListener((obs, old, nw) -> {
            boolean hasSelection = nw != null;
            btnVoir.setDisable(!hasSelection);
            btnRepondre.setDisable(!hasSelection);
            btnModifierRep.setDisable(!hasSelection || !nw.hasReponse());
            btnFermer.setDisable(!hasSelection);
            btnSupprimer.setDisable(!hasSelection);
        });

        btnVoir.setDisable(true);
        btnRepondre.setDisable(true);
        btnModifierRep.setDisable(true);
        btnFermer.setDisable(true);
        btnSupprimer.setDisable(true);
    }

    @FXML
    private void rafraichir() {
        try {
            data.clear();
            String filtre = comboFiltre.getValue();

            if ("TOUS".equals(filtre)) {
                data.addAll(serviceContact.getAllMessages());
            } else {
                data.addAll(serviceContact.getMessagesByStatus(filtre));
            }

            int enAttente = serviceContact.countEnAttente();
            lblCount.setText(enAttente + " message(s) en attente");
            lblCount.setStyle(enAttente > 0 ? "-fx-font-size: 14px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;" : "-fx-font-size: 14px; -fx-text-fill: #27ae60; -fx-font-weight: bold;");

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les messages: " + e.getMessage());
        }
    }

    @FXML
    private void voirDetails() {
        Contact selected = tableMessages.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        // Marquer comme lu
        if (Contact.STATUS_EN_ATTENTE.equals(selected.getStatus())) {
            try {
                serviceContact.marquerCommeLu(selected.getId());
                selected.setStatus(Contact.STATUS_LU);
                tableMessages.refresh();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Message de " + selected.getUserNom());
        alert.setHeaderText(selected.getSujet());

        String content = "📧 Message:\n" + selected.getMessage() + "\n\n" +
                        "👤 Client: " + selected.getUserNom() + "\n" +
                        "📧 Email: " + selected.getUserEmail() + "\n" +
                        "📅 Date: " + selected.getDateEnvoi();

        if (selected.hasReponse()) {
            content += "\n\n💬 Votre Réponse:\n" + selected.getReponse();
        }

        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void repondre() {
        Contact selected = tableMessages.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Répondre au message");
        dialog.setHeaderText("Répondre à " + selected.getUserNom());

        ButtonType sendBtn = new ButtonType("Envoyer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendBtn, ButtonType.CANCEL);

        TextArea txtReponse = new TextArea();
        txtReponse.setPromptText("Votre réponse...");
        txtReponse.setPrefRowCount(6);
        txtReponse.setPrefColumnCount(40);
        txtReponse.setWrapText(true);

        dialog.getDialogPane().setContent(txtReponse);

        dialog.setResultConverter(btn -> btn == sendBtn ? txtReponse.getText() : null);

        dialog.showAndWait().ifPresent(reponse -> {
            if (reponse.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Erreur", "Veuillez saisir une réponse.");
                return;
            }
            try {
                serviceContact.repondreMessage(selected.getId(), reponse);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Réponse envoyée!");
                rafraichir();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de répondre: " + e.getMessage());
            }
        });
    }

    @FXML
    private void modifierReponse() {
        Contact selected = tableMessages.getSelectionModel().getSelectedItem();
        if (selected == null || !selected.hasReponse()) return;

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Modifier la réponse");
        dialog.setHeaderText("Modifier votre réponse");

        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        TextArea txtReponse = new TextArea(selected.getReponse());
        txtReponse.setPrefRowCount(6);
        txtReponse.setPrefColumnCount(40);
        txtReponse.setWrapText(true);

        dialog.getDialogPane().setContent(txtReponse);

        dialog.setResultConverter(btn -> btn == saveBtn ? txtReponse.getText() : null);

        dialog.showAndWait().ifPresent(nouvelleReponse -> {
            try {
                serviceContact.modifierReponse(selected.getId(), nouvelleReponse);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Réponse modifiée!");
                rafraichir();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de modifier: " + e.getMessage());
            }
        });
    }

    @FXML
    private void supprimerMessage() {
        Contact selected = tableMessages.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer ce message ?");
        confirm.setContentText("Action irréversible.");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    serviceContact.supprimerMessageAdmin(selected.getId());
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Message supprimé!");
                    rafraichir();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void fermerMessage() {
        Contact selected = tableMessages.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            serviceContact.fermerMessage(selected.getId());
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Message fermé!");
            rafraichir();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de fermer: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
