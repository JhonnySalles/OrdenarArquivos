package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.components.TextAreaTableCell
import com.fenix.ordenararquivos.model.entities.Processar
import com.fenix.ordenararquivos.model.entities.sumario.VolumeSumario
import com.fenix.ordenararquivos.notification.AlertasModal
import com.fenix.ordenararquivos.util.Utils
import com.jfoenix.controls.*
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.SnapshotParameters
import javafx.scene.control.*
import javafx.scene.control.cell.ComboBoxTableCell
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.effect.BoxBlur
import javafx.scene.input.ClipboardContent
import javafx.scene.input.DataFormat
import javafx.scene.input.TransferMode
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.util.Callback
import javafx.util.StringConverter
import org.jsoup.Jsoup
import org.jsoup.nodes.Comment
import org.jsoup.nodes.Document
import java.io.File
import java.io.IOException
import java.net.URL
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class PopupSumarioController : Initializable {

    @FXML
    private lateinit var apRoot: AnchorPane

    @FXML
    private lateinit var txtEndereco: JFXTextField

    @FXML
    private lateinit var btnExecutar: JFXButton

    @FXML
    private lateinit var btnArquivo: JFXButton

    @FXML
    private lateinit var tbViewProcessar: TableView<Processar>

    @FXML
    private lateinit var clArquivoEsquerda: TableColumn<Processar, String>

    @FXML
    private lateinit var clSerieEsquerda: TableColumn<Processar, String>

    @FXML
    private lateinit var clVolumeEsquerda: TableColumn<Processar, String>

    @FXML
    private lateinit var clProcessar: TableColumn<Processar, String>

    @FXML
    private lateinit var tbViewSumario: TableView<VolumeSumario>

    @FXML
    private lateinit var clVolumeExtraido: TableColumn<VolumeSumario, Double>

    @FXML
    private lateinit var clVolumeVinculado: TableColumn<VolumeSumario, Double?>

    @FXML
    private lateinit var clSumario: TableColumn<VolumeSumario, String>

    @FXML
    private lateinit var apDragOverlay: AnchorPane

    @FXML
    private lateinit var lblDragDrop: Label

    @FXML
    private lateinit var spDragDropZone: StackPane

    private var mListaProcessar: ObservableList<Processar> = FXCollections.observableArrayList()
    private var mListaSumario: ObservableList<VolumeSumario> = FXCollections.observableArrayList()
    private var mCallback: Callback<List<Processar>, Void?>? = null
    var onClose: (() -> Unit)? = null

    private val mFormater = DecimalFormat("0.##", DecimalFormatSymbols(Locale.US))
    private fun formatar(valor: Double?): String = if (valor == null) "" else mFormater.format(valor)

    private val mDataFormatProcessarIdx = DataFormat("application/x-processar-idx")
    private val mDataFormatSumarioIdx = DataFormat("application/x-sumario-idx")

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        configurarTabelas()
        configurarDragAndDrop()
    }

    private fun configurarTabelas() {
        // Tabela Esquerda
        clArquivoEsquerda.cellValueFactory = PropertyValueFactory("arquivo")
        clSerieEsquerda.setCellValueFactory { param -> javafx.beans.property.SimpleStringProperty(param.value.comicInfo?.series ?: "") }
        
        clVolumeEsquerda.setCellValueFactory { param -> 
            javafx.beans.property.SimpleStringProperty(formatar(param.value.comicInfo?.volume?.toDouble())) 
        }
        
        clProcessar.cellFactory = TextAreaTableCell.forTableColumn()
        clProcessar.setCellValueFactory { param -> 
            javafx.beans.property.SimpleStringProperty(param.value.comicInfo?.summary ?: "") 
        }
        clProcessar.setOnEditCommit { e ->
            val item = e.tableView.items[e.tablePosition.row]
            item.comicInfo?.summary = e.newValue
        }

        // Tabela Direita
        clVolumeExtraido.setCellValueFactory { it.value.volumeExtraidoProperty.asObject() }
        clVolumeExtraido.setCellFactory { 
            object : TableCell<VolumeSumario, Double>() {
                override fun updateItem(item: Double?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty || item == null) null else formatar(item)
                }
            }
        }

        clVolumeVinculado.setCellValueFactory { it.value.volumeProperty }
        
        clSumario.cellFactory = TextAreaTableCell.forTableColumn()
        clSumario.setCellValueFactory { it.value.resumoProperty }
        clSumario.setOnEditCommit { it.rowValue.resumo = it.newValue }

        tbViewProcessar.items = mListaProcessar
        tbViewSumario.items = mListaSumario
        tbViewSumario.selectionModel.selectionMode = SelectionMode.MULTIPLE
        
        configurarDragAndDropGrids()

        // Menu de Contexto para Tabela Direita
        val menuImportarVolume = MenuItem("Importar volume da descrição")
        menuImportarVolume.setOnAction {
            val selecionados = tbViewSumario.selectionModel.selectedItems
            selecionados.forEach { processarVolumePeloTitulo(it) }
            tbViewSumario.refresh()
        }
        
        val menuImportarTodosVolumes = MenuItem("Importar todos os volumes de descrições")
        menuImportarTodosVolumes.setOnAction {
            mListaSumario.forEach { processarVolumePeloTitulo(it) }
            tbViewSumario.refresh()
        }
        
        tbViewSumario.contextMenu = ContextMenu(menuImportarVolume, menuImportarTodosVolumes)
    }

    private fun configurarDragAndDropGrids() {
        // Drag de Processar para Sumario
        tbViewProcessar.setRowFactory { tv ->
            val row = TableRow<Processar>()
            row.setOnDragDetected { event ->
                if (!row.isEmpty) {
                    val idx = row.index
                    val db = row.startDragAndDrop(TransferMode.MOVE)
                    val content = ClipboardContent()
                    content[mDataFormatProcessarIdx] = idx
                    db.setContent(content)
                    
                    val vol = row.item.comicInfo?.volume?.toDouble() ?: 0.0
                    db.dragView = criarDragImage(formatar(vol))
                    event.consume()
                }
            }
            row.setOnDragOver { event ->
                if (event.dragboard.hasContent(mDataFormatSumarioIdx)) {
                    event.acceptTransferModes(TransferMode.MOVE)
                    event.consume()
                }
            }
            row.setOnDragEntered { event ->
                if (event.dragboard.hasContent(mDataFormatSumarioIdx)) {
                    row.style = "-fx-background-color: #2196F3; -fx-background-radius: 5;"
                }
            }
            row.setOnDragExited { row.style = "" }
            row.setOnDragDropped { event ->
                val db = event.dragboard
                if (db.hasContent(mDataFormatSumarioIdx)) {
                    val sumarioIdx = db.getContent(mDataFormatSumarioIdx) as Int
                    val processarItem = row.item
                    val vol = processarItem.comicInfo?.volume?.toDouble() ?: 0.0
                    
                    if (sumarioIdx >= 0 && sumarioIdx < mListaSumario.size) {
                        mListaSumario[sumarioIdx].volume = vol
                        tbViewSumario.refresh()
                        event.isDropCompleted = true
                    }
                    event.consume()
                }
            }
            row
        }

        // Drag de Sumario para Processar
        tbViewSumario.setRowFactory { tv ->
            val row = TableRow<VolumeSumario>()
            row.setOnDragDetected { event ->
                if (!row.isEmpty) {
                    val idx = row.index
                    val db = row.startDragAndDrop(TransferMode.MOVE)
                    val content = ClipboardContent()
                    content[mDataFormatSumarioIdx] = idx
                    db.setContent(content)
                    
                    val vol = row.item.volumeExtraido
                    db.dragView = criarDragImage(formatar(vol))
                    event.consume()
                }
            }
            row.setOnDragOver { event ->
                if (event.dragboard.hasContent(mDataFormatProcessarIdx)) {
                    event.acceptTransferModes(TransferMode.MOVE)
                    event.consume()
                }
            }
            row.setOnDragEntered { event ->
                if (event.dragboard.hasContent(mDataFormatProcessarIdx)) {
                    row.style = "-fx-background-color: #2196F3; -fx-background-radius: 5;"
                }
            }
            row.setOnDragExited { row.style = "" }
            row.setOnDragDropped { event ->
                val db = event.dragboard
                if (db.hasContent(mDataFormatProcessarIdx)) {
                    val processarIdx = db.getContent(mDataFormatProcessarIdx) as Int
                    val sumarioItem = row.item
                    val vol = sumarioItem.volumeExtraido
                    
                    if (processarIdx >= 0 && processarIdx < mListaProcessar.size) {
                        mListaProcessar[processarIdx].comicInfo?.volume = vol.toInt()
                        tbViewProcessar.refresh()
                        event.isDropCompleted = true
                    }
                    event.consume()
                }
            }
            row
        }

        tbViewSumario.setOnDragOver { event ->
            if (event.dragboard.hasContent(mDataFormatProcessarIdx)) {
                event.acceptTransferModes(TransferMode.MOVE)
                event.consume()
            }
        }
        tbViewSumario.setOnDragDropped { event ->
            if (event.dragboard.hasContent(mDataFormatProcessarIdx)) {
                event.consume()
            }
        }
        
        // Handlers para a TableView (áreas sem linhas)
        tbViewProcessar.setOnDragOver { event ->
            if (event.dragboard.hasContent(mDataFormatSumarioIdx)) {
                event.acceptTransferModes(TransferMode.MOVE)
                event.consume()
            }
        }
        tbViewProcessar.setOnDragDropped { event ->
            if (event.dragboard.hasContent(mDataFormatSumarioIdx)) {
                event.consume()
            }
        }
    }

    private fun criarDragImage(texto: String): javafx.scene.image.Image {
        val label = Label(texto)
        label.font = Font.font("System Bold", 28.0)
        label.textFill = Color.WHITE
        label.style = "-fx-background-color: #2196F3; -fx-padding: 10 20; -fx-background-radius: 10;"
        
        // Criar uma scene temporária para o label renderizar os estilos
        val pane = StackPane(label)
        Scene(pane)
        
        val params = SnapshotParameters()
        params.fill = Color.TRANSPARENT
        return label.snapshot(params, null)
    }

    private fun processarVolumePeloTitulo(volumeSumario: VolumeSumario) {
        val titulo = volumeSumario.tituloOriginal
        if (titulo.isEmpty())
            return
        
        // Padrões: "Vol. 1", "Vol.1", "（1）", "(1)"
        val regexPtBr = Regex("Vol\\.?\\s*(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE)
        val regexJp = Regex("[（(]\\s*(\\d+(?:\\.\\d+)?)\\s*[）)]")
        
        val matchPtBr = regexPtBr.find(titulo)
        val matchJp = regexJp.find(titulo)
        
        val volumeStr = matchPtBr?.groupValues?.get(1) ?: matchJp?.groupValues?.get(1)
        volumeStr?.toDoubleOrNull()?.let {
            volumeSumario.volume = it
        }
    }

    private fun configurarDragAndDrop() {
        apRoot.setOnDragOver { event ->
            if (event.dragboard.hasFiles() || event.dragboard.hasString()) {
                event.acceptTransferModes(TransferMode.COPY)
                mostrarOverlayDrag(true)
            }
            event.consume()
        }

        apRoot.setOnDragExited { esconderOverlayDrag() }

        apRoot.setOnDragDropped { event ->
            val db = event.dragboard
            if (db.hasFiles()) {
                db.files.firstOrNull { it.extension.lowercase() in listOf("txt", "html", "htm") }?.let { file ->
                    if (file.extension.lowercase() == "txt")
                        extractManualText(file.readText(Charsets.UTF_8))
                    else {
                        txtEndereco.text = file.absolutePath
                        onBtnExecutar()
                    }
                }
            } else if (db.hasString()) {
                val texto = db.string
                if (texto.startsWith("http") || (texto.contains(":\\") || texto.startsWith("/"))) {
                    txtEndereco.text = texto
                    onBtnExecutar()
                } else {
                    extractManualText(texto)
                }
            }
            esconderOverlayDrag()
            event.isDropCompleted = true
            event.consume()
        }
    }

    private fun mostrarOverlayDrag(aceito: Boolean) {
        spDragDropZone.style = if (aceito)
            "-fx-border-color: white; -fx-border-style: dashed; -fx-border-width: 3; -fx-border-radius: 10; -fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 10;"
        else
            "-fx-border-color: red; -fx-border-style: dashed; -fx-border-width: 3; -fx-border-radius: 10; -fx-background-color: rgba(255,0,0,0.1); -fx-background-radius: 10;"

        lblDragDrop.text = if (aceito) "Arraste o arquivo ou cole o texto aqui" else "Formato não aceito"
        lblDragDrop.textFill = if (aceito) Color.WHITE else Color.RED
        apDragOverlay.isVisible = true
    }

    private fun esconderOverlayDrag() {
        apDragOverlay.isVisible = false
    }

    fun setDados(itens: List<Processar>, callback: Callback<List<Processar>, Void?>) {
        mListaProcessar.setAll(itens)
        mCallback = callback

        // Configura volumes na coluna da direita (vinculado)
        val volumes = itens.mapNotNull { it.comicInfo?.volume?.toDouble() }.distinct().sorted()
        clVolumeVinculado.cellFactory = ComboBoxTableCell.forTableColumn(object : StringConverter<Double?>() {
            override fun toString(value: Double?): String = formatar(value)
            override fun fromString(string: String?): Double? = string?.toDoubleOrNull()
        }, FXCollections.observableArrayList(volumes))
        
        clVolumeVinculado.setOnEditCommit { it.rowValue.volume = it.newValue }
    }

    @FXML
    private fun onBtnExecutar() {
        mListaSumario.clear()
        val endereco = txtEndereco.text?.trim() ?: ""
        if (endereco.isEmpty()) return

        try {
            var site = endereco
            val doc: Document = if (endereco.startsWith("http")) {
                Jsoup.connect(endereco)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .get()
            } else {
                val file = File(endereco)
                val pagina = Jsoup.parse(file, "UTF-8")
                for (node in pagina.childNodes()) {
                    if (node is Comment) {
                        val commentData = node.data.trim()
                        if (commentData.startsWith("saved from url=")) {
                            site = "http" + commentData.substringAfter("http").substringBefore("->")
                            break
                        }
                    }
                }
                
                if (!site.startsWith("http")) {
                    val fileName = file.name.lowercase()
                    if (fileName.contains("amazon")) site = "https://amazon.com"
                }
                pagina
            }

            if (site.contains("amazon", ignoreCase = true)) {
                extractAmazon(doc)
            } else {
                AlertasModal.aviso("Aviso", "Scraping para este site ($site) ainda não implementado.")
            }
        } catch (e: Exception) {
            AlertasModal.erro("Erro", "Erro ao carregar endereço: ${e.message}")
        }
    }

    @FXML
    private fun onBtnArquivo() {
        val fileChooser = FileChooser()
        fileChooser.title = "Selecionar Arquivo de Página"
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("HTML Files", "*.html", "*.htm"))
        val file = fileChooser.showOpenDialog(apRoot.scene.window)
        if (file != null) {
            txtEndereco.text = file.absolutePath
            onBtnExecutar()
        }
    }

    fun onBtnConfirmar() {
        val itensAtualizados = mutableListOf<Processar>()
        
        for (item in mListaProcessar) {
            val vol = item.comicInfo?.volume?.toDouble() ?: continue
            val extraidos = mListaSumario.filter { it.volume == vol }
            if (extraidos.isEmpty()) continue
            
            // Une os resumos caso haja mais de um item para o mesmo volume
            val resumoUnificado = extraidos.joinToString("\n\n") { it.resumo }
            
            val currentSummary = item.comicInfo?.summary ?: ""
            val separators = listOf("*Chapter Titles*", "*Chapter Titles Manual*")
            var separatorFound = ""
            var chapterIndex = -1
            
            for (sep in separators) {
                val idx = currentSummary.lowercase().indexOf(sep.lowercase())
                if (idx != -1) {
                    chapterIndex = idx
                    separatorFound = sep
                    break
                }
            }

            val chaptersPart = if (chapterIndex != -1) {
                currentSummary.substring(chapterIndex)
            } else {
                ""
            }

            val newSummary = if (chaptersPart.isNotEmpty()) {
                "${resumoUnificado}\n\n$chaptersPart"
            } else {
                resumoUnificado
            }

            if (item.comicInfo?.summary != newSummary) {
                item.comicInfo?.summary = newSummary
                itensAtualizados.add(item)
            }
        }

        mCallback?.call(itensAtualizados)
        fechar()
    }

    fun onBtnCancelar() {
        fechar()
    }

    private fun fechar() {
        onClose?.invoke() ?: run {
            val stage = apRoot.scene.window as Stage
            stage.close()
        }
    }

    private fun extractManualText(texto: String) {
        mListaSumario.clear()
        val resumos = mutableListOf<VolumeSumario>()
        val volRegex = Regex("(?:Volume|Vol\\.?)\\s*(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE)
        
        var currentVolume: Double? = null
        val currentText = StringBuilder()

        texto.lines().forEach { linha ->
            val l = linha.trim()
            if (l.isEmpty()) {
                if (currentText.isNotEmpty()) currentText.append("\n")
                return@forEach
            }

            val match = volRegex.find(l)
            if (match != null) {
                // Se já tínhamos um volume sendo processado, salva ele
                if (currentVolume != null) {
                    resumos.add(VolumeSumario(currentVolume!!, currentText.toString().trim()))
                    currentText.clear()
                }
                
                currentVolume = match.groupValues[1].toDoubleOrNull()
                // Pega o resto da linha após o volume como início do resumo
                val resto = l.substringAfter(match.value).trim().removePrefix(":").trim()
                if (resto.isNotEmpty()) {
                    currentText.append(resto).append("\n")
                }
            } else {
                if (currentVolume != null) {
                    currentText.append(l).append("\n")
                }
            }
        }

        // Adiciona o último volume
        if (currentVolume != null) {
            resumos.add(VolumeSumario(currentVolume!!, currentText.toString().trim()))
        }

        if (resumos.isNotEmpty()) {
            mListaSumario.setAll(resumos)
            tentarAutoVincular()
        }
    }

    private fun tentarAutoVincular() {
        val volumesOriginais = mListaProcessar.mapNotNull { it.comicInfo?.volume?.toDouble() }.toSet()
        mListaSumario.forEach { sumario ->
            if (volumesOriginais.contains(sumario.volumeExtraido)) {
                sumario.volume = sumario.volumeExtraido
            }
        }
        tbViewSumario.refresh()
    }

    private fun extractAmazon(doc: Document) {
        mListaSumario.clear()
        
        val items = doc.select(".series-childAsin-item")
        val resumos = mutableListOf<VolumeSumario>()
        
        for (item in items) {
            // Volume
            val countText = item.selectFirst(".series-childAsin-count .itemPositionLabel")?.text()?.trim() ?: ""
            val volume = countText.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
            
            // Título (para uso interno)
            val titulo = item.selectFirst(".itemBookTitle h3")?.text()?.trim() ?: ""
            
            // Sumário (preservando quebras de linha)
            val descElement = item.selectFirst(".collectionDescription")
            var resumo = ""
            if (descElement != null) {
                // Converte <br> e <p> em \n antes de pegar o texto
                val html = descElement.html()
                    .replace("(?i)<br\\s*/?>".toRegex(), "\n")
                    .replace("(?i)</p>".toRegex(), "\n")
                    .replace("(?i)<p[^>]*>".toRegex(), "")
                
                // Usa um parser que não colapsa espaços se possível, ou limpa manualmente
                resumo = Jsoup.parse(html).wholeText().trim().lines().joinToString("\n") { it.trim() }
            }
            
            if (resumo.isNotEmpty() || titulo.isNotEmpty()) {
                resumos.add(VolumeSumario(volume, resumo, titulo))
            }
        }
        
        if (resumos.isNotEmpty()) {
            mListaSumario.setAll(resumos)
            tentarAutoVincular()
        } else {
            AlertasModal.aviso("Aviso", "Nenhum resumo encontrado no formato esperado da Amazon.")
        }
    }

    companion object {
        @JvmStatic
        fun abreTelaSumario(stackPane: StackPane, nodeBlur: Node, itens: List<Processar>, callback: Callback<List<Processar>, Void?>) {
            if (itens.isEmpty()) return

            try {
                val loader = FXMLLoader(PopupSumarioController::class.java.getResource("/view/PopupSumario.fxml"))
                val root = loader.load<AnchorPane>()

                val dialogLayout = JFXDialogLayout()
                dialogLayout.setBody(root)
                val dialog = JFXDialog(stackPane, dialogLayout, JFXDialog.DialogTransition.CENTER)
                val controller = loader.getController<PopupSumarioController>()
                controller.onClose = { dialog.close() }

                val blur = BoxBlur(3.0, 3.0, 3)
                dialog.isOverlayClose = true

                val titulo = Label("Importar Sumário")
                titulo.font = Font.font(20.0)
                titulo.textFill = Color.WHITE
                val hbTitulo = HBox(titulo)
                hbTitulo.alignment = Pos.CENTER
                hbTitulo.maxWidth = Double.MAX_VALUE
                dialogLayout.setHeading(hbTitulo)

                controller.setDados(itens, Callback { param ->
                    callback.call(param)
                    dialog.close()
                    null
                })

                val btnVoltar = JFXButton("Voltar")
                btnVoltar.setOnAction { controller.onBtnCancelar() }
                btnVoltar.styleClass.add("background-White1")

                val btnConfirmar = JFXButton("Confirmar")
                btnConfirmar.setOnAction { controller.onBtnConfirmar() }
                btnConfirmar.styleClass.addAll("background-Green2", "texto-stilo-1")

                dialogLayout.setActions(listOf(btnVoltar, btnConfirmar))

                dialog.setOnDialogClosed {
                    nodeBlur.effect = null
                    nodeBlur.isDisable = false
                }

                nodeBlur.effect = blur
                nodeBlur.isDisable = true
                dialogLayout.styleClass.add("dialog-black")
                dialog.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
