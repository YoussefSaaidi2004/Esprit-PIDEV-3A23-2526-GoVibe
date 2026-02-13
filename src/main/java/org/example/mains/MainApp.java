package org.example.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/LoginView.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        // styles.css might be optional for login, check if needed
        // scene.getStylesheets().add(getClass().getResource("/org/example/styles.css").toExternalForm());

        primaryStage.setTitle("GoVibe — Connexion");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
