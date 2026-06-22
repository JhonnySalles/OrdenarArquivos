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

    private var todosTitulos: List<String> = emptyList()
    private var titulosPreview: List<String> = emptyList()

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
        todosTitulos = titulos.filter { it.isNotEmpty() }
        if (todosTitulos.isNotEmpty()) {
            vbPreview.isVisible = true
            atualizarPreview()

            txtLocalizar.focusedProperty().addListener { _, _, focused -> if (!focused) atualizarPreview() }
            txtSubstituir.focusedProperty().addListener { _, _, focused -> if (!focused) atualizarPreview() }
            ckbRegex.selectedProperty().addListener { _, _, _ -> atualizarPreview() }
        } else {
            vbPreview.isVisible = false
        }
    }

    private fun atualizarPreview() {
        vbOriginal.children.clear()
        vbSubstituido.children.clear()
        val localizar = txtLocalizar.text ?: ""
        val substituir = txtSubstituir.text ?: ""
        val isRegex = ckbRegex.isSelected

        if (localizar.isEmpty()) {
            titulosPreview = todosTitulos.shuffled().take(3)
            renderPreviewList(localizar, substituir, isRegex, isValid = true)
        } else {
            var isValid = true
            var regex: Regex? = null
            if (isRegex) {
                try {
                    regex = Regex(localizar)
                } catch (e: Exception) {
                    isValid = false
                }
            }

            if (!isValid) {
                titulosPreview = todosTitulos.shuffled().take(3)
                renderPreviewList(localizar, substituir, isRegex, isValid = false)
            } else {
                val matches = todosTitulos.filter {
                    if (isRegex) regex!!.containsMatchIn(it)
                    else it.contains(localizar)
                }
                titulosPreview = if (matches.isNotEmpty()) {
                    matches.shuffled().take(3)
                } else {
                    todosTitulos.shuffled().take(3)
                }
                renderPreviewList(localizar, substituir, isRegex, isValid = true)
            }
        }
    }

    private fun renderPreviewList(localizar: String, substituir: String, isRegex: Boolean, isValid: Boolean) {
        for (original in titulosPreview) {
            val lblOrig = Label(original)
            lblOrig.textFill = Color.WHITE
            lblOrig.font = Font.font(8.0)
            vbOriginal.children.add(lblOrig)

            val lblSub = Label()
            lblSub.font = Font.font(8.0)
            if (localizar.isEmpty()) {
                lblSub.text = original
                lblSub.textFill = Color.GRAY
            } else if (!isValid) {
                lblSub.text = "Regex Inválido"
                lblSub.textFill = Color.RED
            } else {
                try {
                    lblSub.text = if (isRegex) {
                        original.replace(Regex(localizar), substituir)
                    } else {
                        original.replace(localizar, substituir)
                    }
                    lblSub.textFill = Color.web("#0cff00")
                } catch (e: Exception) {
                    lblSub.text = "Erro ao substituir"
                    lblSub.textFill = Color.RED
                }
            }
            vbSubstituido.children.add(lblSub)
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
