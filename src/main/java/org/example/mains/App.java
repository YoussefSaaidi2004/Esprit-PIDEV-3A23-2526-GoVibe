package org.example.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        // ✅ Charge DashboardSession.fxml depuis src/main/resources
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/Dashboard.fxml"));
        Scene scene = new Scene(loader.load(), 1100, 700);

        stage.setTitle("GoVibe - Dashboard Sessions");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
