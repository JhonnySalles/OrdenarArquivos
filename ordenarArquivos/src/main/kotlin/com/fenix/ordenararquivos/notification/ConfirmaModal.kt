package com.fenix.ordenararquivos.notification

import com.fenix.ordenararquivos.controller.PopupConfirmaController
import com.jfoenix.controls.JFXAlert
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.effect.BoxBlur
import javafx.scene.layout.StackPane
import javafx.stage.Modality
import java.util.*

object ConfirmaModal {

    private lateinit var ROOT_STACK_PANE: StackPane
    private lateinit var NODE_BLUR: Node

    var isTeste: Boolean = false
    var testResult: Boolean = true
    var lastAlertTitle: String? = null
    var lastAlertText: String? = null

    var rootStackPane: StackPane
        get() = ROOT_STACK_PANE
        set(rootStackPane) {
            ROOT_STACK_PANE = rootStackPane
        }
    var nodeBlur: Node
        get() = NODE_BLUR
        set(nodeBlur) {
            NODE_BLUR = nodeBlur
        }

    /**
     * Função para apresentar mensagem com confirmação, utilizando FXML.
     */
    @JvmStatic
    fun confirmacao(rootStackPane: StackPane, nodeBlur: Node, titulo: String, texto: String): Boolean = alertFXML(rootStackPane, nodeBlur, titulo, texto)

    /**
     * Função padrão para apresentar mensagem de confirmação que apenas recebe os textos.
     */
    @JvmStatic
    fun confirmacao(titulo: String, texto: String): Boolean {
        return alertFXML(ROOT_STACK_PANE, NODE_BLUR, titulo, texto)
    }

    private var RESULTADO = false

    private fun alertFXML(rootStackPane: StackPane, nodeBlur: Node, titulo: String, texto: String): Boolean {
        if (isTeste) {
            lastAlertTitle = titulo
            lastAlertText = texto
            return testResult
        }
        RESULTADO = false
        val blur = BoxBlur(3.0, 3.0, 3)
        val alert: JFXAlert<String> = JFXAlert(rootStackPane.scene.window)
        alert.initModality(Modality.APPLICATION_MODAL)
        alert.isOverlayClose = false

        val loader = FXMLLoader(PopupConfirmaController.fxmlLocate)
        val parent: Parent = loader.load()
        val controller: PopupConfirmaController = loader.getController()

        controller.setTexto(titulo, texto)
        controller.setEventosConfirmacao(
            { // Cancelar
                RESULTADO = false
                alert.hideWithAnimation()
            },
            { // Confirmar
                RESULTADO = true
                alert.hideWithAnimation()
            }
        )

        alert.setContent(parent)
        
        alert.onCloseRequestProperty().set {
            nodeBlur.effect = null
            nodeBlur.isDisable = false
        }
        
        nodeBlur.effect = blur
        nodeBlur.isDisable = true

        alert.setResultConverter { null }
        val result: Optional<String> = alert.showAndWait()
        if (result.isPresent) {
            alert.result = null
        }
        return RESULTADO
    }
}
