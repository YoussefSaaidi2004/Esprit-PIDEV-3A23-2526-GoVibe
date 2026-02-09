package org.example.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/VoitureListView.fxml"));
        Scene scene = new Scene(root, 1920, 1080);
        stage.setTitle("GoVibe - Voitures");
        stage.setScene(scene);
        stage.setMinWidth(1920);
        stage.setMinHeight(1080);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}