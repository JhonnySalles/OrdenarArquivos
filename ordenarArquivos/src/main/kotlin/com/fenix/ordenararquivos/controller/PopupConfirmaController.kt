package com.fenix.ordenararquivos.controller

import com.jfoenix.controls.JFXTextArea
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import java.net.URL
import java.util.*

class PopupConfirmaController : Initializable {

    companion object {
        val IMG_CONFIRMA: Image = Image(PopupConfirmaController::class.java.getResourceAsStream("/images/alert/icoConfirma_48.png"))
        val IMG_AVISO: Image = Image(PopupConfirmaController::class.java.getResourceAsStream("/images/alert/icoAviso_48.png"))
        val IMG_ALERTA: Image = Image(PopupConfirmaController::class.java.getResourceAsStream("/images/alert/icoAlerta_48.png"))
        val IMG_ERRO: Image = Image(PopupConfirmaController::class.java.getResourceAsStream("/images/alert/icoErro_48.png"))
        val fxmlLocate: URL get() = PopupConfirmaController::class.java.getResource("/view/PopupConfirmar.fxml") as URL
    }

    @FXML
    private lateinit var lblTitulo: Label

    @FXML
    private lateinit var imgIcone: ImageView

    @FXML
    private lateinit var txtTexto: JFXTextArea

    @FXML
    private lateinit var btnCancelar: Button

    @FXML
    private lateinit var btnConfirmar: Button

    fun setEventosConfirmacao(cancelar: EventHandler<ActionEvent>, confirmar: EventHandler<ActionEvent>) {
        btnCancelar.onAction = cancelar
        btnConfirmar.onAction = confirmar
    }

    fun setTexto(titulo: String, texto: String) {
        if (titulo.isNotEmpty())
            lblTitulo.text = titulo

        if (texto.isNotEmpty())
            txtTexto.text = texto
    }

    fun aviso() {
        imgIcone.image = IMG_AVISO
    }

    fun alerta() {
        imgIcone.image = IMG_ALERTA
    }

    fun erro() {
        imgIcone.image = IMG_ERRO
    }

    fun confirmacao() {
        imgIcone.image = IMG_CONFIRMA
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
    }

}
