package org.example.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Routing service for distance calculations and route info.
 * Uses Haversine formula for straight-line distance calculations.
 * When a GraphHopper OSM file is available, can be extended to use full road routing.
 * 
 * This service avoids direct GraphHopper imports to prevent JPMS module conflicts.
 */
public class RoutingService {
    private boolean initialized = false;
    private String initError = null;

    /**
     * Structured route information returned by getRouteInfo().
     */
    public static class RouteInfo {
        public final double distanceKm;
        public final long timeMinutes;
        public final String distanceText;
        public final String timeText;
        public final boolean isEstimate; // true if Haversine fallback was used

        public RouteInfo(double distanceKm, long timeMinutes, boolean isEstimate) {
            this.distanceKm = distanceKm;
            this.timeMinutes = timeMinutes;
            this.isEstimate = isEstimate;
            this.distanceText = String.format(Locale.ROOT, "%.1f km", distanceKm);

            if (timeMinutes < 60) {
                this.timeText = timeMinutes + " min";
            } else {
                long hours = timeMinutes / 60;
                long mins = timeMinutes % 60;
                this.timeText = hours + "h" + (mins > 0 ? String.format("%02d", mins) : "");
            }
        }

        @Override
        public String toString() {
            return distanceText + " · ~" + timeText + (isEstimate ? " (est.)" : "");
        }
    }

    /**
     * Create a RoutingService in Haversine-only mode (no OSM file needed).
     */
    public RoutingService() {
        this.initialized = false;
        this.initError = "Using Haversine distance estimation (no OSM file configured).";
        System.out.println("[RoutingService] " + initError);
    }

    /**
     * Initialize with an OSM file path for future GraphHopper integration.
     * Currently logs a warning and falls back to Haversine.
     */
    public RoutingService(String osmPath) {
        this(); // Use Haversine mode
        System.out.println("[RoutingService] OSM file configured: " + osmPath
                + " — GraphHopper integration available for future use. Using Haversine for now.");
    }

    /**
     * Get formatted route info (distance + ETA).
     * Uses Haversine straight-line distance with estimated driving time.
     */
    public RouteInfo getRouteInfo(double fromLat, double fromLon, double toLat, double toLon) {
        double distKm = haversineKm(fromLat, fromLon, toLat, toLon);
        // Estimate driving time assuming ~50 km/h average urban speed
        long estimatedMins = Math.max(1, Math.round(distKm / 50.0 * 60));
        return new RouteInfo(distKm, estimatedMins, true);
    }

    /**
     * Get route polyline as a list of [lat, lon] pairs for map display.
     * Without full routing, returns a straight line between origin and destination.
     */
    public List<double[]> getRoutePolyline(double fromLat, double fromLon, double toLat, double toLon) {
        List<double[]> points = new ArrayList<>();
        // Straight line between origin and destination
        points.add(new double[]{fromLat, fromLon});
        // Add an intermediate point for a slightly curved line effect
        double midLat = (fromLat + toLat) / 2.0;
        double midLon = (fromLon + toLon) / 2.0;
        // Add small offset for visual curve
        double offset = Math.abs(fromLat - toLat) * 0.05;
        points.add(new double[]{midLat + offset, midLon});
        points.add(new double[]{toLat, toLon});
        return points;
    }

    /**
     * Check if full routing (GraphHopper) is initialized and ready.
     */
    public boolean isAvailable() {
        return initialized;
    }

    /**
     * Get the initialization info message.
     */
    public String getInitError() {
        return initError;
    }

    // ==================== HAVERSINE FORMULA ====================

    /**
     * Calculate straight-line distance in kilometers between two GPS points.
     */
    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
