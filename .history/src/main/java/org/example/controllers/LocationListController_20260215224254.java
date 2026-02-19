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
import org.example.entities.Location;
import org.example.entities.StatutLocation;
import org.example.entities.Voiture;
import org.example.services.ServiceLocation;
import org.example.utils.LocationSelection;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;
import org.example.entities.personne;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
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

public class LocationListController {

    private static final String ALL_VOITURES = "Toutes";
    private static final String SORT_DEFAULT = "Par defaut";

    @FXML
    private BorderPane root;
    @FXML
    private ListView<Location> locationList;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> voitureFilter;
    @FXML
    private ComboBox<String> sortChoice;
    @FXML
    private ComboBox<Integer> pageSizeBox;
    @FXML
    private Button addLocationButton;
    @FXML
    private Button backToCarsButton;
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
    private Label confirmedCountLabel;
    @FXML
    private Label totalRevenueLabel;
    @FXML
    private Label topVoitureLabel;

    private final ObservableList<Location> masterItems = FXCollections.observableArrayList();
    private final ObservableList<Location> pageItems = FXCollections.observableArrayList();
    private final ServiceLocation locationService = new ServiceLocation();
    private List<Location> currentFiltered = new ArrayList<>();
    private int currentPageIndex = 0;
    private int pageSize = 6;
    private personne currentUser;

    @FXML
    public void initialize() {
        if (!SessionManager.isAuthenticated()) {
            SceneNavigator.switchTo("/org/example/LoginView.fxml", root);
            return;
        }
        if (SessionManager.isAdmin()) {
            SceneNavigator.switchTo("/AdminLocationListView.fxml", root);
            return;
        }
        currentUser = SessionManager.getCurrentUser();
        locationList.setItems(pageItems);
        locationList.setCellFactory(list -> new LocationCell());
        setupFilters();
        refreshList();
    }

    @FXML
    private void handleOpenAdd() {
        SceneNavigator.switchTo("/LocationAddView.fxml", addLocationButton);
    }

    @FXML
    private void handleBackToCars() {
        SceneNavigator.switchTo("/VoitureListView.fxml", backToCarsButton);
    }

    @FXML
    private void handleExportPdf() {
        if (currentFiltered.isEmpty()) {
            showAlert("Export", "Aucune location a exporter.");
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
            showAlert("Export", "Aucune location a exporter.");
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
        if (themeToggleButton == null || root == null) {
            return;
        }
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
        if (currentUser == null) {
            masterItems.clear();
            applyFiltersAndSort();
            return;
        }
        List<Location> locations = locationService.getAllByPersonneId(currentUser.getId());
        masterItems.setAll(locations);
        updateVoitureOptions();
        applyFiltersAndSort();
    }

    private void setupFilters() {
        sortChoice.setItems(FXCollections.observableArrayList(
                SORT_DEFAULT,
                "Montant (desc)",
                "Montant (asc)",
                "Date debut (desc)",
                "Date fin (desc)",
                "Reference (A-Z)"
        ));
        sortChoice.setValue(SORT_DEFAULT);
        if (pageSizeBox != null) {
            pageSizeBox.setItems(FXCollections.observableArrayList(6, 10, 15, 20));
            pageSizeBox.setValue(pageSize);
        }

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFiltersAndSort());
        voitureFilter.setOnAction(event -> applyFiltersAndSort());
        sortChoice.setOnAction(event -> applyFiltersAndSort());
        if (pageSizeBox != null) {
            pageSizeBox.setOnAction(event -> {
                Integer value = pageSizeBox.getValue();
                if (value != null && value > 0) {
                    pageSize = value;
                    currentPageIndex = 0;
                    updatePaginationControls();
                }
            });
        }
    }

