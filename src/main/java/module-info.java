module org.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;
    requires jbcrypt;
    requires java.mail;

    opens org.example.controllers to javafx.fxml;
    opens org.example.entities to javafx.base;
    opens org.example.mains to javafx.graphics;

    exports org.example.mains;
    exports org.example.entities;
    exports org.example.services;
    exports org.example.utils;
    exports org.example.controllers;
}
