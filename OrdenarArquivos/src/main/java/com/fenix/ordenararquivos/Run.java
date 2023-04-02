package com.fenix.ordenararquivos;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import com.fenix.ordenararquivos.controller.TelaInicialController;

public class Run extends Application {
	
	private static Scene mainScene;
	private static TelaInicialController mainController;

	public void start(Stage primaryStage) {
		try {
			// Classe inicial
			FXMLLoader loader = new FXMLLoader(TelaInicialController.getFxmlLocate());
			AnchorPane scPnTelaPrincipal = loader.load();
			mainController = loader.getController();
			
			mainScene = new Scene(scPnTelaPrincipal); // Carrega a scena
			mainScene.setFill(Color.BLACK);
			mainController.configurarAtalhos(mainScene);

			primaryStage.setScene(mainScene); // Seta a cena principal
			primaryStage.setTitle("Ordena Arquivos");
			primaryStage.getIcons()
			.add(new Image(Run.class.getResourceAsStream(TelaInicialController.getIconLocate())));
			primaryStage.initStyle(StageStyle.DECORATED);
			//primaryStage.setMaximized(true);
			primaryStage.setMinWidth(700);
			primaryStage.setMinHeight(600);
			
			primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
				@Override
				public void handle(WindowEvent arg0) {
					System.exit(0);
				}
			});
			primaryStage.show(); // Mostra a tela.

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static TelaInicialController getMainController() {
		return mainController;
	}

	public static void main(String[] args) {
		launch(args);
	}
}
