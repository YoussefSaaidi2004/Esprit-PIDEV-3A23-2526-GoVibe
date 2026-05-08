package org.example.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.example.entities.Contact;
import org.example.entities.personne;
import org.example.mains.MainApp;
import org.example.services.ServiceContact;
import org.example.utils.SessionManager;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

/**
 * Controller for MesMessages.fxml — user's contact/message inbox.
 * Displays messages the user sent to the admin and lets them
 * view details, edit (if still pending), or delete a message.
 */
public class MesMessagesController {

    @FXML private Button btnRetour;
    @FXML private Button btnNouveauMessage;
    @FXML private TableView<Contact> tableMessages;
    @FXML private TableColumn<Contact, String> colSujet;
    @FXML private TableColumn<Contact, String> colMessage;
    @FXML private TableColumn<Contact, String> colStatus;
    @FXML private TableColumn<Contact, String> colDate;
    @FXML private TableColumn<Contact, String> colReponse;
    @FXML private Button btnVoirDetails;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;

    private final ServiceContact serviceContact = new ServiceContact();
    private final ObservableList<Contact> data = FXCollections.observableArrayList();
    private personne currentUser;

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Vous devez être connecté pour voir vos messages.");
            navigateTo("/org/example/LoginView.fxml", "GoVibe — Login");
            return;
        }

        // Column value factories
        colSujet.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getSujet()));

        colMessage.setCellValueFactory(cell -> {
            String msg = cell.getValue().getMessage();
            return new SimpleStringProperty(
                    msg != null && msg.length() > 60 ? msg.substring(0, 60) + "…" : msg);
        });

        colStatus.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getStatus()));

        colDate.setCellValueFactory(cell -> {
            var ts = cell.getValue().getDateEnvoi();
            return new SimpleStringProperty(ts != null ? DATE_FMT.format(ts) : "");
        });

        colReponse.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().hasReponse() ? "✅ Répondu" : "⏳ En attente"));

        // Colour-code the status column
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(switch (item) {
                    case Contact.STATUS_EN_ATTENTE -> "-fx-text-fill: #f39c12; -fx-font-weight: bold;";
                    case Contact.STATUS_LU         -> "-fx-text-fill: #3498db; -fx-font-weight: bold;";
                    case Contact.STATUS_REPONDU    -> "-fx-text-fill: #27ae60; -fx-font-weight: bold;";
                    case Contact.STATUS_FERME      -> "-fx-text-fill: #7f8c8d; -fx-font-weight: bold;";
                    default -> "";
                });
            }
        });

        tableMessages.setItems(data);

        // Enable action buttons only when there is a selection
        tableMessages.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> refreshButtonState(selected));

        refreshButtonState(null);
        chargerMessages();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    private void retourProfil() {
        navigateTo("/org/example/UserProfileView.fxml", "Mon Profil");
    }

    @FXML
    private void nouveauMessage() {
        navigateTo("/NouveauMessage.fxml", "Nouveau Message");
    }

    // ── CRUD actions ──────────────────────────────────────────────────────────

    @FXML
    private void voirDetails() {
        Contact sel = selected();
        if (sel == null) return;

        String body = "📧 Votre Message:\n" + sel.getMessage() + "\n\n";
        body += sel.hasReponse()
                ? "✅ Réponse de l'Admin:\n" + sel.getReponse()
                : "⏳ En attente de réponse de l'administrateur.";

        Alert dlg = new Alert(Alert.AlertType.INFORMATION);
        dlg.setTitle("Détails du Message");
        dlg.setHeaderText(sel.getSujet());
        dlg.setContentText(body);
        dlg.getDialogPane().setPrefWidth(520);
        dlg.showAndWait();
    }

    @FXML
    private void modifierMessage() {
        Contact sel = selected();
        if (sel == null) return;

        // Build an inline edit dialog
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Modifier le Message");
        dlg.setHeaderText("Modifier : " + sel.getSujet());

        TextField sujetField = new TextField(sel.getSujet());
        TextArea msgArea = new TextArea(sel.getMessage());
        msgArea.setWrapText(true);
        msgArea.setPrefRowCount(5);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Sujet :"), 0, 0);   grid.add(sujetField, 1, 0);
        grid.add(new Label("Message :"), 0, 1); grid.add(msgArea,    1, 1);

        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dlg.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            sel.setSujet(sujetField.getText().trim());
            sel.setMessage(msgArea.getText().trim());
            try {
                serviceContact.modifierMessage(sel);
                chargerMessages();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur de modification",
                        "Impossible de modifier : " + e.getMessage());
            }
        }
    }

    @FXML
    private void supprimerMessage() {
        Contact sel = selected();
        if (sel == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer le message « " + sel.getSujet() + " » ?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmer la suppression");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    serviceContact.supprimerMessage(sel.getId(), currentUser.getId());
                    chargerMessages();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur de suppression",
                            "Impossible de supprimer : " + e.getMessage());
                }
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void chargerMessages() {
        data.clear();
        try {
            List<Contact> msgs = serviceContact.getMessagesByUser(currentUser.getId());
            data.addAll(msgs);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Chargement impossible",
                    "Impossible de charger vos messages : " + e.getMessage());
        }
    }

    private void refreshButtonState(Contact selected) {
        boolean has = selected != null;
        boolean canEdit = has && Contact.STATUS_EN_ATTENTE.equals(selected.getStatus());
        btnVoirDetails.setDisable(!has);
        btnModifier.setDisable(!canEdit);
        btnSupprimer.setDisable(!canEdit);
    }

    private Contact selected() {
        Contact sel = tableMessages.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection requise",
                    "Veuillez sélectionner un message dans la liste.");
        }
        return sel;
    }

    private void navigateTo(String fxml, String title) {
        try {
            MainApp.switchScene(fxml, title);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation impossible", e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
