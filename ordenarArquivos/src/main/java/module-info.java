module ordenarArquivos {
	exports ordenarArquivos;
	exports ordenarArquivos.controller;
	exports ordenarArquivos.model;

	requires transitive javafx.graphics;
	requires javafx.base;
	requires javafx.fxml;
	requires transitive com.jfoenix;
	requires transitive javafx.controls;

	opens ordenarArquivos.controller to javafx.fxml, javafx.graphics;
	opens ordenarArquivos.model to javafx.base;
}