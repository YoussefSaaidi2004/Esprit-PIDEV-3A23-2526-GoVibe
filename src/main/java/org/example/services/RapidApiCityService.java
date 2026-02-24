package org.example.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * City information service using free public APIs (no API keys required):
 *  - Wikipedia REST API  → city description + image
 *  - Nominatim (OSM)    → geocoding to resolve country code
 *  - RestCountries      → country details (capital, currency, timezone, population)
 */
public class RapidApiCityService {

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Returns a JsonObject with keys:
     *   extract, description, imageUrl, country, capital, currency, timezone, population
     */
    public CompletableFuture<JsonObject> getCityDetailsByName(String cityName) {
        CompletableFuture<JsonObject> wikiF = fetchWikipedia(cityName);
        CompletableFuture<String> codeF = fetchCountryCode(cityName);

        return codeF.thenCompose(countryCode -> {
            if (countryCode != null && !countryCode.isBlank()) {
                // Nominatim gave us a country code → fetch RestCountries directly
                return wikiF.thenCombine(fetchRestCountries(countryCode),
                        (wiki, rc) -> merge(wiki, rc));
            } else {
                // Nominatim failed (rate-limited) → try to extract country from Wikipedia description
                return wikiF.thenCompose(wiki -> {
                    String countryName = wiki != null ? guessCountryFromWiki(wiki) : null;
                    if (countryName != null) {
                        return fetchRestCountriesByName(countryName)
                                .thenApply(rc -> merge(wiki, rc));
                    }
                    return CompletableFuture.completedFuture(merge(wiki, null));
                });
            }
        }).exceptionally(ex -> {
            System.err.println("CityService error: " + ex.getMessage());
            return new JsonObject();
        });
    }

    /** Kept for backward-compat */
    public CompletableFuture<JsonObject> getCountryDetails(String countryCode) {
        return fetchRestCountries(countryCode);
    }

    // ── Wikipedia ────────────────────────────────────────────────────────────

    private CompletableFuture<JsonObject> fetchWikipedia(String cityName) {
        String encoded = URLEncoder.encode(cityName, StandardCharsets.UTF_8).replace("+", "_");
        String url = "https://en.wikipedia.org/api/rest_v1/page/summary/" + encoded;
        return getJson(url).thenApply(el -> el != null && el.isJsonObject() ? el.getAsJsonObject() : null);
    }

    // ── Nominatim (OpenStreetMap geocoding) ──────────────────────────────────

    private CompletableFuture<String> fetchCountryCode(String cityName) {
        String encoded = URLEncoder.encode(cityName, StandardCharsets.UTF_8);
        String url = "https://nominatim.openstreetmap.org/search?q=" + encoded
                + "&format=json&limit=1&addressdetails=1";
        return getJson(url).thenApply(el -> {
            if (el == null || !el.isJsonArray()) return null;
            JsonArray arr = el.getAsJsonArray();
            if (arr.size() == 0) return null;
            JsonObject first = arr.get(0).getAsJsonObject();
            if (first.has("address") && first.getAsJsonObject("address").has("country_code")) {
                return first.getAsJsonObject("address").get("country_code").getAsString();
            }
            return null;
        }).exceptionally(ex -> null);
    }

    // ── RestCountries ─────────────────────────────────────────────────────────

    private CompletableFuture<JsonObject> fetchRestCountries(String countryCode) {
        String url = "https://restcountries.com/v3.1/alpha/" + countryCode.toUpperCase();
        return getJson(url).thenApply(el -> {
            if (el == null) return null;
            if (el.isJsonArray()) {
                JsonArray arr = el.getAsJsonArray();
                return arr.size() > 0 ? parseCountry(arr.get(0).getAsJsonObject()) : null;
            }
            if (el.isJsonObject()) return parseCountry(el.getAsJsonObject());
            return null;
        }).exceptionally(ex -> {
            System.err.println("RestCountries error: " + ex.getMessage());
            return null;
        });
    }

    /** Search RestCountries by country name (e.g. "France") when we only have the name. */
    private CompletableFuture<JsonObject> fetchRestCountriesByName(String countryName) {
        String encoded = URLEncoder.encode(countryName, StandardCharsets.UTF_8);
        String url = "https://restcountries.com/v3.1/name/" + encoded + "?fullText=true";
        return getJson(url).thenApply(el -> {
            if (el == null) return null;
            if (el.isJsonArray()) {
                JsonArray arr = el.getAsJsonArray();
                return arr.size() > 0 ? parseCountry(arr.get(0).getAsJsonObject()) : null;
            }
            return null;
        }).exceptionally(ex -> {
            System.err.println("RestCountries-by-name error: " + ex.getMessage());
            return null;
        });
    }

