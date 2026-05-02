package com.fenix.ordenararquivos.notification

import com.fenix.ordenararquivos.controller.PopupAlertaController
import com.fenix.ordenararquivos.model.enums.Alerta
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.AnchorPane
import javafx.stage.Modality
import javafx.stage.Stage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/**
 *
 *
 * Classe responsável por apresentar alertas em janela, podendo ser um alerta
 * com borda (tela do windows) ou sem borda.
 *
 *
 * @author Jhonny de Salles Noschang
 */
object AlertasModal {
    private val mLOG: Logger = LoggerFactory.getLogger(AlertasModal::class.java)


    fun aviso(titulo: String, texto: String) {
        mostrar(titulo, texto, Alerta.AVISO)
    }

    fun alerta(titulo: String, texto: String) {
        mostrar(titulo, texto, Alerta.ALERTA)
    }

    fun erro(titulo: String, texto: String) {
        mostrar(titulo, texto, Alerta.ERRO)
    }

    private fun mostrar(titulo: String, texto: String, tipo: Alerta) {
        try {
            val loader = FXMLLoader(PopupAlertaController.fxmlLocate)
            val scPnTelaPrincipal: AnchorPane = loader.load()

            val controller = loader.getController() as PopupAlertaController
            controller.setTexto(titulo, texto)
            val icone = when (tipo) {
                Alerta.AVISO -> {
                    controller.aviso()
                    PopupAlertaController.IMG_AVISO
                }
                Alerta.ALERTA -> {
                    controller.alerta()
                    PopupAlertaController.IMG_ALERTA
                }
                Alerta.ERRO -> {
                    controller.erro()
                    PopupAlertaController.IMG_ERRO
                }
            }
            controller.setVisivel(true, imagem = true)

            val tela = Scene(scPnTelaPrincipal)
            val stageTela = Stage()
            stageTela.scene = tela
            controller.setEventosBotoes { stageTela.close() }

            stageTela.title = titulo
            stageTela.icons.add(icone)
            stageTela.initModality(Modality.APPLICATION_MODAL)

            if (Platform.isFxApplicationThread()) {
                stageTela.showAndWait()
            } else {
                Platform.runLater { stageTela.showAndWait() }
            }
        } catch (e: Exception) {
            println("Erro ao tentar carregar o alerta.")
            mLOG.error(e.message, e)
        }
    }

    // Mantendo para compatibilidade caso necessário, mas agora redireciona para alerta
    fun telaAlerta(titulo: String, texto: String) {
        alerta(titulo, texto)
    }

    fun showTrayMessage(title: String, message: String) {
        try {
            val tray: SystemTray = SystemTray.getSystemTray()
            val image: BufferedImage = ImageIO.read(AlertasModal::class.java.getResource("/images/alert/icoConfirma_48.png"))
            val trayIcon = TrayIcon(image, "Ordenar Arquivos")
            trayIcon.isImageAutoSize = true
            trayIcon.toolTip = "Ordenar Arquivos"
            tray.add(trayIcon)
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO)
            tray.remove(trayIcon)
        } catch (exp: Exception) {
            exp.printStackTrace()
        }
    }
}
