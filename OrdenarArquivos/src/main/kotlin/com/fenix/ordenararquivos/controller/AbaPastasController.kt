package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.components.NumberTextFieldTableCell
import com.fenix.ordenararquivos.model.entities.Caminhos
import com.fenix.ordenararquivos.model.entities.Manga
import com.fenix.ordenararquivos.model.entities.Pasta
import com.fenix.ordenararquivos.model.entities.comicinfo.AgeRating
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.entities.comicinfo.Mal
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.model.enums.Notificacao
import com.fenix.ordenararquivos.notification.AlertasPopup
import com.fenix.ordenararquivos.notification.Notificacoes
import com.fenix.ordenararquivos.process.Compactar
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.fenix.ordenararquivos.util.Utils
import com.jfoenix.controls.*
import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.concurrent.Task
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.paint.Color
import javafx.util.Callback
import javafx.util.converter.NumberStringConverter
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.properties.Delegates


class AbaPastasController : Initializable {

    private val mLOG = LoggerFactory.getLogger(AbaComicInfoController::class.java)

    //<--------------------------  PRINCIPAL   -------------------------->

    @FXML
    private lateinit var apRoot: AnchorPane

    @FXML
    private lateinit var tbTabRoot: JFXTabPane

    @FXML
    private lateinit var tbTabArquivo: Tab

    @FXML
    private lateinit var tbTabComicInfo: Tab

    @FXML
    private lateinit var cbManga: JFXComboBox<String>

    @FXML
    private lateinit var txtPasta: JFXTextField

    @FXML
    private lateinit var btnPesquisarPasta: JFXButton

    @FXML
    private lateinit var btnCarregar: JFXButton

    @FXML
    private lateinit var btnAplicar: JFXButton

    @FXML
    private lateinit var btnGerarCapas: JFXButton

    //<--------------------------  Arquivos   -------------------------->

    @FXML
    private lateinit var tbViewProcessar: TableView<Pasta>

    @FXML
    private lateinit var clArquivo: TableColumn<Pasta, String>

    @FXML
    private lateinit var clScan: TableColumn<Pasta, String>

    @FXML
    private lateinit var clVolume: TableColumn<Pasta, Number>

    @FXML
    private lateinit var clCapitulo: TableColumn<Pasta, Number>

    @FXML
    private lateinit var clFormatado: TableColumn<Pasta, String>

    //<--------------------------  COMIC INFO   -------------------------->
    @FXML
    private lateinit var txtIdMal: JFXTextField

    @FXML
    private lateinit var cbAgeRating: JFXComboBox<AgeRating>

    @FXML
    private lateinit var cbLinguagem: JFXComboBox<Linguagem>

    @FXML
    private lateinit var txtTitle: JFXTextField

    @FXML
    private lateinit var txtSeries: JFXTextField

    @FXML
    private lateinit var txtAlternateSeries: JFXTextField

    @FXML
    private lateinit var txtSeriesGroup: JFXTextField

    @FXML
    private lateinit var txtPublisher: JFXTextField

    @FXML
    private lateinit var txtStoryArc: JFXTextField

    @FXML
    private lateinit var txtImprint: JFXTextField

    @FXML
    private lateinit var txtGenre: JFXTextField

    @FXML
    private lateinit var txtNotes: JFXTextArea

    //<--------------------------  MAL SEARCH   -------------------------->

    @FXML
    private lateinit var txtMalId: JFXTextField

    @FXML
    private lateinit var txtMalNome: JFXTextField

    @FXML
    private lateinit var btnAmazonConsultar: JFXButton

    @FXML
    private lateinit var btnMalAplicar: JFXButton

    @FXML
    private lateinit var btnMalConsultar: JFXButton

    @FXML
    private lateinit var btnGravarComicInfo: JFXButton

    @FXML
    private lateinit var tbViewMal: TableView<Mal>

    @FXML
    private lateinit var clMalId: TableColumn<Mal, String>

    @FXML
    private lateinit var clMalNome: TableColumn<Mal, String>

    @FXML
    private lateinit var clMalSite: TableColumn<Mal, JFXButton?>

