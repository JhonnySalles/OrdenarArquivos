package com.fenix.ordenararquivos.notification

import animatefx.animation.FadeIn
import animatefx.animation.FadeOut
import com.fenix.ordenararquivos.controller.PopupNotificacaoController
import com.fenix.ordenararquivos.model.enums.Notificacao
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.animation.TranslateTransition
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.geometry.Pos
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.AnchorPane
import javafx.util.Duration
import org.controlsfx.control.Notifications
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException

object Notificacoes {
    private val LOGGER: Logger = LoggerFactory.getLogger(Notificacoes::class.java)
    private val IMG_ALERTA: ImageView = ImageView(Image(AlertasPopup::class.java.getResourceAsStream("/images/alert/icoAlerta_48.png")))
    private val IMG_AVISO: ImageView = ImageView(Image(AlertasPopup::class.java.getResourceAsStream("/images/alert/icoAviso_48.png")))
    private val IMG_ERRO: ImageView = ImageView(Image(AlertasPopup::class.java.getResourceAsStream("/images/alert/icoErro_48.png")))
    private val IMG_CONFIRMA: ImageView = ImageView(Image(AlertasPopup::class.java.getResourceAsStream("/images/alert/icoConfirma_48.png")))
    private val IMG_SUCESSO: ImageView = ImageView(Image(AlertasPopup::class.java.getResourceAsStream("/images/alert/btnConfirma_48.png")))

    private lateinit var CONTROLLER: PopupNotificacaoController
    private lateinit var NOTIFICACAO: AnchorPane
    private lateinit var ROOT_ANCHOR_PANE: AnchorPane
    private lateinit var TM_LINE_CLOSE: Timeline
    private lateinit var TT_AP_NOTIFICACOES: TranslateTransition
    private var APARECENDO: Boolean = false

    var rootAnchorPane: AnchorPane
        get() = ROOT_ANCHOR_PANE
        set(rootAnchorPane) {
            ROOT_ANCHOR_PANE = rootAnchorPane
        }

    private fun setTipo(tipo: Notificacao, controller: PopupNotificacaoController, root: AnchorPane) {
        root.styleClass.clear()
        when (tipo) {
            Notificacao.ALERTA -> {
                root.styleClass.add("notificacaoAlertaBackground")
                controller.setImagem(IMG_ALERTA)
            }
            Notificacao.AVISO -> {
                root.styleClass.add("notificacaoAvisoBackground")
                controller.setImagem(IMG_AVISO)
            }
            Notificacao.ERRO -> {
                root.styleClass.add("notificacaoErroBackground")
                controller.setImagem(IMG_ERRO)
            }
            Notificacao.SUCESSO -> {
                root.styleClass.add("notificacaoSucessoBackground")
                controller.setImagem(IMG_SUCESSO)
            }
            else -> {
                root.styleClass.add("notificacaoAvisoBackground")
                controller.setImagem(IMG_AVISO)
            }
        }
    }

    private fun abreNotificacao() {
        TT_AP_NOTIFICACOES.stop()
        TT_AP_NOTIFICACOES.toY = 0.0
        TT_AP_NOTIFICACOES.play()
    }

    private fun closeNotificacao() {
        TT_AP_NOTIFICACOES.stop()
        TT_AP_NOTIFICACOES.toY = CONTROLLER.wheight + 5
        TT_AP_NOTIFICACOES.play()
    }

    private fun clear() {
        APARECENDO = NOTIFICACAO.translateY == 0.0
        if (NOTIFICACAO.translateY != 0.0) {
            TM_LINE_CLOSE.stop()
            NOTIFICACAO.toBack()
        }
    }

    private fun create() {
        try {
            val loader = FXMLLoader(PopupNotificacaoController.fxmlLocate)
            NOTIFICACAO = loader.load()
            CONTROLLER = loader.getController()
            ROOT_ANCHOR_PANE.children.add(NOTIFICACAO)
        } catch (e: IOException) {
            LOGGER.error(e.message, e)
        }
        TT_AP_NOTIFICACOES = TranslateTransition(Duration(500.0), NOTIFICACAO)
        TT_AP_NOTIFICACOES.onFinished = object : EventHandler<ActionEvent> {
            @Override
            override fun handle(actionEvent: ActionEvent) {
                clear()
            }
        }
        TM_LINE_CLOSE = Timeline(KeyFrame(Duration.millis(5000.0), { closeNotificacao() }))
        APARECENDO = false
    }

    /**
     *
     *
     * Função para apresentar um popup com algumas informações como aviso, alertas
     * etc.
     *
     *
     *
     *
     * A referência para o controle é obrigatóriamente recebida apenas uma vez na
     * inicialização.
     *
     *
     * @param tipo   Tipo da notificação solicitada, podendo ser ALERTA, AVISO,
     * ERRO, SUCESSO.
     * @param titulo Titulo da notificação.
     * @param texto  Texto da notificação.
     * @author Jhonny de Salles Noschang
     */
    fun notificacao(tipo: Notificacao, titulo: String, texto: String) {
        if (!Notificacoes::NOTIFICACAO.isInitialized || !Notificacoes::CONTROLLER.isInitialized)
            create()

        TM_LINE_CLOSE.stop()
        if (!APARECENDO) {
            setTipo(tipo, CONTROLLER, NOTIFICACAO)
            NOTIFICACAO.addEventHandler(MouseEvent.MOUSE_PRESSED, object : EventHandler<MouseEvent> {
                override fun handle(e: MouseEvent) {
                    closeNotificacao()
                }
            })
            NOTIFICACAO.toFront()
            CONTROLLER.setTitulo(titulo).setTexto(texto)
            AnchorPane.setBottomAnchor(NOTIFICACAO, 5.0)
            AnchorPane.setRightAnchor(NOTIFICACAO, 5.0)
            NOTIFICACAO.translateY = CONTROLLER.wheight + 5
            abreNotificacao()
        } else {
            val fade = FadeOut(NOTIFICACAO)
            fade.setOnFinished {
                setTipo(tipo, CONTROLLER, NOTIFICACAO)
                CONTROLLER.setTitulo(titulo).setTexto(texto)
                FadeIn(NOTIFICACAO).play()
            }
            fade.setSpeed(100.0)
            fade.play()
        }
        TM_LINE_CLOSE.play()
    }

    /**
     *
     *
     * Notificação indepentende do sistema, onde é apresentado através da biblioteca
     * **Notificatiions**.
     *
     *
     * @param titulo  Titulo que será apresentado na notificação.
     * @param texto   Texto a ser apresentado.
     * @param duracao Duração da sua exibição.
     * @param posicao Sua posição na tela.
     * @author Jhonny de Salles Noschang
     */
    fun windowsDark(titulo: String, texto: String, duracao: Double, posicao: Pos) {
        val notificacao: Notifications = Notifications.create().title(titulo).text(texto)
            .hideAfter(Duration.seconds(duracao)).position(posicao).darkStyle()
        notificacao.show()
    }

    /**
     *
     *
     * Notificação indepentende do sistema, onde é apresentado através da biblioteca
     * **Notificatiions**.
     *
     *
     * @param titulo  Titulo que será apresentado na notificação.
     * @param texto   Texto a ser apresentado.
     * @param duracao Duração da sua exibição.
     * @param posicao Sua posição na tela.
     * @author Jhonny de Salles Noschang
     */
    fun windowsWhite(titulo: String, texto: String, duracao: Double, posicao: Pos) {
        val notificacao = Notifications.create().title(titulo).text(texto)
            .hideAfter(Duration.seconds(duracao)).position(posicao)
        notificacao.show()
    }
}