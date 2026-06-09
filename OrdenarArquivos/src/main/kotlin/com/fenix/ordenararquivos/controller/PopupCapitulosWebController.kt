package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.notification.AlertasModal
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
import javafx.scene.control.Hyperlink
import javafx.scene.control.Label
import javafx.scene.effect.BoxBlur
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.web.WebView
import org.kordamp.ikonli.javafx.FontIcon
import org.kordamp.ikonli.materialdesign2.MaterialDesignR
import java.net.URL
import java.util.*

class PopupCapitulosWebController : Initializable {

    @FXML
    private lateinit var apRoot: AnchorPane

    @FXML
    private lateinit var txtEndereco: JFXTextField

    @FXML
    private lateinit var btnAtualizar: JFXButton

    @FXML
    private lateinit var webView: WebView

    @FXML
    private lateinit var hplComickIO: Hyperlink

    @FXML
    private lateinit var hplComickFan: Hyperlink

    @FXML
    private lateinit var hplTaiyo: Hyperlink

    @FXML
    private lateinit var hplMangaFire: Hyperlink

    @FXML
    private lateinit var hplMangaRead: Hyperlink

    @FXML
    private lateinit var hplMangaDex: Hyperlink

    @FXML
    private lateinit var hplMangak: Hyperlink

    @FXML
    private lateinit var hplMangaPark: Hyperlink

    @FXML
    private lateinit var hplMangaKatana: Hyperlink

    @FXML
    private lateinit var hplVyManga: Hyperlink

    @FXML
    private lateinit var hplMangaTown1: Hyperlink

    @FXML
    private lateinit var hplMangaTown2: Hyperlink

    @FXML
    private lateinit var hplMangaHere: Hyperlink

    @FXML
    private lateinit var hplKMangaKodansha: Hyperlink

    private var sincronizandoEndereco = false

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        btnAtualizar.graphic = FontIcon(MaterialDesignR.REFRESH).apply {
            iconSize = 17
            iconColor = Color.WHITE
        }

        webView.engine.locationProperty().addListener { _, _, newLocation ->
            if (!sincronizandoEndereco && !newLocation.isNullOrBlank() && newLocation != "about:blank") {
                txtEndereco.text = newLocation
            }
        }

        val links = arrayOf(
            hplComickIO, hplComickFan, hplTaiyo, hplMangaDex, hplMangaFire, hplMangaRead,
            hplMangak, hplMangaPark, hplMangaKatana, hplVyManga, hplMangaTown1, hplMangaTown2,
            hplMangaHere, hplKMangaKodansha
        )
        for (link in links) {
            link.setOnAction {
                txtEndereco.text = link.text
                carregarEndereco()
            }
        }
    }

    fun setEnderecoInicial(endereco: String) {
        txtEndereco.text = endereco
    }

    @FXML
    private fun onBtnAtualizar() {
        carregarEndereco()
    }

    fun carregarEndereco() {
        val url = txtEndereco.text.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://"))
            return

        sincronizandoEndereco = true
        webView.engine.load(url)
        sincronizandoEndereco = false
    }

    fun obterConteudoParaExtracao(): Pair<String, String> {
        val site = webView.engine.location
            .takeIf { it.startsWith("http") }
            ?: txtEndereco.text.trim()
        val html = webView.engine.executeScript("document.documentElement.outerHTML") as String
        return site to html
    }

    companion object {
        private val STYLE_SHEET: String =
            PopupCapitulosWebController::class.java.getResource("/css/Dark_TelaInicial.css").toExternalForm()

        internal fun restaurarEstadoDialog(nodeBlur: Node) {
            nodeBlur.effect = null
            nodeBlur.isDisable = false
        }

        fun abreTelaWeb(
            stackPane: StackPane,
            nodeBlur: Node,
            enderecoInicial: String,
            onConfirm: (site: String, html: String) -> Unit
        ) {
            try {
                val blur = BoxBlur(3.0, 3.0, 3)
                val dialogLayout = JFXDialogLayout()
                val subDialog = JFXDialog(stackPane, dialogLayout, JFXDialog.DialogTransition.CENTER)

                val loader = FXMLLoader(PopupCapitulosWebController::class.java.getResource("/view/PopupCapitulosWeb.fxml"))
                val newAnchorPane: Parent = loader.load()
                val cnt: PopupCapitulosWebController = loader.getController()

                cnt.setEnderecoInicial(enderecoInicial)
                if (enderecoInicial.trim().startsWith("http"))
                    cnt.carregarEndereco()

                val titulo = Label("Navegar e importar capítulos")
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
                    val (site, html) = cnt.obterConteudoParaExtracao()
                    onConfirm(site, html)
                    subDialog.close()
                }

                dialogLayout.setActions(listOf(btnVoltar, btnConfirmar))
                dialogLayout.styleClass.add("dialog-black")
                subDialog.stylesheets.add(STYLE_SHEET)

                subDialog.setOnDialogClosed {
                    restaurarEstadoDialog(nodeBlur)
                }

                nodeBlur.effect = blur
                nodeBlur.isDisable = true

                subDialog.show()
            } catch (e: Exception) {
                restaurarEstadoDialog(nodeBlur)
                AlertasModal.erro("Erro ao abrir navegador", e.message ?: e.toString())
            }
        }
    }
}
