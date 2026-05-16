package com.fenix.ordenararquivos.controller

import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXDialog
import com.jfoenix.controls.JFXDialogLayout
import com.jfoenix.controls.JFXTextField
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.effect.BoxBlur
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.text.Font
import java.net.URL
import java.util.*

class PopupCapitulosDividirController : Initializable {

    @FXML
    lateinit var txtInicio: JFXTextField

    @FXML
    lateinit var txtFim: JFXTextField

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        // Inicialização básica se necessário
    }

    fun setRange(inicio: Double, fim: Double) {
        txtInicio.text = if (inicio == Double.MIN_VALUE) "" else inicio.toString()
        txtFim.text = if (fim == Double.MAX_VALUE) "" else fim.toString()
    }

    fun getInicio(): Double = txtInicio.text.toDoubleOrNull() ?: Double.MIN_VALUE
    fun getFim(): Double = txtFim.text.toDoubleOrNull() ?: Double.MAX_VALUE

    companion object {
        fun abreTelaDividir(stackPane: StackPane, nodeBlur: Node, inicio: Double, fim: Double, callback: (Double, Double) -> Unit) {
            try {
                val blur = BoxBlur(3.0, 3.0, 3)
                val dialogLayout = JFXDialogLayout()
                val subDialog = JFXDialog(stackPane, dialogLayout, JFXDialog.DialogTransition.CENTER)

                val loader = FXMLLoader(PopupCapitulosDividirController::class.java.getResource("/view/PopupCapitulosDividir.fxml"))
                val newAnchorPane: Parent = loader.load()
                val cnt: PopupCapitulosDividirController = loader.getController()

                cnt.setRange(inicio, fim)

                val titulo = Label("Dividir Volume")
                titulo.font = Font.font(20.0)
                titulo.textFill = Color.WHITE
                val hbTitulo = HBox(titulo)
                hbTitulo.alignment = Pos.CENTER
                hbTitulo.maxWidth = Double.MAX_VALUE

                dialogLayout.setHeading(hbTitulo)
                dialogLayout.setBody(newAnchorPane)

                val btnVoltar = JFXButton("Voltar")
                btnVoltar.styleClass.add("background-White1")
                btnVoltar.setOnAction { subDialog.close() }

                val btnConfirmar = JFXButton("Confirmar")
                btnConfirmar.styleClass.addAll("background-Green2", "texto-stilo-1")
                btnConfirmar.setOnAction {
                    callback(cnt.getInicio(), cnt.getFim())
                    subDialog.close()
                }

                dialogLayout.setActions(listOf(btnVoltar, btnConfirmar))
                
                subDialog.setOnDialogClosed {
                    nodeBlur.effect = null
                    nodeBlur.isDisable = false
                }

                nodeBlur.effect = blur
                nodeBlur.isDisable = true
                
                subDialog.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
