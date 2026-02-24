package org.example.controllers;

import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.example.entities.Checkout;
import org.example.services.DenizenImageService;
import org.example.services.PaymentCallbackServer;
import org.example.services.RapidApiCityService;
import org.example.services.StripeService;
import org.example.dao.CheckoutDAO;
import org.example.dao.FlightDAO;

import java.math.BigDecimal;
import java.net.URI;
import java.awt.Desktop;
import java.util.Locale;

public class CityPaymentModalController {

    @FXML private ImageView cityImageView;
    @FXML private Label cityTitleLabel;
    @FXML private Label countrySubtitleLabel;
    @FXML private Label populationLabel;
    @FXML private Label timezoneLabel;
    @FXML private Label currencyLabel;
    @FXML private Label capitalLabel;
    @FXML private Label placeDescriptionArea;   // Now a Label (wrapping) — not a TextArea
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private StackPane rootPane;

    private final RapidApiCityService cityService = new RapidApiCityService();
    private final DenizenImageService imageService = new DenizenImageService();
    private final StripeService stripeService = StripeService.getInstance();
    private final CheckoutDAO checkoutDAO = new CheckoutDAO();
    private final FlightDAO flightDAO = new FlightDAO();
    private final PaymentCallbackServer callbackServer = new PaymentCallbackServer();

    private Checkout checkout;
    private org.example.entities.Flight flight;
    private Runnable onSuccessCallback;
    private String successUrl;
    private String cancelUrl;
    private String stripeCheckoutUrl;
    private boolean stripeUnavailable = false;

    public void setData(Checkout checkout, org.example.entities.Flight flight, Runnable onSuccess) {
        this.checkout = checkout;
        this.flight = flight != null ? flight : resolveFlight(checkout);
        this.onSuccessCallback = onSuccess;
        loadData();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  DATA LOAD
    // ────────────────────────────────────────────────────────────────────────

    private void loadData() {
        loadingIndicator.setVisible(true);

        if (checkout == null || flight == null) {
            cityTitleLabel.setText("Flight details unavailable");
            countrySubtitleLabel.setText("Unable to load booking context");
            loadingIndicator.setVisible(false);
            return;
        }

        // Extract destination city from route (e.g. "Tunis → Paris" → "Paris")
        String city = safeText(flight.getDestination(), "Unknown");
        if (city.contains(" -> ")) {
            city = city.split(" -> ")[1].trim();
        } else if (city.contains("→")) {
            city = city.split("→")[city.split("→").length - 1].trim();
        }

        final String cityName = city;
        cityTitleLabel.setText(cityName);
        placeDescriptionArea.setText("Loading destination details for " + cityName + "…");

        // Async — Wikipedia + Nominatim + RestCountries
        cityService.getCityDetailsByName(cityName)
            .thenAccept(data -> Platform.runLater(() -> {
                loadingIndicator.setVisible(false);

                if (data == null) {
                    placeDescriptionArea.setText("Discover " + cityName + " — complete your payment to confirm this trip.");
                    return;
                }

                // Country subtitle
                String country  = str(data, "country",     "");
                String wikiDesc = str(data, "description", "");
                if (!country.isEmpty()) {
                    countrySubtitleLabel.setText(wikiDesc.isEmpty() ? country : country + " — " + wikiDesc);
                } else if (!wikiDesc.isEmpty()) {
                    countrySubtitleLabel.setText(wikiDesc);
                }

                // Stats cards
                long   population = data.has("population") ? data.get("population").getAsLong() : 0L;
                String timezone   = str(data, "timezone", "");
                String currency   = str(data, "currency", "");
                String capital    = str(data, "capital",  "");

                if (population > 0)
                    populationLabel.setText(String.format(Locale.ROOT, "%,d", population));
                if (!timezone.isEmpty()) timezoneLabel.setText(timezone);
                if (!currency.isEmpty()) currencyLabel.setText(currency);
                if (!capital.isEmpty())  capitalLabel.setText(capital);

                // Description — Wikipedia extract (max ~1 400 chars)
                String extract = str(data, "extract", "");
                if (!extract.isEmpty()) {
                    placeDescriptionArea.setText(
                            extract.length() > 1400 ? extract.substring(0, 1400) + "…" : extract);
                } else {
                    placeDescriptionArea.setText(
                            buildPlaceDescription(cityName, country, population > 0 ? population : null,
                                    timezone, currency, capital));
                }

                // Image: Wikipedia thumbnail first (always JPEG), Picsum fallback
                String imageUrl = str(data, "imageUrl", "");
                if (!imageUrl.isEmpty()) {
                    Image img = new Image(imageUrl, true);
                    img.errorProperty().addListener((obs, wasErr, isErr) -> {
                        if (isErr) loadFallbackImage(cityName);
                    });
                    cityImageView.setImage(img);
                } else {
                    loadFallbackImage(cityName);
                }
            }))
            .exceptionally(ex -> {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    placeDescriptionArea.setText("Discover " + cityName + " — complete your payment to confirm your booking.");
                    loadFallbackImage(cityName);
                });
                return null;
            });

        // (payment handled in booking form — not here)
    }

    private void loadFallbackImage(String cityName) {
        imageService.getImageUrl(cityName).thenAccept(url ->
            Platform.runLater(() -> cityImageView.setImage(new Image(url, true)))
        );
    }

    // ────────────────────────────────────────────────────────────────────────
    //  PAYMENT  (handled by booking form, not this modal)
    // ────────────────────────────────────────────────────────────────────────

    @FXML
    public void handlePaymentSuccess() {
        checkout.setStatusReservation("CONFIRMED");
        checkoutDAO.update(checkout);
        checkoutDAO.updateStatus(checkout.getCheckoutId(), "CONFIRMED");
        callbackServer.stop();
        Platform.runLater(() -> {
            if (onSuccessCallback != null) onSuccessCallback.run();
            handleClose();
        });
    }

    @FXML
    public void handleClose() {
        callbackServer.stop();
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.close();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ────────────────────────────────────────────────────────────────────────

    private org.example.entities.Flight resolveFlight(Checkout c) {
        if (c == null || c.getFlightId() == null) return null;
        try { return flightDAO.findById(c.getFlightId()); } catch (Exception e) { return null; }
    }

    private String safeText(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v;
    }

    private static String str(JsonObject obj, String key, String def) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return def;
        String v = obj.get(key).getAsString();
        return v.isBlank() ? def : v;
    }

    private String buildPlaceDescription(String city, String country, Long population,
                                          String timezone, String currency, String capital) {
        StringBuilder sb = new StringBuilder("Discover ").append(safeText(city, "this destination")).append(".");
        if (!country.isEmpty()) sb.append(" Located in ").append(country).append(".");
        if (capital  != null && !capital.isEmpty())  sb.append(" Capital: ").append(capital).append(".");
        if (timezone != null && !timezone.isEmpty()) sb.append(" Timezone: ").append(timezone).append(".");
        if (currency != null && !currency.isEmpty()) sb.append(" Currency: ").append(currency).append(".");
        if (population != null && population > 0)
            sb.append(" Population: ").append(String.format(Locale.ROOT, "%,d", population)).append(".");
        sb.append(" Complete your payment below to confirm this booking.");
        return sb.toString();
    }
}
