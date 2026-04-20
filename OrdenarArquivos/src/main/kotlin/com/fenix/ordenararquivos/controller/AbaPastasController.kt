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
import com.fenix.ordenararquivos.model.enums.Selecionado
import com.fenix.ordenararquivos.model.enums.TipoCapa
import com.fenix.ordenararquivos.notification.AlertasPopup
import com.fenix.ordenararquivos.notification.Notificacoes
import com.fenix.ordenararquivos.process.Winrar
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.fenix.ordenararquivos.service.PastaParsingService
import com.fenix.ordenararquivos.util.Utils
import com.jfoenix.controls.*
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.concurrent.Task
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.image.ImageView
import javafx.scene.input.*
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
    private lateinit var tbTabRootPastas: JFXTabPane

    @FXML
    private lateinit var tbTabPastas_Arquivos: Tab

    @FXML
    private lateinit var tbTabPastas_ComicInfo: Tab

    @FXML
    private lateinit var cbManga: JFXComboBox<String>

    @FXML
    private lateinit var txtPasta: JFXTextField

    @FXML
    private lateinit var btnPesquisarPasta: JFXButton

    @FXML
    private lateinit var btnCarregar: JFXButton

    @FXML
    private lateinit var btnRenomear: JFXButton

    @FXML
    private lateinit var btnGerarCapas: JFXButton

    @FXML
    private lateinit var btnImportarVolumes: JFXButton

    @FXML
    private lateinit var btnCompactar: JFXButton

    //<--------------------------  Arquivos   -------------------------->

    @FXML
    private lateinit var ckbSelecionarTodos: JFXCheckBox

    @FXML
    private lateinit var tbViewProcessar: TableView<Pasta>

    @FXML
    private lateinit var clSelecionado: TableColumn<Pasta, Boolean>

    @FXML
    private lateinit var clArquivo: TableColumn<Pasta, String>

    @FXML
    private lateinit var clScan: TableColumn<Pasta, String>

    @FXML
    private lateinit var clVolume: TableColumn<Pasta, Number>

    @FXML
    private lateinit var clCapitulo: TableColumn<Pasta, Number>

    @FXML
    private lateinit var clTitulo: TableColumn<Pasta, String>

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

    internal val mServiceManga = MangaServices()
    internal val mServiceComicInfo = ComicInfoServices()

    @FXML
    private fun onBtnSelecionarTodos() {
        mObsListaProcessar.forEach { it.isSelecionado = ckbSelecionarTodos.isSelected }
        tbViewProcessar.refresh()
    }

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
    private fun onBtnRenomear() {
        renomear()
    }

    @FXML
    private fun onBtnImportarVolumes() {
        importarVolumes()
    }

    @FXML
    private fun onBtnGerarCapas() {
        val decimal = DecimalFormat("00.##", DecimalFormatSymbols(Locale.US))
        var volume = 0f
        val lista = mutableListOf<Pasta>()
        for (item in mObsListaProcessar) {
            if (item.volume.compareTo(volume) != 0 && mObsListaProcessar.none { it.isCapa && it.volume.compareTo(volume) == 0} && !item.isCapa) {
                volume = item.volume
                val pasta = File(item.pasta.parent, "[${item.scan}] ${item.nome} - Volume ${decimal.format(item.volume)} Capa")
                if (!pasta.exists())
                    Files.createDirectories(pasta.toPath())
                lista.add(Pasta(pasta, pasta.name, item.nome, item.volume, scan = item.scan, isCapa = true, isSelecionado = true))
            }

            lista.add(item)
        }
        mObsListaProcessar = FXCollections.observableArrayList(lista)
        tbViewProcessar.items = mObsListaProcessar
        tbViewProcessar.refresh()
    }

    @FXML
    private fun onBtnMalAplicar() {
        if (tbViewMal.selectionModel.selectedItem != null) {
            val mal = tbViewMal.selectionModel.selectedItem
            controllerPai.setCursor(Cursor.WAIT)
            btnMalAplicar.isDisable = true

            val task = object : Task<Void>() {
                override fun call(): Void? {
                    carregaMal(mal)
                    return null
                }

                override fun succeeded() {
                    controllerPai.setCursor(null)
                    btnMalAplicar.isDisable = false
                }

                override fun failed() {
                    controllerPai.setCursor(null)
                    btnMalAplicar.isDisable = false
                    mLOG.error("Erro ao aplicar MAL", exception)
                }
            }
            Thread(task).start()
        }
    }

    @FXML
    private fun onBtnMalConsultar() {
        tbTabRootPastas.selectionModel.select(tbTabPastas_ComicInfo)
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

    @FXML
    private fun onBtnCompactar() {
        if (btnCompactar.accessibleTextProperty().value.equals("COMPACTAR", ignoreCase = true)) {
            val listaSelecionada = mObsListaProcessar.filter { it.isSelecionado }
            if (listaSelecionada.isEmpty()) {
                AlertasPopup.alertaModal("Alerta", "Nenhum item selecionado para compactar.")
                return
            }

            val pastaTexto = txtPasta.text
            if (pastaTexto.isNullOrEmpty()) {
                txtPasta.unFocusColor = Color.RED
                AlertasPopup.alertaModal("Alerta", "Não informado a pasta para processamento.")
                return
            }

            val mangaProcessar = cbManga.value
            if (mangaProcessar.isNullOrEmpty()) {
                cbManga.unFocusColor = Color.RED
                AlertasPopup.alertaModal("Alerta", "Não informado o nome do manga.")
                return
            }

            btnCompactar.accessibleTextProperty().set("CANCELA")
            btnCompactar.text = "Cancelar"
            controllerPai.setCursor(Cursor.WAIT)
            desabilita()

            val task = object : Task<Void>() {
                override fun call(): Void? {
                    try {
                        mCANCELAR = false
                        val volumes = listaSelecionada.groupBy { it.volume }.toSortedMap()
                        val volumeFormat = DecimalFormat("00.##", DecimalFormatSymbols(Locale.US))
                        val capituloFormat = DecimalFormat("000.##", DecimalFormatSymbols(Locale.US))
                        val destino = File(pastaTexto)

                        val totalVolumes = volumes.size
                        var volIndex = 0

                        for ((vol, itens) in volumes) {
                            volIndex++
                            val message = "Compactando volume ${volumeFormat.format(vol)} ($volIndex de $totalVolumes)"
                            updateMessage(message)

                            val compactar = mutableListOf<File>()
                            val comicMap = mutableMapOf<String, File>()
                            val caminhos = mutableListOf<Caminhos>()
                            val manga = Manga()
                            manga.nome = mangaProcessar
                            manga.volume = volumeFormat.format(vol)

                            val itensOrdenados = itens.sortedWith(compareBy({ !it.isCapa }, { it.capitulo }))

                            var sequencia = 1
                            for (item in itensOrdenados) {
                                compactar.add(item.pasta)
                                val key = if (item.isCapa) "000" else capituloFormat.format(item.capitulo)
                                comicMap[key] = item.pasta
                                if (!item.isCapa) {
                                    caminhos.add(
                                        Caminhos(
                                            capituloFormat.format(item.capitulo),
                                            sequencia.toString(),
                                            "Capítulo " + capituloFormat.format(item.capitulo),
                                            item.titulo
                                        )
                                    )
                                    sequencia += item.pasta.listFiles()?.size ?: 0
                                }
                            }
                            manga.caminhos = caminhos

                            val volume = volumeFormat.format(vol)
                            val tudo = if (comicMap.contains("000")) { comicMap["000"]?.listFiles()?.any { it.name.contains("tudo", true) } == true } else false
                            val semCapa = if (!tudo) " (Sem capa)" else ""
                            val nome = "$mangaProcessar - Volume $volume$semCapa.rar"
                            val arquivoZip = File(destino, nome)

                            val callback = Callback<Triple<Long, Long, String>, Boolean> { param ->
                                if (param.first == -1L)
                                    updateMessage(message + " -- " + param.third)
                                else {
                                    updateProgress(param.first, param.second)
                                    updateMessage(message + " -- " + param.third)
                                }
                                mCANCELAR
                            }

                            mComicInfo.volume = vol.toInt()
                            mComicInfo.number = vol
                            mComicInfo.count = vol.toInt()
                            Winrar.compactar(
                                destino, arquivoZip, manga, mComicInfo, compactar, comicMap, cbLinguagem.value ?: Linguagem.PORTUGUESE,
                                isCompactar = true, isGerarCapitulos = true, isAtualizarComic = false, callback
                            )

                            val generatedXml = File(destino, "ComicInfo.xml")
                            if (generatedXml.exists()) {
                                val newXmlName = "$mangaProcessar - Volume $volume - comicinfo.xml"
                                generatedXml.renameTo(File(destino, newXmlName))
                            }
                        }

                        mServiceComicInfo.save(mComicInfo)
                        Platform.runLater {
                            Notificacoes.notificacao(Notificacao.SUCESSO, "Compactar", "Arquivos compactados com sucesso.")
                        }
                    } catch (e: Exception) {
                        mLOG.error("Erro ao compactar.", e)
                        Platform.runLater {
                            Notificacoes.notificacao(Notificacao.ERRO, "Compactar", "Erro ao compactar: ${e.message}")
                        }
                    }
                    return null
                }

                override fun succeeded() {
                    updateMessage("Compactação Finalizada.")
                    controllerPai.rootProgress.progressProperty().unbind()
                    controllerPai.rootMessage.textProperty().unbind()
                    controllerPai.clearProgress()
                    habilita()
                }

                override fun failed() {
                    controllerPai.rootProgress.progressProperty().unbind()
                    controllerPai.rootMessage.textProperty().unbind()
                    controllerPai.clearProgress()
                    habilita()
                }
            }
            controllerPai.rootProgress.progressProperty().bind(task.progressProperty())
            controllerPai.rootMessage.textProperty().bind(task.messageProperty())
            Thread(task).start()
        } else
            mCANCELAR = true
    }

    private var mCANCELAR = false

    private fun desabilita(isCompactar : Boolean = false) {
        btnPesquisarPasta.isDisable = true
        txtPasta.isDisable = true
        cbManga.isDisable = true
        btnCarregar.isDisable = true
        btnGerarCapas.isDisable = true
        btnRenomear.isDisable = true
        tbViewProcessar.isDisable = true
        ckbSelecionarTodos.isDisable = true

        if (!isCompactar)
            btnCompactar.isDisable = true
    }

    private fun habilita() {
        btnPesquisarPasta.isDisable = false
        txtPasta.isDisable = false
        cbManga.isDisable = false
        btnCarregar.isDisable = false
        btnGerarCapas.isDisable = false
        btnRenomear.isDisable = false
        btnCompactar.isDisable = false
        tbViewProcessar.isDisable = false
        ckbSelecionarTodos.isDisable = false
        controllerPai.setCursor(null)

        btnCompactar.accessibleTextProperty().set("COMPACTAR")
        btnCompactar.text = "Compactar"
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

        txtMalId.text = comic.idMal?.toString() ?: ""
        txtMalNome.text = comic.comic
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

        Platform.runLater {
            mComicInfo = comic
            val selecionado = when {
                mComicInfo.idMal != null -> Selecionado.SELECIONADO
                mObsListaMal.isNotEmpty() -> Selecionado.SELECIONAR
                else -> Selecionado.VAZIO
            }
            Selecionado.setTabColor(tbTabPastas_ComicInfo, selecionado)
        }
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
                        if (id != null && lista.size == 1) {
                            carregaMal(lista.first())
                        }

                        Platform.runLater {
                            tbViewMal.items = mObsListaMal
                            if (lista.isEmpty())
                                Notificacoes.notificacao(Notificacao.ALERTA, "My Anime List", "Nenhum item encontrado.")

                            val selecionado = when {
                                mComicInfo.idMal != null -> Selecionado.SELECIONADO
                                mObsListaMal.isNotEmpty() -> Selecionado.SELECIONAR
                                else -> Selecionado.VAZIO
                            }
                            Selecionado.setTabColor(tbTabPastas_ComicInfo, selecionado)
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

                        val parsingService = PastaParsingService()
                        val max = pasta.listFiles()?.size?.toLong() ?: 0L
                        var i = 0L

                        for (file in pasta.listFiles()!!) {
                            i++
                            if (file.isFile)
                                continue

                            updateProgress(i, max)
                            updateMessage("Carregando item $i de $max.")

                            val result = parsingService.parse(file.name)

                            lista.add(Pasta(
                                pasta = file, 
                                arquivo = file.name, 
                                nome = cbManga.editor.text, 
                                volume = result.volume.toFloatOrNull() ?: 0f, 
                                capitulo = result.capitulo.toFloatOrNull() ?: 0f, 
                                scan = result.scan, 
                                titulo = result.titulo, 
                                isCapa = result.isCapa,
                                isSelecionado = true
                            ))
                        }


                        Platform.runLater {
                            mObsListaProcessar = FXCollections.observableArrayList(lista)
                            tbViewProcessar.items = mObsListaProcessar
                            ckbSelecionarTodos.isSelected = true
                        }
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

    private fun renomear() {
        val pastaTexto = txtPasta.text
        if (pastaTexto.isNullOrEmpty()) {
            txtPasta.unFocusColor = Color.RED
            AlertasPopup.alertaModal("Alerta", "Não informado a pasta para processamento.")
            return
        }

        val listaOriginal = mObsListaProcessar.toList()
        if (listaOriginal.isEmpty()) {
            AlertasPopup.alertaModal("Alerta", "Não existem itens para processar.")
            return
        }

        desabilita()
        val processar: Task<Void> = object : Task<Void>() {
            override fun call(): Void? {
                try {
                    val volume = DecimalFormat("00.##", DecimalFormatSymbols(Locale.US))
                    val capitulo = DecimalFormat("000.##", DecimalFormatSymbols(Locale.US))

                    var i = 0L
                    val max = listaOriginal.size.toLong()

                    for ((index, item) in listaOriginal.sortedBy { it.volume }.withIndex()) {
                        updateProgress(++i, max)
                        updateMessage("Renomeando item $i de $max.")

                        val pasta = "[${item.scan}] ${item.nome} - Volume ${volume.format(item.volume)} ${if (item.isCapa) "Capa" else "Capítulo " + capitulo.format(item.capitulo)}"
                        val path = item.pasta.toPath()
                        val target = path.resolveSibling(pasta)
                        
                        if (path != target)
                            Files.move(path, target, StandardCopyOption.REPLACE_EXISTING).toFile()
                    }

                    Platform.runLater {
                        carregarItens()
                        Notificacoes.notificacao(Notificacao.SUCESSO, "Renomear Pastas", "Pastas renomeadas com sucesso.")
                    }
                } catch (e: Exception) {
                    mLOG.info("Erro ao processar pastas.", e)
                    Platform.runLater {
                        Notificacoes.notificacao(Notificacao.ERRO, "Renomear Pastas", "Erro ao renomear pastas. " + e.message)
                    }
                }
                return null
            }
            override fun succeeded() {
                controllerPai.rootProgress.progressProperty().unbind()
                controllerPai.rootMessage.textProperty().unbind()
                controllerPai.clearProgress()
                habilita()
            }
        }
        controllerPai.rootProgress.progressProperty().bind(processar.progressProperty())
        controllerPai.rootMessage.textProperty().bind(processar.messageProperty())
        Thread(processar).start()
    }

    private fun importarVolumes() {
        if (cbManga.editor.text.isNullOrEmpty())
            return

        val mangas = mServiceManga.findAll(cbManga.editor.text, isCaminho = true)
        val volumes = mutableMapOf<String, String>()
        mangas.forEach { m -> volumes.putAll(m.caminhos.associate { c -> c.capitulo to m.volume }) }
        val formater = DecimalFormat("000.##", DecimalFormatSymbols(Locale.US))
        val letras = Utils.NOT_NUMBER_PATTERN.toRegex()
        for (item in mObsListaProcessar)
            item.volume = if (item.capitulo > 0) volumes[formater.format(item.capitulo)]?.replace(letras,"")?.toFloatOrNull() ?: 0f else 0f
        tbViewProcessar.refresh()
    }

    fun configurarAtalhos(scene: Scene) {
        val kcComicInfo: KeyCombination = KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN)
        val kcArquivos: KeyCombination = KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN)

        scene.addEventFilter(KeyEvent.KEY_PRESSED) { ke: KeyEvent ->
            if (kcComicInfo.match(ke)) {
                if (tbTabRootPastas.selectionModel.selectedItem == tbTabPastas_ComicInfo) {
                    if (txtMalId.text.isEmpty() && txtMalNome.text.isEmpty())
                        txtMalNome.text = cbManga.value

                    if (isAbaSelecionada)
                        btnMalConsultar.fire()
                } else
                    tbTabRootPastas.selectionModel.select(tbTabPastas_ComicInfo)
            }

            if (kcArquivos.match(ke)) {
                if (tbTabRootPastas.selectionModel.selectedItem != tbTabPastas_Arquivos)
                    tbTabRootPastas.selectionModel.select(tbTabPastas_Arquivos)
            }
        }
    }

    private fun configuraTextEdit() {
        var oldPasta = ""
        txtPasta.focusedProperty().addListener { _: ObservableValue<out Boolean>?, oldPropertyValue: Boolean, newPropertyValue: Boolean ->
            if (newPropertyValue)
                oldPasta = txtPasta.text

            if (oldPropertyValue && txtPasta.text.compareTo(oldPasta, ignoreCase = true) != 0)
                carregarItens()

            txtPasta.unFocusColor = Color.web("#4059a9")
        }

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
        cbLinguagem.selectionModel.select(Linguagem.PORTUGUESE)

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

        var oldManga = ""
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
        cbManga.focusedProperty().addListener { _, oldValue, newValue ->
            if (newValue)
                oldManga = cbManga.value ?: ""

            if (oldValue && oldManga != cbManga.value) {
                for (item in mObsListaProcessar)
                    item.nome = cbManga.value

                mComicInfo = if (cbManga.value.isNullOrEmpty())
                    ComicInfo(null, null, cbManga.value, cbManga.value)
                else {
                    val comic = mServiceComicInfo.find(cbManga.value, cbLinguagem.value.sigla) ?: mServiceComicInfo.find(cbManga.value) ?: ComicInfo(null, null, cbManga.value, cbManga.value)
                    if (comic.languageISO != cbLinguagem.value.sigla) {
                        comic.id = null
                        comic.languageISO = cbLinguagem.value.sigla
                    }
                    comic
                }

                if (!cbManga.value.isNullOrEmpty()) {
                    val mangas = mServiceManga.findAll(cbManga.value, isCaminho = true)
                    val volumesMap = mutableMapOf<String, Float>()
                    val decimal = DecimalFormat("000.##", DecimalFormatSymbols(Locale.US))
                    mangas.forEach { m ->
                        val volFloat = m.volume.replace(Utils.NOT_NUMBER_PATTERN.toRegex(), "").toFloatOrNull() ?: 0f
                        m.caminhos.forEach { c -> volumesMap[c.capitulo] = volFloat }
                    }

                    for (item in mObsListaProcessar) {
                        if (item.volume == 0f && item.capitulo > 0f) {
                            val capStr = decimal.format(item.capitulo)
                            if (volumesMap.containsKey(capStr))
                                item.volume = volumesMap[capStr]!!
                        }
                    }
                }
                tbViewProcessar.refresh()
            }

            cbManga.unFocusColor = Color.web("#4059a9")
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
        clTitulo.cellFactory = TextFieldTableCell.forTableColumn()
        clTitulo.setOnEditCommit { e ->
            e.tableView.items[e.tablePosition.row].titulo = e.newValue
            tbViewProcessar.refresh()
        }
    }

    private fun linkaCelulas() {
        clArquivo.cellValueFactory = PropertyValueFactory("arquivo")
        clScan.cellValueFactory = PropertyValueFactory("scan")
        clVolume.cellValueFactory = PropertyValueFactory("volume")
        clCapitulo.cellValueFactory = PropertyValueFactory("capitulo")
        clTitulo.cellValueFactory = PropertyValueFactory("titulo")

        clSelecionado.setCellValueFactory { param ->
            val booleanProp = SimpleBooleanProperty(param.value.isSelecionado)
            booleanProp.addListener { _, _, newValue ->
                param.value.isSelecionado = newValue
            }
            booleanProp
        }

        clSelecionado.setCellFactory {
            val cell = com.fenix.ordenararquivos.components.CheckBoxTableCellCustom<Pasta, Boolean>()
            cell.alignment = Pos.CENTER
            cell
        }

        val volume = DecimalFormat("00.##", DecimalFormatSymbols(Locale.US))
        val capitulo = DecimalFormat("000.##", DecimalFormatSymbols(Locale.US))

        clFormatado.setCellValueFactory { param -> SimpleStringProperty("[${param.value.scan}] ${param.value.nome} - Volume ${volume.format(param.value.volume)} ${if (param.value.isCapa) "Capa" else "Capítulo " + capitulo.format(param.value.capitulo)}") }

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

        val apagarTituloProximos = MenuItem("Apagar titulos nos arquivos proximos")
        apagarTituloProximos.setOnAction {
            tbViewProcessar.selectionModel.selectedItem?.run {
                val index = mObsListaProcessar.indexOf(this)
                if (index != -1) {
                    for (i in index until mObsListaProcessar.size)
                        mObsListaProcessar[i].titulo = ""
                    tbViewProcessar.refresh()
                }
            }
        }
        val apagarTituloAnteriores = MenuItem("Apagar titulos nos arquivos anteriores")
        apagarTituloAnteriores.setOnAction {
            tbViewProcessar.selectionModel.selectedItem?.run {
                val index = mObsListaProcessar.indexOf(this)
                if (index != -1) {
                    for (i in index downTo 0)
                        mObsListaProcessar[i].titulo = ""
                    tbViewProcessar.refresh()
                }
            }
        }

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
        menu.items.add(apagarTituloProximos)
        menu.items.add(apagarTituloAnteriores)
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
        var isAbaSelecionada = false
    }

}