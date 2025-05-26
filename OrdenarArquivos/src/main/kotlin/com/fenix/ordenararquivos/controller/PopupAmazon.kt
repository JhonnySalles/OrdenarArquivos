package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.notification.AlertasPopup
import com.jfoenix.controls.*
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.effect.BoxBlur
import javafx.scene.input.KeyCode
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.robot.Robot
import javafx.scene.text.Font
import javafx.util.Callback
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URL
import java.time.LocalDate
import java.util.*


class PopupAmazon : Initializable {

    private val LOGGER: Logger = LoggerFactory.getLogger(PopupAmazon::class.java)

    @FXML
    lateinit var txtSiteAmazon: JFXTextField

    @FXML
    lateinit var txtSerie: JFXTextField

    @FXML
    lateinit var txtTitulo: JFXTextField

    @FXML
    lateinit var txtEditora: JFXTextField

    @FXML
    lateinit var txtEditoraSite: JFXTextField

    @FXML
    lateinit var dpPublicacao: JFXDatePicker

    @FXML
    lateinit var txtPublicacaoSite: JFXTextField

    @FXML
    lateinit var txtAreaComentario: JFXTextArea

    @FXML
    lateinit var cbLinguagem: JFXComboBox<Linguagem>

    private val robot: Robot = Robot()
    private lateinit var consulta: ComicInfo

    var objeto: ComicInfo
        get() {
            carregaObjeto()
            return consulta
        }
        set(value) {
            consulta = value
            carregaCampos()
        }

    fun setLinguagem(linguagem : Linguagem) = cbLinguagem.selectionModel.select(linguagem)

    private fun carregaCampos() {
        cbLinguagem.setOnKeyPressed { ke -> if (ke.code.equals(KeyCode.ENTER)) robot.keyPress(KeyCode.TAB) }
        txtSerie.text = consulta.series
        txtTitulo.text = consulta.title
        txtEditora.text = consulta.publisher
        txtEditoraSite.text = ""
        dpPublicacao.value = if (consulta.year != null && consulta.month != null && consulta.day != null) LocalDate.of(consulta.year!!, consulta.month!!, consulta.day!!) else null
        txtPublicacaoSite.text = ""
        txtAreaComentario.text = consulta.review
    }

    private fun carregaObjeto() {
        if (!txtSerie.text.isNullOrEmpty())
            consulta.series = txtSerie.text

        if (!txtTitulo.text.isNullOrEmpty())
            consulta.title = txtTitulo.text

        if (!txtEditora.text.isNullOrEmpty())
            consulta.publisher = txtEditora.text

        if (dpPublicacao.value != null) {
            consulta.year = dpPublicacao.value.year
            consulta.month = dpPublicacao.value.monthValue
            consulta.day = dpPublicacao.value.dayOfMonth
        }

        if (!txtAreaComentario.text.isNullOrEmpty())
            consulta.review = txtAreaComentario.text
    }

    private fun obtemData(texto : String, linguagem: Linguagem) : String {
        return when(linguagem) {
            Linguagem.JAPANESE -> {
                val day = texto.substringAfterLast("/")
                val year = texto.substringBefore("/")
                val mouth = texto.substringAfter("/").substringBefore("/")
                "$year-${mouth.padStart(2, '0')}-${day.padStart(2, '0')}"
            }
            Linguagem.ENGLISH -> {
                val day = texto.substringBefore(",").substringAfter(" ")
                val year = texto.substringAfter(",")
                val mouth = when(texto.lowercase().substringBefore(" ")) {
                    "january" -> "01"
                    "february" -> "02"
                    "march" -> "03"
                    "april" -> "04"
                    "may" -> "05"
                    "june" -> "06"
                    "july" -> "07"
                    "august" -> "08"
                    "september" -> "09"
                    "october" -> "10"
                    "november" -> "11"
                    "december" -> "12"
                    else -> ""
                }

                "$year-${mouth.padStart(2, '0')}-${day.padStart(2, '0')}"
            }
            else -> texto
        }
    }

