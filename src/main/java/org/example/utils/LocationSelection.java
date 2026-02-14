package org.example.utils;

import org.example.entities.Location;

public final class LocationSelection {

    private static Location selected;

    private LocationSelection() {
    }

    public static Location get() {
        return selected;
    }

    public static void set(Location location) {
        selected = location;
    }

    public static void clear() {
        selected = null;
    }
}