    @FXML
    private lateinit var clMalImagem: TableColumn<Mal, ImageView?>

    private lateinit var controller: TelaInicialController
    var controllerPai: TelaInicialController
        get() = controller
        set(controller) {
            this.controller = controller
        }

    private var mComicInfo by Delegates.observable(ComicInfo()) { _, _, newValue -> carregaComicInfo(newValue) }
    private var mObsListaProcessar: ObservableList<Pasta> = FXCollections.observableArrayList()
    private var mObsListaMal: ObservableList<Mal> = FXCollections.observableArrayList()

    private val mServiceManga = MangaServices()
    private val mServiceComicInfo = ComicInfoServices()

    @FXML
    private fun onBtnCarregar() {
        carregarItens()
    }

    @FXML
    private fun onBtnCarregarPasta() {
        val caminho = Utils.selecionaPasta(txtPasta.text)
        if (caminho != null)
            txtPasta.text = caminho.absolutePath
        else
            txtPasta.text = ""
        carregarItens()
    }

    @FXML
    private fun onBtnAplicar() {
        aplicar()
    }

    @FXML
    private fun onBtnGerarCapas() {
        val decimal = DecimalFormat("00.##", DecimalFormatSymbols(Locale.US))
        var volume = 0f
        val lista = mObsListaProcessar.toMutableList()
        for (item in mObsListaProcessar) {
            if (item.volume.compareTo(volume) != 0 && mObsListaProcessar.none { it.isCapa && it.volume.compareTo(volume) == 0}) {
                volume = item.volume
                val pasta = File(item.pasta.parent, "[${item.scan}] ${item.nome} - Volume ${decimal.format(item.volume)} Capa")
                if (!pasta.exists())
                    Files.createDirectories(pasta.toPath())
                lista.add(Pasta(pasta, pasta.name, pasta.name, item.volume, scan = item.scan, isCapa = true))
            }
        }
        mObsListaProcessar = FXCollections.observableArrayList(mutableListOf())
        tbViewProcessar.items = mObsListaProcessar
        tbViewProcessar.refresh()
    }

    @FXML
    private fun onBtnMalAplicar() {
        if (tbViewMal.items.isNotEmpty())
            carregaMal(tbViewMal.selectionModel.selectedItem)
    }

    @FXML
    private fun onBtnMalConsultar() {
        tbTabRoot.selectionModel.select(tbTabComicInfo)
        consultarMal()
    }

    @FXML
    private fun onBtnAmazonConsultar() {
        val callback: Callback<ComicInfo, Boolean> = Callback<ComicInfo, Boolean> { param ->
            mComicInfo = param
            null
        }
        PopupAmazon.abreTelaAmazon(controllerPai.rootStack, controllerPai.rootTab, callback, mComicInfo, cbLinguagem.value)
    }

    private fun carregaComicInfo(comic: ComicInfo) {
        txtIdMal.text = if (comic.idMal != null) comic.idMal.toString() else ""
        cbAgeRating.selectionModel.select(comic.ageRating)
        val lingua = Linguagem.getEnum(comic.languageISO) ?: cbLinguagem.value
        cbLinguagem.selectionModel.select(lingua)
        txtTitle.text = comic.title
        txtSeries.text = comic.series
        txtAlternateSeries.text = comic.alternateSeries
        txtSeriesGroup.text = comic.seriesGroup
        txtPublisher.text = comic.publisher
        txtStoryArc.text = comic.storyArc
        txtImprint.text = comic.imprint
        txtGenre.text = comic.genre
        txtNotes.text = comic.notes
    }

    fun atualizaComicInfo(comic: ComicInfo) {
        comic.idMal = if (txtIdMal.text.isNotEmpty()) txtIdMal.text.toLong() else null
        comic.ageRating = cbAgeRating.value
        comic.languageISO = cbLinguagem.value.sigla
        comic.title = txtTitle.text
        comic.series = txtSeries.text
        comic.alternateSeries = txtAlternateSeries.text.ifEmpty { null }
        comic.seriesGroup = txtSeriesGroup.text.ifEmpty { null }
        comic.publisher = txtPublisher.text.ifEmpty { null }
        comic.storyArc = txtStoryArc.text.ifEmpty { null }
        comic.imprint = txtImprint.text.ifEmpty { null }
        comic.genre = txtGenre.text.ifEmpty { null }
        comic.notes = txtNotes.text.ifEmpty { null }
    }