    private fun consulta() {
        if (txtSiteAmazon.text.isNullOrEmpty())
            return

        try {
            val pagina: Document = try {
                Jsoup.connect(txtSiteAmazon.text)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
                    .referrer("http://www.google.com")
                    .get()
            } catch (e: IOException) {
                LOGGER.error(e.message, e)
                AlertasPopup.erroModal("Erro ao carregar o site", e.message.toString())
                return
            } catch (e: Exception) {
                LOGGER.error(e.message, e)
                AlertasPopup.erroModal("Erro ao carregar o site", e.message.toString())
                return
            }

            val linguagem = if (txtSiteAmazon.text.lowercase().contains("www.amazon.co.jp"))
                Linguagem.JAPANESE
            else if (txtSiteAmazon.text.lowercase().contains("www.amazon.com/"))
                Linguagem.ENGLISH
            else
                cbLinguagem.value

            var titulo = ""
            var publicacao = ""
            var publicacaoSite = ""
            var editora = ""
            var editoraSite = ""
            var comentario = ""
            var serie = ""
            var isbn = ""

            when (linguagem) {
                Linguagem.ENGLISH -> {
                    pagina.getElementById("productTitle")?.let { titulo = it.text() }
                    pagina.getElementById("bookDescription_feature_div")?.let { comentario = it.text() }

                    val publication = pagina.getElementById("rpi-attribute-book_details-publication_date")
                    if (publication != null) {
                        for (element in publication.allElements)
                            if (element.className().contains("attribute-value", true)) {
                                publicacaoSite = element.text()
                                publicacao = obtemData(element.text(), linguagem)
                                break
                            }
                    }

                    val detailIsbn = pagina.getElementById("rpi-attribute-book_details-isbn13")
                    if (detailIsbn != null) {
                        for (element in detailIsbn.allElements)
                            if (element.className().contains("attribute-value", true)) {
                                isbn = element.text()
                                break
                            }
                    }

                    val publisher = pagina.getElementById("rpi-attribute-book_details-publisher")
                    if (publisher != null) {
                        for (element in publisher.allElements)
                            if (element.className().contains("attribute-value", true)) {
                                editora = element.text()
                                editoraSite = element.text()
                                break
                            }
                    }

                    val series = pagina.getElementById("rpi-attribute-book_details-series")
                    if (series != null) {
                        for (element in series.allElements)
                            if (element.className().contains("attribute-value", true)) {
                                serie = element.text()
                                break
                            }
                    }

                    val details = pagina.getElementById("detailBullets_feature_div")
                    if (details != null) {
                        val tables = details.getElementsByAttribute("li")
                        for (item in tables) {
                            if (!item.text().contains(":"))
                                continue

                            val title = item.text().substringBefore(":").lowercase().trim()
                            when (title) {
                                "publisher" -> {
                                    val value = item.text().substringAfter(":").trim()

                                    if (editoraSite.isEmpty()) {
                                        editoraSite = value.substringBefore("(").trim()
                                        editora = editoraSite
                                    }

                                    if (publicacaoSite.isEmpty()) {
                                        publicacaoSite = value.substringAfter("(").replace(")","").trim()
                                        publicacao = obtemData(publicacaoSite, linguagem)
                                    }
                                }
                                "isbn-13" -> {
                                    if (isbn.isEmpty()) {
                                        val value = item.text().substringAfter(":").trim()
                                        isbn = value
                                    }
                                }
                            }
                        }
                    }
                }
                Linguagem.JAPANESE -> {
                    pagina.getElementById("productTitle")?.let { titulo = it.text() }
                    pagina.getElementById("bookDescription_feature_div")?.let { comentario = it.text() }

                    val publication = pagina.getElementById("rpi-attribute-book_details-publication_date")
                    if (publication != null) {
                        for (element in publication.allElements)
                            if (element.className().contains("attribute-value", true)) {
                                publicacaoSite = element.text()
                                publicacao = obtemData(element.text(), linguagem)
                                break
                            }
                    }

                    val detailIsbn = pagina.getElementById("rpi-attribute-book_details-isbn13")
                    if (detailIsbn != null) {
                        for (element in detailIsbn.allElements)
                            if (element.className().contains("attribute-value", true)) {
                                isbn = element.text()
                                break
                            }
                    }

                    val publisher = pagina.getElementById("rpi-attribute-book_details-publisher")
                    if (publisher != null) {
                        for (element in publisher.allElements)
                            if (element.className().contains("attribute-value", true)) {
                                editora = element.text()
                                editoraSite = element.text()
                                break
                            }
                    }

                    val details = pagina.getElementById("detailBullets_feature_div")
                    if (details != null) {
                        val tables = details.getElementsByAttribute("li")
                        for (item in tables) {
                            if (!item.text().contains(":"))
                                continue

                            val title = item.text().substringBefore(":").lowercase().trim()
                            when (title) {
                                "出版社" -> {
                                    val value = item.text().substringAfter(":").trim()

                                    if (editoraSite.isEmpty()) {
                                        editoraSite = value.substringAfter(":").substringBefore("(").trim()
                                        editora = editoraSite
                                    }
                                }
                                "発売日" -> {
                                    val value = item.text().substringAfter(":").trim()
                                    if (publicacaoSite.isEmpty()) {
                                        publicacaoSite = value
                                        publicacao = obtemData(value, linguagem)
                                    }
                                }
                                "isbn-13" -> {
                                    if (isbn.isEmpty()) {
                                        val value = item.text().substringAfter(":").trim()
                                        isbn = value
                                    }
                                }
                            }
                        }
                    }
                }
                else -> { }
            }

            if (txtSerie.text.isNullOrEmpty() && serie.isNotEmpty())
                txtSerie.text = serie

            if (txtTitulo.text.isNullOrEmpty())
                txtTitulo.text = titulo

            if (txtEditora.text.isNullOrEmpty())
                txtEditora.text = editora

            if (txtAreaComentario.text.isNullOrEmpty())
                txtAreaComentario.text = comentario

            txtEditoraSite.text = editoraSite
            txtPublicacaoSite.text = publicacaoSite

            if (dpPublicacao.value == null)
                dpPublicacao.value = LocalDate.parse(publicacao.trim())
        } catch (e: Exception) {
            LOGGER.error(e.message, e)
            AlertasPopup.erroModal("Erro ao realizar o processamento do site", e.message.toString())
        }
    }

