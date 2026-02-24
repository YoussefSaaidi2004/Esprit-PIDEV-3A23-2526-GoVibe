package org.example.services;

import java.util.concurrent.CompletableFuture;

/**
 * Provides a city-specific fallback image URL when Wikipedia has no image.
 * Uses picsum.photos with a deterministic city-name seed — always works, no API key required.
 */
public class DenizenImageService {

    /**
     * Returns a picsum.photos URL seeded by city name.
     * Same city always gets the same consistent random image.
     */
    public CompletableFuture<String> getImageUrl(String city) {
        // Strip non-alphanumeric to create a clean seed
        String seed = city.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (seed.isBlank()) seed = "travel";
        String url = "https://picsum.photos/seed/" + seed + "/800/400";
        return CompletableFuture.completedFuture(url);
    }
}
