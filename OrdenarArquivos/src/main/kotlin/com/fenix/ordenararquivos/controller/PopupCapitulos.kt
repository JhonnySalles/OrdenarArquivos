package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.components.CheckBoxTableCellCustom
import com.fenix.ordenararquivos.components.TextAreaTableCell
import com.fenix.ordenararquivos.model.entities.Processar
import com.fenix.ordenararquivos.model.entities.capitulos.Capitulo
import com.fenix.ordenararquivos.model.entities.capitulos.Volume
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.notification.AlertasPopup
import com.fenix.ordenararquivos.util.Utils
import com.jfoenix.controls.*
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
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
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.stage.FileChooser
import javafx.util.Callback
import org.intellij.lang.annotations.Language
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


class PopupCapitulos : Initializable {

    @FXML
    private lateinit var hplMangaPlanet : Hyperlink

    @FXML
    private lateinit var hplComick : Hyperlink

    @FXML
    private lateinit var hplTaiyo : Hyperlink

    @FXML
    private lateinit var hplMangaFire : Hyperlink

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
        if (event.dragboard.hasFiles())
            event.acceptTransferModes(*TransferMode.COPY_OR_MOVE)
        event.consume()
    }

    @FXML
    fun handleDragEntered(event: DragEvent) {
        if (event.dragboard.hasFiles())
            txtEndereco.unFocusColor = Color.web("#0cff00")
        event.consume()
    }

    @FXML
    fun handleDragExited(event: DragEvent) {
        txtEndereco.unFocusColor = Color.web("#106ebe")
        event.consume()
    }

    @FXML
    fun handleDragDropped(event: DragEvent) {
        val db = event.dragboard
        var success = false
        if (db.hasFiles()) {
            // Pega o primeiro arquivo (ou null)
            db.files.firstOrNull()?.let { file ->
                // Atualiza o TextField com o caminho absoluto
                txtEndereco.text = file.absolutePath
                consulta()
                success = true
            } ?: run {
                AlertasPopup.alertaModal(AlertasPopup.rootStackPane, null, mutableListOf(),"Alerta", "Falha ao obter o arquivo.")
            }
        }
        // Sinaliza se o drop foi bem-sucedido
        event.isDropCompleted = success
        // Restaura o estilo (pois onDragExited pode não ser chamado)
        txtEndereco.unFocusColor = Color.web("#106ebe")
        event.consume()
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

    fun setArquivos(arquivos : List<String>) {
        mArquivos = arquivos
        clArquivo.cellFactory = ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(arquivos))
        clArquivo.cellValueFactory = PropertyValueFactory("arquivo")
    }

    fun setLinguagem(linguagem : Linguagem) = cbLinguagem.selectionModel.select(linguagem)

    fun setProcessar(processar : List<Processar>) {
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
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
                        .referrer("http://www.google.com")
                        .get()
                else {
                    val pagina = Jsoup.parse(File(txtEndereco.text))
                    for (node in pagina.childNodes())
                        if (node is Comment) {
                            val commentData = node.data.trim()
                            if (commentData.startsWith("saved from url=")) {
                                site = "http" + commentData.substringAfter("http").substringBefore("->")
                                break
                            }
                        }

                    pagina
                }
            } catch (e: IOException) {
                LOGGER.error(e.message, e)
                AlertasPopup.erroModal(AlertasPopup.rootStackPane, null, mutableListOf(),"Erro ao carregar o site", e.message.toString())
                return
            } catch (e: Exception) {
                LOGGER.error(e.message, e)
                AlertasPopup.erroModal(AlertasPopup.rootStackPane, null, mutableListOf(),"Erro ao carregar o site", e.message.toString())
                return
            }

            site.lowercase().let {
                val list = if (it.contains("mangaplanet.com"))
                    extractMangaPlanet(pagina)
                else if (it.contains("comick.io"))
                    extractComick(pagina)
                else if (it.contains("mangafire.to"))
                    extractMangaFire(pagina)
                else if (it.contains("taiyo.moe"))
                    extractTayo(pagina)
                else
                    mLista.toList()

                preparar(list)
            }
        } catch (e: Exception) {
            LOGGER.error(e.message, e)
            AlertasPopup.erroModal(AlertasPopup.rootStackPane, null, mutableListOf(),"Erro ao realizar o processamento do site", e.message.toString())
        }
    }

    private val formater = DecimalFormat("00.##", DecimalFormatSymbols(Locale.US))
    private fun formatar(valor : Double) : String = formater.format(valor)

    //Regex case insentive, no qual pode começar com capítulo, numero ou formato japoneas.
    private val replace = "(?i)^(ch|chapter|episode|第|[0-9])[0-9０-９ .]+(話|:)?".toRegex()

    private fun preparar(lista: List<Volume>) {
        val linguagem = cbLinguagem.value
        val processada = if (lista.size == 1 && lista[0].volume < 0) {
            val japones = Utils.JAPANESE_PATTERN.toRegex()
            val list = lista[0].capitulos
            val volumes = mutableListOf<Volume>()
            for (processar in mProcessar) {
                val capitulos = mutableListOf<Capitulo>()

                for (tag in processar.tags.split("\n")) {
                    var capitulo = tag.substringAfter(Utils.SEPARADOR_IMAGEM).trim()

                    if (capitulo.endsWith(Utils.SEPARADOR_IMPORTACAO))
                        capitulo = capitulo.substringBefore(Utils.SEPARADOR_IMPORTACAO).trim()

                    if (capitulo.isEmpty())
                        continue

                    capitulo.lowercase().let { c ->
                        if (c.contains("第") || c.contains("chapter") || c.contains("capítulo")) {
                            val numero = if (c.contains("第")) {
                                if (c.matches(japones))
                                    Utils.fromNumberJapanese(c.replace("第", "").replace("話", "").trim()).toDoubleOrNull()
                                else
                                    c.replace("第", "").replace("話", "").trim().toDoubleOrNull()
                            } else
                                c.replace("capítulo", "").replace("chapter", "").trim().toDoubleOrNull()
                            list.find { l -> l.capitulo == numero }?.run { capitulos.add(this) }
                        }
                    }
                }

                val tags = capitulos.joinToString(separator = "\n") { formatar(it.capitulo) + Utils.SEPARADOR_CAPITULO + if (linguagem == Linguagem.JAPANESE && it.japones.isNotEmpty()) it.japones else it.ingles }
                volumes.add(Volume(arquivo = processar.arquivo, volume = processar.comicInfo?.volume?.toDouble() ?: 0.0, capitulos = capitulos, tags = tags))
            }
            volumes
        } else {
            for (item in lista) {
                item.tags = item.capitulos.joinToString(separator = "\n") { formatar(it.capitulo) + Utils.SEPARADOR_CAPITULO + if (linguagem == Linguagem.JAPANESE && it.japones.isNotEmpty()) it.japones else it.ingles }
                item.arquivo = mArquivos.find { it.lowercase().contains("volume " + formatar(item.volume)) } ?: ""
            }
            lista
        }

        mLista = FXCollections.observableArrayList(processada)
        tbViewTabela.items = mLista
        tbViewTabela.refresh()
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
    private fun extractMangaPlanet(pagina: Document) : List<Volume> {
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
                            val inglesTitulo = listItem.selectFirst("h3")?.let { it.selectFirst("p")?.text() ?: it.text()  }?.trim() ?: ""
                            val japanesTitulo = listItem.selectFirst("p span.jp_fonts")?.text()?.trim() ?: ""

                            val ingles = cleanEnglishTitle(inglesTitulo)
                            val japones = cleanJapaneseTitle(japanesTitulo)

                            if (ingles.isNotBlank() || japones.isNotBlank())
                                currentVolume.capitulos.add(Capitulo(capitulo = chapterNumber, ingles = ingles.replace(replace, "").trim(), japones = japones.replace(replace, "").trim()))
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
        title = title.replaceFirst("""^(?:CHAPTER\s*[\d.]*\s*:\s*|BONUS CHAPTER\s*:\s*|Special one-shot\s*:\s*|Final Chapter\s*:\s*|CHAPTER\s*[\d.]*\s*-\s*|CHAPTER\s*[\d.]*\s+|CH\.[\d.]* )""".toRegex(RegexOption.IGNORE_CASE), "")
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
    private fun extractComick(pagina: Document) : List<Volume> {
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
                    // Neste HTML específico, o título está no span com classe "text-xs md:text-base".
                    val titleSpan = linkElement.selectFirst("span.text-xs.md:text-base")
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
            val volNum = finalEntry.vol
            // Apenas adiciona capítulos que puderam ser associados a um volume
            if (volNum != null) {
                val volume = volumesMap.getOrPut(volNum) { Volume(volume = volNum) }
                volume.capitulos.add(Capitulo(capitulo = finalEntry.chap, ingles = finalEntry.title.replace(replace, "").trim(), ""))
            }
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
    private fun extractMangaFire(pagina: Document) : List<Volume> {
        val japones = Utils.JAPANESE_PATTERN.toRegex()
        val volume = Volume(volume = -1.0, capitulos = mutableListOf())

        // Seleciona a lista de capítulos
        // A estrutura é ul.scroll-sm dentro de div.list-body que está dentro de div.tab-content[data-name="chapter"]
        val chapterListContainer = pagina.selectFirst("div.tab-content[data-name=chapter] div.list-body ul.scroll-sm")
        if (chapterListContainer != null) {
            val chapterItems = chapterListContainer.select("li.item")

            for (itemElement in chapterItems) {
                val chapNumString = itemElement.attr("data-number")
                val chapNum = chapNumString.toDoubleOrNull()

                if (chapNum != null) {
                    var englishTitle = ""
                    var japaneseTitle = ""
                    var title = ""
                    val linkElement = itemElement.selectFirst("a")
                    if (linkElement != null) {
                        // O título/descrição está no primeiro span dentro do link
                        val firstSpan = linkElement.selectFirst("span:first-child")
                        val fullText = firstSpan?.text()?.trim() ?: ""

                        // Extrair o título após "Chapter X: " ou "Chapter X "
                        // Regex para capturar o texto após "Chapter XXX: " ou "Chapter XXX "
                        val titleRegex = """^Chapter\s*[\d.]+(?::\s*|\s+)(.*)""".toRegex(RegexOption.IGNORE_CASE)
                        val matchResult = titleRegex.find(fullText)
                        title = matchResult?.groupValues?.get(1)?.trim() ?: ""

                        // Se não houver ":" e o regex não pegar, e o texto for apenas "Chapter XXX", o título é vazio
                        if (!fullText.matches("""^Chapter\s*[\d.]+$""".toRegex(RegexOption.IGNORE_CASE))) {
                            if (title.matches(japones))
                                japaneseTitle = title
                            else
                                englishTitle = title
                        }
                    }
                    volume.capitulos.add(Capitulo(capitulo = chapNum, ingles = englishTitle.replace(replace, "").trim(), japones = japaneseTitle))
                }
            }
        }

        // Ordenar os capítulos por número em ordem crescente
        volume.capitulos.sortBy { it.capitulo }
        return listOf(volume)
    }

    //<--------------------------  Tayo -------------------------->
    private fun extractTayo(pagina: Document) : List<Volume> {
        val volumesList = mutableListOf<Volume>()

        // Seleciona cada bloco de volume (que é um acordeão)
        // Cada volume está em um `div` que é irmão de um `hr` e contém um `h2` para o título do volume
        // e uma `section` para os capítulos.
        val volumeSections = pagina.select("div[data-open=true]:has(h2 button span.text-foreground)")

        for (volumeSectionDiv in volumeSections) {
            val volumeTitleElement = volumeSectionDiv.selectFirst("h2 button span.text-foreground")
            val volumeTitleText = volumeTitleElement?.text()?.trim() // Ex: "Volume 34"

            if (volumeTitleText != null) {
                val volumeNumberString = volumeTitleText.replace("Volume", "", ignoreCase = true).trim()
                val volumeNum = volumeNumberString.toDoubleOrNull()

                if (volumeNum != null) {
                    val currentVolume = Volume(volume = volumeNum)

                    // Capítulos estão dentro de uma 'section' > 'div.py-2' > 'div.flex.flex-col.gap-1'
                    val chapterContainers = volumeSectionDiv.select("section > div.py-2 > div.flex.flex-col.gap-1")

                    for (chapterContainer in chapterContainers) {
                        val chapterNumberElement = chapterContainer.selectFirst("h3.text-sm") // Ex: "Capítulo 139"
                        var chapterTitle = ""
                        var chapNumFromP: Double? = null

                        val linkElement = chapterContainer.selectFirst("a.grid")
                        if (linkElement != null) {
                            val pElement = linkElement.selectFirst("div > p.line-clamp-1")
                            val fullText = pElement?.text()?.trim() ?: "" // Ex: "Capítulo 139 — Em Direção a Árvore Naquela Colina"

                            val chapNumSpan = pElement?.selectFirst("span.font-medium")
                            val chapNumStrFromSpan = chapNumSpan?.text()?.trim()
                            chapNumFromP = chapNumStrFromSpan?.toDoubleOrNull()

                            // Extrai o título
                            if (chapNumStrFromSpan != null) {
                                var titleCandidate = fullText.replaceFirst("Capítulo\\s*$chapNumStrFromSpan\\s*—\\s*", "", ignoreCase = true)
                                if (titleCandidate == fullText) { // Se não houve "—"
                                    titleCandidate = fullText.replaceFirst("Capítulo\\s*$chapNumStrFromSpan\\s*", "", ignoreCase = true)
                                }
                                // Se após a remoção ainda começar com "Capítulo X" (caso não tenha título), define como vazio
                                if (titleCandidate.startsWith("Capítulo $chapNumStrFromSpan", ignoreCase = true) && titleCandidate.length == "Capítulo $chapNumStrFromSpan".length) {
                                    chapterTitle = ""
                                } else {
                                    chapterTitle = titleCandidate.trim()
                                }
                            } else if (fullText.startsWith("Capítulo", ignoreCase = true)) {
                                // Caso onde não há span.font-medium mas o texto começa com "Capítulo"
                                val titleRegex = """^Capítulo\s*[\d.]+(?:\s*—\s*(.*)|\s+(.*))?""".toRegex(RegexOption.IGNORE_CASE)
                                val match = titleRegex.find(fullText)
                                chapterTitle = (match?.groups?.get(1)?.value ?: match?.groups?.get(2)?.value ?: "").trim()
                            }
                        }

                        // Usa o número do capítulo do h3 se o do P não for encontrado, ou para consistência
                        val chapNumFromH3 = chapterNumberElement?.text()
                            ?.replace("Capítulo", "", ignoreCase = true)
                            ?.trim()?.toDoubleOrNull()

                        val finalChapNum = chapNumFromP ?: chapNumFromH3

                        if (finalChapNum != null)
                            currentVolume.capitulos.add(
                                Capitulo(
                                    capitulo = finalChapNum,
                                    ingles = chapterTitle.replace(replace, "").trim(), // Armazenando o título em pt-BR no campo 'ingles'
                                    japones = ""
                                )
                            )
                    }
                    if (currentVolume.capitulos.isNotEmpty()) {
                        // Ordena os capítulos dentro do volume antes de adicionar à lista de volumes
                        currentVolume.capitulos.sortBy { it.capitulo }
                        volumesList.add(currentVolume)
                    }
                }
            }
        }

        // Ordena a lista de volumes
        volumesList.sortBy { it.volume }
        return volumesList
    }

    private fun listeners() {
        txtEndereco.focusedProperty().addListener { _, oldVal, _ ->
            if (oldVal)
                consulta()
        }
    }

    private fun editaColunas() {
        clMarcado.setCellValueFactory { param ->
            val item = param.value
            val booleanProp = SimpleBooleanProperty(item.marcado)
            booleanProp.addListener { _, _, newValue ->
                item.marcado = newValue
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
                descricao = item.capitulos.joinToString(separator = "\n") { capitulo.format(it.capitulo) + ": " + if (cbLinguagem.value == Linguagem.JAPANESE) it.japones else it.ingles }

            SimpleStringProperty(descricao)
        }

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

        hplMangaPlanet.setOnAction { openSite(hplMangaPlanet.text)  }
        hplTaiyo.setOnAction { openSite(hplTaiyo.text)  }
        hplComick.setOnAction { openSite(hplComick.text)  }
        hplMangaFire.setOnAction { openSite(hplMangaFire.text)  }
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(PopupAmazon::class.java)
        private val STYLE_SHEET: String = PopupCapitulos::class.java.getResource("/css/Dark_TelaInicial.css").toExternalForm()
        private lateinit var btnConfirmar: JFXButton
        private lateinit var btnVoltar: JFXButton
        private lateinit var dialog: JFXDialog

        fun abreTelaCapitulos(rootStackPane: StackPane, nodeBlur: Node, callback: Callback<ObservableList<Volume>, Boolean>, linguagem: Linguagem, processar: List<Processar>) {
            try {
                val blur = BoxBlur(3.0, 3.0, 3)
                val dialogLayout = JFXDialogLayout()
                dialog = JFXDialog(rootStackPane, dialogLayout, JFXDialog.DialogTransition.CENTER)
                val loader = FXMLLoader()
                loader.location = fxmlLocate
                val newAnchorPane: Parent = loader.load()
                val cnt: PopupCapitulos = loader.getController()
                cnt.setLinguagem(linguagem)
                cnt.setArquivos(processar.map { it.arquivo })
                cnt.setProcessar(processar)
                val titulo = Label("Importando capitulos")
                titulo.font = Font.font(20.0)
                titulo.textFill = Color.web("#ffffff", 0.8)
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
                dialogLayout.setHeading(titulo)
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

        val fxmlLocate: URL get() = PopupAmazon::class.java.getResource("/view/PopupCapitulos.fxml") as URL
    }

}