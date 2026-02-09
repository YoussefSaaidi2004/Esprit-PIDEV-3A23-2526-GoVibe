module com.example.gestionvol {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.example.gestionvol to javafx.fxml;
    opens com.example.gestionvol.controller to javafx.fxml;
    opens com.example.gestionvol.entities to javafx.base;
    
    exports com.example.gestionvol;
    exports com.example.gestionvol.controller;
    exports com.example.gestionvol.entities;
}