    private fun carregaMal(mal: Mal) {
        val comic = ComicInfo(mComicInfo)
        mServiceComicInfo.updateMal(comic, mal, cbLinguagem.value ?: Linguagem.JAPANESE)
        mComicInfo = comic
    }

    private fun consultarMal() {
        if (txtMalId.text.isNotEmpty() || txtMalNome.text.isNotEmpty()) {
            val id : Long? = if (txtMalId.text.isNotEmpty()) txtMalId.text.toLong() else null
            val nome = txtMalNome.text

            btnMalConsultar.isDisable = true
            val consulta: Task<Void> = object : Task<Void>() {
                override fun call(): Void? {
                    try {
                        val lista = mServiceComicInfo.getMal(id, nome)
                        mObsListaMal = FXCollections.observableArrayList(lista)
                        Platform.runLater {
                            tbViewMal.items = mObsListaMal
                            if (lista.isEmpty())
                                Notificacoes.notificacao(Notificacao.ALERTA, "My Anime List", "Nenhum item encontrado.")
                            else if (id != null && lista.size == 1)
                                carregaMal(lista.first())
                        }
                    } catch (e: Exception) {
                        mLOG.info("Erro ao realizar a consulta do MyAnimeList.", e)
                        Platform.runLater {
                            Notificacoes.notificacao(Notificacao.ERRO, "My Anime List", "Erro ao realizar a consulta do MyAnimeList. " + e.message)
                        }
                    }
                    return null
                }
                override fun succeeded() {
                    Platform.runLater {
                        btnMalConsultar.isDisable = false
                    }
                }
            }

            Thread(consulta).start()
        } else {
            AlertasPopup.alertaModal("Alerta", "Necessário informar um id ou nome.")
            txtMalNome.requestFocus()
        }
    }

    private fun carregarItens() {
        val pasta = File(txtPasta.text)
        if (txtPasta.text.isNotEmpty() && pasta.exists()) {
            btnCarregar.isDisable = true

            val processar: Task<Void> = object : Task<Void>() {
                override fun call(): Void? {
                    try {
                        val lista = mutableListOf<Pasta>()

                        val regexCapitulo = "(?i)(chapter|chap| ch\\.?)[ \\d.]+".toRegex()
                        val regexVolume = "(?i)(volume|vol\\.?)[ \\d.]+".toRegex()
                        val apenasNumeros = "^[\\d.]+".toRegex()

                        val max = pasta.listFiles()?.size ?: 0
                        var i = 0

                        for (file in pasta.listFiles()!!) {
                            i++
                            if (file.isFile)
                                continue

                            updateProgress(i.toLong(), max.toLong())
                            updateMessage("Carregando item $i de $max.")

                            val nome = if (file.name.contains(",")) file.name.replace(",", "") else file.name

                            var volume = "0"
                            var capitulos = "0"
                            var scan = ""
                            var titulo = ""

                            if (nome.matches(apenasNumeros))
                                capitulos = nome
                            else {
                                regexCapitulo.find(nome)?.let { match ->
                                    capitulos = match.value.replace(Utils.NOT_NUMBER_PATTERN.toRegex(), "")
                                    val before = nome.substringBefore(match.value).trim()
                                    scan = regexVolume.find(before)?.let { if (it.value.isNotEmpty()) before.substringBefore(it.value) else before } ?: before
                                    titulo = nome.substringAfter(match.value).trim()
                                }
                                regexVolume.find(nome)?.let { match ->
                                    if (match.value.isNotEmpty())
                                        volume = match.value.replace(Utils.NOT_NUMBER_PATTERN.toRegex(), "")
                                }

                                if (scan.contains("_"))
                                    scan = scan.replace("_", " ").trim()

                                if (titulo.contains("_"))
                                    titulo = titulo.replace("_", " ").trim()

                                if (titulo.startsWith("-"))
                                    titulo = titulo.substring(1).trim()
                            }

                            lista.add(Pasta(pasta = file, arquivo = file.name, nome = cbManga.editor.text, volume = volume.toFloatOrNull() ?: 0f, capitulo = capitulos.toFloatOrNull() ?: 0f, scan = scan, titulo = titulo))
                        }

                        mObsListaProcessar = FXCollections.observableArrayList(lista)
                        Platform.runLater { tbViewProcessar.items = mObsListaProcessar }
                    } catch (e: Exception) {
                        mLOG.info("Erro ao carregar pastas.", e)
                        Platform.runLater {
                            Notificacoes.notificacao(Notificacao.ERRO, "Carregar Pastas", "Erro ao carregar pastas. " + e.message)
                        }
                    }
                    return null
                }
                override fun succeeded() {
                    updateMessage("Pastas carregadas com sucesso.")
                    controllerPai.rootProgress.progressProperty().unbind()
                    controllerPai.rootMessage.textProperty().unbind()
                    controllerPai.clearProgress()
                    btnCarregar.isDisable = false
                }
            }
            controllerPai.rootProgress.progressProperty().bind(processar.progressProperty())
            controllerPai.rootMessage.textProperty().bind(processar.messageProperty())
            Thread(processar).start()
        } else {
            AlertasPopup.alertaModal("Alerta", "Necessário informar uma pasta para carregar.")
            txtPasta.requestFocus()
        }
    }

