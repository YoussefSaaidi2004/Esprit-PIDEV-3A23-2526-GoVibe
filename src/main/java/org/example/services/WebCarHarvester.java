package org.example.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.example.entities.Voiture;
import org.example.utils.LocalDateTimeAdapter;
import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de collecte de donnees web pour les voitures.
 * Simule le moissonnage (harvesting) et stocke les resultats dans JSON.
 */
public class WebCarHarvester {

    private static final String JSON_PATH = "src/main/resources/data/web_cars.json";
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .setPrettyPrinting()
            .create();

    /**
     * Charge les voitures depuis le fichier JSON.
     */
    public List<Voiture> loadWebCars() {
        File file = new File(JSON_PATH);
        if (!file.exists()) return new ArrayList<>();

        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<Voiture>>(){}.getType();
            return gson.fromJson(reader, listType);
        } catch (IOException e) {
            System.err.println("[Harvester] Error loading JSON: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Simule une recherche web et met a jour le JSON.
     */
    public void harvestFromWeb() {
        List<Voiture> current = loadWebCars();
        
        // Simulation d'un nouvel ajout trouve sur le web
        Voiture newCar = new Voiture();
        newCar.setMarque("Audi");
        newCar.setModele("Q5");
        newCar.setPrixJour(110.0);
        newCar.setDescription("Expertly maintained SUV from WebRental.tn");
        newCar.setAdresseAgence("Lac 2, Tunis");
        newCar.setLatitude(36.8333);
        newCar.setLongitude(10.2333);
        
        current.add(newCar);

        try (Writer writer = new FileWriter(JSON_PATH)) {
            gson.toJson(current, writer);
            System.out.println("[Harvester] Harvested 1 new car successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
