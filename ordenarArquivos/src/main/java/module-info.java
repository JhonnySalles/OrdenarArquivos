module ordenarArquivos {
	exports ordenarArquivos;
	exports ordenarArquivos.controller;
	exports ordenarArquivos.model;

	requires transitive javafx.graphics;
	requires javafx.base;
	requires javafx.fxml;
	requires com.jfoenix;
	requires javafx.controls;

	opens ordenarArquivos.controller to javafx.fxml;
	opens ordenarArquivos.model to javafx.base;
}