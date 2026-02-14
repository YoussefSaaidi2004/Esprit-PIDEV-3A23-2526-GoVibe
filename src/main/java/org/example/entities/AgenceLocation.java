package org.example.entities;

public enum AgenceLocation {
    EUROPCAR_TUNIS_AEROPORT("Europcar", "Tunis", 36.8480, 10.2180),
    HERTZ_TUNIS_AEROPORT("Hertz", "Tunis", 36.8480, 10.2180),
    AVIS_CHARGUIA("Avis", "Tunis", 36.8525, 10.1950),
    AVIS_AEROPORT("Avis", "Tunis", 36.8480, 10.2180),
    BUDGET_AEROPORT("Budget", "Tunis", 36.8476, 10.2177),
    BUDGET_DOWNTOWN("Budget", "Tunis", 36.8530, 10.1935),
    WORLD_CAR("World Car", "Sousse", 35.8256, 10.6370),
    YASMINE_RENT_A_CAR("Yasmine Rent-A-Car", "Nabeul", 36.4518, 10.7313);

    private final String agence;
    private final String ville;
    private final double latitude;
    private final double longitude;

    AgenceLocation(String agence, String ville, double latitude, double longitude) {
        this.agence = agence;
        this.ville = ville;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getAgence() {
        return agence;
    }

    public String getVille() {
        return ville;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String toDisplay() {
        return agence + " (" + ville + ")";
    }

    @Override
    public String toString() {
        return toDisplay();
    }
}