    private fun aplicar() {
        val volume = DecimalFormat("00.##", DecimalFormatSymbols(Locale.US))
        val capitulo = DecimalFormat("000.##", DecimalFormatSymbols(Locale.US))
        val compactar = mutableListOf<File>()
        val comic = mutableMapOf<String, File>()
        val destino = File(txtPasta.text)
        val caminhos = mutableListOf<Caminhos>()
        val nome = cbManga.value
        val manga = Manga()
        manga.nome = nome

        var sequencia = 1
        var vol = 0f
        for (item in mObsListaProcessar) {
            if (item.volume > 0f && item.volume != vol) {
                if (vol > 0f) {
                    manga.volume = volume.format(item.volume)
                    val arquivoZip = destino.path.trim { it <= ' ' } + "\\" + nome.trim { it <= ' ' } + " - Volume " + manga.volume + ".cbr"
                    manga.caminhos = caminhos
                    val callback = Callback<Triple<Long, Long, String>, Boolean> { param -> true }
                    Compactar.compactar(destino, File(arquivoZip), manga, mComicInfo, compactar, comic, Linguagem.PORTUGUESE, isCompactar = true, isGerarCapitulos = true, callback)
                    compactar.clear()
                    comic.clear()
                    caminhos.clear()
                    sequencia = 1
                }
                vol = item.volume
            }

            val pasta = "[${item.scan}] ${item.nome} - Volume ${volume.format(item.volume)} ${if (item.isCapa) "Capa" else "Capítulo " + capitulo.format(item.capitulo)}"
            val path = item.pasta.toPath()
            val file = Files.move(path, path.resolveSibling(pasta), StandardCopyOption.REPLACE_EXISTING).toFile()
            compactar.add(file)
            if (item.isCapa)
                comic["000"] = file
            else
                comic[capitulo.format(item.capitulo)] = file
            caminhos.add(Caminhos(capitulo.format(item.capitulo), sequencia.toString(), "Capítulo " + capitulo.format(item.capitulo), ""))
            sequencia += file.listFiles()?.size ?: 0
        }
        mObsListaProcessar = FXCollections.observableArrayList(mutableListOf())
        tbViewProcessar.items = mObsListaProcessar
        Notificacoes.notificacao(Notificacao.SUCESSO, "Renomear Pastas", "Pastas renomeadas com sucesso.")
    }