    private void updateVoitureOptions() {
        String currentSelection = voitureFilter.getValue();
        Set<String> voitures = masterItems.stream()
                .map(this::getVoitureLabel)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));
        List<String> options = new ArrayList<>();
        options.add(ALL_VOITURES);
        options.addAll(voitures);
        voitureFilter.setItems(FXCollections.observableArrayList(options));
        if (currentSelection != null && options.contains(currentSelection)) {
            voitureFilter.setValue(currentSelection);
        } else {
            voitureFilter.setValue(ALL_VOITURES);
        }
    }

    private void applyFiltersAndSort() {
        String query = normalizeText(searchField.getText());
        String selectedVoiture = voitureFilter.getValue();
        List<Location> filtered = masterItems.stream()
                .filter(item -> matchesSearch(item, query))
                .filter(item -> matchesVoiture(item, selectedVoiture))
                .collect(Collectors.toList());

        Comparator<Location> comparator = buildComparator(sortChoice.getValue());
        if (comparator != null) {
            filtered.sort(comparator);
        }

        currentFiltered = filtered;
        currentPageIndex = 0;
        updateStats();
        updatePaginationControls();
    }

    private boolean matchesSearch(Location item, String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        return containsIgnoreCase(item.getReference(), query)
                || containsIgnoreCase(getVoitureLabel(item), query)
                || containsIgnoreCase(item.getStatut() != null ? item.getStatut().toString() : null, query);
    }

    private boolean matchesVoiture(Location item, String selectedVoiture) {
        if (selectedVoiture == null || selectedVoiture.equals(ALL_VOITURES)) {
            return true;
        }
        return selectedVoiture.equalsIgnoreCase(safeTrim(getVoitureLabel(item)));
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
        long confirmed = currentFiltered.stream()
                .filter(item -> item.getStatut() == StatutLocation.CONFIRMEE)
                .count();
        double totalRevenue = currentFiltered.stream()
                .mapToDouble(Location::getMontantTotal)
                .sum();
        String topVoiture = currentFiltered.stream()
                .collect(Collectors.groupingBy(this::getVoitureLabel, Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("-");

        totalCountLabel.setText(String.valueOf(total));
        if (confirmedCountLabel != null) {
            confirmedCountLabel.setText(String.valueOf(confirmed));
        }
        if (totalRevenueLabel != null) {
            totalRevenueLabel.setText(String.format(Locale.ROOT, "%.2f", totalRevenue));
        }
        if (topVoitureLabel != null) {
            topVoitureLabel.setText(topVoiture);
        }
    }

    private Comparator<Location> buildComparator(String sortValue) {
        if (sortValue == null || SORT_DEFAULT.equals(sortValue)) {
            return Comparator.comparingInt(Location::getIdLocation).reversed();
        }
        switch (sortValue) {
            case "Montant (desc)":
                return Comparator.comparingDouble(Location::getMontantTotal).reversed();
            case "Montant (asc)":
                return Comparator.comparingDouble(Location::getMontantTotal);
            case "Date debut (desc)":
                return Comparator.comparing(Location::getDateDebut, Comparator.nullsLast(LocalDate::compareTo)).reversed();
            case "Date fin (desc)":
                return Comparator.comparing(Location::getDateFin, Comparator.nullsLast(LocalDate::compareTo)).reversed();
            case "Reference (A-Z)":
                return Comparator.comparing(Location::getReference, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            default:
                return null;
        }
    }

    private void exportExcel(File file, List<Location> items) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             FileOutputStream outputStream = new FileOutputStream(file)) {
            XSSFSheet sheet = workbook.createSheet("Locations");
            String[] headers = new String[]{
                    "ID", "Reference", "Voiture", "Date debut", "Date fin", "Nb jours", "Montant", "Statut"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            int rowIndex = 1;
            for (Location item : items) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(item.getIdLocation());
                row.createCell(1).setCellValue(safeTrim(item.getReference()));
                row.createCell(2).setCellValue(safeTrim(getVoitureLabel(item)));
                row.createCell(3).setCellValue(item.getDateDebut() != null ? item.getDateDebut().toString() : "");
                row.createCell(4).setCellValue(item.getDateFin() != null ? item.getDateFin().toString() : "");
                row.createCell(5).setCellValue(item.getNbJours());
                row.createCell(6).setCellValue(item.getMontantTotal());
                row.createCell(7).setCellValue(item.getStatut() != null ? item.getStatut().toString() : "");
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(outputStream);
        }
    }

    private void exportPdf(File file, List<Location> items) throws IOException {
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
            contentStream.showText("Liste des locations");
            contentStream.endText();
            y -= 24;

            contentStream.setFont(PDType1Font.HELVETICA, 9);
            String header = "Reference | Voiture | Date debut | Date fin | Montant | Statut";
            y = writePdfLine(contentStream, header, margin, y);
            y -= 4;

            for (Location item : items) {
                String line = String.join(" | ",
                        shorten(item.getReference(), 14),
                        shorten(getVoitureLabel(item), 16),
                        item.getDateDebut() != null ? item.getDateDebut().toString() : "",
                        item.getDateFin() != null ? item.getDateFin().toString() : "",
                        String.format(Locale.ROOT, "%.2f", item.getMontantTotal()),
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

    private void openEdit(Location location, Button source) {
        LocationSelection.set(location);
        SceneNavigator.switchTo("/LocationEditView.fxml", source);
    }

    private void deleteFromRow(Location item) {
        try {
            locationService.delete(item.getIdLocation());
            refreshList();
        } catch (RuntimeException ex) {
            showAlert("Erreur", ex.getMessage());
        }
    }

    private void openFile(String path) {
        if (path == null || path.trim().isEmpty()) {
            showAlert("Info", "Aucun fichier disponible.");
            return;
        }
        File file = new File(path);
        if (!file.exists()) {
            showAlert("Info", "Fichier introuvable.");
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            } else {
                showAlert("Info", "Ouverture de fichier non supportee.");
            }
        } catch (IOException ex) {
            showAlert("Erreur", "Erreur lors de l'ouverture du fichier.");
        }
    }

    private void saveCopy(String path, String title, String description, String... extensions) {
        if (path == null || path.trim().isEmpty()) {
            showAlert("Info", "Aucun fichier disponible.");
            return;
        }
        File source = new File(path);
        if (!source.exists()) {
            showAlert("Info", "Fichier introuvable.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, extensions));
        File destination = chooser.showSaveDialog(locationList.getScene().getWindow());
        if (destination == null) {
            return;
        }
        try {
            Files.copy(source.toPath(), destination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            showAlert("Info", "Fichier enregistre.");
        } catch (IOException ex) {
            showAlert("Erreur", "Erreur lors de l'enregistrement.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String getVoitureLabel(Location item) {
        if (item == null) {
            return "";
        }
        Voiture voiture = item.getVoiture();
        if (voiture != null) {
            return safeTrim(voiture.getMarque()) + " " + safeTrim(voiture.getModele()) + " Â· " + safeTrim(voiture.getMatricule());
        }
        return String.valueOf(item.getIdVoiture());
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

    private class LocationCell extends ListCell<Location> {
        private final HBox root = new HBox();
        private final ImageView thumbnail = new ImageView();
        private final VBox left = new VBox();
        private final VBox right = new VBox();
        private final HBox center = new HBox();
        private final Label title = new Label();
        private final Label subtitle = new Label();
        private final Label meta = new Label();
        private final Label amount = new Label();
        private final Label status = new Label();
        private final Button editButton = new Button("Modifier");
        private final Button deleteButton = new Button("Supprimer");
        private final Button viewContractButton = new Button("Contrat");
        private final Button viewQrButton = new Button("QR");
        private final Button saveContractButton = new Button("Sauver PDF");
        private final Button saveQrButton = new Button("Sauver QR");
        private final Region leftSpacer = new Region();
        private final Region rightSpacer = new Region();

        LocationCell() {
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
            amount.getStyleClass().add("voiture-price");
            status.getStyleClass().add("voiture-status");
            editButton.getStyleClass().addAll("row-btn", "row-btn-edit");
            deleteButton.getStyleClass().addAll("row-btn", "row-btn-delete");
            viewContractButton.getStyleClass().addAll("row-btn", "row-btn-edit");
            viewQrButton.getStyleClass().addAll("row-btn", "row-btn-edit");
            saveContractButton.getStyleClass().addAll("row-btn", "row-btn-edit");
            saveQrButton.getStyleClass().addAll("row-btn", "row-btn-edit");

            left.getChildren().addAll(title, subtitle, meta);
                center.getChildren().addAll(editButton, deleteButton, viewContractButton, viewQrButton, saveContractButton, saveQrButton);
            right.getChildren().addAll(amount, status);

            HBox.setHgrow(leftSpacer, Priority.ALWAYS);
            HBox.setHgrow(rightSpacer, Priority.ALWAYS);
            root.getChildren().addAll(thumbnail, left, leftSpacer, center, rightSpacer, right);
            setText(null);
        }

        @Override
        protected void updateItem(Location item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            title.setText(safeTrim(item.getReference()));
            subtitle.setText(getVoitureLabel(item));
            meta.setText(item.getDateDebut() + " -> " + item.getDateFin() + " Â· " + item.getNbJours() + " jours");
            amount.setText(String.format(Locale.ROOT, "%.2f TND", item.getMontantTotal()));
            status.setText(item.getStatut() != null ? item.getStatut().toString() : "");

            Voiture voiture = item.getVoiture();
            String imageUrl = voiture != null ? voiture.getImageUrl() : null;
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
            viewContractButton.setOnAction(event -> openFile(item.getContratPdf()));
            viewQrButton.setOnAction(event -> openFile(item.getQrCode()));
            saveContractButton.setOnAction(event -> saveCopy(item.getContratPdf(), "Enregistrer le contrat", "PDF", "*.pdf"));
            saveQrButton.setOnAction(event -> saveCopy(item.getQrCode(), "Enregistrer le QR", "Image", "*.png", "*.jpg", "*.jpeg"));

            setGraphic(root);
        }
    }

    private static boolean isHttpUrl(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    // ==================== NAVIGATION METHODS ====================

    @FXML
    private void handleGoHome() {
        SceneNavigator.switchTo("/UserHome.fxml", locationList);
    }

    @FXML
    private void handleGoActivities() {
        SceneNavigator.switchTo("/UserHome.fxml", locationList);
    }

    @FXML
    private void handleGoLocations() {
        refreshList();
    }

    @FXML
    private void handleGoFlights() {
        org.example.mains.MainApp.switchScene("/views/user-dashboard.fxml", "Vols");
    }

    @FXML
    private void handleGoHotels() {
        org.example.mains.MainApp.switchScene("/views/room-booking.fxml", "Hôtels");
    }

    @FXML
    private void handleGoForums() {
        SceneNavigator.switchTo("/poste-forumviews/ListForum.fxml", locationList);
    }

    @FXML
    private void handleHome() {
        org.example.mains.MainApp.switchScene("/org/example/UserHomeView.fxml", "Accueil");
    }

    @FXML
    private void handleLogout() {
        SessionManager.clear();
        SceneNavigator.switchTo("/Login.fxml", locationList);
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        voitureFilter.setValue(ALL_VOITURES);
        sortChoice.setValue(SORT_DEFAULT);
        applyFiltersAndSort();
    }

    @FXML
    private void handleSearch() {
        applyFiltersAndSort();
    }

    @FXML
    private void handleProfile() {
        showAlert("Profil", "Fonctionnalité à venir : Gestion du profil utilisateur");
    }

    @FXML
    private void handleMyReservations() {
        org.example.mains.MainApp.switchScene("/views/room-booking.fxml", "Mes Réservations");
    }

    @FXML
    private void handleMyLocations() {
        refreshList();
    }

    @FXML
    private void handleMyFlights() {
        org.example.mains.MainApp.switchScene("/views/user-dashboard.fxml", "Mes Vols");
    }

    @FXML
    private void handleSettings() {
        showAlert("Paramètres", "Fonctionnalité à venir : Paramètres utilisateur");
    }

    @FXML
    private void handleHelp() {
        showAlert("Aide", "Besoin d'aide ? Contactez-nous à support@govibe.tn");
    }
}
