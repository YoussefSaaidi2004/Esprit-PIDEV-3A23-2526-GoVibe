package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.scene.control.Button;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import org.example.entities.Forum;
import org.example.entities.Poste;
import org.example.services.ServiceForum;
import org.example.services.ServicePoste;
import org.example.utils.ForumPdfExporter;
import org.example.utils.SessionManager;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ListForumController {

    @FXML private Label roleLabel;
    @FXML private VBox forumsContainer;
    @FXML private StackPane rootStackPane;
    @FXML private ImageView bgImageView;
    @FXML private StackPane formOverlay;
    @FXML private VBox formContainer;
    @FXML private VBox emptyState;
    @FXML private TextField searchField;

    // Stats modal fields
    @FXML private StackPane statsOverlay;
    @FXML private Label statsForumTitleLabel;
    @FXML private Label statsCountriesCountLabel;
    @FXML private FlowPane countryCardsContainer;

    // Dynamic stat tiles
    @FXML private Label statForumsLabel;
    @FXML private Label statMembersLabel;
    @FXML private Label statPostsLabel;

    // Filter tab state
    @FXML private ToggleButton tabPopular;
    @FXML private ToggleButton tabRecent;
    @FXML private ToggleButton tabMine;

    private final ServiceForum serviceForum = new ServiceForum();
    private final ServicePoste servicePoste = new ServicePoste();
    private List<Forum> allForums;
    private String activeTab = "popular";

    @FXML
    public void initialize() {
        loadForums();
        setupHeroBackground();
    }

    // ─── Forum Loading ──────────────────────────────────────────────────────

    private void loadForums() {
        try {
            allForums = serviceForum.afficher();
            updateStats(allForums);
            renderForums(allForums);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void renderForums(List<Forum> list) {
        forumsContainer.getChildren().clear();

        boolean isEmpty = list == null || list.isEmpty();
        if (emptyState != null) {
            emptyState.setVisible(isEmpty);
            emptyState.setManaged(isEmpty);
        }

        if (!isEmpty) {
            // Admin view: render glass cards with Stats button
            boolean isAdmin = SessionManager.getCurrentUser() != null
                    && "admin".equalsIgnoreCase(SessionManager.getCurrentUser().getRole());

            if (isAdmin && statsOverlay != null) {
                // Render as admin glass cards
                FlowPane flow = new FlowPane();
                flow.setHgap(18);
                flow.setVgap(18);
                flow.setStyle("-fx-background-color: transparent;");
                for (Forum f : list) {
                    flow.getChildren().add(buildAdminForumCard(f));
                }
                forumsContainer.getChildren().add(flow);
            } else {
                // User view: load ForumItem.fxml per forum
                for (Forum f : list) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/poste-forumviews/ForumItem.fxml"));
                        Node node = loader.load();
                        ForumItemController controller = loader.getController();
                        controller.setParentListController(this);
                        controller.setData(f);
                        forumsContainer.getChildren().add(node);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private VBox buildAdminForumCard(Forum f) {
        // Title row
        Label nameLabel = new Label(f.getName() != null ? f.getName() : "Forum sans nom");
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: white; -fx-wrap-text: true;");
        nameLabel.setMaxWidth(280);

        // Description
        Label descLabel = new Label(f.getDescription() != null && !f.getDescription().isBlank()
                ? f.getDescription() : "Pas de description");
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.6); -fx-wrap-text: true;");
        descLabel.setMaxWidth(280);
        descLabel.setMinHeight(40);

        // Stats row
        HBox statsRow = new HBox(12);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        statsRow.getChildren().addAll(
            makeStatChip("👥", String.valueOf(f.getNbr_members())),
            makeStatChip("💬", String.valueOf(f.getPost_count()))
        );

        // Date
        String dateStr = f.getDate_creation() != null
                ? f.getDate_creation().toString().substring(0, 10) : "N/A";
        Label dateLabel = new Label("📅 Créé le " + dateStr);
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.4); -fx-font-weight: 600;");

        // Separator
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: rgba(80,200,120,0.15);");

        // Action buttons
        Button btnView = new Button("Voir");
        btnView.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: white;"
                + "-fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 10;"
                + "-fx-border-color: rgba(255,255,255,0.15); -fx-border-width: 1; -fx-border-radius: 10;"
                + "-fx-padding: 8 16; -fx-cursor: hand;");
        btnView.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/poste-forumviews/DetailsForum.fxml"));
                Parent root = loader.load();
                DetailsForumController controller = loader.getController();
                controller.initData(f);
                Scene scene = btnView.getScene();
                if (scene != null) scene.setRoot(root);
            } catch (IOException ex) { ex.printStackTrace(); }
        });

        Button btnEdit = new Button("Modifier");
        btnEdit.setStyle("-fx-background-color: rgba(80,200,120,0.12); -fx-text-fill: #50C878;"
                + "-fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 10;"
                + "-fx-border-color: rgba(80,200,120,0.3); -fx-border-width: 1; -fx-border-radius: 10;"
                + "-fx-padding: 8 16; -fx-cursor: hand;");
        btnEdit.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/poste-forumviews/ModifierForum.fxml"));
                Parent root = loader.load();
                ModifierForumController controller = loader.getController();
                controller.setOverlayController(this);
                controller.initData(f);
                showFormOverlay(root);
            } catch (IOException ex) { ex.printStackTrace(); }
        });

        Button btnStats = new Button("Stats");
        btnStats.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: rgba(255,255,255,0.8);"
                + "-fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 10;"
                + "-fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 1; -fx-border-radius: 10;"
                + "-fx-padding: 8 16; -fx-cursor: hand;");
        btnStats.setOnAction(e -> showForumStats(f));

        Button btnDelete = new Button("Supprimer");
        btnDelete.setStyle("-fx-background-color: rgba(231,76,60,0.12); -fx-text-fill: #ff7675;"
                + "-fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 10;"
                + "-fx-border-color: rgba(231,76,60,0.3); -fx-border-width: 1; -fx-border-radius: 10;"
                + "-fx-padding: 8 16; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation");
            alert.setHeaderText("Supprimer le forum ?");
            alert.setContentText("Voulez-vous vraiment supprimer " + f.getName() + " ?");
            if (alert.showAndWait().get() == javafx.scene.control.ButtonType.OK) {
                try {
                    serviceForum.supprimer(f.getForum_id());
                    loadForums();
                } catch (SQLException ex) { ex.printStackTrace(); }
            }
        });

        HBox actionsRow = new HBox(10, btnView, btnEdit, btnStats, btnDelete);
        actionsRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(15, nameLabel, descLabel, statsRow, dateLabel, sep, actionsRow);
        card.setPrefWidth(330);
        card.setMaxWidth(330);
        card.setPadding(new Insets(24));
        card.setAlignment(Pos.TOP_LEFT);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.06);"
                + "-fx-border-color: rgba(80,200,120,0.22); -fx-border-width: 1.5;"
                + "-fx-border-radius: 20; -fx-background-radius: 20;"
                + "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.4),20,0,0,8);");
        
        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-background-color: rgba(255,255,255,0.1); -fx-border-color: rgba(80,200,120,0.4);"));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("-fx-background-color: rgba(255,255,255,0.1); -fx-border-color: rgba(80,200,120,0.4);", "")));
        
        return card;
    }

    private HBox makeStatChip(String label, String value) {
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #50C878;");
        Label lbl = new Label(" " + label);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(200,230,215,0.60);");
        HBox chip = new HBox(2, val, lbl);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setStyle("-fx-background-color: rgba(80,200,120,0.10); -fx-background-radius: 8; -fx-padding: 5 10;");
        return chip;
    }

    // ─── Country Stats ───────────────────────────────────────────────────────────────

    private static final List<String> WORLD_COUNTRIES = Arrays.asList(
        "Afghanistan","Albania","Algeria","Andorra","Angola","Argentina","Armenia","Australia",
        "Austria","Azerbaijan","Bahamas","Bahrain","Bangladesh","Belarus","Belgium","Belize",
        "Benin","Bhutan","Bolivia","Bosnia","Botswana","Brazil","Brunei","Bulgaria",
        "Burkina Faso","Burundi","Cambodia","Cameroon","Canada","Chad","Chile","China",
        "Colombia","Congo","Costa Rica","Croatia","Cuba","Cyprus","Czech","Denmark",
        "Djibouti","Dominican","Ecuador","Egypt","El Salvador","Estonia","Ethiopia","Finland",
        "France","Gabon","Gambia","Georgia","Germany","Ghana","Greece","Guatemala","Guinea",
        "Haiti","Honduras","Hungary","Iceland","India","Indonesia","Iran","Iraq","Ireland",
        "Israel","Italy","Jamaica","Japan","Jordan","Kazakhstan","Kenya","Kosovo","Kuwait",
        "Kyrgyzstan","Laos","Latvia","Lebanon","Lesotho","Libya","Liechtenstein","Lithuania",
        "Luxembourg","Madagascar","Malawi","Malaysia","Maldives","Mali","Malta","Mauritania",
        "Mauritius","Mexico","Moldova","Monaco","Mongolia","Montenegro","Morocco","Mozambique",
        "Myanmar","Namibia","Nepal","Netherlands","New Zealand","Nicaragua","Niger","Nigeria",
        "Norway","Oman","Pakistan","Palestine","Panama","Paraguay","Peru","Philippines",
        "Poland","Portugal","Qatar","Romania","Russia","Rwanda","Saudi Arabia","Senegal",
        "Serbia","Sierra Leone","Singapore","Slovakia","Slovenia","Somalia","South Africa",
        "South Korea","Spain","Sri Lanka","Sudan","Sweden","Switzerland","Syria","Taiwan",
        "Tajikistan","Tanzania","Thailand","Togo","Tunisia","Turkey","Turkmenistan",
        "Uganda","Ukraine","United Arab Emirates","United Kingdom","United States","USA","UK",
        "Uruguay","Uzbekistan","Venezuela","Vietnam","Yemen","Zambia","Zimbabwe",
        // French names for common countries
        "Maroc","Algerie","Tunisie","Egypte","France","Espagne","Italie","Allemagne",
        "Russie","Chine","Japon","Inde","Bresil","Mexique","Argentine","Turquie",
        "Emirats","Senegal","Cameroun","Mali","Niger","Mauritanie","Soudan","Libye",
        "Jordanie","Liban","Syrie","Irak","Iran","Arabie","Koweît","Bahrain",
        "Portugal","Grece","Pologne","Roumanie","Ukraine","Belgique","Hollande","Pays-Bas"
    );

    public void showForumStats(Forum forum) {
        if (statsOverlay == null) return;
        if (statsForumTitleLabel != null)
            statsForumTitleLabel.setText("\u2013 " + (forum.getName() != null ? forum.getName() : ""));

        // Collect text from forum name + description
        StringBuilder allText = new StringBuilder();
        if (forum.getName() != null) allText.append(forum.getName()).append(" ");
        if (forum.getDescription() != null) allText.append(forum.getDescription()).append(" ");

        // Collect text from posts
        try {
            List<Poste> posts = servicePoste.afficherParForum(forum.getForum_id());
            for (Poste p : posts) {
                if (p.getContenu() != null) allText.append(p.getContenu()).append(" ");
            }
        } catch (SQLException e) { e.printStackTrace(); }

        // Count country mentions
        String textLower = allText.toString().toLowerCase();
        Map<String, Integer> countryCounts = new LinkedHashMap<>();
        for (String country : WORLD_COUNTRIES) {
            String key = country.toLowerCase();
            int count = 0;
            int idx = 0;
            while ((idx = textLower.indexOf(key, idx)) != -1) {
                count++;
                idx += key.length();
            }
            if (count > 0) {
                // Use canonical name (prefer first encountered)
                String canonical = countryCounts.containsKey(country) ? country : country;
                countryCounts.merge(country, count, Integer::sum);
            }
        }

        // Sort by count descending
        List<Map.Entry<String, Integer>> sorted = countryCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .collect(Collectors.toList());

        if (statsCountriesCountLabel != null)
            statsCountriesCountLabel.setText(String.valueOf(sorted.size()));

        if (countryCardsContainer != null) {
            countryCardsContainer.getChildren().clear();
            if (sorted.isEmpty()) {
                Label none = new Label("Aucun pays detecte dans ce forum");
                none.setStyle("-fx-text-fill: rgba(200,220,210,0.55); -fx-font-size: 14;");
                countryCardsContainer.getChildren().add(none);
            } else {
                for (Map.Entry<String, Integer> entry : sorted) {
                    countryCardsContainer.getChildren().add(buildCountryCard(entry.getKey(), entry.getValue()));
                }
            }
        }

        // Show overlay with fade in
        statsOverlay.setManaged(true);
        statsOverlay.setVisible(true);
        statsOverlay.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(220), statsOverlay);
        ft.setToValue(1.0);
        ft.play();
    }

    private VBox buildCountryCard(String country, int count) {
        String flag = getCountryFlag(country);
        Label flagLabel = new Label(flag);
        flagLabel.setStyle("-fx-font-size: 28px;");

        Label nameLabel = new Label(country);
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: white; -fx-wrap-text: true;");
        nameLabel.setMaxWidth(110);
        nameLabel.setAlignment(Pos.CENTER);

        Label countLabel = new Label(count + " mention" + (count > 1 ? "s" : ""));
        countLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #50C878; -fx-font-weight: 700;");

        VBox card = new VBox(6, flagLabel, nameLabel, countLabel);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(130);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.08);"
                + "-fx-border-color: rgba(80,200,120,0.25); -fx-border-width: 1;"
                + "-fx-border-radius: 14; -fx-background-radius: 14;"
                + "-fx-padding: 16 10; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.35),10,0,0,4);");
        return card;
    }

    private String getCountryFlag(String country) {
        // Map common countries to flag emojis
        Map<String, String> flags = new LinkedHashMap<>();
        flags.put("France", "\uD83C\uDDEB\uD83C\uDDF7");
        flags.put("Spain", "\uD83C\uDDEA\uD83C\uDDF8");
        flags.put("Espagne", "\uD83C\uDDEA\uD83C\uDDF8");
        flags.put("Italy", "\uD83C\uDDEE\uD83C\uDDF9");
        flags.put("Italie", "\uD83C\uDDEE\uD83C\uDDF9");
        flags.put("Germany", "\uD83C\uDDE9\uD83C\uDDEA");
        flags.put("Allemagne", "\uD83C\uDDE9\uD83C\uDDEA");
        flags.put("USA", "\uD83C\uDDFA\uD83C\uDDF8");
        flags.put("United States", "\uD83C\uDDFA\uD83C\uDDF8");
        flags.put("UK", "\uD83C\uDDEC\uD83C\uDDE7");
        flags.put("United Kingdom", "\uD83C\uDDEC\uD83C\uDDE7");
        flags.put("China", "\uD83C\uDDE8\uD83C\uDDF3");
        flags.put("Chine", "\uD83C\uDDE8\uD83C\uDDF3");
        flags.put("Japan", "\uD83C\uDDEF\uD83C\uDDF5");
        flags.put("Japon", "\uD83C\uDDEF\uD83C\uDDF5");
        flags.put("India", "\uD83C\uDDEE\uD83C\uDDF3");
        flags.put("Inde", "\uD83C\uDDEE\uD83C\uDDF3");
        flags.put("Brazil", "\uD83C\uDDE7\uD83C\uDDF7");
        flags.put("Bresil", "\uD83C\uDDE7\uD83C\uDDF7");
        flags.put("Canada", "\uD83C\uDDE8\uD83C\uDDE6");
        flags.put("Australia", "\uD83C\uDDE6\uD83C\uDDFA");
        flags.put("Morocco", "\uD83C\uDDF2\uD83C\uDDE6");
        flags.put("Maroc", "\uD83C\uDDF2\uD83C\uDDE6");
        flags.put("Algeria", "\uD83C\uDDE9\uD83C\uDDFF");
        flags.put("Algerie", "\uD83C\uDDE9\uD83C\uDDFF");
        flags.put("Tunisia", "\uD83C\uDDF9\uD83C\uDDF3");
        flags.put("Tunisie", "\uD83C\uDDF9\uD83C\uDDF3");
        flags.put("Egypt", "\uD83C\uDDEA\uD83C\uDDEC");
        flags.put("Egypte", "\uD83C\uDDEA\uD83C\uDDEC");
        flags.put("Turkey", "\uD83C\uDDF9\uD83C\uDDF7");
        flags.put("Turquie", "\uD83C\uDDF9\uD83C\uDDF7");
        flags.put("Russia", "\uD83C\uDDF7\uD83C\uDDFA");
        flags.put("Russie", "\uD83C\uDDF7\uD83C\uDDFA");
        flags.put("Portugal", "\uD83C\uDDF5\uD83C\uDDF9");
        flags.put("Greece", "\uD83C\uDDEC\uD83C\uDDF7");
        flags.put("Grece", "\uD83C\uDDEC\uD83C\uDDF7");
        flags.put("Mexico", "\uD83C\uDDF2\uD83C\uDDFD");
        flags.put("Mexique", "\uD83C\uDDF2\uD83C\uDDFD");
        flags.put("Argentina", "\uD83C\uDDE6\uD83C\uDDF7");
        flags.put("Argentine", "\uD83C\uDDE6\uD83C\uDDF7");
        flags.put("Saudi Arabia", "\uD83C\uDDF8\uD83C\uDDE6");
        flags.put("Arabie", "\uD83C\uDDF8\uD83C\uDDE6");
        flags.put("UAE", "\uD83C\uDDE6\uD83C\uDDEA");
        flags.put("Emirats", "\uD83C\uDDE6\uD83C\uDDEA");
        flags.put("United Arab Emirates", "\uD83C\uDDE6\uD83C\uDDEA");
        flags.put("Lebanon", "\uD83C\uDDF1\uD83C\uDDE7");
        flags.put("Liban", "\uD83C\uDDF1\uD83C\uDDE7");
        flags.put("Jordan", "\uD83C\uDDEF\uD83C\uDDF4");
        flags.put("Jordanie", "\uD83C\uDDEF\uD83C\uDDF4");
        flags.put("Thailand", "\uD83C\uDDF9\uD83C\uDDED");
        flags.put("Vietnam", "\uD83C\uDDFB\uD83C\uDDF3");
        flags.put("Singapore", "\uD83C\uDDF8\uD83C\uDDEC");
        flags.put("South Africa", "\uD83C\uDDFF\uD83C\uDDE6");
        flags.put("Nigeria", "\uD83C\uDDF3\uD83C\uDDEC");
        flags.put("Kenya", "\uD83C\uDDF0\uD83C\uDDEA");
        flags.put("Senegal", "\uD83C\uDDF8\uD83C\uDDF3");
        flags.put("Cameroun", "\uD83C\uDDE8\uD83C\uDDF2");
        flags.put("Cameroon", "\uD83C\uDDE8\uD83C\uDDF2");
        flags.put("Belgium", "\uD83C\uDDE7\uD83C\uDDEA");
        flags.put("Belgique", "\uD83C\uDDE7\uD83C\uDDEA");
        flags.put("Switzerland", "\uD83C\uDDE8\uD83C\uDDED");
        flags.put("Netherlands", "\uD83C\uDDF3\uD83C\uDDF1");
        flags.put("Poland", "\uD83C\uDDF5\uD83C\uDDF1");
        flags.put("Sweden", "\uD83C\uDDF8\uD83C\uDDEA");
        flags.put("Norway", "\uD83C\uDDF3\uD83C\uDDF4");
        flags.put("Denmark", "\uD83C\uDDE9\uD83C\uDDF0");
        flags.put("Finland", "\uD83C\uDDEB\uD83C\uDDEE");
        String flag = flags.get(country);
        return flag != null ? flag : "\uD83C\uDF0D";
    }

    @FXML
    public void handleCloseStats() {
        if (statsOverlay == null) return;
        FadeTransition ft = new FadeTransition(Duration.millis(180), statsOverlay);
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            statsOverlay.setVisible(false);
            statsOverlay.setManaged(false);
        });
        ft.play();
    }

    @FXML
    public void handleStatsContainerClick(MouseEvent event) {
        event.consume(); // prevent click propagating to overlay (which closes it)
    }

    private void updateStats(List<Forum> list) {
        if (statForumsLabel != null)
            statForumsLabel.setText(String.valueOf(list.size()));
        if (statMembersLabel != null)
            statMembersLabel.setText(String.valueOf(
                    list.stream().mapToInt(Forum::getNbr_members).sum()));
        if (statPostsLabel != null)
            statPostsLabel.setText(String.valueOf(
                    list.stream().mapToInt(Forum::getPost_count).sum()));
    }

    // ─── Search ─────────────────────────────────────────────────────────────

    @FXML
    private void handleSearch() {
        if (allForums == null) return;
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            renderForums(allForums);
            return;
        }
        List<Forum> filtered = allForums.stream()
                .filter(f -> (f.getName() != null && f.getName().toLowerCase().contains(query))
                        || (f.getDescription() != null && f.getDescription().toLowerCase().contains(query)))
                .collect(Collectors.toList());
        renderForums(filtered);
    }

    // ─── Tab Filters ────────────────────────────────────────────────────────

    @FXML
    private void handleTabPopular() {
        activeTab = "popular";
        updateTabStyles();
        if (allForums == null) return;
        List<Forum> sorted = allForums.stream()
                .sorted((a, b) -> Integer.compare(b.getNbr_members(), a.getNbr_members()))
                .collect(Collectors.toList());
        renderForums(sorted);
    }

    @FXML
    private void handleTabRecent() {
        activeTab = "recent";
        updateTabStyles();
        if (allForums == null) return;
        List<Forum> sorted = allForums.stream()
                .sorted((a, b) -> {
                    if (b.getDate_creation() == null) return -1;
                    if (a.getDate_creation() == null) return 1;
                    return b.getDate_creation().compareTo(a.getDate_creation());
                })
                .collect(Collectors.toList());
        renderForums(sorted);
    }

    @FXML
    private void handleTabMine() {
        activeTab = "mine";
        updateTabStyles();
        if (allForums == null || SessionManager.getCurrentUser() == null) return;
        int userId = SessionManager.getCurrentUser().getId();
        List<Forum> mine = allForums.stream()
                .filter(f -> f.getCreated_by() == userId)
                .collect(Collectors.toList());
        renderForums(mine);
    }

    private void updateTabStyles() {
        if (tabPopular != null) tabPopular.setSelected("popular".equals(activeTab));
        if (tabRecent != null)  tabRecent.setSelected("recent".equals(activeTab));
        if (tabMine != null)    tabMine.setSelected("mine".equals(activeTab));
    }

    // ─── PDF Export ─────────────────────────────────────────────────────────

    @FXML
    private void handleExportPdf() {
        if (allForums == null || allForums.isEmpty()) {
            showAlert("Export", "Aucun forum à exporter.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les forums en PDF");
        chooser.setInitialFileName("govibe-forums.pdf");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        File file = chooser.showSaveDialog(rootStackPane.getScene().getWindow());
        if (file == null) return;
        try {
            ForumPdfExporter.exportForums(allForums, file);
            showAlert("Succès ✅", "PDF exporté avec succès :\n" + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de l'export PDF:\n" + e.getMessage());
        }
    }

    // ─── Chat Room ──────────────────────────────────────────────────────────

    @FXML
    private void handleOpenChat() {
        org.example.mains.MainApp.switchScene("/poste-forumviews/ForumChatRoom.fxml", "GoVibe Chat IA");
    }

    // ─── Overlay Animations ─────────────────────────────────────────────────

    public void showFormOverlay(Parent formRoot) {
        formContainer.getChildren().clear();
        formContainer.getChildren().add(formRoot);
        formOverlay.setVisible(true);
        formOverlay.setManaged(true);

        // Blur every background child (all except overlay panes)
        GaussianBlur blur = new GaussianBlur(8);
        for (Node child : rootStackPane.getChildren()) {
            if (child != formOverlay && child != statsOverlay) {
                child.setEffect(blur);
            }
        }

        // Overlay fade-in
        FadeTransition fadeIn = new FadeTransition(Duration.millis(280), formOverlay);
        fadeIn.setFromValue(0.0); fadeIn.setToValue(1.0);

        // Form card: slide up + scale in
        formContainer.setScaleX(0.92); formContainer.setScaleY(0.92);
        TranslateTransition slideUp = new TranslateTransition(Duration.millis(300), formContainer);
        slideUp.setFromY(60); slideUp.setToY(0);
        javafx.animation.ScaleTransition scaleIn =
                new javafx.animation.ScaleTransition(Duration.millis(300), formContainer);
        scaleIn.setToX(1.0); scaleIn.setToY(1.0);

        new ParallelTransition(fadeIn, slideUp, scaleIn).play();
    }

    public void hideFormOverlay() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), formOverlay);
        fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);
        TranslateTransition slideDown = new TranslateTransition(Duration.millis(250), formContainer);
        slideDown.setFromY(0); slideDown.setToY(50);

        ParallelTransition pt = new ParallelTransition(fadeOut, slideDown);
        pt.setOnFinished(e -> {
            formOverlay.setVisible(false);
            formOverlay.setManaged(false);
            formContainer.getChildren().clear();
            // Remove blur from all background children
            for (Node child : rootStackPane.getChildren()) {
                if (child != formOverlay && child != statsOverlay) {
                    child.setEffect(null);
                }
            }
            loadForums();
        });
        pt.play();
    }

    // ─── Navigation ─────────────────────────────────────────────────────────

    @FXML
    private void handleAjoutForum(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/poste-forumviews/AjoutForum.fxml"));
            Parent root = loader.load();
            AjoutForumController controller = loader.getController();
            controller.setOverlayController(this);
            showFormOverlay(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleGoToPosts(ActionEvent event) {
        boolean isAdmin = SessionManager.getCurrentUser() != null
                && "admin".equalsIgnoreCase(SessionManager.getCurrentUser().getRole());
        if (isAdmin) {
            AdminListPostController.clearFilter();
            org.example.mains.MainApp.switchScene("/org/example/AdminPostView.fxml", "Publications Admin");
        } else {
            org.example.mains.MainApp.switchScene("/poste-forumviews/ListPost.fxml", "Publications");
        }
    }

    @FXML
    private void handleGoToForums(ActionEvent event) { loadForums(); }

    @FXML
    private void handleResetFilters() { loadForums(); }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            if (SessionManager.getCurrentUser() != null && "admin".equalsIgnoreCase(SessionManager.getCurrentUser().getRole()))
                org.example.mains.MainApp.switchScene("/org/example/AdminDashboardView.fxml", "Admin Dashboard");
            else
                org.example.mains.MainApp.switchScene("/org/example/UserHomeView.fxml", "Accueil");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.clear();
        org.example.mains.MainApp.switchScene("/org/example/LoginView.fxml", "GoVibe - Connexion");
    }

    @FXML private void handleGoHome() { handleBack(null); }
    @FXML private void handleGoActivities() { org.example.mains.MainApp.switchScene("/UserHome.fxml", "Activités"); }
    @FXML private void handleGoLocations() { org.example.mains.MainApp.switchScene("/LocationListView.fxml", "Locations"); }
    @FXML private void handleGoFlights() { org.example.mains.MainApp.switchScene("/views/user-dashboard.fxml", "Vols"); }
    @FXML private void handleGoHotels() { org.example.mains.MainApp.switchScene("/views/room-booking.fxml", "Hôtels"); }
    @FXML private void handleProfile() { showAlert("Profil", "Fonctionnalité à venir."); }
    @FXML private void handleMyReservations() { org.example.mains.MainApp.switchScene("/views/room-booking.fxml", "Réservations"); }
    @FXML private void handleMyLocations() { org.example.mains.MainApp.switchScene("/LocationListView.fxml", "Locations"); }
    @FXML private void handleMyFlights() { org.example.mains.MainApp.switchScene("/views/user-dashboard.fxml", "Vols"); }
    @FXML private void handleSettings() { showAlert("Paramètres", "Fonctionnalité à venir."); }
    @FXML private void handleHelp() { showAlert("Aide", "Contactez support@govibe.tn"); }

    // ─── Utilities ──────────────────────────────────────────────────────────

    private void showAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void setupHeroBackground() {
        if (bgImageView != null && rootStackPane != null) {
            bgImageView.fitWidthProperty().bind(rootStackPane.widthProperty());
            bgImageView.fitHeightProperty().bind(rootStackPane.heightProperty());
            var url = getClass().getResource("/messages/home-hero5.png");
            if (url != null) bgImageView.setImage(new Image(url.toExternalForm(), true));
        }
    }
}
