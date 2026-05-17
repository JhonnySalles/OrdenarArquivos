package com.fenix.ordenararquivos.controller

import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXCheckBox
import com.jfoenix.controls.JFXDialog
import com.jfoenix.controls.JFXDialogLayout
import com.jfoenix.controls.JFXTextField
import javafx.scene.layout.VBox
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

class PopupSubstituirController : Initializable {

    @FXML
    lateinit var txtLocalizar: JFXTextField

    @FXML
    lateinit var txtSubstituir: JFXTextField

    @FXML
    lateinit var ckbRegex: JFXCheckBox

    @FXML
    lateinit var hbSugestoes: HBox

    @FXML
    lateinit var vbPreview: VBox

    @FXML
    lateinit var vbOriginal: VBox

    @FXML
    lateinit var vbSubstituido: VBox

    private var titulosOriginais: List<String> = emptyList()

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        adicionarSugestoes()
    }

    private fun adicionarSugestoes() {
        val sugestoes = listOf(
            Sugestao("Colchetes [ ]", "\\[.*?\\]", ""),
            Sugestao("Parênteses ( )", "\\(.*?\\)", ""),
            Sugestao("Traços -", "\\s*-\\s*", " "),
            Sugestao("Extensão", "\\.[a-zA-Z0-9]+$", "")
        )

        for (sugestao in sugestoes) {
            val btn = JFXButton(sugestao.nome)
            btn.styleClass.add("background-Black3")
            btn.textFill = Color.WHITE
            btn.setOnAction {
                txtLocalizar.text = sugestao.localizar
                txtSubstituir.text = sugestao.substituir
            }
            hbSugestoes.children.add(btn)
        }
    }

    fun setExemplos(titulos: List<String>) {
        titulosOriginais = titulos.filter { it.isNotEmpty() }.shuffled().take(3)
        if (titulosOriginais.isNotEmpty()) {
            vbPreview.isVisible = true
            vbOriginal.children.clear()
            for (titulo in titulosOriginais) {
                val lbl = Label(titulo)
                lbl.textFill = Color.WHITE
                lbl.font = Font.font(8.0)
                vbOriginal.children.add(lbl)
            }
            atualizarPreview()

            txtLocalizar.textProperty().addListener { _, _, _ -> atualizarPreview() }
            txtSubstituir.textProperty().addListener { _, _, _ -> atualizarPreview() }
            ckbRegex.selectedProperty().addListener { _, _, _ -> atualizarPreview() }
        } else {
            vbPreview.isVisible = false
        }
    }

    private fun atualizarPreview() {
        vbSubstituido.children.clear()
        val localizar = txtLocalizar.text ?: ""
        val substituir = txtSubstituir.text ?: ""

        for (original in titulosOriginais) {
            val lbl = Label()
            lbl.font = Font.font(8.0)
            if (localizar.isEmpty()) {
                lbl.text = original
                lbl.textFill = Color.GRAY
            } else {
                try {
                    if (ckbRegex.isSelected) {
                        lbl.text = original.replace(Regex(localizar), substituir)
                        lbl.textFill = Color.web("#0cff00")
                    } else {
                        lbl.text = original.replace(localizar, substituir)
                        lbl.textFill = Color.web("#0cff00")
                    }
                } catch (e: Exception) {
                    lbl.text = "Regex Inválido"
                    lbl.textFill = Color.RED
                }
            }
            vbSubstituido.children.add(lbl)
        }
    }

    class Sugestao(val nome: String, val localizar: String, val substituir: String)

    companion object {
        fun abreTelaSubstituir(stackPane: StackPane, nodeBlur: Node, titulos: List<String>, callback: (String, String, Boolean) -> Unit) {
            try {
                val blur = BoxBlur(3.0, 3.0, 3)
                val dialogLayout = JFXDialogLayout()
                val subDialog = JFXDialog(stackPane, dialogLayout, JFXDialog.DialogTransition.CENTER)

                val loader = FXMLLoader(PopupSubstituirController::class.java.getResource("/view/PopupSubstituir.fxml"))
                val newAnchorPane: Parent = loader.load()
                val cnt: PopupSubstituirController = loader.getController()
                cnt.setExemplos(titulos)

                val titulo = Label("Substituir no Título")
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
                    val loc = cnt.txtLocalizar.text ?: ""
                    val sub = cnt.txtSubstituir.text ?: ""
                    val isRegex = cnt.ckbRegex.isSelected
                    if (loc.isNotEmpty()) {
                        callback(loc, sub, isRegex)
                        subDialog.close()
                    } else {
                        cnt.txtLocalizar.requestFocus()
                    }
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
