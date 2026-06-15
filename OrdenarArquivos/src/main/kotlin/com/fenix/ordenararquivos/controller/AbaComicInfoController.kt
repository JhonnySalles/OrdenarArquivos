package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.components.CheckBoxTableCellCustom
import com.fenix.ordenararquivos.components.TextAreaTableCell
import com.fenix.ordenararquivos.model.entities.Processar
import com.fenix.ordenararquivos.model.entities.capitulos.Volume
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.entities.comicinfo.Manga
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.model.enums.Notificacao
import com.fenix.ordenararquivos.notification.AlertasModal
import com.fenix.ordenararquivos.notification.ConfirmaModal
import com.fenix.ordenararquivos.notification.Notificacoes
import com.fenix.ordenararquivos.service.OcrServices
import com.fenix.ordenararquivos.service.WinrarServices
import com.fenix.ordenararquivos.util.Utils
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXCheckBox
import com.jfoenix.controls.JFXComboBox
import com.jfoenix.controls.JFXTextField
import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.concurrent.Task
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.css.PseudoClass
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.TransferMode
import javafx.scene.layout.AnchorPane
import javafx.scene.paint.Color
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.util.Callback
import javafx.scene.effect.BoxBlur
import javafx.scene.text.Font
import com.fenix.ordenararquivos.util.GridHistoryManager
import com.fenix.ordenararquivos.util.PropertyChangeAction
import com.fenix.ordenararquivos.util.CompositeAction
import com.fenix.ordenararquivos.util.ReversibleAction
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.fxml.FXMLLoader
import com.jfoenix.controls.JFXDialogLayout
import com.jfoenix.controls.JFXDialog
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class AbaComicInfoController : Initializable {

    private val mLOG = LoggerFactory.getLogger(AbaComicInfoController::class.java)
    private val ALERTA_PSEUDO_CLASS = PseudoClass.getPseudoClass("alerta")
    private val activeTasksCount = AtomicInteger(0)
    private val saveExecutor = java.util.concurrent.Executors.newFixedThreadPool(3) { r ->
        Thread(r).apply { isDaemon = true }
    }

    //<--------------------------  PRINCIPAL   -------------------------->

    @FXML
    private lateinit var apRoot: AnchorPane

    @FXML
    private lateinit var cbLinguagem: JFXComboBox<Linguagem>

    @FXML
    private lateinit var txtPastaProcessar: JFXTextField

    @FXML
    private lateinit var btnPesquisarPastaProcessar: JFXButton

    @FXML
    private lateinit var btnCarregar: JFXButton

    @FXML
    private lateinit var btnOcrProcessar: JFXButton

    @FXML
    private lateinit var btnTagsProcessar: JFXButton

    @FXML
    private lateinit var btnTagsNormaliza: JFXButton

    @FXML
    private lateinit var btnTagsAplicar: JFXButton

    @FXML
    private lateinit var btnSalvarTodos: JFXButton

    @FXML
    private lateinit var btnSubstituirTags: JFXButton

    @FXML
    private lateinit var btnCapitulos: JFXButton

    @FXML
    private lateinit var btnSumario: JFXButton

    @FXML
    private lateinit var btnProcessar: JFXButton

    @FXML
    private lateinit var ckbTodosProcessado: JFXCheckBox

    @FXML
    private lateinit var tbViewProcessar: TableView<Processar>

    @FXML
    private lateinit var clProcessado: TableColumn<Processar, Boolean>

    @FXML
    private lateinit var clCapa: TableColumn<Processar, String>

    @FXML
    private lateinit var clArquivo: TableColumn<Processar, String>

    @FXML
    private lateinit var clSerie: TableColumn<Processar, String>

    @FXML
    private lateinit var clTitulo: TableColumn<Processar, String>

    @FXML
    private lateinit var clEditora: TableColumn<Processar, String>

    @FXML
    private lateinit var clPublicacao: TableColumn<Processar, String>

    @FXML
    private lateinit var clResumo: TableColumn<Processar, String>

    @FXML
    private lateinit var clTags: TableColumn<Processar, String>

    @FXML
    private lateinit var clAcoes: TableColumn<Processar, Void>

    internal var mRarService = WinrarServices()
    internal var mOcrService = OcrServices()

    private lateinit var controller: TelaInicialController
    var controllerPai: TelaInicialController
        get() = controller
        set(controller) {
            this.controller = controller
        }

    private var mObsListaProcessar: ObservableList<Processar> = FXCollections.observableArrayList()
    private val mDecimal = DecimalFormat("000.##", DecimalFormatSymbols(Locale.US))
    private val mPASTA_TEMPORARIA = File(System.getProperty("user.dir"), "temp/")
    private val mPASTA_COVERS = File(mPASTA_TEMPORARIA, "covers/")
    private val mHistory = GridHistoryManager()

    @FXML
    private fun onBtnCapitulos() {
        abrirPopupCapitulos()
    }

    private fun abrirPopupCapitulos(textoInicial: String? = null) {
        val selected = tbViewProcessar.selectionModel.selectedItems
        if (selected.isEmpty()) {
            Notificacoes.notificacao(Notificacao.ALERTA, "Importar capítulos", "Selecione pelo menos um registro na tabela para abrir os capítulos.")
            return
        }
        val listToProcess = selected.toList()

        val callback: Callback<ObservableList<Volume>, Boolean> = Callback<ObservableList<Volume>, Boolean> { param ->
            val linguagem = cbLinguagem.value ?: Linguagem.PORTUGUESE
            val novasTags = PopupCapitulosController.aplicarVolumesConfirmados(
                param.toList(),
                listToProcess,
                linguagem,
                mDecimal
            )
            val actions = mutableListOf<ReversibleAction>()
            for ((arquivo, newTags) in novasTags) {
                val item = mObsListaProcessar.find { it.arquivo == arquivo } ?: continue
                val oldTags = item.tags
                if (oldTags != newTags) {
                    actions.add(PropertyChangeAction(item, oldTags, newTags) { i, v -> i.tags = v })
                    item.tags = newTags
                }
            }
            if (actions.isNotEmpty()) mHistory.pushAction(CompositeAction(actions))
            tbViewProcessar.refresh()
            null
        }
        PopupCapitulosController.abreTelaCapitulos(controllerPai.rootStack, controllerPai.rootTab, callback, cbLinguagem.value, listToProcess, textoInicial)
    }

    @FXML
    private fun onBtnSumario() {
        val selected = tbViewProcessar.selectionModel.selectedItems
        if (selected.isEmpty()) {
            AlertasModal.alerta("Alerta", "Nenhum item selecionado.")
            return
        }

        val callback = Callback<List<Processar>, Void?> { param ->
            for (atualizado in param) {
                val item = mObsListaProcessar.find { it.arquivo == atualizado.arquivo } ?: continue
                item.comicInfo?.summary = atualizado.comicInfo?.summary
            }
            tbViewProcessar.refresh()
            null
        }

        PopupSumarioController.abreTelaSumario(controllerPai.rootStack, controllerPai.rootTab,selected.toList(), callback)
    }

    @FXML
    private fun onBtnCarregar() {
        carregarItens()
    }

    @FXML
    private fun onBtnOcrProcessar() {
        if (mObsListaProcessar.isNotEmpty()) {
            if (btnOcrProcessar.accessibleTextProperty().value.equals("PROCESSA", ignoreCase = true)) {
                btnOcrProcessar.accessibleTextProperty().set("CANCELA")
                btnOcrProcessar.text = "Cancelar"
                controllerPai.setCursor(Cursor.WAIT)
                desabilita(btnOcrProcessar)
                processaOCR()
            } else
                mCANCELAR = true
        }
    }

    @FXML
    private fun onBtnProcessar() {
        if (btnProcessar.accessibleTextProperty().value.equals("PROCESSA", ignoreCase = true)) {
            if (txtPastaProcessar.text.isEmpty()) {
                AlertasModal.alerta("Alerta", "Necessário informar uma pasta para processar.")
                txtPastaProcessar.requestFocus()
                return
            }

            btnProcessar.accessibleTextProperty().set("CANCELA")
            btnProcessar.text = "Cancelar"
            controllerPai.setCursor(Cursor.WAIT)
            mObsListaProcessar.clear()
            desabilita(btnProcessar)

            val marcaCapitulo = when (cbLinguagem.value) {
                Linguagem.PORTUGUESE -> "Capítulo"
                Linguagem.ENGLISH -> "Chapter"
                Linguagem.JAPANESE -> "第%s話"
                else -> ""
            }

            val processa: Task<Boolean> = object : Task<Boolean>() {
                override fun call(): Boolean {
                    updateMessage("Processando ComicInfo...")
                    com.fenix.ordenararquivos.process.ComicInfo.processa(cbLinguagem.value, txtPastaProcessar.text, marcaCapitulo) { param ->
                        updateProgress(param[0], param[1])
                        updateMessage("Processando item ${param[0]} de ${param[1]}")
                        null
                    }
                    return true
                }

                override fun succeeded() {
                    updateMessage("Processamento finalizado com sucesso.")
                    controllerPai.rootProgress.progressProperty().unbind()
                    controllerPai.rootMessage.textProperty().unbind()
                    controllerPai.clearProgress()
                    habilita()
                    carregarItens()
                    Notificacoes.notificacao(Notificacao.SUCESSO, "Processamento ComicInfo", "ComicInfo processado com sucesso.")
                }

                override fun failed() {
                    super.failed()
                    mLOG.error("Erro na Task de processamento de ComicInfo", exception)
                    updateMessage("Erro ao processar o ComicInfo.")
                    AlertasModal.erro("Erro ao processar o ComicInfo", exception?.message ?: "Erro desconhecido")
                    habilita()
                }
            }

            controllerPai.rootProgress.progressProperty().unbind()
            controllerPai.rootMessage.textProperty().unbind()
            controllerPai.rootProgress.progressProperty().bind(processa.progressProperty())
            controllerPai.rootMessage.textProperty().bind(processa.messageProperty())
            val t = Thread(processa)
            t.isDaemon = true
            t.start()

        } else {
            com.fenix.ordenararquivos.process.ComicInfo.cancelar()
        }
    }

    @FXML
    private fun onBtnTagsProcessar() {
        if (mObsListaProcessar.isNotEmpty()) {
            controllerPai.setCursor(Cursor.WAIT)
            val language = cbLinguagem.value ?: Linguagem.PORTUGUESE
            val actions = mutableListOf<ReversibleAction>()
            for (item in mObsListaProcessar) {
                val oldTags = item.tags
                gerarTagItem(item, language)
                if (oldTags != item.tags) {
                    actions.add(PropertyChangeAction(item, oldTags, item.tags) { i, v -> i.tags = v })
                }
            }
            if (actions.isNotEmpty()) mHistory.pushAction(CompositeAction(actions))
            tbViewProcessar.refresh()
            controllerPai.setCursor(null)
        }
    }

    @FXML
    private fun onBtnTagsNormaliza() {
        if (mObsListaProcessar.isNotEmpty()) {
            controllerPai.setCursor(Cursor.WAIT)
            val language = cbLinguagem.value ?: Linguagem.PORTUGUESE
            val actions = mutableListOf<ReversibleAction>()
            for (item in mObsListaProcessar) {
                val oldTags = item.tags
                normalizarTagItem(item, language)
                if (oldTags != item.tags) {
                    actions.add(PropertyChangeAction(item, oldTags, item.tags) { i, v -> i.tags = v })
                }
            }
            if (actions.isNotEmpty()) mHistory.pushAction(CompositeAction(actions))
            tbViewProcessar.refresh()
            controllerPai.setCursor(null)
        }
    }

    @FXML
    private fun onBtnTagsAplicar() {
        if (mObsListaProcessar.isNotEmpty()) {
            controllerPai.setCursor(Cursor.WAIT)
            val actions = mutableListOf<ReversibleAction>()
            for (item in mObsListaProcessar) {
                if (item.tags.isEmpty() || !item.tags.contains("\n") || !item.tags.contains(Utils.SEPARADOR_IMPORTACAO))
                    continue

                val oldTags = item.tags
                val linhas = mutableListOf<String>()
                val separador = Utils.SEPARADOR_CAPITULO
                for (linha in item.tags.split("\n"))
                    linhas.add(
                        if (linha.contains(Utils.SEPARADOR_IMPORTACAO)) linha.substringBefore(Utils.SEPARADOR_IMPORTACAO)
                            .trim() + " - " + linha.substringAfterLast(separador) else linha
                    )

                val newTags = linhas.joinToString(separator = "\n")
                if (oldTags != newTags) {
                    actions.add(PropertyChangeAction(item, oldTags, newTags) { i, v -> i.tags = v })
                    item.tags = newTags
                }
            }
            if (actions.isNotEmpty()) mHistory.pushAction(CompositeAction(actions))
            tbViewProcessar.refresh()
            controllerPai.setCursor(null)
        }
    }

    @FXML
    private fun onBtnSubstituirTags() {
        val selecionados = tbViewProcessar.selectionModel.selectedItems.toList()
        val listToProcess = if (selecionados.size > 1) selecionados else mObsListaProcessar.toList()

        if (listToProcess.isEmpty()) {
            Notificacoes.notificacao(Notificacao.ALERTA, "Substituir Tags", "Nenhum item carregado para processar.")
            return
        }

        val exemplos = listToProcess
            .flatMap { it.tags.split("\n") }
            .filter { it.isNotEmpty() }
            .shuffled()
            .take(5)

        PopupSubstituirController.abreTelaSubstituir(controllerPai.rootStack, apRoot, exemplos) { localizar, substituir, isRegex ->
            try {
                val actions = mutableListOf<ReversibleAction>()

                if (isRegex) {
                    val regex = Regex(localizar)
                    for (item in listToProcess) {
                        val original = item.tags
                        val novo = original.replace(regex, substituir)
                        if (original != novo) {
                            actions.add(PropertyChangeAction(item, original, novo) { i, v -> i.tags = v })
                            item.tags = novo
                        }
                    }
                } else {
                    for (item in listToProcess) {
                        val original = item.tags
                        val novo = original.replace(localizar, substituir)
                        if (original != novo) {
                            actions.add(PropertyChangeAction(item, original, novo) { i, v -> i.tags = v })
                            item.tags = novo
                        }
                    }
                }

                if (actions.isNotEmpty()) {
                    mHistory.pushAction(CompositeAction(actions))
                }
                tbViewProcessar.refresh()
                Notificacoes.notificacao(Notificacao.SUCESSO, "Substituir Tags", "Substituição concluída em ${actions.size} item(ns).")
            } catch (e: Exception) {
                AlertasModal.erro("Erro Regex", "Expressão regular inválida: ${e.message}")
            }
        }
    }

    @FXML
    private fun onBtnSalvarTodos() {
        if (mObsListaProcessar.isNotEmpty())
            salvarItens(startIndex = 0, endIndex = mObsListaProcessar.size)
    }

    @FXML
    private fun onBtnCarregarPasta() {
        val caminho = Utils.selecionaPasta(txtPastaProcessar.text)
        if (caminho != null)
            txtPastaProcessar.text = caminho.absolutePath
        else
            txtPastaProcessar.text = ""
        carregarItens()
    }

    @FXML
    private fun onBtnTodosProcessado() {
        mObsListaProcessar.forEach { it.isProcessado = ckbTodosProcessado.isSelected }
        tbViewProcessar.refresh()
    }

    private fun atualizaCheckTodosProcessado() {
        ckbTodosProcessado.isSelected = mObsListaProcessar.isNotEmpty() && mObsListaProcessar.all { it.isProcessado }
    }

    private fun desabilita(botaoAtivo: JFXButton? = null) {
        txtPastaProcessar.isDisable = true
        btnPesquisarPastaProcessar.isDisable = true
        cbLinguagem.isDisable = true
        btnCarregar.isDisable = true
        btnTagsProcessar.isDisable = true
        btnTagsNormaliza.isDisable = true
        btnTagsAplicar.isDisable = true
        btnSalvarTodos.isDisable = true
        btnCapitulos.isDisable = true
        tbViewProcessar.isDisable = true

        btnOcrProcessar.isDisable = botaoAtivo != btnOcrProcessar
        btnProcessar.isDisable = botaoAtivo != btnProcessar
    }

    private fun habilita() {
        txtPastaProcessar.isDisable = false
        btnPesquisarPastaProcessar.isDisable = false
        cbLinguagem.isDisable = false
        btnCarregar.isDisable = false
        btnTagsProcessar.isDisable = false
        btnTagsNormaliza.isDisable = false
        btnTagsAplicar.isDisable = false
        btnSalvarTodos.isDisable = false
        btnCapitulos.isDisable = false
        tbViewProcessar.isDisable = false
        btnOcrProcessar.isDisable = false
        btnProcessar.isDisable = false

        btnOcrProcessar.accessibleTextProperty().set("PROCESSA")
        btnOcrProcessar.text = "OCR proximos 10"

        btnProcessar.accessibleTextProperty().set("PROCESSA")
        btnProcessar.text = "Processar ComicInfo"
        if (::controller.isInitialized) controllerPai.setCursor(null)
    }

    private fun bloquearControlesGerais(block: Boolean) {
        txtPastaProcessar.isDisable = block
        btnPesquisarPastaProcessar.isDisable = block
        cbLinguagem.isDisable = block
        btnCarregar.isDisable = block
        btnTagsProcessar.isDisable = block
        btnTagsNormaliza.isDisable = block
        btnTagsAplicar.isDisable = block
        btnSubstituirTags.isDisable = block
        btnSalvarTodos.isDisable = block
        btnCapitulos.isDisable = block
        btnSumario.isDisable = block
        btnOcrProcessar.isDisable = block
        btnProcessar.isDisable = block
    }

    private fun configuraTextEdit() {
        txtPastaProcessar.onKeyPressed = EventHandler { e: javafx.scene.input.KeyEvent ->
            if (e.code == KeyCode.ENTER) {
                carregarItens()
                Utils.clickTab()
            }
        }
    }

    private fun configurarDragAndDrop() {
        apRoot.onDragOver = EventHandler { event ->
            if (event.gestureSource !== apRoot && event.dragboard.hasFiles()) {
                val aceito = event.dragboard.files.any { it.isDirectory || Utils.isRar(it.name) || it.extension.lowercase() == "xml" || it.extension.lowercase() == "txt" }
                event.acceptTransferModes(if (aceito) TransferMode.COPY else null)
                mostrarOverlayDrag(aceito)
            }
            event.consume()
        }

        apRoot.onDragExited = EventHandler { esconderOverlayDrag() }

        apRoot.onDragDropped = EventHandler { event ->
            val db = event.dragboard
            if (db.hasFiles()) {
                val files = db.files ?: emptyList<File>()
                if (files.isNotEmpty()) {
                    val first = files.first()
                    if (first.extension.lowercase() == "txt") {
                        val content = first.readText(Charsets.UTF_8)
                        val capRegex = Regex("(?i)(?:Chapter|Ch\\.?|Capítulo|Cap\\.?)\\s*(\\d+(?:\\.\\d+)?)")
                        if (content.lines().any { capRegex.containsMatchIn(it) }) {
                            abrirPopupCapitulos(content)
                        }
                    } else {
                        val targetDir = if (first.isDirectory) first else first.parentFile
                        if (targetDir != null && targetDir.exists()) {
                            txtPastaProcessar.text = targetDir.absolutePath
                            carregarItens()
                        }
                    }
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

        controllerPai.lblDragDrop.text = if (aceito) "Arraste a pasta aqui" else "Formato não aceito"
        controllerPai.lblDragDrop.textFill = if (aceito) Color.WHITE else Color.RED
        controllerPai.apDragOverlay.isVisible = true
    }

    private fun esconderOverlayDrag() {
        controllerPai.rootTab.effect = null
        controllerPai.apDragOverlay.isVisible = false
    }

    private var mCANCELAR = false
    private fun processaOCR() {
        val separador = Utils.SEPARADOR_CAPITULO
        val listaProcessar = mObsListaProcessar.toList()
        val linguagemOcr = cbLinguagem.value ?: Linguagem.JAPANESE
        val processaOCR: Task<Boolean> = object : Task<Boolean>() {
            override fun call(): Boolean {
                try {
                    mCANCELAR = false
                    val max = 10L
                    var i = 0
                    updateMessage("Processando o OCR...")

                    for (item in listaProcessar) {
                        if (item.isProcessado)
                            continue
                        i++
                        if (mCANCELAR || i > max)
                            break

                        updateProgress(i.toLong(), max)
                        updateMessage("Processando item " + i + " de " + max + ". Processando OCR - " + item.arquivo)

                        val extractDir = File(mPASTA_TEMPORARIA, "ocr_task_${System.currentTimeMillis()}")
                        extractDir.mkdirs()
                        try {
                            val sumario = mRarService.extraiSumario(item.file!!, extractDir)
                            if (sumario == null) {
                                i--
                                continue
                            }

                            val capitulos = mOcrService.processOcr(
                                sumario,
                                Utils.SEPARADOR_PAGINA,
                                separador,
                                linguagemOcr
                            ).split("\n")
                            val newTag = mutableSetOf<String>()
                            val tags = item.comicInfo?.pages?.filter { !it.bookmark.isNullOrEmpty() }?.map { it.image.toString() + Utils.SEPARADOR_IMAGEM + it.bookmark }?.toList()
                                ?: emptyList()

                            var index = -1
                            for (tag in tags) {
                                if (tag.contains("capítulo", ignoreCase = true) || tag.contains("第", ignoreCase = true)) {
                                    index++
                                    val capitulo = if (index < capitulos.size) capitulos[index] else ""
                                    newTag.add("$tag ${Utils.SEPARADOR_IMPORTACAO} $capitulo")
                                } else
                                    newTag.add(tag)
                            }

                            if (index < capitulos.size) {
                                for (idx in index + 1 until capitulos.size)
                                    newTag.add("0${Utils.SEPARADOR_IMAGEM} Capítulo novo ${Utils.SEPARADOR_IMPORTACAO} ${capitulos[idx]}")
                            }

                            val finalTags = if (newTag.isNotEmpty()) newTag.joinToString(separator = "\n") else item.tags
                            Platform.runLater {
                                item.tags = finalTags
                                item.isProcessado = true
                            }
                        } finally {
                            extractDir.deleteRecursively()
                        }
                    }

                    if (!mCANCELAR) {
                        updateProgress(max, max)
                        updateMessage("Processamento de OCR finalizado.")
                    }
                } catch (e: Exception) {
                    mLOG.error("Erro ao processar o OCR.", e)
                    Platform.runLater { AlertasModal.erro("Erro ao processar o OCR", e.stackTrace.toString()) }
                }
                return true
            }

            override fun succeeded() {
                updateMessage("OCR processado com sucesso.")
                controllerPai.rootProgress.progressProperty().unbind()
                controllerPai.rootMessage.textProperty().unbind()
                controllerPai.clearProgress()
                habilita()
                tbViewProcessar.refresh()
                Notificacoes.notificacao(Notificacao.SUCESSO, "OCR Capítulos", "OCR processado com sucesso.")
            }

            override fun failed() {
                super.failed()
                updateMessage("Erro ao processar o OCR.")
                AlertasModal.erro("Erro ao processar o OCR", super.getMessage() ?: "Erro desconhecido")
                habilita()
                tbViewProcessar.refresh()
            }
        }
        controllerPai.rootProgress.progressProperty().unbind()
        controllerPai.rootMessage.textProperty().unbind()
        controllerPai.rootProgress.progressProperty().bind(processaOCR.progressProperty())
        controllerPai.rootMessage.textProperty().bind(processaOCR.messageProperty())
        val t = Thread(processaOCR)
        t.isDaemon = true
        t.start()
    }

    private fun extrairCapaDoArquivo(arquivo: File): String? {
        try {
            val conteudo = mRarService.listarConteudo(arquivo)
            val primeiraImagem = conteudo.filter { Utils.isImage(it) }.sortedNaturally().firstOrNull() ?: return null
            
            if (!mPASTA_COVERS.exists()) mPASTA_COVERS.mkdirs()
            
            val extension = File(primeiraImagem).extension.ifEmpty { "jpg" }
            val cleanName = arquivo.nameWithoutExtension.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val coverDestFile = File(mPASTA_COVERS, "cover_${cleanName}.${extension}")
            
            val tempExtractDir = File(mPASTA_TEMPORARIA, "cover_extract_${System.currentTimeMillis()}")
            tempExtractDir.mkdirs()
            try {
                if (mRarService.extrairItens(arquivo, listOf(primeiraImagem), tempExtractDir)) {
                    val extractedFile = File(tempExtractDir, primeiraImagem)
                    if (extractedFile.exists()) {
                        extractedFile.copyTo(coverDestFile, overwrite = true)
                        return coverDestFile.absolutePath
                    } else {
                        val foundImg = tempExtractDir.walkTopDown().filter { it.isFile && Utils.isImage(it.name) }.firstOrNull()
                        if (foundImg != null) {
                            foundImg.copyTo(coverDestFile, overwrite = true)
                            return coverDestFile.absolutePath
                        }
                    }
                }
            } finally {
                tempExtractDir.deleteRecursively()
            }
        } catch (e: Exception) {
            mLOG.error("Erro ao extrair capa do arquivo: ${arquivo.name}", e)
        }
        return null
    }

    private fun carregarItens() {
        val path = txtPastaProcessar.text?.trim() ?: ""
        val pasta = File(path)
        if (path.isNotEmpty() && pasta.exists() && pasta.isDirectory) {
            if (mPASTA_COVERS.exists()) {
                mPASTA_COVERS.deleteRecursively()
            }
            mPASTA_COVERS.mkdirs()

            btnCarregar.isDisable = true
            controllerPai.setCursor(Cursor.WAIT)
            val linguagemCarregamento = cbLinguagem.value ?: Linguagem.PORTUGUESE

            val processar: Task<Void> = object : Task<Void>() {
                override fun call(): Void? {
                    try {
                        val lista = mutableListOf<Processar>()
                        val max = pasta.listFiles()?.size ?: 0
                        var i = 0
                        updateMessage("Carregando arquivos...")

                        val linguagem = linguagemCarregamento
                        for (arquivo in pasta.listFiles()!!) {
                            if (!Utils.isRar(arquivo.name))
                                continue

                            i++
                            updateProgress(i.toLong(), max.toLong())
                            updateMessage("Carregando item $i de $max.")

                            val extractDir = File(mPASTA_TEMPORARIA, "load_${i}_${System.currentTimeMillis()}")
                            extractDir.mkdirs()
                            var semComicInfo = !arquivoPossuiComicInfo(arquivo)
                            val comic: ComicInfo = try {
                                if (semComicInfo) {
                                    val basico = criarComicInfoBasico(arquivo, linguagem)
                                    if (inserirComicInfoNoArquivo(arquivo, basico)) {
                                        semComicInfo = false
                                        lerComicInfoDoArquivo(arquivo, extractDir) ?: basico
                                    } else
                                        basico
                                } else {
                                    lerComicInfoDoArquivo(arquivo, extractDir)?.also {
                                        semComicInfo = false
                                    } ?: ComicInfo().also { semComicInfo = true }
                                }
                            } catch (e: Exception) {
                                mLOG.error(e.message, e)
                                ComicInfo().also { semComicInfo = true }
                            } finally {
                                extractDir.deleteRecursively()
                            }

                            val tags = tagsFromComic(comic)
                            val coverPath = extrairCapaDoArquivo(arquivo)
                            val processar = JFXButton("Processar").apply { id = "btnProcessar_${i}" }
                            val amazon = JFXButton("Amazon").apply { id = "btnAmazon_${i}" }
                            val salvar = JFXButton("Salvar").apply { id = "btnSalvar_${i}" }
                            val item = Processar(
                                arquivo.name,
                                tags,
                                arquivo,
                                comic,
                                processar,
                                amazon,
                                salvar,
                                semComicInfo = semComicInfo,
                                capaPath = coverPath
                            )

                            processar.styleClass.add("background-White1")
                            processar.setOnAction { processarOcrItem(item) }
                            amazon.styleClass.add("background-White1")
                            amazon.setOnAction { popupAmazon(item) }
                            salvar.styleClass.add("background-White1")
                            salvar.setOnAction { salvarComicInfoItem(item) }

                            lista.add(item)
                        }

                        Platform.runLater {
                            mHistory.clear()
                            mObsListaProcessar = FXCollections.observableArrayList(lista)
                            tbViewProcessar.items = mObsListaProcessar
                            atualizaCheckTodosProcessado()

                            val lastLang = lista.lastOrNull()?.comicInfo?.languageISO
                            if (!lastLang.isNullOrEmpty()) {
                                val lingua = Linguagem.getEnum(lastLang)
                                if (lingua != null) cbLinguagem.selectionModel.select(lingua)
                            }
                        }
                    } catch (e: Exception) {
                        mLOG.info("Erro ao carregar itens para processamento de mangas.", e)
                        Platform.runLater {
                            Notificacoes.notificacao(Notificacao.ERRO, "Mangas Processamento", "Erro ao carregar itens para processamento de mangas. " + e.message)
                        }
                    }
                    return null
                }

                override fun succeeded() {
                    updateMessage("Arquivos carregados com sucesso.")
                    controllerPai.rootProgress.progressProperty().unbind()
                    controllerPai.rootMessage.textProperty().unbind()
                    controllerPai.clearProgress()
                    controllerPai.setCursor(null)
                    btnCarregar.isDisable = false
                }
            }
            controllerPai.rootProgress.progressProperty().unbind()
            controllerPai.rootMessage.textProperty().unbind()
            controllerPai.rootProgress.progressProperty().bind(processar.progressProperty())
            controllerPai.rootMessage.textProperty().bind(processar.messageProperty())
            Thread(processar).start()
        } else {
            AlertasModal.alerta("Alerta", "Necessário informar uma pasta para processar.")
            txtPastaProcessar.requestFocus()
        }
    }

    private fun salvarComicInfoItem(item: Processar) {
        val tempDir = File(mPASTA_TEMPORARIA, "save_single_${System.currentTimeMillis()}_${UUID.randomUUID()}")
        tempDir.mkdirs()
        try {
            val info = File(tempDir, "ComicInfo.xml")
            if (info.exists())
                info.delete()

            item.comicInfo?.pages?.forEach { it.bookmark = null }

            val tags = item.tags.split("\n")
            var temTitulo = false
            val sbSumario = StringBuilder()

            for (tag in tags) {
                val imagem = tag.substringBefore(Utils.SEPARADOR_IMAGEM)
                var capitulo = tag.substringAfter(Utils.SEPARADOR_IMAGEM).trim()

                if (capitulo.endsWith(Utils.SEPARADOR_IMPORTACAO))
                    capitulo = capitulo.substringBefore(Utils.SEPARADOR_IMPORTACAO).trim()

                if (capitulo.isEmpty())
                    continue

                capitulo.lowercase().let {
                    if (it.contains("第") || it.contains("chapter") || it.contains("capítulo")) {
                        val hasDelimiter = capitulo.contains("-") || capitulo.contains("—")
                        val delimiter = if (capitulo.contains("—")) "—" else "-"

                        val chapterPart = if (hasDelimiter) capitulo.substringBefore(delimiter).trim() else capitulo.trim()
                        val numero = if (chapterPart.lowercase().contains("第"))
                            Utils.fromNumberJapanese(chapterPart.lowercase().replace("第", "Chapter ").replace("話", "").trim())
                        else
                            chapterPart.lowercase().replace("capítulo", "Chapter").replace("chapter", "Chapter").trim()

                        val tituloCapitulo = if (hasDelimiter) capitulo.substringAfter(delimiter).trim() else ""

                        if (tituloCapitulo.isNotEmpty() && !tituloCapitulo.equals(chapterPart, ignoreCase = true)) {
                            temTitulo = true
                            sbSumario.append(Utils.normaliza(numero)).append(": ").append(tituloCapitulo).append("\n")
                        }
                    }
                }

                val page = item.comicInfo?.pages?.firstOrNull { it.image.toString() == imagem } ?: continue
                page.bookmark = capitulo
            }

            if (temTitulo) {
                val separator = "*Chapter Titles Manual*"
                val sumarioFinal = "$separator\n" + sbSumario.toString()
                item.comicInfo?.run {
                    val currentSummary = summary ?: ""
                    val idx = currentSummary.lowercase().indexOf(separator.lowercase())
                    summary = if (idx != -1) {
                        currentSummary.substring(0, idx).trim() + "\n\n" + sumarioFinal
                    } else {
                        if (currentSummary.isEmpty()) sumarioFinal else currentSummary.trim() + "\n\n" + sumarioFinal
                    }
                }
            }

            val marshaller = JAXBContext.newInstance(ComicInfo::class.java).createMarshaller()
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
            val out = FileOutputStream(info)
            marshaller.marshal(item.comicInfo, out)
            out.close()
            mRarService.insereComicInfo(item.file!!, info)
            Platform.runLater {
                mHistory.removeHistoryForItem(item)
                mObsListaProcessar.remove(item)
                tbViewProcessar.refresh()
            }
        } catch (e: Exception) {
            mLOG.error(e.message, e)
            Platform.runLater {
                AlertasModal.erro("Erro", "Erro ao salvar o ComicInfo no arquivo ComicInfo.xml. " + e.message)
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun salvarComicInfoItemAsync(item: Processar) {
        if (item.isSalvandoOuProcessando) return
        item.isSalvandoOuProcessando = true
        tbViewProcessar.refresh()

        if (activeTasksCount.getAndIncrement() == 0) {
            bloquearControlesGerais(true)
        }

        val task = object : Task<Void>() {
            override fun call(): Void? {
                salvarComicInfoItem(item)
                return null
            }

            override fun succeeded() {
                item.isSalvandoOuProcessando = false
                tbViewProcessar.refresh()
                if (activeTasksCount.decrementAndGet() == 0) {
                    bloquearControlesGerais(false)
                }
                Notificacoes.notificacao(Notificacao.SUCESSO, "Salvar ComicInfo", "Item salvo com sucesso.")
            }

            override fun failed() {
                item.isSalvandoOuProcessando = false
                tbViewProcessar.refresh()
                if (activeTasksCount.decrementAndGet() == 0) {
                    bloquearControlesGerais(false)
                }
                mLOG.error("Erro ao salvar o item", exception)
                AlertasModal.erro("Erro ao salvar", exception?.message ?: "Erro desconhecido")
            }
        }
        saveExecutor.submit(task)
    }

    private fun processarComicInfoItemAsync(item: Processar) {
        if (item.isSalvandoOuProcessando) return
        val file = item.file ?: return
        if (!file.exists()) {
            AlertasModal.alerta("Alerta", "Arquivo não encontrado para processar.")
            return
        }

        item.isSalvandoOuProcessando = true
        tbViewProcessar.refresh()

        if (activeTasksCount.getAndIncrement() == 0) {
            bloquearControlesGerais(true)
        }

        val linguagem = cbLinguagem.value ?: Linguagem.PORTUGUESE
        val marcaCapitulo = when (linguagem) {
            Linguagem.PORTUGUESE -> "Capítulo"
            Linguagem.ENGLISH -> "Chapter"
            Linguagem.JAPANESE -> "第%s話"
            else -> ""
        }

        val task = object : Task<Void>() {
            override fun call(): Void? {
                val tempDir = File(mPASTA_TEMPORARIA, "process_single_${System.currentTimeMillis()}_${UUID.randomUUID()}")
                tempDir.mkdirs()
                try {
                    com.fenix.ordenararquivos.process.ComicInfo.processaArquivo(linguagem, file, marcaCapitulo, tempDir)
                } finally {
                    tempDir.deleteRecursively()
                }
                return null
            }

            override fun succeeded() {
                item.isSalvandoOuProcessando = false
                recarregarComicInfoItem(item)
                tbViewProcessar.refresh()
                if (activeTasksCount.decrementAndGet() == 0) {
                    bloquearControlesGerais(false)
                }
                Notificacoes.notificacao(Notificacao.SUCESSO, "Processamento ComicInfo", "ComicInfo do item processado com sucesso.")
            }

            override fun failed() {
                item.isSalvandoOuProcessando = false
                tbViewProcessar.refresh()
                if (activeTasksCount.decrementAndGet() == 0) {
                    bloquearControlesGerais(false)
                }
                mLOG.error("Erro ao processar item", exception)
                AlertasModal.erro("Erro ao processar o ComicInfo", exception?.message ?: "Erro desconhecido")
            }
        }
        Thread(task).start()
    }

    private fun salvarItens(startIndex: Int = 0, endIndex: Int = 0) {
        val list = mObsListaProcessar.toList()
        val start = if (startIndex < 0) 0 else startIndex
        val end = if (endIndex > list.size) list.size else endIndex
        for (i in start until end) {
            salvarComicInfoItemAsync(list[i])
        }
    }

    private fun salvarListaItens(itens: List<Processar>) {
        itens.forEach { salvarComicInfoItemAsync(it) }
    }

    internal fun arquivoPossuiComicInfo(arquivo: File): Boolean =
        arquivoPossuiComicInfoNaListagem(mRarService.listarConteudo(arquivo))

    private fun lerComicInfoDoArquivo(arquivo: File, extractDir: File): ComicInfo? {
        val info = mRarService.extraiComicInfo(arquivo, extractDir) ?: return null
        if (!info.exists())
            return null
        return try {
            val unmarshaller = JAXBContext.newInstance(ComicInfo::class.java).createUnmarshaller()
            unmarshaller.unmarshal(info) as ComicInfo
        } catch (e: Exception) {
            mLOG.error(e.message, e)
            null
        }
    }

    private fun inserirComicInfoNoArquivo(arquivo: File, comic: ComicInfo): Boolean {
        val tempDir = File(mPASTA_TEMPORARIA, "comicinfo_insert_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        return try {
            val info = File(tempDir, Utils.COMICINFO)
            val marshaller = JAXBContext.newInstance(ComicInfo::class.java).createMarshaller()
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
            FileOutputStream(info).use { marshaller.marshal(comic, it) }
            mRarService.insereComicInfo(arquivo, info)
        } catch (e: Exception) {
            mLOG.error("Erro ao inserir ComicInfo em ${arquivo.name}", e)
            false
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun recarregarComicInfoItem(item: Processar) {
        val arquivo = item.file ?: return
        val extractDir = File(mPASTA_TEMPORARIA, "reload_${System.currentTimeMillis()}_${arquivo.name.hashCode()}")
        extractDir.mkdirs()
        try {
            val possuiComicInfo = arquivoPossuiComicInfo(arquivo)
            val comic = lerComicInfoDoArquivo(arquivo, extractDir)
            item.semComicInfo = !possuiComicInfo || comic == null
            item.comicInfo = comic ?: ComicInfo()
            item.tags = if (comic != null) tagsFromComic(comic) else ""
            item.arquivo = arquivo.name
            item.capaPath = extrairCapaDoArquivo(arquivo)
        } finally {
            extractDir.deleteRecursively()
        }
    }

    private fun recarregarComicInfoItens(itens: List<Processar>) {
        itens.forEach { recarregarComicInfoItem(it) }
        tbViewProcessar.refresh()
    }

    private fun processarComicInfoItem(item: Processar) {
        processarComicInfoItens(listOf(item))
    }

    private fun processarComicInfoItens(itens: List<Processar>) {
        if (itens.isEmpty()) {
            Notificacoes.notificacao(Notificacao.ALERTA, "Processar ComicInfo", "Selecione pelo menos um registro na tabela.")
            return
        }

        val validos = itens.filter { it.file != null && it.file!!.exists() }
        if (validos.isEmpty()) {
            AlertasModal.alerta("Alerta", "Arquivo não encontrado para processar.")
            return
        }

        controllerPai.setCursor(Cursor.WAIT)
        desabilita()

        val linguagem = cbLinguagem.value ?: Linguagem.PORTUGUESE
        val marcaCapitulo = when (linguagem) {
            Linguagem.PORTUGUESE -> "Capítulo"
            Linguagem.ENGLISH -> "Chapter"
            Linguagem.JAPANESE -> "第%s話"
            else -> ""
        }

        val processa: Task<Boolean> = object : Task<Boolean>() {
            override fun call(): Boolean {
                val total = validos.size.toLong()
                validos.forEachIndexed { i, item ->
                    updateProgress(i.toLong() + 1, total)
                    updateMessage("Processando ComicInfo ${i + 1} de $total: ${item.arquivo}")
                    com.fenix.ordenararquivos.process.ComicInfo.processaArquivo(linguagem, item.file!!, marcaCapitulo)
                }
                return true
            }

            override fun succeeded() {
                updateMessage("Processamento finalizado.")
                controllerPai.rootProgress.progressProperty().unbind()
                controllerPai.rootMessage.textProperty().unbind()
                controllerPai.clearProgress()
                controllerPai.setCursor(null)
                habilita()
                Platform.runLater {
                    recarregarComicInfoItens(validos)
                    val mensagem = if (validos.size == 1)
                        "ComicInfo do item processado com sucesso."
                    else
                        "${validos.size} itens processados com sucesso."
                    Notificacoes.notificacao(Notificacao.SUCESSO, "Processamento ComicInfo", mensagem)
                }
            }

            override fun failed() {
                super.failed()
                mLOG.error("Erro na Task de processamento de ComicInfo", exception)
                updateMessage("Erro ao processar o ComicInfo.")
                controllerPai.rootProgress.progressProperty().unbind()
                controllerPai.rootMessage.textProperty().unbind()
                controllerPai.clearProgress()
                controllerPai.setCursor(null)
                habilita()
                AlertasModal.erro("Erro ao processar o ComicInfo", exception?.message ?: "Erro desconhecido")
            }
        }

        controllerPai.rootProgress.progressProperty().unbind()
        controllerPai.rootMessage.textProperty().unbind()
        controllerPai.rootProgress.progressProperty().bind(processa.progressProperty())
        controllerPai.rootMessage.textProperty().bind(processa.messageProperty())
        val t = Thread(processa)
        t.isDaemon = true
        t.start()
    }

    private fun processarOcrItem(item: Processar) {
        controllerPai.setCursor(Cursor.WAIT)
        val extractDir = File(mPASTA_TEMPORARIA, "ocr_direto_${System.currentTimeMillis()}")
        extractDir.mkdirs()

        val task = object : Task<File?>() {
            override fun call(): File? {
                return mRarService.extraiSumario(item.file!!, extractDir)
            }

            override fun succeeded() {
                val sumario = value
                if (sumario != null && sumario.exists()) {
                    executarOcrItem(item, sumario, extractDir)
                } else {
                    controllerPai.setCursor(null)
                    extractDir.deleteRecursively()
                    AlertasModal.aviso("OCR", "Não foi possível localizar o sumário no arquivo.")
                }
            }

            override fun failed() {
                controllerPai.setCursor(null)
                extractDir.deleteRecursively()
                AlertasModal.erro("Erro", "Erro ao extrair sumário: ${exception.message}")
            }
        }
        Thread(task).start()
    }

    private fun visualizarSumarioItem(item: Processar) {
        controllerPai.setCursor(Cursor.WAIT)
        val extractDir = File(mPASTA_TEMPORARIA, "ocr_preview_${System.currentTimeMillis()}")
        extractDir.mkdirs()

        val task = object : Task<File?>() {
            override fun call(): File? {
                return mRarService.extraiSumario(item.file!!, extractDir)
            }

            override fun succeeded() {
                controllerPai.setCursor(null)
                val sumario = value
                if (sumario != null && sumario.exists()) {
                    abrirPopupVisualizarSumario(item, sumario, extractDir)
                } else {
                    extractDir.deleteRecursively()
                    AlertasModal.aviso("OCR", "Não foi possível localizar o sumário no arquivo.")
                }
            }

            override fun failed() {
                controllerPai.setCursor(null)
                extractDir.deleteRecursively()
                AlertasModal.erro("Erro", "Erro ao extrair sumário: ${exception.message}")
            }
        }
        Thread(task).start()
    }

    private fun abrirPopupVisualizarSumario(item: Processar, sumarioFile: File, extractDir: File) {
        PopupImagemSumarioController.abreTelaImagemSumario(controllerPai.rootStack, controllerPai.rootTab, item, sumarioFile, { file, dir ->
            executarOcrItem(item, file, dir)
        }, {
            if (extractDir.exists()) {
                Thread {
                    Thread.sleep(500)
                    extractDir.deleteRecursively()
                }.start()
            }
        })
    }

    private fun executarOcrItem(item: Processar, sumario: File, extractDir: File) {
        controllerPai.setCursor(Cursor.WAIT)
        val language = cbLinguagem.value ?: Linguagem.PORTUGUESE

        val task = object : Task<Unit>() {
            override fun call() {
                val capitulos = mOcrService.processOcr(
                    sumario,
                    Utils.SEPARADOR_PAGINA,
                    Utils.SEPARADOR_CAPITULO,
                    language
                ).split("\n")
                val newTag = mutableSetOf<String>()
                val tags = item.comicInfo!!.pages?.filter { !it.bookmark.isNullOrEmpty() }?.map { it.image.toString() + Utils.SEPARADOR_IMAGEM + it.bookmark }?.toList() ?: emptyList()

                var index = -1
                for (tag in tags) {
                    if (tag.contains("capítulo", ignoreCase = true) || tag.contains("第", ignoreCase = true)) {
                        index++
                        val capitulo = if (index < capitulos.size) capitulos[index] else ""
                        newTag.add("$tag ${Utils.SEPARADOR_IMPORTACAO} $capitulo")
                    } else
                        newTag.add(tag)
                }

                if (index < capitulos.size) {
                    for (i in index + 1 until capitulos.size) {
                        val capLabel = when (language) {
                            Linguagem.JAPANESE -> "?${Utils.toNumberJapanese(mDecimal.format(Utils.getNumber(capitulos[i]) ?: 0.0))}?"
                            Linguagem.PORTUGUESE -> "Capítulo novo ${mDecimal.format(Utils.getNumber(capitulos[i]) ?: 0.0)}"
                            else -> "New Chapter ${mDecimal.format(Utils.getNumber(capitulos[i]) ?: 0.0)}"
                        }
                        newTag.add("-1${Utils.SEPARADOR_IMAGEM} $capLabel ${Utils.SEPARADOR_IMPORTACAO} ${capitulos[i]}")
                    }
                }

                item.tags = if (newTag.isNotEmpty()) newTag.joinToString(separator = "\n") else item.tags
                item.isProcessado = true
            }

            override fun succeeded() {
                controllerPai.setCursor(null)
                tbViewProcessar.refresh()
                extractDir.deleteRecursively()
            }

            override fun failed() {
                controllerPai.setCursor(null)
                extractDir.deleteRecursively()
                AlertasModal.erro("Erro", "Erro no processamento OCR: ${exception.message}")
            }
        }
        Thread(task).start()
    }

    private fun aplicarTag(item: Processar): PropertyChangeAction<Processar, String>? {
        if (item.tags.isEmpty() || !item.tags.contains(Utils.SEPARADOR_IMPORTACAO))
            return null

        val oldTags = item.tags
        val linhas = mutableListOf<String>()
        val separador = Utils.SEPARADOR_CAPITULO
        for (linha in item.tags.split("\n")) {
            if (linha.contains(Utils.SEPARADOR_IMPORTACAO)) {
                val prefix = linha.substringBefore(Utils.SEPARADOR_IMPORTACAO).trim()
                val basePrefix = prefix.split(Regex("—|-"), 2)[0].trim()
                val title = linha.substringAfterLast(separador).trim()
                val cleanTitle = Utils.limparTitulo(title)
                linhas.add("$basePrefix — $cleanTitle")
            } else {
                linhas.add(linha)
            }
        }

        val newTags = linhas.joinToString(separator = "\n")
        if (newTags != oldTags) {
            item.tags = newTags
            return PropertyChangeAction(item, oldTags, newTags) { i, v -> i.tags = v }
        }
        return null
    }

    private fun gerarTagItem(item: Processar, language: Linguagem, isAjustar: Boolean = false): PropertyChangeAction<Processar, String>? {
        val oldTags = item.tags
        
        val mapTitulosExistentes = if (isAjustar && oldTags.isNotEmpty()) {
            oldTags.split("\n").associate { linha ->
                val imagem = linha.substringBefore(Utils.SEPARADOR_IMAGEM)
                val resto = linha.substringAfter(Utils.SEPARADOR_IMAGEM)
                val tagSemImportacao = if (resto.contains(Utils.SEPARADOR_IMPORTACAO)) {
                    resto.substringBefore(Utils.SEPARADOR_IMPORTACAO).trim()
                } else {
                    resto.trim()
                }
                val titulo = if (tagSemImportacao.contains("第") || tagSemImportacao.contains("capítulo") || tagSemImportacao.contains("capitulo") || tagSemImportacao.contains("chapter")) {
                    val partes = tagSemImportacao.split(Regex("—|-"), 2)
                    if (partes.size > 1) partes[1].trim() else ""
                } else {
                    tagSemImportacao
                }
                imagem to titulo
            }
        } else {
            emptyMap()
        }

        val mapImportacoesExistentes = if (isAjustar && oldTags.isNotEmpty()) {
            oldTags.split("\n").associate { linha ->
                val imagem = linha.substringBefore(Utils.SEPARADOR_IMAGEM)
                val resto = linha.substringAfter(Utils.SEPARADOR_IMAGEM)
                val importacao = if (resto.contains(Utils.SEPARADOR_IMPORTACAO)) {
                    Utils.SEPARADOR_IMPORTACAO + " " + resto.substringAfter(Utils.SEPARADOR_IMPORTACAO).trim()
                } else {
                    ""
                }
                imagem to importacao
            }
        } else {
            emptyMap()
        }

        val tags = item.comicInfo?.pages?.filter { !it.bookmark.isNullOrEmpty() }?.map { p ->
            val b = p.bookmark!!.split("-", "—")[0].trim()
            val mark = b.lowercase()
            var prefix = if (mark.contains("第") || mark.contains("capítulo") || mark.contains("capitulo") || mark.contains("chapter")) {
                val capitulo = Utils.getNumber(if (mark.contains("第")) Utils.fromNumberJapanese(b) else b) ?: 0.0
                val formattedCap = when (language) {
                    Linguagem.JAPANESE -> "第${Utils.toNumberJapanese(mDecimal.format(capitulo))}話"
                    Linguagem.PORTUGUESE -> "Capítulo ${mDecimal.format(capitulo)}"
                    else -> "Chapter ${mDecimal.format(capitulo)}"
                }
                var tagFinal = formattedCap
                if (isAjustar) {
                    var titulo = mapTitulosExistentes[p.image.toString()] ?: ""
                    if (titulo.isEmpty() && !p.bookmark.isNullOrEmpty()) {
                        val parts = p.bookmark!!.split(Regex("—|-"), 2)
                        if (parts.size > 1) {
                            titulo = parts[1].trim()
                        }
                    }
                    if (titulo.isNotEmpty()) {
                        tagFinal += " — $titulo"
                    }
                }
                tagFinal
            } else b

            if (isAjustar) {
                val importacao = mapImportacoesExistentes[p.image.toString()] ?: ""
                if (importacao.isNotEmpty()) {
                    prefix += " $importacao"
                }
            }

            p.image.toString() + Utils.SEPARADOR_IMAGEM + prefix
        }?.toList() ?: emptyList()
        val newTags = tags.joinToString(separator = "\n")
        if (oldTags != newTags) {
            item.tags = newTags
            return PropertyChangeAction(item, oldTags, newTags) { i, v -> i.tags = v }
        }
        return null
    }

    private fun normalizarTagItem(item: Processar, language: Linguagem): PropertyChangeAction<Processar, String>? {
        val oldTags = item.tags
        val linhas = item.tags.split("\n")
        val tags = mutableListOf<String>()
        for (linha in linhas) {
            val ln = linha.lowercase()
            if (ln.contains("第") || ln.contains("capítulo") || ln.contains("capitulo") || ln.contains("chapter")) {
                val imagem = linha.substringBefore(Utils.SEPARADOR_IMAGEM)
                val resto = linha.substringAfter(Utils.SEPARADOR_IMAGEM)
                
                var prefixPart = resto
                var importPart = ""
                
                if (resto.contains(Utils.SEPARADOR_IMPORTACAO)) {
                    prefixPart = resto.substringBefore(Utils.SEPARADOR_IMPORTACAO).trim()
                    importPart = resto.substringAfter(Utils.SEPARADOR_IMPORTACAO).trim()
                }
                
                // Processa prefixPart (Capítulo + Título Original)
                val capParts = prefixPart.split(Regex("—|-"), 2)
                val capLabel = capParts[0].trim()
                val originalTitle = if (capParts.size > 1) capParts[1].trim() else ""
                
                val numero = Utils.getNumber(if (capLabel.contains("第")) Utils.fromNumberJapanese(capLabel) else capLabel) ?: 0.0
                val formattedCap = when (language) {
                    Linguagem.JAPANESE -> "第${Utils.toNumberJapanese(mDecimal.format(numero))}話"
                    Linguagem.PORTUGUESE -> "Capítulo ${mDecimal.format(numero)}"
                    else -> "Chapter ${mDecimal.format(numero)}"
                }
                
                var resultLine = "$imagem${Utils.SEPARADOR_IMAGEM}$formattedCap"
                if (originalTitle.isNotEmpty()) {
                    resultLine += " — ${Utils.normalizaSentenca(originalTitle)}"
                }
                
                // Processa importPart ($# Número|Título Importado)
                if (importPart.isNotEmpty()) {
                    val importNum = importPart.substringBefore(Utils.SEPARADOR_CAPITULO).trim()
                    val importTitle = if (importPart.contains(Utils.SEPARADOR_CAPITULO)) importPart.substringAfter(Utils.SEPARADOR_CAPITULO).trim() else ""
                    
                    resultLine += " ${Utils.SEPARADOR_IMPORTACAO} $importNum"
                    if (importTitle.isNotEmpty()) {
                        resultLine += "${Utils.SEPARADOR_CAPITULO}${Utils.normalizaSentenca(importTitle)}"
                    }
                }
                
                tags.add(resultLine)
            } else
                tags.add(linha)
        }
        val newTags = tags.joinToString(separator = "\n")
        if (oldTags != newTags) {
            item.tags = newTags
            return PropertyChangeAction(item, oldTags, newTags) { i, v -> i.tags = v }
        }
        return null
    }

    private fun formatarTitleCaseTagItem(item: Processar, language: Linguagem): PropertyChangeAction<Processar, String>? {
        val oldTags = item.tags
        val linhas = item.tags.split("\n")
        val tags = mutableListOf<String>()
        for (linha in linhas) {
            val ln = linha.lowercase()
            if (ln.contains("第") || ln.contains("capítulo") || ln.contains("capitulo") || ln.contains("chapter")) {
                val imagem = linha.substringBefore(Utils.SEPARADOR_IMAGEM)
                val resto = linha.substringAfter(Utils.SEPARADOR_IMAGEM)
                
                var prefixPart = resto
                var importPart = ""
                
                if (resto.contains(Utils.SEPARADOR_IMPORTACAO)) {
                    prefixPart = resto.substringBefore(Utils.SEPARADOR_IMPORTACAO).trim()
                    importPart = resto.substringAfter(Utils.SEPARADOR_IMPORTACAO).trim()
                }
                
                // Processa prefixPart (Capítulo + Título Original)
                val capParts = prefixPart.split(Regex("—|-"), 2)
                val capLabel = capParts[0].trim()
                val originalTitle = if (capParts.size > 1) capParts[1].trim() else ""
                
                val numero = Utils.getNumber(if (capLabel.contains("第")) Utils.fromNumberJapanese(capLabel) else capLabel) ?: 0.0
                val formattedCap = when (language) {
                    Linguagem.JAPANESE -> "第${Utils.toNumberJapanese(mDecimal.format(numero))}話"
                    Linguagem.PORTUGUESE -> "Capítulo ${mDecimal.format(numero)}"
                    else -> "Chapter ${mDecimal.format(numero)}"
                }
                
                var resultLine = "$imagem${Utils.SEPARADOR_IMAGEM}$formattedCap"
                if (originalTitle.isNotEmpty()) {
                    resultLine += " — ${Utils.toTitleCaseInteligente(originalTitle)}"
                }
                
                // Processa importPart ($# Número|Título Importado)
                if (importPart.isNotEmpty()) {
                    val importNum = importPart.substringBefore(Utils.SEPARADOR_CAPITULO).trim()
                    val importTitle = if (importPart.contains(Utils.SEPARADOR_CAPITULO)) importPart.substringAfter(Utils.SEPARADOR_CAPITULO).trim() else ""
                    
                    resultLine += " ${Utils.SEPARADOR_IMPORTACAO} $importNum"
                    if (importTitle.isNotEmpty()) {
                        resultLine += "${Utils.SEPARADOR_CAPITULO}${Utils.toTitleCaseInteligente(importTitle)}"
                    }
                }
                
                tags.add(resultLine)
            } else
                tags.add(linha)
        }
        val newTags = tags.joinToString(separator = "\n")
        if (oldTags != newTags) {
            item.tags = newTags
            return PropertyChangeAction(item, oldTags, newTags) { i, v -> i.tags = v }
        }
        return null
    }


    private fun popupAmazon(item: Processar) {
        val callback: Callback<ComicInfo, Boolean> = Callback<ComicInfo, Boolean> { param ->
            item.comicInfo = param
            tbViewProcessar.refresh()
            null
        }
        PopupAmazonController.abreTelaAmazon(controllerPai.rootStack, controllerPai.rootTab, callback, item.comicInfo, cbLinguagem.value)
    }

    private fun editaColunas() {
        clTitulo.cellFactory = TextAreaTableCell.forTableColumn()
        clTitulo.setOnEditStart { e ->
            val item = e.tableView.items[e.tablePosition.row]
            if (item.isSalvandoOuProcessando) {
                Platform.runLater { e.tableView.edit(-1, null) }
            }
        }
        clTitulo.setOnEditCommit { e ->
            val item = e.tableView.items[e.tablePosition.row]
            if (item.isSalvandoOuProcessando) return@setOnEditCommit
            val oldVal = item.comicInfo?.title ?: ""
            val newVal = e.newValue
            if (oldVal != newVal) {
                mHistory.pushAction(PropertyChangeAction(item, oldVal, newVal) { i, v -> i.comicInfo?.title = v })
                item.comicInfo?.title = newVal
            }
        }

        clSerie.cellFactory = TextAreaTableCell.forTableColumn()
        clSerie.setOnEditStart { e ->
            val item = e.tableView.items[e.tablePosition.row]
            if (item.isSalvandoOuProcessando) {
                Platform.runLater { e.tableView.edit(-1, null) }
            }
        }
        clSerie.setOnEditCommit { e ->
            val item = e.tableView.items[e.tablePosition.row]
            if (item.isSalvandoOuProcessando) return@setOnEditCommit
            val oldVal = item.comicInfo?.series ?: ""
            val newVal = e.newValue
            if (oldVal != newVal) {
                mHistory.pushAction(PropertyChangeAction(item, oldVal, newVal) { i, v -> i.comicInfo?.series = v })
                item.comicInfo?.series = newVal
            }
        }

        clResumo.cellFactory = TextAreaTableCell.forTableColumn()
        clResumo.setOnEditStart { e ->
            val item = e.tableView.items[e.tablePosition.row]
            if (item.isSalvandoOuProcessando) {
                Platform.runLater { e.tableView.edit(-1, null) }
            }
        }
        clResumo.setOnEditCommit { e: TableColumn.CellEditEvent<Processar, String> ->
            val item = e.tableView.items[e.tablePosition.row]
            if (item.isSalvandoOuProcessando) return@setOnEditCommit
            val oldVal = item.comicInfo?.summary ?: ""
            val newVal = e.newValue
            if (oldVal != newVal) {
                mHistory.pushAction(PropertyChangeAction(item, oldVal, newVal) { i, v -> i.comicInfo?.summary = v })
                if (item.comicInfo != null)
                    item.comicInfo!!.summary = newVal
            }
        }

        clProcessado.setCellValueFactory { param ->
            val item = param.value
            val booleanProp = SimpleBooleanProperty(item.isProcessado)
            booleanProp.addListener { _, _, newValue ->
                if (item.isSalvandoOuProcessando) {
                    Platform.runLater { tbViewProcessar.refresh() }
                    return@addListener
                }
                if (item.isProcessado != newValue) {
                    val oldVal = item.isProcessado
                    mHistory.pushAction(PropertyChangeAction(item, oldVal, newValue) { i, v -> 
                        i.isProcessado = v
                        Platform.runLater { tbViewProcessar.refresh() }
                    })
                    item.isProcessado = newValue
                    atualizaCheckTodosProcessado()
                    tbViewProcessar.refresh()
                }
            }
            return@setCellValueFactory booleanProp
        }
        clProcessado.setCellFactory {
            val cell: CheckBoxTableCellCustom<Processar, Boolean> = CheckBoxTableCellCustom()
            cell.alignment = Pos.CENTER
            cell
        }

        clTags.cellFactory = TextAreaTableCell.forTableColumn(
            Tooltip(
                "Com o shift e alt pressionados poderá ser executado algumas funções no texto apresentado, são eles:\n" +
                        "Shift + Alt + Enter: Aplicação da tag gerada aos capítulos.\nShift + Alt + Delete: Apaga a linha selecionada ou remove o novo título se existir.\nShift + Alt + Left: Aplicar as tags da linha selecionada.\n" +
                        "Shift + Alt + Right: Aplicar capítulo importado da linha selecionada.\n" +
                        "Shift + Alt + Acima/Baixo: Move a tag da linha selecionada para cima ou baixo, movimentando outras tags subjacentes."
            )
        )
        clTags.setOnEditStart { e ->
            val item = e.tableView.items[e.tablePosition.row]
            if (item.isSalvandoOuProcessando) {
                Platform.runLater { e.tableView.edit(-1, null) }
            }
        }
        clTags.setOnEditCommit { e: TableColumn.CellEditEvent<Processar, String> ->
            val item = e.tableView.items[e.tablePosition.row]
            if (item.isSalvandoOuProcessando) return@setOnEditCommit
            val oldVal = item.tags
            val newVal = e.newValue
            if (oldVal != newVal) {
                mHistory.pushAction(PropertyChangeAction(item, oldVal, newVal) { i, v -> i.tags = v })
                item.tags = newVal
            }
        }

        clAcoes.setCellFactory {
            object : TableCell<Processar, Void>() {
                private val btnAplicar = JFXButton("Aplicar").apply {
                    styleClass.add("background-Indigo1")
                    textFill = Color.WHITE
                    setOnAction {
                        val rowItem = tableView.items[index]
                        aplicarTag(rowItem)?.let { mHistory.pushAction(it) }
                        tableView.refresh()
                    }
                }

                private val btnAjustar = JFXButton("Ajustar").apply {
                    styleClass.add("background-Blue3")
                    textFill = Color.WHITE
                    setOnAction {
                        val rowItem = tableView.items[index]
                        val language = cbLinguagem.value ?: Linguagem.PORTUGUESE
                        gerarTagItem(rowItem, language, isAjustar = true)?.let { mHistory.pushAction(it) }
                        tableView.refresh()
                    }
                }

                private val btnNormalizar = JFXButton("Normalizar").apply {
                    styleClass.add("background-Purple2")
                    textFill = Color.WHITE
                    setOnAction {
                        val rowItem = tableView.items[index]
                        val language = cbLinguagem.value ?: Linguagem.PORTUGUESE
                        normalizarTagItem(rowItem, language)?.let { mHistory.pushAction(it) }
                        tableView.refresh()
                    }
                }
                private val btnTitulo = JFXButton("Título").apply {
                    styleClass.add("background-Orange1")
                    textFill = Color.WHITE
                    setOnAction {
                        val rowItem = tableView.items[index]
                        val language = cbLinguagem.value ?: Linguagem.PORTUGUESE
                        formatarTitleCaseTagItem(rowItem, language)?.let { mHistory.pushAction(it) }
                        tableView.refresh()
                    }
                }

                private val btnAmazon = JFXButton("Amazon").apply {
                    styleClass.add("background-Orange1")
                    textFill = Color.WHITE
                    setOnAction { popupAmazon(tableView.items[index]) }
                }
                private val btnEditar = JFXButton("Editar").apply {
                    styleClass.add("background-Blue3")
                    textFill = Color.WHITE
                    setOnAction { abrirPopupComicInfo(listOf(tableView.items[index])) }
                }
                private val btnProcessar = JFXButton("Processar").apply {
                    styleClass.add("background-Black2")
                    textFill = Color.WHITE
                    setOnAction { processarComicInfoItemAsync(tableView.items[index]) }
                }

                private val btnDeletar = JFXButton("Deletar").apply {
                    styleClass.add("background-Red2")
                    textFill = Color.WHITE
                    setOnAction {
                        val rowItem = tableView.items[index]
                        if (ConfirmaModal.confirmacao("Aviso", "Deseja remover o registro?")) {
                            mHistory.removeHistoryForItem(rowItem)
                            mObsListaProcessar.remove(rowItem)
                            tableView.refresh()
                        }
                    }
                }
                private val btnOcr = JFXButton("OCR").apply {
                    styleClass.add("background-White1")
                    textFill = Color.BLACK
                    setOnAction { visualizarSumarioItem(tableView.items[index]) }
                }
                private val btnSalvar = JFXButton("Salvar").apply {
                    styleClass.add("background-Green3")
                    textFill = Color.WHITE
                    setOnAction { salvarComicInfoItemAsync(tableView.items[index]) }
                }

                private val hbox1 = HBox(5.0, btnAplicar, btnAjustar, btnNormalizar, btnTitulo).apply {
                    alignment = Pos.CENTER
                }
                private val hbox2 = HBox(5.0, btnAmazon, btnEditar, btnProcessar).apply {
                    alignment = Pos.CENTER
                }
                private val hbox3 = HBox(5.0, btnDeletar, btnOcr, btnSalvar).apply {
                    alignment = Pos.CENTER
                }

                private val vbox = VBox(5.0, hbox1, hbox2, hbox3).apply {
                    alignment = Pos.CENTER
                }

                override fun updateItem(item: Void?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (empty) {
                        graphic = null
                    } else {
                        val rowItem = tableView.items[index]
                        val isDisable = rowItem.isSalvandoOuProcessando
                        btnAjustar.isDisable = isDisable
                        btnNormalizar.isDisable = isDisable
                        btnTitulo.isDisable = isDisable
                        btnAmazon.isDisable = isDisable
                        btnEditar.isDisable = isDisable
                        btnProcessar.isDisable = isDisable
                        btnDeletar.isDisable = isDisable
                        btnOcr.isDisable = isDisable
                        btnSalvar.isDisable = isDisable
                        graphic = vbox
                    }
                }
            }
        }

        val menu = ContextMenu()

        // GRUPO TAGS
        val gerarTagsAnteriores = MenuItem("Gerar Tags até o Item Atual")
        gerarTagsAnteriores.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null) {
                controllerPai.setCursor(Cursor.WAIT)
                val language = cbLinguagem.value ?: Linguagem.PORTUGUESE
                val index = mObsListaProcessar.indexOf(tbViewProcessar.selectionModel.selectedItem)
                val actions = mutableListOf<ReversibleAction>()
                for (i in 0 until index + 1) {
                    gerarTagItem(mObsListaProcessar[i], language)?.let { actions.add(it) }
                }
                if (actions.isNotEmpty()) mHistory.pushAction(CompositeAction(actions))
                tbViewProcessar.refresh()
                controllerPai.setCursor(null)
            }
        }

        val gerarTagsAtual = MenuItem("Gerar Tags (Ctrl + T)")
        gerarTagsAtual.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null) {
                val language = cbLinguagem.value ?: Linguagem.PORTUGUESE
                gerarTagItem(tbViewProcessar.selectionModel.selectedItem, language)?.let { mHistory.pushAction(it) }
                tbViewProcessar.refresh()
            }
        }

        val tagsAjustar = MenuItem("Ajustar Tags (Ctrl + A)")
        tagsAjustar.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null) {
                val language = cbLinguagem.value ?: Linguagem.PORTUGUESE
                gerarTagItem(tbViewProcessar.selectionModel.selectedItem, language, isAjustar = true)?.let { mHistory.pushAction(it) }
                tbViewProcessar.refresh()
            }
        }

        val tagsAplicar = MenuItem("Aplicar Tags (Shift + Alt + Enter)")
        tagsAplicar.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null) {
                aplicarTag(tbViewProcessar.selectionModel.selectedItem)?.let { mHistory.pushAction(it) }
                tbViewProcessar.refresh()
            }
        }

        val tagsNormalizar = MenuItem("Normalizar Tags (Ctrl + N)")
        tagsNormalizar.setOnAction {
            val selecionados = tbViewProcessar.selectionModel.selectedItems.toList()
            if (selecionados.isNotEmpty()) {
                val language = cbLinguagem.value ?: Linguagem.PORTUGUESE
                val actions = mutableListOf<ReversibleAction>()
                for (item in selecionados) {
                    normalizarTagItem(item, language)?.let { actions.add(it) }
                }
                if (actions.isNotEmpty()) mHistory.pushAction(CompositeAction(actions))
                tbViewProcessar.refresh()
            }
        }

        val tagsTitleCase = MenuItem("Tags Como TÍtulo (Ctrl + Shift + N)")
        tagsTitleCase.setOnAction {
            val selecionados = tbViewProcessar.selectionModel.selectedItems.toList()
            if (selecionados.isNotEmpty()) {
                val language = cbLinguagem.value ?: Linguagem.PORTUGUESE
                val actions = mutableListOf<ReversibleAction>()
                for (item in selecionados) {
                    formatarTitleCaseTagItem(item, language)?.let { actions.add(it) }
                }
                if (actions.isNotEmpty()) mHistory.pushAction(CompositeAction(actions))
                tbViewProcessar.refresh()
            }
        }

        val tagsSubstituir = MenuItem("Substituir nas Tags (Ctrl + R)")
        tagsSubstituir.setOnAction {
            onBtnSubstituirTags()
        }

        val atualizarTagsDoArquivo = MenuItem("Gerar Tags do Arquivo (Através das pastas)")
        atualizarTagsDoArquivo.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null)
                gerarTagsDoArquivo(tbViewProcessar.selectionModel.selectedItem)
        }

        val atualizarPaginaTag = MenuItem("Atualizar Página da Tag")
        atualizarPaginaTag.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null)
                atualizarPaginaTag(tbViewProcessar.selectionModel.selectedItem)
        }

        // GRUPO OCR
        val processarOcr = MenuItem("Processar OCR")
        processarOcr.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null)
                processarOcrItem(tbViewProcessar.selectionModel.selectedItem)
        }

        val visualizarSumario = MenuItem("Visualizar Sumário (Ctrl + O)")
        visualizarSumario.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null)
                visualizarSumarioItem(tbViewProcessar.selectionModel.selectedItem)
        }

        // GRUPO SALVAR
        val salvarAnteriores = MenuItem("Salvar ComicInfo do Inicio até o Atual")
        salvarAnteriores.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null) {
                val index = mObsListaProcessar.indexOf(tbViewProcessar.selectionModel.selectedItem)
                salvarItens(startIndex = 0, endIndex = index + 1)
            }
        }

        val salvar = MenuItem("Salvar ComicInfo (Ctrl + S)")
        salvar.setOnAction {
            val selecionados = tbViewProcessar.selectionModel.selectedItems.toList()
            if (selecionados.isNotEmpty()) {
                if (selecionados.size > 1)
                    salvarListaItens(selecionados)
                else
                    salvarComicInfoItemAsync(selecionados[0])
            }
        }

        val salvarPosteriores = MenuItem("Salvar ComicInfo do Atual até o Final")
        salvarPosteriores.setOnAction {
            if (tbViewProcessar.selectionModel.selectedItem != null) {
                val index = mObsListaProcessar.indexOf(tbViewProcessar.selectionModel.selectedItem)
                salvarItens(startIndex = index, endIndex = mObsListaProcessar.size)
            }
        }

        // GRUPO RECARREGAR
        val recarregarTodos = MenuItem("Recarregar todos ComicInfo")
        recarregarTodos.setOnAction {
            recarregarComicInfoItens(mObsListaProcessar.toList())
            mHistory.clear()
        }

        val recarregarSelecionados = MenuItem("Recarregar ComicInfo")
        recarregarSelecionados.setOnAction {
            val selecionados = tbViewProcessar.selectionModel.selectedItems.toList()
            if (selecionados.isNotEmpty()) {
                recarregarComicInfoItens(selecionados)
                selecionados.forEach { mHistory.removeHistoryForItem(it) }
            } else if (tbViewProcessar.selectionModel.selectedItem != null) {
                val item = tbViewProcessar.selectionModel.selectedItem
                recarregarComicInfoItens(listOf(item))
                mHistory.removeHistoryForItem(item)
            }
        }

        val processarComicInfo = MenuItem("Processar ComicInfo")
        processarComicInfo.setOnAction {
            val selecionados = tbViewProcessar.selectionModel.selectedItems.toList()
            if (selecionados.isNotEmpty())
                processarComicInfoItens(selecionados)
            else
                Notificacoes.notificacao(Notificacao.ALERTA, "Processar ComicInfo", "Selecione pelo menos um registro na tabela.")
        }

        // GRUPO GERAL
        val remover = MenuItem("Remover registro (Del)")
        remover.setOnAction {
            val selected = tbViewProcessar.selectionModel.selectedItems.toList()
            if (selected.isNotEmpty())
                if (ConfirmaModal.confirmacao("Aviso", "Deseja remover o registro?")) {
                    selected.forEach { mHistory.removeHistoryForItem(it) }
                    mObsListaProcessar.removeAll(selected)
                    tbViewProcessar.refresh()
                }
        }

        val editarComicInfo = MenuItem("Editar ComicInfo").apply {
            setOnAction {
                val selected = tbViewProcessar.selectionModel.selectedItems
                if (selected.isNotEmpty())
                    abrirPopupComicInfo(selected.toList())
            }
        }

        val chamarCapitulos = MenuItem("Abrir Capítulo").apply {
            setOnAction { onBtnCapitulos() }
        }

        val chamarAmazon = MenuItem("Consulta Amazon").apply {
            setOnAction { popupAmazon(tbViewProcessar.selectionModel.selectedItem) }
        }

        val chamarImportarSumario = MenuItem("Importar Sumário").apply {
            setOnAction {
                val selected = tbViewProcessar.selectionModel.selectedItems
                if (selected.isNotEmpty())
                    abrirPopupSumario(selected.toList())
            }
        }

        menu.items.addAll(
            editarComicInfo,
            chamarCapitulos,
            chamarImportarSumario,
            chamarAmazon,
            SeparatorMenuItem(),
            tagsAplicar,
            atualizarPaginaTag,
            atualizarTagsDoArquivo,
            gerarTagsAnteriores,
            gerarTagsAtual,
            tagsAjustar,
            tagsNormalizar,
            tagsTitleCase,
            tagsSubstituir,
            SeparatorMenuItem(),

            processarOcr,
            visualizarSumario,
            SeparatorMenuItem(),
            salvarAnteriores,
            salvar,
            salvarPosteriores,
            SeparatorMenuItem(),
            recarregarTodos,
            recarregarSelecionados,
            processarComicInfo,
            SeparatorMenuItem(),
            remover
        )

        tbViewProcessar.contextMenu = menu

        tbViewProcessar.setRowFactory {
            object : TableRow<Processar>() {
                init {
                    setOnMouseClicked { event ->
                        if (event.clickCount == 2 && !isEmpty) {
                            if (item != null && !item.isSalvandoOuProcessando) {
                                abrirPopupComicInfo(listOf(item))
                            }
                        }
                    }
                }

                override fun updateItem(item: Processar?, empty: Boolean) {
                    super.updateItem(item, empty)
                    pseudoClassStateChanged(ALERTA_PSEUDO_CLASS, item?.semComicInfo == true)
                    if (!empty && item != null && item.isSalvandoOuProcessando) {
                        style = "-fx-background-color: #d4edda; -fx-text-background-color: black;"
                    } else {
                        style = ""
                    }
                }
            }
        }

        TextAreaTableCell.setOnKeyPress { pair ->
            val textArea = pair.key
            val key = pair.value
            if ((key.isShiftDown && key.isAltDown) || key.isControlDown) {
                when (key.code) {
                    KeyCode.ENTER -> {
                        if (textArea.text.isEmpty() || !textArea.text.contains(Utils.SEPARADOR_IMPORTACAO))
                            return@setOnKeyPress true

                        val txt = textArea.text ?: ""
                        val linhas = mutableListOf<String>()
                        val separador = Utils.SEPARADOR_CAPITULO
                        for (linha in txt.split("\n")) {
                            if (linha.contains(Utils.SEPARADOR_IMPORTACAO)) {
                                val prefix = linha.substringBefore(Utils.SEPARADOR_IMPORTACAO).trim()
                                val basePrefix = prefix.split(Regex("—|-"), 2)[0].trim()
                                val title = linha.substringAfterLast(separador).trim()
                                val cleanTitle = Utils.limparTitulo(title)
                                linhas.add("$basePrefix — $cleanTitle")
                            } else {
                                linhas.add(linha)
                            }
                        }

                        textArea.replaceText(0, textArea.length, linhas.joinToString(separator = "\n"))
                        key.consume()
                    }

                    KeyCode.DELETE -> {
                        if (textArea.text.isEmpty())
                            return@setOnKeyPress true

                        val lastCaretPos = textArea.caretPosition
                        val txt = textArea.text ?: ""
                        val scroll = textArea.scrollTopProperty().value

                        val before = if (txt.indexOf('\n', lastCaretPos) > 0) txt.substring(0, txt.indexOf('\n', lastCaretPos)) else txt
                        val last = if (txt.indexOf('\n', lastCaretPos) > 0) txt.substring(txt.indexOf('\n', lastCaretPos)) else ""
                        val line = before.substringAfterLast("\n", before) + last.substringBefore("\n", "")
                        
                        if (line.contains(Utils.SEPARADOR_IMPORTACAO)) {
                            val newLine = line.substringBefore(Utils.SEPARADOR_IMPORTACAO).trimEnd()
                            val prefix = before.substringBeforeLast(line)
                            textArea.replaceText(0, textArea.length, (prefix + newLine + last.substringAfter(line.substringAfterLast("\n", line), "")).trim())
                            textArea.positionCaret(prefix.length + newLine.length)
                        } else {
                            val newBefore = before.substringBeforeLast(line)
                            textArea.replaceText(0, textArea.length, (newBefore + last).trim())
                            textArea.positionCaret(newBefore.length)
                        }
                        textArea.scrollTop = scroll
                        key.consume()
                    }

                    KeyCode.LEFT, KeyCode.RIGHT -> {
                        if (textArea.text.isEmpty() || !textArea.text.contains(Utils.SEPARADOR_IMPORTACAO))
                            return@setOnKeyPress true

                        val scroll = textArea.scrollTopProperty().value
                        val selection = textArea.selection
                        val fullText = textArea.text

                        val (processStart, processEnd) = if (selection.length > 0) {
                            Pair(selection.start, selection.end)
                        } else {
                            val caret = textArea.caretPosition
                            val lineStart = fullText.substring(0, caret).lastIndexOf('\n').let { if (it == -1) 0 else it + 1 }
                            val lineEnd = fullText.indexOf('\n', caret).let { if (it == -1) fullText.length else it }
                            Pair(lineStart, lineEnd)
                        }

                        val beforeText = fullText.substring(0, processStart)
                        val selectedText = fullText.substring(processStart, processEnd)
                        val afterText = fullText.substring(processEnd)

                        val separadorImportacao = Utils.SEPARADOR_IMPORTACAO
                        val separadorCapitulo = Utils.SEPARADOR_CAPITULO
                        val separadorImagem = Utils.SEPARADOR_IMAGEM

                        val lines = selectedText.split("\n").toMutableList()
                        val updatedLines = lines.map { line ->
                            if (!line.contains(separadorImportacao)) return@map line

                            val prefix = line.substringBefore(separadorImportacao).trim()
                            val importedPart = line.substringAfterLast(separadorCapitulo).trim()

                            val cleanTitle = Utils.limparTitulo(importedPart)

                            val pagePart = if (prefix.contains(separadorImagem)) prefix.substringBefore(separadorImagem) else ""
                            val labelPart = if (prefix.contains(separadorImagem)) {
                                prefix.substringAfter(separadorImagem).trim()
                            } else {
                                prefix
                            }.split("-", "—")[0].trim()

                            val pagePrefix = if (pagePart.isNotEmpty()) "$pagePart$separadorImagem" else ""
                            "$pagePrefix$labelPart — $cleanTitle"
                        }

                        val newSelectedText = updatedLines.joinToString("\n")
                        textArea.replaceText(processStart, processEnd, newSelectedText)

                        textArea.scrollTop = scroll
                        if (selection.length > 0) {
                            textArea.selectRange(processStart, processStart + newSelectedText.length)
                        } else {
                            textArea.positionCaret(processStart + newSelectedText.length)
                        }
                        key.consume()
                    }

                    KeyCode.UP,
                    KeyCode.DOWN -> {
                        if (textArea.text.isEmpty() || !textArea.text.contains("\n") || !textArea.text.contains(Utils.SEPARADOR_IMPORTACAO))
                            return@setOnKeyPress true

                        val lastCaretPos = textArea.caretPosition

                        val txt = textArea.text ?: ""
                        val linhas = txt.split("\n").toMutableList()
                        val scroll = textArea.scrollTopProperty().value

                        val before = if (txt.indexOf('\n', lastCaretPos) > 0) txt.substring(0, txt.indexOf('\n', lastCaretPos)) else txt
                        val last = if (txt.indexOf('\n', lastCaretPos) > 0) txt.substring(txt.indexOf('\n', lastCaretPos)) else ""
                        val line = before.substringAfterLast("\n", before) + last.substringBefore("\n", "")

                        if (!line.contains(Utils.SEPARADOR_IMPORTACAO))
                            return@setOnKeyPress true

                        var index = -1
                        for ((idx, item) in linhas.withIndex()) {
                            if (item == line) {
                                index = idx
                                break
                            }
                        }

                        if (key.code == KeyCode.UP) {
                            if (index > 0) {
                                if (index >= linhas.size)
                                    index = linhas.size - 2

                                var spaco = -1
                                for (i in (index - 1) downTo 2) {
                                    if (!linhas[i].contains(Utils.SEPARADOR_IMPORTACAO)) {
                                        spaco = i
                                        break
                                    }
                                }

                                var inicio = 1
                                if (spaco > 0)
                                    inicio = spaco + 1

                                for (i in inicio until index + 1) {
                                    linhas[i - 1] =
                                        linhas[i - 1] + if (linhas[i].contains(Utils.SEPARADOR_IMPORTACAO)) Utils.SEPARADOR_IMPORTACAO + linhas[i].substringAfter(Utils.SEPARADOR_IMPORTACAO) else ""
                                    linhas[i] = linhas[i].substringBefore(Utils.SEPARADOR_IMPORTACAO)
                                }
                            }
                        } else if (key.code == KeyCode.DOWN) {
                            if (index < linhas.size - 1) {
                                var spaco = -1
                                for (i in index + 1 until linhas.size) {
                                    if (!linhas[i].contains(Utils.SEPARADOR_IMPORTACAO)) {
                                        spaco = i
                                        break
                                    }
                                }

                                var inicio = linhas.size - 2
                                if (spaco > 0 && spaco < linhas.size - 1)
                                    inicio = spaco - 1

                                for (i in inicio downTo index) {
                                    linhas[i + 1] =
                                        linhas[i + 1] + if (linhas[i].contains(Utils.SEPARADOR_IMPORTACAO)) Utils.SEPARADOR_IMPORTACAO + linhas[i].substringAfter(Utils.SEPARADOR_IMPORTACAO) else ""
                                    linhas[i] = linhas[i].substringBefore(Utils.SEPARADOR_IMPORTACAO)
                                }
                            }
                        }

                        for (i in linhas.size - 1 downTo 0) {
                            val linha = linhas[i]
                            if (linha.contains("-1${Utils.SEPARADOR_IMAGEM}", ignoreCase = true) && !linha.contains(Utils.SEPARADOR_IMPORTACAO, ignoreCase = true))
                                linhas.removeAt(i)
                        }

                        textArea.replaceText(0, textArea.length, linhas.joinToString(separator = "\n"))

                        val caret = if (key.code == KeyCode.UP) {
                            if (textArea.text.length > lastCaretPos)
                                textArea.text.substring(0, lastCaretPos).lastIndexOf(Utils.SEPARADOR_IMPORTACAO)
                            else
                                textArea.text.lastIndexOf(Utils.SEPARADOR_IMPORTACAO)
                        } else
                            textArea.text.indexOf(Utils.SEPARADOR_IMPORTACAO, lastCaretPos)

                        textArea.positionCaret(caret)
                        textArea.scrollTop = scroll
                    }

                    else -> {}
                }
            }

            return@setOnKeyPress true
        }

    }

    private fun configurarAtalhosGrid() {
        tbViewProcessar.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED) { e ->
            if (tbViewProcessar.editingCellProperty().get() != null || e.target is javafx.scene.control.TextInputControl) return@addEventFilter

            val selecionados = tbViewProcessar.selectionModel.selectedItems.toList()
            val selecionado = tbViewProcessar.selectionModel.selectedItem
            val language = cbLinguagem.value ?: Linguagem.PORTUGUESE

            if (e.code.isLetterKey && !e.isControlDown && !e.isAltDown) {
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
                return@addEventFilter
            }

            if (e.isAltDown && e.isShiftDown && e.code.equals(KeyCode.ENTER)) {
                val actions = mutableListOf<ReversibleAction>()
                for (item in selecionados)
                    aplicarTag(item)?.let { actions.add(it) }
                if (actions.isNotEmpty())
                    mHistory.pushAction(CompositeAction(actions))
                tbViewProcessar.refresh()
            } else if (e.isControlDown) {
                when (e.code) {
                    KeyCode.Z -> {
                        val action = if (e.isShiftDown) mHistory.redo() else mHistory.undo()
                        if (action != null) {
                            tbViewProcessar.refresh()
                            (action.getFirstAffectedItem() as? Processar)?.let { item ->
                                val idx = mObsListaProcessar.indexOf(item)
                                if (idx != -1)
                                    tbViewProcessar.scrollTo(idx)
                            }
                        }
                        e.consume()
                    }
                    KeyCode.Y -> {
                        val action = mHistory.redo()
                        if (action != null) {
                            tbViewProcessar.refresh()
                            (action.getFirstAffectedItem() as? Processar)?.let { item ->
                                val idx = mObsListaProcessar.indexOf(item)
                                if (idx != -1) tbViewProcessar.scrollTo(idx)
                            }
                        }
                        e.consume()
                    }
                    KeyCode.S -> {
                        if (selecionados.isNotEmpty()) {
                            if (selecionados.size > 1)
                                salvarListaItens(selecionados)
                            else if (selecionado != null)
                                salvarComicInfoItemAsync(selecionado)
                        }
                        e.consume()
                    }
                    KeyCode.T -> {
                        if (selecionado != null) {
                            gerarTagItem(selecionado, language)?.let { mHistory.pushAction(it) }
                            tbViewProcessar.refresh()
                        }
                        e.consume()
                    }
                    KeyCode.O -> {
                        if (selecionado != null) {
                            visualizarSumarioItem(selecionado)
                        }
                        e.consume()
                    }
                    KeyCode.A -> {
                        if (selecionado != null) {
                            gerarTagItem(selecionado, language, isAjustar = true)?.let { mHistory.pushAction(it) }
                            tbViewProcessar.refresh()
                        }
                        e.consume()
                    }
                    KeyCode.N -> {
                        if (selecionados.isNotEmpty()) {
                            val actions = mutableListOf<ReversibleAction>()
                            if (e.isShiftDown) {
                                for (item in selecionados) {
                                    formatarTitleCaseTagItem(item, language)?.let { actions.add(it) }
                                }
                            } else {
                                for (item in selecionados) {
                                    normalizarTagItem(item, language)?.let { actions.add(it) }
                                }
                            }
                            if (actions.isNotEmpty())
                                mHistory.pushAction(CompositeAction(actions))
                            tbViewProcessar.refresh()
                        }
                        e.consume()
                    }
                    KeyCode.R -> {
                        onBtnSubstituirTags()
                        e.consume()
                    }
                    else -> {}
                }
            } else {
                when (e.code) {
                    KeyCode.DELETE -> {
                        if (selecionados.isNotEmpty()) {
                            if (ConfirmaModal.confirmacao("Aviso", "Deseja remover o registro?")) {
                                selecionados.forEach { mHistory.removeHistoryForItem(it) }
                                mObsListaProcessar.removeAll(selecionados)
                                tbViewProcessar.refresh()
                            }
                            e.consume()
                        }
                    }
                    KeyCode.SPACE -> {
                        if (selecionados.isNotEmpty()) {
                            val novoEstado = !selecionados.first().isProcessado
                            selecionados.forEach { it.isProcessado = novoEstado }
                            tbViewProcessar.refresh()
                            atualizaCheckTodosProcessado()
                        }
                        e.consume()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun gerarTagsDoArquivo(item: Processar) {
        val file = item.file ?: return

        // 1. Preparação da pasta temporária
        val extractDir = File(mPASTA_TEMPORARIA, "extract_${System.currentTimeMillis()}")
        if (extractDir.exists()) extractDir.deleteRecursively()
        extractDir.mkdirs()

        try {
            // 2. Extração total do arquivo
            if (!mRarService.extrairTudo(file, extractDir)) {
                AlertasModal.erro("Erro", "Não foi possível extrair o conteúdo do arquivo.")
                return
            }

            val language = cbLinguagem.value ?: Linguagem.PORTUGUESE
            val mapCapitulos = mutableMapOf<String, String>()

            // 3. Mapear títulos existentes nas tags atuais
            item.tags.split("\n").forEach { linha ->
                if (linha.contains(Utils.SEPARADOR_IMPORTACAO)) {
                    val num = linha.substringAfter(Utils.SEPARADOR_IMPORTACAO).trim().substringBefore(Utils.SEPARADOR_CAPITULO).trim()
                    val titulo = linha.substringAfterLast(Utils.SEPARADOR_CAPITULO).trim()
                    if (num.isNotEmpty() && titulo.isNotEmpty()) mapCapitulos[num] = titulo
                } else if (linha.contains("—") || (linha.contains("-") && !linha.contains(";"))) {
                    val labelPart = if (linha.contains(";")) linha.substringAfter(";").trim() else linha
                    val capPart = labelPart.split("—", "-")[0].trim()
                    val titulo = if (linha.contains("—")) linha.substringAfter("—").trim() else linha.substringAfter("-").trim()

                    val num = Utils.getNumber(capPart)
                    if (num != null && titulo.isNotEmpty() && !titulo.lowercase().let { it == "cover" || it == "back" || it == "all cover" || it == "sumary" || it == "sumário" }) {
                        mapCapitulos[mDecimal.format(num)] = titulo
                    }
                }
            }

            // Extrair títulos do resumo (ComicInfo)
            item.comicInfo?.summary?.let { summary ->
                if (summary.lowercase().contains("*chapter titles manual*") || summary.lowercase().contains("*chapter titles*")) {
                    summary.split("\n").forEach { line ->
                        if (line.contains(":")) {
                            val capRaw = line.substringBefore(":").replace(Regex("(?i)chapter|capítulo|第|話"), "").trim()
                            val titulo = line.substringAfter(":").trim()
                            val num = Utils.getNumber(capRaw)
                            if (num != null && titulo.isNotEmpty()) mapCapitulos[mDecimal.format(num)] = titulo
                        }
                    }
                }
            }

            // 4. Listar e ordenar imagens fisicamente
            val imagensFisicas = extractDir.walkTopDown()
                .filter { it.isFile && Utils.isImage(it.name) }
                .toList()
                .sortedNaturally()

            val newTags = mutableListOf<String>()
            val chaptersFoundInFile = mutableSetOf<String>()
            val foldersProcessed = mutableSetOf<String>()

            imagensFisicas.forEachIndexed { index, fileImg ->
                val fileName = fileImg.name.lowercase()
                val folderPath = fileImg.parentFile.absolutePath

                var tag: String? = null

                // Identificar especiais
                if (fileName.contains("frente") || fileName.contains("cover")) tag = "Cover"
                else if (fileName.contains("tras") || fileName.contains("back")) tag = "Back"
                else if (fileName.contains("tudo") || fileName.contains("all cover")) tag = "All cover"
                else if (fileName.contains("sumario") || fileName.contains("sumário") || fileName.contains("sumary")) tag = "Sumary"

                if (tag == null) {
                    // Identificar capítulos por pastas (primeira imagem da pasta)
                    if (!foldersProcessed.contains(folderPath)) {
                        val regexCap = Regex("(?i)(capítulo|chapter|第|ch)\\s*([\\d.]+)", RegexOption.IGNORE_CASE)
                        val match = regexCap.find(fileImg.parentFile.name)
                        if (match != null) {
                            val numeroStr = match.groupValues[2]
                            val numero = Utils.getNumber(numeroStr)
                            if (numero != null) {
                                val formatado = mDecimal.format(numero)
                                chaptersFoundInFile.add(formatado)
                                val prefix = when (language) {
                                    Linguagem.JAPANESE -> "第${Utils.toNumberJapanese(formatado)}話"
                                    Linguagem.ENGLISH -> "Chapter $formatado"
                                    else -> "Capítulo $formatado"
                                }
                                val titulo = mapCapitulos[formatado]
                                tag = if (titulo != null) {
                                    "$prefix ${Utils.SEPARADOR_IMPORTACAO} $formatado${Utils.SEPARADOR_CAPITULO}$titulo"
                                } else {
                                    prefix
                                }
                            }
                        }
                        foldersProcessed.add(folderPath)
                    }
                }

                if (tag != null) {
                    newTags.add("$index${Utils.SEPARADOR_IMAGEM}$tag")
                }
            }

            // 5. Capítulos novos (não encontrados no arquivo)
            mapCapitulos.keys.subtract(chaptersFoundInFile).sortedBy { Utils.getNumber(it) ?: 0.0 }.forEach { num ->
                val titulo = mapCapitulos[num]
                val prefix = when (language) {
                    Linguagem.JAPANESE -> "第${Utils.toNumberJapanese(num)}話"
                    Linguagem.ENGLISH -> "Chapter $num"
                    else -> "Capítulo $num"
                }
                newTags.add("-1${Utils.SEPARADOR_IMAGEM} Capítulo novo $num ${Utils.SEPARADOR_IMPORTACAO} $num${Utils.SEPARADOR_CAPITULO}$prefix — $titulo")
            }

            val oldTags = item.tags
            val oldProcessado = item.isProcessado
            val newTagsStr = if (newTags.isNotEmpty()) newTags.joinToString(separator = "\n") else item.tags
            val newProcessado = true

            val actions = mutableListOf<ReversibleAction>()
            if (oldTags != newTagsStr) {
                actions.add(PropertyChangeAction(item, oldTags, newTagsStr) { i, v -> i.tags = v })
                item.tags = newTagsStr
            }
            if (oldProcessado != newProcessado) {
                actions.add(PropertyChangeAction(item, oldProcessado, newProcessado) { i, v -> i.isProcessado = v })
                item.isProcessado = newProcessado
            }
            if (actions.isNotEmpty()) {
                mHistory.pushAction(CompositeAction(actions))
            }
            tbViewProcessar.refresh()

        } catch (e: Exception) {
            mLOG.error("Erro ao gerar tags do arquivo: ${e.message}", e)
            AlertasModal.erro("Erro", "Falha ao processar o arquivo: ${e.message}")
        } finally {
            extractDir.deleteRecursively()
        }
    }

    private fun atualizarPaginaTag(item: Processar) {
        val file = item.file ?: return

        // 1. Preparação da pasta temporária
        val extractDir = File(mPASTA_TEMPORARIA, "update_pages_${System.currentTimeMillis()}")
        if (extractDir.exists()) extractDir.deleteRecursively()
        extractDir.mkdirs()

        try {
            // 2. Extração total do arquivo
            if (!mRarService.extrairTudo(file, extractDir)) {
                AlertasModal.erro("Erro", "Não foi possível extrair o conteúdo do arquivo.")
                return
            }

            // 3. Listar apenas imagens fisicamente e ordenar
            val imagensFisicas = extractDir.walkTopDown()
                .filter { it.isFile && Utils.isImage(it.name) }
                .toList()
                .sortedNaturally()

            if (imagensFisicas.isEmpty()) return

            val tags = item.tags.split("\n")
            val updatedTags = mutableListOf<String>()

            tags.forEach { tagLine ->
                if (tagLine.isBlank()) return@forEach

                val parts = tagLine.split(Utils.SEPARADOR_IMAGEM, limit = 2)
                if (parts.size < 2) {
                    updatedTags.add(tagLine)
                    return@forEach
                }

                val tagText = parts[1]
                val tagTextLower = tagText.lowercase()

                var foundIndex = -1

                // 4. Identificar o alvo
                if (tagTextLower.contains("cover") || tagTextLower.contains("frente")) {
                    foundIndex = imagensFisicas.indexOfFirst { it.name.lowercase().let { name -> name.contains("frente") || name.contains("cover") } }
                } else if (tagTextLower.contains("back") || tagTextLower.contains("tras")) {
                    foundIndex = imagensFisicas.indexOfFirst { it.name.lowercase().let { name -> name.contains("tras") || name.contains("back") } }
                } else if (tagTextLower.contains("all cover") || tagTextLower.contains("tudo")) {
                    foundIndex = imagensFisicas.indexOfFirst { it.name.lowercase().let { name -> name.contains("tudo") || name.contains("all cover") } }
                } else if (tagTextLower.contains("sumary") || tagTextLower.contains("sumario") || tagTextLower.contains("sumário")) {
                    foundIndex = imagensFisicas.indexOfFirst { it.name.lowercase().let { name -> name.contains("sumario") || name.contains("sumário") || name.contains("sumary") } }
                } else {
                    // Procurar por capítulo
                    val capInfo = if (tagText.contains(Utils.SEPARADOR_IMPORTACAO)) {
                        tagText.substringAfter(Utils.SEPARADOR_IMPORTACAO).trim().substringBefore(Utils.SEPARADOR_CAPITULO).trim()
                    } else {
                        val match = Regex("(?i)(capítulo|chapter|第|ch)\\s*([\\d.]+)").find(tagText)
                        if (match != null) {
                            val num = Utils.getNumber(match.groupValues[2])
                            if (num != null) mDecimal.format(num) else null
                        } else null
                    }

                    if (capInfo != null) {
                        val num = Utils.getNumber(capInfo)
                        if (num != null) {
                            val formatado = mDecimal.format(num)
                            // Procura a primeira página de uma pasta que contenha o número do capítulo
                            foundIndex = imagensFisicas.indexOfFirst { fileImg ->
                                val folderName = fileImg.parentFile.name.lowercase()
                                folderName.contains(formatado) || folderName.contains(num.toInt().toString())
                            }
                        }
                    }
                }

                if (foundIndex != -1) {
                    updatedTags.add("$foundIndex${Utils.SEPARADOR_IMAGEM}$tagText")
                } else {
                    updatedTags.add(tagLine)
                }
            }

            val oldTags = item.tags
            val newTagsStr = updatedTags.joinToString("\n")
            if (oldTags != newTagsStr) {
                mHistory.pushAction(PropertyChangeAction(item, oldTags, newTagsStr) { i, v -> i.tags = v })
                item.tags = newTagsStr
            }
            tbViewProcessar.refresh()

        } catch (e: Exception) {
            mLOG.error("Erro ao atualizar páginas: ${e.message}", e)
            AlertasModal.erro("Erro", "Falha ao processar o arquivo: ${e.message}")
        } finally {
            extractDir.deleteRecursively()
        }
    }

    private fun <T> List<T>.sortedNaturally(): List<T> {
        val regex = "(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)".toRegex()
        return this.sortedWith { o1, o2 ->
            val s1 = if (o1 is File) o1.absolutePath else o1.toString()
            val s2 = if (o2 is File) o2.absolutePath else o2.toString()

            val chunks1 = s1.split(regex)
            val chunks2 = s2.split(regex)
            var result = 0
            for (i in 0 until minOf(chunks1.size, chunks2.size)) {
                val c1 = chunks1[i]
                val c2 = chunks2[i]
                if (c1 != c2) {
                    val n1 = c1.toLongOrNull()
                    val n2 = c2.toLongOrNull()
                    result = if (n1 != null && n2 != null) n1.compareTo(n2) else c1.compareTo(c2, ignoreCase = true)
                    if (result != 0) break
                }
            }
            if (result == 0) chunks1.size.compareTo(chunks2.size) else result
        }
    }

    private fun linkaCelulas() {
        clProcessado.cellValueFactory = PropertyValueFactory("isProcessado")
        clCapa.setCellValueFactory { param ->
            SimpleStringProperty(param.value.capaPath)
        }
        clCapa.setCellFactory {
            object : TableCell<Processar, String>() {
                private val imageView = ImageView().apply {
                    fitWidth = 50.0
                    fitHeight = 70.0
                    isPreserveRatio = true
                }
                override fun updateItem(item: String?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (empty || item.isNullOrEmpty()) {
                        graphic = null
                    } else {
                        val file = File(item)
                        if (file.exists()) {
                            imageView.image = Image(file.toURI().toString(), true)
                            graphic = imageView
                        } else {
                            graphic = null
                        }
                    }
                }
            }
        }
        clArquivo.cellValueFactory = PropertyValueFactory("arquivo")
        clSerie.setCellValueFactory { param ->
            val item = param.value
            if (item.comicInfo != null)
                SimpleStringProperty(item.comicInfo!!.series)
            else
                SimpleStringProperty("")
        }

        clTitulo.setCellValueFactory { param ->
            val item = param.value
            if (item.comicInfo != null)
                SimpleStringProperty(item.comicInfo!!.title)
            else
                SimpleStringProperty("")
        }

        clEditora.setCellValueFactory { param ->
            val item = param.value
            if (item.comicInfo != null)
                SimpleStringProperty(item.comicInfo!!.publisher)
            else
                SimpleStringProperty("")
        }

        clPublicacao.setCellValueFactory { param ->
            val item = param.value
            if (item.comicInfo != null && item.comicInfo!!.year != null)
                SimpleStringProperty("${item.comicInfo!!.day}/${item.comicInfo!!.month}/${item.comicInfo!!.year}")
            else
                SimpleStringProperty("")
        }

        clResumo.setCellValueFactory { param ->
            val item = param.value
            if (item.comicInfo != null && item.comicInfo!!.summary != null)
                SimpleStringProperty(item.comicInfo!!.summary)
            else
                SimpleStringProperty("")
        }

        clTags.cellValueFactory = PropertyValueFactory("tags")

        editaColunas()
    }


    private fun abrirPopupComicInfo(itens: List<Processar>) {
        PopupComicInfoController.abreTelaComicInfo(controllerPai.rootStack, controllerPai.rootTab, itens) {
            tbViewProcessar.refresh()
        }
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        cbLinguagem.items.addAll(Linguagem.JAPANESE, Linguagem.ENGLISH, Linguagem.PORTUGUESE)
        cbLinguagem.selectionModel.selectFirst()

        tbViewProcessar.selectionModel.selectionMode = SelectionMode.MULTIPLE

        linkaCelulas()
        configurarAtalhosGrid()
        configuraTextEdit()
        configurarDragAndDrop()
        habilita()
    }

    private fun abrirPopupSumario(itens: List<Processar>) {
        PopupSumarioController.abreTelaSumario(controllerPai.rootStack, controllerPai.rootTab, itens) {
            tbViewProcessar.refresh()
            null
        }
    }

    companion object {
        val fxmlLocate: URL get() = TelaInicialController::class.java.getResource("/view/AbaComicInfo.fxml")

        internal fun arquivoPossuiComicInfoNaListagem(conteudo: List<String>): Boolean =
            conteudo.any { entrada ->
                entrada.equals(Utils.COMICINFO, ignoreCase = true) ||
                    entrada.endsWith("/${Utils.COMICINFO}", ignoreCase = true) ||
                    entrada.endsWith("\\${Utils.COMICINFO}", ignoreCase = true)
            }

        internal fun criarComicInfoBasico(arquivo: File, linguagem: Linguagem): ComicInfo {
            val nome = arquivo.name.substringBeforeLast(".")
            val titulo = nome.substringBeforeLast("-").trim().ifEmpty { nome }
            val volume = Regex("volume\\s*(\\d+)", RegexOption.IGNORE_CASE)
                .find(nome)
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull() ?: 0
            return ComicInfo(
                id = UUID.randomUUID(),
                comic = titulo,
                title = titulo,
                series = titulo,
                volume = volume,
                languageISO = linguagem.sigla,
                manga = Manga.Yes
            )
        }

        internal fun tagsFromComic(comic: ComicInfo): String {
            val bookMarks = comic.pages
                ?.filter { !it.bookmark.isNullOrEmpty() }
                ?.map { it.image.toString() + Utils.SEPARADOR_IMAGEM + it.bookmark }
                ?.toSet()
                ?: emptySet()
            return bookMarks.joinToString(separator = "\n")
        }
    }

}