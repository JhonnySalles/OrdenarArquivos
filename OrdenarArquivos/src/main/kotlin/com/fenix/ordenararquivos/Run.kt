package com.fenix.ordenararquivos

import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.database.DataBase
import javafx.application.Application
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.layout.AnchorPane
import javafx.scene.paint.Color
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle
import kotlin.system.exitProcess

class Run : Application() {

    override fun start(primaryStage: Stage) {
        try {
            // Classe inicial
            val loader = FXMLLoader(TelaInicialController.fxmlLocate)
            val scPnTelaPrincipal = loader.load<AnchorPane>()
            mainController = loader.getController()
            mainScene = Scene(scPnTelaPrincipal) // Carrega a scena
            mainScene.fill = Color.BLACK
            mainController.configurarAtalhos(mainScene)
            primaryStage.scene = mainScene // Seta a cena principal
            primaryStage.title = "Ordena Arquivos"
            primaryStage.icons.add(Image(Run::class.java.getResourceAsStream(TelaInicialController.iconLocate)))
            primaryStage.initStyle(StageStyle.DECORATED)
            //primaryStage.setMaximized(true);

            if (Screen.getScreens()[0].bounds.width > 720) {
                primaryStage.minWidth = 800.0
                primaryStage.minHeight = 700.0
                primaryStage.width = 800.0
                primaryStage.height = 900.0
            } else {
                primaryStage.minWidth = 700.0
                primaryStage.minHeight = 600.0
            }

            primaryStage.onCloseRequest = EventHandler {
                DataBase.closeConnection()
                exitProcess(0)
            }
            primaryStage.show() // Mostra a tela.
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun start(args: Array<String>) {
        launch(*args)
    }

    companion object {
        private lateinit var mainScene: Scene
        lateinit var mainController: TelaInicialController
    }
}