    private fun configuraListeners() {
        txtSiteAmazon.focusedProperty().addListener { _, oldVal, _ ->
            if (oldVal)
                consulta()
        }

        txtPublicacaoSite.focusedProperty().addListener { _, oldVal, _ ->
            if (oldVal && !txtPublicacaoSite.text.isNullOrEmpty())
                dpPublicacao.value = LocalDate.parse(txtPublicacaoSite.text)
        }

        txtSiteAmazon.setOnKeyPressed { ke -> if (ke.code.equals(KeyCode.ENTER)) robot.keyPress(KeyCode.TAB) }
        cbLinguagem.setOnKeyPressed { ke -> if (ke.code.equals(KeyCode.ENTER)) robot.keyPress(KeyCode.TAB) }
        txtSerie.setOnKeyPressed { ke -> if (ke.code.equals(KeyCode.ENTER)) robot.keyPress(KeyCode.TAB) }
        txtTitulo.setOnKeyPressed { ke -> if (ke.code.equals(KeyCode.ENTER)) robot.keyPress(KeyCode.TAB) }
        txtEditora.setOnKeyPressed { ke -> if (ke.code.equals(KeyCode.ENTER)) robot.keyPress(KeyCode.TAB) }
        txtEditoraSite.setOnKeyPressed { ke -> if (ke.code.equals(KeyCode.ENTER)) robot.keyPress(KeyCode.TAB) }
        dpPublicacao.setOnKeyPressed { ke -> if (ke.code.equals(KeyCode.ENTER)) robot.keyPress(KeyCode.TAB) }
        txtPublicacaoSite.setOnKeyPressed { ke -> if (ke.code.equals(KeyCode.ENTER)) robot.keyPress(KeyCode.TAB) }
    }

    @Override
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        cbLinguagem.items.addAll(Linguagem.PORTUGUESE, Linguagem.ENGLISH, Linguagem.JAPANESE)
        cbLinguagem.selectionModel.selectFirst()
        cbLinguagem.setOnKeyPressed { ke ->
            if (ke.code.equals(KeyCode.ESCAPE))
                cbLinguagem.selectionModel.clearSelection()
            else if (ke.code.equals(KeyCode.ENTER))
                robot.keyPress(KeyCode.TAB)
        }

        configuraListeners()
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(PopupAmazon::class.java)
        private val STYLE_SHEET: String = PopupAmazon::class.java.getResource("/css/Dark_TelaInicial.css").toExternalForm()
        private lateinit var btnConfirmar: JFXButton
        private lateinit var btnVoltar: JFXButton
        private lateinit var dialog: JFXDialog

        fun abreTelaAmazon(rootStackPane: StackPane, nodeBlur: Node, callback: Callback<ComicInfo, Boolean>, item: ComicInfo?, linguagem: Linguagem) {
            try {
                val blur = BoxBlur(3.0, 3.0, 3)
                val dialogLayout = JFXDialogLayout()
                dialog = JFXDialog(rootStackPane, dialogLayout, JFXDialog.DialogTransition.CENTER)
                val loader = FXMLLoader()
                loader.location = fxmlLocate
                val newAnchorPane: Parent = loader.load()
                val cnt: PopupAmazon = loader.getController()
                cnt.objeto = item ?: ComicInfo()
                cnt.setLinguagem(linguagem)
                val titulo = Label("Consulta de dados do site da amazon")
                titulo.font = Font.font(20.0)
                titulo.textFill = Color.web("#ffffff", 0.8)
                val botoes = mutableListOf<JFXButton>()
                btnConfirmar = JFXButton("Confirmar")
                btnConfirmar.setOnAction {
                    callback.call(cnt.objeto)
                    dialog.close()
                }
                btnConfirmar.styleClass.add("background-Green2")
                btnConfirmar.styleClass.add("texto-stilo-1")
                botoes.add(btnConfirmar)
                btnVoltar = JFXButton("Voltar")
                btnVoltar.setOnAction { dialog.close() }
                btnVoltar.styleClass.add("background-White1")
                botoes.add(btnVoltar)
                dialogLayout.setHeading(titulo)
                dialogLayout.setBody(newAnchorPane)
                dialogLayout.setActions(botoes)
                dialogLayout.styleClass.add("dialog-black")
                dialog.stylesheets.add(STYLE_SHEET)
                dialog.padding = Insets(0.0, 0.0, 0.0, 0.0)
                dialog.setOnDialogClosed { nodeBlur.effect = null }
                nodeBlur.effect = blur
                dialog.show()
            } catch (e: IOException) {
                LOGGER.error(e.message, e)
            }
        }

        val fxmlLocate: URL get() = PopupAmazon::class.java.getResource("/view/PopupAmazon.fxml") as URL
    }
}