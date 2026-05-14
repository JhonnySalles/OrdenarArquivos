package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.components.NumberTextFieldTableCell
import com.fenix.ordenararquivos.configuration.Configuracao
import com.fenix.ordenararquivos.model.entities.Caminhos
import com.fenix.ordenararquivos.model.entities.Manga
import com.fenix.ordenararquivos.model.entities.Pasta
import com.fenix.ordenararquivos.model.entities.Processar
import com.fenix.ordenararquivos.model.entities.capitulos.Volume
import com.fenix.ordenararquivos.model.entities.comicinfo.AgeRating
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.entities.comicinfo.Mal
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.model.enums.Notificacao
import com.fenix.ordenararquivos.model.enums.Selecionado
import com.fenix.ordenararquivos.notification.AlertasModal
import com.fenix.ordenararquivos.notification.ConfirmaModal
import com.fenix.ordenararquivos.notification.Notificacoes
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.fenix.ordenararquivos.service.PastaParsingService
import com.fenix.ordenararquivos.service.WinrarServices
import com.fenix.ordenararquivos.util.Utils
import com.jfoenix.controls.*
import jakarta.xml.bind.JAXBContext
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
import javafx.scene.effect.BoxBlur
import javafx.scene.image.ImageView
import javafx.scene.input.*
import javafx.scene.layout.AnchorPane
import javafx.scene.paint.Color
import javafx.util.Callback
import javafx.util.converter.NumberStringConverter
import org.slf4j.LoggerFactory
import com.fenix.ordenararquivos.util.GridHistoryManager
import com.fenix.ordenararquivos.util.PropertyChangeAction
import com.fenix.ordenararquivos.util.CompositeAction
import com.fenix.ordenararquivos.util.ReversibleAction
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import javafx.css.PseudoClass
import javafx.scene.image.Image
import kotlin.properties.Delegates

class AbaPastasController : Initializable {

    private val mLOG = LoggerFactory.getLogger(AbaComicInfoController::class.java)
    private val ALERTA_PSEUDO_CLASS = PseudoClass.getPseudoClass("alerta")

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
    private lateinit var btnValidar: JFXButton

    @FXML
    private lateinit var btnRenomear: JFXButton

    @FXML
    private lateinit var btnGerarCapas: JFXButton

    @FXML
    private lateinit var btnImportarVolumes: JFXButton

    @FXML
    private lateinit var btnCompactar: JFXButton

    @FXML
    private lateinit var btnAjustarPastas: JFXButton

    @FXML
    private lateinit var cbApagarArquivo: JFXCheckBox
    
    @FXML
    private lateinit var btnCapitulos: JFXButton

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
    private lateinit var clMalTipo: TableColumn<Mal, String>
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

    internal var mComicInfo by Delegates.observable(ComicInfo()) { _, _, newValue -> carregaComicInfo(newValue) }
    internal var mObsListaProcessar: ObservableList<Pasta> = FXCollections.observableArrayList()
    private var mObsListaMal: ObservableList<Mal> = FXCollections.observableArrayList()
    private val mHistory = GridHistoryManager()

    internal var mServiceManga = MangaServices()
    internal var mServiceComicInfo = ComicInfoServices()
    internal var mRarService = WinrarServices()
    internal var contextMenu: ContextMenu? = null
    private val mPASTA_TEMPORARIA = File(System.getProperty("user.dir"), "temp/")
    private var isConsultandoMal = false
    private var volumeBuffer = ""
    private var chapterBuffer = ""

    @FXML
    private fun onBtnSelecionarTodos() {
        mObsListaProcessar.forEach { it.isSelecionado = ckbSelecionarTodos.isSelected }
        tbViewProcessar.refresh()
    }

    private fun atualizaCheckSelecionarTodos() {
        ckbSelecionarTodos.isSelected = mObsListaProcessar.isNotEmpty() && mObsListaProcessar.all { it.isSelecionado }
    }

    @FXML
    private fun onBtnCarregar() {
        carregarItens()
    }

    @FXML
    private fun onBtnValidar() {
        if (btnValidar.accessibleTextProperty().value.equals("VALIDAR", ignoreCase = true)) {
            desabilita(btnValidar)
            controllerPai.setCursor(Cursor.WAIT)
            validarRegistros()
            habilita(btnValidar, "Validar")
        }
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
    private fun onBtnCapitulos() {
        val selected = tbViewProcessar.selectionModel.selectedItems
        val listToProcess = if (selected.size > 1) selected.toList() else mObsListaProcessar

        val callback: Callback<ObservableList<Volume>, Boolean> = Callback<ObservableList<Volume>, Boolean> { param ->
            val linguagem = cbLinguagem.value ?: Linguagem.PORTUGUESE
            for (volume in param)
                if (volume.marcado && volume.arquivo.isNotEmpty()) {
                    val item = mObsListaProcessar.find { it.arquivo == volume.arquivo } ?: continue
                    val capituloInfo = volume.capitulos.firstOrNull()
                    if (capituloInfo != null) {
                        item.titulo = if (linguagem == Linguagem.JAPANESE && capituloInfo.japones.isNotEmpty())
                            capituloInfo.japones
                        else
                            capituloInfo.ingles
                    }
                }
            tbViewProcessar.refresh()
            null
        }

        val processarList = listToProcess.map { item ->
            Processar(arquivo = item.arquivo, file = item.pasta, tags = "")
        }

        PopupCapitulosController.abreTelaCapitulos(controllerPai.rootStack, controllerPai.rootTab, callback, cbLinguagem.value ?: Linguagem.PORTUGUESE, processarList)
    }

    @FXML
    private fun onBtnGerarCapas() {
        if (btnGerarCapas.accessibleTextProperty().value.equals("GERAR", ignoreCase = true)) {
            val decimal = DecimalFormat("00.##", DecimalFormatSymbols(Locale.US))
            val volumes = mObsListaProcessar.filter { !it.isCapa }.map { it.volume }.distinct()
            val lista = mObsListaProcessar.toMutableList()

            desabilita(btnGerarCapas)
            controllerPai.setCursor(Cursor.WAIT)

            val task = object : Task<Void>() {
                override fun call(): Void? {
                    mCANCELAR = false
                    val total = volumes.size.toLong()
                    var atual = 0L

                    for (vol in volumes) {
                        if (mCANCELAR) break
                        atual++
                        updateProgress(atual, total)
                        updateMessage("Gerando capa para o volume ${decimal.format(vol)}")

                        val jaExiste = lista.any { it.isCapa && it.volume == vol }
                        if (!jaExiste) {
                            val primeiroDoVolume = lista.firstOrNull { !it.isCapa && it.volume == vol } ?: continue
                            val nomePasta = "[${primeiroDoVolume.scan}] ${primeiroDoVolume.nome} - Volume ${decimal.format(vol)} Capa"
                            val pasta = File(primeiroDoVolume.pasta.parent, nomePasta)

                            if (!pasta.exists())
                                Files.createDirectories(pasta.toPath())

                            lista.add(Pasta(pasta, pasta.name, primeiroDoVolume.nome, vol, scan = primeiroDoVolume.scan, isCapa = true, isSelecionado = true))
                        }
                    }

                    if (!mCANCELAR) {
                        Platform.runLater {
                            mObsListaProcessar = FXCollections.observableArrayList(lista)
                            mObsListaProcessar.sortWith(compareBy({ it.volume }, { it.capitulo }))
                            tbViewProcessar.items = mObsListaProcessar
                            tbViewProcessar.refresh()
                            validarRegistros()
                        }
                    }
                    return null
                }

                override fun succeeded() {
                    controllerPai.rootProgress.progressProperty().unbind()
                    controllerPai.rootMessage.textProperty().unbind()
                    controllerPai.clearProgress()
                    habilita(btnGerarCapas, "Gerar Capas")
                }

                override fun failed() {
                    controllerPai.rootProgress.progressProperty().unbind()
                    controllerPai.rootMessage.textProperty().unbind()
                    controllerPai.clearProgress()
                    habilita(btnGerarCapas, "Gerar Capas")
                }
            }
            controllerPai.rootProgress.progressProperty().bind(task.progressProperty())
            controllerPai.rootMessage.textProperty().bind(task.messageProperty())
            Thread(task).start()
        } else
            mCANCELAR = true
    }

    @FXML
    private fun onBtnMalAplicar() {
        if (tbViewMal.selectionModel.selectedItem != null) {
            val mal = tbViewMal.selectionModel.selectedItem
            val linguagem = cbLinguagem.value ?: Linguagem.JAPANESE
            val comicInfo = ComicInfo(mComicInfo)

            controllerPai.setCursor(Cursor.WAIT)
            btnMalAplicar.isDisable = true

            val task = object : Task<Void>() {
                override fun call(): Void? {
                    mServiceComicInfo.updateMal(comicInfo, mal, linguagem)
                    return null
                }

                override fun succeeded() {
                    mComicInfo = comicInfo

                    val selecionado = if (mComicInfo.idMal != null) Selecionado.SELECIONADO else Selecionado.SELECIONAR
                    Selecionado.setTabColor(tbTabPastas_ComicInfo, selecionado)
                    tbTabPastas_ComicInfo.text = "Comic Info" + (if (selecionado == Selecionado.SELECIONADO) " (" + comicInfo.comic + ")" else "")

                    btnMalAplicar.isDisable = false
                    controllerPai.setCursor(null)
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
        tbTabRootPastas.selectionModel.select(tbTabPastas_ComicInfo)
        val callback: Callback<ComicInfo, Boolean> = Callback<ComicInfo, Boolean> { param ->
            mComicInfo = param
            null
        }
        PopupAmazonController.abreTelaAmazon(controllerPai.rootStack, controllerPai.rootTab, callback, mComicInfo, cbLinguagem.value)
    }

    @FXML
    private fun onBtnCompactar() {
        if (btnCompactar.accessibleTextProperty().value.equals("COMPACTAR", ignoreCase = true)) {
            val listaSelecionada = mObsListaProcessar.filter { it.isSelecionado }
            if (listaSelecionada.isEmpty()) {
                AlertasModal.alerta("Alerta", "Nenhum item selecionado para compactar.")
                return
            }

            val pastaTexto = txtPasta.text
            if (pastaTexto.isNullOrEmpty()) {
                txtPasta.unFocusColor = Color.RED
                AlertasModal.alerta("Alerta", "Não informado a pasta para processamento.")
                return
            }

            val mangaProcessar = cbManga.value
            if (mangaProcessar.isNullOrEmpty()) {
                cbManga.unFocusColor = Color.RED
                AlertasModal.alerta("Alerta", "Não informado o nome do manga.")
                return
            }

            controllerPai.setCursor(Cursor.WAIT)
            desabilita(btnCompactar)

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
                                if (!item.pasta.exists())
                                    continue

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
                            val tudo = if (comicMap.contains("000")) {
                                comicMap["000"]?.listFiles()?.any { it.name.contains("tudo", true) } == true
                            } else
                                false
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

                            var summary = mComicInfo.summary ?: ""
                            if (itensOrdenados.any { it.titulo.trim().isNotEmpty() }) {
                                val hasChapter = summary.lowercase().indexOf("*chapter titles")
                                val sbChapters = StringBuilder()
                                if (hasChapter != -1)
                                    sbChapters.append("*Chapter Titles Manual*\n")
                                else
                                    sbChapters.append("*Chapter Titles*\n")

                                itensOrdenados.forEach { item ->
                                    sbChapters.append("Chapter ${capituloFormat.format(item.capitulo)}: ${item.titulo}\n")
                                }

                                summary += sbChapters.toString().trimEnd()
                            }

                            val comicInfoEnvio = ComicInfo(mComicInfo)
                            comicInfoEnvio.volume = vol.toInt()
                            comicInfoEnvio.number = vol
                            comicInfoEnvio.count = vol.toInt()
                            comicInfoEnvio.summary = summary.trim()

                            mRarService.compactar(
                                destino, arquivoZip, manga, comicInfoEnvio, compactar, comicMap, cbLinguagem.value ?: Linguagem.PORTUGUESE,
                                isCompactar = true, isGerarCapitulos = true, isAtualizarComic = false, callback
                            )

                            if (cbApagarArquivo.isSelected) {
                                compactar.forEach { it.deleteRecursively() }
                            }
                        }

                        mServiceComicInfo.save(mComicInfo)
                        Platform.runLater {
                            Notificacoes.notificacao(Notificacao.SUCESSO, "Compactar", "Arquivos compactados com sucesso.")
                        }
                    } catch (e: Exception) {
                        mLOG.error("Erro ao compactar.", e)
                        Platform.runLater { AlertasModal.erro("Erro ao compactar", e.message ?: "Erro desconhecido") }
                    }
                    return null
                }

                override fun succeeded() {
                    updateMessage("Compactação Finalizada.")
                    controllerPai.rootProgress.progressProperty().unbind()
                    controllerPai.rootMessage.textProperty().unbind()
                    controllerPai.clearProgress()
                    habilita(btnCompactar, "Compactar")
                }

                override fun failed() {
                    controllerPai.rootProgress.progressProperty().unbind()
                    controllerPai.rootMessage.textProperty().unbind()
                    controllerPai.clearProgress()
                    habilita(btnCompactar, "Compactar")
                }
            }
            controllerPai.rootProgress.progressProperty().bind(task.progressProperty())
            controllerPai.rootMessage.textProperty().bind(task.messageProperty())
            Thread(task).start()
        } else
            mCANCELAR = true
    }

