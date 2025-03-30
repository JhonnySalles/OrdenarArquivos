package com.fenix.ordenararquivos.notification

import com.fenix.ordenararquivos.controller.PopupAlertaController
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.AnchorPane
import javafx.stage.Modality
import javafx.stage.Stage
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 *
 *
 * Classe responssável por apresentar alertas em janela, podendo ser um alerta
 * com borda (tala do windows) ou sem borda.
 *
 *
 * @author Jhonny de Salles Noschang
 */
object Alertas {
    private val LOGGER: Logger = LoggerFactory.getLogger(Alertas::class.java)
    fun telaAlerta(titulo: String, texto: String) {
        try {
            val loader = FXMLLoader(PopupAlertaController.fxmlLocate)
            val scPnTelaPrincipal: AnchorPane = loader.load()

            // Obtem a referencia do controller para editar as label.
            val controller = loader.getController() as PopupAlertaController
            controller.setTexto(titulo, texto)
            val tela = Scene(scPnTelaPrincipal) // Carrega a scena
            val stageTela = Stage()
            stageTela.scene = tela // Seta a cena principal
            controller.setEventosBotoes { stageTela.close() }

            // Faz a tela ser obrigatoria para voltar ao voltar a tela anterior
            stageTela.title = titulo
            stageTela.icons.add(PopupAlertaController.IMG_ALERTA)
            stageTela.initModality(Modality.APPLICATION_MODAL)
            controller.setVisivel(true, imagem = true)
            stageTela.showAndWait() // Mostra a tela.
        } catch (e: Exception) {
            println("Erro ao tentar carregar o alerta.")
            LOGGER.error(e.message, e)
        }
    }
}