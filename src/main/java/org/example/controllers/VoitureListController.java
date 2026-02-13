package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.entities.Statut;
import org.example.entities.Voiture;
import org.example.services.IService;
import org.example.services.ServiceVoiture;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;
import org.example.utils.VoitureSelection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class VoitureListController {

    private static final String ALL_AGENCIES = "Toutes";
    private static final String SORT_DEFAULT = "Par defaut";

    @FXML
    private BorderPane root;
    @FXML
    private ListView<Voiture> voitureList;
    @FXML
    private Button addPageButton;
    @FXML
    private Button locationListButton;
    @FXML
    private Button adminLocationButton;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> agenceFilter;
    @FXML
    private ComboBox<String> sortChoice;
    @FXML
    private ComboBox<Integer> pageSizeBox;
    @FXML
    private Button exportPdfButton;
    @FXML
    private Button exportExcelButton;
    @FXML
    private Button themeToggleButton;
    @FXML
    private Button prevPageButton;
    @FXML
    private Button nextPageButton;
    @FXML
    private Label pageInfoLabel;
    @FXML
    private Label totalCountLabel;
    @FXML
    private Label availableCountLabel;
    @FXML
    private Label avgPriceLabel;
    @FXML
    private Label topAgenceLabel;

    private final ObservableList<Voiture> masterItems = FXCollections.observableArrayList();
    private final ObservableList<Voiture> pageItems = FXCollections.observableArrayList();
    private final IService<Voiture> voitureService = new ServiceVoiture();
    private List<Voiture> currentFiltered = new ArrayList<>();
    private int currentPageIndex = 0;
    private int pageSize = 6;

    @FXML
    public void initialize() {
        if (!SessionManager.isAuthenticated()) {
            SceneNavigator.switchTo("/org/example/LoginView.fxml", root);
            return;
        }
        if (!SessionManager.isAdmin()) {
            SceneNavigator.switchTo("/LocationListView.fxml", root);
            return;
        }
        adminLocationButton.setVisible(true);
        adminLocationButton.setManaged(true);
        if (locationListButton != null) {
            locationListButton.setVisible(false);
            locationListButton.setManaged(false);
        }
        voitureList.setItems(pageItems);
        voitureList.setCellFactory(list -> new VoitureCell());
        setupFilters();
        refreshList();
    }

    @FXML
    private void handleGoDashboard() {
        SceneNavigator.switchTo("/org/example/AdminDashboardView.fxml", root);
    }

    @FXML
    private void handleGoPersonnes() {
        SceneNavigator.switchTo("/org/example/PersonneView.fxml", root);
    }

    @FXML
    private void handleGoAdminLocations() {
        SceneNavigator.switchTo("/AdminLocationListView.fxml", root);
    }

    @FXML
    private void handleLogout() {
        SessionManager.clear();
        SceneNavigator.switchTo("/org/example/LoginView.fxml", root);
    }

    @FXML
    private void handleOpenAdd() {
        SceneNavigator.switchTo("/VoitureAddView.fxml", addPageButton);
    }

    @FXML
    private void handleOpenLocations() {
        SceneNavigator.switchTo("/LocationListView.fxml", locationListButton);
    }

    @FXML
    private void handleOpenAdminLocations() {
        SceneNavigator.switchTo("/AdminLocationListView.fxml", adminLocationButton);
    }

    @FXML
    private void handleExportPdf() {
        if (currentFiltered.isEmpty()) {
            showAlert("Export", "Aucune voiture a exporter.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = chooser.showSaveDialog(exportPdfButton.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            exportPdf(file, currentFiltered);
            showAlert("Export", "Export PDF termine.");
        } catch (IOException ex) {
            showAlert("Erreur", "Erreur lors de l'export PDF.");
        }
    }

    @FXML
    private void handleExportExcel() {
        if (currentFiltered.isEmpty()) {
            showAlert("Export", "Aucune voiture a exporter.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter Excel");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File file = chooser.showSaveDialog(exportExcelButton.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            exportExcel(file, currentFiltered);
            showAlert("Export", "Export Excel termine.");
        } catch (IOException ex) {
            showAlert("Erreur", "Erreur lors de l'export Excel.");
        }
    }

    @FXML
    private void handleToggleTheme() {
        if (root.getStyleClass().contains("theme-dark")) {
            root.getStyleClass().remove("theme-dark");
            themeToggleButton.setText("Mode sombre");
        } else {
            root.getStyleClass().add("theme-dark");
            themeToggleButton.setText("Mode clair");
        }
    }

    @FXML
    private void handlePrevPage() {
        if (currentPageIndex > 0) {
            currentPageIndex--;
            updatePageItems();
            updatePaginationControls();
        }
    }

    @FXML
    private void handleNextPage() {
        int totalPages = getTotalPages();
        if (currentPageIndex < totalPages - 1) {
            currentPageIndex++;
            updatePageItems();
            updatePaginationControls();
        }
    }

    private void refreshList() {
        List<Voiture> voitures = voitureService.getAll();
        masterItems.setAll(voitures);
        updateAgenceOptions();
        applyFiltersAndSort();
    }

    private void setupFilters() {
        sortChoice.setItems(FXCollections.observableArrayList(
                SORT_DEFAULT,
                "Prix (asc)",
                "Prix (desc)",
                "Annee (desc)",
                "Marque (A-Z)",
                "Date ajout (desc)"
        ));
        sortChoice.setValue(SORT_DEFAULT);
        pageSizeBox.setItems(FXCollections.observableArrayList(6, 10, 15, 20));
        pageSizeBox.setValue(pageSize);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFiltersAndSort());
        agenceFilter.setOnAction(event -> applyFiltersAndSort());
        sortChoice.setOnAction(event -> applyFiltersAndSort());
        pageSizeBox.setOnAction(event -> {
            Integer value = pageSizeBox.getValue();
            if (value != null && value > 0) {
                pageSize = value;
                currentPageIndex = 0;
                updatePaginationControls();
            }
        });
    }

    private void updateAgenceOptions() {
        String currentSelection = agenceFilter.getValue();
        Set<String> agencies = masterItems.stream()
                .map(Voiture::getAdresseAgence)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));
        List<String> options = new ArrayList<>();
        options.add(ALL_AGENCIES);
        options.addAll(agencies);
        agenceFilter.setItems(FXCollections.observableArrayList(options));
        if (currentSelection != null && options.contains(currentSelection)) {
            agenceFilter.setValue(currentSelection);
        } else {
            agenceFilter.setValue(ALL_AGENCIES);
        }
    }

    private void applyFiltersAndSort() {
        String query = normalizeText(searchField.getText());
        String selectedAgency = agenceFilter.getValue();
        List<Voiture> filtered = masterItems.stream()
                .filter(item -> matchesSearch(item, query))
                .filter(item -> matchesAgency(item, selectedAgency))
                .collect(Collectors.toList());

        Comparator<Voiture> comparator = buildComparator(sortChoice.getValue());
        if (comparator != null) {
            filtered.sort(comparator);
        }

        currentFiltered = filtered;
        currentPageIndex = 0;
        updateStats();
        updatePaginationControls();
    }

    private boolean matchesSearch(Voiture item, String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        return containsIgnoreCase(item.getMarque(), query)
                || containsIgnoreCase(item.getModele(), query)
                || containsIgnoreCase(item.getMatricule(), query)
                || containsIgnoreCase(item.getAdresseAgence(), query);
    }

    private boolean matchesAgency(Voiture item, String selectedAgency) {
        if (selectedAgency == null || selectedAgency.equals(ALL_AGENCIES)) {
            return true;
        }
        return selectedAgency.equalsIgnoreCase(safeTrim(item.getAdresseAgence()));
    }

    private void updatePaginationControls() {
        updatePageItems();
        int totalPages = getTotalPages();
        if (currentFiltered.isEmpty()) {
            pageInfoLabel.setText("Page 0 / 0");
        } else {
            pageInfoLabel.setText("Page " + (currentPageIndex + 1) + " / " + totalPages);
        }
        prevPageButton.setDisable(currentPageIndex <= 0);
        nextPageButton.setDisable(currentPageIndex >= totalPages - 1);
    }

    private int getTotalPages() {
        if (currentFiltered.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil((double) currentFiltered.size() / pageSize);
    }

    private void updatePageItems() {
        if (currentFiltered.isEmpty()) {
            pageItems.clear();
            return;
        }
        int fromIndex = Math.min(currentPageIndex * pageSize, currentFiltered.size());
        int toIndex = Math.min(fromIndex + pageSize, currentFiltered.size());
        pageItems.setAll(currentFiltered.subList(fromIndex, toIndex));
    }

    private void updateStats() {
        int total = currentFiltered.size();
        long available = currentFiltered.stream()
                .filter(item -> item.getStatut() == Statut.DISPONIBLE)
                .count();
        double avgPrice = currentFiltered.stream()
                .mapToDouble(Voiture::getPrixJour)
                .average()
                .orElse(0);
        String topAgency = currentFiltered.stream()
                .map(Voiture::getAdresseAgence)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.groupingBy(value -> value, Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("-");

        totalCountLabel.setText(String.valueOf(total));
        availableCountLabel.setText(String.valueOf(available));
        avgPriceLabel.setText(String.format(Locale.ROOT, "%.2f", avgPrice));
        topAgenceLabel.setText(topAgency);
    }

    private Comparator<Voiture> buildComparator(String sortValue) {
        if (sortValue == null || SORT_DEFAULT.equals(sortValue)) {
            return Comparator.comparingInt(Voiture::getIdVoiture).reversed();
        }
        switch (sortValue) {
            case "Prix (asc)":
                return Comparator.comparingDouble(Voiture::getPrixJour);
            case "Prix (desc)":
                return Comparator.comparingDouble(Voiture::getPrixJour).reversed();
            case "Annee (desc)":
                return Comparator.comparingInt(Voiture::getAnnee).reversed();
            case "Marque (A-Z)":
                return Comparator.comparing(Voiture::getMarque, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "Date ajout (desc)":
                return Comparator.comparing(Voiture::getDateCreation, Comparator.nullsLast(LocalDateTime::compareTo)).reversed();
            default:
                return null;
        }
    }

    private void exportExcel(File file, List<Voiture> items) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             FileOutputStream outputStream = new FileOutputStream(file)) {
            XSSFSheet sheet = workbook.createSheet("Voitures");
            String[] headers = new String[]{
                    "ID", "Matricule", "Marque", "Modele", "Annee", "Type carburant",
                    "Prix/jour", "Statut", "Agence", "Description", "Image"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            int rowIndex = 1;
            for (Voiture item : items) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(item.getIdVoiture());
                row.createCell(1).setCellValue(safeTrim(item.getMatricule()));
                row.createCell(2).setCellValue(safeTrim(item.getMarque()));
                row.createCell(3).setCellValue(safeTrim(item.getModele()));
                row.createCell(4).setCellValue(item.getAnnee());
                row.createCell(5).setCellValue(item.getTypeCarburant() != null ? item.getTypeCarburant().toString() : "");
                row.createCell(6).setCellValue(item.getPrixJour());
                row.createCell(7).setCellValue(item.getStatut() != null ? item.getStatut().toString() : "");
                row.createCell(8).setCellValue(safeTrim(item.getAdresseAgence()));
                row.createCell(9).setCellValue(safeTrim(item.getDescription()));
                row.createCell(10).setCellValue(safeTrim(item.getImageUrl()));
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(outputStream);
        }
    }

    private void exportPdf(File file, List<Voiture> items) throws IOException {
        PDDocument document = new PDDocument();
        try {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            float margin = 40;
            float y = page.getMediaBox().getHeight() - margin;
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, y);
            contentStream.showText("Liste des voitures");
            contentStream.endText();
            y -= 24;

            contentStream.setFont(PDType1Font.HELVETICA, 9);
            String header = "Matricule | Marque | Modele | Annee | Agence | Prix | Statut";
            y = writePdfLine(contentStream, header, margin, y);
            y -= 4;

            for (Voiture item : items) {
                String line = String.join(" | ",
                        shorten(item.getMatricule(), 12),
                        shorten(item.getMarque(), 12),
                        shorten(item.getModele(), 12),
                        String.valueOf(item.getAnnee()),
                        shorten(item.getAdresseAgence(), 14),
                        String.format(Locale.ROOT, "%.2f", item.getPrixJour()),
                        item.getStatut() != null ? item.getStatut().toString() : ""
                );
                if (y < margin + 20) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    y = page.getMediaBox().getHeight() - margin;
                    contentStream.setFont(PDType1Font.HELVETICA, 9);
                }
                y = writePdfLine(contentStream, line, margin, y);
            }
            contentStream.close();
            document.save(file);
        } finally {
            document.close();
        }
    }

    private float writePdfLine(PDPageContentStream contentStream, String text, float x, float y) throws IOException {
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
        return y - 14;
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

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean containsIgnoreCase(String value, String query) {
        if (value == null || query == null) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).contains(query);
    }

    private static String shorten(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, Math.max(0, max - 3)) + "...";
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