    private var mCANCELAR = false

    @FXML
    private fun onBtnAjustarPastas() {
        val selecionados = mObsListaProcessar.filter { it.isSelecionado }
        if (selecionados.isEmpty()) {
            AlertasModal.alerta("Alerta", "Nenhum item selecionado.")
            return
        }

        if (!ConfirmaModal.confirmacao("Confirmação", "Deseja mover todos os arquivos internos para a primeira pasta de cada item selecionado?")) {
            return
        }

        if (btnAjustarPastas.accessibleTextProperty().value.equals("AJUSTAR", ignoreCase = true)) {
            desabilita(btnAjustarPastas)
            controllerPai.setCursor(Cursor.WAIT)

            val task = object : Task<Void>() {
                override fun call(): Void? {
                    try {
                        mCANCELAR = false
                        val total = selecionados.size.toLong()
                        var atual = 0L

                        for (item in selecionados) {
                            if (mCANCELAR) break

                            atual++
                            updateProgress(atual, total)
                            updateMessage("Movendo arquivos em: ${item.arquivo}")

                            val raizItem = item.pasta
                            processaMoverRecursivo(raizItem, raizItem)
                            removePastasVazias(raizItem)
                        }

                        if (!mCANCELAR) {
                            Platform.runLater {
                                carregarItens()
                                Notificacoes.notificacao(Notificacao.SUCESSO, "Ajustar Pastas", "Arquivos ajustados com sucesso.")
                            }
                        }
                    } catch (e: Exception) {
                        mLOG.error("Erro ao ajustar pastas.", e)
                        Platform.runLater { AlertasModal.erro("Erro ao ajustar pastas", e.message ?: "Erro desconhecido") }
                    }
                    return null
                }

                override fun succeeded() {
                    controllerPai.rootProgress.progressProperty().unbind()
                    controllerPai.rootMessage.textProperty().unbind()
                    habilita(btnAjustarPastas, "Ajustar Pastas")
                    controllerPai.clearProgress()
                }

                override fun failed() {
                    controllerPai.rootProgress.progressProperty().unbind()
                    controllerPai.rootMessage.textProperty().unbind()
                    habilita(btnAjustarPastas, "Ajustar Pastas")
                    controllerPai.clearProgress()
                }
            }

            controllerPai.rootProgress.progressProperty().bind(task.progressProperty())
            controllerPai.rootMessage.textProperty().bind(task.messageProperty())
            Thread(task).start()
        } else
            mCANCELAR = true
    }

