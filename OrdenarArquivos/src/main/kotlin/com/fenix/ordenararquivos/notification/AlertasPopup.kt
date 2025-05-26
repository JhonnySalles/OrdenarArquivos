package com.fenix.ordenararquivos.notification

import com.jfoenix.controls.*
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.effect.BoxBlur
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.stage.Modality
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.TrayIcon.MessageType
import java.awt.image.BufferedImage
import java.net.URL
import java.util.*
import javax.imageio.ImageIO

object AlertasPopup {
    private val ALERTA: Image = Image(AlertasPopup::class.java.getResourceAsStream("/images/alert/icoAlerta_48.png"))
    private val AVISO: Image = Image(AlertasPopup::class.java.getResourceAsStream("/images/alert/icoAviso_48.png"))
    private val ERRO: Image = Image(AlertasPopup::class.java.getResourceAsStream("/images/alert/icoErro_48.png"))
    private val CONFIRMA: Image = Image(AlertasPopup::class.java.getResourceAsStream("/images/alert/icoConfirma_48.png"))

    private val CSS: String = (AlertasPopup::class.java.getResource("/css/Dark_Alerts.css") as URL).toExternalForm()
    private val CSS_THEME: String = (AlertasPopup::class.java.getResource("/css/Dark_TelaInicial.css") as URL).toExternalForm()
    private lateinit var ROOT_STACK_PANE: StackPane
    private lateinit var NODE_BLUR: Node

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
     *
     *
     * Função para apresentar mensagem de aviso, onde irá mostrar uma caixa no topo
     * e esmaecer o fundo.
     *
     *
     * @param Primeiro  parâmetro deve-se passar a referência para o stack pane.
     * @param Conforme  a cascata, obter o primeiro conteudo interno para que seja
     * esmaecido.
     * @param Parametro opcional, pode-se passar varios botões em uma lista, caso
     * não informe por padrão irá adicionar um botão ok.
     * @param Campo     **String** que irá conter a mensagem a ser exibida.
     */
    fun avisoModal(rootStackPane: StackPane, nodeBlur: Node?, botoes: MutableList<JFXButton>, titulo: String, texto: String) = dialogModern(rootStackPane, nodeBlur, botoes, titulo, texto, ImageView(AVISO))

    /**
     *
     *
     * Função padrão de aviso que apenas recebe os textos, irá obter os pane global
     * do dashboard.
     *
     *
     */
    fun avisoModal(titulo: String, texto: String) = dialogModern(ROOT_STACK_PANE, NODE_BLUR, mutableListOf(), titulo, texto, ImageView(AVISO))

    /**
     *
     *
     * Função para apresentar mensagem de alerta, onde irá mostrar uma caixa no topo
     * e esmaecer o fundo.
     *
     *
     * @param Primeiro  parâmetro deve-se passar a referência para o stack pane.
     * @param Conforme  a cascata, obter o primeiro conteudo interno para que seja
     * esmaecido.
     * @param Parametro opcional, pode-se passar varios botões em uma lista, caso
     * não informe por padrão irá adicionar um botão ok.
     * @param Campo     **String** que irá conter a mensagem a ser exibida.
     */
    fun alertaModal(rootStackPane: StackPane, nodeBlur: Node?, botoes: MutableList<JFXButton>, titulo: String, texto: String) = dialogModern(rootStackPane, nodeBlur, botoes, titulo, texto, ImageView(ALERTA))

    /**
     *
     *
     * Função padrão de alerta que apenas recebe os textos, irá obter os pane global
     * do dashboard.
     *
     *
     */
    fun alertaModal(titulo: String, texto: String) = dialogModern(ROOT_STACK_PANE, NODE_BLUR, mutableListOf(), titulo, texto, ImageView(ALERTA))

    /**
     *
     *
     * Função para apresentar mensagem de erro, onde irá mostrar uma caixa no topo e
     * esmaecer o fundo.
     *
     *
     * @param Primeiro  parâmetro deve-se passar a referência para o stack pane.
     * @param Conforme  a cascata, obter o primeiro conteudo interno para que seja
     * esmaecido.
     * @param Parametro opcional, pode-se passar varios botões em uma lista, caso
     * não informe por padrão irá adicionar um botão ok.
     * @param Campo     **String** que irá conter a mensagem a ser exibida.
     */
    fun erroModal(rootStackPane: StackPane, nodeBlur: Node?, botoes: MutableList<JFXButton>, titulo: String, texto: String) {
        dialogModern(rootStackPane, nodeBlur, botoes, titulo, texto, ImageView(ERRO))
    }

    /**
     *
     *
     * Função padrão de alerta que apenas recebe os textos, irá obter os pane global
     * do dashboard.
     *
     *
     */
    fun erroModal(titulo: String, texto: String) = dialogModern(ROOT_STACK_PANE, NODE_BLUR, mutableListOf(), titulo, texto, ImageView(ERRO))

