package com.example.gestionvol;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main JavaFX Application
 * Launches the GoVibe Flight Management System
 */
public class MainApp extends Application {
    
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("/main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1200, 700);
        
        // Apply CSS stylesheet
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        stage.setTitle("GoVibe Flight Management System");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(600);
        
        // Set application icon (if available)
        try {
            // stage.getIcons().add(new Image(getClass().getResourceAsStream("/icon.png")));
        } catch (Exception e) {
            System.out.println("Icon not found, using default");
        }
        
        stage.show();
        System.out.println("✅ GoVibe Flight Management System launched successfully!");
    }

    public static void main(String[] args) {
        launch();
    }
}

