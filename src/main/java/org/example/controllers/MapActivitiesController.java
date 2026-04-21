package org.example.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import netscape.javascript.JSObject;

import com.google.gson.reflect.TypeToken;
import org.example.entities.Activity;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
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

    private WebEngine engine;
    private final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
    private Task<CountryData> currentTask;
    
    // API Keys (Centralized) - Fallback system with Gemini support
    private static final String GEMINI_KEY = "AIzaSyCEml6UPswBHnEZsqP4jbFgYMAp5ftIYRI";
    private static final String[] DEEPSEEK_KEYS = {
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
                String prompt = "Donne-moi 3 conseils de voyage uniques et secrets (hidden gems) pour " + country + ". " +
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
                    String geminiPrompt = "Donne-moi 3 conseils de voyage uniques et secrets (hidden gems) pour " + country + 
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

    // Data Models
    private static class CountryData {
        String name, code, capital, region, flagUrl, currency = "N/A", language = "N/A";
        long population;
        java.util.List<Activity> aiActivities = new java.util.ArrayList<>();
        CountryData(String name, String code) { this.name = name; this.code = code; }
    }
}