    /**
     *
     *
     * Função para apresentar mensagem com confirmação, onde irá mostrar uma caixa
     * no topo e esmaecer o fundo.
     *
     *
     * @param Primeiro  parâmetro deve-se passar a referência para o stack pane.
     * @param Conforme  a cascata, obter o primeiro conteudo interno para que seja
     * esmaecido.
     * @param Parametro opcional, pode-se passar varios botões em uma lista, caso
     * não informe por padrão irá adicionar um botão ok.
     * @param Campo     **String** que irá conter a mensagem a ser exibida.
     * @return Resulta o valor referente ao botão cancelar ou confirmar.
     */
    fun confirmacaoModal(rootStackPane: StackPane, nodeBlur: Node, titulo: String, texto: String): Boolean = alertModern(rootStackPane, nodeBlur, titulo, texto, ImageView(CONFIRMA))

    /**
     *
     *
     * Função padrão para apresentar mensagem de confirmação que apenas recebe os
     * textos, irá obter os pane global do dashboard.
     *
     *
     */
    fun confirmacaoModal(titulo: String, texto: String): Boolean {
        return alertModern(ROOT_STACK_PANE, NODE_BLUR, titulo, texto, ImageView(CONFIRMA))
    }

    var RESULTADO = false
    private fun alertModern(rootStackPane: StackPane, nodeBlur: Node, titulo: String, texto: String, imagem: ImageView): Boolean {
        RESULTADO = false
        val blur = BoxBlur(3.0, 3.0, 3)
        val alert: JFXAlert<String> = JFXAlert(rootStackPane.scene.window)
        alert.initModality(Modality.APPLICATION_MODAL)
        alert.isOverlayClose = false
        val layout = JFXDialogLayout()
        val title = Label(titulo)
        title.styleClass.add("texto-stilo-fundo-azul")
        title.font = Font.font(20.0)
        title.textFill = Color.web("#ffffff", 0.8)
        layout.setHeading(title)
        val content = JFXTextArea(texto)
        content.isEditable = false
        content.prefHeight = 100.0
        content.styleClass.add("texto-stilo-fundo-azul")
        layout.setBody(HBox(imagem, content))
        layout.stylesheets.add(CSS)
        layout.stylesheets.add(CSS_THEME)
        val confirmButton = JFXButton("Confirmar")
        confirmButton.isDefaultButton = true
        confirmButton.setOnAction {
            RESULTADO = true
            alert.hideWithAnimation()
        }
        confirmButton.styleClass.add("btnConfirma")
        val cancelButton = JFXButton("Cancelar")
        cancelButton.isCancelButton = true
        cancelButton.setOnAction {
            RESULTADO = false
            alert.hideWithAnimation()
        }
        cancelButton.styleClass.add("btnCancela")
        layout.setActions(cancelButton, confirmButton)
        alert.setContent(layout)
        alert.onCloseRequestProperty().set { nodeBlur.effect = null }
        nodeBlur.effect = blur

        // Devido a um erro no componente, não funciona o retorno padrão, será feito
        // pela variável resultado.
        alert.setResultConverter { null }
        val result: Optional<String> = alert.showAndWait()
        if (result.isPresent) {
            alert.result = null
        }
        return RESULTADO
    }

    private fun dialogModern(rootStackPane: StackPane, nodeBlur: Node?, botoes: MutableList<JFXButton>, titulo: String, texto: String, imagem: ImageView) {
        val blur = BoxBlur(3.0, 3.0, 3)

        if (botoes.isEmpty())
            botoes.add(JFXButton("Ok"))

        val dialogLayout = JFXDialogLayout()
        val dialog = JFXDialog(rootStackPane, dialogLayout, JFXDialog.DialogTransition.CENTER)
        dialog.stylesheets.add(CSS)
        dialog.stylesheets.add(CSS_THEME)
        botoes.forEach { controlButton ->
            controlButton.styleClass.add("btnAlerta")
            controlButton.addEventHandler(MouseEvent.MOUSE_CLICKED) { dialog.close() }
        }
        dialogLayout.setHeading(Label(titulo))
        dialogLayout.setBody(HBox(imagem, Label(texto)))
        dialogLayout.setActions(botoes)
        dialog.setOnDialogClosed { nodeBlur?.effect = null }
        nodeBlur?.effect = blur
        dialog.show()
    }

    // Ver
    const val CAMINHO_ICONE = "/org/jisho/textosJapones/resources/images/bd/icoDataBase_48.png"
    fun showTrayMessage(title: String, message: String) {
        try {
            val tray: SystemTray = SystemTray.getSystemTray()
            val image: BufferedImage = ImageIO.read(AlertasPopup::class.java.getResource(CAMINHO_ICONE))
            val trayIcon = TrayIcon(image, "Teste")
            trayIcon.isImageAutoSize = true
            trayIcon.toolTip = "Teste"
            tray.add(trayIcon)
            trayIcon.displayMessage(title, message, MessageType.INFO)
            tray.remove(trayIcon)
        } catch (exp: Exception) {
            exp.printStackTrace()
        }
    }
}