package org.example.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.control.*;
import javafx.scene.shape.SVGPath;
import javafx.scene.control.Tooltip;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;
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
import org.example.services.BookNowClient;
import org.example.services.TraccarService;
import org.example.services.RoutingService;
import org.example.services.AiRecommendationService;
import org.example.services.WebCarHarvester;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.example.services.RoutingService.RouteInfo;
import org.example.services.TraccarService.TraccarPosition;

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
    private StackPane mainStack;
    @FXML
    private VBox contentRoot;
    @FXML
    private StackPane modalOverlay;
    @FXML
    private VBox modalContent;
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
    private ImageView bgImageView;

    @FXML
    private Label fleetTotalLabel;
    @FXML
    private Label fleetAvailableLabel;
    @FXML
    private Label fleetRentedLabel;

    @FXML
    private TextField aiBudgetField;
    @FXML
    private TextField aiDestinationField;
    @FXML
    private Label aiStatusLabel;

    private final ObservableList<Location> masterItems = FXCollections.observableArrayList();
    private final ObservableList<Location> pageItems = FXCollections.observableArrayList();
    private final ServiceLocation locationService = new ServiceLocation();
    private final BookNowClient bookNowClient = new BookNowClient();
    private final TraccarService traccarService = new TraccarService();
    private final RoutingService routingService = new RoutingService(); // Haversine fallback mode
    private final AiRecommendationService aiRecommendationService = new AiRecommendationService();
    private List<Location> currentFiltered = new ArrayList<>();
    private int currentPageIndex = 0;
    private int pageSize = 6;
    private personne currentUser;
    private Timeline trackingTimeline; // For live GPS updates
    private WebEngine currentEngine;   // Reference to active map engine

    @FXML
    public void initialize() {
        if (!SessionManager.isAuthenticated()) {
            SceneNavigator.switchTo("/org/example/LoginView.fxml", mainStack);
            return;
        }
        if (SessionManager.isAdmin()) {
            SceneNavigator.switchTo("/AdminLocationListView.fxml", mainStack);
            return;
        }
        currentUser = SessionManager.getCurrentUser();
        
        setupHeroBackground();
        
        // Setup ListView
        locationList.setItems(pageItems);
        locationList.setCellFactory(list -> new LocationCell());
        
        setupFilters();
        refreshList();
        refreshFleetStatus();
        
        // Auto-refresh fleet status every 30 seconds
        Timeline fleetRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(30), e -> refreshFleetStatus()));
        fleetRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        fleetRefreshTimeline.play();
        
        // Initial state for modal
        modalOverlay.setVisible(false);
        modalOverlay.setOpacity(0);
    }

    @FXML
    private void showFeatureGuide() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/HelpCenterView.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Guide des FonctionnalitÃ©s");
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.initModality(Modality.APPLICATION_MODAL);
            
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            
            // Allow dragging
            final double[] xOffset = new double[1];
            final double[] yOffset = new double[1];
            root.setOnMousePressed(event -> {
                xOffset[0] = event.getSceneX();
                yOffset[0] = event.getSceneY();
            });
            root.setOnMouseDragged(event -> {
                stage.setX(event.getScreenX() - xOffset[0]);
                stage.setY(event.getScreenY() - yOffset[0]);
            });
            
            stage.show();
        } catch (Exception e) {
            System.err.println("[LocationList] Error showing feature guide: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupHeroBackground() {
        if (bgImageView != null && mainStack != null) {
            bgImageView.fitWidthProperty().bind(mainStack.widthProperty());
            bgImageView.fitHeightProperty().bind(mainStack.heightProperty());
            
            var resourcePath = "/messages/home-hero5.png";
            var url = getClass().getResource(resourcePath);
            if (url != null) {
                Image img = new Image(url.toExternalForm(), true);
                bgImageView.setImage(img);
                System.out.println("[Background] Hero image loaded successfully in LocationListView");
            } else {
                System.err.println("[Background] ERROR: Resource " + resourcePath + " not found!");
            }
        }
    }

    @FXML
    private void handleOpenAdd() {
        showModal("/LocationAddView.fxml");
    }

    @FXML
    private void handleOpenAiForum() {
        showModal("/AiAssistantForumView.fxml");
    }

    // ==================== MODAL SYSTEM ====================

    public void showModal(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent modalNode = loader.load();
            
            // Pass the controller if needed
            Object controller = loader.getController();
            if (controller instanceof LocationAddController) {
                ((LocationAddController) controller).setParentController(this);
            } else if (controller instanceof LocationEditController) {
                ((LocationEditController) controller).setParentController(this);
            } else if (controller instanceof AiAssistantForumController) {
                ((AiAssistantForumController) controller).setParentController(this);
            }

            modalContent.getChildren().setAll(modalNode);
            
            // Apply Blur Effect to background
            BoxBlur blur = new BoxBlur(0, 0, 3);
            contentRoot.setEffect(blur);
            
            modalOverlay.setVisible(true);

            // Animations
            FadeTransition fade = new FadeTransition(Duration.millis(300), modalOverlay);
            fade.setFromValue(0);
            fade.setToValue(1);

            ScaleTransition scale = new ScaleTransition(Duration.millis(300), modalContent);
            scale.setFromX(0.85);
            scale.setFromY(0.85);
            scale.setToX(1.0);
            scale.setToY(1.0);

            ParallelTransition pt = new ParallelTransition(fade, scale);
            
            // Animate Blur
            javafx.animation.Timeline blurTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.millis(300),
                    new javafx.animation.KeyValue(blur.widthProperty(), 15),
                    new javafx.animation.KeyValue(blur.heightProperty(), 15)
                )
            );
            
            pt.play();
            blurTimeline.play();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger le formulaire : " + e.getMessage());
        }
    }

    public void closeModal() {
        FadeTransition fade = new FadeTransition(Duration.millis(250), modalOverlay);
        fade.setFromValue(1);
        fade.setToValue(0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(250), modalContent);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(0.85);
        scale.setToY(0.85);

        // FIX Crash #1: Guard against null/non-BoxBlur effect (e.g. tracking map modal
        // sets effect directly and may have already cleared it)
        javafx.scene.effect.Effect rawEffect = contentRoot.getEffect();
        ParallelTransition pt = new ParallelTransition(fade, scale);

        if (rawEffect instanceof BoxBlur blur) {
            javafx.animation.Timeline blurTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.millis(250),
                    new javafx.animation.KeyValue(blur.widthProperty(), 0),
                    new javafx.animation.KeyValue(blur.heightProperty(), 0)
                )
            );
            blurTimeline.play();
        }

        pt.setOnFinished(e -> {
            modalOverlay.setVisible(false);
            contentRoot.setEffect(null);
            refreshList(); // Auto refresh when modal closes
        });

        pt.play();
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
        if (themeToggleButton == null || mainStack == null) {
            return;
        }
        if (mainStack.getStyleClass().contains("theme-dark")) {
            mainStack.getStyleClass().remove("theme-dark");
            themeToggleButton.setText("Mode sombre");
        } else {
            mainStack.getStyleClass().add("theme-dark");
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
        new Thread(() -> {
            try {
                List<Location> locations = locationService.getAllByPersonneId(currentUser.getId());
                Platform.runLater(() -> {
                    masterItems.setAll(locations);
                    updateVoitureOptions();
                    applyFiltersAndSort();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "User-Location-Refresh-Thread").start();
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
        showModal("/LocationEditView.fxml");
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

    private void handleTrackCar(Location item) {
        if (item == null || item.getVoiture() == null) return;
        showTrackingMap(item);
    }

    private void showTrackingMap(Location item) {
        try {
            // Stop any previous tracking timeline
            if (trackingTimeline != null) {
                trackingTimeline.stop();
                trackingTimeline = null;
            }

            WebView trackWebView = new WebView();
            trackWebView.setPrefHeight(550);
            WebEngine engine = trackWebView.getEngine();

            java.net.URL mapUrl = getClass().getResource("/map.html");
            if (mapUrl == null) {
                showAlert("Erreur", "Fichier map.html introuvable.");
                return;
            }
            engine.load(mapUrl.toExternalForm());
            this.currentEngine = engine;

            // Close button for the tracking modal
            Button closeMapBtn = new Button("✕ Fermer");
            closeMapBtn.getStyleClass().addAll("btn-primary-emerald");
            closeMapBtn.setStyle("-fx-font-size: 13px; -fx-padding: 8 20;");
            closeMapBtn.setOnAction(e -> {
                if (trackingTimeline != null) {
                    trackingTimeline.stop();
                    trackingTimeline = null;
                }
                closeModal();
            });

            // Route info label
            Label routeLabel = new Label();
            routeLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 13px;");

            HBox bottomBar = new HBox(15, routeLabel, new Region(), closeMapBtn);
            bottomBar.setAlignment(Pos.CENTER_RIGHT);
            bottomBar.setStyle("-fx-padding: 10 15;");
            HBox.setHgrow(bottomBar.getChildren().get(1), Priority.ALWAYS);

            VBox mapContainer = new VBox(trackWebView, bottomBar);
            mapContainer.setStyle("-fx-background-color: rgba(1,46,32,0.85); -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: rgba(80,200,120,0.3); -fx-border-width: 1;");
            VBox.setVgrow(trackWebView, Priority.ALWAYS);

            modalContent.getChildren().setAll(mapContainer);
            modalOverlay.setVisible(true);
            modalOverlay.setOpacity(1);

            // Apply blur
            javafx.scene.effect.BoxBlur blur = new javafx.scene.effect.BoxBlur(15, 15, 3);
            contentRoot.setEffect(blur);

            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    // Set up close bridge from JavaScript
                    netscape.javascript.JSObject window = (netscape.javascript.JSObject) engine.executeScript("window");
                    window.setMember("javaConnector", new MapBridge(closeMapBtn));

                    updateCarPositionOnMap(engine, item, routeLabel);
                    startLiveTracking(engine, item, routeLabel);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Bridge object exposed to JavaScript for map callbacks.
     */
    public class MapBridge {
        private final Button closeBtn;
        public MapBridge(Button closeBtn) { this.closeBtn = closeBtn; }
        public void onCloseMap() {
            javafx.application.Platform.runLater(() -> closeBtn.fire());
        }
        public void onMapClick(double lat, double lng) {
            System.out.println("[Map] Clicked at: " + lat + ", " + lng);
        }
    }

    private void updateCarPositionOnMap(WebEngine engine, Location item, Label routeLabel) {
        Voiture v = item.getVoiture();
        double carLat = v.getLatitude();
        double carLon = v.getLongitude();

        // Try to get live position from Traccar
        TraccarPosition livePos = traccarService.getLatestPositionParsed(String.valueOf(item.getIdVoiture()));
        if (livePos != null) {
            carLat = livePos.latitude;
            carLon = livePos.longitude;
            engine.executeScript("showSpeed(" + livePos.speed + ")");
        }

        // Show marker and fly to position
        engine.executeScript("addMarker(" + carLat + "," + carLon + ")");
        engine.executeScript("flyTo(" + carLat + "," + carLon + ", 14)");

        // Compute and show route info (user ← → car)
        // Use the agency position as a reference for the user
        double userLat = 36.8065; // Default: Tunis center
        double userLon = 10.1815;
        RouteInfo info = routingService.getRouteInfo(userLat, userLon, carLat, carLon);
        if (info != null) {
            routeLabel.setText("📍 " + info.distanceText + " · ~" + info.timeText + (info.isEstimate ? " (est.)" : ""));
            engine.executeScript("showRouteInfo('" + info.distanceText + "', '" + info.timeText + "', " + info.isEstimate + ")");

            // Draw route polyline
            List<double[]> polyline = routingService.getRoutePolyline(userLat, userLon, carLat, carLon);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < polyline.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("[").append(polyline.get(i)[0]).append(",").append(polyline.get(i)[1]).append("]");
            }
            sb.append("]");
            engine.executeScript("drawRoute(" + sb.toString() + ")");
        }
    }

    /**
     * Start periodic live GPS tracking updates from Traccar (every 10s).
     */
    private void startLiveTracking(WebEngine engine, Location item, Label routeLabel) {
        trackingTimeline = new Timeline(new KeyFrame(Duration.seconds(10), event -> {
            TraccarPosition pos = traccarService.getLatestPositionParsed(String.valueOf(item.getIdVoiture()));
            if (pos != null) {
                engine.executeScript("updateMarker(" + pos.latitude + "," + pos.longitude + ")");
                engine.executeScript("showSpeed(" + pos.speed + ")");

                // Recalculate route info
                RouteInfo info = routingService.getRouteInfo(36.8065, 10.1815, pos.latitude, pos.longitude);
                if (info != null) {
                    routeLabel.setText("📍 " + info.distanceText + " · ~" + info.timeText + (info.isEstimate ? " (est.)" : ""));
                    engine.executeScript("showRouteInfo('" + info.distanceText + "', '" + info.timeText + "', " + info.isEstimate + ")");
                }
            }
        }));
        trackingTimeline.setCycleCount(Timeline.INDEFINITE);
        trackingTimeline.play();
    }

    @FXML
    private void refreshFleetStatus() {
        new Thread(() -> {
            if (bookNowClient.testConnection()) {
                java.util.Map<String, Object> status = bookNowClient.getFleetStatus();
                if (!status.isEmpty()) {
                    Platform.runLater(() -> {
                        fleetTotalLabel.setText(String.valueOf(Math.round(((Double) status.getOrDefault("total", 0.0)))));
                        fleetAvailableLabel.setText(String.valueOf(Math.round(((Double) status.getOrDefault("available", 0.0)))));
                        fleetRentedLabel.setText(String.valueOf(Math.round(((Double) status.getOrDefault("rented", 0.0)))));
                    });
                }
            } else {
                Platform.runLater(() -> {
                    fleetTotalLabel.setText("N/A");
                    fleetAvailableLabel.setText("N/A");
                    fleetRentedLabel.setText("N/A");
                });
            }
        }, "Fleet-Status-Refresh-Thread").start();
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
            // FIX Crash #5: Use proper middle-dot character (U+00B7), not Â· mojibake
            return safeTrim(voiture.getMarque()) + " " + safeTrim(voiture.getModele()) + " \u00B7 " + safeTrim(voiture.getMatricule());
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
        // Card components - Root is now an HBox for horizontal flow
        private final HBox cardRoot = new HBox(25);

        // Cached placeholder URL (resolved once per cell)
        private static java.net.URL placeholderUrl = null;

        // Left Column: Image
        private final ImageView carImageView = new ImageView();

        // Middle Column: Info (Model, Dates, Duration, Route)
        private final VBox detailsBox = new VBox(5);
        private final Label carModelLabel = new Label();
        private final Label dateRangeLabel = new Label();
        private final Label durationLabel = new Label();
        private final Label routeInfoLabel = new Label(); // Distance + ETA

        // Right Column: Price & Status
        private final VBox rightDetails = new VBox(8);
        private final Label priceLabel = new Label();
        private final Label statusBadge = new Label();
        private final Label refLabel = new Label();

        // Action Column: Icons
        private final HBox actionButtons = new HBox(10);

        // Buttons
        private final Button editButton = new Button();
        private final Button deleteButton = new Button();
        private final Button viewContractButton = new Button();
        private final Button viewQrButton = new Button();
        private final Button saveContractButton = new Button();
        private final Button saveQrButton = new Button();
        private final Button trackButton = new Button();

        /** FIX Crash #2: Load placeholder image safely using resource URL. */
        private void loadPlaceholderImage() {
            if (placeholderUrl == null) {
                placeholderUrl = getClass().getResource("/images/car_placeholder.png");
            }
            if (placeholderUrl != null) {
                try {
                    carImageView.setImage(new Image(placeholderUrl.toExternalForm(), true));
                } catch (Exception e) {
                    carImageView.setImage(null);
                }
            } else {
                carImageView.setImage(null);
            }
        }

        private SVGPath createIcon(String pathData) {
            SVGPath svg = new SVGPath();
            svg.setContent(pathData);
            svg.getStyleClass().add("svg-icon");
            return svg;
        }

        LocationCell() {
            // Setup cardRoot (HBox)
            cardRoot.getStyleClass().add("premium-location-card");
            cardRoot.setAlignment(Pos.CENTER_LEFT);
            cardRoot.setPadding(new Insets(15, 25, 15, 25));
            HBox.setHgrow(detailsBox, Priority.ALWAYS); // Push price/actions to the right
            
            // 1. Image Styling
            carImageView.setFitWidth(180); // More compact for horizontal
            carImageView.setFitHeight(100);
            carImageView.setPreserveRatio(true);
            StackPane imageWrapper = new StackPane(carImageView);
            imageWrapper.getStyleClass().add("location-card-image-clip");
            
            // 2. Details Styling
            detailsBox.setAlignment(Pos.CENTER_LEFT);
            carModelLabel.getStyleClass().add("location-card-subtitle"); // Still Emerald green
            carModelLabel.setStyle("-fx-font-size: 18px;"); // Adjust size for horizontal
            dateRangeLabel.getStyleClass().add("location-card-meta");
            durationLabel.getStyleClass().add("location-card-meta");
            routeInfoLabel.getStyleClass().add("location-card-meta");
            routeInfoLabel.setStyle("-fx-text-fill: #50C878; -fx-font-size: 12px;");
            detailsBox.getChildren().addAll(carModelLabel, dateRangeLabel, durationLabel, routeInfoLabel);
            
            // 3. Right Details & Status
            rightDetails.setAlignment(Pos.CENTER_RIGHT);
            refLabel.getStyleClass().add("location-card-title"); // Dark green
            priceLabel.getStyleClass().add("location-card-price"); // Dark green
            priceLabel.setStyle("-fx-font-size: 20px;"); 
            statusBadge.getStyleClass().add("location-card-status");
            rightDetails.getChildren().addAll(refLabel, statusBadge, priceLabel);
            
            // 4. Action Buttons (SVG Icons)
            actionButtons.setAlignment(Pos.CENTER_RIGHT);
            actionButtons.setSpacing(8);
            
            // SVG Paths and Tooltips
            editButton.setGraphic(createIcon("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"));
            deleteButton.setGraphic(createIcon("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"));
            viewContractButton.setGraphic(createIcon("M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"));
            viewQrButton.setGraphic(createIcon("M3 3h8v8H3V3zm2 2v4h4V5H5zm8-2h8v8h-8V3zm2 2v4h4V5h-4zM3 13h8v8H3v-8zm2 2v4h4v-4H5zm13-2h3v2h-3v-2zm-3 0h2v2h-2v-2zm3 3h3v2h-3v-2zm-3 0h2v2h-2v-2zm3 3h3v2h-3v-2zm-3 0h2v2h-2v-2z"));
            saveContractButton.setGraphic(createIcon("M19 12v7H5v-7H3v7c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2v-7h-2zm-6 .67l2.59-2.58L17 11.5l-5 5-5-5 1.41-1.41L11 12.67V3h2v9.67z"));
            saveQrButton.setGraphic(createIcon("M12 1L8 5h3v9h2V5h3L12 1zm10 14h-4v2h4v7H4v-7h4v-2H4c-1.1 0-2 .9-2 2v9c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2v-9c0-1.1-.9-2-2-2z"));
            trackButton.setGraphic(createIcon("M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"));

            editButton.setTooltip(new Tooltip("Modifier la réservation"));
            deleteButton.setTooltip(new Tooltip("Supprimer cette location"));
            viewContractButton.setTooltip(new Tooltip("Visualiser le contrat (PDF)"));
            viewQrButton.setTooltip(new Tooltip("Visualiser le code QR"));
            saveContractButton.setTooltip(new Tooltip("Télécharger le dossier complet"));
            saveQrButton.setTooltip(new Tooltip("Exporter le justificatif QR"));
            trackButton.setTooltip(new Tooltip("📍 Suivi GPS en temps réel & Trajet"));

            String iconStyle = "btn-icon-premium";
            editButton.getStyleClass().add(iconStyle);
            deleteButton.getStyleClass().add(iconStyle);
            viewContractButton.getStyleClass().add(iconStyle);
            viewQrButton.getStyleClass().add(iconStyle);
            saveContractButton.getStyleClass().add(iconStyle);
            saveQrButton.getStyleClass().add(iconStyle);
            trackButton.getStyleClass().addAll(iconStyle, "btn-track-premium");
            
            actionButtons.getChildren().addAll(editButton, deleteButton, viewContractButton, trackButton, viewQrButton, saveContractButton, saveQrButton);
            
            // Assemble everything in one horizontal row
            cardRoot.getChildren().addAll(imageWrapper, detailsBox, rightDetails, actionButtons);
            setGraphic(null);
            setText(null);
        }

        @Override
        protected void updateItem(Location item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                refLabel.setText("REF: #" + item.getIdLocation()); // Changed to getIdLocation()
                
                String st = item.getStatut() != null ? item.getStatut().toString().toLowerCase() : ""; // Changed to getStatut()
                statusBadge.setText(item.getStatut() != null ? item.getStatut().toString().toUpperCase() : ""); // Changed to getStatut()

                trackButton.setOnAction(e -> handleTrackCar(item));
                statusBadge.getStyleClass().removeAll("status-en-attente", "status-confirmee", "status-annulee");
                if (st.contains("attente")) statusBadge.getStyleClass().add("status-en-attente");
                else if (st.contains("confirm")) statusBadge.getStyleClass().add("status-confirmee");
                else if (st.contains("annul")) statusBadge.getStyleClass().add("status-annulee");

                // Get car info
                Voiture v = item.getVoiture();
                if (v != null) {
                    carModelLabel.setText(v.getMarque() + " " + v.getModele());
                    // FIX Crash #2: Use getClass().getResource() so JavaFX gets a valid URL,
                    // not a bare classpath path that throws IllegalArgumentException.
                    if (v.getImageUrl() != null && !v.getImageUrl().isEmpty()) {
                        try {
                            carImageView.setImage(new Image(v.getImageUrl(), true));
                        } catch (Exception imgEx) {
                            loadPlaceholderImage();
                        }
                    } else {
                        loadPlaceholderImage();
                    }

                    // Compute route info (distance + ETA) from Tunis center to car
                    double userLat = 36.8065, userLon = 10.1815;
                    RouteInfo routeInfo = routingService.getRouteInfo(userLat, userLon, v.getLatitude(), v.getLongitude());
                    if (routeInfo != null) {
                        routeInfoLabel.setText("\uD83D\uDCCD " + routeInfo.distanceText + " \u00B7 ~" + routeInfo.timeText
                                + (routeInfo.isEstimate ? " (est.)" : ""));
                    } else {
                        routeInfoLabel.setText("");
                    }
                } else {
                    carModelLabel.setText("Voiture non sp\u00E9cifi\u00E9e");
                    loadPlaceholderImage();
                    routeInfoLabel.setText("");
                }

                dateRangeLabel.setText("📅 " + (item.getDateDebut() != null ? item.getDateDebut().toString() : "") + " au " + (item.getDateFin() != null ? item.getDateFin().toString() : ""));
                long days = 0;
                if (item.getDateDebut() != null && item.getDateFin() != null) {
                    days = java.time.temporal.ChronoUnit.DAYS.between(
                            item.getDateDebut(), 
                            item.getDateFin());
                }
                durationLabel.setText("⏱ Durée: " + days + " jours");
                
                priceLabel.setText(String.format("%.2f DT", item.getMontantTotal()));

                editButton.setOnAction(e -> openEdit(item, editButton));
                deleteButton.setOnAction(e -> deleteFromRow(item));
                viewContractButton.setOnAction(e -> openFile(item.getContratPdf()));
                viewQrButton.setOnAction(e -> openFile(item.getQrCode()));
                saveContractButton.setOnAction(e -> saveCopy(item.getContratPdf(), "Enregistrer le contrat", "PDF", "*.pdf"));
                saveQrButton.setOnAction(e -> saveCopy(item.getQrCode(), "Enregistrer le QR", "Image", "*.png", "*.jpg", "*.jpeg"));

                setGraphic(cardRoot);
            }
        }
    }

    private static boolean isHttpUrl(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    // ==================== NAVIGATION METHODS ====================

    @FXML
    private void handleGoHome() {
        org.example.mains.MainApp.switchScene("/org/example/UserHomeView.fxml", "Accueil");
    }

    @FXML
    private void handleGoActivities() {
        org.example.mains.MainApp.switchScene("/UserHome.fxml", "Activités");
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
        org.example.mains.MainApp.switchScene("/poste-forumviews/ListForum.fxml", "Forums");
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

    @FXML
    private void handleAiRecommend() {
        String budgetText = aiBudgetField.getText().trim();
        String destination = aiDestinationField.getText().trim();

        if (budgetText.isEmpty()) {
            showAlert("Champ requis", "Veuillez entrer un budget maximum.");
            return;
        }

        double budget;
        try {
            budget = Double.parseDouble(budgetText);
        } catch (NumberFormatException e) {
            showAlert("Erreur de format", "Le budget doit \u00EAtre un nombre valide.");
            if (aiStatusLabel != null) aiStatusLabel.setText("Erreur");
            return;
        }

        // FIX Crash #3: Offload ALL network/AI work to a background thread.
        // Running harvestFromWeb() + recommendCars() on the FX thread would freeze the UI.
        if (aiBudgetField != null) aiBudgetField.setDisable(true);
        if (aiDestinationField != null) aiDestinationField.setDisable(true);
        if (aiStatusLabel != null) {
            aiStatusLabel.setText("Recherche sur le web...");
            aiStatusLabel.setStyle("-fx-text-fill: #FFD700;");
        }

        final double finalBudget = budget;
        final String finalDest   = destination;

        new Thread(() -> {
            try {
                // 1. Web harvesting (network IO — must NOT run on FX thread)
                WebCarHarvester harvester = new WebCarHarvester();
                harvester.harvestFromWeb();

                Platform.runLater(() -> {
                    if (aiStatusLabel != null) {
                        aiStatusLabel.setText("DeepSeek analyse...");
                        aiStatusLabel.setStyle("-fx-text-fill: #50C878;");
                    }
                });

                // 2. AI ranking (may call network — must NOT run on FX thread)
                List<Voiture> recommendations = aiRecommendationService.recommendCars(finalBudget, finalDest);

                Platform.runLater(() -> {
                    try {
                        if (recommendations.isEmpty()) {
                            if (aiStatusLabel != null) aiStatusLabel.setText("Aucun r\u00E9sultat");
                            showAlert("AI Assistant", "D\u00E9sol\u00E9, aucune voiture ne correspond \u00E0 votre budget, m\u00EAme sur le web.");
                            return;
                        }

                        if (aiStatusLabel != null)
                            aiStatusLabel.setText("Optimis\u00E9 ! (" + recommendations.size() + " voitures)");

                        // Format to JSON for JavaScript
                        StringBuilder jsonBuilder = new StringBuilder("[");
                        for (int i = 0; i < recommendations.size(); i++) {
                            Voiture v = recommendations.get(i);
                            jsonBuilder.append(String.format(
                                "{\"id\":%d, \"marque\":\"%s\", \"modele\":\"%s\", \"prix\":%.2f, \"lat\":%.6f, \"lng\":%.6f}",
                                v.getIdVoiture(), v.getMarque(), v.getModele(), v.getPrixJour(), v.getLatitude(), v.getLongitude()
                            ));
                            if (i < recommendations.size() - 1) jsonBuilder.append(",");
                        }
                        jsonBuilder.append("]");

                        runInWebView("showRecommendations(" + jsonBuilder.toString() + ")");
                        showAlert("Succ\u00E8s", "L'Assistant Pro a trouv\u00E9 " + recommendations.size() +
                                 " options. Les voitures recommand\u00E9es (locales et web) sont \u00E9toil\u00E9es sur la carte.");
                    } finally {
                        if (aiBudgetField != null) aiBudgetField.setDisable(false);
                        if (aiDestinationField != null) aiDestinationField.setDisable(false);
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    if (aiStatusLabel != null) aiStatusLabel.setText("Erreur");
                    if (aiBudgetField != null) aiBudgetField.setDisable(false);
                    if (aiDestinationField != null) aiDestinationField.setDisable(false);
                    showAlert("Erreur", "Erreur lors de la recommandation : " + ex.getMessage());
                });
            }
        }, "AI-Recommend-Thread").start();
    }

    public void highlightCarOnMap(Voiture v) {
        // Zoom to car location and show details on map
        String description = v.getDescription() != null ? v.getDescription().replace("'", "\\'") : "";
        String imageUrl = v.getImageUrl() != null ? v.getImageUrl() : "";
        
        String script = String.format("window.highlightCar(%d, '%.6f', '%.6f', '%s %s', '%.2f', '%s', '%s')", 
            v.getIdVoiture(), v.getLatitude(), v.getLongitude(), 
            v.getMarque(), v.getModele(), v.getPrixJour(), description, imageUrl);
        
        runInWebView(script);
        System.out.println("[AI Forum] Highlighting " + v.getMarque() + " with detailed info.");
    }

    private void runInWebView(String script) {
        if (currentEngine != null) {
            try {
                // Ensure UI thread
                javafx.application.Platform.runLater(() -> {
                    try {
                        currentEngine.executeScript(script);
                    } catch (Exception e) {
                        System.err.println("[WebView] Error executing script: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Optionnel: Ouvrir la carte si elle n'est pas ouverte
            System.out.println("[AI] Map is not open, recommendations displayed as list only.");
        }
    }
}
