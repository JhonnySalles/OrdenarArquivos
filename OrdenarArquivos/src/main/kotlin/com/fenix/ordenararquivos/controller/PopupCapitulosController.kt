package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.components.CheckBoxTableCellCustom
import com.fenix.ordenararquivos.components.TextAreaTableCell
import com.fenix.ordenararquivos.model.entities.Processar
import com.fenix.ordenararquivos.model.entities.capitulos.Capitulo
import com.fenix.ordenararquivos.model.entities.capitulos.Volume
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.notification.AlertasModal
import com.fenix.ordenararquivos.util.Utils
import com.jfoenix.controls.*
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Hyperlink
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.cell.ComboBoxTableCell
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.effect.BoxBlur
import javafx.scene.input.DragEvent
import javafx.scene.input.TransferMode
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.stage.FileChooser
import javafx.util.Callback
import org.jsoup.Jsoup
import org.jsoup.nodes.Comment
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class PopupCapitulosController : Initializable {

    @FXML
    private lateinit var hplMangaPlanet: Hyperlink

    @FXML
    private lateinit var hplComick: Hyperlink

    @FXML
    private lateinit var hplTaiyo: Hyperlink

    @FXML
    private lateinit var hplMangaFire: Hyperlink

    @FXML
    private lateinit var hplMangaForest: Hyperlink

    @FXML
    private lateinit var hplMangaRead: Hyperlink

    @FXML
    private lateinit var hplMangaDex: Hyperlink

    @FXML
    private lateinit var hplZBato: Hyperlink

    @FXML
    private lateinit var hplMangaPark: Hyperlink

    @FXML
    private lateinit var cbLinguagem: JFXComboBox<Linguagem>

    @FXML
    private lateinit var txtEndereco: JFXTextField

    @FXML
    private lateinit var btnExecutar: JFXButton

    @FXML
    private lateinit var btnArquivo: JFXButton

    @FXML
    private lateinit var cbMarcarTodos: JFXCheckBox

    @FXML
    private lateinit var tbViewTabela: TableView<Volume>

    @FXML
    private lateinit var clMarcado: TableColumn<Volume, Boolean>

    @FXML
    private lateinit var clArquivo: TableColumn<Volume, String>

    @FXML
    private lateinit var clVolume: TableColumn<Volume, Double>

    @FXML
    private lateinit var clCapitulos: TableColumn<Volume, String>

    @FXML
    private lateinit var clDescricoes: TableColumn<Volume, String>

    @FXML
    private lateinit var clTags: TableColumn<Volume, String>

    @FXML
    private lateinit var apRoot: AnchorPane

    @FXML
    private lateinit var apDragOverlay: AnchorPane

    @FXML
    private lateinit var lblDragDrop: Label

    @FXML
    private lateinit var spDragDropZone: StackPane

    private var mLista: ObservableList<Volume> = FXCollections.observableArrayList()
    private var mArquivos: List<String> = listOf()
    private var mProcessar: List<Processar> = listOf()

    @FXML
    private fun onBtnExecutar() {
        consulta()
    }

    @FXML
    private fun onBtnArquivo() {
        val arquivo = selecionaPasta(txtEndereco.text)
        if (arquivo != null)
            txtEndereco.text = arquivo.absolutePath
        else
            txtEndereco.text = ""
        consulta()
    }

    @FXML
    fun handleDragOver(event: DragEvent) {
        if (event.dragboard.hasFiles() || event.dragboard.hasString()) {
            val aceito = if (event.dragboard.hasFiles())
                event.dragboard.files.any { it.extension.lowercase() in listOf("txt", "html", "htm") }
            else true

            if (aceito) {
                event.acceptTransferModes(TransferMode.COPY)
            }
        }
        event.consume()
    }

    @FXML
    fun handleDragEntered(event: DragEvent) {
        if (event.dragboard.hasFiles() || event.dragboard.hasString()) {
            val aceito = if (event.dragboard.hasFiles())
                event.dragboard.files.any { it.extension.lowercase() in listOf("txt", "html", "htm") }
            else true
            mostrarOverlayDrag(aceito)
        }
        event.consume()
    }

    @FXML
    fun handleDragExited(event: DragEvent) {
        esconderOverlayDrag()
        event.consume()
    }

    @FXML
    fun handleDragDropped(event: DragEvent) {
        val db = event.dragboard
        var success = false
        if (db.hasFiles()) {
            db.files.firstOrNull { it.extension.lowercase() in listOf("txt", "html", "htm") }?.let { file ->
                if (file.extension.lowercase() == "txt")
                    extractManualText(file.readText(Charsets.UTF_8))
                else {
                    txtEndereco.text = file.absolutePath
                    consulta()
                }
                success = true
            }
        } else if (db.hasString()) {
            extractManualText(db.string)
            success = true
        }
        event.isDropCompleted = success
        esconderOverlayDrag()
        event.consume()
    }

    private fun mostrarOverlayDrag(aceito: Boolean) {
        spDragDropZone.style = if (aceito)
            "-fx-background-color: rgba(0, 0, 0, 0.7); -fx-border-color: #0cff00; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;"
        else
            "-fx-background-color: rgba(0, 0, 0, 0.7); -fx-border-color: #ff0000; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;"

        lblDragDrop.text = if (aceito) "Arraste o arquivo aqui" else "Formato não aceito"
        lblDragDrop.textFill = if (aceito) Color.WHITE else Color.RED
        apDragOverlay.isVisible = true
    }

    private fun esconderOverlayDrag() {
        apDragOverlay.isVisible = false
    }

    private fun atualizaCheckMarcarTodos() {
        cbMarcarTodos.isSelected = mLista.isNotEmpty() && mLista.all { it.marcado }
    }

    private fun extractManualText(texto: String) {
        val volumesMap = mutableMapOf<Double, Volume>()

        // Regex para capturar Volume e Capítulo de forma flexível (prefixos opcionais)
        val volRegex = Regex("(?:Volume|Vol\\.?)\\s*(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE)
        val capRegex = Regex("(?:(?:Chapter|Ch\\.?|Capítulo|Cap\\.?)\\s*)?(\\d+(?:\\.\\d+)?)(?:\\s*[:\\-]?\\s*(.*))?", RegexOption.IGNORE_CASE)

        texto.lines().forEach { linha ->
            val l = linha.trim()
            if (l.isEmpty()) return@forEach

            val volMatch = volRegex.find(l)
            val capMatch = capRegex.find(l)

            if (capMatch != null) {
                val chapterNumber = capMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                val title = capMatch.groupValues.getOrNull(2)?.trim() ?: ""

                // Se houver volume na linha, usa ele, senão usa 0.0
                val volumeNumber = volMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

                val volume = volumesMap.getOrPut(volumeNumber) { Volume(volume = volumeNumber) }
                val capitulo = Capitulo(capitulo = chapterNumber, ingles = title, japones = "")
                volume.capitulos.add(capitulo)
            }
        }

        if (volumesMap.isNotEmpty()) {
            val lista = if (volumesMap.size == 1 && volumesMap.keys.first() == 0.0) {
                // Se só tem um volume e é 0.0, transformamos em -1.0 para forçar o match por capítulo/tag no preparar
                val vol = volumesMap.values.first()
                listOf(Volume(volume = -1.0, capitulos = vol.capitulos))
            } else {
                volumesMap.values.toList()
            }

            preparar(lista)
            cbMarcarTodos.isSelected = true
            marcarTodos()
        }
    }


    @FXML
    private fun marcarTodos() {
        mLista.forEach { it.marcado = cbMarcarTodos.isSelected }
        tbViewTabela.refresh()
    }

    private fun selecionaPasta(pasta: String): File? {
        val fileChooser = FileChooser()
        fileChooser.title = "Selecione o arquivo."
        if (pasta.isNotEmpty()) {
            val initial = File(pasta)
            if (initial.isDirectory)
                fileChooser.initialDirectory = initial
            else
                fileChooser.initialDirectory = File(initial.parent)
        }
        return fileChooser.showOpenDialog(null)
    }

    fun setArquivos(arquivos: List<String>) {
        mArquivos = arquivos
        clArquivo.cellFactory = ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(arquivos))
        clArquivo.cellValueFactory = PropertyValueFactory("arquivo")
    }

    fun setLinguagem(linguagem: Linguagem) = cbLinguagem.selectionModel.select(linguagem)

    fun setProcessar(processar: List<Processar>) {
        mProcessar = processar
    }

    private fun consulta() {
        if (txtEndereco.text.isNullOrEmpty())
            return

        try {
            var site = txtEndereco.text
            val pagina: Document = try {
                if (site.contains("https:") || site.contains("http:"))
                    Jsoup.connect(site)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                        .header("Accept-Language", "en-US,en;q=0.9,pt-BR;q=0.8,pt;q=0.7")
                        .header("Connection", "keep-alive")
                        .header("Upgrade-Insecure-Requests", "1")
                        .referrer("http://www.google.com")
                        .get()
                else {
                    val file = File(txtEndereco.text)
                    val pagina = Jsoup.parse(file)
                    for (node in pagina.childNodes())
                        if (node is Comment) {
                            val commentData = node.data.trim()
                            if (commentData.startsWith("saved from url=")) {
                                site = "http" + commentData.substringAfter("http").substringBefore("->")
                                break
                            }
                        }

                    // Se não encontrou o comentário, tenta inferir pelo nome do arquivo (útil para testes)
                    if (!site.startsWith("http")) {
                        val fileName = file.name.lowercase()
                        if (fileName.contains("mangaplanet")) site = "https://mangaplanet.com"
                        else if (fileName.contains("comick")) site = "https://comick.io"
                        else if (fileName.contains("mangafire")) site = "https://mangafire.to"
                        else if (fileName.contains("taiyo")) site = "https://taiyo.moe"
                        else if (fileName.contains("mangapark")) site = "https://mangapark.net"
                        else if (fileName.contains("mangaforest")) site = "https://mangaforest.me"
                        else if (fileName.contains("mangaread")) site = "https://mangaread.org"
                        else if (fileName.contains("mangadex")) site = "https://mangadex.org"
                        else if (fileName.contains("zbato")) site = "https://zbato.org"
                    }

                    pagina
                }
            } catch (e: IOException) {
                LOGGER.error(e.message, e)
                AlertasModal.erro("Erro ao carregar o site", e.message.toString())
                return
            } catch (e: Exception) {
                LOGGER.error(e.message, e)
                AlertasModal.erro("Erro ao carregar o site", e.message.toString())
                return
            }

            site.lowercase().let {
                val list = if (it.contains("mangaplanet.com"))
                    extractMangaPlanet(pagina)
                else if (it.contains("comick."))
                    extractComick(pagina)
                else if (it.contains("mangafire.to"))
                    extractMangaFire(pagina)
                else if (it.contains("taiyo.moe"))
                    extractTayo(pagina)
                else if (it.contains("mangapark"))
                    extractMangaPark(pagina)
                else if (it.contains("mangaforest.me"))
                    extractMangaForest(pagina)
                else if (it.contains("mangaread.org"))
                    extractMangaRead(pagina)
                else if (it.contains("mangadex.org"))
                    extractMangaDex(pagina)
                else if (it.contains("zbato.org"))
                    extractZBato(pagina)
                else
                    mLista.toList()

                preparar(list)
            }
        } catch (e: Exception) {
            LOGGER.error(e.message, e)
            AlertasModal.erro("Erro ao realizar o processamento do site", e.message.toString())
        }
    }

    private val mFormater = DecimalFormat("00.##", DecimalFormatSymbols(Locale.US))
    private fun formatar(valor: Double): String = mFormater.format(valor)

    //Regex case insentive, no qual pode começar com capítulo, numero ou formato japoneas.
    private val mReplace = "(?i)^(ch|chapter|episode|第|[0-9])[0-9０-９ .]+(話|:)?".toRegex()

    private fun preparar(lista: List<Volume>) {
        val linguagem = cbLinguagem.value
        val capRegex = Regex("(?:(?:Chapter|Capítulo|Ch\\.?|Cap\\.?|第)\\s*)?(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE)

        // Criamos uma lista flat de todos os capítulos da fonte, lembrando seu volume original
        data class SourceItem(val vol: Double, val cap: Capitulo)

        val sourcePool = mutableListOf<SourceItem>()
        lista.forEach { v ->
            v.capitulos.forEach { c -> sourcePool.add(SourceItem(v.volume, c)) }
        }

        val volumesResult = mutableListOf<Volume>()

        if (mProcessar.isNotEmpty()) {
            // Primeiro, identificamos o range de capítulos e volume de cada registro do mProcessar
            val processarInfos = mProcessar.map { p ->
                val numeros = p.tags.split("\n").mapNotNull { tag ->
                    var capitulo = tag.substringAfter(Utils.SEPARADOR_IMAGEM).trim()
                    if (capitulo.endsWith(Utils.SEPARADOR_IMPORTACAO))
                        capitulo = capitulo.substringBefore(Utils.SEPARADOR_IMPORTACAO).trim()

                    capRegex.find(capitulo)?.groupValues?.get(1)?.toDoubleOrNull()
                }
                val vol = p.comicInfo?.volume?.toDouble() ?: -1.0
                object {
                    val processar = p
                    val volume = vol
                    val min = numeros.minOrNull() ?: Double.MAX_VALUE
                    val max = numeros.maxOrNull() ?: Double.MIN_VALUE
                    val tagCapitulos = numeros
                }
            }

            for (info in processarInfos) {
                val capitulosEncontrados = mutableListOf<Capitulo>()

                // 2.1 - Localizar por volume e capítulo (Exato)
                // Percorremos os capítulos que este arquivo "deveria" ter conforme suas tags
                info.tagCapitulos.forEach { numCapTag ->
                    val found = sourcePool.find { it.vol == info.volume && it.cap.capitulo == numCapTag }
                    if (found != null) {
                        capitulosEncontrados.add(found.cap)
                        sourcePool.remove(found)
                    }
                }

                // 2.2 - Caso não encontrado, procurar apenas por capítulo (Independente de volume)
                // Só tentamos os capítulos das tags que ainda não foram preenchidos
                val restantesTags = info.tagCapitulos.filter { tagNum -> capitulosEncontrados.none { it.capitulo == tagNum } }
                restantesTags.forEach { numCapTag ->
                    val found = sourcePool.find { it.cap.capitulo == numCapTag }
                    if (found != null) {
                        capitulosEncontrados.add(found.cap)
                        sourcePool.remove(found)
                    }
                }

                // 2.3 - Localizar por proximidade (capítulos entre os valores do volume/arquivo)
                if (info.min <= info.max) {
                    val proximos = sourcePool.filter { it.cap.capitulo >= info.min && it.cap.capitulo <= info.max }
                    capitulosEncontrados.addAll(proximos.map { it.cap })
                    sourcePool.removeAll(proximos)
                }

                val tags = capitulosEncontrados.sortedBy { it.capitulo }.joinToString(separator = "\n") {
                    val num = formatar(it.capitulo)
                    val title = if (linguagem == Linguagem.JAPANESE && it.japones.isNotEmpty()) it.japones else it.ingles
                    val cleanTitle = Utils.limparTitulo(title)
                    val label = "Capítulo $num"
                    "-1${Utils.SEPARADOR_IMAGEM}$label ${Utils.SEPARADOR_IMPORTACAO} $num${Utils.SEPARADOR_CAPITULO}$cleanTitle"
                }
                volumesResult.add(Volume(arquivo = info.processar.arquivo, volume = info.volume, capitulos = capitulosEncontrados, tags = tags))
            }

            // Capítulos não localizados vão para o volume 0
            if (sourcePool.isNotEmpty()) {
                val caps = sourcePool.map { it.cap }.sortedBy { it.capitulo }
                val tags = caps.joinToString(separator = "\n") {
                    val num = formatar(it.capitulo)
                    val title = if (linguagem == Linguagem.JAPANESE && it.japones.isNotEmpty()) it.japones else it.ingles
                    val cleanTitle = Utils.limparTitulo(title)
                    val label = "Capítulo $num"
                    "-1${Utils.SEPARADOR_IMAGEM}$label ${Utils.SEPARADOR_IMPORTACAO} $num${Utils.SEPARADOR_CAPITULO}$cleanTitle"
                }
                volumesResult.add(Volume(arquivo = "Não Localizados", volume = 0.0, capitulos = caps.toMutableList(), tags = tags))
            }
        } else {
            // Se mProcessar estiver vazio, apenas exibe o que veio da lista
            for (item in lista) {
                item.tags = item.capitulos.joinToString(separator = "\n") {
                    val num = formatar(it.capitulo)
                    val title = if (linguagem == Linguagem.JAPANESE && it.japones.isNotEmpty()) it.japones else it.ingles
                    val cleanTitle = Utils.limparTitulo(title)
                    val label = "Capítulo $num"
                    "-1${Utils.SEPARADOR_IMAGEM}$label ${Utils.SEPARADOR_IMPORTACAO} $num${Utils.SEPARADOR_CAPITULO}$cleanTitle"
                }
                item.arquivo = mArquivos.find { it.lowercase().contains("volume " + formatar(item.volume)) } ?: ""
                volumesResult.add(item)
            }
        }

        mLista = FXCollections.observableArrayList(volumesResult)
        tbViewTabela.items = mLista
        tbViewTabela.refresh()
    }

    private fun atualizarTags(volume: Volume) {
        val linguagem = cbLinguagem.value
        volume.tags = volume.capitulos.joinToString(separator = "\n") {
            val num = formatar(it.capitulo)
            val title = if (linguagem == Linguagem.JAPANESE && it.japones.isNotEmpty()) it.japones else it.ingles
            val cleanTitle = Utils.limparTitulo(title)
            val label = "Capítulo $num"
            "-1${Utils.SEPARADOR_IMAGEM}$label ${Utils.SEPARADOR_IMPORTACAO} $num${Utils.SEPARADOR_CAPITULO}$cleanTitle"
        }
    }

    private fun popupDividir() {
        val volume = tbViewTabela.selectionModel.selectedItem ?: return
        if (volume.capitulos.isEmpty()) return

        try {
            val dialogLayout = JFXDialogLayout()
            val subDialog = JFXDialog(stackPane, dialogLayout, JFXDialog.DialogTransition.CENTER)

            val loader = FXMLLoader(PopupCapitulosController::class.java.getResource("/view/PopupCapitulosDividir.fxml"))
            val newAnchorPane: Parent = loader.load()
            val cnt: PopupCapitulosDividirController = loader.getController()

            cnt.setRange(
                volume.capitulos.minByOrNull { it.capitulo }?.capitulo ?: 0.0,
                volume.capitulos.maxByOrNull { it.capitulo }?.capitulo ?: 0.0
            )

            val titulo = Label("Dividir Volume")
            titulo.style = "-fx-text-fill: white; -fx-font-size: 18px;"
            dialogLayout.setHeading(titulo)
            dialogLayout.setBody(newAnchorPane)

            val btnConfirmarSplit = JFXButton("Confirmar")
            btnConfirmarSplit.styleClass.addAll("background-Green2", "texto-stilo-1")
            btnConfirmarSplit.setOnAction {
                val inicio = cnt.getInicio()
                val fim = cnt.getFim()

                val extraidos = volume.capitulos.filter { it.capitulo in inicio..fim }
                if (extraidos.isNotEmpty()) {
                    volume.capitulos.removeAll(extraidos)
                    val novoVolume = Volume(
                        marcado = volume.marcado,
                        arquivo = "Não Localizados",
                        volume = 0.0,
                        capitulos = extraidos.toMutableList()
                    )
                    atualizarTags(volume)
                    atualizarTags(novoVolume)
                    val index = mLista.indexOf(volume)
                    mLista.add(index + 1, novoVolume)
                    tbViewTabela.refresh()
                }
                subDialog.close()
            }

            val btnCancelarSplit = JFXButton("Cancelar")
            btnCancelarSplit.styleClass.addAll("background-White1")
            btnCancelarSplit.setOnAction { subDialog.close() }

            dialogLayout.setActions(btnConfirmarSplit, btnCancelarSplit)
            dialogLayout.styleClass.add("dialog-black")
            subDialog.stylesheets.add(STYLE_SHEET)
            subDialog.isOverlayClose = true
            subDialog.show()
        } catch (e: Exception) {
            LOGGER.error(e.message, e)
        }
    }

    private fun openSite(site: String) {
        try {
            Desktop.getDesktop().browse(URI(site))
        } catch (e: IOException) {
            LOGGER.error(e.message, e)
        } catch (e: URISyntaxException) {
            LOGGER.error(e.message, e)
        }
    }

    //<--------------------------  Manga Planet  -------------------------->
    internal fun extractMangaPlanet(pagina: Document): List<Volume> {
        val volumes = mutableListOf<Volume>()

        // Select each accordion item, which groups chapters under a volume title
        val accordionItems = pagina.select("div[id^=accordion_] > div.card.mt-4.select-options")

        for (item in accordionItems) {
            // Extract volume title element
            val volumeTitleElement = item.selectFirst("div.card-body.book-detail.panel-collapse h3[id^=vol_title_]")
            if (volumeTitleElement != null) {
                val volumeTitleText = volumeTitleElement.text() // e.g., "Volume 1", "Volume 17-18"
                val volumeNumber = parseVolumeNumber(volumeTitleText)
                val currentVolume = Volume(volume = volumeNumber)

                // Chapters are in <ul> elements with id starting with "epi"
                val chapterUls = item.select("ul[id^=epi]")
                for (ulElement in chapterUls) {
                    val chapterIdFull = ulElement.id() // e.g., "epi1", "epi14"
                    val chapterNumberString = chapterIdFull.removePrefix("epi")
                    val chapterNumber = chapterNumberString.toDoubleOrNull()

                    if (chapterNumber != null) {
                        val listItem = ulElement.selectFirst("li.list-group-item")
                        if (listItem != null) {
                            val inglesTitulo = listItem.selectFirst("h3")?.let { it.selectFirst("p")?.text() ?: it.text() }?.trim() ?: ""
                            val japanesTitulo = listItem.selectFirst("p span.jp_fonts")?.text()?.trim() ?: ""

                            val ingles = cleanEnglishTitle(inglesTitulo)
                            val japones = cleanJapaneseTitle(japanesTitulo)

                            if (ingles.isNotBlank() || japones.isNotBlank())
                                currentVolume.capitulos.add(
                                    Capitulo(
                                        capitulo = chapterNumber,
                                        ingles = ingles.replace(mReplace, "").trim(),
                                        japones = japones.replace(mReplace, "").trim()
                                    )
                                )
                        }
                    }
                }
                if (currentVolume.capitulos.isNotEmpty())
                    volumes.add(currentVolume)
            }
        }
        return volumes
    }

    private fun parseVolumeNumber(volumeTitle: String): Double {
        // Extracts the first number from strings like "Volume 1", "Volume 17-18", "Volume 27-28"
        val regex = """Volume\s*(\d+)""".toRegex()
        val matchResult = regex.find(volumeTitle)
        return matchResult?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    }

    private fun cleanEnglishTitle(rawTitle: String): String {
        // Removes prefixes like "CHAPTER X: ", "BONUS CHAPTER: ", "Final Chapter: "
        var title = rawTitle
        title = title.replaceFirst(
            """^(?:CHAPTER\s*[\d.]*\s*:\s*|BONUS CHAPTER\s*:\s*|Special one-shot\s*:\s*|Final Chapter\s*:\s*|CHAPTER\s*[\d.]*\s*-\s*|CHAPTER\s*[\d.]*\s+|CH\.[\d.]* )""".toRegex(
                RegexOption.IGNORE_CASE
            ), ""
        )
        // Specific case for "CHAPTER X: Title" without space after colon found in data
        title = title.replaceFirst("""^CHAPTER\s*\d+:""".toRegex(RegexOption.IGNORE_CASE), "")
        // For titles like "াCHAPTER 206: You've ended up this way."
        title = title.replaceFirst("""^[^A-Za-z0-9]*CHAPTER\s*[\d.]*\s*:?\s*""".toRegex(RegexOption.IGNORE_CASE), "")
        title = title.replaceFirst("""^[^A-Za-z0-9]*CHAPTER\s*[\d.]*\s*:?\s*""".toRegex(RegexOption.IGNORE_CASE), "")
        return title.trim()
    }

    private fun cleanJapaneseTitle(rawTitle: String): String {
        // Removes prefixes like "第X話 ", "番外編 ", "最終話 "
        return rawTitle.replaceFirst("""^(?:第[\d.]+話\s*|番外編\s*|特別読み切り\s*|最終話\s*)""".toRegex(), "").trim()
    }

    //<--------------------------  Comick  -------------------------->
    internal fun extractComick(pagina: Document): List<Volume> {
        val rawChapterEntries = mutableListOf<TempCapInfo>()
        val chapterRows = pagina.select("table tbody tr.group")

        chapterRows.forEachIndexed { index, row ->
            val linkElement = row.selectFirst("td a div.truncate")
            if (linkElement != null) {
                val chapNumString = linkElement.selectFirst("span.font-semibold[title^=Chapter]")?.text()
                    ?.replace("Ch.", "", ignoreCase = true)?.trim()
                val chapNum = chapNumString?.toDoubleOrNull()

                if (chapNum != null) {
                    var volNum: Double? = null
                    var title = ""

                    val spans = linkElement.children().filterIsInstance<Element>().filter { it.tagName() == "span" }
                    // O primeiro span é o número do capítulo, já processado.
                    // O segundo span PODE ser o volume.
                    if (spans.size > 1) {
                        val potentialVolSpan = spans[1]
                        if (potentialVolSpan.text().contains("Vol.", ignoreCase = true)) {
                            volNum = potentialVolSpan.text().replace("Vol.", "", ignoreCase = true)
                                .trim().toDoubleOrNull()
                            // Se há span de volume, o título é o próximo span, se existir
                            if (spans.size > 2 && spans[2].hasClass("text-xs"))
                                title = spans[2].text().trim()
                        } else if (potentialVolSpan.hasClass("text-xs")) {
                            // Não há span de volume, este é o span do título
                            title = potentialVolSpan.text().trim()
                        }
                    }
                    // Se o título ainda estiver vazio e houver um terceiro span (sem span de volume no meio)
                    // Isso pode acontecer se o span de volume não for detectado corretamente ou não existir
                    // e o título estiver no terceiro span (após o span do número do capítulo e um span vazio/diferente).
                    // Neste HTML específico, o título está no span com classe "text-xs".
                    val titleSpan = linkElement.selectFirst("span.text-xs")
                    if (title.isBlank() && titleSpan != null)
                        title = titleSpan.text().trim()

                    rawChapterEntries.add(TempCapInfo(volNum, chapNum, title, index))
                }
            }
        }

        // Deduplicar capítulos, priorizando aqueles com descrição e depois com número de volume
        val finalChapterMap = mutableMapOf<Double, TempCapInfo>() // Key: chapNum
        for (currentEntry in rawChapterEntries.sortedBy { it.sortKey }) { // Manter ordem original para desempate
            val existingEntry = finalChapterMap[currentEntry.chap]

            if (existingEntry == null) {
                finalChapterMap[currentEntry.chap] = currentEntry
            } else {
                // Priorizar entrada com título
                val currentHasTitle = currentEntry.title.isNotBlank()
                val existingHasTitle = existingEntry.title.isNotBlank()

                if (currentHasTitle && !existingHasTitle) {
                    finalChapterMap[currentEntry.chap] = currentEntry
                } else if (currentHasTitle == existingHasTitle) {
                    // Se ambos têm título (ou ambos não têm), priorizar o que tem volume
                    if (currentEntry.vol != null && existingEntry.vol == null) {
                        finalChapterMap[currentEntry.chap] = currentEntry
                    }
                    // Se ambos têm título e volume (ou nenhum tem volume), o primeiro encontrado (mantido pela ordenação por sortKey) é mantido.
                }
            }
        }

        // Agrupar capítulos finais por volume
        val volumesMap = mutableMapOf<Double, Volume>()
        for (finalEntry in finalChapterMap.values.sortedBy { it.chap }) { // Processar em ordem de capítulo
            val volNum = finalEntry.vol ?: -1.0
            val volume = volumesMap.getOrPut(volNum) { Volume(volume = volNum) }
            volume.capitulos.add(Capitulo(capitulo = finalEntry.chap, ingles = finalEntry.title.replace(mReplace, "").trim(), ""))
        }

        // Ordenar capítulos dentro de cada volume e os próprios volumes
        volumesMap.values.forEach { it.capitulos.sortBy { chap -> chap.capitulo } }
        return volumesMap.values.toList().sortedBy { it.volume }
    }

    // Data class temporária para ajudar no processamento e deduplicação
    private data class TempCapInfo(
        val vol: Double?,
        val chap: Double,
        val title: String,
        val sortKey: Int // Para manter a ordem original da tabela para desempate
    )

    //<--------------------------  Manga Fire  -------------------------->
    internal fun extractMangaFire(pagina: Document): List<Volume> {
        val volumesMap = mutableMapOf<Double, Volume>()
        val volRegex = Regex("(?:Volume|Vol\\.?)\\s*(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE)

        val chapterListContainer = pagina.selectFirst("ul.scroll-sm")
        if (chapterListContainer != null) {
            val chapterItems = chapterListContainer.select("li.item")

            for (itemElement in chapterItems) {
                val chapNumString = itemElement.attr("data-number")
                val chapNum = chapNumString.toDoubleOrNull()

                if (chapNum != null) {
                    var title = ""
                    var volNum = -1.0
                    val linkElement = itemElement.selectFirst("a")
                    if (linkElement != null) {
                        val firstSpan = linkElement.selectFirst("span:first-child")
                        val fullText = firstSpan?.text()?.trim() ?: ""

                        volRegex.find(fullText)?.let {
                            volNum = it.groupValues[1].toDoubleOrNull() ?: -1.0
                        }

                        val titleRegex = """^.*?(?:Chapter|Ch\.?|Ch)\s*[\d.]+(?::\s*|\s+)(.*)""".toRegex(RegexOption.IGNORE_CASE)
                        val matchResult = titleRegex.find(fullText)
                        title = matchResult?.groupValues?.get(1)?.trim() ?: ""

                        if (title.isEmpty() && !fullText.matches("""(?i)^.*?(?:Chapter|Ch\.?|Ch)\s*[\d.]+$""".toRegex())) {
                            title = fullText
                        }
                    }
                    val volume = volumesMap.getOrPut(volNum) { Volume(volume = volNum) }
                    volume.capitulos.add(Capitulo(capitulo = chapNum, ingles = title.replace(mReplace, "").trim(), japones = ""))
                }
            }
        }

        volumesMap.values.forEach { it.capitulos.sortBy { it.capitulo } }
        return volumesMap.values.sortedBy { it.volume }
    }

    //<--------------------------  Tayo -------------------------->
    internal fun extractTayo(pagina: Document): List<Volume> {
        val volumesList = mutableListOf<Volume>()

        // Seleciona cada bloco de volume
        // Tenta o seletor antigo primeiro
        var volumeSections = pagina.select("div[data-open=true]:has(h2 button span.text-foreground)")
        if (volumeSections.isEmpty()) {
            // Tenta um seletor mais genérico baseado no HTML fornecido (Wata - Tai.html)
            // Onde o volume está em um span com data-open="true"
            volumeSections = pagina.select("div:has(span[data-open=true].text-foreground)")
        }

        if (volumeSections.isEmpty()) {
            // Se ainda assim não encontrar, tenta pegar todos os capítulos e colocar num volume genérico
            val volume = Volume(volume = -1.0)
            val chapterContainers = pagina.select("div.flex.flex-col.gap-1:has(p.line-clamp-1)")
            for (container in chapterContainers) {
                val pElement = container.selectFirst("p.line-clamp-1")
                val fullText = pElement?.text()?.trim() ?: ""

                val chapNumSpan = pElement?.selectFirst("span.font-medium")
                val chapNumStr = chapNumSpan?.text()?.trim()
                val chapNum = chapNumStr?.toDoubleOrNull() ?: extractChapterNumber(fullText)

                if (chapNum != null) {
                    val title = cleanTaiyoTitle(fullText, chapNum.toString())
                    volume.capitulos.add(Capitulo(capitulo = chapNum, ingles = title, japones = ""))
                }
            }
            if (volume.capitulos.isNotEmpty()) {
                volume.capitulos.sortBy { it.capitulo }
                volumesList.add(volume)
            }
        } else {
            for (volumeSectionDiv in volumeSections) {
                val volumeTitleElement = volumeSectionDiv.selectFirst("h2 button span.text-foreground, span[data-open=true].text-foreground")
                val volumeTitleText = volumeTitleElement?.text()?.trim()

                if (volumeTitleText != null) {
                    val volumeNumberString = volumeTitleText.replace("Volume", "", ignoreCase = true).trim()
                    val volumeNum = volumeNumberString.toDoubleOrNull() ?: 0.0

                    val currentVolume = Volume(volume = volumeNum)
                    val chapterContainers = volumeSectionDiv.select("section > div.py-2 > div.flex.flex-col.gap-1, div.flex.flex-col.gap-1:has(p.line-clamp-1)")

                    for (chapterContainer in chapterContainers) {
                        val pElement = chapterContainer.selectFirst("p.line-clamp-1")
                        val fullText = pElement?.text()?.trim() ?: ""

                        val chapNumSpan = pElement?.selectFirst("span.font-medium")
                        val chapNumStr = chapNumSpan?.text()?.trim()
                        val chapNum = chapNumStr?.toDoubleOrNull() ?: extractChapterNumber(fullText)

                        if (chapNum != null) {
                            val title = cleanTaiyoTitle(fullText, chapNum.toString())
                            currentVolume.capitulos.add(Capitulo(capitulo = chapNum, ingles = title, japones = ""))
                        }
                    }
                    if (currentVolume.capitulos.isNotEmpty()) {
                        currentVolume.capitulos.sortBy { it.capitulo }
                        volumesList.add(currentVolume)
                    }
                }
            }
        }

        volumesList.sortBy { it.volume }
        return volumesList
    }

    private fun extractChapterNumber(text: String): Double? {
        val regex = """(?:Capítulo|Chapter)\s*([\d.]+)""".toRegex(RegexOption.IGNORE_CASE)
        return regex.find(text)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun cleanTaiyoTitle(fullText: String, chapNum: String): String {
        var title = fullText.replaceFirst("""^(?:Capítulo|Chapter)\s*$chapNum\s*[—\-\:]\s*""".toRegex(RegexOption.IGNORE_CASE), "")
        if (title == fullText) {
            title = fullText.replaceFirst("""^(?:Capítulo|Chapter)\s*$chapNum\s*""".toRegex(RegexOption.IGNORE_CASE), "")
        }
        // Se o título resultante for igual ao número do capítulo ou apenas "Capítulo X", limpamos
        if (title.trim().equals("Capítulo $chapNum", ignoreCase = true) || title.trim().isEmpty()) {
            return ""
        }
        return title.trim().replace(mReplace, "").trim()
    }

    //<--------------------------  MangaForest -------------------------->
    internal fun extractMangaForest(pagina: Document): List<Volume> {
        val volumesMap = mutableMapOf<Double, Volume>()
        val chapterList = pagina.getElementById("chapter-list-inner")
        val chapters = chapterList?.getElementsByTag("li")

        // Regex para capturar o número do volume (opcional), número do capítulo e descrição
        val regex = Regex("(?:Vol(?:ume)?\\s?(\\d+(?:\\.\\d+)?))?\\s?(?:Chapter|Chap|Extra)\\s?([\\d.]+|Extra(?: V\\d+)?)\\s?:?\\s?(.*)", RegexOption.IGNORE_CASE)

        chapters?.forEach { li ->
            val title = li.getElementsByTag("strong").first()?.text()
            if (title != null) {
                val matchResult = regex.find(title)
                if (matchResult != null) {
                    val (volumeStr, chapterStr, description) = matchResult.destructured

                    val volumeNumber = volumeStr.toDoubleOrNull() ?: -1.0

                    // Tratamento especial para casos como "Extra V2"
                    val chapterNumberCleaned = chapterStr.replace(Regex("[^\\d.]"), "")
                    val chapterNumber = chapterNumberCleaned.toDoubleOrNull() ?: 0.0

                    val volume = volumesMap.getOrPut(volumeNumber) {
                        Volume(volume = volumeNumber)
                    }

                    val capitulo = Capitulo(capitulo = chapterNumber, ingles = description.trim(), japones = "")
                    volume.capitulos.add(capitulo)
                }
            }
        }

        return volumesMap.values.sortedBy { it.volume }
    }

    //<--------------------------  MangaRead -------------------------->
    internal fun extractMangaRead(pagina: Document): List<Volume> {
        val volumesMap = mutableMapOf<Double, Volume>()
        val chapterSelector = pagina.selectFirst("div.c-selectpicker.selectpicker_chapter")
        val chapterOptions = chapterSelector?.select("option") ?: emptyList()

        val regex = Regex("(?:Chapter|ch\\.|Chap)\\s*([\\d.]+)\\s*:?\\s*(.*)", RegexOption.IGNORE_CASE)
        val volRegex = Regex("(?:Volume|Vol\\.?)\\s*(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE)

        for (option in chapterOptions) {
            val text = option.text().trim()
            val matchResult = regex.find(text)

            if (matchResult != null) {
                val (chapterStr, description) = matchResult.destructured
                chapterStr.toDoubleOrNull()?.let { chapterNumber ->
                    var volNum = -1.0
                    volRegex.find(text)?.let { volNum = it.groupValues[1].toDoubleOrNull() ?: -1.0 }

                    val volume = volumesMap.getOrPut(volNum) { Volume(volume = volNum) }
                    val capitulo = Capitulo(capitulo = chapterNumber, ingles = description.trim(), japones = "")
                    volume.capitulos.add(capitulo)
                }
            }
        }

        volumesMap.values.forEach { it.capitulos.sortBy { it.capitulo } }
        return volumesMap.values.sortedBy { it.volume }
    }

    //<--------------------------  MangaDex -------------------------->
    internal fun extractMangaDex(pagina: Document): List<Volume> {
        val volumesMap = mutableMapOf<Double, Volume>()
        val numberRegex = Regex("""[\d.]+""")
        val selectedLang = cbLinguagem.selectionModel.selectedItem
        val langNameRequested = selectedLang?.name?.lowercase() ?: "portuguese"

        // Localiza todos os cabeçalhos de capítulos
        val chapterHeaders = pagina.select(".chapter-header")

        for (header in chapterHeaders) {
            // Extrai o número do capítulo (ex: "Chapter 118")
            val chapterNumber = numberRegex.find(header.text())?.value?.toDoubleOrNull() ?: continue

            // Tenta localizar o volume associado percorrendo os pais até encontrar algo com "Volume"
            var volumeNumber = -1.0
            var currentParent = header.parent()
            while (currentParent != null) {
                val volMatch = Regex("""(?i)Volume\s*([\d.]+)""").find(currentParent.text())
                if (volMatch != null) {
                    volumeNumber = volMatch.groupValues[1].toDoubleOrNull() ?: -1.0
                    break
                }
                currentParent = currentParent.parent()
            }

            val currentVolume = volumesMap.getOrPut(volumeNumber) { Volume(volume = volumeNumber) }

            // O contêiner pai do header geralmente contém as versões de línguas
            val container = header.parent()
            val languageVersions = container?.select(".chapter.relative.read") ?: emptyList()

            var extractedTitle = ""
            var fallbackTitle = ""

            for (version in languageVersions) {
                val langImg = version.selectFirst("img")
                val langTitle = langImg?.attr("title")?.lowercase() ?: ""
                val description = version.selectFirst(".line-clamp-1")?.text()?.trim() ?: ""

                // Se for a língua selecionada, prioriza
                if (langTitle.contains(langNameRequested) || (langNameRequested == "portuguese" && langTitle.contains("brazil"))) {
                    extractedTitle = description
                    break
                }

                // Mantém a primeira versão encontrada como fallback caso a selecionada não exista
                if (fallbackTitle.isEmpty()) {
                    fallbackTitle = description
                }
            }

            // Se não encontrou a língua solicitada, usa o fallback ("mantenha o selecionado" / disponível)
            val finalTitle = if (extractedTitle.isNotEmpty()) extractedTitle else fallbackTitle

            currentVolume.capitulos.add(Capitulo(chapterNumber, finalTitle.replace(mReplace, "").trim(), ""))
        }

        // Ordena os capítulos dentro de cada volume e depois os próprios volumes.
        volumesMap.values.forEach { it.capitulos.sortByDescending { cap -> cap.capitulo } }
        return volumesMap.values.sortedByDescending { it.volume }
    }

    //<--------------------------  Zbato -------------------------->
    internal fun extractZBato(pagina: Document): List<Volume> {
        val volumesMap = mutableMapOf<Double, Volume>()
        val processedChapters = mutableSetOf<Double>()
        val chapterListContainer = pagina.selectFirst("div[name=chapter-list]")
        val chapterRows = chapterListContainer?.select("div.px-2.py-2") ?: emptyList()

        val chapterRegex = Regex("(?:Chapter|ch\\.|Chap)\\s*([\\d\\.]+)", RegexOption.IGNORE_CASE)
        val volRegex = Regex("(?:Volume|Vol\\.?)\\s*(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE)

        for (row in chapterRows) {
            val linkElement = row.selectFirst("a")
            if (linkElement != null) {
                val linkText = linkElement.text()

                val matchResult = chapterRegex.find(linkText)
                if (matchResult != null) {
                    val chapterNumberStr = matchResult.groupValues[1]
                    val chapterNumber = chapterNumberStr.toDoubleOrNull()

                    if (chapterNumber != null) {
                        if (processedChapters.contains(chapterNumber)) continue
                        processedChapters.add(chapterNumber)

                        var volNum = -1.0
                        volRegex.find(linkText)?.let { volNum = it.groupValues[1].toDoubleOrNull() ?: -1.0 }

                        val volume = volumesMap.getOrPut(volNum) { Volume(volume = volNum) }
                        val description = row.selectFirst("span.opacity-80")
                            ?.text()
                            ?.replaceFirst(":", "")
                            ?.trim() ?: ""

                        volume.capitulos.add(Capitulo(capitulo = chapterNumber, ingles = description, japones = ""))
                    }
                }
            }
        }

        volumesMap.values.forEach { it.capitulos.sortBy { it.capitulo } }
        return volumesMap.values.sortedBy { it.volume }
    }

    //<--------------------------  MangaPark -------------------------->
    internal fun extractMangaPark(pagina: Document): List<Volume> {
        val volumesMap = mutableMapOf<Double, Volume>()
        val volRegex = Regex("(?:Volume|Vol\\.?)\\s*(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE)
        val chapterRegex = """(?:Chapter|Ch\.)\s*([\d.]+)""".toRegex(RegexOption.IGNORE_CASE)

        val chapterItems = pagina.select("div.tab-content[data-name=chapter] li.item")
        if (chapterItems.isEmpty()) chapterItems.addAll(pagina.select("li.item[data-number]"))

        for (item in chapterItems) {
            val linkElement = item.selectFirst("a")
            if (linkElement != null) {
                val titleAttr = linkElement.attr("title")
                val match = chapterRegex.find(titleAttr)
                val chapNum = match?.groupValues?.get(1)?.toDoubleOrNull()

                if (chapNum != null) {
                    var volNum = -1.0
                    volRegex.find(titleAttr)?.let { volNum = it.groupValues[1].toDoubleOrNull() ?: -1.0 }

                    var description = ""
                    val span = linkElement.selectFirst("span")
                    if (span != null) {
                        val fullText = span.text().trim()
                        if (volNum == -1.0) volRegex.find(fullText)?.let { volNum = it.groupValues[1].toDoubleOrNull() ?: -1.0 }
                        if (fullText.contains(":")) description = fullText.substringAfter(":").trim()
                    }

                    if (description.isEmpty()) {
                        description = titleAttr.replace(match?.value ?: "", "").trim()
                        if (description.startsWith(":") || description.startsWith("-")) description = description.substring(1).trim()
                    }

                    val volume = volumesMap.getOrPut(volNum) { Volume(volume = volNum) }
                    volume.capitulos.add(Capitulo(capitulo = chapNum, ingles = description.replace(mReplace, "").trim(), japones = ""))
                }
            }
        }

        volumesMap.values.forEach { it.capitulos.sortBy { it.capitulo } }
        return volumesMap.values.sortedBy { it.volume }
    }

    private fun listeners() {
        txtEndereco.focusedProperty().addListener { _, oldVal, _ ->
            if (oldVal)
                consulta()
        }

        tbViewTabela.setOnMouseClicked { event ->
            if (event.clickCount == 2 && tbViewTabela.selectionModel.selectedItem != null) {
                popupDividir()
            }
        }
    }

    private fun editaColunas() {
        clMarcado.setCellValueFactory { param ->
            val item = param.value
            val booleanProp = SimpleBooleanProperty(item.marcado)
            booleanProp.addListener { _, _, newValue ->
                item.marcado = newValue
                atualizaCheckMarcarTodos()
                tbViewTabela.refresh()
            }
            return@setCellValueFactory booleanProp
        }
        clMarcado.setCellFactory {
            val cell = CheckBoxTableCellCustom<Volume, Boolean>()
            cell.alignment = Pos.CENTER
            cell
        }

        clTags.cellFactory = TextAreaTableCell.forTableColumn()
        clTags.setOnEditCommit { e: TableColumn.CellEditEvent<Volume, String> ->
            e.tableView.items[e.tablePosition.row].tags = e.newValue
        }
    }

    private fun linkaCelulas() {
        clVolume.cellValueFactory = PropertyValueFactory("volume")
        clTags.cellValueFactory = PropertyValueFactory("tags")

        val capitulo = DecimalFormat("000.##", DecimalFormatSymbols(Locale.US))
        clCapitulos.setCellValueFactory { param ->
            val item = param.value
            var descricao = ""
            if (item.capitulos.isNotEmpty())
                descricao = item.capitulos.joinToString(separator = "\n") { capitulo.format(it.capitulo) }
            SimpleStringProperty(descricao)
        }
        clDescricoes.setCellValueFactory { param ->
            val item = param.value
            var descricao = ""
            if (item.capitulos.isNotEmpty())
                descricao =
                    item.capitulos.joinToString(separator = "\n") { capitulo.format(it.capitulo) + ": " + if (cbLinguagem.value == Linguagem.JAPANESE) it.japones else it.ingles }

            SimpleStringProperty(descricao)
        }

        mLista.addListener(ListChangeListener {
            atualizaCheckMarcarTodos()
        })
        editaColunas()
    }

    @Synchronized
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        cbLinguagem.items.addAll(Linguagem.PORTUGUESE, Linguagem.ENGLISH, Linguagem.JAPANESE)
        cbLinguagem.selectionModel.selectFirst()
        cbLinguagem.valueProperty().addListener { _, _, _ -> preparar(mLista) }

        linkaCelulas()
        listeners()
        tbViewTabela.items = mLista

        for (button in arrayOf(hplComick, hplTaiyo, hplMangaDex, hplMangaFire, hplMangaPlanet, hplMangaForest, hplMangaRead, hplZBato, hplMangaPark))
            button.setOnAction { openSite(button.text) }

        val menu = javafx.scene.control.ContextMenu()
        val colar = javafx.scene.control.MenuItem("Colar (Ctrl+V)")
        colar.setOnAction {
            val clipboard = javafx.scene.input.Clipboard.getSystemClipboard()
            if (clipboard.hasString()) {
                extractManualText(clipboard.string)
            }
        }

        val dividir = javafx.scene.control.MenuItem("Dividir")
        dividir.setOnAction { popupDividir() }

        menu.items.addAll(colar, dividir)
        tbViewTabela.contextMenu = menu
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(PopupAmazonController::class.java)
        private val STYLE_SHEET: String = PopupCapitulosController::class.java.getResource("/css/Dark_TelaInicial.css").toExternalForm()
        private lateinit var btnConfirmar: JFXButton
        private lateinit var btnVoltar: JFXButton
        private lateinit var dialog: JFXDialog
        private lateinit var stackPane: StackPane

        @JvmStatic
        fun abreTelaCapitulos(rootStackPane: StackPane, nodeBlur: Node, callback: Callback<ObservableList<Volume>, Boolean>, linguagem: Linguagem, processar: List<Processar>) {
            try {
                stackPane = rootStackPane
                val blur = BoxBlur(3.0, 3.0, 3)
                val dialogLayout = JFXDialogLayout()
                dialog = JFXDialog(rootStackPane, dialogLayout, JFXDialog.DialogTransition.CENTER)
                val loader = FXMLLoader()
                loader.location = fxmlLocate
                val newAnchorPane: Parent = loader.load()
                val cnt: PopupCapitulosController = loader.getController()
                cnt.setLinguagem(linguagem)
                cnt.setArquivos(processar.map { it.arquivo })
                cnt.setProcessar(processar)
                val titulo = Label("Importando capitulos")
                titulo.font = Font.font(20.0)
                titulo.textFill = Color.WHITE
                val hbTitulo = HBox(titulo)
                hbTitulo.alignment = Pos.CENTER
                hbTitulo.maxWidth = Double.MAX_VALUE

                val botoes = mutableListOf<JFXButton>()
                btnConfirmar = JFXButton("Confirmar")
                btnConfirmar.setOnAction {
                    callback.call(cnt.mLista)
                    dialog.close()
                }
                btnConfirmar.styleClass.add("background-Green2")
                btnConfirmar.styleClass.add("texto-stilo-1")
                botoes.add(btnConfirmar)
                btnVoltar = JFXButton("Voltar")
                btnVoltar.setOnAction { dialog.close() }
                btnVoltar.styleClass.add("background-White1")
                botoes.add(btnVoltar)

                dialogLayout.setHeading(hbTitulo)
                dialogLayout.setBody(newAnchorPane)
                dialogLayout.setActions(botoes)
                dialogLayout.styleClass.add("dialog-black")
                dialog.stylesheets.add(STYLE_SHEET)
                dialog.padding = Insets(0.0, 0.0, 0.0, 0.0)
                dialog.setOnDialogClosed {
                    nodeBlur.effect = null
                    nodeBlur.isDisable = false
                }
                nodeBlur.effect = blur
                nodeBlur.isDisable = true
                dialog.show()
            } catch (e: IOException) {
                LOGGER.error(e.message, e)
            }
        }

        val fxmlLocate: URL get() = PopupAmazonController::class.java.getResource("/view/PopupCapitulos.fxml") as URL
    }

}