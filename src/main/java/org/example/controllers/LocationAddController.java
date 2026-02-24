package org.example.controllers;

import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import org.example.entities.Location;
import org.example.entities.Statut;
import org.example.entities.StatutLocation;
import org.example.entities.Voiture;
import org.example.services.BookNowClient;
import org.example.services.IService;
import org.example.services.ServiceLocation;
import org.example.services.ServiceVoiture;
import org.example.utils.SessionManager;
import org.example.utils.SceneNavigator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class LocationAddController {

    @FXML
    private TextField referenceField;
    @FXML
    private ComboBox<Voiture> voitureBox;
    @FXML
    private DatePicker dateDebutPicker;
    @FXML
    private DatePicker dateFinPicker;
    @FXML
    private TextField nbJoursField;
    @FXML
    private TextField montantTotalField;
    @FXML
    private ComboBox<StatutLocation> statutBox;

    @FXML
    private Button addButton;
    @FXML
    private Button backButton;

    private LocationListController parentController;

    public void setParentController(LocationListController parentController) {
        this.parentController = parentController;
    }

    private final IService<Location> locationService = new ServiceLocation();
    private final IService<Voiture> voitureService = new ServiceVoiture();
    private final BookNowClient bookNowClient = new BookNowClient();

    @FXML
    public void initialize() {
        if (!SessionManager.isAuthenticated()) {
            SceneNavigator.switchTo("/org/example/LoginView.fxml", backButton);
            return;
        }
        if (SessionManager.isAdmin()) {
            SceneNavigator.switchTo("/AdminLocationListView.fxml", backButton);
            return;
        }
        statutBox.setItems(FXCollections.observableArrayList(StatutLocation.values()));
        statutBox.setValue(StatutLocation.EN_ATTENTE);
        statutBox.setDisable(true);
        loadAvailableVoitures();
        setupAutoCompute();
    }

    @FXML
    private void handleAjouter() {
        if (!validateFields()) {
            return;
        }
        Location location = buildLocationFromFields();
        try {
            String reference = location.getReference();
            Path baseDir = ensureGeneratedDir();
            Path contractPath = generateContractPdf(baseDir, reference, location);
            Path qrPath = generateQrCode(baseDir, reference);
            location.setContratPdf(contractPath.toString());
            location.setQrCode(qrPath.toString());
        } catch (IOException ex) {
            showAlert("Erreur", "Erreur lors de la generation du contrat ou QR code.");
            return;
        }
        try {
            locationService.add(location);
            if (parentController != null) {
                parentController.closeModal();
            } else {
                SceneNavigator.switchTo("/LocationListView.fxml", addButton);
            }
        } catch (RuntimeException ex) {
            showAlert("Erreur", ex.getMessage());
        }
    }

    @FXML
    private void handleRetour() {
        if (parentController != null) {
            parentController.closeModal();
        } else {
            SceneNavigator.switchTo("/LocationListView.fxml", addButton);
        }
    }

    @FXML
    private void handleGoHome() {
        SceneNavigator.switchTo("/UserHome.fxml", backButton);
    }

    @FXML
    private void handleGoLocations() {
        SceneNavigator.switchTo("/LocationListView.fxml", backButton);
    }

    @FXML
    private void handleLogout() {
        SessionManager.clear();
        SceneNavigator.switchTo("/org/example/LoginView.fxml", backButton);
    }

    private void loadAvailableVoitures() {
        List<Voiture> disponibles;
        
        // Try getting real-time fleet from BookNow API
        if (bookNowClient.testConnection()) {
            System.out.println("[LocationAdd] Fetching real-time fleet from BookNow API...");
            disponibles = bookNowClient.getAvailableCars();
        } else {
            // Fallback to local database
            System.out.println("[LocationAdd] BookNow API offline, falling back to local DB.");
            List<Voiture> voitures = voitureService.getAll();
            disponibles = voitures.stream()
                    .filter(voiture -> voiture.getStatut() == Statut.DISPONIBLE)
                    .collect(Collectors.toList());
        }

        voitureBox.setItems(FXCollections.observableArrayList(disponibles));
        voitureBox.setConverter(new javafx.util.StringConverter<Voiture>() {
            @Override
            public String toString(Voiture object) {
                if (object == null) {
                    return "";
                }
                return String.format(Locale.ROOT, "%s %s · %s", safeTrim(object.getMarque()), safeTrim(object.getModele()), safeTrim(object.getMatricule()));
            }

            @Override
            public Voiture fromString(String string) {
                return null;
            }
        });
    }

    private void setupAutoCompute() {
        dateDebutPicker.valueProperty().addListener((obs, oldValue, newValue) -> recomputeFields());
        dateFinPicker.valueProperty().addListener((obs, oldValue, newValue) -> recomputeFields());
        voitureBox.valueProperty().addListener((obs, oldValue, newValue) -> recomputeFields());
    }

    private void recomputeFields() {
        LocalDate debut = dateDebutPicker.getValue();
        LocalDate fin = dateFinPicker.getValue();
        Voiture voiture = voitureBox.getValue();
        if (debut == null || fin == null || voiture == null) {
            return;
        }
        if (fin.isBefore(debut)) {
            return;
        }
        int nbJours = (int) ChronoUnit.DAYS.between(debut, fin) + 1;
        double montantTotal = nbJours * voiture.getPrixJour();
        nbJoursField.setText(String.valueOf(nbJours));
        montantTotalField.setText(String.format(Locale.ROOT, "%.2f", montantTotal));
    }

    private Location buildLocationFromFields() {
        Voiture voiture = voitureBox.getValue();
        int userId = 0;
        if (SessionManager.getCurrentUser() != null) {
            userId = SessionManager.getCurrentUser().getId();
        }
        return new Location(
                referenceField.getText().trim(),
                dateDebutPicker.getValue(),
                dateFinPicker.getValue(),
                Integer.parseInt(nbJoursField.getText().trim()),
                Double.parseDouble(montantTotalField.getText().trim()),
                "",
                "",
                StatutLocation.EN_ATTENTE,
                voiture != null ? voiture.getIdVoiture() : 0,
                userId
        );
    }

    private boolean validateFields() {
        clearValidationStyles();
        boolean valid = true;

        if (isBlank(referenceField)) {
            markInvalid(referenceField);
            valid = false;
        }
        if (voitureBox.getValue() == null) {
            markInvalid(voitureBox);
            valid = false;
        }
        if (dateDebutPicker.getValue() == null) {
            markInvalid(dateDebutPicker);
            valid = false;
        }
        if (dateFinPicker.getValue() == null) {
            markInvalid(dateFinPicker);
            valid = false;
        }
        if (isBlank(nbJoursField)) {
            markInvalid(nbJoursField);
            valid = false;
        }
        if (isBlank(montantTotalField)) {
            markInvalid(montantTotalField);
            valid = false;
        }
        if (statutBox.getValue() == null) {
            markInvalid(statutBox);
            valid = false;
        }

        if (dateDebutPicker.getValue() != null && dateFinPicker.getValue() != null) {
            if (dateFinPicker.getValue().isBefore(dateDebutPicker.getValue())) {
                markInvalid(dateFinPicker);
                valid = false;
            }
        }

        if (!isBlank(nbJoursField)) {
            try {
                Integer.parseInt(nbJoursField.getText().trim());
            } catch (NumberFormatException ex) {
                markInvalid(nbJoursField);
                valid = false;
            }
        }
        if (!isBlank(montantTotalField)) {
            try {
                Double.parseDouble(montantTotalField.getText().trim());
            } catch (NumberFormatException ex) {
                markInvalid(montantTotalField);
                valid = false;
            }
        }
        return valid;
    }

    private boolean isBlank(TextField field) {
        return field.getText() == null || field.getText().trim().isEmpty();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearValidationStyles() {
        removeErrorStyle(referenceField);
        removeErrorStyle(voitureBox);
        removeErrorStyle(dateDebutPicker);
        removeErrorStyle(dateFinPicker);
        removeErrorStyle(nbJoursField);
        removeErrorStyle(montantTotalField);
        removeErrorStyle(statutBox);
    }

    private void markInvalid(Node node) {
        if (!node.getStyleClass().contains("field-error")) {
            node.getStyleClass().add("field-error");
        }
        playShake(node);
    }

    private void removeErrorStyle(Node node) {
        node.getStyleClass().remove("field-error");
    }

    private void playShake(Node node) {
        TranslateTransition transition = new TranslateTransition(Duration.millis(220), node);
        transition.setFromX(0);
        transition.setByX(8);
        transition.setCycleCount(4);
        transition.setAutoReverse(true);
        transition.playFromStart();
    }

    private Path ensureGeneratedDir() throws IOException {
        String baseDir = System.getProperty("user.dir");
        Path dir = Paths.get(baseDir, "generated", "locations");
        Files.createDirectories(dir);
        return dir;
    }

    private Path generateContractPdf(Path baseDir, String reference, Location location) throws IOException {
        String safeRef = sanitizeReference(reference);
        Path path = baseDir.resolve("contrat_" + safeRef + "_" + System.currentTimeMillis() + ".pdf");
        double montantHt = location.getMontantTotal();
        double tauxTva = 0.19;
        double tva = montantHt * tauxTva;
        double totalTtc = montantHt + tva;
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float margin = 50;
                float y = page.getMediaBox().getHeight() - margin;
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, y);
                contentStream.showText("Contrat de location");
                contentStream.endText();
                y -= 28;

                contentStream.setFont(PDType1Font.HELVETICA, 11);
                y = writeLine(contentStream, "Reference: " + reference, margin, y);
                y = writeLine(contentStream, "Date debut: " + location.getDateDebut(), margin, y);
                y = writeLine(contentStream, "Date fin: " + location.getDateFin(), margin, y);
                y = writeLine(contentStream, "Nb jours: " + location.getNbJours(), margin, y);
                y -= 6;
                y = writeLine(contentStream, String.format(Locale.ROOT, "Montant HT: %.2f TND", montantHt), margin, y);
                y = writeLine(contentStream, String.format(Locale.ROOT, "TVA (19%%): %.2f TND", tva), margin, y);
                y = writeLine(contentStream, String.format(Locale.ROOT, "Total TTC: %.2f TND", totalTtc), margin, y);
                if (location.getIdVoiture() > 0) {
                    y = writeLine(contentStream, "Voiture ID: " + location.getIdVoiture(), margin, y);
                }
                y -= 10;
                y = writeLine(contentStream, "Remerciements:", margin, y);
                y = writeLine(contentStream, "Merci pour votre confiance et votre fidelite.", margin, y);
                y = writeLine(contentStream, "Nous vous souhaitons une excellente route.", margin, y);
            }
            document.save(path.toFile());
        }
        return path;
    }

    private Path generateQrCode(Path baseDir, String reference) throws IOException {
        String safeRef = sanitizeReference(reference);
        Path path = baseDir.resolve("qr_" + safeRef + "_" + System.currentTimeMillis() + ".png");
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(reference, BarcodeFormat.QR_CODE, 240, 240);
            MatrixToImageWriter.writeToPath(matrix, "PNG", path);
        } catch (Exception ex) {
            throw new IOException("Erreur lors de la generation du QR code", ex);
        }
        return path;
    }

    private float writeLine(PDPageContentStream contentStream, String text, float x, float y) throws IOException {
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
        return y - 16;
    }

    private String sanitizeReference(String reference) {
        if (reference == null || reference.trim().isEmpty()) {
            return "location";
        }
        return reference.trim().replaceAll("[^a-zA-Z0-9-_]", "_");
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