    private fun processaMoverRecursivo(diretorio: File, raizDestino: File) {
        val files = diretorio.listFiles() ?: return
        for (f in files) {
            if (f.isDirectory) {
                processaMoverRecursivo(f, raizDestino)
            } else {
                if (f.parentFile != raizDestino) {
                    var destino = File(raizDestino, f.name)
                    if (destino.exists()) {
                        var count = 1
                        val name = f.nameWithoutExtension
                        val ext = f.extension
                        while (destino.exists()) {
                            destino = File(raizDestino, "$name ($count).$ext")
                            count++
                        }
                    }
                    Files.move(f.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }

    private fun removePastasVazias(diretorio: File) {
        val files = diretorio.listFiles() ?: return
        for (f in files) {
            if (f.isDirectory) {
                removePastasVazias(f)
                if (f.listFiles()?.isEmpty() == true) {
                    f.delete()
                }
            }
        }
    }

    private fun desabilita(activeButton: JFXButton? = null) {
        btnPesquisarPasta.isDisable = true
        txtPasta.isDisable = true
        cbManga.isDisable = true
        btnCarregar.isDisable = true
        btnGerarCapas.isDisable = true
        btnRenomear.isDisable = true
        btnAjustarPastas.isDisable = true
        tbViewProcessar.isDisable = true
        ckbSelecionarTodos.isDisable = true
        cbApagarArquivo.isDisable = true
        btnCompactar.isDisable = true
        btnCapitulos.isDisable = true
        btnValidar.isDisable = true
        btnImportarVolumes.isDisable = true

        activeButton?.let {
            it.isDisable = false
            it.accessibleTextProperty().set("CANCELA")
            it.text = "Cancelar"
        }
    }

    private fun habilita(activeButton: JFXButton? = null, originalText: String = "") {
        btnPesquisarPasta.isDisable = false
        txtPasta.isDisable = false
        cbManga.isDisable = false
        btnCarregar.isDisable = false
        btnGerarCapas.isDisable = false
        btnRenomear.isDisable = false
        btnAjustarPastas.isDisable = false
        btnCompactar.isDisable = false
        btnValidar.isDisable = false
        btnCapitulos.isDisable = false
        btnImportarVolumes.isDisable = false
        tbViewProcessar.isDisable = false
        ckbSelecionarTodos.isDisable = false
        cbApagarArquivo.isDisable = false
        
        if (::controller.isInitialized)
            controllerPai.setCursor(null)

        activeButton?.let {
            it.accessibleTextProperty().set(originalText.uppercase())
            it.text = originalText
        }
    }

    internal fun carregaComicInfo(comic: ComicInfo) {
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
        atualizaTituloComicInfo(comic)
    }

    internal fun carregaComicInfo() {
        val mangaNome = cbManga.editor.text ?: ""
        val comic = if (mangaNome.isEmpty())
            ComicInfo(null, null, mangaNome, mangaNome)
        else {
            var nome = mangaNome
            if (nome.contains("]"))
                nome = nome.substring(nome.indexOf("]")).replace("]", "").trim { it <= ' ' }

            if (nome.isNotEmpty() && nome.endsWith("-", ignoreCase = true))
                nome = nome.substring(0, nome.length - 1).trim { it <= ' ' }

            val comic = mServiceComicInfo.find(nome, cbLinguagem.value?.sigla ?: "ja") ?: mServiceComicInfo.find(nome) ?: ComicInfo(null, null, nome, nome)
            if (comic.languageISO != cbLinguagem.value.sigla) {
                comic.id = null
                comic.languageISO = cbLinguagem.value.sigla
            }
            comic
        }

        if (comic.id == null) {
            mLOG.info("Gerando novo ComicInfo.")
            txtMalId.text = ""
        } else {
            mLOG.info("ComicInfo localizado: " + comic.title)
            txtMalId.text = comic.idMal.toString()
        }

        if (comic.comic.isEmpty())
            comic.comic = mangaNome

        if (comic.series.isEmpty())
            comic.series = mangaNome

        txtMalNome.text = comic.comic
        mComicInfo = comic

        if (mangaNome.isNotEmpty() && mComicInfo.id != null)
            consultarMal()
    }

    private fun atualizaTituloComicInfo(comic: ComicInfo) {
        val selecionado = when {
            comic.idMal != null -> Selecionado.SELECIONADO
            mObsListaMal.isNotEmpty() -> Selecionado.SELECIONAR
            else -> Selecionado.VAZIO
        }
        Selecionado.setTabColor(tbTabPastas_ComicInfo, selecionado)
        val serie = if (comic.series.length > 50) comic.series.substring(0, 50) + "..." else comic.series
        val sufixo = if (selecionado == Selecionado.SELECIONADO && serie.isNotEmpty()) " ($serie)" else ""
        tbTabPastas_ComicInfo.text = "Comic Info$sufixo"
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

    private fun consultarMal(offset: Int = 0) {
        if (isConsultandoMal)
            return
        isConsultandoMal = true

        if (txtMalId.text.isNotEmpty() || txtMalNome.text.isNotEmpty()) {
            val id: Long? = if (txtMalId.text.isNotEmpty()) txtMalId.text.toLongOrNull() else null
            val nome = txtMalNome.text.replace(Regex("[^\\p{L}\\p{N}\\s_\\-]"), "")
            val linguagem = cbLinguagem.value ?: Linguagem.JAPANESE
            val comicInfo = ComicInfo(mComicInfo)

            btnMalConsultar.isDisable = true
            controllerPai.setCursor(Cursor.WAIT)
            controllerPai.rootProgress.progress = -1.0
            controllerPai.rootMessage.text = "Consultando MyAnimeList..."

            val consulta: Task<Void> = object : Task<Void>() {
                private var listaResults = listOf<Mal>()
                private var atualizado = false

                override fun call(): Void? {
                    try {
                        listaResults = mServiceComicInfo.getMal(id, nome, offset)
                        if (id != null && listaResults.size == 1) {
                            mServiceComicInfo.updateMal(comicInfo, listaResults.first(), linguagem)
                            atualizado = true
                        }
                    } catch (e: Exception) {
                        mLOG.info("Erro ao realizar a consulta do MyAnimeList.", e)
                        Platform.runLater { AlertasModal.erro("Erro no My Anime List", e.message ?: "Erro desconhecido") }
                    }
                    return null
                }

                override fun succeeded() {
                    if (offset == 0)
                        mObsListaMal.setAll(listaResults)
                    else
                        mObsListaMal.addAll(listaResults)

                    tbViewMal.items = mObsListaMal
                    tbViewMal.refresh()

                    if (mObsListaMal.isEmpty())
                        Notificacoes.notificacao(Notificacao.ALERTA, "My Anime List", "Nenhum item encontrado.")
                    else if (atualizado)
                        mComicInfo = comicInfo

                    btnMalConsultar.isDisable = false
                    isConsultandoMal = false
                    controllerPai.setCursor(null)
                    controllerPai.clearProgress()
                }

                override fun failed() {
                    btnMalConsultar.isDisable = false
                    isConsultandoMal = false
                    controllerPai.setCursor(null)
                    controllerPai.clearProgress()
                }

                override fun cancelled() {
                    btnMalConsultar.isDisable = false
                    isConsultandoMal = false
                    controllerPai.setCursor(null)
                    controllerPai.clearProgress()
                }
            }
            Thread(consulta).start()
        } else {
            AlertasModal.alerta("Alerta", "Necessário informar um id ou nome.")
            txtMalNome.requestFocus()
            isConsultandoMal = false
        }
    }

    private fun carregarItens() {
        val path = txtPasta.text?.trim() ?: ""
        val pasta = File(path)
        if (path.isNotEmpty() && pasta.exists() && pasta.isDirectory) {
            val state = btnCarregar.accessibleTextProperty().value ?: ""
            if (state.equals("CARREGAR", ignoreCase = true)) {
                desabilita(btnCarregar)

            val processar: Task<Void> = object : Task<Void>() {
                override fun call(): Void? {
                    try {
                        mCANCELAR = false
                        val lista = mutableListOf<Pasta>()
                        val parsingService = PastaParsingService()
                        val files = pasta.listFiles()
                        if (files == null) {
                            Platform.runLater { AlertasModal.erro("Erro ao carregar", "Não foi possível listar os arquivos do diretório: ${pasta.absolutePath}") }
                            return null
                        }

                        val max = files.size.toLong()
                        var i = 0L

                        for (file in files) {
                            if (mCANCELAR) break

                            i++
                            if (file.isFile)
                                continue

                            updateProgress(i, max)
                            updateMessage("Carregando item $i de $max.")

                            val result = parsingService.parse(file.name)

                            var volume = result.volume.toFloatOrNull() ?: 0f
                            var capitulo = result.capitulo.toFloatOrNull() ?: 0f
                            var titulo = result.titulo

                            val comicFile = File(file, "ComicInfo.xml")
                            if (comicFile.exists()) {
                                try {
                                    val jaxb = JAXBContext.newInstance(ComicInfo::class.java)
                                    val unmarshaller = jaxb.createUnmarshaller()
                                    val comicInfo = unmarshaller.unmarshal(comicFile) as ComicInfo

                                    if (comicInfo.number > 0)
                                        capitulo = comicInfo.number

                                    if (comicInfo.volume > 0)
                                        volume = comicInfo.volume.toFloat()

                                    if (comicInfo.title.isNotEmpty()) {
                                        val cTitle = comicInfo.title

                                        // Se ainda não temos volume/capítulo do XML, tenta extrair do Title (conforme sugestão do usuário)
                                        if (capitulo == 0f || volume == 0f) {
                                            val parsingInfo = parsingService.parse(cTitle)
                                            if (capitulo == 0f && parsingInfo.capitulo.toFloatOrNull() ?: 0f > 0f)
                                                capitulo = parsingInfo.capitulo.toFloat()
                                            if (volume == 0f && parsingInfo.volume.toFloatOrNull() ?: 0f > 0f)
                                                volume = parsingInfo.volume.toFloat()
                                        }

                                        // Tenta identificar subtítulo (Tag) após delimitadores :, - ou afins após marcadores
                                        val regexSub = "(?i).*?\\b(chapter|capitulo|capítulo|ch|cap|c|volume|vol|v)\\b.*?[:\\- ]+\\s*(.*)".toRegex()
                                        val match = regexSub.find(cTitle)
                                        titulo = if (match != null && match.groups.size > 2) {
                                            match.groups[2]?.value?.trim() ?: cTitle
                                        } else if (cTitle.contains(":")) {
                                            cTitle.substringAfter(":").trim()
                                        } else if (cTitle.contains("-")) {
                                            cTitle.substringAfter("-").trim()
                                        } else {
                                            cTitle
                                        }

                                        if (titulo.isEmpty()) titulo = cTitle
                                    }
                                } catch (e: Exception) {
                                    mLOG.error("Erro ao carregar ComicInfo da pasta ${file.name}", e)
                                }
                            }

                            lista.add(
                                Pasta(
                                    pasta = file,
                                    arquivo = file.name,
                                    nome = cbManga.editor.text,
                                    volume = volume,
                                    capitulo = capitulo,
                                    scan = result.scan,
                                    titulo = titulo,
                                    isCapa = result.isCapa,
                                    isSelecionado = true
                                )
                            )
                        }


                        if (!mCANCELAR) {
                            Platform.runLater {
                                mHistory.clear()
                                mObsListaProcessar = FXCollections.observableArrayList(lista)
                                mObsListaProcessar.sortWith(compareBy({ it.volume }, { it.capitulo }))
                                tbViewProcessar.items = mObsListaProcessar
                                ckbSelecionarTodos.isSelected = true
                                atualizaCheckSelecionarTodos()
                                validarRegistros()
                            }
                        }
                    } catch (e: Exception) {
                        mLOG.info("Erro ao carregar pastas.", e)
                        Platform.runLater { AlertasModal.erro("Erro ao carregar pastas", e.message ?: "Erro desconhecido") }
                    }
                    return null
                }

                override fun succeeded() {
                    updateMessage("Pastas carregadas com sucesso.")
                    controllerPai.rootProgress.progressProperty().unbind()
                    controllerPai.rootMessage.textProperty().unbind()
                    controllerPai.clearProgress()
                    habilita(btnCarregar, "Carregar")
                }

                override fun failed() {
                    controllerPai.rootProgress.progressProperty().unbind()
                    controllerPai.rootMessage.textProperty().unbind()
                    controllerPai.clearProgress()
                    habilita(btnCarregar, "Carregar")
                }
            }
            controllerPai.rootProgress.progressProperty().bind(processar.progressProperty())
            controllerPai.rootMessage.textProperty().bind(processar.messageProperty())
            Thread(processar).start()
        } else
            mCANCELAR = true
        } else {
            AlertasModal.alerta("Alerta", "Necessário informar uma pasta para carregar.")
            txtPasta.requestFocus()
        }
    }

    private fun renomear() {
        val pastaTexto = txtPasta.text
        if (pastaTexto.isNullOrEmpty()) {
            txtPasta.unFocusColor = Color.RED
            AlertasModal.alerta("Alerta", "Não informado a pasta para processamento.")
            return
        }

        val listaProcessar = mObsListaProcessar.filter { it.isSelecionado }.ifEmpty { tbViewProcessar.selectionModel.selectedItems.toList() }
        if (listaProcessar.isEmpty()) {
            AlertasModal.alerta("Alerta", "Não existem itens selecionados para processar.")
            return
        }

        if (btnRenomear.accessibleTextProperty().value.equals("RENOMEAR", ignoreCase = true)) {
            desabilita(btnRenomear)
            val processar: Task<Void> = object : Task<Void>() {
                override fun call(): Void? {
                    val falhas = mutableListOf<String>()
                    try {
                        mCANCELAR = false
                        val volume = DecimalFormat("00.##", DecimalFormatSymbols(Locale.US))
                        val capitulo = DecimalFormat("000.##", DecimalFormatSymbols(Locale.US))

                        var i = 0L
                        val max = listaProcessar.size.toLong()

                        for ((index, item) in listaProcessar.sortedBy { it.volume }.withIndex()) {
                            if (mCANCELAR) break

                            updateProgress(++i, max)
                            updateMessage("Renomeando item $i de $max.")

                            try {
                                val pasta =
                                    "[${item.scan}] ${item.nome} - Volume ${volume.format(item.volume)} ${if (item.isCapa) "Capa" else "Capítulo " + capitulo.format(item.capitulo)}"
                                val path = item.pasta.toPath()
                                val target = path.resolveSibling(pasta)

                                if (path != target) {
                                    if (Files.exists(target)) {
                                        falhas.add(item.arquivo + " (Destino já existe)")
                                    } else {
                                        item.pasta = Files.move(path, target, StandardCopyOption.REPLACE_EXISTING).toFile()
                                        item.arquivo = item.pasta.name
                                    }
                                }
                            } catch (e: Exception) {
                                falhas.add(item.arquivo)
                                mLOG.error("Erro ao renomear pasta: ${item.arquivo}", e)
                            }
                        }

                        if (!mCANCELAR) {
                            Platform.runLater {
                                tbViewProcessar.refresh()
                                if (falhas.isNotEmpty()) {
                                    Notificacoes.notificacao(Notificacao.ALERTA, "Renomear Pastas", "Processo concluído com ${falhas.size} falhas.")
                                    AlertasModal.alerta("Aviso", "Não foi possível renomear as seguintes pastas:\n\n" + falhas.joinToString("\n"))
                                } else {
                                    Notificacoes.notificacao(Notificacao.SUCESSO, "Renomear Pastas", "Pastas renomeadas com sucesso.")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        mLOG.info("Erro ao processar pastas.", e)
                        Platform.runLater { AlertasModal.erro("Erro ao renomear pastas", e.message ?: "Erro desconhecido") }
                    }
                    return null
                }

                override fun succeeded() {
                    controllerPai.rootProgress.progressProperty().unbind()
                    controllerPai.rootMessage.textProperty().unbind()
                    controllerPai.clearProgress()
                    habilita(btnRenomear, "Renomear")
                }

                override fun failed() {
                    controllerPai.rootProgress.progressProperty().unbind()
                    controllerPai.rootMessage.textProperty().unbind()
                    controllerPai.clearProgress()
                    habilita(btnRenomear, "Renomear")
                }
            }
            controllerPai.rootProgress.progressProperty().bind(processar.progressProperty())
            controllerPai.rootMessage.textProperty().bind(processar.messageProperty())
            Thread(processar).start()
        } else
            mCANCELAR = true
    }

    private fun importarVolumes() {
        if (cbManga.editor.text.isNullOrEmpty())
            return

        val mangas = mServiceManga.findAll(cbManga.editor.text, isCaminho = true)
        val volumes = mutableMapOf<String, String>()
        mangas.forEach { m -> volumes.putAll(m.caminhos.associate { c -> c.capitulo to m.volume }) }
        val formater = DecimalFormat("000.##", DecimalFormatSymbols(Locale.US))
        val letras = Utils.NOT_NUMBER_PATTERN.toRegex()
        for (item in mObsListaProcessar) {
            item.volume = if (item.capitulo > 0) {
                val capStr = formater.format(item.capitulo)
                val vol = volumes[capStr]
                if (vol != null) {
                    vol.replace(letras, "").toFloatOrNull() ?: 0f
                } else {
                    val capInteiroStr = formater.format(item.capitulo.toInt())
                    volumes[capInteiroStr]?.replace(letras, "")?.toFloatOrNull() ?: 0f
                }
            } else 0f
        }
        mObsListaProcessar.sortWith(compareBy({ it.volume }, { it.capitulo }))
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

        txtPasta.onKeyPressed = EventHandler { e: KeyEvent ->
            if (e.code == KeyCode.ENTER) {
                carregarItens()
                Utils.clickTab()
            }
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

        txtSeries.textProperty().addListener { _, _, newValue ->
            mComicInfo.series = newValue
            if (tbTabPastas_ComicInfo.text.contains("(")) {
                val serie = if (newValue.length > 50) newValue.substring(0, 50) + "..." else newValue
                if (serie.isNotEmpty())
                    tbTabPastas_ComicInfo.text = "Comic Info ($serie)"
                else
                    tbTabPastas_ComicInfo.text = "Comic Info"
            }
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

        cbAgeRating.focusedProperty().addListener { _, _, newPropertyValue ->
            if (newPropertyValue)
                cbAgeRating.setUnFocusColor(Color.web("#4059a9"))
            else {
                if (cbAgeRating.value == null)
                    cbAgeRating.setUnFocusColor(Color.RED)
                else
                    cbAgeRating.setUnFocusColor(Color.web("#4059a9"))
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
        autoCompletePopup.setSelectionHandler { event ->
            cbManga.setValue(event.getObject())
            oldManga = event.getObject()

            if (!event.getObject().isNullOrEmpty()) {
                val mangas = mServiceManga.findAll(event.getObject(), isCaminho = true)
                val volumesMap = mutableMapOf<String, Float>()
                val decimal = DecimalFormat("000.##", DecimalFormatSymbols(Locale.US))
                mangas.forEach { m ->
                    m.caminhos.forEach { c -> volumesMap[c.capitulo] = m.volume.replace(Utils.NOT_NUMBER_PATTERN.toRegex(), "").toFloatOrNull() ?: 0f }
                }

                for (item in mObsListaProcessar) {
                    item.nome = event.getObject()
                    if (item.volume == 0f && item.capitulo > 0f) {
                        val capStr = decimal.format(item.capitulo)
                        if (volumesMap.containsKey(capStr))
                            item.volume = volumesMap[capStr]!!
                    }
                }

                mObsListaProcessar.sortWith(compareBy({ it.volume }, { it.capitulo }))
                tbViewProcessar.refresh()
                carregaComicInfo()
            }
        }
        cbManga.editor.textProperty().addListener { _, _, _ ->
            autoCompletePopup.filter { item -> item.lowercase(Locale.getDefault()).contains(cbManga.editor.text.lowercase(Locale.getDefault())) }
            if (autoCompletePopup.filteredSuggestions.isEmpty() || cbManga.showingProperty().get() || cbManga.editor.text.isEmpty())
                autoCompletePopup.hide()
            else
                autoCompletePopup.show(cbManga.editor)
        }
        cbManga.setOnKeyPressed { ke -> if (ke.code.equals(KeyCode.ENTER)) Utils.clickTab() }
        cbManga.focusedProperty().addListener { _, oldValue, newValue ->
            if (newValue) {
                oldManga = cbManga.editor.text?.trim() ?: ""
            } else if (oldValue) {
                // Focus lost
                val novoNome = cbManga.editor.text?.trim() ?: ""
                
                if (oldManga != novoNome) {
                    oldManga = novoNome
                    
                    for (item in mObsListaProcessar) {
                        item.nome = novoNome
                        if (item.volume == 0f) {
                            val onlyNumbers = item.arquivo.replace(Utils.NOT_NUMBER_PATTERN.toRegex(), "")
                            if (onlyNumbers.isNotEmpty())
                                item.volume = onlyNumbers.toFloatOrNull() ?: 0f
                        }
                    }

                    if (novoNome.isNotEmpty()) {
                        val mangas = mServiceManga.findAll(novoNome, isCaminho = true)
                        val volumesMap = mutableMapOf<String, Float>()
                        val decimal = DecimalFormat("000.##", DecimalFormatSymbols(Locale.US))
                        
                        mangas.forEach { m ->
                            m.caminhos.forEach { c -> 
                                volumesMap[c.capitulo] = m.volume.replace(Utils.NOT_NUMBER_PATTERN.toRegex(), "").toFloatOrNull() ?: 0f 
                            }
                        }

                        for (item in mObsListaProcessar) {
                            if (item.volume == 0f && item.capitulo > 0f) {
                                val capStr = decimal.format(item.capitulo)
                                if (volumesMap.containsKey(capStr))
                                    item.volume = volumesMap[capStr]!!
                            }
                        }

                        mObsListaProcessar.sortWith(compareBy({ it.volume }, { it.capitulo }))
                        tbViewProcessar.refresh()
                        carregaComicInfo()
                        
                        if (txtMalNome.text.isNotEmpty() || txtMalId.text.isNotEmpty()) {
                            btnMalConsultar.fire()
                        }
                    } else {
                        tbViewProcessar.refresh()
                    }
                }
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

    private fun ajustarCapitulo(todos: Boolean = false) {
        val lista = if (todos) mObsListaProcessar else tbViewProcessar.selectionModel.selectedItems.toList().ifEmpty {
            mObsListaProcessar.filter { it.isSelecionado }
        }

        if (lista.isEmpty()) {
            AlertasModal.alerta("Alerta", "Nenhum item selecionado.")
            return
        }

        val actions = mutableListOf<ReversibleAction>()
        for (item in lista) {
            val parteInteira = item.capitulo.toInt()
            val parteDecimal = item.capitulo - parteInteira
            if (Math.abs(parteDecimal - 0.1f) < 0.001f) {
                val oldVal = item.capitulo
                val newVal = parteInteira.toFloat()
                if (oldVal != newVal) {
                    actions.add(PropertyChangeAction(item, oldVal, newVal) { it, v -> it.capitulo = v })
                    item.capitulo = newVal
                }
            }
        }
        if (actions.isNotEmpty()) mHistory.pushAction(CompositeAction(actions))

        tbViewProcessar.refresh()
    }

    private fun apagarTodosTitulos() {
        val actions = mutableListOf<ReversibleAction>()
        for (item in mObsListaProcessar) {
            if (item.titulo.isNotEmpty()) {
                actions.add(PropertyChangeAction(item, item.titulo, "") { it, v -> it.titulo = v })
                item.titulo = ""
            }
        }
        if (actions.isNotEmpty()) mHistory.pushAction(CompositeAction(actions))
        tbViewProcessar.refresh()
    }

    private fun linkaCelulas() {
        clArquivo.cellValueFactory = PropertyValueFactory("arquivo")
        clScan.cellValueFactory = PropertyValueFactory("scan")
        clVolume.cellValueFactory = PropertyValueFactory("volume")
        clCapitulo.cellValueFactory = PropertyValueFactory("capitulo")
        clTitulo.cellValueFactory = PropertyValueFactory("titulo")
        clTitulo.setOnEditCommit { e ->
            val item = e.tableView.items[e.tablePosition.row]
            val oldVal = item.titulo
            val newVal = e.newValue
            if (oldVal != newVal) {
                mHistory.pushAction(PropertyChangeAction(item, oldVal, newVal) { i, v -> i.titulo = v })
                item.titulo = newVal
            }
        }
        
        clScan.setOnEditCommit { e ->
            val item = e.tableView.items[e.tablePosition.row]
            val oldVal = item.scan
            val newVal = e.newValue
            if (oldVal != newVal) {
                mHistory.pushAction(PropertyChangeAction(item, oldVal, newVal) { i, v -> i.scan = v })
                item.scan = newVal
            }
        }

        clVolume.setOnEditCommit { e ->
            val item = e.tableView.items[e.tablePosition.row]
            val oldVal = item.volume
            val newVal : Float = e.newValue as Float
            if (oldVal != newVal) {
                mHistory.pushAction(PropertyChangeAction(item, oldVal, newVal) { i, v -> i.volume = v })
                item.volume = newVal
            }
        }

        clCapitulo.setOnEditCommit { e ->
            val item = e.tableView.items[e.tablePosition.row]
            val oldVal = item.capitulo
            val newVal : Float = e.newValue as Float
            if (oldVal != newVal) {
                mHistory.pushAction(PropertyChangeAction(item, oldVal, newVal) { i, v -> i.capitulo = v })
                item.capitulo = newVal
            }
        }

        clSelecionado.setCellValueFactory { param ->
            val booleanProp = SimpleBooleanProperty(param.value.isSelecionado)
            booleanProp.addListener { _, _, newValue ->
                param.value.isSelecionado = newValue
                atualizaCheckSelecionarTodos()
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

        clFormatado.setCellValueFactory { param ->
            SimpleStringProperty(
                "[${param.value.scan}] ${param.value.nome} - Volume ${volume.format(param.value.volume)} ${
                    if (param.value.isCapa) "Capa" else "Capítulo " + capitulo.format(
                        param.value.capitulo
                    )
                }"
            )
        }

        val menu = ContextMenu()
        val scanProximos = MenuItem("Aplicar scan nos arquivos próximos")
        scanProximos.setOnAction {
            tbViewProcessar.selectionModel.selectedItem?.run {
                val scan = this.scan
                val index = mObsListaProcessar.indexOf(this)
                if (scan.isNotEmpty() && index < tbViewProcessar.items.size - 1) {
                    val actions = mutableListOf<ReversibleAction>()
                    for (i in index + 1 until tbViewProcessar.items.size) {
                        val item = mObsListaProcessar[i]
                        if (item.scan != scan) {
                            actions.add(PropertyChangeAction(item, item.scan, scan) { it, v -> it.scan = v })
                            item.scan = scan
                        }
                    }
                    if (actions.isNotEmpty()) mHistory.pushAction(CompositeAction(actions))
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
                    val actions = mutableListOf<ReversibleAction>()
                    for (i in index - 1 downTo 0) {
                        val item = mObsListaProcessar[i]
                        if (item.scan != scan) {
                            actions.add(PropertyChangeAction(item, item.scan, scan) { it, v -> it.scan = v })
                            item.scan = scan
                        }
                    }
                    if (actions.isNotEmpty()) mHistory.pushAction(CompositeAction(actions))
                    tbViewProcessar.refresh()
                }
            }
        }

        val scanMesmoVolume = MenuItem("Aplicar scan nos arquivos de mesmo volume")
        scanMesmoVolume.setOnAction {
            tbViewProcessar.selectionModel.selectedItem?.run {
                val scanValue = this.scan
                val volumeValue = this.volume
                if (scanValue.isNotEmpty()) {
                    val actions = mutableListOf<ReversibleAction>()
                    mObsListaProcessar.filter { it.volume == volumeValue }.forEach { item ->
                        if (item.scan != scanValue) {
                            actions.add(PropertyChangeAction(item, item.scan, scanValue) { it, v -> it.scan = v })
                            item.scan = scanValue
                        }
                    }
                    if (actions.isNotEmpty()) mHistory.pushAction(CompositeAction(actions))
                    tbViewProcessar.refresh()
                }
            }
        }

        val volumesZerar = MenuItem("Zerar volumes")
        volumesZerar.setOnAction {
            val actions = mutableListOf<ReversibleAction>()
            mObsListaProcessar.forEach { item -> 
                if (item.volume != 0f) {
                    actions.add(PropertyChangeAction(item, item.volume, 0f) { it, v -> it.volume = v })
                    item.volume = 0f 
                }
            }
            if (actions.isNotEmpty()) mHistory.pushAction(CompositeAction(actions))
            tbViewProcessar.refresh()
        }
        val volumesImportar = MenuItem("Importar volumes")
        volumesImportar.setOnAction { importarVolumes() }

        val volumeProximos = MenuItem("Aplicar volume nos arquivos próximos")
        volumeProximos.setOnAction {
            tbViewProcessar.selectionModel.selectedItem?.run {
                val volumeItem = this.volume
                val index = mObsListaProcessar.indexOf(this)
                if (index != -1 && index < tbViewProcessar.items.size - 1) {
                    val actions = mutableListOf<ReversibleAction>()
                    for (i in index + 1 until tbViewProcessar.items.size) {
                        val item = mObsListaProcessar[i]
                        if (item.volume != volumeItem) {
                            actions.add(PropertyChangeAction(item, item.volume, volumeItem) { it, v -> it.volume = v })
                            item.volume = volumeItem
                        }
                    }
                    if (actions.isNotEmpty()) mHistory.pushAction(CompositeAction(actions))
                    tbViewProcessar.refresh()
                }
            }
        }
        val volumeAnteriores = MenuItem("Aplicar volume nos arquivos anteriores")
        volumeAnteriores.setOnAction {
            tbViewProcessar.selectionModel.selectedItem?.run {
                val volumeItem = this.volume
                val index = mObsListaProcessar.indexOf(this)
                if (index != -1 && index > 0) {
                    val actions = mutableListOf<ReversibleAction>()
                    for (i in index - 1 downTo 0) {
                        val item = mObsListaProcessar[i]
                        if (item.volume != volumeItem) {
                            actions.add(PropertyChangeAction(item, item.volume, volumeItem) { it, v -> it.volume = v })
                            item.volume = volumeItem
                        }
                    }
                    if (actions.isNotEmpty()) mHistory.pushAction(CompositeAction(actions))
                    tbViewProcessar.refresh()
                }
            }
        }
        val volumeFaltantes = MenuItem("Aplicar volume nos arquivos faltantes")
        volumeFaltantes.setOnAction {
            tbViewProcessar.selectionModel.selectedItem?.run {
                val volumeItem = this.volume
                val actions = mutableListOf<ReversibleAction>()
                for (item in mObsListaProcessar) {
                    if (item.volume == 0f && item.volume != volumeItem) {
                        actions.add(PropertyChangeAction(item, item.volume, volumeItem) { it, v -> it.volume = v })
                        item.volume = volumeItem
                    }
                }
                if (actions.isNotEmpty()) mHistory.pushAction(CompositeAction(actions))
                tbViewProcessar.refresh()
            }
        }

        val apagarTitulo = MenuItem("Apagar titulo")
        apagarTitulo.setOnAction {
            val lista = tbViewProcessar.selectionModel.selectedItems.toList().ifEmpty {
                mObsListaProcessar.filter { it.isSelecionado }
            }

            if (lista.isEmpty()) {
                AlertasModal.alerta("Alerta", "Nenhum item selecionado.")
                return@setOnAction
            }

            val actions = mutableListOf<ReversibleAction>()
            for (item in lista) {
                if (item.titulo.isNotEmpty()) {
                    actions.add(PropertyChangeAction(item, item.titulo, "") { it, v -> it.titulo = v })
                    item.titulo = ""
                }
            }
            if (actions.isNotEmpty()) mHistory.pushAction(CompositeAction(actions))
            tbViewProcessar.refresh()
        }

        val apagarTodosTitulos = MenuItem("Apagar todos os títulos")
        apagarTodosTitulos.setOnAction { apagarTodosTitulos() }

        val ajustarCapitulo = MenuItem("Ajustar capítulo")
        ajustarCapitulo.setOnAction { ajustarCapitulo() }

        val ajustarTodosCapitulos = MenuItem("Ajustar todos os capítulos")
        ajustarTodosCapitulos.setOnAction { ajustarCapitulo(todos = true) }

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

        val ajustarTitulos = MenuItem("Ajustar título (Ctrl + T)")
        ajustarTitulos.setOnAction { ajustarTitulos() }

        val ajustarTodosTitulos = MenuItem("Ajustar todos os títulos")
        ajustarTodosTitulos.setOnAction { ajustarTitulos(todos = true) }

        val normalizarTitulo = MenuItem("Normalizar título (Ctrl + N)")
        normalizarTitulo.setOnAction { normalizarTitulos() }

        val remover = MenuItem("Remover registro (Del)")
        remover.setOnAction { removerRegistro() }
        menu.items.addAll(
            scanAnterior,
            scanProximos,
            scanMesmoVolume,
            SeparatorMenuItem(),
            volumeAnteriores,
            volumeProximos,
            volumeFaltantes,
            volumesZerar,
            volumesImportar,
            SeparatorMenuItem(),
            ajustarCapitulo,
            ajustarTodosCapitulos,
            SeparatorMenuItem(),
            apagarTituloAnteriores,
            apagarTitulo,
            apagarTituloProximos,
            apagarTodosTitulos,
            ajustarTitulos,
            ajustarTodosTitulos,
            normalizarTitulo,
            SeparatorMenuItem(),
            remover
        )

        tbViewProcessar.contextMenu = menu
        this.contextMenu = menu

        configurarAtalhosGrid()

        clMalId.cellValueFactory = PropertyValueFactory("idVisual")
        clMalNome.cellValueFactory = PropertyValueFactory("nome")
        clMalTipo.cellValueFactory = PropertyValueFactory("tipo")
        clMalSite.cellValueFactory = PropertyValueFactory("site")
        clMalImagem.cellValueFactory = PropertyValueFactory("imagem")
        tbViewMal.items = mObsListaMal

        tbViewMal.onMouseClicked = EventHandler { click: MouseEvent ->
            if (click.clickCount > 1 && tbViewMal.items.isNotEmpty())
                carregaMal(tbViewMal.selectionModel.selectedItem)
        }

        val contextMenuMal = ContextMenu()
        val itemRecarregar = MenuItem("Recarregar imagem")
        itemRecarregar.setOnAction {
            val selected = tbViewMal.selectionModel.selectedItem
            if (selected != null && selected.imagem != null) {
                val mal = selected.mal
                val url = when {
                    mal.mainPicture.largeURL != null -> mal.mainPicture.largeURL
                    mal.mainPicture.mediumURL != null -> mal.mainPicture.mediumURL
                    mal.pictures.isNotEmpty() -> {
                        when {
                            mal.pictures[0].largeURL != null -> mal.pictures[0].largeURL
                            else -> mal.pictures[0].mediumURL
                        }
                    }
                    else -> null
                }
                if (url != null) {
                    selected.imagem!!.image = Image(url, true)
                }
            }
        }
        contextMenuMal.items.add(itemRecarregar)
        tbViewMal.contextMenu = contextMenuMal

        editaColunas()
        configurarDragAndDrop()
    }

    private fun ajustarTitulos(todos: Boolean = false) {
        val lista = if (todos) mObsListaProcessar else tbViewProcessar.selectionModel.selectedItems.toList().ifEmpty {
            mObsListaProcessar.filter { it.isSelecionado }
        }

        if (lista.isEmpty()) {
            AlertasModal.alerta("Alerta", "Nenhum item selecionado.")
            return
        }

        val actions = mutableListOf<ReversibleAction>()
        val regex = "(?i)^.*?((ch|chapter|cap|capitulo|c|v|volume|vol)\\.?\\s*\\d+|\\d+)\\s*[:\\- ]+\\s*".toRegex()
        for (item in lista) {
            val oldVal = item.titulo
            val newVal = item.titulo.replace(regex, "").trim()
            if (oldVal != newVal) {
                actions.add(PropertyChangeAction(item, oldVal, newVal) { i, v -> i.titulo = v })
                item.titulo = newVal
            }
        }
        if (actions.isNotEmpty()) mHistory.pushAction(CompositeAction(actions))
        tbViewProcessar.refresh()
    }

    private fun normalizarTitulos() {
        val lista = tbViewProcessar.selectionModel.selectedItems.toList().ifEmpty {
            mObsListaProcessar.filter { it.isSelecionado }
        }

        if (lista.isEmpty()) {
            AlertasModal.alerta("Alerta", "Nenhum item selecionado.")
            return
        }

        val actions = mutableListOf<ReversibleAction>()
        for (item in lista) {
            if (item.titulo.isNotEmpty()) {
                val oldVal = item.titulo
                val newVal = item.titulo.lowercase(Locale.getDefault()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                if (oldVal != newVal) {
                    actions.add(PropertyChangeAction(item, oldVal, newVal) { i, v -> i.titulo = v })
                    item.titulo = newVal
                }
            }
        }
        if (actions.isNotEmpty()) mHistory.pushAction(CompositeAction(actions))
        tbViewProcessar.refresh()
    }

    private fun removerRegistro() {
        val selecionados = tbViewProcessar.selectionModel.selectedItems.toList()
        if (selecionados.isNotEmpty()) {
            if (ConfirmaModal.confirmacao("Aviso", "Deseja remover o registro?")) {
                selecionados.forEach { mHistory.removeHistoryForItem(it) }
                mObsListaProcessar.removeAll(selecionados)
                tbViewProcessar.refresh()
            }
        }
    }

    private fun configurarAtalhosGrid() {
        tbViewProcessar.addEventFilter(KeyEvent.KEY_PRESSED) { e ->
            if (e.target is javafx.scene.control.TextInputControl) return@addEventFilter
            
            if (e.code.isLetterKey && !e.isControlDown && !e.isAltDown) {
                val selecionado = tbViewProcessar.selectionModel.selectedItem
                val letter = e.code.name.lowercase()
                val startIdx = if (selecionado != null) mObsListaProcessar.indexOf(selecionado) + 1 else 0
                
                var foundIdx = -1
                for (i in startIdx until mObsListaProcessar.size) {
                    if (mObsListaProcessar[i].arquivo.lowercase().startsWith(letter)) {
                        foundIdx = i
                        break
                    }
                }
                if (foundIdx == -1) {
                    for (i in 0 until startIdx) {
                        if (mObsListaProcessar[i].arquivo.lowercase().startsWith(letter)) {
                            foundIdx = i
                            break
                        }
                    }
                }
                if (foundIdx != -1) {
                    tbViewProcessar.selectionModel.clearAndSelect(foundIdx)
                    tbViewProcessar.scrollTo(foundIdx)
                }
                e.consume()
                return@addEventFilter
            }


            when (e.code) {
                KeyCode.Z -> {
                    if (e.isControlDown) {
                        val action = mHistory.undo()
                        if (action != null) {
                            tbViewProcessar.refresh()
                            (action.getFirstAffectedItem() as? Pasta)?.let { item ->
                                val idx = mObsListaProcessar.indexOf(item)
                                if (idx != -1) tbViewProcessar.scrollTo(idx)
                            }
                        }
                        e.consume()
                    }
                }
                KeyCode.Y -> {
                    if (e.isControlDown) {
                        val action = mHistory.redo()
                        if (action != null) {
                            tbViewProcessar.refresh()
                            (action.getFirstAffectedItem() as? Pasta)?.let { item ->
                                val idx = mObsListaProcessar.indexOf(item)
                                if (idx != -1) tbViewProcessar.scrollTo(idx)
                            }
                        }
                        e.consume()
                    }
                }
                KeyCode.DELETE -> {
                    removerRegistro()
                    e.consume()
                }
                KeyCode.SPACE -> {
                    val selecionados = tbViewProcessar.selectionModel.selectedItems
                    if (selecionados.isNotEmpty()) {
                        val novoEstado = !selecionados.first().isSelecionado
                        selecionados.forEach { it.isSelecionado = novoEstado }
                        tbViewProcessar.refresh()
                        atualizaCheckSelecionarTodos()
                    }
                    e.consume()
                }
                KeyCode.T -> {
                    if (e.isControlDown) {
                        ajustarTitulos()
                        e.consume()
                    }
                }
                KeyCode.N -> {
                    if (e.isControlDown) {
                        normalizarTitulos()
                        e.consume()
                    }
                }
                in KeyCode.DIGIT0..KeyCode.DIGIT9, in KeyCode.NUMPAD0..KeyCode.NUMPAD9 -> {
                    val digit = e.text
                    if (digit.isNotEmpty()) {
                        if (e.isControlDown) {
                            volumeBuffer += digit
                            e.consume()
                        } else if (e.isAltDown) {
                            chapterBuffer += digit
                            e.consume()
                        }
                    }
                }
                else -> {}
            }
        }

        tbViewProcessar.addEventFilter(KeyEvent.KEY_RELEASED) { e ->
            if (volumeBuffer.isNotEmpty() && !e.isControlDown) {
                val volumeValue = volumeBuffer.toFloatOrNull()
                if (volumeValue != null) {
                    val lista = mObsListaProcessar.filter { it.isSelecionado }.ifEmpty {
                        tbViewProcessar.selectionModel.selectedItem?.let { listOf(it) } ?: emptyList()
                    }
                    val actions = mutableListOf<ReversibleAction>()
                    lista.forEach { item -> 
                        if (item.volume != volumeValue) {
                            actions.add(PropertyChangeAction(item, item.volume, volumeValue) { i, v -> i.volume = v })
                            item.volume = volumeValue
                        }
                    }
                    if (actions.isNotEmpty()) mHistory.pushAction(CompositeAction(actions))
                    tbViewProcessar.refresh()
                }
                volumeBuffer = ""
                e.consume()
            }

            if (chapterBuffer.isNotEmpty() && !e.isAltDown) {
                val chapterValue = chapterBuffer.toFloatOrNull()
                if (chapterValue != null) {
                    val lista = mObsListaProcessar.filter { it.isSelecionado }.ifEmpty {
                        tbViewProcessar.selectionModel.selectedItem?.let { listOf(it) } ?: emptyList()
                    }
                    val actions = mutableListOf<ReversibleAction>()
                    lista.forEach { item -> 
                        if (item.capitulo != chapterValue) {
                            actions.add(PropertyChangeAction(item, item.capitulo, chapterValue) { i, v -> i.capitulo = v })
                            item.capitulo = chapterValue
                        }
                    }
                    if (actions.isNotEmpty()) mHistory.pushAction(CompositeAction(actions))
                    tbViewProcessar.refresh()
                }
                chapterBuffer = ""
                e.consume()
            }
        }
    }

    private fun configurarDragAndDrop() {
        apRoot.onDragOver = EventHandler { event ->
            if (event.gestureSource !== apRoot && event.dragboard.hasFiles()) {
                val aceito = event.dragboard.files.any { it.name.substringAfterLast('.', "").lowercase() in listOf("rar", "cbr", "xml") }
                event.acceptTransferModes(if (aceito) TransferMode.COPY else null)
                mostrarOverlayDrag(aceito)
            }
            event.consume()
        }

        apRoot.onDragExited = EventHandler { esconderOverlayDrag() }

        apRoot.onDragDropped = EventHandler { event ->
            val db = event.dragboard
            if (db.hasFiles()) {
                val archives = mutableListOf<File>()
                val xmls = mutableListOf<File>()

                val files = db.files ?: emptyList<File>()
                for (file in files) {
                    val ext = file.name.substringAfterLast('.', "").lowercase()
                    if (ext == "rar" || ext == "cbr") {
                        archives.add(file)
                    } else if (ext == "xml") {
                        xmls.add(file)
                    }
                }

                if (archives.isNotEmpty()) {
                    processaArquivosRar(archives)
                }

                if (xmls.isNotEmpty()) {
                    processaArquivoXml(xmls.first())
                }

                if (archives.isNotEmpty() || xmls.isNotEmpty()) {
                    event.isDropCompleted = true
                }
            }
            esconderOverlayDrag()
            event.consume()
        }
    }

    private fun mostrarOverlayDrag(aceito: Boolean) {
        val blur = BoxBlur(3.0, 3.0, 3)
        controllerPai.rootTab.effect = blur

        controllerPai.spDragDropZone.style = if (aceito)
            "-fx-border-color: white; -fx-border-style: dashed; -fx-border-width: 3; -fx-border-radius: 10; -fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 10;"
        else
            "-fx-border-color: red; -fx-border-style: dashed; -fx-border-width: 3; -fx-border-radius: 10; -fx-background-color: rgba(255,0,0,0.1); -fx-background-radius: 10;"

        controllerPai.lblDragDrop.text = if (aceito) "Arraste o arquivo aqui" else "Formato não aceito"
        controllerPai.lblDragDrop.textFill = if (aceito) Color.WHITE else Color.RED
        controllerPai.apDragOverlay.isVisible = true
    }

    private fun esconderOverlayDrag() {
        controllerPai.rootTab.effect = null
        controllerPai.apDragOverlay.isVisible = false
    }

    private fun processaArquivosRar(files: List<File>) {
        if (txtPasta.text.isNullOrEmpty()) {
            txtPasta.unFocusColor = Color.RED
            AlertasModal.alerta("Alerta", "Não informado a pasta para processamento.")
            return
        }

        val diretorio = txtPasta.text
        if (!mPASTA_TEMPORARIA.exists())
            mPASTA_TEMPORARIA.mkdirs()

        desabilita()
        controllerPai.setCursor(Cursor.WAIT)

        val task = object : Task<Void>() {
            override fun call(): Void? {
                var atualizado = false
                val regexVol = "(?i)Volume\\s*(\\d+[,.]?\\d*)".toRegex()
                val regexDelimitadores = Regex("(?i)\\s*(volume|capítulo|capitulo|chapter|-).*")

                val total = files.size.toLong()
                var atual = 0L

                files.forEach { file ->
                    atual++
                    updateProgress(atual, total)
                    updateMessage("Processando: ${file.name}")

                    val tempDir = File(mPASTA_TEMPORARIA, "process_arrastado_" + System.currentTimeMillis())
                    tempDir.mkdirs()

                    try {
                        val conteudo = mRarService.listarConteudo(file)
                        val itensCapa = conteudo.filter { it.contains("capa", ignoreCase = true) || it.contains("cover", ignoreCase = true) }

                        if (itensCapa.isNotEmpty()) {
                            mRarService.extrairItens(file, itensCapa, tempDir)
                        } else {
                            mLOG.info("Nenhuma pasta ou arquivo de capa encontrado no arquivo: ${file.name}")
                            return@forEach
                        }

                        val capa = tempDir.walk().filter { it.isDirectory && (it.name.contains("Capa", true) || it.name.contains("Cover", true)) }.firstOrNull()
                        if (capa != null) {
                            val pasta = capa.name
                            var mangaNome = pasta
                            if (mangaNome.contains("]"))
                                mangaNome = mangaNome.substringAfter("]").trim()

                            mangaNome = mangaNome.replace(regexDelimitadores, "").trim()
                            if (mangaNome.endsWith("-"))
                                mangaNome = mangaNome.substringBeforeLast("-").trim()

                            val sugestoes = mServiceManga.sugestao(mangaNome)
                            mangaNome = if (sugestoes.isNotEmpty()) sugestoes.first() else mangaNome

                            if (!atualizado) {
                                Platform.runLater {
                                    cbManga.value = mangaNome
                                    txtMalNome.text = mangaNome
                                    consultarMal()
                                }
                                atualizado = true
                            }

                            val volume = (regexVol.find(pasta)?.groups?.get(1)?.value?.replace(",", ".") ?: "0").toFloatOrNull() ?: 0f
                            val decimal = DecimalFormat("00.##", DecimalFormatSymbols(Locale.US))

                            // 1. Tenta localizar uma capa já existente na grid para este volume
                            var itemCapaExistente = mObsListaProcessar.find { it.isCapa && it.volume == volume }
                            
                            val pastaDestino: File
                            if (itemCapaExistente != null) {
                                pastaDestino = itemCapaExistente.pasta
                            } else {
                                // 2. Se não existir, gera o nome da pasta baseado no primeiro registro do volume na grid
                                val primeiroDoVolume = mObsListaProcessar.firstOrNull { it.volume == volume }
                                val nomePastaPadrao = if (primeiroDoVolume != null) {
                                    "[${primeiroDoVolume.scan}] ${primeiroDoVolume.nome} - Volume ${decimal.format(volume)} Capa"
                                } else {
                                    pasta // Fallback para o nome do RAR
                                }
                                pastaDestino = File(diretorio, nomePastaPadrao)
                            }

                            if (!pastaDestino.exists())
                                pastaDestino.mkdirs()

                            capa.copyRecursively(pastaDestino, overwrite = true)

                            Platform.runLater {
                                if (itemCapaExistente == null) {
                                    val primeiroDoVolume = mObsListaProcessar.firstOrNull { it.volume == volume }
                                    mObsListaProcessar.add(
                                        Pasta(
                                            pasta = pastaDestino,
                                            arquivo = pastaDestino.name,
                                            nome = primeiroDoVolume?.nome ?: mangaNome,
                                            volume = volume,
                                            scan = primeiroDoVolume?.scan ?: "",
                                            isCapa = true,
                                            isSelecionado = true
                                        )
                                    )
                                    mObsListaProcessar.sortWith(compareBy({ it.volume }, { it.capitulo }))
                                }
                                tbViewProcessar.refresh()
                            }

                        }
                    } catch (e: Exception) {
                        mLOG.error("Erro ao processar arquivo arrastado: ${file.name}", e)
                    } finally {
                        tempDir.deleteRecursively()
                    }
                }
                return null
            }

            override fun succeeded() {
                updateMessage("Processamento de capas concluído.")
                controllerPai.rootProgress.progressProperty().unbind()
                controllerPai.rootMessage.textProperty().unbind()
                controllerPai.clearProgress()
                habilita()
                Notificacoes.notificacao(Notificacao.SUCESSO, "Drag & Drop", "Processamento de capas concluído.")
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
    }

    private fun processaArquivoXml(file: File) {
        if (file.name.equals("ComicInfo.xml", ignoreCase = true)) {
            try {
                val jaxb = JAXBContext.newInstance(ComicInfo::class.java)
                val unmarshaller = jaxb.createUnmarshaller()
                val comicInfo = unmarshaller.unmarshal(file) as ComicInfo

                Platform.runLater {
                    mComicInfo = comicInfo
                    Notificacoes.notificacao(Notificacao.SUCESSO, "ComicInfo", "Metadados carregados do arquivo XML.")
                }
            } catch (e: Exception) {
                mLOG.error("Erro ao carregar ComicInfo.xml arrastado", e)
                Platform.runLater { AlertasModal.erro("Erro ao carregar XML", e.message ?: "Erro desconhecido") }
            }
        } else {
            mLOG.info("Arquivo XML ignorado por não ser 'ComicInfo.xml': ${file.name}")
        }
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        tbViewProcessar.selectionModel.selectionMode = SelectionMode.MULTIPLE
        tbViewProcessar.setRowFactory {
            object : TableRow<Pasta>() {
                override fun updateItem(item: Pasta?, empty: Boolean) {
                    super.updateItem(item, empty)
                    pseudoClassStateChanged(ALERTA_PSEUDO_CLASS, item?.isAlerta ?: false)
                }
            }
        }

        linkaCelulas()
        configuraTextEdit()
        configuraComboBox()
        
        // Inicializa o accessibleText dos botões que possuem estado (Carregar/Cancela)
        btnCarregar.accessibleText = "CARREGAR"
        btnValidar.accessibleText = "VALIDAR"
        btnGerarCapas.accessibleText = "GERAR"
        btnCompactar.accessibleText = "COMPACTAR"
        btnRenomear.accessibleText = "RENOMEAR"
        btnAjustarPastas.accessibleText = "AJUSTAR"
        
        habilita()

        tbViewMal.skinProperty().addListener { _, _, newSkin ->
            if (newSkin != null) {
                val flow = tbViewMal.lookup(".virtual-flow")
                if (flow != null) {
                    val vbar = flow.lookup(".scroll-bar:vertical") as ScrollBar?
                    vbar?.valueProperty()?.addListener { _, _, newValue ->
                        if (newValue.toDouble() == vbar.max && !isConsultandoMal && txtMalId.text.isEmpty() && mObsListaMal.size >= (Configuracao.registrosConsultaMal - 1)) {
                            consultarMal(mObsListaMal.size)
                        }
                    }
                }
            }
        }
    }

    private fun validarRegistros() {
        if (mObsListaProcessar.isEmpty()) return

        // Limpa alertas anteriores
        mObsListaProcessar.forEach { it.isAlerta = false }

        // Ordena apenas por capítulo para a validação (conforme feedback)
        val listaOrdenada = mObsListaProcessar.filter { !it.isCapa }.sortedBy { it.capitulo }
        val faltantes = mutableListOf<Int>()

        for (i in 0 until listaOrdenada.size - 1) {
            val atual = listaOrdenada[i]
            val proximo = listaOrdenada[i + 1]

            // Ignora capítulos de meia entrada (decimais) na validação
            val capAtual = Math.floor(atual.capitulo.toDouble()).toInt()
            val capProximo = Math.floor(proximo.capitulo.toDouble()).toInt()

            if (capProximo - capAtual > 1) {
                // Existe um buraco
                atual.isAlerta = true
                proximo.isAlerta = true

                for (f in (capAtual + 1) until capProximo) {
                    faltantes.add(f)
                }
            }
        }

        if (faltantes.isNotEmpty()) {
            val msg = "Capítulos não encontrados: ${faltantes.distinct().sorted().joinToString(", ")}"
            Notificacoes.notificacao(Notificacao.ALERTA, "Validação de Capítulos", msg)
        }

        tbViewProcessar.refresh()
    }

    companion object {
        val fxmlLocate: URL get() = TelaInicialController::class.java.getResource("/view/AbaPastas.fxml")
        var isAbaSelecionada = false
    }

}