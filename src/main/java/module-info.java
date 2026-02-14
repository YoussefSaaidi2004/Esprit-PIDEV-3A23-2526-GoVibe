module com.example.gestionvol {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.example.gestionvol to javafx.fxml;
    opens com.example.gestionvol.controller to javafx.fxml;
    opens com.example.gestionvol.controller.admin to javafx.fxml;
    opens com.example.gestionvol.controller.user to javafx.fxml;
    opens com.example.gestionvol.entities to javafx.base;
    opens com.example.gestionvol.service to javafx.fxml;
    opens com.example.gestionvol.util to javafx.fxml;
    
    exports com.example.gestionvol;
    exports com.example.gestionvol.controller;
    exports com.example.gestionvol.controller.admin;
    exports com.example.gestionvol.controller.user;
    exports com.example.gestionvol.entities;
    exports com.example.gestionvol.service;
    exports com.example.gestionvol.util;
}