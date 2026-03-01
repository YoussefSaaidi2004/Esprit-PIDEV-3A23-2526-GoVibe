package org.example.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Service for IP geolocation using the free ip-api.com REST API.
 * Returns country and city for a given IP address.
 */
public class GeoIPService {

    private static final String API_URL = "http://ip-api.com/json/";
    private static final int TIMEOUT_SECONDS = 5;

    private final HttpClient httpClient;

    public GeoIPService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    /**
     * Simple record holding geo information.
     */
    public static class GeoInfo {
        private final String country;
        private final String city;
        private final String ip;

        public GeoInfo(String country, String city, String ip) {
            this.country = country;
            this.city = city;
            this.ip = ip;
        }

        public String getCountry() { return country; }
        public String getCity() { return city; }
        public String getIp() { return ip; }

        @Override
        public String toString() {
            return "GeoInfo{country='" + country + "', city='" + city + "', ip='" + ip + "'}";
        }
    }

    /**
     * Queries ip-api.com for geolocation data of the given IP.
     * Returns a default GeoInfo for local/private IPs.
     */
    public GeoInfo getGeoInfo(String ip) {
        // Handle local/private IPs
        if (ip == null || ip.isEmpty() || ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1") || ip.startsWith("192.168.") || ip.startsWith("10.")) {
            System.out.println("🌍 [GeoIP] Local/private IP detected: " + ip + " → defaulting to local");
            return new GeoInfo("Local", "Local", ip != null ? ip : "unknown");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + ip))
                    .GET()
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                String status = json.has("status") ? json.get("status").getAsString() : "fail";

                if ("success".equals(status)) {
                    String country = json.has("country") ? json.get("country").getAsString() : "Unknown";
                    String city = json.has("city") ? json.get("city").getAsString() : "Unknown";
                    System.out.println("🌍 [GeoIP] " + ip + " → " + city + ", " + country);
                    return new GeoInfo(country, city, ip);
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ [GeoIP] Error querying ip-api.com: " + e.getMessage());
        }

        return new GeoInfo("Unknown", "Unknown", ip);
    }

    /**
     * Gets the external IP address of this machine.
     * Uses api.ipify.org as a simple IP lookup.
     */
    public String getExternalIP() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.ipify.org"))
                    .GET()
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String ip = response.body().trim();
                System.out.println("🌐 [GeoIP] External IP: " + ip);
                return ip;
            }
        } catch (Exception e) {
            System.err.println("⚠️ [GeoIP] Cannot determine external IP: " + e.getMessage());
        }
        return "127.0.0.1";
    }
}