    /**
     * Guess the country name from the Wikipedia short description.
     * e.g. "Capital and largest city of France" → "France"
     *      "City in the United Kingdom" → "United Kingdom"
     */
    private String guessCountryFromWiki(JsonObject wiki) {
        if (wiki == null) return null;
        String desc = str(wiki, "description", "");
        if (desc.isEmpty()) return null;

        // Look for " of <Country>" or " in <Country>" patterns
        for (String sep : new String[]{" of ", " in the ", " in "}) {
            int idx = desc.toLowerCase().lastIndexOf(sep);
            if (idx >= 0) {
                String candidate = desc.substring(idx + sep.length()).trim()
                        .replaceAll(",.*", "").trim();
                // Must start with uppercase and be reasonably short
                if (!candidate.isEmpty() && Character.isUpperCase(candidate.charAt(0))
                        && candidate.length() < 40) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private JsonObject parseCountry(JsonObject c) {
        JsonObject r = new JsonObject();
        // name
        if (c.has("name") && c.getAsJsonObject("name").has("common")) {
            r.addProperty("country", c.getAsJsonObject("name").get("common").getAsString());
        }
        // capital
        if (c.has("capital") && c.get("capital").isJsonArray()
                && c.getAsJsonArray("capital").size() > 0) {
            r.addProperty("capital", c.getAsJsonArray("capital").get(0).getAsString());
        }
        // timezone
        if (c.has("timezones") && c.get("timezones").isJsonArray()
                && c.getAsJsonArray("timezones").size() > 0) {
            r.addProperty("timezone", c.getAsJsonArray("timezones").get(0).getAsString());
        }
        // population
        if (c.has("population")) {
            r.addProperty("population", c.get("population").getAsLong());
        }
        // currency (first entry)
        if (c.has("currencies") && c.get("currencies").isJsonObject()) {
            JsonObject currencies = c.getAsJsonObject("currencies");
            for (String code : currencies.keySet()) {
                JsonObject cur = currencies.getAsJsonObject(code);
                String symbol = cur.has("symbol") ? cur.get("symbol").getAsString() : "";
                r.addProperty("currency", code + (symbol.isEmpty() ? "" : " (" + symbol + ")"));
                break;
            }
        }
        return r;
    }

    // ── Merge Wikipedia + RestCountries ──────────────────────────────────────

    private JsonObject merge(JsonObject wiki, JsonObject rc) {
        JsonObject result = new JsonObject();

        if (wiki != null) {
            result.addProperty("extract", str(wiki, "extract", ""));
            result.addProperty("description", str(wiki, "description", ""));

            // Prefer thumbnail (always JPEG/PNG). Only use originalimage if not SVG.
            String imgUrl = "";
            if (wiki.has("thumbnail") && wiki.get("thumbnail").isJsonObject()) {
                imgUrl = str(wiki.getAsJsonObject("thumbnail"), "source", "");
            }
            if (imgUrl.isEmpty() && wiki.has("originalimage") && wiki.get("originalimage").isJsonObject()) {
                String orig = str(wiki.getAsJsonObject("originalimage"), "source", "");
                if (!orig.toLowerCase().endsWith(".svg")) imgUrl = orig;
            }
            result.addProperty("imageUrl", imgUrl);
        } else {
            result.addProperty("extract", "");
            result.addProperty("description", "");
            result.addProperty("imageUrl", "");
        }

        if (rc != null) {
            result.addProperty("country", str(rc, "country", ""));
            result.addProperty("capital", str(rc, "capital", ""));
            result.addProperty("currency", str(rc, "currency", ""));
            result.addProperty("timezone", str(rc, "timezone", ""));
            result.addProperty("population",
                    rc.has("population") ? rc.get("population").getAsLong() : 0L);
        } else {
            result.addProperty("country", "");
            result.addProperty("capital", "");
            result.addProperty("currency", "");
            result.addProperty("timezone", "");
            result.addProperty("population", 0L);
        }
        return result;
    }

    // ── HTTP helper ───────────────────────────────────────────────────────────

    private CompletableFuture<JsonElement> getJson(String url) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "GoVibe-App/1.0 (contact@example.com)")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    if (resp.statusCode() == 200) {
                        return JsonParser.parseString(resp.body());
                    }
                    System.err.println("HTTP " + resp.statusCode() + " for " + url);
                    return null;
                }).exceptionally(ex -> {
                    System.err.println("Request failed for " + url + ": " + ex.getMessage());
                    return null;
                });
    }

    private static String str(JsonObject obj, String key, String def) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return def;
        return obj.get(key).getAsString();
    }
}
