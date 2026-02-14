package org.example.mains;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/main-layout.fxml")
        );

        Scene scene = new Scene(fxmlLoader.load(), 1400, 800);

        stage.setTitle("GoVibe Travel - Gestion Hôtelière");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}