    private fun importarVolumes() {
        if (cbManga.editor.text.isNullOrEmpty())
            return

        val formater = DecimalFormat("000.##", DecimalFormatSymbols(Locale.US))
        val mangas = mServiceManga.findAll(cbManga.editor.text)
        val volumes = mangas.associate { it.capitulo to it.volume }
        for (item in mObsListaProcessar)
            item.volume = if (item.capitulo > 0) volumes[formater.format(item.capitulo)]?.toFloatOrNull() ?: 0f else 0f
        tbViewProcessar.refresh()
    }

    private fun configuraTextEdit() {
        txtIdMal.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) Utils.clickTab() }
        cbAgeRating.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) Utils.clickTab() }
        cbLinguagem.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) Utils.clickTab() }
        txtTitle.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) Utils.clickTab() }
        txtSeries.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) Utils.clickTab() }
        txtAlternateSeries.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) Utils.clickTab() }
        txtSeriesGroup.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) Utils.clickTab() }
        txtPublisher.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) Utils.clickTab() }
        txtStoryArc.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) Utils.clickTab() }
        txtImprint.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) Utils.clickTab() }
        txtGenre.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) Utils.clickTab() }

        txtMalId.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) btnMalConsultar.fire() }
        txtMalNome.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) btnMalConsultar.fire() }

        txtIdMal.textProperty().addListener { _: ObservableValue<out String?>?, oldValue: String?, newValue: String? ->
            if (newValue != null && !newValue.matches(Utils.NUMBER_REGEX))
                txtIdMal.text = oldValue
        }

        txtMalId.textProperty().addListener { _: ObservableValue<out String?>?, oldValue: String?, newValue: String? ->
            if (newValue != null && newValue.isNotEmpty() && !newValue.matches(Utils.NUMBER_REGEX))
                txtMalId.text = oldValue
        }
    }

    private fun configuraComboBox() {
        cbAgeRating.items.addAll(AgeRating.values())
        cbLinguagem.items.addAll(Linguagem.JAPANESE, Linguagem.ENGLISH, Linguagem.PORTUGUESE)
        cbLinguagem.selectionModel.select(Linguagem.JAPANESE)

        cbLinguagem.focusedProperty().addListener { _, _, newPropertyValue ->
            if (newPropertyValue)
                cbLinguagem.setUnFocusColor(Color.web("#4059a9"))
            else {
                if (cbLinguagem.value == null)
                    cbLinguagem.setUnFocusColor(Color.RED)
                else
                    cbLinguagem.setUnFocusColor(Color.web("#4059a9"))
            }
        }

        try {
            cbManga.items.setAll(mServiceManga.listar())
        } catch (e: Exception) {
            mLOG.error(e.message, e)
        }

        val autoCompletePopup: JFXAutoCompletePopup<String> = JFXAutoCompletePopup()
        autoCompletePopup.suggestions.addAll(cbManga.items)
        autoCompletePopup.setSelectionHandler { event -> cbManga.setValue(event.getObject()) }
        cbManga.editor.textProperty().addListener { _, _, _ ->
            autoCompletePopup.filter { item -> item.lowercase(Locale.getDefault()).contains(cbManga.editor.text.lowercase(Locale.getDefault())) }
            if (autoCompletePopup.filteredSuggestions.isEmpty() || cbManga.showingProperty().get() || cbManga.editor.text.isEmpty())
                autoCompletePopup.hide()
            else
                autoCompletePopup.show(cbManga.editor)
        }
        cbManga.setOnKeyPressed { ke -> if (ke.code.equals(KeyCode.ENTER)) Utils.clickTab() }
        cbManga.focusedProperty().addListener { _, oldValue, _ ->
            if (oldValue) {
                for (item in mObsListaProcessar)
                    item.nome = cbManga.value
                tbViewProcessar.refresh()
            }
        }
    }

    private fun editaColunas() {
        clScan.cellFactory = TextFieldTableCell.forTableColumn()
        clScan.setOnEditCommit { e ->
            e.tableView.items[e.tablePosition.row].scan = e.newValue
            tbViewProcessar.refresh()
        }
        clVolume.cellFactory = NumberTextFieldTableCell.forTableColumn(NumberStringConverter(Locale.US, "00.##"))
        clVolume.setOnEditCommit { e ->
            e.tableView.items[e.tablePosition.row].volume = e.newValue.toFloat()
            tbViewProcessar.refresh()
        }
        clCapitulo.cellFactory = NumberTextFieldTableCell.forTableColumn(NumberStringConverter(Locale.US, "000.##"))
        clCapitulo.setOnEditCommit { e ->
            e.tableView.items[e.tablePosition.row].capitulo = e.newValue.toFloat()
            tbViewProcessar.refresh()
        }
    }

    private fun linkaCelulas() {
        clArquivo.cellValueFactory = PropertyValueFactory("arquivo")
        clScan.cellValueFactory = PropertyValueFactory("scan")
        clVolume.cellValueFactory = PropertyValueFactory("volume")
        clCapitulo.cellValueFactory = PropertyValueFactory("capitulo")

        val volume = DecimalFormat("00.##", DecimalFormatSymbols(Locale.US))
        val capitulo = DecimalFormat("000.##", DecimalFormatSymbols(Locale.US))

        clFormatado.setCellValueFactory { param -> SimpleStringProperty("[${param.value.scan}] ${param.value.nome} - Volume ${volume.format(param.value.volume)} Capítulo ${capitulo.format(param.value.capitulo)}") }

        val menu = ContextMenu()
        val scanProximos = MenuItem("Aplicar scan nos arquivos próximos")
        scanProximos.setOnAction {
            tbViewProcessar.selectionModel.selectedItem?.run {
                val scan = this.scan
                val index = mObsListaProcessar.indexOf(this)
                if (scan.isNotEmpty() && index < tbViewProcessar.items.size - 1) {
                    for (i in index + 1 until tbViewProcessar.items.size)
                        mObsListaProcessar[i].scan = scan
                    tbViewProcessar.refresh()
                }
            }
        }
        val scanAnterior = MenuItem("Aplicar scan nos arquivos anteriores")
        scanAnterior.setOnAction {
            tbViewProcessar.selectionModel.selectedItem?.run {
                val scan = this.scan
                val index = mObsListaProcessar.indexOf(this)
                if (scan.isNotEmpty() && index > 0) {
                    for (i in index - 1 downTo 0)
                        mObsListaProcessar[i].scan = scan
                    tbViewProcessar.refresh()
                }
            }
        }
        val volumesZerar = MenuItem("Zerar volumes")
        volumesZerar.setOnAction {
            mObsListaProcessar.forEach { item -> item.volume = 0f }
            tbViewProcessar.refresh()
        }
        val volumesImportar = MenuItem("Importar volumes")
        volumesImportar.setOnAction { importarVolumes() }
        val remover = MenuItem("Remover registro")
        remover.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null)
                if (AlertasPopup.confirmacaoModal("Aviso", "Deseja remover o registro?")) {
                    mObsListaProcessar.remove(tbViewProcessar.selectionModel.selectedItem)
                    tbViewProcessar.refresh()
                }
        }
        menu.items.add(scanProximos)
        menu.items.add(scanAnterior)
        menu.items.add(volumesZerar)
        menu.items.add(volumesImportar)
        menu.items.add(remover)

        tbViewProcessar.contextMenu = menu

        clMalId.cellValueFactory = PropertyValueFactory("idVisual")
        clMalNome.cellValueFactory = PropertyValueFactory("nome")
        clMalSite.cellValueFactory = PropertyValueFactory("site")
        clMalImagem.cellValueFactory = PropertyValueFactory("imagem")

        tbViewMal.onMouseClicked = EventHandler { click: MouseEvent ->
            if (click.clickCount > 1 && tbViewMal.items.isNotEmpty())
                carregaMal(tbViewMal.selectionModel.selectedItem)
        }

        editaColunas()
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        linkaCelulas()
        configuraTextEdit()
        configuraComboBox()
    }

    companion object {
        val fxmlLocate: URL get() = TelaInicialController::class.java.getResource("/view/AbaPastas.fxml")
    }

}