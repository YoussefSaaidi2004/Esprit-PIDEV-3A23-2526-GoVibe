package org.example.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import netscape.javascript.JSObject;

import com.google.gson.reflect.TypeToken;
import org.example.entities.Activity;
import org.example.entities.Activite;
import org.example.services.ServiceActivite;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MapActivitiesController {

    @FXML private StackPane mainStack;
    @FXML private ImageView bgImageView;
    @FXML private VBox sidePanel;
    @FXML private WebView mapWebView;
    @FXML private TextField searchField;
    @FXML private VBox detailsVBox;
    @FXML private VBox emptyState;
    @FXML private VBox countryContent;
    @FXML private ImageView countryFlag;
    @FXML private Label countryName;
    @FXML private Label countryRegion;
    @FXML private Label capitalLabel;
    @FXML private Label populationLabel;
    @FXML private Label currencyLabel;
    @FXML private Label languageLabel;
    @FXML private VBox attractionsContainer;
    @FXML private VBox loadingOverlay;
    @FXML private VBox errorOverlay;
    @FXML private Label errorMessage;
    @FXML private Label emptyTitle;
    @FXML private Button aiInsightsBtn;
    @FXML private Label emptySubtitle;

    // --- DB Activities & Propose Popup ---
    @FXML private VBox root;
    @FXML private Label activityCountLabel;
    @FXML private Label activitySubtitle;
    @FXML private TextField activitySearchField;
    @FXML private TilePane dbActivitiesPane;
    @FXML private VBox emptyActivities;
    @FXML private HBox activitiesLoading;
    @FXML private Button proposeBtn;
    @FXML private StackPane modalOverlay;
    @FXML private VBox popupCard;
    @FXML private TextField popupName;
    @FXML private TextField popupType;
    @FXML private TextField popupPrix;
    @FXML private ComboBox<String> popupLocCombo;
    @FXML private TextArea popupDesc;
    @FXML private Label popupFeedback;
    @FXML private Button submitPopupBtn;

    private final ServiceActivite serviceActivite = new ServiceActivite();
    private List<Activite> allDbActivities = new ArrayList<>();

    private WebEngine engine;
    private final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
    private Task<CountryData> currentTask;
    
    // API Keys (Centralized) - Fallback system with Gemini support
    private static final String GEMINI_KEY = "AIzaSyCNgI3fpLERtasCdFw2R1qBn1ENIpTKQNc";
    private static final String[] DEEPSEEK_KEYS = {
        "sk-868178ef790c4205acc04d02d4792ee2",
        "sk-4490f1e6d27546df86fce308c484cf87",
        "sk-df61158ebba24dff98c7d1d7df5715ce"
    };
    private static final String GEOAPIFY_KEY = "36873335552a446583d789e921606822"; 

    // Caching
    private static class CachedData<T> {
        final T data;
        final Instant expiry;
        CachedData(T data, int ttlHours) {
            this.data = data;
            this.expiry = Instant.now().plus(java.time.Duration.ofHours(ttlHours));
        }
        boolean isExpired() { return Instant.now().isAfter(expiry); }
    }
    private final Map<String, CachedData<CountryData>> countryCache = new HashMap<>();
    private Map<String, List<Activity>> activitiesMap = new HashMap<>();

    @FXML
    public void initialize() {
        engine = mapWebView.getEngine();
        
        // Bind background image to stack size for true full-bleed effect
        if (bgImageView != null && mainStack != null) {
            bgImageView.fitWidthProperty().bind(mainStack.widthProperty());
            bgImageView.fitHeightProperty().bind(mainStack.heightProperty());
            
            // Debug listener to log load status
            bgImageView.imageProperty().addListener((obs, oldImg, newImg) -> {
                if (newImg != null) {
                    newImg.errorProperty().addListener((o, eOld, eNew) -> {
                        if (eNew) System.err.println("[Premium UI] Failed to load background: " + newImg.getException());
                        else System.out.println("[Premium UI] Background image loaded successfully");
                    });
                }
            });
            
            setupHeroBackground();
        }

        // Setup Bridge
        engine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaConnector", this);
                System.out.println("[Map] Bridge established");
            }
        });

        // Load Map
        var mapUrl = getClass().getResource("/map.html");
        if (mapUrl != null) {
            engine.load(mapUrl.toExternalForm());
        } else {
            showError("Impossible de charger la carte locale.");
        }

        // Setup loading/empty states
        countryContent.setManaged(false);
        countryContent.setVisible(false);
        
        // Load AI Activities for all countries
        loadActivities();

        // Load DB activities and setup popup
        loadDbActivities();
        
        // Disable context menu for snappier feel
        mapWebView.setContextMenuEnabled(false);
    }

    private void setupHeroBackground() {
        var resourcePath = "/messages/home-hero5.png";
        var url = getClass().getResource(resourcePath);
        if (url != null) {
            Image img = new Image(url.toExternalForm(), true);
            bgImageView.setImage(img);
            System.out.println("[Background] Hero image loaded successfully in MapActivitiesView");
        } else {
            System.err.println("[Background] ERROR: Resource " + resourcePath + " not found!");
        }
    }

    private void loadActivities() {
        String path = "/data/activities.json";
        InputStream is = getClass().getResourceAsStream(path);
        
        if (is == null) {
            System.err.println("[AI] CRITICAL ERROR: File not found at " + path);
            // Try fallback without leading slash
            is = getClass().getClassLoader().getResourceAsStream("data/activities.json");
        }

        if (is != null) {
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                Type type = new TypeToken<Map<String, List<Activity>>>(){}.getType();
                activitiesMap = new com.google.gson.Gson().fromJson(reader, type);
                if (activitiesMap == null) activitiesMap = new HashMap<>();
                System.out.println("[AI] Successfully loaded " + activitiesMap.size() + " countries from cache.");
                if (!activitiesMap.containsKey("FR")) {
                    System.err.println("[AI] WARNING: France (FR) missing from loaded JSON!");
                }
            } catch (Exception e) {
                System.err.println("[AI] Failed to parse activities JSON: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("[AI] FAILED to load activities.json from both classpath and classloader root.");
        }
    }

    /**
     * Called from JavaScript when map is clicked
     */
    public void onMapClick(double lat, double lon) {
        System.out.println(String.format("[Map] Clicked at: %.6f, %.6f", lat, lon));
        Platform.runLater(() -> fetchCountryData(lat, lon));
    }

    private void fetchCountryData(double lat, double lon) {
        // 1. Cancel previous task if running
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
        }

        showLoading(true);
        hideError();

        currentTask = new Task<>() {
            @Override
            protected CountryData call() throws Exception {
                // Throttling check removed for better responsiveness

                // Check Cache first if we had a country code... but we only have lat/lon here.
                // We'll reverse geocode first.
                
                // STEP 1: Nominatim Reverse Geocoding - Use Locale.ROOT to ensure dots for decimals
                String nominatimUrl = String.format(java.util.Locale.ROOT, "https://nominatim.openstreetmap.org/reverse?format=json&lat=%f&lon=%f", lat, lon);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(nominatimUrl))
                        .header("User-Agent", "GoVibe-Native-App/1.0")
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (isCancelled()) return null;

                JsonObject geoJson = JsonParser.parseString(response.body()).getAsJsonObject();
                if (geoJson.has("error")) {
                    return null; // Return null to indicate no country found (Ocean)
                }

                JsonObject address = geoJson.getAsJsonObject("address");
                String countryCode = address.get("country_code").getAsString().toUpperCase();
                String countryNameStr = address.get("country").getAsString();

                // Check Cache with Country Code
                if (countryCache.containsKey(countryCode)) {
                    CachedData<CountryData> cached = countryCache.get(countryCode);
                    if (!cached.isExpired()) {
                        System.out.println("[Cache] Hit for " + countryCode);
                        return cached.data;
                    }
                }

                // STEP 2: REST Countries Info
                String restCountriesUrl = "https://restcountries.com/v3.1/alpha/" + countryCode;
                HttpRequest restReq = HttpRequest.newBuilder().uri(URI.create(restCountriesUrl)).build();
                HttpResponse<String> restRes = httpClient.send(restReq, HttpResponse.BodyHandlers.ofString());
                
                JsonArray countryArr = JsonParser.parseString(restRes.body()).getAsJsonArray();
                JsonObject countryJson = countryArr.get(0).getAsJsonObject();

                // CLEAN NAME: Use English common name to avoid encoding/font issues (square boxes)
                String cleanName = countryJson.getAsJsonObject("name").get("common").getAsString();
                
                CountryData data = new CountryData(cleanName, countryCode);
                data.capital = countryJson.has("capital") ? countryJson.getAsJsonArray("capital").get(0).getAsString() : "N/A";
                data.population = countryJson.get("population").getAsLong();
                data.region = countryJson.get("region").getAsString();
                data.flagUrl = countryJson.getAsJsonObject("flags").get("png").getAsString();
                
                // Currencies
                JsonObject currencies = countryJson.getAsJsonObject("currencies");
                if (currencies != null && !currencies.keySet().isEmpty()) {
                    String firstKey = currencies.keySet().iterator().next();
                    data.currency = currencies.getAsJsonObject(firstKey).get("name").getAsString();
                }

                // Languages
                JsonObject languages = countryJson.getAsJsonObject("languages");
                if (languages != null && !languages.keySet().isEmpty()) {
                    String firstKey = languages.keySet().iterator().next();
                    data.language = languages.get(firstKey).getAsString();
                }

                // STEP 3: Activities (Localized AI Data)
                data.aiActivities = activitiesMap.getOrDefault(countryCode, Collections.emptyList());
                
                // Fallback attempt for live data only if AI data is empty (Optional)
                if (data.aiActivities.isEmpty()) {
                    try {
                        String geoapifyUrl = String.format(java.util.Locale.ROOT, "https://api.geoapify.com/v2/places?categories=tourism.attraction&filter=countrycode:%s&limit=5&apiKey=%s", 
                                countryCode.toLowerCase(), GEOAPIFY_KEY);
                        // ... existing Geoapify logic if needed for long tail countries
                    } catch (Exception ex) {
                        System.err.println("[Geoapify] Failed: " + ex.getMessage());
                    }
                }

                // Cache result
                countryCache.put(countryCode, new CachedData<>(data, 24));
                return data;
            }

            @Override
            protected void succeeded() {
                CountryData result = getValue();
                if (result != null) {
                    updateUI(result);
                    hideError();
                } else {
                    showOceanMessage();
                }
                showLoading(false);
            }

            @Override
            protected void failed() {
                showLoading(false);
                Throwable ex = getException();
                if (ex != null && !(ex instanceof java.util.concurrent.CancellationException)) {
                    showError(ex.getMessage() != null ? ex.getMessage() : "Erreur de service");
                    ex.printStackTrace();
                }
            }
        };

        new Thread(currentTask).start();
    }

    private void updateUI(CountryData data) {
        emptyState.setManaged(false);
        emptyState.setVisible(false);
        
        countryContent.setManaged(true);
        countryContent.setVisible(true);
        
        countryName.setText(data.name.toUpperCase());
        countryRegion.setText(data.region);
        capitalLabel.setText(data.capital);
        populationLabel.setText(String.format("%,d", data.population));
        currencyLabel.setText(data.currency);
        languageLabel.setText(data.language);
        
        // Load flag
        countryFlag.setImage(new Image(data.flagUrl, true));

        // Update attractions
        attractionsContainer.getChildren().clear();
        if (data.aiActivities != null && !data.aiActivities.isEmpty()) {
            for (Activity a : data.aiActivities) {
                VBox card = createActivityCard(a);
                attractionsContainer.getChildren().add(card);
            }
        } else {
            VBox emptyBox = new VBox(15);
            emptyBox.setAlignment(javafx.geometry.Pos.CENTER);
            emptyBox.setStyle("-fx-padding: 20; -fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 15;");
            
            Label placeholder = new Label("Nous n'avons pas encore d'activités pour ce pays.");
            placeholder.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-style: italic;");
            
            Button askAI = new Button("Demander des idées à l'IA 🤖");
            askAI.getStyleClass().add("btn-ai-mini");
            askAI.setOnAction(e -> handleAskAI());
            
            emptyBox.getChildren().addAll(placeholder, askAI);
            attractionsContainer.getChildren().add(emptyBox);
        }

        // Premium Entrance Animation
        Platform.runLater(() -> {
            countryContent.setOpacity(0);
            countryContent.setTranslateY(30);
            
            FadeTransition ft = new FadeTransition(Duration.millis(600), countryContent);
            ft.setFromValue(0);
            ft.setToValue(1);
            
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(Duration.millis(600), countryContent);
            tt.setToY(0);
            
            ft.play();
            tt.play();
            
            // Force map resize after layout settles
            engine.executeScript("if(typeof map !== 'undefined') map.invalidateSize();");
        });
    }

    @FXML
    private void handleAskAI() {
        if (countryName.getText().isEmpty()) return;
        
        showLoading(true);
        String country = countryName.getText();
        
        Task<String> aiTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                String prompt = "Agis comme un guide de voyage indépendant et passionné. Donne-moi 3 conseils de voyage uniques et secrets (hidden gems) pour " + country + ". " +
                        "Sois concis et passionnant. Réponds en français.";
                
                JsonObject root = new JsonObject();
                root.addProperty("model", "deepseek-chat");
                JsonArray messages = new JsonArray();
                JsonObject msg = new JsonObject();
                msg.addProperty("role", "user");
                msg.addProperty("content", prompt);
                messages.add(msg);
                root.add("messages", messages);
                
                String lastError = "";
                
                // --- Phase 1: Try Gemini (New Primary) ---
                try {
                    String geminiPrompt = "Agis comme un guide de voyage indépendant et passionné. Donne-moi 3 conseils de voyage uniques et secrets (hidden gems) pour " + country + 
                                         ". Sois concis et passionnant. Réponds en français.";
                    
                    JsonObject geminiRoot = new JsonObject();
                    JsonArray contents = new JsonArray();
                    JsonObject contentObj = new JsonObject();
                    JsonArray parts = new JsonArray();
                    JsonObject partObj = new JsonObject();
                    partObj.addProperty("text", geminiPrompt);
                    parts.add(partObj);
                    contentObj.add("parts", parts);
                    contents.add(contentObj);
                    geminiRoot.add("contents", contents);

                    // MODELS TO TRY: Verified list for this specific project/key
                    String[] models = {
                        "gemini-2.0-flash", 
                        "gemini-2.5-flash", 
                        "gemini-2.0-flash-lite", 
                        "gemini-flash-latest",
                        "gemini-1.5-flash"
                    };
                    String[] versions = {"v1", "v1beta"};
                    
                    HttpResponse<String> geminiRes = null;
                    boolean success = false;
                    
                    outer: for (String v : versions) {
                        for (String m : models) {
                            try {
                                String url = String.format("https://generativelanguage.googleapis.com/%s/models/%s:generateContent?key=%s", v, m, GEMINI_KEY);
                                HttpRequest geminiReq = HttpRequest.newBuilder()
                                        .uri(URI.create(url))
                                        .header("Content-Type", "application/json")
                                        .POST(HttpRequest.BodyPublishers.ofString(new com.google.gson.Gson().toJson(geminiRoot)))
                                        .build();

                                geminiRes = httpClient.send(geminiReq, HttpResponse.BodyHandlers.ofString());
                                if (geminiRes.statusCode() == 200) {
                                    JsonObject resJson = JsonParser.parseString(geminiRes.body()).getAsJsonObject();
                                    return resJson.getAsJsonArray("candidates").get(0).getAsJsonObject()
                                                 .getAsJsonObject("content").getAsJsonArray("parts").get(0).getAsJsonObject()
                                                 .get("text").getAsString();
                                }
                                System.err.println("[Gemini] " + v + "/" + m + " failed with " + geminiRes.statusCode());
                            } catch (Exception ex) {
                                System.err.println("[Gemini] Error trying " + m + ": " + ex.getMessage());
                            }
                        }
                    }
                    
                    if (geminiRes != null) {
                        System.err.println("[Gemini] All attempts failed. Last body: " + geminiRes.body());
                        lastError = "Gemini " + geminiRes.statusCode();
                    }
                } catch (Exception ex) {
                    System.err.println("[Gemini] Request failed: " + ex.getMessage());
                    lastError = "Gemini " + ex.getMessage();
                }

                // --- Phase 2: Fallback to DeepSeek Keys ---
                for (String key : DEEPSEEK_KEYS) {
                    try {
                        HttpRequest req = HttpRequest.newBuilder()
                                .uri(URI.create("https://api.deepseek.com/v1/chat/completions"))
                                .header("Content-Type", "application/json")
                                .header("Authorization", "Bearer " + key)
                                .POST(HttpRequest.BodyPublishers.ofString(new com.google.gson.Gson().toJson(root)))
                                .build();

                        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                        
                        if (res.statusCode() == 200) {
                            JsonObject resJson = JsonParser.parseString(res.body()).getAsJsonObject();
                            return resJson.getAsJsonArray("choices").get(0).getAsJsonObject()
                                         .getAsJsonObject("message").get("content").getAsString();
                        } else if (res.statusCode() == 402) {
                            System.err.println("[AI] Key balance insufficient, trying next...");
                            lastError = "402";
                            continue;
                        } else {
                            System.err.println("[AI] API Error " + res.statusCode() + ": " + res.body());
                            lastError = String.valueOf(res.statusCode());
                        }
                    } catch (Exception ex) {
                        System.err.println("[AI] Request failed: " + ex.getMessage());
                        lastError = ex.getMessage();
                    }
                }
                throw new RuntimeException("DeepSeek Error: " + lastError);
            }

            @Override
            protected void succeeded() {
                showLoading(false);
                
                // Apply premium blur to the background
                GaussianBlur blur = new GaussianBlur(15);
                mainStack.setEffect(blur);
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Conseils GoVibe IA 🤖");
                alert.setHeaderText("Inspirations pour " + country);
                alert.setContentText(getValue());
                alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles/unified-styles.css").toExternalForm());
                alert.getDialogPane().getStyleClass().add("custom-alert");
                
                // Block and then remove blur
                alert.showAndWait();
                mainStack.setEffect(null);
            }

            @Override
            protected void failed() {
                showLoading(false);
                Throwable ex = getException();
                if (ex != null && ex.getMessage() != null && ex.getMessage().contains("402")) {
                    showError("Solde DeepSeek insuffisant. Veuillez recharger votre compte ou utiliser une autre clé.");
                } else {
                    showError("L'IA est occupée à préparer son prochain voyage. Réessayez plus tard !");
                }
            }
        };
        new Thread(aiTask).start();
    }

    private VBox createActivityCard(Activity a) {
        VBox card = new VBox(8);
        card.getStyleClass().add("activity-card-premium");
        
        Label category = new Label(a.getCategory().toUpperCase());
        category.getStyleClass().add("activity-category");
        
        Label name = new Label(a.getName());
        name.getStyleClass().add("activity-name");
        
        Label desc = new Label(a.getDescription());
        desc.getStyleClass().add("activity-desc");
        desc.setWrapText(true);
        
        card.getChildren().addAll(category, name, desc);
        
        // Entrance animation for each card (Staggered effect)
        card.setOpacity(0);
        card.setTranslateY(20);
        
        Platform.runLater(() -> {
            FadeTransition ft = new FadeTransition(Duration.millis(500), card);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(100)); // Slight delay for flow
            
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(Duration.millis(500), card);
            tt.setToY(0);
            tt.setDelay(Duration.millis(100));
            
            ft.play();
            tt.play();
        });

        return card;
    }

    private void showLoading(boolean active) {
        loadingOverlay.setVisible(active);
    }

    private void showError(String msg) {
        errorMessage.setText(msg != null ? msg : "Erreur de connexion aux services.");
        errorOverlay.setVisible(true);
    }

    private void hideError() {
        errorOverlay.setVisible(false);
    }

    private void showOceanMessage() {
        countryContent.setVisible(false);
        countryContent.setManaged(false);
        emptyState.setVisible(true);
        emptyState.setManaged(true);
        
        emptyTitle.setText("Océan ou zone inconnue");
        emptySubtitle.setText("Aucun pays n'a été détecté à cet emplacement. Essayez de cliquer sur une terre ferme.");
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText();
        if (query == null || query.isBlank()) return;
        
        System.out.println("[Search] Query: " + query);
        showLoading(true);
        hideError();

        Task<double[]> searchTask = new Task<>() {
            @Override
            protected double[] call() throws Exception {
                String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
                String url = "https://nominatim.openstreetmap.org/search?format=json&q=" + encoded + "&limit=1";
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "GoVibe-Native-App/1.0")
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonArray results = JsonParser.parseString(response.body()).getAsJsonArray();
                
                if (results.size() > 0) {
                    JsonObject first = results.get(0).getAsJsonObject();
                    double lat = first.get("lat").getAsDouble();
                    double lon = first.get("lon").getAsDouble();
                    return new double[]{lat, lon};
                }
                return null;
            }

            @Override
            protected void succeeded() {
                double[] coords = getValue();
                if (coords != null) {
                    System.out.println(String.format("[Search] Found: %.6f, %.6f", coords[0], coords[1]));
                    try {
                        // Ensure engine is ready and perform call with safety
                        engine.executeScript(String.format(java.util.Locale.ROOT, 
                            "if(typeof flyTo === 'function') { flyTo(%f, %f, %d); } else { console.error('flyTo not ready'); }", 
                            coords[0], coords[1], 6));
                    } catch (Exception e) {
                        System.err.println("[Map] Failed to execute flyTo: " + e.getMessage());
                    }
                    fetchCountryData(coords[0], coords[1]);
                } else {
                    showError("Aucun résultat trouvé pour cette recherche.");
                    showLoading(false);
                }
            }

            @Override
            protected void failed() {
                showLoading(false);
                showError("Erreur de recherche. Vérifiez votre connexion.");
            }
        };
        new Thread(searchTask).start();
    }

    @FXML
    private void handleRetry() {
        hideError();
        // Maybe repeat last click?
    }

    private void fetchWikipediaAttractions(double lat, double lon, CountryData data) {
        try {
            // Use the clean name for better Wiki searches
            String wikiUrl = String.format(java.util.Locale.ROOT, "https://en.wikipedia.org/w/api.php?action=query&list=geosearch&gsradius=15000&gscoord=%f|%f&format=json", lat, lon);
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(wikiUrl)).build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            
            JsonObject wikiJson = JsonParser.parseString(res.body()).getAsJsonObject();
            JsonObject query = wikiJson.getAsJsonObject("query");
            if (query != null) {
                JsonArray geosearch = query.getAsJsonArray("geosearch");
                for (JsonElement ge : geosearch) {
                    JsonObject entry = ge.getAsJsonObject();
                    data.aiActivities.add(new Activity(
                        entry.get("title").getAsString(), 
                        "Découvrez ce lieu historique via Wikipedia.", 
                        "Culture"
                    ));
                }
            }
        } catch (Exception ex) {
            System.err.println("[Wikipedia] Fallback failed: " + ex.getMessage());
        }
    }

    // ==================== DB ACTIVITIES ====================

    private void loadDbActivities() {
        if (activitiesLoading != null) {
            activitiesLoading.setVisible(true);
            activitiesLoading.setManaged(true);
        }
        Thread t = new Thread(() -> {
            List<Activite> list;
            try {
                list = serviceActivite.getAll();
                // Show only confirmed activities to users
                list = list.stream()
                    .filter(a -> Activite.STATUS_CONFIRMED.equalsIgnoreCase(a.getStatus()))
                    .collect(Collectors.toList());
            } catch (Exception e) {
                System.err.println("[DB Activities] Failed to load: " + e.getMessage());
                list = new ArrayList<>();
            }
            final List<Activite> result = list;
            Platform.runLater(() -> {
                allDbActivities = result;
                if (activitiesLoading != null) {
                    activitiesLoading.setVisible(false);
                    activitiesLoading.setManaged(false);
                }
                if (activityCountLabel != null) activityCountLabel.setText(String.valueOf(result.size()));
                if (activitySubtitle != null)
                    activitySubtitle.setText(result.size() + " activit" + (result.size() != 1 ? "és" : "é")
                        + " disponible" + (result.size() != 1 ? "s" : ""));
                refreshActivitiesGrid(result);

                // Wire up search filter
                if (activitySearchField != null) {
                    activitySearchField.textProperty().addListener((obs, old, val) -> {
                        String q = val == null ? "" : val.toLowerCase();
                        List<Activite> filtered = allDbActivities.stream()
                            .filter(a -> safe(a.getName()).contains(q)
                                || safe(a.getType()).contains(q)
                                || safe(a.getLocalisation()).contains(q))
                            .collect(Collectors.toList());
                        refreshActivitiesGrid(filtered);
                    });
                }
            });
        }, "DB-Activities-Loader");
        t.setDaemon(true);
        t.start();
    }

    private String safe(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    private void refreshActivitiesGrid(List<Activite> list) {
        if (dbActivitiesPane == null) return;
        dbActivitiesPane.getChildren().clear();
        if (list.isEmpty()) {
            if (emptyActivities != null) { emptyActivities.setVisible(true); emptyActivities.setManaged(true); }
        } else {
            if (emptyActivities != null) { emptyActivities.setVisible(false); emptyActivities.setManaged(false); }
            for (Activite a : list) {
                dbActivitiesPane.getChildren().add(createDbActivityCard(a));
            }
        }
    }

    private javafx.scene.Node createDbActivityCard(Activite a) {
        // Outer card
        VBox card = new VBox(12);
        card.setPrefWidth(410);
        card.setMinHeight(170);
        card.setStyle(
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-background-radius: 18;" +
            "-fx-border-color: rgba(80,200,120,0.22);" +
            "-fx-border-radius: 18;" +
            "-fx-border-width: 1;" +
            "-fx-padding: 18 20 16 20;" +
            "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.4),14,0,0,4);"
        );

        // Header row: icon + name + type badge
        HBox header = new HBox(14);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        StackPane iconCircle = new StackPane();
        iconCircle.setMinSize(44, 44);
        iconCircle.setMaxSize(44, 44);
        iconCircle.setStyle("-fx-background-color: rgba(80,200,120,0.18); -fx-background-radius: 50%;");
        String emoji = typeEmoji(a.getType());
        Label iconLbl = new Label(emoji);
        iconLbl.setStyle("-fx-font-size: 20;");
        iconCircle.getChildren().add(iconLbl);

        VBox nameBox = new VBox(4);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        Label nameLbl = new Label(a.getName() != null ? a.getName() : "Activité");
        nameLbl.setStyle("-fx-font-size: 15; -fx-font-weight: 900; -fx-text-fill: white;");
        nameLbl.setWrapText(true);

        Label typeBadge = new Label(a.getType() != null ? a.getType().toUpperCase() : "");
        typeBadge.setStyle(
            "-fx-background-color: rgba(80,200,120,0.2);" +
            "-fx-text-fill: #50C878;" +
            "-fx-font-size: 9;" +
            "-fx-font-weight: 900;" +
            "-fx-padding: 3 9;" +
            "-fx-background-radius: 8;"
        );

        nameBox.getChildren().addAll(nameLbl, typeBadge);
        header.getChildren().addAll(iconCircle, nameBox);

        // Description
        Label desc = new Label(a.getDescription() != null ? a.getDescription() : "");
        desc.setStyle("-fx-text-fill: rgba(255,255,255,0.62); -fx-font-size: 12.5;");
        desc.setWrapText(true);
        desc.setMaxWidth(Double.MAX_VALUE);

        // Footer row: location + price
        HBox footer = new HBox();
        footer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label locLbl = new Label("\uD83D\uDCCD " + (a.getLocalisation() != null ? a.getLocalisation() : "-"));
        locLbl.setStyle("-fx-text-fill: rgba(255,255,255,0.45); -fx-font-size: 12;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String prixStr = a.getPrix() != null ? a.getPrix().setScale(0, java.math.RoundingMode.HALF_UP) + " DT" : "Gratuit";
        Label prixLbl = new Label(prixStr);
        prixLbl.setStyle("-fx-text-fill: #50C878; -fx-font-size: 14; -fx-font-weight: 900;");

        footer.getChildren().addAll(locLbl, spacer, prixLbl);
        card.getChildren().addAll(header, desc, footer);

        // Fade-in on creation
        card.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(400), card);
        ft.setFromValue(0); ft.setToValue(1);
        ft.play();

        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle(card.getStyle().replace("rgba(80,200,120,0.22)", "rgba(80,200,120,0.5)"));
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.025); st.setToY(1.025); st.play();
        });
        card.setOnMouseExited(e -> {
            card.setStyle(card.getStyle().replace("rgba(80,200,120,0.5)", "rgba(80,200,120,0.22)"));
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });

        return card;
    }

    private String typeEmoji(String type) {
        if (type == null) return "\uD83C\uDF0D";
        String t = type.toLowerCase();
        if (t.contains("sport") || t.contains("outdoor")) return "⚽";
        if (t.contains("cultur") || t.contains("museum") || t.contains("heritage")) return "\uD83C\uDFDB\uFE0F";
        if (t.contains("food") || t.contains("gastro") || t.contains("degustation")) return "\uD83C\uDF7D\uFE0F";
        if (t.contains("music") || t.contains("concert") || t.contains("festival")) return "\uD83C\uDFB5";
        if (t.contains("art") || t.contains("peinture") || t.contains("craft")) return "\uD83C\uDFA8";
        if (t.contains("aventure") || t.contains("adventure")) return "\uD83E\uDDD7";
        if (t.contains("relax") || t.contains("spa") || t.contains("bien")) return "\uD83E\uDDD8";
        if (t.contains("nature") || t.contains("eco") || t.contains("randonnee")) return "\uD83C\uDF3F";
        return "\uD83C\uDF1F";
    }

    // ==================== PROPOSE POPUP ====================

    @FXML
    private void openProposePopup() {
        if (root != null) root.setEffect(new GaussianBlur(16));
        if (popupLocCombo != null) {
            popupLocCombo.getItems().setAll(
                "Tunis (Medina)", "Nabeul", "Sidi Bou Said",
                "Ghar El Melh", "Cap Bon", "Degustation Huile d'Olive",
                "Hammamet", "Sousse", "Sfax", "Djerba"
            );
        }
        clearPopupForm();
        if (modalOverlay != null) {
            modalOverlay.setVisible(true);
            modalOverlay.setManaged(true);
        }
    }

    @FXML
    private void closeProposePopup() {
        if (root != null) root.setEffect(null);
        if (modalOverlay != null) {
            modalOverlay.setVisible(false);
            modalOverlay.setManaged(false);
        }
    }

    @FXML
    private void consumePopupClick(MouseEvent e) {
        e.consume();
    }

    private void clearPopupForm() {
        if (popupName != null) popupName.clear();
        if (popupType != null) popupType.clear();
        if (popupPrix != null) popupPrix.clear();
        if (popupDesc != null) popupDesc.clear();
        if (popupLocCombo != null) popupLocCombo.setValue(null);
        if (popupFeedback != null) { popupFeedback.setVisible(false); popupFeedback.setManaged(false); popupFeedback.setText(""); }
        if (submitPopupBtn != null) submitPopupBtn.setDisable(false);
    }

    @FXML
    private void submitProposalPopup() {
        // Basic validation
        String name = popupName != null ? popupName.getText().trim() : "";
        String type = popupType != null ? popupType.getText().trim() : "";
        String prixStr = popupPrix != null ? popupPrix.getText().trim() : "";
        String loc = popupLocCombo != null ? popupLocCombo.getValue() : null;
        String desc = popupDesc != null ? popupDesc.getText().trim() : "";

        if (name.isEmpty() || type.isEmpty() || loc == null) {
            showPopupFeedback("Veuillez remplir le nom, le type et la localisation.", true);
            return;
        }

        BigDecimal prix;
        try {
            prix = prixStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(prixStr.replace(",", "."));
        } catch (NumberFormatException ex) {
            showPopupFeedback("Le prix doit être un nombre valide (ex: 25.00).", true);
            return;
        }

        if (submitPopupBtn != null) submitPopupBtn.setDisable(true);

        Activite activite = new Activite(name, desc, type, loc, prix, Activite.STATUS_PENDING);
        Thread t = new Thread(() -> {
            try {
                serviceActivite.ajouter(activite);
                Platform.runLater(() -> {
                    showPopupFeedback("✔ Proposition envoyée ! Elle sera validée par un administrateur.", false);
                    // Reload grid (pending wont show for users, but admin will see it)
                    loadDbActivities();
                    // Auto-close after brief delay
                    javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.seconds(2.2));
                    pause.setOnFinished(ev -> closeProposePopup());
                    pause.play();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showPopupFeedback("Erreur lors de l'envoi: " + ex.getMessage(), true);
                    if (submitPopupBtn != null) submitPopupBtn.setDisable(false);
                });
            }
        }, "Submit-Activity-Thread");
        t.setDaemon(true);
        t.start();
    }

    private void showPopupFeedback(String msg, boolean isError) {
        if (popupFeedback == null) return;
        popupFeedback.setText(msg);
        popupFeedback.setStyle(isError
            ? "-fx-font-size: 12; -fx-text-fill: #E74C3C;"
            : "-fx-font-size: 12; -fx-text-fill: #50C878;");
        popupFeedback.setVisible(true);
        popupFeedback.setManaged(true);
    }

    // ==================== DATA MODELS ====================

    // Data Models
    private static class CountryData {
        String name, code, capital, region, flagUrl, currency = "N/A", language = "N/A";
        long population;
        java.util.List<Activity> aiActivities = new java.util.ArrayList<>();
        CountryData(String name, String code) { this.name = name; this.code = code; }
    }
}
