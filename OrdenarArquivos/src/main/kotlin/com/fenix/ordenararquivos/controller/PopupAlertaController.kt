package com.fenix.ordenararquivos.controller

import com.jfoenix.controls.JFXTextArea
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.geometry.Insets
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import java.net.URL
import java.util.*


class PopupAlertaController : Initializable {
    
    @FXML
    private lateinit var btnOk: Button

    @FXML
    private lateinit var lblTitulo: Label

    @FXML
    private lateinit var imgIcone: ImageView

    @FXML
    private lateinit var txtTexto: JFXTextArea

    private var topo: Double = 0.0
    private var esquerda: Double = 0.0

    fun setEventosBotoes(ok: EventHandler<ActionEvent>) {
        btnOk.onAction = ok
    }

    fun setTexto(titulo: String, texto: String) {
        if (titulo != "")
            lblTitulo.text = titulo

        if (texto != "")
            txtTexto.text = texto
    }

    fun setVisivel(titulo: Boolean, imagem: Boolean) {
        topo = 0.0
        esquerda = 0.0

        if (titulo)
            topo = 20.0
        if (imagem)
            esquerda = 65.0

        lblTitulo.isVisible = titulo
        imgIcone.isVisible = imagem
        txtTexto.padding = Insets(topo, 0.0, 0.0, esquerda)
    }

    @Override
    override fun initialize(location: URL?, resources: ResourceBundle?) {
    }

    companion object {
        val IMG_ALERTA: Image = Image(PopupAlertaController::class.java.getResourceAsStream("/comum/imagens/alerta/icoAlerta_48.png"))
        val fxmlLocate: URL get() = PopupAlertaController::class.java.getResource("/view/PopupAlerta.fxml") as URL
    }
}