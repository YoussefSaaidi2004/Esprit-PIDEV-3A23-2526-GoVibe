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
    requires java.net.http;
    requires javafx.web;
    requires com.google.gson;
    requires jdk.jsobject;
    requires stripe.java;
    requires jdk.httpserver;
    requires webcam.capture;
    requires vosk;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;

    // org.example packages
    opens org.example.controllers to javafx.fxml, jdk.jsobject;
    opens org.example.entities to javafx.base, com.google.gson;
    opens org.example.mains to javafx.graphics;
    opens org.example.dao to javafx.base;
    opens org.example.config to javafx.base;
    opens org.example.assistant to javafx.fxml;

    exports org.example.mains;
    exports org.example.entities;
    exports org.example.services;
    exports org.example.utils;
    exports org.example.controllers;
    exports org.example.dao;
    exports org.example.config;
    exports org.example.assistant;

    // tn.esprit packages (Forum / Poste / Membre management)
    opens tn.esprit.controllers to javafx.fxml;
    opens tn.esprit.entities to javafx.base, com.google.gson;
    opens tn.esprit.mains to javafx.graphics;

    exports tn.esprit.controllers;
    exports tn.esprit.entities;
    exports tn.esprit.services;
    exports tn.esprit.utils;
    exports tn.esprit.mains;
}
