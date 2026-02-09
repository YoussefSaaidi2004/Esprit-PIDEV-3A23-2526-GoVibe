module com.example.gestionvol {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens com.example.gestionvol to javafx.fxml;
    exports com.example.gestionvol;
}