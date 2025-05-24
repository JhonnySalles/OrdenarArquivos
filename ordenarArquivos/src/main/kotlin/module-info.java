module com.fenix.ordenararquivos {
    exports com.fenix.ordenararquivos;
    exports com.fenix.ordenararquivos.controller;
    exports com.fenix.ordenararquivos.model.entities;
    exports com.fenix.ordenararquivos.logback;

    requires transitive javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires javafx.base;
    requires transitive com.jfoenix;
    requires java.desktop;
    requires org.flywaydb.core;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires java.logging;
    requires org.slf4j;
    requires logback.classic;
    requires logback.core;
    requires kotlin.stdlib;
    requires net.kurobako.gesturefx;
    requires firebase.admin;
    requires com.google.auth.oauth2;
    requires com.google.auth;
    requires google.cloud.core;
    requires google.cloud.firestore;
    requires com.google.api.apicommon;
    requires tess4j;
    requires libsvm;
    requires colt;
    requires org.apache.commons.lang3;
    requires opencv;
    requires io.grpc.internal;
    requires io.grpc;
    requires jakarta.xml.bind;
    requires com.google.gson;
    requires AnimateFX;
    requires org.controlsfx.controls;
    requires Mal4J;
    requires java.net.http;
    requires okhttp;
    requires org.json;
    requires org.jsoup;

    opens com.fenix.ordenararquivos.controller to javafx.fxml, javafx.graphics;
    opens com.fenix.ordenararquivos.model.firebase to javafx.base, google.cloud.firestore, com.google.gson;
    opens com.fenix.ordenararquivos.model.entities.comicinfo to java.xml.bind, jakarta.xml.bind, javafx.base;
    opens com.fenix.ordenararquivos.model.entities.comet to java.xml.bind, jakarta.xml.bind, javafx.base;

    opens db.migration;
}