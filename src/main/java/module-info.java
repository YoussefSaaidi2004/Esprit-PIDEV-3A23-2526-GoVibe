module org.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;
    requires java.desktop;
    requires jbcrypt;
    requires java.mail;
    requires org.apache.pdfbox;
    requires org.apache.poi.ooxml;
    requires com.google.zxing;
    requires com.google.zxing.javase;

    opens org.example.controllers to javafx.fxml;
    opens org.example.entities to javafx.base;
    opens org.example.mains to javafx.graphics;
    opens org.example.dao to javafx.base;
    opens org.example.config to javafx.base;

    exports org.example.mains;
    exports org.example.entities;
    exports org.example.services;
    exports org.example.utils;
    exports org.example.controllers;
    exports org.example.dao;
    exports org.example.config;
}
