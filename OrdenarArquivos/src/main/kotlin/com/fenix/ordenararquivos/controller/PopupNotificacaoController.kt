package com.fenix.ordenararquivos.controller

import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.AnchorPane
import java.net.URL
import java.util.*


class PopupNotificacaoController : Initializable {

    @FXML
    private lateinit var notificacaoBackground: AnchorPane

    @FXML
    private lateinit var imgImagem: ImageView

    @FXML
    private lateinit var lblTitulo: Label

    @FXML
    private lateinit var lblTexto: Label

    var wheight: Double = 0.0
    val imagem: Image get() = imgImagem.image

    fun setImagem(imagem: ImageView): PopupNotificacaoController {
        imgImagem = imagem
        return this
    }

    val titulo: String get() = lblTitulo.text

    fun setTitulo(titulo: String): PopupNotificacaoController {
        lblTitulo.text = titulo
        return this
    }

    val texto: String
        get() = lblTexto.text

    fun setTexto(texto: String): PopupNotificacaoController {
        lblTexto.text = texto
        wheight = if (texto.length <= 80) {
            notificacaoBackground.setPrefSize(350.0, 45.0)
            notificacaoBackground.setMaxSize(350.0, 45.0)
            imgImagem.fitHeight = 35.0
            imgImagem.fitWidth = 35.0
            60.0
        } else {
            if (texto.length <= 225) {
                notificacaoBackground.setPrefSize(500.0, 80.0)
                notificacaoBackground.setMaxSize(500.0, 80.0)
                imgImagem.fitHeight = 35.0
                imgImagem.fitWidth = 35.0
                80.0
            } else {
                notificacaoBackground.setPrefSize(650.0, 100.0)
                notificacaoBackground.setMaxSize(650.0, 100.0)
                imgImagem.fitHeight = 45.0
                imgImagem.fitWidth = 45.0
                100.0
            }
        }
        return this
    }

    @Override
    override fun initialize(location: URL?, resources: ResourceBundle?) {
    }

    companion object {
        val fxmlLocate: URL get() = PopupNotificacaoController::class.java.getResource("/view/PopupNotificacao.fxml") as URL
    }
}