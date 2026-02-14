package org.example.mains;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/hotel.fxml")


        );

        Scene scene = new Scene(fxmlLoader.load(), 1000, 600);

        stage.setTitle("GoVibe - Gestion Hôtels");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}


