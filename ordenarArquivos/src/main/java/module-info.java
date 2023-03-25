module ordenarArquivos {
	exports ordenarArquivos;
	exports ordenarArquivos.controller;
	exports ordenarArquivos.model;
	exports ordenarArquivos.logback;

	requires transitive javafx.graphics;
	requires javafx.base;
	requires javafx.fxml;
	requires transitive com.jfoenix;
	requires transitive javafx.controls;
	requires java.desktop;
	requires org.flywaydb.core;
	requires java.sql;
	requires org.xerial.sqlitejdbc;
	requires java.logging;
	requires org.slf4j;
	requires logback.classic;
	requires logback.core;

	opens ordenarArquivos.controller to javafx.fxml, javafx.graphics;
	opens ordenarArquivos.model to javafx.base;
}